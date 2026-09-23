# -*- coding: utf-8 -*-
"""
合并多个同类别体系的 YOLO 数据集（如 FaceTouch 伪标注 + 自录视频伪标注）。

用法：
    python merge_datasets.py --inputs datasets/bfrb_behavior_v1 datasets/bfrb_selfrecord \
        --output datasets/bfrb_merged [--max-bg-ratio 0.3]

说明：
- 各输入数据集需有 data.yaml 且类别顺序一致（本流水线产出的均一致）；
- 文件名加来源前缀避免冲突；train/val 分别合并；
- --max-bg-ratio 控制背景图（无 label 的图片）占比上限，防止背景过多压低召回。
"""

import argparse
import random
import shutil
from pathlib import Path

import yaml


def collect_split(ds_dir, split):
    img_dir = ds_dir / 'images' / split
    lbl_dir = ds_dir / 'labels' / split
    pairs, backgrounds = [], []
    if not img_dir.exists():
        return pairs, backgrounds
    for img in sorted(img_dir.iterdir()):
        lbl = lbl_dir / (img.stem + '.txt')
        if lbl.exists() and lbl.stat().st_size > 0:
            pairs.append((img, lbl))
        else:
            backgrounds.append(img)
    return pairs, backgrounds


def main():
    parser = argparse.ArgumentParser(description='合并 YOLO 数据集')
    parser.add_argument('--inputs', nargs='+', required=True, help='数据集目录列表')
    parser.add_argument('--output', required=True, help='合并输出目录')
    parser.add_argument('--max-bg-ratio', type=float, default=0.3,
                        help='背景图占总图数的比例上限（默认 0.3）')
    args = parser.parse_args()

    out = Path(args.output)
    names = None
    for ds in args.inputs:
        with open(Path(ds) / 'data.yaml', encoding='utf-8') as f:
            data = yaml.safe_load(f)
        ds_names = [data['names'][i] for i in sorted(data['names'])]
        if names is None:
            names = ds_names
        elif names != ds_names:
            raise SystemExit(f'类别不一致：{ds} -> {ds_names} != {names}')

    rng = random.Random(42)
    stats = {}
    for split in ['train', 'val']:
        all_pairs, all_bgs = [], []
        for ds in args.inputs:
            prefix = Path(ds).name
            pairs, bgs = collect_split(Path(ds), split)
            all_pairs += [(prefix, img, lbl) for img, lbl in pairs]
            all_bgs += [(prefix, img) for img in bgs]

        # 背景图按比例上限抽样子集
        max_bg = int(len(all_pairs) * args.max_bg_ratio / max(1.0 - args.max_bg_ratio, 1e-6))
        if len(all_bgs) > max_bg:
            all_bgs = rng.sample(all_bgs, max_bg)

        img_out = out / 'images' / split
        lbl_out = out / 'labels' / split
        img_out.mkdir(parents=True, exist_ok=True)
        lbl_out.mkdir(parents=True, exist_ok=True)

        for prefix, img, lbl in all_pairs:
            dst = img_out / f'{prefix}_{img.name}'
            shutil.copy2(img, dst)
            shutil.copy2(lbl, lbl_out / f'{prefix}_{img.stem}.txt')
        for prefix, img in all_bgs:
            shutil.copy2(img, img_out / f'{prefix}_{img.name}')

        stats[split] = (len(all_pairs), len(all_bgs))

    data_yaml = {
        'path': str(out.resolve()),
        'train': 'images/train',
        'val': 'images/val',
        'nc': len(names),
        'names': {i: n for i, n in enumerate(names)},
    }
    with open(out / 'data.yaml', 'w', encoding='utf-8') as f:
        yaml.safe_dump(data_yaml, f, allow_unicode=True, sort_keys=False)

    print('合并完成：', out)
    for split, (n_labeled, n_bg) in stats.items():
        print(f'  {split}: 带标注 {n_labeled} 张，背景图 {n_bg} 张')
    print('  类别：', names)


if __name__ == '__main__':
    main()
