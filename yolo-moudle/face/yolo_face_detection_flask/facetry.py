# -*- coding: utf-8 -*-
import json
import os
import subprocess
import sys
import cv2
import requests
import time
import numpy as np  # 必须导入，用于后面类型判断
from flask import Flask, Response, request
from ultralytics import YOLO
from flask_socketio import SocketIO, emit

# 兼容通过 scripts/run-flask-with-certifi.py（runpy）启动的场景：
# 那种启动方式不会把本文件所在目录加入 sys.path，导致找不到 bfrb_detect。
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from bfrb_detect import BfrbDetector, BehaviorDetector, is_behavior_model  # BFRB 检测（见 bfrb_detect.py）
from bfrb_events import BfrbEventAggregator

# 表情类别（顺序必须和你训练时的 data.yaml 完全一致！）
EMOTION_CLASSES = ['angry', 'happy', 'neutral', 'sad']

ANALYSIS_MODES = [
    {
        "value": "combined",
        "label": "综合分析（情绪 + BFRB双证据）",
        "modelLabel": "emotion.pt + bfrb_behavior.pt + 手脸几何",
    },
    {"value": "emotion", "label": "情绪表情识别", "modelLabel": "emotion.pt"},
    {"value": "bfrb_behavior", "label": "BFRB行为直检", "modelLabel": "bfrb_behavior.pt"},
    {"value": "bfrb_geometry", "label": "BFRB手脸几何", "modelLabel": "yolo26_hand_pose.pt + yolo26_face.pt"},
]

MODE_ALIASES = {
    "behavior": "bfrb_behavior",
    "geometry": "bfrb_geometry",
    "bfrb": "bfrb_behavior",
}

# ==================== 安全的 json dumps（解决 numpy 类型问题） ====================
def safe_json_dumps(data):
    """把 numpy 类型转成 Python 原生类型后再 json 序列化"""
    def convert(o):
        if isinstance(o, np.integer):
            return int(o)
        if isinstance(o, np.floating):
            return float(o)
        if isinstance(o, np.ndarray):
            return o.tolist()
        if isinstance(o, list):
            return [convert(i) for i in o]
        if isinstance(o, dict):
            return {k: convert(v) for k, v in o.items()}
        return o
    return json.dumps(convert(data), ensure_ascii=False)

def as_bool(value, default=False):
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in ("1", "true", "yes", "on")

