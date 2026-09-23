# 「心安动识」Web后端技术架构与YOLO模型嵌入方案

## Backend Architecture & YOLO Model Integration Plan

> **版本**: V1.0  
> **日期**: 2026年6月  
> **技术栈**: Python 3.11 + FastAPI + PyTorch + PostgreSQL + Redis + Docker

---

## 目录

1. [整体架构概览](#1-整体架构概览)
2. [技术选型与理由](#2-技术选型与理由)
3. [项目目录结构](#3-项目目录结构)
4. [YOLO模型嵌入方案](#4-yolo模型嵌入方案)
5. [核心API设计](#5-核心api设计)
6. [数据库设计](#6-数据库设计)
7. [实时检测方案](#7-实时检测方案)
8. [部署与运维](#8-部署与运维)
9. [开发环境搭建指南](#9-开发环境搭建指南)
10. [性能优化策略](#10-性能优化策略)

---

## 1. 整体架构概览

### 1.1 系统架构图

```
                          ┌──────────────────────┐
                          │     Nginx (反向代理)    │
                          │  - SSL终止             │
                          │  - 负载均衡             │
                          │  - 静态资源             │
                          └──────────┬───────────┘
                                     │
                          ┌──────────┴───────────┐
                          │    API Gateway        │
                          │    (FastAPI 内置)      │
                          │  - 路由分发            │
                          │  - 请求限流            │
                          │  - CORS中间件          │
                          └──────────┬───────────┘
                                     │
          ┌──────────────────────────┼──────────────────────────┐
          │                          │                          │
          ▼                          ▼                          ▼
┌─────────────────┐    ┌─────────────────────┐    ┌─────────────────┐
│   核心业务服务    │    │   AI推理服务         │    │   异步任务服务   │
│   (FastAPI)     │    │   (FastAPI)         │    │   (Celery)      │
│                 │    │                     │    │                 │
│ ·用户认证/管理   │    │ ·YOLOv8 模型加载     │    │ ·视频处理        │
│ ·行为数据CRUD   │    │ ·实时推理接口        │    │ ·批量检测        │
│ ·情绪日记       │    │ ·批量图片检测        │    │ ·报告生成        │
│ ·疗愈内容       │    │ ·模型版本管理        │    │ ·数据导出        │
│ ·社区功能       │    │ ·推理结果缓存        │    │ ·推送通知        │
└────────┬────────┘    └──────────┬──────────┘    └────────┬────────┘
         │                        │                        │
         └────────────────────────┼────────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    │                           │
                    ▼                           ▼
          ┌───────────────┐           ┌───────────────┐
          │  PostgreSQL   │           │    Redis       │
          │  (主数据库)    │           │   (缓存/队列)   │
          │               │           │               │
          │ ·用户表       │           │ ·推理结果缓存   │
          │ ·行为记录表    │           │ ·会话管理      │
          │ ·检测结果表    │           │ ·Celery Broker │
          │ ·日记/内容表   │           │ ·实时计数      │
          └───────────────┘           └───────────────┘
                    │
                    ▼
          ┌───────────────┐
          │  MinIO / OSS  │
          │  (对象存储)    │
          │               │
          │ ·模型权重文件  │
          │ ·上传视频/图片 │
          │ ·报告文件     │
          │ ·静态资源     │
          └───────────────┘
```

### 1.2 为什么选择FastAPI？

| 考量维度        | FastAPI              | Django        | Flask      | Spring Boot        |
| --------------- | -------------------- | ------------- | ---------- | ------------------ |
| 异步支持        | ⭐⭐⭐⭐⭐ 原生async | ⭐⭐⭐ 需配置 | ⭐⭐ 有限  | ⭐⭐⭐⭐ WebFlux   |
| API文档自动生成 | ⭐⭐⭐⭐⭐ OpenAPI   | ⭐⭐⭐ DRF    | ⭐ 需扩展  | ⭐⭐⭐ Swagger     |
| AI/ML集成便利性 | ⭐⭐⭐⭐⭐           | ⭐⭐⭐        | ⭐⭐⭐⭐   | ⭐⭐               |
| 学习曲线        | ⭐⭐⭐⭐             | ⭐⭐⭐        | ⭐⭐⭐⭐⭐ | ⭐⭐⭐             |
| 性能            | ⭐⭐⭐⭐⭐           | ⭐⭐⭐        | ⭐⭐⭐     | ⭐⭐⭐⭐           |
| 适合本项目      | ✅ **最推荐**        | 偏重          | 适中型项目 | 团队Java经验多时选 |

**结论**：FastAPI 是AI推理服务的最佳搭档——原生异步、自动API文档、与PyTorch生态无缝集成。

---

## 2. 技术选型与理由

### 2.1 技术栈总表

| 层级           | 技术                            | 版本        | 选型理由                                |
| -------------- | ------------------------------- | ----------- | --------------------------------------- |
| **Web框架**    | FastAPI                         | ≥0.110      | 异步高性能、自动OpenAPI文档、Python原生 |
| **ASGI服务器** | Uvicorn + Gunicorn              | ≥0.27       | 生产级ASGI服务器，多worker管理          |
| **AI推理**     | PyTorch + Ultralytics           | ≥2.0 + ≥8.0 | YOLOv8官方框架，GPU加速                 |
| **数据库**     | PostgreSQL                      | ≥15         | 成熟的关系型数据库，支持JSON字段        |
| **缓存**       | Redis                           | ≥7.0        | 高性能缓存 + Celery消息队列             |
| **对象存储**   | MinIO (本地) / 阿里云OSS (生产) | —           | S3兼容，存储模型与媒体文件              |
| **任务队列**   | Celery + Redis                  | ≥5.3        | 异步视频处理与批量检测                  |
| **ORM**        | SQLAlchemy 2.0 + Alembic        | ≥2.0        | 异步ORM + 数据库迁移                    |
| **容器化**     | Docker + Docker Compose         | —           | 环境一致性，一键部署                    |
| **CI/CD**      | GitHub Actions                  | —           | 自动化测试与部署                        |
| **监控**       | Prometheus + Grafana            | —           | 服务监控与告警                          |
| **日志**       | Loguru + ELK (可选)             | —           | 结构化日志                              |

### 2.2 依赖清单 (requirements.txt)

```txt
# Web Framework
fastapi==0.111.0
uvicorn[standard]==0.30.1
gunicorn==22.0.0
python-multipart==0.0.9

# AI / ML
torch==2.3.1
torchvision==0.18.1
ultralytics==8.2.28
onnx==1.16.0
onnxruntime-gpu==1.18.0   # GPU推理加速（有GPU时）
# onnxruntime==1.18.0      # CPU推理（无GPU时）
opencv-python-headless==4.10.0.84
numpy==1.26.4
Pillow==10.3.0

# Database
sqlalchemy[asyncio]==2.0.30
asyncpg==0.29.0            # PostgreSQL异步驱动
alembic==1.13.1
psycopg2-binary==2.9.9     # Celery同步用

# Cache & Queue
redis==5.0.7
celery[redis]==5.4.0

# Object Storage
minio==7.2.7
boto3==1.34.122            # AWS S3 / 阿里云OSS SDK

# Auth
python-jose[cryptography]==3.3.0
passlib[bcrypt]==1.7.4
python-multipart==0.0.9

# Validation
pydantic==2.7.3
pydantic-settings==2.3.2
email-validator==2.1.1

# Monitoring
prometheus-fastapi-instrumentator==7.0.0

# Utils
loguru==0.7.2
httpx==0.27.0
python-dotenv==1.0.1
```

---

## 3. 项目目录结构

```
mind-ease-backend/
│
├── app/                          # 主应用目录
│   ├── __init__.py
│   ├── main.py                   # FastAPI应用入口
│   ├── config.py                 # 配置管理（环境变量 + Pydantic Settings）
│   │
│   ├── api/                      # API路由层
│   │   ├── __init__.py
│   │   ├── deps.py               # 依赖注入（DB Session, Current User等）
│   │   ├── v1/                   # API V1 版本
│   │   │   ├── __init__.py
│   │   │   ├── router.py         # V1 路由汇总
│   │   │   ├── auth.py           # 认证相关API（注册/登录/Token）
│   │   │   ├── users.py          # 用户管理API
│   │   │   ├── detect.py         # 🔥 行为检测API（YOLO推理核心）
│   │   │   ├── results.py        # 检测结果查询API
│   │   │   ├── diary.py          # 情绪日记API
│   │   │   ├── therapy.py        # 疗愈内容API
│   │   │   ├── community.py      # 社区API
│   │   │   └── dashboard.py      # 数据仪表盘API
│   │   └── websocket/            # WebSocket路由
│   │       ├── __init__.py
│   │       └── realtime_detect.py # 🔥 实时检测WebSocket
│   │
│   ├── models/                   # SQLAlchemy 数据模型
│   │   ├── __init__.py
│   │   ├── base.py               # 基础模型类
│   │   ├── user.py               # 用户模型
│   │   ├── behavior_record.py    # 行为记录模型
│   │   ├── detection_result.py   # 检测结果模型
│   │   ├── diary.py              # 日记模型
│   │   └── therapy.py            # 疗愈内容模型
│   │
│   ├── schemas/                  # Pydantic 请求/响应模型
│   │   ├── __init__.py
│   │   ├── auth.py
│   │   ├── user.py
│   │   ├── detect.py             # 检测相关Schema
│   │   └── common.py             # 通用Schema（分页等）
│   │
│   ├── services/                 # 业务逻辑层
│   │   ├── __init__.py
│   │   ├── auth_service.py       # 认证服务
│   │   ├── detect_service.py     # 🔥 检测服务（调用YOLO模型）
│   │   ├── result_service.py     # 结果分析服务
│   │   └── therapy_service.py    # 疗愈推荐服务
│   │
│   ├── ml/                       # 🔥 机器学习模块
│   │   ├── __init__.py
│   │   ├── model_manager.py      # 模型加载/管理/版本控制
│   │   ├── yolo_detector.py      # YOLOv8 检测器封装
│   │   ├── preprocessor.py       # 图像/视频预处理
│   │   ├── postprocessor.py      # 检测结果后处理（NMS/分类映射）
│   │   └── behavior_classifier.py # 行为分类器（必要时辅助模型）
│   │
│   ├── core/                     # 核心工具
│   │   ├── __init__.py
│   │   ├── security.py           # JWT/密码哈希
│   │   ├── database.py           # 数据库连接管理
│   │   ├── redis_client.py       # Redis客户端
│   │   └── storage.py            # 对象存储工具
│   │
│   └── utils/                    # 通用工具
│       ├── __init__.py
│       ├── image_utils.py        # 图片处理工具
│       ├── video_utils.py        # 视频处理工具
│       └── response_utils.py     # 统一响应格式
│
├── alembic/                      # 数据库迁移
│   ├── versions/
│   └── env.py
│
├── models/                       # YOLO模型权重文件目录
│   ├── yolov8n_bfrb.pt           # Fine-tuned YOLOv8n 模型
│   └── README.md                 # 模型版本说明
│
├── tests/                        # 测试目录
│   ├── __init__.py
│   ├── conftest.py               # Pytest fixtures
│   ├── test_api/
│   └── test_ml/
│
├── scripts/                      # 工具脚本
│   ├── init_db.py                # 初始化数据库
│   ├── seed_data.py              # 填充测试数据
│   └── export_model.py           # 模型导出脚本（.pt → .onnx）
│
├── docker/                       # Docker相关配置
│   ├── Dockerfile
│   ├── Dockerfile.gpu            # GPU版本Dockerfile
│   └── docker-compose.yml
│
├── .env.example                  # 环境变量模板
├── .gitignore
├── alembic.ini
├── requirements.txt
├── requirements-dev.txt
└── README.md
```

---

## 4. YOLO模型嵌入方案

### 4.1 模型管理器 (model_manager.py)

```python
"""
模型管理器：负责YOLO模型的加载、缓存、版本管理和推理调度
"""

import os
from pathlib import Path
from typing import Optional, Dict
from dataclasses import dataclass
import torch
from ultralytics import YOLO
from loguru import logger


@dataclass
class ModelInfo:
    """模型元信息"""
    name: str
    version: str
    path: Path
    device: str
    input_size: tuple  # (width, height)
    classes: list[str]
    is_loaded: bool = False


class ModelManager:
    """
    YOLO模型管理器（单例模式）

    职责：
    1. 模型加载与卸载
    2. 多版本模型管理
    3. GPU/CPU自动切换
    4. 模型预热
    """

    _instance = None
    _models: Dict[str, YOLO] = {}

    # BFRBs 行为类别映射
    BFRB_CLASSES = [
        "nail_biting",          # 0: 咬指甲
        "finger_biting",        # 1: 咬手指
        "hair_pulling",         # 2: 拔头发
        "skin_picking",         # 3: 抓挠皮肤
        "finger_picking",       # 4: 抠手指
        "lip_chewing",          # 5: 咀嚼嘴唇
        "nail_edge_tearing",    # 6: 撕扯指甲边缘
        "face_touching_repeated", # 7: 反复触摸面部
        # 非BFRBs日常动作
        "drinking",             # 8: 喝水
        "combing_hair",         # 9: 梳头
        "typing",               # 10: 打字
        "writing",              # 11: 写字
        "phone_call",           # 12: 打电话
        "face_touch_normal",    # 13: 摸脸(正常)
        "chin_resting",         # 14: 托腮
        "page_turning",         # 15: 翻书
        "fist_clenching",       # 16: 握拳
        "hand_hanging",         # 17: 自然垂手
    ]

    # BFRBs 类别索引 (0-7)
    BFRB_INDICES = set(range(8))

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        if not hasattr(self, 'initialized'):
            self.model_dir = Path(os.getenv("MODEL_DIR", "./models"))
            self.device = self._detect_device()
            self.default_model_name = os.getenv("DEFAULT_MODEL", "yolov8n_bfrb")
            self.initialized = True
            logger.info(f"ModelManager initialized. Device: {self.device}")

    def _detect_device(self) -> str:
        """自动检测可用设备"""
        if torch.cuda.is_available():
            gpu_count = torch.cuda.device_count()
            device = "cuda:0"
            logger.info(f"CUDA available. Using {device} ({gpu_count} GPUs)")
        elif torch.backends.mps.is_available():
            device = "mps"
            logger.info("Apple MPS available. Using MPS.")
        else:
            device = "cpu"
            logger.warning("No GPU detected. Using CPU (inference will be slower).")
        return device

    def load_model(self, model_name: Optional[str] = None) -> YOLO:
        """
        加载YOLO模型（带缓存）

        Args:
            model_name: 模型文件名（不含扩展名），默认使用配置的默认模型

        Returns:
            YOLO模型实例
        """
        model_name = model_name or self.default_model_name

        if model_name in self._models:
            logger.debug(f"Model '{model_name}' already loaded, using cached.")
            return self._models[model_name]

        model_path = self.model_dir / f"{model_name}.pt"

        if not model_path.exists():
            raise FileNotFoundError(
                f"Model file not found: {model_path}. "
                f"Please place your fine-tuned YOLO model in the {self.model_dir} directory."
            )

        logger.info(f"Loading model from {model_path} on {self.device}...")

        # 加载模型
        model = YOLO(str(model_path))

        # 预热模型（首次推理会触发JIT编译，提前执行避免首次请求延迟）
        logger.info("Warming up model...")
        dummy_input = torch.randn(1, 3, 640, 640).to(self.device)
        model.predict(dummy_input, verbose=False, device=self.device)

        self._models[model_name] = model
        logger.info(f"Model '{model_name}' loaded and warmed up successfully.")

        return model

    def unload_model(self, model_name: str):
        """卸载模型释放显存"""
        if model_name in self._models:
            del self._models[model_name]
            torch.cuda.empty_cache()
            logger.info(f"Model '{model_name}' unloaded, cache cleared.")

    def get_model_info(self) -> ModelInfo:
        """获取当前模型信息"""
        return ModelInfo(
            name=self.default_model_name,
            version="1.0.0",
            path=self.model_dir / f"{self.default_model_name}.pt",
            device=self.device,
            input_size=(640, 640),
            classes=self.BFRB_CLASSES,
            is_loaded=self.default_model_name in self._models,
        )

    def get_device(self) -> str:
        return self.device


# 全局单例
model_manager = ModelManager()
```

### 4.2 YOLO检测器 (yolo_detector.py)

```python
"""
YOLO检测器：封装YOLO推理逻辑，提供统一的检测接口
"""

import time
from typing import Optional, List, Tuple
import numpy as np
from PIL import Image
import cv2
from ultralytics import YOLO
from ultralytics.engine.results import Results
from loguru import logger

from app.ml.model_manager import model_manager
from app.ml.preprocessor import ImagePreprocessor
from app.ml.postprocessor import DetectionPostprocessor


class YOLODetector:
    """
    YOLOv8 行为检测器

    支持：
    - 单张图片检测
    - 批量图片检测
    - 视频帧检测
    - 置信度过滤
    - NMS后处理
    """

    def __init__(self, conf_threshold: float = 0.5, iou_threshold: float = 0.45):
        self.model: Optional[YOLO] = None
        self.conf_threshold = conf_threshold
        self.iou_threshold = iou_threshold
        self.preprocessor = ImagePreprocessor(target_size=(640, 640))
        self.postprocessor = DetectionPostprocessor(
            classes=model_manager.BFRB_CLASSES,
            bfrb_indices=model_manager.BFRB_INDICES,
        )

    async def initialize(self):
        """异步初始化：加载模型"""
        if self.model is None:
            # 在线程池中加载模型（避免阻塞事件循环）
            import asyncio
            loop = asyncio.get_event_loop()
            self.model = await loop.run_in_executor(
                None, model_manager.load_model
            )
            logger.info("YOLODetector initialized successfully.")

    async def detect_image(
        self,
        image: np.ndarray,
        conf: Optional[float] = None,
    ) -> dict:
        """
        检测单张图片

        Args:
            image: BGR格式的numpy图片数组 (H, W, 3)
            conf: 置信度阈值，默认使用初始化值

        Returns:
            {
                "detections": [
                    {
                        "class_id": 0,
                        "class_name": "nail_biting",
                        "confidence": 0.94,
                        "bbox": [x1, y1, x2, y2],  # xyxy格式
                        "is_bfrb": True,
                    },
                    ...
                ],
                "inference_time_ms": 23.5,
                "image_size": (640, 480),
            }
        """
        await self.initialize()

        conf = conf or self.conf_threshold

        # 预处理
        processed_image = self.preprocessor.preprocess(image)

        # 推理计时
        start_time = time.time()

        # YOLO推理
        results: List[Results] = self.model.predict(
            source=processed_image,
            conf=conf,
            iou=self.iou_threshold,
            device=model_manager.get_device(),
            verbose=False,
        )

        inference_time = (time.time() - start_time) * 1000  # 转为毫秒

        # 后处理
        detections = self.postprocessor.process(results[0])

        return {
            "detections": detections,
            "inference_time_ms": round(inference_time, 2),
            "image_size": (image.shape[1], image.shape[0]),
        }

    async def detect_batch(
        self,
        images: List[np.ndarray],
        conf: Optional[float] = None,
    ) -> List[dict]:
        """批量检测多张图片"""
        await self.initialize()

        conf = conf or self.conf_threshold

        results_list = []
        for image in images:
            result = await self.detect_image(image, conf)
            results_list.append(result)

        return results_list

    async def detect_video_frame(
        self,
        frame: np.ndarray,
        frame_index: int,
        conf: Optional[float] = None,
    ) -> dict:
        """
        检测视频帧（用于实时视频流和离线视频处理）

        相比detect_image，增加了帧索引追踪
        """
        result = await self.detect_image(frame, conf)
        result["frame_index"] = frame_index
        return result


# 全局检测器单例
detector = YOLODetector()
```

### 4.3 图像预处理 (preprocessor.py)

```python
"""
图像预处理模块：YOLO模型的输入预处理
"""

import numpy as np
import cv2
from typing import Tuple


class ImagePreprocessor:
    """
    图像预处理器

    处理流程：
    1. BGR → RGB 转换
    2. Resize (保持宽高比 + padding)
    3. 归一化 [0,255] → [0,1]
    4. (可选) 数据增强（仅训练时）
    """

    def __init__(self, target_size: Tuple[int, int] = (640, 640)):
        self.target_size = target_size

    def preprocess(self, image: np.ndarray) -> np.ndarray:
        """
        预处理输入图像

        Args:
            image: BGR格式 (H, W, 3), uint8 [0,255]

        Returns:
            预处理后的RGB图像 (640, 640, 3), uint8
        """
        # 1. BGR → RGB
        image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

        # 2. Letterbox resize（保持宽高比 + 灰边填充）
        image_resized = self._letterbox_resize(image_rgb)

        return image_resized

    def _letterbox_resize(self, image: np.ndarray) -> np.ndarray:
        """
        Letterbox resize：保持宽高比缩放并填充灰边

        YOLOv8内部已处理此步骤，这里保留以便自定义处理
        """
        h, w = image.shape[:2]
        target_w, target_h = self.target_size

        # 计算缩放比例
        scale = min(target_w / w, target_h / h)
        new_w, new_h = int(w * scale), int(h * scale)

        # 缩放
        resized = cv2.resize(image, (new_w, new_h), interpolation=cv2.INTER_LINEAR)

        # 创建目标尺寸的画布并居中放置
        canvas = np.full((target_h, target_w, 3), 114, dtype=np.uint8)  # 灰色填充
        offset_x = (target_w - new_w) // 2
        offset_y = (target_h - new_h) // 2
        canvas[offset_y:offset_y+new_h, offset_x:offset_x+new_w] = resized

        return canvas

    def preprocess_batch(self, images: list[np.ndarray]) -> np.ndarray:
        """批量预处理"""
        return np.stack([self.preprocess(img) for img in images])
```

### 4.4 后处理 (postprocessor.py)

```python
"""
检测结果后处理模块
"""

from typing import List, Dict, Any
import numpy as np
from ultralytics.engine.results import Results, Boxes


class DetectionPostprocessor:
    """
    检测结果后处理器

    功能：
    1. 提取检测框信息
    2. 分类是否为BFRBs行为
    3. 计算异常评分
    4. 格式化输出
    """

    def __init__(self, classes: List[str], bfrb_indices: set):
        self.classes = classes
        self.bfrb_indices = bfrb_indices

    def process(self, result: Results) -> List[Dict[str, Any]]:
        """
        处理YOLO推理结果

        Args:
            result: YOLO推理结果对象

        Returns:
            检测结果列表
        """
        detections = []

        if result.boxes is None:
            return detections

        boxes: Boxes = result.boxes

        for i in range(len(boxes)):
            # 提取边界框 (xyxy格式)
            bbox = boxes.xyxy[i].cpu().numpy().tolist()

            # 置信度
            confidence = float(boxes.conf[i].cpu().numpy())

            # 类别
            class_id = int(boxes.cls[i].cpu().numpy())
            class_name = self.classes[class_id] if class_id < len(self.classes) else "unknown"

            # 判断是否为BFRBs行为
            is_bfrb = class_id in self.bfrb_indices

            # 计算异常评分（基于置信度和是否为BFRBs）
            anomaly_score = self._compute_anomaly_score(confidence, is_bfrb)

            detection = {
                "class_id": class_id,
                "class_name": class_name,
                "confidence": round(confidence, 4),
                "bbox": [round(x, 2) for x in bbox],  # [x1, y1, x2, y2]
                "is_bfrb": is_bfrb,
                "anomaly_score": round(anomaly_score, 4),
            }

            detections.append(detection)

        # 按置信度降序排列
        detections.sort(key=lambda x: x["confidence"], reverse=True)

        return detections

    def _compute_anomaly_score(self, confidence: float, is_bfrb: bool) -> float:
        """
        计算异常评分

        BFRBs行为 × 高置信度 → 高异常评分
        非BFRBs行为 → 低异常评分
        """
        if is_bfrb:
            # BFRBs行为：置信度越高，异常评分越高
            return confidence * 0.7 + 0.3
        else:
            # 非BFRBs行为：低异常评分
            return (1 - confidence) * 0.3
```

### 4.5 检测服务 (detect_service.py)

```python
"""
检测服务：业务逻辑层，整合YOLO检测器与业务需求
"""

from typing import Optional
import numpy as np
from datetime import datetime, timezone
from loguru import logger

from app.ml.yolo_detector import detector
from app.models.detection_result import DetectionResult
from app.core.database import get_db


class DetectService:
    """
    行为检测业务服务

    职责：
    1. 调用YOLO检测器进行推理
    2. 将检测结果持久化到数据库
    3. 触发异常告警逻辑
    4. 维护用户行为统计数据
    """

    def __init__(self):
        self.detector = detector

    async def detect_and_save(
        self,
        user_id: int,
        image: np.ndarray,
        source: str = "web_upload",  # web_upload, app_camera, wearable_device
    ) -> dict:
        """
        执行检测并保存结果

        Args:
            user_id: 用户ID
            image: 输入图片
            source: 数据来源

        Returns:
            {
                "detection_id": "uuid",
                "detections": [...],
                "bfrb_count": 2,
                "max_anomaly_score": 0.94,
                "trigger_alert": True,  # 是否触发告警
                "recommendation": "建议进行3分钟呼吸练习",
            }
        """
        # 1. 执行检测
        result = await self.detector.detect_image(image)

        # 2. 提取BFRBs检测
        bfrb_detections = [d for d in result["detections"] if d["is_bfrb"]]
        bfrb_count = len(bfrb_detections)

        # 3. 计算最高异常评分
        max_anomaly_score = (
            max(d["anomaly_score"] for d in bfrb_detections)
            if bfrb_detections
            else 0.0
        )

        # 4. 判断是否触发告警
        trigger_alert = bfrb_count > 0 and max_anomaly_score > 0.6

        # 5. 生成疗愈建议
        recommendation = self._generate_recommendation(bfrb_detections)

        # 6. 持久化到数据库
        detection_record = DetectionResult(
            user_id=user_id,
            source=source,
            bfrb_count=bfrb_count,
            total_detections=len(result["detections"]),
            max_anomaly_score=max_anomaly_score,
            detections_detail=result["detections"],
            inference_time_ms=result["inference_time_ms"],
            image_size=result["image_size"],
            created_at=datetime.now(timezone.utc),
        )
        # db.add(detection_record) ... (实际使用时需要注入db session)

        logger.info(
            f"Detection complete for user {user_id}: "
            f"{bfrb_count} BFRBs detected, "
            f"max_anomaly={max_anomaly_score:.2f}, "
            f"alert={trigger_alert}"
        )

        return {
            "detection_id": str(detection_record.id) if hasattr(detection_record, 'id') else None,
            "detections": result["detections"],
            "bfrb_count": bfrb_count,
            "total_detections": len(result["detections"]),
            "max_anomaly_score": max_anomaly_score,
            "trigger_alert": trigger_alert,
            "inference_time_ms": result["inference_time_ms"],
            "recommendation": recommendation,
        }

    def _generate_recommendation(self, bfrb_detections: list) -> str:
        """基于检测结果生成个性化建议"""
        if not bfrb_detections:
            return "当前未检测到焦虑躯体化行为，继续保持！"

        # 获取最主要的行为类型
        main_behavior = max(bfrb_detections, key=lambda x: x["confidence"])
        behavior_name = main_behavior["class_name"]

        recommendations = {
            "nail_biting": "建议尝试握拳-放松交替练习，或使用指尖陀螺转移注意力",
            "hair_pulling": "建议将手放在大腿上深呼吸3次，感受身体与椅子的接触",
            "skin_picking": "建议轻轻按摩手指代替抓挠，或涂抹护手霜进行感官替代",
            "finger_picking": "建议进行3分钟呼吸练习，关注吸气与呼气的节奏",
        }

        return recommendations.get(
            behavior_name,
            "检测到焦虑行为，建议暂停当前活动，进行1分钟深呼吸"
        )


# 全局服务单例
detect_service = DetectService()
```

---

## 5. 核心API设计

### 5.1 API路由结构

```
Base URL: /api/v1

认证相关:
  POST   /auth/register           # 用户注册
  POST   /auth/login              # 用户登录
  POST   /auth/refresh            # 刷新Token
  POST   /auth/logout             # 登出

检测相关 (核心):
  POST   /detect/image            # 单张图片检测 🔥
  POST   /detect/batch            # 批量图片检测
  POST   /detect/video            # 视频文件检测（异步）
  GET    /detect/status/{task_id} # 查询异步任务状态
  WS     /ws/realtime-detect      # 实时视频流检测 🔥

检测结果:
  GET    /results                 # 检测历史列表（分页+筛选）
  GET    /results/{id}           # 单条检测详情
  GET    /results/stats          # 行为统计数据
  GET    /results/trends         # 行为趋势数据

用户管理:
  GET    /users/me                # 获取当前用户信息
  PUT    /users/me                # 更新当前用户信息
  GET    /users/me/profile        # 获取情绪健康画像

情绪日记:
  POST   /diary                   # 创建日记
  GET    /diary                   # 日记列表
  GET    /diary/{id}             # 日记详情
  PUT    /diary/{id}             # 更新日记
  DELETE /diary/{id}             # 删除日记

疗愈内容:
  GET    /therapy/recommend       # AI推荐疗愈内容
  GET    /therapy/mindfulness     # 正念冥想列表
  GET    /therapy/cbt             # CBT练习列表
  GET    /therapy/content/{id}   # 内容详情

仪表盘:
  GET    /dashboard/overview      # 概览数据
  GET    /dashboard/weekly-report # 周报

模型管理:
  GET    /models/info             # 模型信息
  GET    /models/health           # 模型健康检查
```

### 5.2 核心API详细设计

#### 5.2.1 图片检测接口

```
POST /api/v1/detect/image

Request:
  Content-Type: multipart/form-data
  Body:
    image: (binary file)  # 图片文件 (jpg/png, ≤10MB)
    conf_threshold: 0.5   # (optional) 置信度阈值
    return_image: false   # (optional) 是否返回标注框的图片

Response 200:
{
  "code": 200,
  "message": "success",
  "data": {
    "detection_id": "d7f3a2b1-...",
    "detections": [
      {
        "class_id": 0,
        "class_name": "nail_biting",
        "confidence": 0.9421,
        "bbox": [120.5, 230.3, 280.7, 410.2],
        "is_bfrb": true,
        "anomaly_score": 0.9595
      }
    ],
    "bfrb_count": 1,
    "total_detections": 1,
    "max_anomaly_score": 0.9595,
    "trigger_alert": true,
    "inference_time_ms": 23.5,
    "recommendation": "建议尝试握拳-放松交替练习，或使用指尖陀螺转移注意力",
    "annotated_image_url": "https://..."  // (仅当return_image=true)
  }
}

Response 400:
{
  "code": 400,
  "message": "No valid image provided",
  "data": null
}
```

#### 5.2.2 实时视频流检测 WebSocket

```
WebSocket: ws://host:8000/ws/realtime-detect

连接参数:
  ?token=<jwt_token>&conf=0.5&fps=5

客户端 → 服务端:
  发送视频帧（JPEG编码的二进制数据）

服务端 → 客户端:
{
  "type": "detection",
  "frame_index": 142,
  "detections": [...],
  "bfrb_count": 1,
  "trigger_alert": false,
  "inference_time_ms": 25.3
}

{
  "type": "alert",
  "message": "连续检测到咬指甲行为超过5分钟",
  "recommendation": "建议暂停当前活动..."
}

{
  "type": "error",
  "message": "Model inference timeout"
}
```

### 5.3 FastAPI路由实现示例 (detect.py)

```python
"""
检测API路由
"""

import io
from typing import Optional
import numpy as np
import cv2
from PIL import Image
from fastapi import APIRouter, UploadFile, File, Depends, Query, HTTPException
from fastapi.responses import JSONResponse

from app.api.deps import get_current_user
from app.schemas.detect import DetectResponse, DetectBatchResponse
from app.services.detect_service import detect_service
from app.models.user import User

router = APIRouter(prefix="/detect", tags=["行为检测"])


@router.post("/image", response_model=DetectResponse)
async def detect_image(
    image: UploadFile = File(..., description="待检测图片 (jpg/png)"),
    conf_threshold: float = Query(0.5, ge=0.1, le=0.9, description="置信度阈值"),
    return_image: bool = Query(False, description="是否返回标注图片"),
    current_user: User = Depends(get_current_user),
):
    """
    单张图片行为检测

    - 支持jpg/png格式
    - 文件大小限制10MB
    - 返回18类行为分类结果
    """
    # 验证文件类型
    if image.content_type not in ["image/jpeg", "image/png", "image/webp"]:
        raise HTTPException(400, "仅支持 JPG/PNG/WebP 格式")

    # 读取图片
    contents = await image.read()

    # 文件大小限制 (10MB)
    if len(contents) > 10 * 1024 * 1024:
        raise HTTPException(400, "图片大小不能超过10MB")

    # 解码图片
    nparr = np.frombuffer(contents, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    if img is None:
        raise HTTPException(400, "无法解析图片文件")

    # 执行检测
    result = await detect_service.detect_and_save(
        user_id=current_user.id,
        image=img,
        source="web_upload",
    )

    # 可选：生成标注图片
    if return_image:
        annotated_img = _draw_boxes(img, result["detections"])
        # 上传到OSS，返回URL
        # annotated_url = await upload_to_oss(annotated_img)
        # result["annotated_image_url"] = annotated_url

    return JSONResponse({
        "code": 200,
        "message": "success",
        "data": result,
    })


@router.post("/batch", response_model=DetectBatchResponse)
async def detect_batch(
    images: list[UploadFile] = File(..., description="批量图片 (最多10张)"),
    current_user: User = Depends(get_current_user),
):
    """
    批量图片检测

    - 一次最多10张图片
    - 返回每张图片的检测结果
    """
    if len(images) > 10:
        raise HTTPException(400, "单次最多检测10张图片")

    results = []
    for image in images:
        contents = await image.read()
        nparr = np.frombuffer(contents, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if img is not None:
            result = await detect_service.detect_and_save(
                user_id=current_user.id,
                image=img,
                source="web_batch",
            )
            results.append(result)

    return JSONResponse({
        "code": 200,
        "message": "success",
        "data": {
            "total": len(results),
            "results": results,
        },
    })


def _draw_boxes(image: np.ndarray, detections: list) -> np.ndarray:
    """在图片上绘制检测框（用于可视化）"""
    img_copy = image.copy()
    for det in detections:
        bbox = det["bbox"]
        x1, y1, x2, y2 = map(int, bbox)
        color = (0, 0, 255) if det["is_bfrb"] else (0, 255, 0)
        cv2.rectangle(img_copy, (x1, y1), (x2, y2), color, 2)
        label = f"{det['class_name']} {det['confidence']:.2f}"
        cv2.putText(img_copy, label, (x1, y1-10),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)
    return img_copy
```

---

## 6. 数据库设计

### 6.1 ER图（核心表）

```
┌──────────────┐       ┌──────────────────┐       ┌──────────────────┐
│    users     │       │  detection_results│       │  behavior_records │
├──────────────┤       ├──────────────────┤       ├──────────────────┤
│ id (PK)      │──┐    │ id (PK)          │       │ id (PK)          │
│ username     │  │    │ user_id (FK)     │──┐    │ user_id (FK)     │
│ email        │  ├───→│ source           │  │    │ date             │
│ password_hash│  │    │ bfrb_count       │  ├───→│ bfrb_total       │
│ role         │  │    │ total_detections │  │    │ behavior_breakdown│
│ created_at   │  │    │ max_anomaly_score│  │    │ anxiety_index    │
│ updated_at   │  │    │ detections_detail│  │    │ created_at       │
└──────────────┘  │    │ inference_time_ms│  │    └──────────────────┘
                  │    │ image_size       │  │
┌──────────────┐  │    │ created_at       │  │    ┌──────────────────┐
│   diaries    │  │    └──────────────────┘  │    │  therapy_records  │
├──────────────┤  │                          │    ├──────────────────┤
│ id (PK)      │  │    ┌──────────────────┐  │    │ id (PK)          │
│ user_id (FK) │──┘    │   alert_logs     │  │    │ user_id (FK)     │──┘
│ content      │       ├──────────────────┤  │    │ therapy_type     │
│ mood_score   │       │ id (PK)          │  │    │ content_id       │
│ ai_analysis  │       │ user_id (FK)     │──┘    │ duration_seconds │
│ created_at   │       │ alert_type       │       │ completed_at     │
└──────────────┘       │ message          │       └──────────────────┘
                       │ is_read          │
                       │ created_at       │
                       └──────────────────┘
```

### 6.2 核心表SQL定义

```sql
-- 用户表
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'user',  -- user, therapist, admin
    avatar_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 检测结果表 (核心)
CREATE TABLE detection_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source VARCHAR(20) NOT NULL,  -- web_upload, app_camera, wearable_device
    bfrb_count INT DEFAULT 0,
    total_detections INT DEFAULT 0,
    max_anomaly_score FLOAT DEFAULT 0.0,
    detections_detail JSONB DEFAULT '[]',
    inference_time_ms FLOAT,
    image_size INT[] DEFAULT '{0,0}',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_detection_user_time ON detection_results(user_id, created_at DESC);

-- 行为日统计表
CREATE TABLE behavior_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    bfrb_total INT DEFAULT 0,
    behavior_breakdown JSONB DEFAULT '{}',  -- {"nail_biting": 5, "hair_pulling": 2}
    anxiety_index FLOAT DEFAULT 0.0,        -- 综合焦虑指数
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, date)
);

-- 情绪日记表
CREATE TABLE diaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    mood_score INT CHECK (mood_score BETWEEN 1 AND 10),
    ai_analysis JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 告警日志表
CREATE TABLE alert_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    detection_id UUID REFERENCES detection_results(id),
    alert_type VARCHAR(30),
    message TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 7. 实时检测方案

### 7.1 方案对比

| 方案                     | 延迟         | 带宽 | 复杂度 | 适用场景       | 推荐       |
| ------------------------ | ------------ | ---- | ------ | -------------- | ---------- |
| **WebSocket + 逐帧发送** | 低 (~30ms)   | 高   | 中     | 实时摄像头检测 | ⭐⭐⭐⭐⭐ |
| HTTP轮询                 | 高 (秒级)    | 中   | 低     | 低频检测场景   | ⭐⭐       |
| WebRTC                   | 极低 (<10ms) | 中   | 高     | 点对点视频通话 | ⭐⭐⭐     |
| MJPEG流                  | 低           | 很高 | 低     | 简单监控展示   | ⭐⭐⭐     |

**推荐方案**：WebSocket + 逐帧发送（平衡性能与复杂度）

### 7.2 WebSocket实时检测流程

```
客户端 (浏览器/APP)                    服务端
      │                                  │
      │──── WS连接 (含JWT认证) ────────→│
      │                                  │ 加载YOLO模型
      │←─── 连接确认 (model_ready) ─────│
      │                                  │
      │──── 发送视频帧 (JPEG二进制) ────→│
      │                                  │ 解码 → 预处理 → YOLO推理
      │←─── 检测结果 (JSON) ────────────│ (23ms)
      │                                  │
      │──── 发送下一帧 ─────────────────→│
      │                                  │ ...
      │                                  │
      │──── 关闭连接 ───────────────────→│
      │                                  │ 清理资源
```

### 7.3 帧率控制策略

```python
# 自适应帧率控制
class AdaptiveFrameRate:
    """
    根据推理速度自适应调整检测帧率

    - 推理快 → 提高检测帧率（最高15fps）
    - 推理慢 → 降低检测帧率（最低2fps）
    - 无BFRBs → 降低帧率节省资源
    - 检测到BFRBs → 提高帧率精细追踪
    """

    def __init__(self):
        self.base_fps = 5
        self.max_fps = 15
        self.min_fps = 2
        self.bfrb_detected = False

    def calculate_fps(self, inference_time_ms: float) -> int:
        max_theoretical = 1000 / inference_time_ms

        if self.bfrb_detected:
            # BFRBs检测中：提高帧率
            return min(int(max_theoretical * 0.7), self.max_fps)
        else:
            # 无BFRBs：降低帧率
            return max(self.min_fps, min(int(max_theoretical * 0.3), self.base_fps))
```

---

## 8. 部署与运维

### 8.1 Docker Compose 一键部署

```yaml
# docker-compose.yml
version: "3.8"

services:
  # Nginx 反向代理
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./docker/nginx.conf:/etc/nginx/nginx.conf
      - ./static:/app/static
    depends_on:
      - backend
    restart: unless-stopped

  # FastAPI 后端服务
  backend:
    build:
      context: .
      dockerfile: docker/Dockerfile
    expose:
      - "8000"
    environment:
      - DATABASE_URL=postgresql+asyncpg://user:pass@postgres:5432/mindease
      - REDIS_URL=redis://redis:6379/0
      - MODEL_DIR=/app/models
      - DEFAULT_MODEL=yolov8n_bfrb
    volumes:
      - ./models:/app/models
      - ./logs:/app/logs
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu] # GPU支持（如有）

  # Celery Worker (异步任务)
  worker:
    build:
      context: .
      dockerfile: docker/Dockerfile
    command: celery -A app.tasks worker --loglevel=info
    environment:
      - DATABASE_URL=postgresql+asyncpg://user:pass@postgres:5432/mindease
      - REDIS_URL=redis://redis:6379/0
    volumes:
      - ./models:/app/models
    depends_on:
      - redis
      - postgres
    restart: unless-stopped

  # PostgreSQL 数据库
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
      POSTGRES_DB: mindease
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U user -d mindease"]
      interval: 5s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # Redis 缓存
  redis:
    image: redis:7-alpine
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # MinIO 对象存储 (开发环境)
  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - minio_data:/data
    restart: unless-stopped

volumes:
  postgres_data:
  minio_data:
```

### 8.2 Dockerfile

```dockerfile
# docker/Dockerfile
FROM python:3.11-slim

WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# 安装Python依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY ./app ./app
COPY ./alembic ./alembic
COPY ./alembic.ini .

# 创建模型目录
RUN mkdir -p /app/models /app/logs

# 暴露端口
EXPOSE 8000

# 启动命令
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4"]
```

---

## 9. 开发环境搭建指南

### 9.1 快速开始

```bash
# 1. 克隆项目
git clone <repo_url> mind-ease-backend
cd mind-ease-backend

# 2. 创建虚拟环境
python -m venv venv
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# 3. 安装依赖
pip install -r requirements.txt
pip install -r requirements-dev.txt  # 开发依赖

# 4. 配置环境变量
cp .env.example .env
# 编辑 .env 填入实际配置

# 5. 放置YOLO模型
# 将训练好的 yolov8n_bfrb.pt 放入 models/ 目录

# 6. 初始化数据库
python scripts/init_db.py

# 7. 启动开发服务器
uvicorn app.main:app --reload --port 8000

# 8. 访问API文档
# 浏览器打开: http://localhost:8000/docs
```

### 9.2 环境变量配置 (.env.example)

```env
# 应用配置
APP_NAME=MindEase
APP_VERSION=1.0.0
DEBUG=true
SECRET_KEY=your-secret-key-change-in-production

# 数据库
DATABASE_URL=postgresql+asyncpg://postgres:postgres@localhost:5432/mindease

# Redis
REDIS_URL=redis://localhost:6379/0

# 模型配置
MODEL_DIR=./models
DEFAULT_MODEL=yolov8n_bfrb
CONF_THRESHOLD=0.5
IOU_THRESHOLD=0.45

# JWT配置
JWT_SECRET_KEY=your-jwt-secret-key
JWT_ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=60

# 对象存储 (MinIO开发环境)
MINIO_ENDPOINT=localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=mindease

# 日志
LOG_LEVEL=DEBUG
```

---

## 10. 性能优化策略

### 10.1 模型推理优化

| 优化项       | 方法                      | 预期提升                       |
| ------------ | ------------------------- | ------------------------------ |
| **模型量化** | INT8量化 (TensorRT/ONNX)  | 推理速度 2-3x ↑，模型大小 4x ↓ |
| **模型剪枝** | 结构化剪枝去除冗余通道    | 推理速度 1.5x ↑，精度损失 <1%  |
| **批处理**   | 合并多请求一次推理        | GPU利用率提升 3-5x             |
| **模型预热** | 启动时预加载+一次前向传播 | 首次请求延迟 -90%              |
| **推理缓存** | 相同/相似图片缓存结果     | 命中率约15-20%                 |

### 10.2 系统级优化

```python
# 1. 使用异步数据库驱动
DATABASE_URL = "postgresql+asyncpg://..."  # asyncpg 比 psycopg2 快 2-3x

# 2. Redis缓存热点数据
@router.get("/results/stats")
async def get_stats(user_id: int):
    cache_key = f"stats:{user_id}"
    cached = await redis.get(cache_key)
    if cached:
        return json.loads(cached)
    # ... 计算统计数据
    await redis.setex(cache_key, 300, json.dumps(stats))  # 5分钟过期
    return stats

# 3. 数据库连接池
engine = create_async_engine(
    DATABASE_URL,
    pool_size=20,        # 连接池大小
    max_overflow=10,     # 最大溢出连接
    pool_pre_ping=True,  # 连接健康检查
)

# 4. 响应压缩
from fastapi.middleware.gzip import GZipMiddleware
app.add_middleware(GZipMiddleware, minimum_size=1000)
```

### 10.3 关键性能指标 (KPI)

| 指标               | 目标值                     | 测量方法             |
| ------------------ | -------------------------- | -------------------- |
| API响应时间 (P50)  | <200ms                     | Prometheus + Grafana |
| API响应时间 (P99)  | <1s                        | Prometheus + Grafana |
| YOLO推理延迟       | <50ms (GPU) / <200ms (CPU) | 内置计时             |
| 并发检测QPS        | ≥20 (单GPU)                | 压力测试             |
| 系统可用性         | ≥99.5%                     | Uptime监控           |
| 模型精度 (mAP@0.5) | ≥92%                       | 定期评估             |

---

## 11. 多传感器融合扩展：BFRBs 智能检测的研究定位

本项目承载的研究课题全称为「基于多传感器融合的 BFRBs（身体聚焦重复行为）智能检测系统研究」。
"多传感器融合"在系统中的通道划分如下：

| 通道 | 传感器 | 检测对象 | 当前状态 |
| --- | --- | --- | --- |
| 视觉-面部 | 摄像头/图片/视频 | 面部表情（angry/happy/neutral/sad，emotion.pt） | 已落地 |
| 视觉-躯体 | 摄像头/图片/视频 | 手脸接触类 BFRB 行为线索（yolo26_hand_pose.pt + yolo26_face.pt + 几何规则） | 已落地 |
| 视觉-皮肤 | 摄像头/图片 | 自伤痕迹/皮损（bfrb_wound.pt，Roboflow 数据集微调） | 已落地（基线） |
| 腕戴传感 | IMU/热电堆/ToF | BFRB 手势时序识别 | 扩展方向（未接入） |

**腕戴传感器通道的公开依据**：Kaggle 竞赛
[CMI - Detect Behavior with Sensor Data](https://www.kaggle.com/competitions/cmi-detect-behavior-with-sensor-data)
（Child Mind Institute 主办）提供了目前最大的 BFRB 专项公开数据：81 名被试、腕戴 IMU/
热电堆/ToF 多传感器时序，覆盖 18 种手势（含 8 种 BFRB-like：拔头发、抠皮肤、摸脸等）。
该数据集可作为腕戴通道的训练基础，与视觉通道构成真正意义上的多传感器融合；
参考实现见 [pfischer1687/kaggle-cmi-bfrb](https://github.com/pfischer1687/kaggle-cmi-bfrb)
及 LightGBM 方案 [9mohammadt/bfrb-wearable-detection](https://github.com/9mohammadt/bfrb-wearable-detection)。

**融合策略演进**：

1. 当前：视觉多通道线索的规则级并行输出（各通道独立产出觉察线索）；
2. 近期：决策级融合——各通道线索汇入统一的觉察记录体系，按置信度与持续时间加权；
3. 远期：接入腕戴传感器通道后，视觉 + 时序信号做特征级融合（晚融合为主，避免小数据过拟合）。

> 说明：研究叙事宜表述为"视觉多通道线索融合 + 面向可穿戴传感的扩展接口"，
> 腕戴通道在未实际接入前不应表述为已完成能力。

---

> **编制单位**：[团队名称]  
> **编制日期**：2026年6月

---

_— 技术架构方案结束 —_
