# 暖爪 AI Agent 服务

独立 Python 服务，提供派单建议、喂养报告、客服对话和路线排序能力。小程序客服已通过 Spring Boot 的 `/api/ai/chat` 代理接入；其他能力的业务接入仍需后续联调。

## 框架选择

- **FastAPI**：适合独立 AI 微服务，异步 I/O、Pydantic 校验、OpenAPI 文档开箱可用。
- **LangGraph**：把 agent 步骤表达为状态图；当前工作流刻意保持单节点，后续需要人工审核、重试、工具调用或持久化状态时再增加节点。
- **OpenAI 兼容 Chat Completions**：模型接入集中在 `app/llm.py`，可配置现有兼容服务的地址和模型。

现阶段不建议引入多 agent 协作框架。派单、报告等任务边界清楚，先用单 agent 工作流更容易测试和控制。派单候选资格必须由业务后端计算，模型仅能从白名单候选中选择，最终写入仍应由 Spring Boot 再校验。

## 本地运行

要求 Python 3.11 或更高版本。

```powershell
cd D:\CodeDemo\petDemo\5.ai
py -m venv .venv
.venv\Scripts\Activate.ps1
python -m pip install -e .
Copy-Item .env.example .env
```

编辑 `.env` 设置 `HUALONG_BASE_URL`、`HUALONG_API_KEY`、`HUALONG_MODEL`。也兼容 `OPENAI_BASE_URL`、`OPENAI_API_KEY`、`OPENAI_MODEL`。生产环境建议同时设置 `AI_API_KEY`，Spring Boot 调用时通过 `X-AI-API-Key` 传递。模型密钥仅放服务端环境变量，不下发前端。

```powershell
python -m app.main
```

- 健康检查：`http://localhost:8090/health`
- OpenAPI：`http://localhost:8090/docs`
- AI 路由：`POST /api/ai/dispatch`、`/api/ai/report`、`/api/ai/chat`、`/api/ai/route`

派单请求需由后端传入已通过资质、档期、服务半径、容量等校验的候选人及分数；服务验证模型选出的 sitter_id 属于该候选列表。路线接口只做站点排序建议，不提供地图路线、道路距离或实时交通能力。

## 当前边界

客服使用 pgvector embedding 检索：11 篇、73 条问答，按 OWNER / SITTER 身份过滤，并融合关键词检索。短期记忆来自当前客服会话最近 12 条消息，追问检索会结合最近几轮；不会跨会话保留，历史 AI 回复也不作为政策依据。首次部署需要管理员为目标 PostgreSQL 安装 pgvector，并通过 `RAG_DATABASE_URL` 配置 AI 服务连接专用数据库；配置 embedding 模型及服务端密钥后，在 `5.ai` 目录运行 `python scripts/ingest_knowledge.py --init-db`。该操作在该数据库启用 vector 扩展并新建专用 `ai_knowledge` schema。回复要求注明标题、小节与版本；客服没有实时订单查询或业务执行能力。详见 [knowledge/README.md](knowledge/README.md)。

更新项目后在有效的 Python 虚拟环境执行 `python -m pip install -e .` 安装 PostgreSQL 驱动。语料或 embedding 模型变化后重新入库，再重启服务；启动时会检查当前语料对应的索引。向量验收使用 `python scripts/evaluate_knowledge.py --vector`；每次向量评测及在线检索都会调用 embedding 服务。

宠主与喂养师共用智能客服页面及 `/api/ai/chat`。Spring Boot 从登录态传入 `context.role`，会话按发起人账号隔离；喂养师端可从底部导航或「我的」进入。会话表的 `owner_id` 为兼容已有接口保留，实际表示发起人账号 ID。


服务暂不连接 PostgreSQL，也不消费报告回调。小程序客服通过 Spring Boot 代理访问 Python `/api/ai/chat`；业务登录态和聊天记录仍由 Spring Boot 管理，`AI_SERVICE_BASE_URL` 与 `AI_SERVICE_API_KEY` 配置服务间调用。报告生成结果由调用方负责保存，并将 `feeding_report.status` 从 `PENDING_AI` 推进为 `READY` 或按业务规则处理失败。
