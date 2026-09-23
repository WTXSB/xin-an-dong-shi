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

# 表情类别（顺序必须和你训练时的 data.yaml 完全一致！）
EMOTION_CLASSES = ['angry', 'happy', 'neutral', 'sad']

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
        self.emotion_detector = None
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
        return json.dumps({'weight_items': items}, ensure_ascii=False)

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
        data = request.get_json()
        self.data = data

        self.emotion_detector = EmotionDetector(
            yolo_weights=f'./weights/{data["weight"]}',
            conf=float(data["conf"])
        )

        img_path = './temp_img.jpg'
        self.download(data["inputImg"], img_path)
        img = cv2.imread(img_path)
        if img is None:
            return json.dumps({"status": 400, "message": "图片加载失败"}, ensure_ascii=False)

        result_img, labels, confs, bboxes, track_ids = self.emotion_detector.process_frame(img, is_image=True)

        cv2.imwrite(self.paths['result_img'], result_img)
        uploaded_url = self.upload(self.paths['result_img'])
        if os.path.exists(img_path):
            os.remove(img_path)

        emotion_map = {'angry': '生气', 'happy': '高兴', 'neutral': '中性', 'sad': '悲伤'}
        for i, (label, conf, bbox) in enumerate(zip(labels, confs, bboxes)):
            tid = track_ids[i] if i < len(track_ids) and track_ids[i] is not None else f"img_{int(time.time())}_{i}"
            emotion_data = {
                "emotionKind": emotion_map.get(label, label),
                "confidence": conf,
                "username": data["username"],
                "startTime": data["startTime"],
                "resultImg": uploaded_url,
                "bbox": bbox
            }
            self.save_data(safe_json_dumps(emotion_data), 'http://localhost:9999/emotion')

        return json.dumps({
            "status": 200 if labels else 400,
            "message": "预测成功" if labels else "未检测到表情",
            "outImg": uploaded_url,
            "label": json.dumps(labels),
            "confidence": json.dumps(confs)
        }, ensure_ascii=False)

    # ====================== 视频预测 ======================
    def predictVideo(self):
        self.data = request.args.to_dict()
        save_record = as_bool(self.data.get("saveRecord", self.data.get("keepRecord")), True)
        keep_media = as_bool(self.data.get("keepMedia"), False)
        self.emotion_detector = EmotionDetector(
            yolo_weights=f'./weights/{self.data["weight"]}',
            conf=float(self.data["conf"])
        )

        recorded_ids = set()
        self.download(self.data["inputVideo"], self.paths['download'])
        cap = cv2.VideoCapture(self.paths['download'])
        if not cap.isOpened():
            return Response("视频打开失败", status=400)

        fps = max(cap.get(cv2.CAP_PROP_FPS), 1)
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

                frame, labels, confs, bboxes, track_ids = self.emotion_detector.process_frame(frame)

                emotion_map = {'angry': '生气', 'happy': '高兴', 'neutral': '中性', 'sad': '悲伤'}
                for label, conf, bbox, tid in zip(labels, confs, bboxes, track_ids):
                    if save_record and tid not in recorded_ids:
                        recorded_ids.add(tid)
                        emotion_data = {
                            "emotionKind": emotion_map.get(label, label),
                            "confidence": conf,
                            "username": self.data["username"],
                            "startTime": self.data["startTime"],
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

            for p in self.convert_avi_to_mp4(self.paths['video_output']):
                self.socketio.emit('progress', {'data': p})

            url = self.upload(self.paths['output']) if keep_media else ""
            if not keep_media:
                self.data["inputVideo"] = ""
            self.data["outVideo"] = url or ""
            if not save_record:
                self.cleanup_files([self.paths['download'], self.paths['output'], self.paths['video_output']])
                return
            self.save_data(safe_json_dumps(self.data), 'http://localhost:9999/videoRecords')  # 安全序列化
            self.cleanup_files([self.paths['download'], self.paths['output'], self.paths['video_output']])

        return Response(generate(), mimetype='multipart/x-mixed-replace; boundary=frame')

    # ====================== 摄像头实时预测 ======================
    def predictCamera(self):
        self.data = request.args.to_dict()
        save_record = as_bool(self.data.get("saveRecord", self.data.get("keepRecord")), True)
        keep_media = as_bool(self.data.get("keepMedia"), False)
        self.emotion_detector = EmotionDetector(
            yolo_weights=f'./weights/{self.data["weight"]}',
            conf=float(self.data["conf"])
        )

        recorded_ids = set()
        cap = cv2.VideoCapture(0)
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
        writer = cv2.VideoWriter(self.paths['camera_output'], cv2.VideoWriter_fourcc(*'XVID'), 20, (640, 480))
        self.recording = True

        def generate():
            nonlocal recorded_ids
            fps_counter = 0
            fps_start = time.time()

            while self.recording:
                ret, frame = cap.read()
                if not ret:
                    continue

                frame, labels, confs, bboxes, track_ids = self.emotion_detector.process_frame(frame)

                emotion_map = {'angry': '生气', 'happy': '高兴', 'neutral': '中性', 'sad': '悲伤'}
                for label, conf, bbox, tid in zip(labels, confs, bboxes, track_ids):
                    if save_record and tid not in recorded_ids:
                        recorded_ids.add(tid)
                        emotion_data = {
                            "emotion_id": str(tid),
                            "emotionType": emotion_map.get(label, label),
                            "confidence": conf,
                            "username": self.data["username"],
                            "startTime": self.data["startTime"],
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

                yield b'--frame\r\nContent-Type: image/jpeg\r\n\r\n' + jpeg.tobytes() + b'\r\n'

            cap.release()
            writer.release()
            for p in self.convert_avi_to_mp4(self.paths['camera_output']):
                self.socketio.emit('progress', {'data': p})

            url = self.upload(self.paths['output']) if keep_media else ""
            self.data["outVideo"] = url or ""
            if not save_record:
                self.cleanup_files([self.paths['output'], self.paths['camera_output']])
                return
            self.save_data(safe_json_dumps(self.data), 'http://localhost:9999/cameraRecords')
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