class VideoProcessingApp:
    def __init__(self, host='0.0.0.0', port=5000):
        self.app = Flask(__name__)
        self.socketio = SocketIO(self.app, cors_allowed_origins="*")
        self.host = host
        self.port = port
        self.setup_routes()
        self.data = {}
        self.paths = {
            'download': './runs/video/download.mp4',
            'output': './runs/video/output.mp4',
            'camera_output': "./runs/video/camera_output.avi",
            'video_output': "./runs/video/camera_output.avi",
            'result_img': './runs/result.jpg'
        }
        self.recording = False
        self.bfrb_detectors = {}  # BFRB 检测器缓存：key=(weight, face_weight, conf)

    def setup_routes(self):
        self.app.add_url_rule('/file_names', 'file_names', self.file_names, methods=['GET'])
        self.app.add_url_rule('/predictImg', 'predictImg', self.predictImg, methods=['POST'])
        self.app.add_url_rule('/predictVideo', 'predictVideo', self.predictVideo)
        self.app.add_url_rule('/predictCamera', 'predictCamera', self.predictCamera)
        self.app.add_url_rule('/stopCamera', 'stopCamera', self.stopCamera, methods=['GET'])
        self.app.add_url_rule('/predictBfrb', 'predictBfrb', self.predictBfrb, methods=['POST'])

        @self.socketio.on('connect')
        def handle_connect():
            emit('message', {'data': 'Connected to WebSocket server!'})

    def run(self):
        self.socketio.run(self.app, host=self.host, port=self.port, allow_unsafe_werkzeug=True)

    def file_names(self):
        items = [{'value': f, 'label': f} for f in os.listdir("./weights") if f.endswith('.pt')]
        return json.dumps({
            'weight_items': items,
            'analysis_modes': ANALYSIS_MODES,
        }, ensure_ascii=False)

    @staticmethod
    def normalize_mode(value):
        mode = MODE_ALIASES.get(str(value or '').strip(), str(value or '').strip())
        valid_modes = {item["value"] for item in ANALYSIS_MODES}
        return mode if mode in valid_modes else "emotion"

    @staticmethod
    def safe_weight_name(value, fallback):
        name = str(value or fallback)
        if os.path.basename(name) != name or not name.endswith('.pt'):
            return fallback
        return name if os.path.exists(f'./weights/{name}') else fallback

    def get_emotion_detector(self, weight, conf):
        weight = self.safe_weight_name(weight, 'emotion.pt')
        # Keep one tracker per request. Reusing a tracker would leak track IDs from
        # one user's image/video/camera session into the next session.
        return EmotionDetector(
            yolo_weights=f'./weights/{weight}',
            conf=float(conf),
        )

    def build_analyzers(self, data):
        """Create the model set selected by the shared image/video/camera mode."""
        mode = self.normalize_mode(data.get('kind'))
        conf = min(max(float(data.get('conf', 0.5)), 0.05), 0.95)
        analyzers = {"mode": mode, "emotion": None, "behavior": None, "geometry": None}

        if mode in ("combined", "emotion"):
            requested_weight = data.get('weight') if mode == "emotion" else 'emotion.pt'
            analyzers["emotion"] = self.get_emotion_detector(requested_weight, conf)
        if mode in ("combined", "bfrb_behavior"):
            _, analyzers["behavior"] = self.get_bfrb_detector('bfrb_behavior.pt', 'yolo26_face.pt', conf)
        if mode in ("combined", "bfrb_geometry"):
            _, analyzers["geometry"] = self.get_bfrb_detector('yolo26_hand_pose.pt', 'yolo26_face.pt', conf)
        return analyzers

    @staticmethod
    def normalize_behavior_cues(result):
        cues = []
        for detection in result.get("detections", []):
            if detection.get("behavior") == "hand_normal":
                continue
            cues.append({
                "behavior": detection.get("behavior", "unknown"),
                "cueType": detection.get("cue_type", detection.get("behavior", "未知线索")),
                "confidence": float(detection.get("confidence", 0)),
                "bbox": detection.get("bbox", []),
                "evidenceType": "behavior-model",
            })
        return cues

    @staticmethod
    def normalize_geometry_cues(result):
        cues = []
        faces = result.get("faces", [])
        hands = result.get("hands", [])
        for contact in result.get("contacts", []):
            hand_index = int(contact.get("hand_index", -1))
            face_index = int(contact.get("face_index", -1))
            hand = hands[hand_index] if 0 <= hand_index < len(hands) else {}
            face = faces[face_index] if 0 <= face_index < len(faces) else {}
            confidence = min(float(hand.get("confidence", 0)), float(face.get("confidence", 0)))
            cues.append({
                "behavior": f'geometry_{contact.get("region", "face")}',
                "cueType": contact.get("cue_type", "手脸接触线索"),
                "confidence": confidence,
                "bbox": hand.get("bbox", []),
                "evidenceType": "hand-face-geometry",
                "geometry": {
                    "iou": contact.get("iou", 0),
                    "distance": contact.get("center_distance_norm", 0),
                    "contactPoint": contact.get("contact_point", []),
                    "faceBbox": face.get("bbox", []),
                },
            })
        return cues

    @staticmethod
    def merge_bfrb_evidence(behavior_cues, geometry_cues):
        """Use behavior detections as events and geometry as corroborating evidence."""
        merged = []
        geometry_by_type = {}
        for cue in geometry_cues:
            geometry_by_type.setdefault(cue["cueType"], []).append(cue)

        covered_types = set()
        for cue in behavior_cues:
            item = dict(cue)
            support = geometry_by_type.get(cue["cueType"], [])
            if support:
                strongest = max(support, key=lambda value: value.get("confidence", 0))
                item["evidenceType"] = "behavior-model+hand-face-geometry"
                item["geometry"] = strongest.get("geometry", {})
                item["geometryConfidence"] = strongest.get("confidence", 0)
                covered_types.add(cue["cueType"])
            merged.append(item)

        # Geometry remains useful when the behavior model misses a frame.
        merged.extend(cue for cue in geometry_cues if cue["cueType"] not in covered_types)
        return merged

    @staticmethod
    def draw_bfrb_evidence(frame, cues):
        for cue in cues:
            bbox = cue.get("bbox") or []
            if len(bbox) != 4:
                continue
            x1, y1, x2, y2 = [int(value) for value in bbox]
            is_supported = "+" in cue.get("evidenceType", "")
            color = (70, 170, 70) if is_supported else (180, 100, 40)
            cv2.rectangle(frame, (x1, y1), (x2, y2), color, 2)
            confidence = float(cue.get("confidence", 0))
            label = f'BFRB {confidence:.2f}'
            cv2.putText(frame, label, (x1, max(y1 - 8, 16)), cv2.FONT_HERSHEY_SIMPLEX, 0.55, color, 2)
        return frame

    def analyze_frame(self, frame, analyzers, is_image=False, run_bfrb=True):
        """Run one shared analysis pipeline without changing legacy emotion fields."""
        annotated = frame.copy()
        emotion = {"labels": [], "confidences": [], "bboxes": [], "trackIds": []}
        behavior_result = {}
        geometry_result = {}

        if analyzers.get("emotion") is not None:
            annotated, labels, confs, bboxes, track_ids = analyzers["emotion"].process_frame(
                frame.copy(), is_image=is_image
            )
            emotion = {
                "labels": labels,
                "confidences": confs,
                "bboxes": bboxes,
                "trackIds": track_ids,
            }

        if run_bfrb and analyzers.get("behavior") is not None:
            behavior_result = analyzers["behavior"].detect(frame.copy())
        if run_bfrb and analyzers.get("geometry") is not None:
            geometry_result = analyzers["geometry"].detect(frame.copy())

        behavior_cues = self.normalize_behavior_cues(behavior_result)
        geometry_cues = self.normalize_geometry_cues(geometry_result)
        if analyzers["mode"] == "combined":
            cues = self.merge_bfrb_evidence(behavior_cues, geometry_cues)
        else:
            cues = behavior_cues or geometry_cues

        self.draw_bfrb_evidence(annotated, cues)
        awareness_parts = []
        if behavior_result.get("awareness_text"):
            awareness_parts.append(behavior_result["awareness_text"])
        if geometry_result.get("awareness_text") and geometry_result.get("contacts"):
            awareness_parts.append("手脸空间关系也提供了辅助佐证。")

        return annotated, {
            "mode": analyzers["mode"],
            "emotion": emotion,
            "bfrb": {
                "cue": bool(cues),
                "cues": cues,
                "behaviorDetections": behavior_result.get("detections", []),
                "geometry": {
                    "faces": geometry_result.get("faces", []),
                    "hands": geometry_result.get("hands", []),
                    "contacts": geometry_result.get("contacts", []),
                },
                "awarenessText": "".join(awareness_parts),
            },
        }

    # ====================== BFRB 手脸接触线索检测 ======================
    def get_bfrb_detector(self, weight, face_weight, conf):
        """BFRB 检测器懒加载缓存，避免每次请求重复加载模型。

        返回 (mode, detector)：
        - mode='behavior'：weight 是 BFRB 行为直检模型（如 bfrb_behavior.pt，
          5 类行为输出），用 BehaviorDetector 直接检测；
        - mode='geometry'：weight 是手部模型，配合 face_weight 走几何规则判定。
        """
        key = (weight, face_weight, conf)
        if key not in self.bfrb_detectors:
            probe = YOLO(f'./weights/{weight}')
            if is_behavior_model(probe):
                self.bfrb_detectors[key] = ('behavior', BehaviorDetector.from_model(probe, conf))
            else:
                self.bfrb_detectors[key] = ('geometry', BfrbDetector(
                    hand_weights=f'./weights/{weight}',
                    face_weights=f'./weights/{face_weight}',
                    conf=conf,
                ))
        return self.bfrb_detectors[key]

    def predictBfrb(self):
        """POST /predictBfrb：multipart 上传图片(file 字段)，
        可选表单参数 weight(默认 yolo26_hand_pose.pt 几何规则模式；
        传 bfrb_behavior.pt 等行为模型则走直检模式) /
        face_weight(几何模式用,默认 yolo26_face.pt) / conf(默认 0.3)。"""
        if 'file' not in request.files:
            return json.dumps({"status": 400, "message": "缺少上传文件字段 file"}, ensure_ascii=False)
        file = request.files['file']
        weight = request.form.get('weight', 'yolo26_hand_pose.pt')
        face_weight = request.form.get('face_weight', 'yolo26_face.pt')
        conf = float(request.form.get('conf', 0.3))

        # 只允许 weights/ 目录下的纯文件名，防止路径穿越
        if os.path.basename(weight) != weight or os.path.basename(face_weight) != face_weight:
            return json.dumps({"status": 400, "message": "非法的权重文件名"}, ensure_ascii=False)
        if not os.path.exists(f'./weights/{weight}'):
            return json.dumps({"status": 400, "message": "权重文件不存在"}, ensure_ascii=False)

        img_bytes = np.frombuffer(file.read(), np.uint8)
        img = cv2.imdecode(img_bytes, cv2.IMREAD_COLOR)
        if img is None:
            return json.dumps({"status": 400, "message": "图片解码失败"}, ensure_ascii=False)

        mode, detector = self.get_bfrb_detector(weight, face_weight, conf)
        result = detector.detect(img)
        if mode == 'behavior':
            return safe_json_dumps({
                "status": 200,
                "message": "检测完成",
                "mode": "behavior",
                "bfrbCue": result["bfrb_cue"],
                "detections": result["detections"],
                "awarenessText": result["awareness_text"],
            })
        return safe_json_dumps({
            "status": 200,
            "message": "检测完成",
            "mode": "geometry",
            "bfrbCue": result["bfrb_cue"],
            "faces": result["faces"],
            "hands": result["hands"],
            "contacts": result["contacts"],
            "awarenessText": result["awareness_text"],
        })

    # ====================== 图片预测 ======================
    def predictImg(self):
        started_at = time.time()
        data = request.get_json() or {}
        self.data = data

        analyzers = self.build_analyzers(data)

        img_path = './temp_img.jpg'
        self.download(data.get("inputImg", ""), img_path)
        img = cv2.imread(img_path)
        if img is None:
            return json.dumps({"status": 400, "message": "图片加载失败"}, ensure_ascii=False)

        result_img, analysis = self.analyze_frame(img, analyzers, is_image=True)
        emotion = analysis["emotion"]
        labels = emotion["labels"]
        confs = emotion["confidences"]
        bboxes = emotion["bboxes"]
        track_ids = emotion["trackIds"]

        cv2.imwrite(self.paths['result_img'], result_img)
        uploaded_url = self.upload(self.paths['result_img'])
        if os.path.exists(img_path):
            os.remove(img_path)

        emotion_map = {'angry': '生气', 'happy': '高兴', 'neutral': '中性', 'sad': '悲伤'}
        save_record = as_bool(data.get("keepRecord"), True)
        for i, (label, conf, bbox) in enumerate(zip(labels, confs, bboxes)):
            if not save_record:
                break
            tid = track_ids[i] if i < len(track_ids) and track_ids[i] is not None else f"img_{int(time.time())}_{i}"
            emotion_data = {
                "emotionKind": emotion_map.get(label, label),
                "confidence": conf,
                "username": data.get("username", ""),
                "startTime": data.get("startTime", ""),
                "resultImg": uploaded_url,
                "bbox": bbox
            }
            self.save_data(safe_json_dumps(emotion_data), 'http://localhost:9999/emotion')

        has_signal = bool(labels or analysis["bfrb"]["cues"])
        completed_without_signal = analyzers["mode"] != "emotion"
        return safe_json_dumps({
            "status": 200 if has_signal or completed_without_signal else 400,
            "message": "综合感知完成" if has_signal else "分析完成，暂未检测到清晰的情绪或BFRB线索",
            "outImg": uploaded_url,
            "label": json.dumps(labels),
            "confidence": json.dumps(confs),
            "allTime": f"{time.time() - started_at:.2f}",
            "personCount": len(labels),
            "analysis": analysis,
        })

    # ====================== 视频预测 ======================
    def predictVideo(self):
        video_data = request.args.to_dict()
        self.data = video_data
        save_record = as_bool(video_data.get("saveRecord", video_data.get("keepRecord")), True)
        keep_media = as_bool(video_data.get("keepMedia"), False)
        analyzers = self.build_analyzers(video_data)

        recorded_ids = set()
        self.download(video_data.get("inputVideo", ""), self.paths['download'])
        cap = cv2.VideoCapture(self.paths['download'])
        if not cap.isOpened():
            return Response("视频打开失败", status=400)

        fps = max(cap.get(cv2.CAP_PROP_FPS), 1)
        bfrb_sample_interval = max(int(round(fps / 5)), 1)
        event_aggregator = BfrbEventAggregator(fps=fps, min_hits=3, max_gap_seconds=0.5)
        emotion_stats = {}
        w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        writer = cv2.VideoWriter(self.paths['video_output'], cv2.VideoWriter_fourcc(*'XVID'), fps, (w, h))

        def generate():
            nonlocal recorded_ids
            total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
            processed = 0

            while cap.isOpened():
                ret, frame = cap.read()
                if not ret:
                    break

                run_bfrb = processed % bfrb_sample_interval == 0
                frame, analysis = self.analyze_frame(frame, analyzers, run_bfrb=run_bfrb)
                emotion = analysis["emotion"]
                labels = emotion["labels"]
                confs = emotion["confidences"]
                bboxes = emotion["bboxes"]
                track_ids = emotion["trackIds"]

                for label, confidence in zip(labels, confs):
                    item = emotion_stats.setdefault(label, {"count": 0, "confidences": []})
                    item["count"] += 1
                    item["confidences"].append(float(confidence))

                if run_bfrb:
                    event_aggregator.update(processed, analysis["bfrb"]["cues"])
                    self.socketio.emit('bfrb_live', {'data': {
                        "scene": "video",
                        "frameSeconds": round(processed / fps, 2),
                        "cues": analysis["bfrb"]["cues"],
                    }})

                emotion_map = {'angry': '生气', 'happy': '高兴', 'neutral': '中性', 'sad': '悲伤'}
                for label, conf, bbox, tid in zip(labels, confs, bboxes, track_ids):
                    if save_record and tid not in recorded_ids:
                        recorded_ids.add(tid)
                        emotion_data = {
                            "emotionKind": emotion_map.get(label, label),
                            "confidence": conf,
                            "username": video_data.get("username", ""),
                            "startTime": video_data.get("startTime", ""),
                            "bbox": bbox
                        }
                        self.save_data(safe_json_dumps(emotion_data), 'http://localhost:9999/emotion')  # 使用安全序列化

                writer.write(frame)
                _, jpeg = cv2.imencode('.jpg', frame)
                processed += 1
                if processed % 10 == 0 and total_frames > 0:
                    progress = processed / total_frames * 100
                    self.socketio.emit('progress', {'data': round(progress, 1)})

                yield b'--frame\r\nContent-Type: image/jpeg\r\n\r\n' + jpeg.tobytes() + b'\r\n'

            cap.release()
            writer.release()

            bfrb_summary = event_aggregator.finish(processed)
            emotion_summary = []
            for label, values in emotion_stats.items():
                confidences = values["confidences"]
                emotion_summary.append({
                    "label": label,
                    "frameCount": values["count"],
                    "averageConfidence": round(sum(confidences) / len(confidences), 3),
                    "maxConfidence": round(max(confidences), 3),
                })
            emotion_summary.sort(key=lambda item: -item["frameCount"])
            self.socketio.emit('analysis_result', {'data': {
                "scene": "video",
                "mode": analyzers["mode"],
                "emotionSummary": emotion_summary,
                "bfrbSummary": bfrb_summary,
            }})

            for p in self.convert_avi_to_mp4(self.paths['video_output']):
                self.socketio.emit('progress', {'data': p})

            url = self.upload(self.paths['output']) if keep_media else ""
            if not keep_media:
                video_data["inputVideo"] = ""
            video_data["outVideo"] = url or ""
            if not save_record:
                self.cleanup_files([self.paths['download'], self.paths['output'], self.paths['video_output']])
                return
            self.save_data(safe_json_dumps(video_data), 'http://localhost:9999/videoRecords')  # 安全序列化
            self.cleanup_files([self.paths['download'], self.paths['output'], self.paths['video_output']])

        return Response(generate(), mimetype='multipart/x-mixed-replace; boundary=frame')

    # ====================== 摄像头实时预测 ======================
    def predictCamera(self):
        camera_data = request.args.to_dict()
        self.data = camera_data
        save_record = as_bool(camera_data.get("saveRecord", camera_data.get("keepRecord")), True)
        keep_media = as_bool(camera_data.get("keepMedia"), False)
        analyzers = self.build_analyzers(camera_data)

        recorded_ids = set()
        cap = cv2.VideoCapture(0)
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
        writer = cv2.VideoWriter(self.paths['camera_output'], cv2.VideoWriter_fourcc(*'XVID'), 20, (640, 480))
        self.recording = True
        event_aggregator = BfrbEventAggregator(fps=20, min_hits=3, max_gap_seconds=0.5)
        emotion_stats = {}

        def generate():
            nonlocal recorded_ids
            fps_counter = 0
            fps_start = time.time()
            frame_index = 0

            while self.recording:
                ret, frame = cap.read()
                if not ret:
                    continue

                run_bfrb = frame_index % 4 == 0
                frame, analysis = self.analyze_frame(frame, analyzers, run_bfrb=run_bfrb)
                emotion = analysis["emotion"]
                labels = emotion["labels"]
                confs = emotion["confidences"]
                bboxes = emotion["bboxes"]
                track_ids = emotion["trackIds"]

                for label, confidence in zip(labels, confs):
                    item = emotion_stats.setdefault(label, {"count": 0, "confidences": []})
                    item["count"] += 1
                    item["confidences"].append(float(confidence))

                if run_bfrb:
                    event_aggregator.update(frame_index, analysis["bfrb"]["cues"])
                    self.socketio.emit('bfrb_live', {'data': {
                        "scene": "camera",
                        "frameSeconds": round(frame_index / 20, 2),
                        "cues": analysis["bfrb"]["cues"],
                    }})

                emotion_map = {'angry': '生气', 'happy': '高兴', 'neutral': '中性', 'sad': '悲伤'}
                for label, conf, bbox, tid in zip(labels, confs, bboxes, track_ids):
                    if save_record and tid not in recorded_ids:
                        recorded_ids.add(tid)
                        emotion_data = {
                            "emotion_id": str(tid),
                            "emotionType": emotion_map.get(label, label),
                            "confidence": conf,
                            "username": camera_data.get("username", ""),
                            "startTime": camera_data.get("startTime", ""),
                            "bbox": bbox
                        }
                        self.save_data(safe_json_dumps(emotion_data), 'http://localhost:9999/emotion')  # 安全序列化

                if self.recording:
                    writer.write(frame)

                _, jpeg = cv2.imencode('.jpg', frame)
                fps_counter += 1
                if fps_counter % 10 == 0:
                    elapsed = time.time() - fps_start
                    fps = fps_counter / elapsed if elapsed > 0 else 0
                    self.socketio.emit('fps', {'data': f"{fps:.1f}"})
                frame_index += 1

                yield b'--frame\r\nContent-Type: image/jpeg\r\n\r\n' + jpeg.tobytes() + b'\r\n'

            cap.release()
            writer.release()
            bfrb_summary = event_aggregator.finish(frame_index)
            emotion_summary = []
            for label, values in emotion_stats.items():
                confidences = values["confidences"]
                emotion_summary.append({
                    "label": label,
                    "frameCount": values["count"],
                    "averageConfidence": round(sum(confidences) / len(confidences), 3),
                    "maxConfidence": round(max(confidences), 3),
                })
            emotion_summary.sort(key=lambda item: -item["frameCount"])
            self.socketio.emit('analysis_result', {'data': {
                "scene": "camera",
                "mode": analyzers["mode"],
                "emotionSummary": emotion_summary,
                "bfrbSummary": bfrb_summary,
            }})
            for p in self.convert_avi_to_mp4(self.paths['camera_output']):
                self.socketio.emit('progress', {'data': p})

            url = self.upload(self.paths['output']) if keep_media else ""
            camera_data["outVideo"] = url or ""
            if not save_record:
                self.cleanup_files([self.paths['output'], self.paths['camera_output']])
                return
            self.save_data(safe_json_dumps(camera_data), 'http://localhost:9999/cameraRecords')
            self.cleanup_files([self.paths['output'], self.paths['camera_output']])

        return Response(generate(), mimetype='multipart/x-mixed-replace; boundary=frame')

    def stopCamera(self):
        self.recording = False
        return json.dumps({"status": 200, "message": "已停止"}, ensure_ascii=False)

    # ====================== 工具函数 ======================
    def save_data(self, data, url):
        try:
            requests.post(url, data=data, headers={'Content-Type': 'application/json'}, timeout=5)
        except Exception as e:
            print("上传记录失败:", e)

    def convert_avi_to_mp4(self, avi_path):
        cmd = f'ffmpeg -i "{avi_path}" -c:v libx264 -crf 23 "{self.paths["output"]}" -y'
        p = subprocess.Popen(cmd, shell=True, stderr=subprocess.PIPE, text=True, bufsize=1)
        total = self.get_video_duration(avi_path)
        for line in p.stderr:
            if 'time=' in line:
                try:
                    t = line.split('time=')[1].split()[0]
                    h, m, s = map(float, t.split(':'))
                    done = h*3600 + m*60 + s
                    if total > 0:
                        yield min(99.9, done / total * 100)
                except:
                    pass
        p.wait()
        yield 100

    def get_video_duration(self, path):
        c = cv2.VideoCapture(path)
        f = c.get(cv2.CAP_PROP_FRAME_COUNT)
        fps = c.get(cv2.CAP_PROP_FPS)
        c.release()
        return f/fps if fps > 0 else 1

    def upload(self, path):
        if not os.path.exists(path): return None
        try:
            with open(path, 'rb') as f:
                r = requests.post("http://localhost:9999/files/upload", files={'file': f}, timeout=60)
                return r.json().get('data') if r.status_code == 200 else None
        except Exception as e:
            print("上传失败:", e)
            return None

    def download(self, url, save_path):
        os.makedirs(os.path.dirname(save_path), exist_ok=True)
        try:
            r = requests.get(url, stream=True, timeout=60)
            r.raise_for_status()
            with open(save_path, 'wb') as f:
                for chunk in r.iter_content(8192):
                    f.write(chunk)
        except Exception as e:
            print("下载失败:", e)

    def cleanup_files(self, files):
        for f in files:
            if os.path.exists(f):
                try: os.remove(f)
                except: pass


