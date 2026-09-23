# BFRB 手脸接触线索检测模块

本模块属于研究课题「基于多传感器融合的 BFRBs（身体聚焦重复行为）智能检测系统研究」
的**视觉传感器通道**。多传感器融合的整体叙事为：

- **视觉通道（本模块，已落地）**：摄像头/图片/视频 → 面部表情（emotion.pt）
  + 躯体行为线索（手脸接触规则 + 皮损痕迹检测）；
- **腕戴传感器通道（扩展方向）**：腕戴 IMU/热电堆/ToF 时序信号 → BFRB 手势识别，
  公开依据见 Kaggle CMI「Detect Behavior with Sensor Data」数据集；
- **融合层**：当前为视觉多通道线索的规则级并行输出，决策级/特征级融合为后续工作。

「心安动识」BFRB（Body-Focused Repetitive Behaviors，身体聚焦重复行为）方向的躯体动作识别方案一：
**face + hand YOLO 检测 + 手脸接触几何规则判定**。输出"觉察线索"，不做医学诊断。

## 背景与选型

调研结论：GitHub / HuggingFace / Kaggle / Roboflow 上均不存在开源的"躯体动作→情绪/BFRB"
YOLO 预训练权重；学界（如拔毛癖识别研究）也普遍自采数据自训。本模块是业界开源 BFRB 视觉项目
（calm-hands、nail-biting-detection-app 等）实际采用路线的 YOLO 化实现：先检测手与脸，
再用几何规则判定"手脸接触/贴近"这一 BFRB 行为的核心线索。

## 权重来源

