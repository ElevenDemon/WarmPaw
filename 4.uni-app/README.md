# 暖爪双端客户端

基于 uni-app、Vue 3、TypeScript 和 Pinia，使用同一工程按登录角色提供宠主端与喂养师端。后端位于 `../3.petSpringboot`。

## 启动和构建

1. 按后端项目配置 PostgreSQL、启动 Spring Boot；演示数据和支付需要启用后端的 `DEMO_ENABLED`。演示账号和密码以实际后端配置为准。
2. 在本目录执行 `npm install`、`npm run dev:h5`，通过开发服务器访问。H5 开发环境将 `/api` 代理到本地后端。
3. 执行 `npm run build:mp-weixin`，在微信开发者工具中导入 `dist/build/mp-weixin`（该目录应包含 `app.json` 和 `project.config.json`）；在 `manifest.json` 配置微信 AppID。修改源码后需重新执行构建，并在开发者工具中重新编译。也可以导入本项目根目录，其配置会指向上述构建目录。
4. 执行 `npm run build:app` 生成 App 资源，在 HBuilderX 中导入并配置原生打包环境、证书及平台权限后打包。资源构建成功不代表已完成 Android/iOS 安装包或真机验证。
5. 使用 `npm run typecheck` 检查类型；`npm run build:h5` 构建 H5。

小程序和 App 发布前通过 `.env.production` 设置 `VITE_API_BASE_URL` 为可访问的 HTTPS API 地址，并配置微信小程序服务器合法域名。真机上的 localhost 指向设备自身；定位选址还需按平台配置地图及位置权限。请根据部署环境检查 H5 代理和跨域配置。

宠主服务地址使用后端代理调用高德 Web 服务的输入提示、地理编码和逆地理编码接口。部署后端时设置 `AMAP_WEB_KEY`（高德控制台创建的 Web 服务 Key），不要将该 Key 写入小程序/H5 环境变量；微信小程序还需将后端 HTTPS 域名加入 request 合法域名。`AMAP_BASE_URL` 可选，默认 `https://restapi.amap.com`。

微信开发者工具本地调试时，需要启动后端，并在工具的本地设置中勾选“不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”。该设置仅用于开发调试。

后端如需生成独立包以避开被运行中进程占用的默认 jar，可在后端目录执行 `mvn -Pmobile -DskipTests package`，运行 `target/pet-backend-mobile.jar`。媒体根目录由 `MEDIA_ROOT` 配置，当前为本地文件存储。

## 功能边界

当前使用账号密码登录；微信登录、真实支付、AI、实时 IM、短信或订阅消息、自动录像/直播及喂养师资质材料上传尚需对接对应服务。演示支付只在后端允许时可用。应急功能目前需关联已有订单。媒体采用受权限保护的上传和读取接口，视频保留期按业务规则限制访问；生产部署还需考虑对象存储、多实例共享和过期文件清理。

`tests/live-smoke.mjs` 使用隔离 PostgreSQL schema、独立端口和浏览器测试主要流程；运行前需本机 PostgreSQL、Java、已构建的移动端后端 jar、H5 构建产物及 Edge。测试会创建独立 schema，结果写入 `test-results`。

`uni_modules`、`nativeplugins` 和 `platform` 为扩展预留，按需安装插件或配置平台能力。
