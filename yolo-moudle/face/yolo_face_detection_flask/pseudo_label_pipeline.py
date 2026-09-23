# -*- coding: utf-8 -*-
"""
伪标注数据集流水线：视频/图片 → YOLO 格式 BFRB 检测数据集。

用法：
    cd yolo-moudle/face/yolo_face_detection_flask
    python pseudo_label_pipeline.py --input <视频文件或目录> --output <数据集目录> [--every 5] [--conf 0.3]

示例：
    python pseudo_label_pipeline.py --input ../../../test-samples/emotion-change-mafw.mp4 \
        --output datasets/pseudo_test --every 5 --conf 0.3

输入：
    - 单个视频文件（mp4/avi/mov）：按 --every 每 N 帧抽一帧；
    - 单个图片文件（jpg/png）：直接使用；
    - 目录：递归查找上述后缀的文件。

处理逻辑（每帧用 BfrbDetector 推理，模型全程只加载一次）：
    1. 有 contact（手脸接触）时按 region 映射类别，标注框取对应手的 bbox：
         mouth -> nail_biting(0)、eye -> skin_picking(1)、
         hair  -> hair_pulling(2)、face -> face_touching(3)
       同一帧多个 contact 写多行；
    2. 有手但无 contact -> hand_normal(4)，每只手写一行；
    3. 无手 -> 背景图（只存图片、不写 label 文件），用于降低误检。

过滤：
    - 模糊帧：cv2.Laplacian 方差 < 50 跳过；
    - 去重：与上一保留帧的均值灰度差过小（< DUP_DIFF_THRESHOLD）跳过。

输出（YOLO 数据集结构，train:val = 8:2 随机划分，固定种子 42）：
    output/images/train|val/*.jpg
    output/labels/train|val/*.txt      （背景图不写 label 文件）
    output/data.yaml                   （nc=5，names 顺序固定）
    output/qa/*.jpg                    每类最多 5 张画框抽检图
并在结束时打印统计：总帧数、各类别标注数、背景图数、跳过帧数。
"""

import argparse
import random
import shutil
import sys
from pathlib import Path

import cv2
import numpy as np

from bfrb_detect import BfrbDetector

# 模型权重（相对本脚本所在目录）
SCRIPT_DIR = Path(__file__).resolve().parent
HAND_WEIGHTS = SCRIPT_DIR / "weights" / "yolo26_hand_pose.pt"
FACE_WEIGHTS = SCRIPT_DIR / "weights" / "yolo26_face.pt"

VIDEO_EXTS = {".mp4", ".avi", ".mov"}
IMAGE_EXTS = {".jpg", ".jpeg", ".png"}

# 类别定义（顺序即 data.yaml 的 names 顺序）
CLASS_NAMES = ["nail_biting", "skin_picking", "hair_pulling", "face_touching", "hand_normal"]
REGION_TO_CLASS = {"mouth": 0, "eye": 1, "hair": 2, "face": 3}
HAND_NORMAL_CLASS = 4

# 过滤阈值
BLUR_THRESHOLD = 50.0        # Laplacian 方差，低于该值视为模糊
DUP_DIFF_THRESHOLD = 3.0     # 与上一保留帧的均值灰度差，低于该值视为重复
QA_PER_CLASS = 5             # 每类最多保存的抽检图数
SEED = 42

# 抽检图每类颜色（BGR）
CLASS_COLORS = [
    (0, 0, 255),    # nail_biting 红
    (0, 165, 255),  # skin_picking 橙
    (255, 0, 255),  # hair_pulling 紫
    (0, 255, 0),    # face_touching 绿
    (255, 255, 0),  # hand_normal 青
]


def collect_inputs(input_path):
    """收集输入：返回 [(kind, path), ...]，kind ∈ {'video', 'image'}。"""
    p = Path(input_path)
    if not p.exists():
        raise FileNotFoundError(f"输入路径不存在: {input_path}")
    files = [p] if p.is_file() else sorted(f for f in p.rglob("*") if f.is_file())
    items = []
    for f in files:
        ext = f.suffix.lower()
        if ext in VIDEO_EXTS:
            items.append(("video", f))
        elif ext in IMAGE_EXTS:
            items.append(("image", f))
    return items