均来自 HuggingFace 公开仓库（免登录）：
[poptoz/yolo26-hand-pose-face-detection](https://huggingface.co/poptoz/yolo26-hand-pose-face-detection)

| 文件 | 任务 | 类别 | 训练数据 |
| --- | --- | --- | --- |
| `weights/yolo26_hand_pose.pt` | pose | `{0: 'hand'}`，每只手 21 个关键点（0=手腕，4/8/12/16/20=五指指尖） | Ultralytics Hand Keypoints 数据集 |
| `weights/yolo26_face.pt` | detect | `{0: 'face'}` | WiderFace |

项目原有的 `weights/emotion.pt`（面部 4 类表情 angry/happy/neutral/sad）不受影响，
两个通道（表情 + 躯体）可并行使用，构成"多智能体协作"中的躯体通道。

## 几何规则（`bfrb_detect.py`）

1. 每只手框与所有人脸框计算 IoU，并取手框中心/可见指尖到脸框的最近距离，
   用脸框对角线长度归一化；
2. `IoU > 0.02` 或归一化距离 `< 0.5` → 判定"手脸接触/贴近"线索；
3. 接触点区域划分：
   - 脸框上缘及以上 → 拔头发/抓挠头皮类
   - 脸框下 1/3（嘴部估计区域）→ 咬指甲/进食类
   - 脸框上 1/3（眼部估计区域）→ 抠皮肤/揉眼类
   - 其余 → 手摸脸（一般线索）
4. 输出温柔口吻觉察文案（看见线索，不贴标签）。

## 接口

```
POST /predictBfrb
multipart/form-data:
  file        图片文件（必填）
  weight      手部模型文件名，默认 yolo26_hand_pose.pt（仅允许 weights/ 下纯文件名）
  face_weight 人脸模型文件名，默认 yolo26_face.pt
  conf        置信度阈值，默认 0.3
```

返回示例：

```json
{
  "status": 200,
  "bfrbCue": true,
  "faces": [{"bbox": [x1, y1, x2, y2], "confidence": 0.93}],
  "hands": [{"bbox": [...], "confidence": 0.81}],
  "contacts": [{"iou": 0.2, "center_distance_norm": 0.0,
                "contact_point": [190.0, 260.0], "region": "mouth",
                "cue_type": "咬指甲/进食类", "hand_index": 0, "face_index": 0}],
  "awarenessText": "画面里手部靠近了嘴部区域，这可能是一个值得留意的身体信号……"
}
```

检测器在服务内懒加载并缓存（`facetry.py` 的 `get_bfrb_detector`），避免每次请求重复加载模型。

## 已验证

- 规则层单元测试：嘴部/头发/眼部/远距离四场景判定正确。
- 真实图片端到端：`test-samples/sad-happy-face.jpg`（有脸无手）→ 正确识别 2 张脸、无接触线索。
- `/predictImg`（emotion.pt）回归通过，表情通道不受影响。

## 方案二：Roboflow 数据集微调（已完成首版训练）

实际执行后发现：调研阶段预期的"nail-biting / skin-picking / hair-pulling 行为数据集"
在 Roboflow Universe 上并不存在可直接下载的版本。最对口的可用数据集为
[test-self-harm/self-harm v1](https://universe.roboflow.com/test-self-harm/self-harm)
（CC BY 4.0），经逐张抽验，其标注框全部指向**皮肤伤口/疤痕/皮损区域**而非行为动作，
因此如实合并为单一类别 `self-harm-wound`，作为抠皮肤类 BFRB **后果痕迹**的检测代理
（行为检测由方案一的手脸接触规则承担，两路互补）。

- 数据集：`datasets/self-harm/`（YOLOv8 格式，train/valid/test 划分，类别重映射脚本
  `datasets/remap_single_class.py`，抽验图在 `datasets/qa/`）；
- 训练：`train_bfrb.py`，基座 yolo11n.pt，RTX 4050 Laptop GPU，
  内置增广（mosaic/mixup/HSV/旋转/缩放/翻转 + close_mosaic）；
- 指标（valid 集 58 张 / 60 实例）：P=0.606，R=0.487，mAP50=0.519，mAP50-95=0.204
  ——小数据集首版结果，仅作基线；
- 权重：`weights/bfrb_wound.pt`，Flask 各接口可通过 `weight` 参数加载；
- 推理验证样例：`datasets/qa/infer_bfrb_wound.jpg`。

复训命令：

```bash
D:/software/anaconda3/envs/pytorch/python.exe train_bfrb.py --data datasets/self-harm/data.yaml
```

## 方案三：BFRB 行为直检模型（bfrb_behavior.pt，当前核心模型）

真正的 BFRB **行为**检测模型：单个 YOLO 直接输出 5 类行为框，
不再依赖手脸几何规则（规则模式仍保留，接口按权重类别名自动切换）。

### 类别

`nail_biting`（咬指甲/进食类）、`skin_picking`（抠皮肤/揉眼类）、
`hair_pulling`（拔头发/抓挠头皮类）、`face_touching`（摸脸）、
`hand_normal`（手部自然状态，作负例）。

### 数据来源与伪标注流水线

公开渠道不存在 BFRB 行为标注数据集，本模型数据来自两路，均经
`pseudo_label_pipeline.py`（视频/图片 → 抽帧 → 手脸模型自动打标 → YOLO 格式）生成：

1. **FaceTouch 公开数据集**（PLOS ONE 2023, doi:10.1371/journal.pone.0288670，
   `datasets/raw/facetouch_dataset/`）：手-脸接触图像 9337 张，伪标注后提供
   nail_biting 697 框等（多人群多场景，保证多样性）；
2. **自录动作视频**（`datasets/raw/videos/`，10 段，覆盖全部 5 类）：
   抽帧 830 张有效帧，补齐 FaceTouch 空缺的 hair_pulling / skin_picking /
   face_touching / hand_normal。

合并：`merge_datasets.py`（类别对齐、背景图比例上限 0.3）→ `datasets/bfrb_merged/`
（train 1116 标注 + 478 背景，val 263 标注 + 112 背景）。

### 训练与指标

`train_bfrb.py --data datasets/bfrb_merged/data.yaml`（yolo11n 基座，50 轮，RTX 4050）：

| 评估集 | P | R | mAP50 | mAP50-95 |
| --- | --- | --- | --- | --- |
| 合并集 val（同源） | 0.731 | 0.693 | 0.744 | 0.553 |
| FaceTouch 独立测试集（跨域） | 0.197 | 0.263 | 0.212 | 0.146 |

分类别（合并集 val）：nail_biting 0.871 / skin_picking 0.796 / hair_pulling 0.949 /
face_touching 0.630 / hand_normal 0.474（mAP50）。
跨域指标明显偏低属预期（FaceTouch 为陌生人低清监控风格图像，与自录数据域差大），
提升路径是持续补充多样数据迭代，见"后续衔接"。

### 接口（双模式）

`POST /predictBfrb` 传 `weight=bfrb_behavior.pt` 即走直检模式，返回：

```json
{"mode": "behavior", "bfrbCue": true,
 "detections": [{"bbox": [...], "confidence": 0.895,
                 "behavior": "skin_picking", "cue_type": "抠皮肤/揉眼类"}],
 "awarenessText": "画面里留意到「抠皮肤/揉眼类」的身体信号……"}
```

不传 `weight`（默认 yolo26_hand_pose.pt）则走方案一几何规则模式（`mode=geometry`）。
两种模式均已 curl 实测通过；推理可视化样例在 `datasets/qa/behavior_*.jpg`。

### 迭代指南

1. 新素材（自录视频/图片）放入 `datasets/raw/`，跑
   `pseudo_label_pipeline.py --input <素材> --output <新数据集> --blur 1.0`
   （手机压缩视频必须加 `--blur 1.0`，默认阈值 50 会误杀软画质帧）；
2. 抽检 `qa/` 目录修正误标（或导入 Roboflow 在线修标）；
3. `merge_datasets.py` 合并新旧数据集后重跑 `train_bfrb.py`；
4. 伪标注已知噪声：过渡帧（手在空中未触脸）可能被误标为行为类，
   数据量上来后可收紧 `bfrb_detect.py` 的 `DIST_THRESHOLD` 重标。

## 后续衔接

1. 继续扩充 BFRB 行为数据（不同人、光线、角度），迭代 `bfrb_behavior.pt`；
2. 更远的情绪层路线：YOLOv11-pose 骨架关键点 + 时序分类器（LSTM/Transformer），
   情绪数据可参考 BoLD 躯体语言数据集（需申请）；
3. 腕戴传感器通道（IMU 时序手势识别）：Kaggle CMI 数据集，待前后端闭环后接入。
