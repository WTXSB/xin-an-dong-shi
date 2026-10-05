# 心安动识 Web 全栈平台

「心安动识」是一个以情绪觉察、身体信号感知与温柔陪伴为核心的全栈 Web 项目。当前版本已跑通 Vue 前端、Spring Boot 后端、Flask YOLO 推理服务，并接入 DeepSeek 作为安小宁对话能力的后端支撑。

本项目承载的研究课题全称为「基于多传感器融合的 BFRBs（身体聚焦重复行为）智能检测系统研究」：以视觉传感器通道（面部表情 + 躯体动作线索）为当前落地主体，腕戴传感器通道为扩展方向。

项目的表达原则是：看见线索，不贴标签；提供陪伴，不制造压力；尊重隐私，让使用者始终拥有选择权。

## 当前定位

- 面向已经感到紧绷、疲惫或需要情绪照顾的使用者。
- 通过图片、视频、摄像头感知情绪与身体线索。
- 用「觉察记录」承接结果，而不是给出冷冰冰的结论。
- 用「安小宁」作为全局陪伴入口，帮助使用者慢下来、恢复内驱力。
- 支持心灵 SPA 导引、安心对话、觉察记录回看等疗愈体验。

## 技术架构

| 模块 | 技术 | 当前作用 |
| --- | --- | --- |
| 前端 | Vue 3 + Vite + Element Plus | 温柔风 UI、识别入口、觉察记录、安心对话、安小宁悬浮球 |
| 后端 | Spring Boot + MyBatis Plus | 用户、文件上传、觉察记录、隐私授权、DeepSeek 后端代理 |
| 推理服务 | Flask + Ultralytics YOLO + PyTorch | 图片、视频、摄像头感知 |
| 本地演示数据库 | H2 Demo Profile | 文件型持久化，重启 Spring Boot 后保留觉察记录 |
| 生产数据库方案 | MySQL | 腾讯云部署时使用 |
| AI 对话 | DeepSeek Chat Completions | 安小宁陪伴式对话 |

## 已完成的核心功能

### 1. 三端基础跑通

- 前端运行在 `http://127.0.0.1:8100`
- Spring Boot 后端运行在 `http://127.0.0.1:9999`
- Flask YOLO 服务运行在 `http://127.0.0.1:5000`
- YOLO 权重使用 `emotion.pt`
- 本地 `pytorch` conda 环境已用于 YOLO 服务

### 2. 温柔风前端改造

- 首页、图片感知、视频感知、摄像头感知、觉察记录、心灵 SPA 导引、安心对话均已向温柔陪伴风格靠拢。
- 页面文案避免使用容易造成压力的表达。
- 左侧栏旧建筑图标已替换为更温柔可爱的图标。
- 全局安小宁悬浮球已支持拖动，并会记住位置。

### 3. 图片感知闭环

- 上传图片前要求确认隐私说明。
- 支持选择是否保存觉察记录。
- 支持选择是否在记录中保留图片路径。
- 结果展示为：
  - 主要线索
  - 我看见的线索
  - 身体可以被温柔询问
  - 此刻的小练习
  - 保存选择提示
- 后端 `/flask/predict` 已接收 `keepRecord` 与 `keepMedia`，用户选择会真正影响记录保存。

### 4. 视频感知闭环

- 上传视频前要求确认隐私说明。
- 支持选择是否保存觉察记录。
- 支持选择是否保留视频路径。
- 视频处理仍由 Flask YOLO 流式输出画面。
- 处理完成后生成陪伴式反馈与觉察记录。
- Flask YOLO 已接入 `saveRecord` 与 `keepMedia`，避免仅做界面提示。

### 5. 摄像头感知闭环

- 摄像头只在使用者主动确认后开启。
- 支持随时停止并整理本次线索。
- 支持选择是否保存觉察记录。
- 支持选择是否保留结果视频路径。
- 停止后生成温柔反馈：
  - 实时线索整理
  - 身体信号提醒
  - 当下小练习

### 6. 觉察记录

