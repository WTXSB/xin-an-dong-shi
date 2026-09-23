# -*- coding: utf-8 -*-
"""
BFRB 行为检测模型微调训练脚本（方案二）。

用法：
    python train_bfrb.py --data <数据集目录>/data.yaml

数据集：Roboflow Universe 下载的 YOLO 格式数据集（含 nail-biting / skin-picking /
hair-pulling 等类别），目录内需有 data.yaml（train/val 路径与类别名）。

基座：yolo11n.pt（ultralytics 官方预训练），在 RTX 4050 Laptop (6GB) 上可训练。
数据增广：依赖 ultralytics 内置增广（mosaic / hsv / 翻转 / 旋转 / 缩放 / mixup），
其中 close_mosaic 让最后 10 个 epoch 关闭 mosaic，提升收敛质量。

训练产物在 runs/bfrb/ 下，最优权重为 runs/bfrb/train/weights/best.pt，
确认效果后可复制为 weights/bfrb_behavior.pt 供 Flask 服务加载。
"""

import argparse

from ultralytics import YOLO


def main():
    parser = argparse.ArgumentParser(description='BFRB 行为检测 YOLO 微调训练')
    parser.add_argument('--data', required=True, help='数据集 data.yaml 路径')
    parser.add_argument('--base', default='yolo11n.pt', help='基座权重（默认 yolo11n.pt）')
    parser.add_argument('--epochs', type=int, default=50, help='训练轮数')
    parser.add_argument('--imgsz', type=int, default=640, help='输入尺寸')
    parser.add_argument('--batch', type=int, default=16, help='批大小（6GB 显存建议 8~16）')
    args = parser.parse_args()

    model = YOLO(args.base)
    model.train(
        data=args.data,
        epochs=args.epochs,
        batch=args.batch,
        imgsz=args.imgsz,
        amp=True,                # 混合精度；若出现 loss=NaN 改为 False
        project='runs/bfrb',
        name='train',
        exist_ok=True,
        # ---- 数据增广（对自采/小数据集尤其重要）----
        hsv_h=0.015,             # 色相抖动
        hsv_s=0.6,               # 饱和度抖动
        hsv_v=0.4,               # 明度抖动
        degrees=10.0,            # 随机旋转
        translate=0.1,           # 随机平移
        scale=0.4,               # 随机缩放
        fliplr=0.5,              # 水平翻转
        mosaic=1.0,              # mosaic 拼接
        mixup=0.1,               # mixup 混合
        close_mosaic=10,         # 最后 10 轮关闭 mosaic
    )
    # 训练完自动在 val 集上评估一次，输出 mAP50 / mAP50-95
    model.val()
    print('训练完成，最优权重：runs/bfrb/train/weights/best.pt')


if __name__ == '__main__':
    main()
