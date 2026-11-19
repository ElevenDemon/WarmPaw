# 暖爪后端

依据 `2.petVue` 页面与 `1.前期准备` PRD 实现。技术栈为 Java 21、Spring Boot 3.5.7、MyBatis-Plus 3.5.14、PostgreSQL、Flyway、springdoc Swagger。代码包含中文类注释、业务方法注释、实体字段说明和 Swagger 请求字段说明。

## 启动

本机 PostgreSQL 默认连接 `localhost:5432/pet_demo`，用户名 `postgres`，密码 `root`。当前已创建 pet_demo，启动时 Flyway 自动维护表结构和参考配置；更换空环境时需先创建数据库，不必手工建表。

```powershell
cd D:\CodeDemo\petDemo\3.petSpringboot
.\start.ps1
```

默认超级管理员：账号 `admin`，密码 `admin123456`，角色 ADMIN，拥有全部后台权限。首次启动自动创建，重复启动不会覆盖已有账号的密码、状态或权限。首次创建前可设置 `$env:BOOTSTRAP_PASSWORD='自己的8–64位密码'` 覆盖初始密码；已有账号不受此变量影响。部署时请使用自己的初始密码。

启动脚本先执行构建和集成测试，再启动 JAR。已有打包文件时可用 `./start.ps1 -SkipBuild`。本地完整演示使用 `./start.ps1 -Demo`，它会创建 `owner`、`sitter`，密码与配置的初始密码相同（默认admin123456），并开启演示支付/退款。默认不开启演示模式。

IDE 可直接运行 `com.nuanzhua.pet.PetApplication`，无需额外配置即可创建默认超级管理员。

- Swagger：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>
- 健康检查：<http://localhost:8080/api/public/health>

`docs/openapi.json` 提供本次验证时导出的接口文档快照，可导入支持OpenAPI的联调工具；修改接口后以运行中的 `/v3/api-docs` 为准。

Swagger 中先调用 `/api/auth/login`，请求 `{"username":"admin","password":"你的初始密码"}`，复制 `data.token` 到右上角 **Authorize**，无需额外填写 Bearer 前缀。登录、注册和公共目录可匿名访问，其余接口校验会话及业务权限。会话默认12小时到期。

## 配置和构建

`application.yml` 可通过环境变量覆盖：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`SERVER_PORT`、`CORS_ORIGINS`、`TOKEN_HOURS`、`BOOTSTRAP_PASSWORD`、`DEMO_ENABLED`。Vue 默认允许 localhost/127.0.0.1 的5173端口。

小程序智能客服由已登录的 Spring Boot `/api/ai/chat` 代理调用 Python AI 服务。默认地址为 `http://localhost:8090`，部署时通过 `AI_SERVICE_BASE_URL` 和 `AI_SERVICE_API_KEY` 配置服务地址及内部鉴权；不要把模型 API Key 配置到小程序。

本项目的 `settings.xml` 使用 HTTPS Maven Central，避开机器全局的旧HTTP镜像。已下载依赖可离线构建：

```powershell
mvn -o -gs settings.xml -s settings.xml "-Dmaven.repo.local=$PWD/.m2" -B -ntp package
java -jar target/pet-backend-1.0.0.jar
```

新环境首次下载时去掉 `-o`。集成测试需要可连接的 PostgreSQL，会创建随机 `pet_test_*` schema 并运行迁移，各用例事务回滚。可使用 `TEST_DB_URL`、`TEST_DB_USERNAME`、`TEST_DB_PASSWORD` 指定测试数据库。测试schema中保留参考及演示账号；不写入public业务表。重新打包前请停止使用同一 JAR 的进程，避免 Windows 文件占用。

## 日志输出

默认同时输出控制台和UTF-8日志文件（相对于启动工作目录）：

- `logs/pet-backend.log`：启动、数据库迁移、业务接口执行结果与耗时、异常日志。
- `logs/pet-backend-error.log`：ERROR级别异常及堆栈。
- `logs/archive/`：按天或单文件20MB滚动压缩，保留30天；全量归档上限1GB，错误归档上限512MB。

