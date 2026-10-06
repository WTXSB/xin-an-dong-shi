# 心安动识 Web 全栈平台

「心安动识」是一个以情绪觉察、身体信号感知与温柔陪伴为核心的全栈 Web 项目。当前版本已跑通 Vue 前端、Spring Boot 后端、Flask YOLO 推理服务，并接入 DeepSeek 作为安小宁对话能力的后端支撑。

本项目承载的研究课题全称为「基于多传感器融合的 BFRBs（身体聚焦重复行为）智能检测系统研究」：以视觉传感器通道（面部表情 + 躯体动作线索）为当前落地主体，腕戴传感器通道为扩展方向。

项目的表达原则是：看见线索，不贴标签；提供陪伴，不制造压力；尊重隐私，让使用者始终拥有选择权。

## 当前整合版本（2026-10-06，main）

本次按项目创作者要求，将以下已整合的真实网页代码直接提交到 `main`，不再作为待合并的演示分支。仓库包含前端、后端、既有推理服务、手绘素材、测试、数据库增量脚本与说明文档；不包含本机密钥、用户数据库、依赖缓存、浏览器登录资料或编译产物。

- 顶部继续使用横向导航。首页轮播下方展示关于产品的完整内容，导航不再保留关于产品入口；旧地址 `/aboutProduct` 重定向到首页。
- 奶油暖色加深，保留各页面不同的手绘主题；心有灵犀增加书信花园、独立说明卡片与对话气泡。
- 情绪画像升级为情绪日记，沿用 `/dataView`：三个时段独立心情评分、动物表情弹窗、按账号和日期持久化的日历与立体日记本，历史日记不会被新一天覆盖。
- 日记 AI 回顾逐次确认后才发送给 DeepSeek，分析和具体祝福展示在觉察记录的独立信笺卡片。**当前本地未配置密钥，真实日记 AI 调用尚未验收**；未配置或失败会显示错误，不用展示样例冒充真实结果。
- 页面内按钮跳转、浏览器前进/后退及刷新都会同步顶部导航高亮、文字和窗帘；感知子页面同步展开其父级导航。
- 首页四张既有 4K 手绘壁纸自动轮播间隔改为 **2.5 秒**，保留悬停暂停和手动切换。

前端构建、34 项后端测试通过；日记与导航浏览器检查使用隔离内存数据库，不写真实账号内容。最新验收说明与截图见 [情绪日记及导航验收](qa/emotion-diary-20261006/README.md)，日记完整页面见 [截图](qa/emotion-diary-20261006/diary-full-1440.jpg)。其中分析信笺截图仅为明确标注的视觉测试样例。

启动方式见下方「本地启动」相关章节。首次克隆运行时安装依赖并重新构建；演示 H2 的新增表自动创建，生产 MySQL 必须先执行 [日记增量建表脚本](deploy/migrations/20261006-emotion-diary.sql)。已有日记保存在本机后端数据库，不提交到公开仓库；后端重启后需要重新登录。

## 奶油暖黄手绘界面改造（2026-10-05）

2026-10-05 的首轮贡献通过独立分支和 Pull Request 提交，已由项目创作者合并到 `main`。以下是首轮改造的历史记录；当前整合版本见上方说明。首轮保留已有全栈功能、路由、接口和地图配置。

- 保留横向顶部导航，桌面默认图标显示，点击展开原导航名称；黄色窗帘采用 CSS 3D 透视、立体褶皱、上方与左右慢开和最终扎帘动画，选中背景透明，并支持减少动态效果偏好。
- 关于产品、温柔感知、情绪画像、心灵 SPA、觉察记录、安心对话采用六套不同的手绘主题。大卡片用奶油背景和画面包围文字，小信息框保留内容但移除厚重框体。
- 页间装饰用云层、草坡和小径连接；各栏目底部使用与栏目关联的故事书、观察溪流、情绪彩虹、林间休憩、觉察日记和安心茶叙场景。图片等比显示，窄屏裁切并调整画面焦点，不非等比拉伸。
- 情绪画像新增独立的积极、消极、中性动物情绪展示卡，共十二个名称与表情；不参与原识别统计或数据处理。消极表情采用独立安全裁切窗口，避免串入相邻图案。
- 觉察记录底图紧接内容，间距为 12px。首页轮播替换为四张 4K 手绘治愈壁纸，并移除原叠加动物装饰。
- 八个原页面的正文、动态插值、业务事件与业务脚本保留；新增情绪名称仅位于独立展示组件。`/trashMap` 仍使用真实高德地图，不以插画替代。

