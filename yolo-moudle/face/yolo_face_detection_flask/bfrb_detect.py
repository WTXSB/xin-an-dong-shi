# -*- coding: utf-8 -*-
"""
BFRB（身体聚焦重复行为）检测模块：手部 + 人脸检测 + 手脸接触几何规则判定。

权重来源（HuggingFace，公开免登录，经 hf-mirror.com 镜像下载）：
  - 手部姿态模型 weights/yolo26_hand_pose.pt
      https://huggingface.co/poptoz/yolo26-hand-pose-face-detection
      （checkpoints/yolo26_hand_pose.pt）
      类别：{0: 'hand'}，pose 任务，每只手输出 21 个关键点（0=手腕，4/8/12/16/20=五指指尖）
      训练数据：Ultralytics Hand Keypoints 数据集（真实照片，MediaPipe 标注）
  - 人脸检测模型 weights/yolo26_face.pt
      同上仓库 checkpoints/yolo26_face.pt
      类别：{0: 'face'}，detect 任务
      训练数据：WiderFace

几何规则（方案一，无 BFRB 标注数据集时的落地近似）：
  1. 每只手框与所有人脸框计算 IoU，并取手中心/可见指尖到脸框的最近距离，
     用脸框对角线长度归一化；
  2. IoU > IOU_THRESHOLD 或归一化距离 < DIST_THRESHOLD 判定为“手脸接触/贴近”线索；
  3. 接触点在脸框上缘及以上 → “拔头发/抓挠头皮类”线索；
     落在脸框下 1/3（嘴部估计区域）→ 倾向“咬指甲/进食类”线索；
     落在脸框上 1/3（眼部估计区域）→ 倾向“抠皮肤/揉眼类”线索；
     其余为“手摸脸”一般线索。

注意：本模块只输出“觉察线索”，不做医学诊断；文案语气保持温柔、不贴标签。
"""

import numpy as np
from ultralytics import YOLO

# 判定阈值（经验值，可在构造时覆盖）
IOU_THRESHOLD = 0.02      # 手脸框 IoU 超过该值视为重叠
DIST_THRESHOLD = 0.5      # 手到脸的归一化最近距离小于该值视为贴近
FINGERTIP_INDICES = [4, 8, 12, 16, 20]  # 拇指/食指/中指/无名指/小指指尖

CUE_LABELS = {
    "mouth": "咬指甲/进食类",
    "eye": "抠皮肤/揉眼类",
    "hair": "拔头发/抓挠头皮类",
    "face": "手摸脸",
}


def _iou(box_a, box_b):
    """计算两个 xyxy 框的 IoU。"""
    x1 = max(box_a[0], box_b[0])
    y1 = max(box_a[1], box_b[1])
    x2 = min(box_a[2], box_b[2])
    y2 = min(box_a[3], box_b[3])
    inter_w = max(0.0, x2 - x1)
    inter_h = max(0.0, y2 - y1)
    inter = inter_w * inter_h
    if inter <= 0:
        return 0.0
    area_a = max(0.0, (box_a[2] - box_a[0])) * max(0.0, (box_a[3] - box_a[1]))
    area_b = max(0.0, (box_b[2] - box_b[0])) * max(0.0, (box_b[3] - box_b[1]))
    union = area_a + area_b - inter
    return inter / union if union > 0 else 0.0


def _point_to_box_distance(px, py, box):
    """点到 xyxy 框的最近距离（框内为 0）。"""
    dx = max(box[0] - px, 0.0, px - box[2])
    dy = max(box[1] - py, 0.0, py - box[3])
    return float(np.hypot(dx, dy))