可通过 `LOG_PATH` 修改目录、`LOG_LEVEL` 修改业务日志级别（默认INFO）。控制器日志不记录密码、Token和请求体。预期业务错误记录WARN，未处理异常由全局异常处理器记录ERROR及堆栈。SQL参数日志默认不启用。

```powershell
Get-Content ./logs/pet-backend.log -Tail 50 -Wait
```

## 分层结构

包根目录 `com.nuanzhua.pet`，按要求设置 common、config、controller、entity、mapper、service、service/impl、dto、vo、enums、filter、aspect、exception。

- Controller：接口路由、校验、Swagger；ApplicationService：接口业务编排；领域Service：订单、派单、优惠券、会员等核心规则和事务。
- Entity 与 BaseMapper：每个业务表对应实体与 Mapper。复杂报表、行锁、JSONB和 `RETURNING` 使用 `BusinessMapper` 参数化SQL；`common/Db` 是其统一访问门面。分页使用 MyBatis-Plus 插件。
- filter：Bearer登录拦截；config：跨域、MyBatis、Swagger等；aspect：方法耗时日志；exception：统一异常响应。
- resources/db/migration：数据库版本迁移；logback-spring.xml：日志配置。static/templates/mapper 为预留目录，本项目REST接口无需模板和XML Mapper。

## 业务范围

包含宠主、宠物、地址、喂养师资质/档期、订单计价与快照、人工派单、接拒单、服务清单、媒体元数据、报告状态、会员、优惠券、结算、评价投诉、客服、紧急送医授权、医院、站内通知、权限和后台配置。

智能派单由 Spring Boot 实现，客服、报告等 AI 接口仍为预留。真实支付、微信/短信、对象存储上传与签名播放、实时IM、地图服务未接入；当前支付/退款仅演示模式可用，媒体仅保存对象键等元数据，通知为站内通知。

智能派单接口需要 `dispatch:update` 和订单读取权限：`GET /api/admin/orders/{id}/dispatch-candidates` 返回候选人的分项分数，`POST /api/admin/orders/{id}/llm-dispatch` 让大模型从合格名单中选择喂养师；`POST /api/admin/orders/{id}/auto-dispatch` 保留按分数直接选择的方式。系统每10秒扫描新订单和待派单订单并调用大模型，不依赖支付流程。模型服务故障时订单保留原状态并在后续扫描重试；没有合格候选人时由运营处理。喂养师拒单或接单超时后订单回到待派状态并重新调用模型。

大模型通过 OpenAI 兼容的 `/chat/completions` 接口调用。配置项为 `hualong.base-url`、`hualong.api-key` 和 `hualong.model`；本地 `application-ai-local.properties` 已被 Git 忽略，当前模型为 `gpt-6.1-sol`，可通过 `HUALONG_MODEL` 覆盖。API Key 只在后端读取，不下发到小程序。

喂养师档案增加“服务经验与偏好”（最多1000字），可填写可接大型犬、怕生宠物、性格较凶宠物等经验，以及明确不承接的情况。订单有特殊备注时，派单模型会读取订单备注和候选人的自述偏好，优先匹配明确相关经验，并在派单理由中说明。自述信息仅影响推荐，服务类别、认证、档期和容量等后端硬性条件仍必须通过。

先校验账号、资质、档期、服务半径、容量和时间冲突，并排除该宠主给过一至二星评价的喂养师。总分100分：完成过同一宠物的服务得40分；距离按服务半径线性计算0至30分；等级配置权重和评价得分各占等级项的一半，共20分，其中评价使用5条4星先验平滑；未完成订单数按10/(1+数量)计算0至10分。距离使用订单地址快照和喂养师当日档期位置（没有则用档案位置），要求使用相同坐标系；此处不接入实时定位或地图道路距离。派单按订单、喂养师顺序加锁并重新校验准入。大模型负责基于候选信息选择和给出理由，不能自行添加或覆盖候选人。

数据库设计、初始化SQL及前端联调说明均位于 `D:\CodeDemo\petDemo\1.前期准备`。推荐使用 Flyway 自动迁移，不要再对同一数据库重复执行独立初始化SQL。