### 前端预览与构建

在仓库根目录执行（Node.js 20.19+ 或 22.12+）：

```powershell
cd yolo-moudle/face/yolo_face_detection_vue
npm ci
npm run dev -- --host 127.0.0.1 --port 8100
```

地图页面地址保持为 `http://127.0.0.1:8100/#/trashMap`；登录、识别、资源搜索和聊天等业务需相应后端服务，可使用下方现有全栈启动脚本。

```powershell
npm run build
node qa/verify-preservation.mjs
```

### 验收资料与素材

- [本次设计、素材路径及验收说明](yolo-moudle/face/yolo_face_detection_vue/qa/connected-scenes-notes.md)
- [手绘素材完整提示词](yolo-moudle/face/yolo_face_detection_vue/qa/connected-scenes-prompts.txt)（内置 imagegen 生成）
- [原文、事件与脚本比对结果](yolo-moudle/face/yolo_face_detection_vue/qa/preservation-report.json)
- [布局检查结果](yolo-moudle/face/yolo_face_detection_vue/qa/healing-cards-report.json) · [六套场景与 CSS 3D 检查](yolo-moudle/face/yolo_face_detection_vue/qa/connected-scenes-report.json)
- [心灵 SPA 完整截图](yolo-moudle/face/yolo_face_detection_vue/qa/healing-cards-trashMap-full-1440.png)
- [情绪画像完整截图](yolo-moudle/face/yolo_face_detection_vue/qa/healing-cards-dataView-full-1440.png)
- [觉察记录完整截图](yolo-moudle/face/yolo_face_detection_vue/qa/healing-cards-trashRecords-full-1440.png)

完整代码、最终及留存手绘素材、文档、验收脚本与截图随 PR 提交；不包含浏览器配置/登录资料、Maven/npm 缓存、`node_modules`、`dist` 或本机密钥。本次前端构建通过，1440px/390px 页面和地图额外 1920px/1024px/768px 布局已检查。当前独立验证环境未配置高德 Key，只现场检查未配置状态；配置有效 Key 后需补做定位、地图标记和详情等完整交互验收。这不否定下文原版本已有的地图实测记录。

## 心有灵犀与首页整合（2026-10-05）

- `/spaConnect` 新增独立的书信花园手绘主题：大段说明和状态采用围绕正文的装饰卡片，小型条目去掉厚重边框，往来书信采用圆润气泡。插画与正文分别占位，底部为连续花园画面，图片按原始比例显示。
- 关于产品的完整组件复用到首页轮播下方，桌面与移动端菜单不再显示关于产品入口；旧 `/aboutProduct` 地址只重定向到 `/homePage`。首页删除“开始温柔感知”和“了解心安动识”两个按钮，关于产品组件内原有功能不变。
- `warm-depth.scss` 统一加深奶油杏黄、陶土橙及鼠尾草绿。不对整页应用滤镜，不改变高德标记、图表分类、接口或数据结构。
- 心有灵犀全部原文、插值、事件绑定与业务脚本逐项核对；书信和状态交互在独立测试浏览器内用拦截样例验证，不向真实后端写入数据。

手绘素材与提示词见 [书信插画说明](yolo-moudle/face/yolo_face_detection_vue/src/assets/healing-frames/letter-art-notes.md)。验收脚本、完整截图和报告保存于 `qa/warm-connect-20261005/`；该目录的 `edge-profile/` 为本机测试浏览器资料，不应上传。

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

## 情绪日记、三时段日历与 AI 回顾（2026-10-06）