class BfrbDetector:
    """手部 + 人脸检测与手脸接触规则判定。"""

    def __init__(self, hand_weights, face_weights, conf=0.3,
                 iou_threshold=IOU_THRESHOLD, dist_threshold=DIST_THRESHOLD):
        self.hand_model = YOLO(hand_weights)  # yolo26_hand_pose.pt（pose，含 21 关键点）
        self.face_model = YOLO(face_weights)  # yolo26_face.pt（detect）
        self.conf = conf
        self.iou_threshold = iou_threshold
        self.dist_threshold = dist_threshold

    def _detect_hands(self, img):
        """返回 [{'bbox': [...], 'confidence': float, 'fingertips': [(x, y), ...]}]"""
        results = self.hand_model.predict(source=img, conf=self.conf, imgsz=640, verbose=False)[0]
        hands = []
        if results.boxes is None or len(results.boxes) == 0:
            return hands
        boxes = results.boxes.xyxy.cpu().numpy()
        confs = results.boxes.conf.cpu().numpy()
        kpts = None
        kpt_confs = None
        if getattr(results, "keypoints", None) is not None and results.keypoints.xy is not None:
            kpts = results.keypoints.xy.cpu().numpy()          # [N, 21, 2]
            if results.keypoints.conf is not None:
                kpt_confs = results.keypoints.conf.cpu().numpy()  # [N, 21]
        for i, (box, cf) in enumerate(zip(boxes, confs)):
            fingertips = []
            if kpts is not None and i < len(kpts):
                for k in FINGERTIP_INDICES:
                    if k < len(kpts[i]):
                        x, y = float(kpts[i][k][0]), float(kpts[i][k][1])
                        vis_ok = True
                        if kpt_confs is not None and i < len(kpt_confs):
                            vis_ok = float(kpt_confs[i][k]) > 0.3
                        if vis_ok and x > 0 and y > 0:
                            fingertips.append((x, y))
            hands.append({
                "bbox": [float(v) for v in box],
                "confidence": round(float(cf), 3),
                "fingertips": fingertips,
            })
        return hands

    def _detect_faces(self, img):
        """返回 [{'bbox': [...], 'confidence': float}]"""
        results = self.face_model.predict(source=img, conf=self.conf, imgsz=640, verbose=False)[0]
        faces = []
        if results.boxes is None or len(results.boxes) == 0:
            return faces
        boxes = results.boxes.xyxy.cpu().numpy()
        confs = results.boxes.conf.cpu().numpy()
        for box, cf in zip(boxes, confs):
            faces.append({
                "bbox": [float(v) for v in box],
                "confidence": round(float(cf), 3),
            })
        return faces

    def _judge_contact(self, hand, face):
        """对一对手/脸框做接触判定，返回 contact dict 或 None。"""
        fb = face["bbox"]
        face_diag = float(np.hypot(fb[2] - fb[0], fb[3] - fb[1])) or 1.0
        iou = _iou(hand["bbox"], fb)

        # 候选接触点：手框中心 + 可见指尖，取到脸框的最近距离
        hb = hand["bbox"]
        points = [((hb[0] + hb[2]) / 2.0, (hb[1] + hb[3]) / 2.0)] + hand.get("fingertips", [])
        dists = [_point_to_box_distance(px, py, fb) for px, py in points]
        min_idx = int(np.argmin(dists))
        min_dist = dists[min_idx]
        norm_dist = min_dist / face_diag
        contact_point = points[min_idx]

        if iou <= self.iou_threshold and norm_dist >= self.dist_threshold:
            return None

        # 区域估计：按接触点相对脸框的纵向位置划分。
        # 注意先判断“脸框上缘及以上”（此时 rel_y 可能为负），否则头顶的接触会被误归为眼部。
        face_h = max(fb[3] - fb[1], 1.0)
        rel_y = (contact_point[1] - fb[1]) / face_h
        if contact_point[1] <= fb[1]:
            region = "hair"           # 脸框上缘及以上 → 头皮/头发区域
        elif rel_y >= 2.0 / 3.0:
            region = "mouth"          # 下 1/3 → 嘴部估计区域
        elif rel_y <= 1.0 / 3.0:
            region = "eye"            # 上 1/3 → 眼部估计区域
        else:
            region = "face"           # 中间 → 面颊/一般摸脸

        return {
            "iou": round(iou, 4),
            "center_distance_norm": round(norm_dist, 3),
            "contact_point": [round(contact_point[0], 1), round(contact_point[1], 1)],
            "region": region,
            "cue_type": CUE_LABELS.get(region, "手摸脸"),
        }

    @staticmethod
    def _build_awareness_text(faces, hands, contacts):
        """温柔口吻的觉察文案：看见线索，不贴标签、不下诊断。"""
        if not faces and not hands:
            return ("这一帧里暂时没有看清脸和手部。如果愿意，可以换一个光线充足、"
                    "正面对着镜头的角度再试一次。")
        if not contacts:
            if hands and faces:
                return ("画面里看到了脸和手部，它们此刻保持着一些距离。"
                        "身体此刻看起来是放松的，可以留意一下这份安定。")
            return ("画面里检测到了" +
                    ("脸部" if faces else "手部") +
                    "。这一刻没有明显的身体信号，安好便是好消息。")
        cue_types = {c["cue_type"] for c in contacts}
        if "咬指甲/进食类" in cue_types:
            detail = "手部靠近了嘴部区域"
        elif "抠皮肤/揉眼类" in cue_types:
            detail = "手部靠近了眼部区域"
        elif "拔头发/抓挠头皮类" in cue_types:
            detail = "手部靠近了头发附近"
        else:
            detail = "手部靠近了脸部"
        return (f"画面里{detail}，这可能是一个值得留意的身体信号。"
                "不必自责，也不必急着停下——先轻轻注意到它，"
                "就是照顾自己的开始。")

    def detect(self, img):
        """对一张 BGR 图像（cv2.imread 结果）做完整 BFRB 线索分析。"""
        faces = self._detect_faces(img)
        hands = self._detect_hands(img)

        contacts = []
        for hi, hand in enumerate(hands):
            best = None
            best_face_idx = -1
            for fi, face in enumerate(faces):
                c = self._judge_contact(hand, face)
                if c is not None and (best is None or c["center_distance_norm"] < best["center_distance_norm"]):
                    best = c
                    best_face_idx = fi
            if best is not None:
                best["hand_index"] = hi
                best["face_index"] = best_face_idx
                contacts.append(best)

        # 输出里隐去内部计算用的指尖坐标，保持返回结构简洁
        out_hands = [{"bbox": h["bbox"], "confidence": h["confidence"]} for h in hands]
        return {
            "faces": faces,
            "hands": out_hands,
            "contacts": contacts,
            "bfrb_cue": len(contacts) > 0,
            "awareness_text": self._build_awareness_text(faces, hands, contacts),
        }