- 新增 `awareness_records` 记录体系。
- 图片、视频、摄像头结果可以保存为「觉察记录」。
- 记录中保留的是温柔摘要、身体提醒、小练习与用户选择，而不是冷硬结论。
- 支持不保存素材路径，只保存文字摘要。
- 新增结构化检测会话，分表保存情绪统计与 BFRB 事件明细。
- 保存分析模式、模型组合、事件起止时间、持续时长、关键帧、置信度和证据类型。
- 使用 `sessionId` 防止视频流重连导致重复落库。
- 觉察记录可在原页面展开完整分析，刷新后从后端恢复相同结果。
- 删除觉察记录时，同步删除关联的结构化结果。

### 7. 隐私授权记录

- 新增隐私授权记录接口。
- 图片、视频、摄像头使用前均保存授权选择。
- 对话模块明确只发送使用者主动输入的文字。
- 图片、摄像头画面、识别记录不会自动进入安小宁对话。

### 8. DeepSeek 与安小宁

- 安小宁悬浮球和安心对话页面已统一调用后端 `/api/ai/chat`。
- DeepSeek API Key 只保存在后端环境变量中，前端不会接触 Key。
- 支持最近几轮上下文，让对话更连贯。
- 支持选择是否保存本次对话。
- 无 Key 时会走本地温柔兜底回复。
- 已测试接通 `deepseek-v4-pro`，后端返回 `provider = deepseek`。

### 9. 预诊报告与双视角输出

- 结构化检测记录可生成预诊报告（AI 辅助摘要 + 本地模板兜底 + 禁词拦截）。
- 同一份检测数据提供两种视角：
  - **患者视角（默认）**：书信式关怀信，温柔口吻、生活化表达，给出可行动的小建议，
    不出现"检测、诊断、症状"等医学用词，结尾声明"只是陪伴与参考，不是任何结论"。
  - **心灵SPA师视角**：完整客观预诊报告 + 安全关注标注
    （持续愤怒主导、BFRB 高频/长持续、自述危险关键词三档规则），
    标注仅为算法线索汇总，不构成诊断依据。
- 书信与报告均支持打印 / 另存为 PDF。
- 心灵 SPA 地图导引已接入真实高德地图 API：IP 定位（城市级）+ 浏览器精确定位、
  附近真实机构检索（心理咨询/心理门诊/冥想瑜伽/公园绿道/书店茶饮）、
  机构详情弹窗（电话/地址/高德详情链接）、自由缩放。

### 10. 心有灵犀（医患联动）

- 注册后首次登录进入身份向导：「我来照顾自己」（使用者）或「我是心灵SPA师」。
- 双端认证体系（模拟审核流，管理员手动通过/驳回）：
  - 心灵SPA师：医师资格证号 + 实名 + 就职医院 + 科室 + 职称 + 简介；
  - 使用者：平时无需实名，发起倾诉前完成实名认证（身份证仅存掩码，绝不存明文）。
- 已认证心灵SPA师卡片展示（医院/科室/职称/简介），使用者「向TA倾诉」寄出第一封信
  建立联结，书信式站内对话（轮询增量刷新）。
- 心灵SPA师可在对话侧栏查看求助者历史觉察记录，并跳转预诊报告客观参考视角，
  形成"线上预诊、必要时转线下"的流程雏形。
- 认证审核界面（admin）：待审列表 + 通过/驳回 + 备注。

### 11. BFRB 行为检测模型与数据管线

- 五类 BFRB 行为直检模型 `weights/bfrb_behavior.pt`（咬指甲/抠皮肤揉眼/拔头发抓头皮/摸脸/手部自然状态），
  同源验证集 mAP50 = 0.744；皮肤损伤痕迹模型 `weights/bfrb_wound.pt`（mAP50 = 0.519）作为辅助通道。
- 数据管线（Flask 目录）：`pseudo_label_pipeline.py`（视频/图片抽帧 → 手脸模型自动伪标注）、
  `merge_datasets.py`（多来源数据集合并）、`train_bfrb.py`（GPU 微调训练，内置全套增广）。
