# 心安动识部署说明

## 推荐部署边界

- 腾讯云最小规格服务器：Nginx、Vue dist、Spring Boot、MySQL、DeepSeek 后端代理。
- YOLO/PyTorch 推理服务：先独立运行在本地或更高配置机器上，避免占满 2GB 云服务器内存。

## MySQL 初始化

1. 创建数据库与用户。
2. 执行 `yolo-moudle/face/yolo_face_detection_springboot/src/main/resources/mysql-schema.sql`。
3. 执行 `deploy/migrations/20261006-emotion-diary.sql`，创建情绪日记及三时段评分表；已有生产数据库升级也需要执行该增量脚本，不要删除原表或记录。
4. 将真实连接信息写入服务器私有环境文件，不要提交到仓库。

## 后端启动

1. 复制 `deploy/env.prod.example` 到 `/etc/xinan-dongshi/xinan-dongshi.env`。
2. 修改 `MYSQL_PASSWORD`、`DEEPSEEK_API_KEY`、`FILE_PUBLIC_HOST`。
3. 将后端 Jar 放到 `/opt/xinan-dongshi/app/`。
4. 安装 `deploy/xinan-dongshi.service` 到 `/etc/systemd/system/`。
5. 执行 `sudo systemctl daemon-reload && sudo systemctl enable --now xinan-dongshi`。

## 前端启动

1. 本地执行 `npm run build`。
2. 将 `dist` 内容复制到 `/opt/xinan-dongshi/web/`。
3. 安装 `deploy/nginx-xinan-dongshi.conf` 到 Nginx 站点配置。
4. 检查并重载 Nginx。

## 隐私与表达原则

- API Key、数据库密码只放服务端环境变量。
- 摄像头、图片、视频使用前必须明确提示用途与保存策略。
- 识别结果仅作为觉察线索，不作为定义用户状态的结论。
- 日记 AI 分析仅在用户逐次勾选同意并点击分析后发送选定当天的日记和评分。无 DeepSeek 密钥时日历、评分、日记保存仍可用，但 AI 回顾明确提示未配置。
- 日记接口使用登录建立的服务端会话。生产环境需 HTTPS、安全 Cookie 和整站认证/CSRF 防护；当前版本不代表已完成所有旧接口的安全改造。新增日记写入针对单后端实例同步与版本校验，多实例需补数据库行锁或分布式并发控制。