# ==================== 方案三：BFRB 行为直检模型（bfrb_behavior.pt） ====================
# 由 FaceTouch 公开数据 + 自录视频经伪标注流水线训练而来，直接输出行为类别，
# 不再依赖手脸几何规则。类别顺序见 datasets/bfrb_merged/data.yaml。
BEHAVIOR_CLASS_NAMES = ["nail_biting", "skin_picking", "hair_pulling", "face_touching", "hand_normal"]

BEHAVIOR_CUE_LABELS = {
    "nail_biting": "咬指甲/进食类",
    "skin_picking": "抠皮肤/揉眼类",
    "hair_pulling": "拔头发/抓挠头皮类",
    "face_touching": "手摸脸",
    "hand_normal": "手部自然状态",
}


def is_behavior_model(model):
    """判断一个已加载的 YOLO 模型是否为 BFRB 行为直检模型（按类别名匹配）。"""
    names = [model.names[i] for i in sorted(model.names)] if isinstance(model.names, dict) else list(model.names)
    return names == BEHAVIOR_CLASS_NAMES


class BehaviorDetector:
    """BFRB 行为直检：单模型直接输出行为类别框。"""

    def __init__(self, weights, conf=0.25):
        self.model = YOLO(weights)
        self.conf = conf

    @classmethod
    def from_model(cls, model, conf=0.25):
        """用已加载的 YOLO 模型实例构造（避免重复加载权重）。"""
        obj = cls.__new__(cls)
        obj.model = model
        obj.conf = conf
        return obj

    def detect(self, img):
        results = self.model.predict(source=img, conf=self.conf, imgsz=640, verbose=False)[0]
        detections = []
        if results.boxes is not None and len(results.boxes) > 0:
            for box, cf, cls in zip(results.boxes.xyxy.cpu().numpy(),
                                    results.boxes.conf.cpu().numpy(),
                                    results.boxes.cls.cpu().numpy()):
                name = results.names[int(cls)]
                detections.append({
                    "bbox": [float(v) for v in box],
                    "confidence": round(float(cf), 3),
                    "behavior": name,
                    "cue_type": BEHAVIOR_CUE_LABELS.get(name, name),
                })
        cues = [d for d in detections if d["behavior"] != "hand_normal"]
        return {
            "detections": detections,
            "bfrb_cue": len(cues) > 0,
            "awareness_text": self._build_text(detections, cues),
        }

    @staticmethod
    def _build_text(detections, cues):
        if not detections:
            return ("这一帧里没有看清手部动作。如果愿意，可以换一个光线充足、"
                    "手部完整入镜的角度再试一次。")
        if not cues:
            return ("画面里的手部处于自然状态，没有留意到重复性的身体信号。"
                    "此刻的安稳，也值得被自己看见。")
        cue_types = []
        for d in cues:
            if d["cue_type"] not in cue_types:
                cue_types.append(d["cue_type"])
        detail = "、".join(cue_types[:2])
        return (f"画面里留意到「{detail}」的身体信号。"
                "不必自责，也不必急着停下——先轻轻注意到它，就是照顾自己的开始。")