- 训练数据：FaceTouch 公开数据集（PLOS ONE）+ 团队自录动作视频，经伪标注合并。
- Flask `/predictBfrb` 双模式：行为模型直检 / 手脸几何规则兜底，按权重类别名自动切换。
- 详细文档见 `yolo-moudle/face/yolo_face_detection_flask/BFRB_README.md`。

## 常用本地地址

| 页面 | 地址 |
| --- | --- |
| 前端首页 | `http://127.0.0.1:8100/#/homePage` |
| 图片感知 | `http://127.0.0.1:8100/#/imgPredict` |
| 视频感知 | `http://127.0.0.1:8100/#/videoPredict` |
| 摄像头感知 | `http://127.0.0.1:8100/#/cameraPredict` |
| 觉察记录 | `http://127.0.0.1:8100/#/trashRecords` |
| 心灵 SPA 导引 | `http://127.0.0.1:8100/#/trashMap` |
| 安心对话 | `http://127.0.0.1:8100/#/smartChat` |
| 心有灵犀 | `http://127.0.0.1:8100/#/spaConnect` |

## 启动方式

推荐使用脚本统一启动：

```powershell
.\scripts\start-all.ps1 -BuildBackend
```

停止本地服务：

```powershell
.\scripts\stop-all.ps1
```

服务端口：

```text
Frontend:    http://127.0.0.1:8100
Backend API: http://127.0.0.1:9999
YOLO Flask:  http://127.0.0.1:5000
```

## DeepSeek 本地配置

本地 Key 文件：

```text
scripts/deepseek-env.local.ps1
```

示例：

```powershell
$env:DEEPSEEK_API_KEY = "你的真实 DeepSeek API Key"
$env:DEEPSEEK_MODEL = "deepseek-v4-pro"
$env:DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"
```

注意：

- `scripts/deepseek-env.local.ps1` 已加入根目录 `.gitignore`。
- 不要把真实 Key 写入前端代码。
- 不要把真实 Key 提交到仓库。
- 修改 Key 后需要重启 Spring Boot。

## 高德地图与 Roboflow 本地配置

高德地图 Key（Web端 JS API，心灵 SPA 地图使用）配置在前端 `yolo-moudle/face/yolo_face_detection_vue/.env.local`：

```text
VITE_AMAP_KEY=你的高德 Key
VITE_AMAP_SECURITY_CODE=你的高德安全密钥
```

Roboflow API Key（BFRB 数据集下载）保存在 `scripts/roboflow-env.local.txt`。
两者均已加入 `.gitignore`，不会提交到仓库；修改高德 Key 后需重启前端 dev server。

## 数据库与部署方向

本地演示阶段使用文件型 H2 Demo Profile，数据保存在 Spring Boot 目录的 `data/yolo-demo.mv.db`，重启后仍可回看觉察记录和结构化分析。测试环境使用独立的内存 H2，不会写入演示数据。

腾讯云部署建议：

- 前端：Vue 打包后由 Nginx 托管。
- 后端：Spring Boot 使用 systemd 或类似方式常驻运行。
- 数据库：生产环境切到 MySQL。
- DeepSeek：通过后端环境变量配置，不放在前端。
- YOLO 推理：不建议塞进 2GB 最小规格云服务器，优先独立部署或使用更高配置机器。

详细部署材料见：

```text
deploy/README-deploy.md
deployment-recommendation-tencent-cloud.docx
```

## 当前重点文件

