# 六栏目手绘卡片验收

> 本文记录早期视觉迭代。最终实现不再使用循环花边；最新的连续场景、十二表情、低颗粒画面、12px 底图间距和 CSS 3D 窗帘以 [connected-scenes-notes.md](connected-scenes-notes.md) 为准。

实现文件：`src/theme/healing-cards.scss`、`src/theme/index.scss`；动物表情容器：`src/views/dataView/index.vue`；导航新交互：`src/layout/navMenu/horizontal.vue`、`src/layout/navMenu/HealingNavCurtain.vue`。

页面业务脚本、全部原始正文和动态文字、业务指令保持原样。最新用户要求允许桌面导航默认收起文字；点击展开原文字，原 router 跳转不变。移动端导航仍按原行为工作。

## 手绘素材

使用 imagegen 技能的内置生成模式，最终素材均复制至本 D 盘项目 `src/assets/healing-frames/`。

提示词组摘要：奶油杏黄色、低饱和鼠尾草绿与淡粉、水彩纸质感、细腻手绘、小动物置于角落、中心透明大留白、不含文字和水印。六套场景分别为柴犬云朵、小猫兔子花园、小动物情绪陪伴、林间休憩、月夜日记、猫咪茶桌。各栏目的页尾留白使用自己的主题动物，不共用同一陪伴图。

| 栏目 | 素材 |
| --- | --- |
| 关于产品 | shiba-cloud-frame.png |
| 温柔感知（图片、视频、摄像头） | cat-rabbit-meadow-frame.png |
| 情绪画像 | tiny-companions-frame.png |
| 心灵 SPA | woodland-rest-frame.png |
| 觉察记录 | moonlit-journal-frame.png |
| 安心对话 | cozy-tea-chat-frame.png |

角饰九切片按原比例显示；四周边缘使用 repeat 而非 stretch/round，花草包围正文。页尾以固定 3:2 比例裁切主题图，不改变横纵比例。正文不承载图片；装饰 pointer-events 为 none。小信息框透明、无边框、无阴影，不增加背景。

情绪动物素材：`animal-emotion-sprite.png`，使用内置 imagegen 生成透明 2×2 表情图。提示词摘要：左上好奇仓鼠、右上开心柴犬、左下难过兔子、右下平静小猫，淡彩水粉和彩铅细线，透明背景，无文字水印，四个角色各自居中。仅映射到既有四项统计的视觉容器，不新增情绪数据、按钮或标签。

窗帘只在点击后创建。2.4 秒动画中左、右帘布展开，同时上方帘幔上升；最终保留波浪帘幔、褶皱和系带。关于产品使用既有 Element Plus InfoFilled 图标，不修改路由元数据。

## 验证证据

- `preservation-report.json`：八个实际页面的静态文字、动态插值、业务指令和脚本均通过比对。纯视觉 class/style 绑定不计入业务指令。
- `curtain-navigation-report.json`：初始七个导航名称宽度均为 0 且没有窗帘；六栏目点击后仅当前文字展开，原路由和横向排列均通过。另核对上、左、右动画裁切的不同阶段，截图为 `curtain-opening-300ms.png`、`curtain-opening-1100ms.png`、`curtain-opening-1900ms.png`、`curtain-opening-2700ms.png`。
- `healing-cards-report.json`：八页面 1440px 与 390px 验证无文档横向滚动；SPA 额外检查 1920、1024、768px。小浮动助手自身装饰的溢出为原有视觉，不是页面溢出。
- 完整截图：`healing-cards-栏目路由-full-宽度.png`；导航点击截图：`curtain-nav-栏目路由-1440.png`。
- `npm run build` 成功。现有 Sass import、字体路径和包大小警告未改动。

高德 Key 未配置：只验证现有未配置提示与禁用按钮，不能声称已运行真实地图定位、标记或详情；相关业务脚本和事件绑定保留，需配置有效 Key 后完成地图端到端验证。