def iter_video_frames(video_path, every):
    """生成器：从视频中每 every 帧抽一帧，产出 (frame, frame_index)。"""
    cap = cv2.VideoCapture(str(video_path))
    if not cap.isOpened():
        print(f"[警告] 视频打不开，已跳过: {video_path}")
        return
    idx = 0
    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                break
            if idx % every == 0:
                yield frame, idx
            idx += 1
    finally:
        cap.release()


def is_blurry(gray, threshold=BLUR_THRESHOLD):
    return cv2.Laplacian(gray, cv2.CV_64F).var() < threshold


def is_duplicate(gray_small, prev_gray_small):
    """与上一保留帧的小尺寸灰度图做均值差比较。"""
    if prev_gray_small is None:
        return False
    return float(np.mean(cv2.absdiff(gray_small, prev_gray_small))) < DUP_DIFF_THRESHOLD


def yolo_line(class_id, bbox, img_w, img_h):
    """xyxy bbox -> YOLO 归一化 'class cx cy w h' 行。"""
    x1, y1, x2, y2 = bbox
    cx = ((x1 + x2) / 2.0) / img_w
    cy = ((y1 + y2) / 2.0) / img_h
    w = (x2 - x1) / img_w
    h = (y2 - y1) / img_h
    return f"{class_id} {cx:.6f} {cy:.6f} {w:.6f} {h:.6f}"


def detect_to_labels(detector, frame):
    """推理一帧，返回 [(class_id, bbox_xyxy), ...]。无手返回空列表（背景图）。"""
    result = detector.detect(frame)
    hands = result["hands"]
    contacts = result["contacts"]
    labels = []
    contacted_hands = set()
    for c in contacts:
        cls = REGION_TO_CLASS.get(c.get("region"))
        hi = c.get("hand_index")
        if cls is None or hi is None or hi >= len(hands):
            continue
        labels.append((cls, hands[hi]["bbox"]))
        contacted_hands.add(hi)
    if labels:
        return labels
    # 有手但无 contact -> hand_normal
    for h in hands:
        labels.append((HAND_NORMAL_CLASS, h["bbox"]))
    return labels  # 无手时为空列表


def draw_qa_image(frame, labels):
    """画框 + 类别名，返回图像。"""
    out = frame.copy()
    for cls, bbox in labels:
        x1, y1, x2, y2 = [int(v) for v in bbox]
        color = CLASS_COLORS[cls]
        cv2.rectangle(out, (x1, y1), (x2, y2), color, 2)
        cv2.putText(out, CLASS_NAMES[cls], (x1, max(y1 - 5, 15)),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)
    return out


