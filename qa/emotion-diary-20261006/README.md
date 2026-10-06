# 情绪日记验收（2026-10-06）

## 导航同步与首页轮播回归（2026-10-06）

`navigation-carousel-regression.mjs` 完成 18 组检查：实际点击首页产品介绍中的感知按钮、对话按钮、日记卡片和 SPA 卡片后，对应顶部导航同时获得 `is-active`、文字展开和窗帘；覆盖图片/视频/摄像头子页面、直接地址跳转、查询参数改变、浏览器前进/后退及刷新。

首页连续三次自动切换的两个完整间隔实测为 2500、2501 毫秒；原来的鼠标悬停暂停和手动切换逻辑未改。Vue 构建成功，浏览器未捕获异常。结果见 `navigation-carousel-verification.json`，截图见 `navigation-synced-1440.jpg`，构建日志见 `navigation-build.log`。复测同样仅使用 10099 端口的独立内存后端，没有写入正式数据库。

## 最终结果

- Vue `npm run build` 成功。
- Spring Boot `mvn package` 成功；34 项测试、0 失败、0 错误。
- 浏览器 13 组检查通过，无未捕获异常。1920、1440、1024、768、390 像素宽度没有横向溢出，装饰图片保持原始比例。
- 输入、保存、历史日期回看、三时段评分与手动修改、再次点击导航不重复弹出、未配置 AI 时保留正文均已检查。
- 最终运行站点 `http://127.0.0.1:8100/` 返回 HTTP 200；实际后端登录、日记状态、退出接口均返回成功，`aiConfigured=false`。该检查未写入真实日记数据。
- 当前后端已重新启动；原浏览器登录会话失效，需要重新登录。

## 截图说明

- `diary-full-1440.jpg`：情绪日记完整网页。
- `diary-390.jpg`：手机宽度页面。
- `mood-dialog-1440.jpg`、`mood-dialog-390.jpg`：动物评分弹窗。
- `awareness-empty-1440.jpg`：觉察记录的日记回顾空状态。
- `awareness-analysis-example-1440.jpg`、`awareness-analysis-example-390.jpg`：隔离的分析信笺展示样例，**不是真实 DeepSeek 输出**，样例未存入真实数据库。

## 数据隔离与限制

JUnit 使用独立 H2 内存数据库。`verify.mjs` 将专用浏览器中的 `/api/` 请求全部转发至 10099 端口的独立内存后端；记录、评分和示例文字均不写入实际运行账号。结果详见 `verification.json`，测试日志见本目录。

真实 DeepSeek 尚未配置，未执行真实外部模型验收。服务端请求合同、结果校验、逐次同意、异常和缓存失效由 MockRestServiceServer / MockBean 测试；不把测试样例当作 AI 调用证据。

自动弹窗的上午、下午、晚上边界及日期更换由固定时钟单元测试覆盖；浏览器检查的是当前时段、手动补记及重复导航行为，不声称在现场跨时段等待验收。

浏览器复测需先按脚本要求启动专用 Edge（CDP 9446）与独立内存后端（10099，`jdbc:h2:mem:diary-ui-qa;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1`），再从项目根目录运行 `node qa/emotion-diary-20261006/verify.mjs`。切勿将脚本的测试接口改指向真实数据库。隔离测试后端已停止；正式 8100 前端和 9999 后端保留运行。