# ====================== YOLOv11 + ByteTrack 核心检测器 ======================
class EmotionDetector:
    def __init__(self, yolo_weights, conf=0.25):
        self.model = YOLO(yolo_weights)  # 自动支持 YOLOv11
        self.conf = conf

    def process_frame(self, frame, is_image=False):
        results = self.model.track(
            source=frame,
            conf=self.conf,
            imgsz=640,
            persist=True,
            tracker="bytetrack.yaml",
            verbose=False
        )[0]

        labels, confs, bboxes, track_ids = [], [], [], []
        color_map = {'angry': (0,0,255), 'happy': (0,255,0), 'neutral': (255,255,0), 'sad': (255,0,0)}

        if results.boxes.id is not None:
            boxes = results.boxes.xyxy.cpu().numpy().astype(int)
            cls   = results.boxes.cls.cpu().numpy().astype(int)
            conf  = results.boxes.conf.cpu().numpy()
            ids   = results.boxes.id.cpu().numpy().astype(int)  # numpy.int64

            for box, c, cf, tid in zip(boxes, cls, conf, ids):
                if c >= len(EMOTION_CLASSES): continue
                label = EMOTION_CLASSES[c]
                x1, y1, x2, y2 = box
                color = color_map.get(label, (255,255,255))

                cv2.rectangle(frame, (x1, y1), (x2, y2), color, 2)
                text = f"ID:{tid} {label} {cf:.2f}"
                (tw, th), _ = cv2.getTextSize(text, cv2.FONT_HERSHEY_SIMPLEX, 0.6, 2)
                cv2.rectangle(frame, (x1, y1-th-12), (x1+tw, y1), color, -1)
                cv2.putText(frame, text, (x1, y1-5), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255,255,255), 2)

                labels.append(label)
                confs.append(f"{cf:.2f}")
                bboxes.append([x1, y1, x2, y2])
                track_ids.append(tid)  # 保持原样，safe_json_dumps 会自动转 int

        return frame, labels, confs, bboxes, track_ids


# ====================== 启动 ======================
if __name__ == '__main__':
    os.makedirs('./runs/video', exist_ok=True)
    os.makedirs('./weights', exist_ok=True)
    app = VideoProcessingApp()
    app.run()