- 原 `/dataView` 路由保持不变，导航及标题由「情绪画像」改为「情绪日记」。原识别统计、动物情绪展示与表情识别记录仍保留在日历和日记本下方。
- 根据 `ui-showcase (1).html` 的月历与打分结构实现真实日历：星期一开头，完整月份、前后月切换、选中日期详情；每一天独立保留上午、下午、晚上三份 1–10 分评分，动物表情与文字标签对应。空时段不填假数据，未来时段不能提前记录。
- 浏览器本地时间分段为上午 00:00–11:59、下午 12:00–17:59、晚上 18:00–23:59。进入或重复点击情绪日记导航时由后端原子认领本时段弹窗，每账号、每天、每时段自动弹一次；关闭未填写也不会重复自动弹。日历仍可手动补记、修改。标记存入数据库，刷新、重开或同账号另一个标签页不会重复认领。
- 精装日记本使用真实键盘输入、保存、日期回看，3D 书脊、叠页和书签为 CSS，插画等比显示。日记按「账号 + 日期」保存至后端；新一天不会覆盖旧日记，历史日记可回看与编辑，未保存的文字在切换日期/路由和关闭页面前提示。版本冲突不会静默覆盖另一窗口内容。
- 日记和评分通过登录成功后建立的服务端 HttpOnly 会话进行账号隔离，不信任客户端提交的用户名；后端重启后需重新登录一次。退出登录会失效后端会话。生产部署还需使用 HTTPS、安全 Cookie 和整站一致的认证/CSRF 体系；原有其他接口认证方式未在本次全面重写。
- 点击「分析日记」之前必须勾选同意将选定当天日记与三份评分发送给 **DeepSeek**；不会自动发送其他日期或识别记录。密钥只在后端，复用 `DEEPSEEK_API_KEY`、`DEEPSEEK_API_URL` 与 `DEEPSEEK_CHAT_MODEL` 配置。保存与手动补记不调用 AI。
- 分析返回当天的复杂情绪、可核对原文的线索、小行动和基于具体经历的个性化祝福。结果会自动出现在「觉察记录」的新信笺卡片中；历史回顾分页保留。日记/评分变更后该天的旧分析失效，需要重新确认分析。AI 回顾仅供辅助觉察，不是医学诊断。
- 当前本地副本没有配置 DeepSeek 密钥，因此真实 AI 请求尚未现场验收；未配置、超时、异常或不合规 JSON 会明确报错，正文和评分仍保留，不用固定文案冒充 AI 结果。请在被 Git 忽略的 `scripts/deepseek-env.local.ps1` 中配置环境变量，再用 `scripts/run-local-spring.ps1` 重启后端。不要把密钥提交到 GitHub 或粘贴到公开对话。
- 本地 H2 表由 `demo-schema.sql` 非破坏性初始化；生产 MySQL 升级须先运行 [日记增量建表脚本](deploy/migrations/20261006-emotion-diary.sql)。当前保存采用单后端实例同步及乐观版本校验，多实例部署需进一步加入数据库行锁/分布式并发控制。

验收位于 [qa/emotion-diary-20261006](qa/emotion-diary-20261006/)：JUnit 使用内存数据库测试去重、三评分、过去日记、隔离、冲突、同意、缓存、失效与 AI 请求/输出合同；浏览器验收将所有 API 流量仅转发至 10099 的独立内存后端，正常登录后测试输入、保存、回看及响应式布局，不写真实账号记录。AI 信笺截图使用明确标注的隔离展示样例，不是真实模型输出。

新增手绘素材与完整生成提示词见 [diary-art-notes.md](yolo-moudle/face/yolo_face_detection_vue/src/assets/healing-frames/diary-art-notes.md)。

## 后续优化建议

- 将 H2 Demo 数据正式迁移到 MySQL。
- 补充对话历史管理与删除功能。
- 给安心对话增加流式输出体验。
- 行为模型迭代：扩充多人物多场景自录数据、修正伪标注噪声帧，缩小跨域差距。
- 腕戴传感器通道原型（STM32 + IMU 时序手势识别），与视觉通道形成多传感器融合。
- 医院实地调研，「心有灵犀」接入真实医生并完善角色权限。
- 腾讯云部署时拆分主站与 YOLO 推理服务，避免小规格服务器资源紧张。