| 文件 | 说明 |
| --- | --- |
| `yolo-moudle/face/yolo_face_detection_vue/src/views/imgPredict/index.vue` | 图片感知页面 |
| `yolo-moudle/face/yolo_face_detection_vue/src/views/videoPredict/index.vue` | 视频感知页面 |
| `yolo-moudle/face/yolo_face_detection_vue/src/views/cameraPredict/index.vue` | 摄像头感知页面 |
| `yolo-moudle/face/yolo_face_detection_vue/src/views/trashRecords/index.vue` | 觉察记录与完整分析回看 |
| `yolo-moudle/face/yolo_face_detection_vue/src/views/smartChat/index.vue` | 安心对话页面 |
| `yolo-moudle/face/yolo_face_detection_vue/src/components/AnXiaoNingFloat/index.vue` | 安小宁悬浮球 |
| `yolo-moudle/face/yolo_face_detection_springboot/src/main/java/com/example/Ece/controller/AiChatController.java` | DeepSeek 后端代理 |
| `yolo-moudle/face/yolo_face_detection_springboot/src/main/java/com/example/Ece/controller/AwarenessRecordController.java` | 觉察记录接口 |
| `yolo-moudle/face/yolo_face_detection_springboot/src/main/java/com/example/Ece/controller/DetectionAnalysisRecordController.java` | 结构化检测结果保存、查询与关联接口 |
| `yolo-moudle/face/yolo_face_detection_springboot/src/main/java/com/example/Ece/controller/PreVisitReportSummaryController.java` | 预诊报告摘要与关怀书信生成、安全标注 |
| `yolo-moudle/face/yolo_face_detection_springboot/src/main/java/com/example/Ece/controller/SpaConnectController.java` | 心有灵犀：身份认证、求助单、站内对话 |
| `yolo-moudle/face/yolo_face_detection_vue/src/views/preVisitReport/index.vue` | 预诊报告双视角页面（书信/客观参考） |
| `yolo-moudle/face/yolo_face_detection_vue/src/views/spaConnect/index.vue` | 心有灵犀模块页 |
| `yolo-moudle/face/yolo_face_detection_vue/src/views/trashMap/index.vue` | 心灵 SPA 专属疗愈地图（高德 API） |
| `yolo-moudle/face/yolo_face_detection_flask/facetry.py` | YOLO 图片、视频、摄像头推理服务 |
| `yolo-moudle/face/yolo_face_detection_flask/bfrb_detect.py` | BFRB 手脸几何规则 + 行为模型直检 |
| `yolo-moudle/face/yolo_face_detection_flask/pseudo_label_pipeline.py` | 伪标注数据管线 |

## 隐私与表达原则

- 所有涉及摄像头、图片、视频的功能，都要先让使用者确认用途。
- 是否保存记录、是否保留素材路径，由使用者选择。
- 安心对话只发送主动输入的文字。
- 不自动把图片、摄像头画面或识别记录发送给 DeepSeek。
- 页面表达坚持温柔、鼓励、可行动，不做压迫式判断。
- 识别结果只作为自我觉察线索，不能替代线下专业支持。

## 已验证

- 前端 `npm run build` 通过。
- Spring Boot `mvn test` 通过（12 项测试）。
- Flask YOLO `py_compile` 通过，`test_bfrb_events.py` 4 项测试通过。
- `/flask/file_names` 可返回 `emotion.pt` 与 BFRB 系列权重。
- `/ai/chat` 已接入 DeepSeek，返回 `provider = deepseek`。
- 图片、视频、摄像头页面均已通过浏览器基础检查。
- 图片与视频链路已通过结构化落库、幂等、刷新回查与隐私不保存验证。
- 心有灵犀全流程（注册 → 身份向导 → 认证提交 → admin 审核 → 发起倾诉 → 双向书信对话 → 医生查看历史报告）已经浏览器端到端走查。
- 预诊报告双视角（书信禁词检查、幂等缓存、安全标注规则）已经接口与浏览器验证。
- 心灵 SPA 地图高德 API（IP 定位、真实 POI、详情弹窗、缩放）已经浏览器实测。

## 后续优化建议

- 将 H2 Demo 数据正式迁移到 MySQL。
- 补充对话历史管理与删除功能。
- 给安心对话增加流式输出体验。
- 行为模型迭代：扩充多人物多场景自录数据、修正伪标注噪声帧，缩小跨域差距。
- 腕戴传感器通道原型（STM32 + IMU 时序手势识别），与视觉通道形成多传感器融合。
- 医院实地调研，「心有灵犀」接入真实医生并完善角色权限。
- 腾讯云部署时拆分主站与 YOLO 推理服务，避免小规格服务器资源紧张。
