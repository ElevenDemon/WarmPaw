# 构建辅助工具

`schema-model.json` 是本次PostgreSQL结构快照；`generate-docs.cjs` 用于根据快照生成 `1.前期准备` 中的表结构说明和初始化SQL。数据库结构变更应先新增Flyway迁移，再更新结构快照及文档，不能只改快照。

其他 cjs 文件是本次初始化、分层提取和中文注释补充的一次性脚本，保留供追溯。**不要重复执行**：它们可能覆盖已修改的代码或重复插入注释。正常构建、运行和测试均不依赖这些脚本。

## 业务闭环演示数据

先显式开启演示后端，再运行初始化脚本：

```powershell
java -jar target/pet-backend-1.0.0.jar --server.port=8081 --app.demo-enabled=true
node tools/seed-integration.mjs
```

默认连接 http://localhost:8081/api，SEED_API_URL 可覆盖地址，SEED_PASSWORD 可覆盖演示账号密码。使用 Bootstrap 创建的 admin/owner/sitter，全部业务通过 HTTP 接口写入，不直接修改订单状态。仅用于本地演示。

数据带“联调演示”标记；媒体和资质材料仅为对象键，没有真实上传。脚本保留待派单、待审核、待结算、服务中、送医中和客服记录，并验证审核、会员购买、券领取/直发、下单、派单、接单、打卡、清单、完单、评价、结算、退款、投诉、应急授权与客服回复。

integration-seed-state.json 记录成功步骤，同库重复执行跳过已完成步骤。不要将此状态文件用于另一套数据库。单步中途失败时需核对已有记录再重试，不能视为跨接口事务。结果在 integration-seed-report.json。已验证同库重复运行不会增加记录。