def main():
    parser = argparse.ArgumentParser(description="BFRB 伪标注数据集流水线（视频/图片 -> YOLO 格式）")
    parser.add_argument("--input", required=True, help="视频文件、图片文件或目录（递归）")
    parser.add_argument("--output", required=True, help="数据集输出目录")
    parser.add_argument("--every", type=int, default=5, help="视频抽帧间隔，每 N 帧取一帧（默认 5）")
    parser.add_argument("--conf", type=float, default=0.3, help="检测置信度阈值（默认 0.3）")
    parser.add_argument("--blur", type=float, default=BLUR_THRESHOLD,
                        help="模糊帧过滤阈值（Laplacian 方差，默认 50；手机压缩视频画质偏软，可用 1~2 近似关闭）")
    args = parser.parse_args()

    out_dir = Path(args.output)
    qa_dir = out_dir / "qa"
    qa_dir.mkdir(parents=True, exist_ok=True)

    # 收集输入
    items = collect_inputs(args.input)
    if not items:
        print("未找到可处理的视频/图片文件。")
        return
    print(f"输入文件 {len(items)} 个（视频 {sum(1 for k, _ in items if k == 'video')}，"
          f"图片 {sum(1 for k, _ in items if k == 'image')}），抽帧间隔 every={args.every}")

    # 模型只加载一次
    print("加载模型中（仅一次）...")
    detector = BfrbDetector(str(HAND_WEIGHTS), str(FACE_WEIGHTS), conf=args.conf)

    # 先暂存样本，最后统一划分 train/val
    samples = []  # [(frame, [(cls, bbox), ...]), ...]，labels 为空即背景图
    stats = {
        "total_frames": 0,
        "skipped_blur": 0,
        "skipped_dup": 0,
        "infer_errors": 0,
        "background": 0,
        "class_counts": {name: 0 for name in CLASS_NAMES},
    }
    qa_saved = {i: 0 for i in range(len(CLASS_NAMES))}
    prev_gray_small = None

    for kind, path in items:
        if kind == "video":
            frame_iter = iter_video_frames(path, max(args.every, 1))
        else:
            frame_iter = iter([(cv2.imread(str(path)), 0)])
        for frame, fidx in frame_iter:
            if frame is None:
                print(f"[警告] 图片读取失败，已跳过: {path}")
                continue
            stats["total_frames"] += 1

            # 过滤：模糊
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            if is_blurry(gray, args.blur):
                stats["skipped_blur"] += 1
                continue
            # 过滤：与上一保留帧差异过小
            gray_small = cv2.resize(gray, (64, 64))
            if is_duplicate(gray_small, prev_gray_small):
                stats["skipped_dup"] += 1
                continue
            prev_gray_small = gray_small

            # 推理（单帧异常不中断）
            try:
                labels = detect_to_labels(detector, frame)
            except Exception as e:
                stats["infer_errors"] += 1
                print(f"[警告] 推理异常，跳过该帧（{path.name}#{fidx}）: {e}")
                continue

            if not labels:
                stats["background"] += 1
            for cls, _ in labels:
                stats["class_counts"][CLASS_NAMES[cls]] += 1
            samples.append((frame, labels))

            # 抽检图：每类最多 QA_PER_CLASS 张
            for cls in {c for c, _ in labels}:
                if qa_saved[cls] < QA_PER_CLASS:
                    qa_img = draw_qa_image(frame, labels)
                    cv2.imwrite(str(qa_dir / f"{CLASS_NAMES[cls]}_{qa_saved[cls]:02d}.jpg"), qa_img)
                    qa_saved[cls] += 1

    # 8:2 随机划分（固定种子）
    random.seed(SEED)
    random.shuffle(samples)
    n_val = max(1, int(len(samples) * 0.2)) if samples else 0
    val_set, train_set = samples[:n_val], samples[n_val:]

    for split, split_samples in (("train", train_set), ("val", val_set)):
        img_dir = out_dir / "images" / split
        lbl_dir = out_dir / "labels" / split
        img_dir.mkdir(parents=True, exist_ok=True)
        lbl_dir.mkdir(parents=True, exist_ok=True)
        for i, (frame, labels) in enumerate(split_samples):
            stem = f"{split}_{i:05d}"
            cv2.imwrite(str(img_dir / f"{stem}.jpg"), frame)
            if labels:  # 背景图不写 label 文件
                h, w = frame.shape[:2]
                lines = [yolo_line(cls, bbox, w, h) for cls, bbox in labels]
                (lbl_dir / f"{stem}.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")

    # data.yaml
    names_yaml = "\n".join(f"  {i}: {name}" for i, name in enumerate(CLASS_NAMES))
    (out_dir / "data.yaml").write_text(
        f"path: {out_dir.resolve().as_posix()}\n"
        f"train: images/train\n"
        f"val: images/val\n"
        f"nc: {len(CLASS_NAMES)}\n"
        f"names:\n{names_yaml}\n",
        encoding="utf-8",
    )

    # 统计输出
    kept = len(samples)
    print("\n===== 伪标注流水线统计 =====")
    print(f"输入总帧数（抽帧后）: {stats['total_frames']}")
    print(f"保留帧数: {kept}（train {len(train_set)} / val {len(val_set)}）")
    print(f"跳过-模糊帧: {stats['skipped_blur']}")
    print(f"跳过-重复帧: {stats['skipped_dup']}")
    print(f"跳过-推理异常: {stats['infer_errors']}")
    print(f"背景图数（无手，不写 label）: {stats['background']}")
    print("各类别标注数:")
    for name in CLASS_NAMES:
        print(f"  {name}: {stats['class_counts'][name]}")
    print(f"数据集目录: {out_dir.resolve()}")
    print(f"抽检图目录: {qa_dir.resolve()}")


if __name__ == "__main__":
    main()
