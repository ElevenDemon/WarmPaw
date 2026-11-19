# 暖爪运营后台

Vue 3 + Vue Router + Element Plus，已对接同级 3.petSpringboot 的 REST API。

## 启动

先启动后端（默认 http://localhost:8080），然后在本目录运行：

```powershell
npm install
npm run dev
```

访问 http://localhost:5173，使用后台账号登录。后端初始管理员为 admin，初始密码见后端 README；前端不预填密码。会话令牌仅存当前标签页 sessionStorage，刷新通过 /auth/me 恢复，401 自动跳转登录。菜单、路由和写入按钮根据服务端权限展示，最终权限由后端校验。

## 接口与部署

开发环境 /api 代理到 localhost:8080；代理移除浏览器 Origin，避免开发端口变化触发后端跨域拒绝。生产部署应将 /api 反向代理到后端。也可以复制 .env.example 为 .env.local，使用 VITE_API_BASE_URL 指定完整 API 地址；跨域直连时须配置后端 CORS_ORIGINS。环境变量修改后重新启动或构建。

- src/api/client.js：统一 fetch、Bearer、业务错误、20 秒超时和登录失效处理。
- src/api/schemas.json：2026-11-16 从运行中 /v3/api-docs 提取的请求模型。后端 DTO 修改时需同步。
- src/api/forms.js、src/components/SchemaFields.vue：表单字段、嵌套档期、数组、基础校验和蛇形转驼峰映射。
- src/config/modules.js：真实列表字段和模块配置；src/config/actions.js：写入接口与权限。
- src/views/ModulePage.vue：服务端搜索/分页、详情、配置提交、人工派单准入校验、批量结算、客服会话。

已接入订单、资质审核、会员/套餐/权益、喂养师/等级规则、宠主状态、优惠券/直发/流水、价格、评价/投诉、应急/医院、消息模板、权限/员工/日志、字典与站内通知。导出明确为当前页 CSV。订单与喂养师详情读取专用接口，其余详情展示该条实际数据。AI 派单、AI 报告与趋势统计没有使用虚构数据；退款明确标为演示退款。媒体对象键不作为可直接播放地址。

## 验证

```powershell
npm test
npm run build
npm run test:browser
npm run test:live
```

npm test 校验 24 个列表配置、22 个写入接口与 DTO、嵌套字段转换、表单校验、401 和错误处理。

浏览器测试默认使用本机 Edge 无头访客实例，可用 BROWSER_PATH 更换 Chromium 浏览器路径；使用 4199 端口。test:browser 使用隔离 HTTP 测试数据，覆盖登录、所有业务模块及子页、价格修改、客服接管/发消息、刷新会话和退出。

test:live 连接真实 localhost:8080，使用 admin 与后端默认初始密码，可通过 TEST_ADMIN_PASSWORD 覆盖。此测试读取所有页面，并对第一条服务价格原值保存一次（产生操作日志，不改变价格），最后注销测试会话。

2026-11-16 验证结果：真实管理员登录、23 个后台列表接口、看板、353 个字典字段、站内通知，以及真实浏览器页面/价格保存/刷新/退出均通过。当前库无订单、宠主、喂养师、工单和结算数据，因此派单、审核、退款、结算等业务状态流转未进行真实数据端到端验证。客服接管和发送已通过隔离浏览器测试。构建通过，Element Plus 主包仍有体积提示。

## 业务闭环补齐（2026-11-16）

已补入真实数据库演示数据：7 笔订单，2 名喂养师、9 条资质、会员、优惠券/领取流水、评价、投诉、应急工单、医院和客服消息。所有业务数据由真实 API 生成。初始化与复跑方法见后端 tools/README.md。

后端新增 /api/admin/owners/{id} 关联宠主、宠物、地址、会员与订单汇总；/api/public/health 返回 demoPaymentsEnabled。前端新增递归详情展示，并读取订单媒体和报告。演示退款仅在后端显式开启演示支付时展示。

本次已启动联调环境：http://127.0.0.1:5174（新版后端 8081，同一个 pet_demo 数据库）。前端重启运行 npm run dev:demo；后端运行 java -jar target/pet-backend-1.0.0.jar --server.port=8081 --app.demo-enabled=true。默认 npm run dev 仍指向 8080，原 8080 服务需重启加载新代码才能使用新增详情接口。

真实数据浏览器验证：

```powershell
$env:TEST_BACKEND_URL='http://localhost:8081'
node tests/browser-smoke.mjs --live --business
```

后端 10 项 PostgreSQL 隔离集成测试、完整 HTTP 业务闭环及新详情浏览器验证均通过。此前无业务数据的验证限制已解除。演示支付/退款不代表接入真实支付；AI、第三方消息和对象存储仍按既定后端范围预留。
