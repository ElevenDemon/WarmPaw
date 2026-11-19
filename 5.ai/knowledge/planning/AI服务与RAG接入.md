# AI 服务现状与 RAG 接入说明（工程说明，不入库）

> 2026-11-17 更新：以下为初始接入分析，保留作历史设计参考。客服现已接入 OpenAI 兼容 embedding + PostgreSQL pgvector 持久化向量检索，并按 Spring Boot 登录态角色过滤。当前实现、发布标记含义和验证命令以 `../README.md` 为准。服务端密钥与后端身份校验仍是信任边界。

## 当前模块

截至 2026-11-16，`5.ai` 是独立的 Python 3.11+ 服务，主要技术为 FastAPI、Pydantic Settings、httpx 和 LangGraph。配置从服务端 `.env` 加载，聊天模型通过 OpenAI 兼容的 Chat Completions API 调用。

README 暴露健康检查和派单、报告、客服、路线四个接口。当前客服 `POST /api/ai/chat` 接收 `message` 与可选 `context`，调用单节点 LangGraph 工作流，再直接返回 `{"reply": ...}`。客服 system prompt 只有通用平台助手指引；它不会读取 `knowledge/` 文件，也不会查询任何向量数据库。`context` 当前是任意字典，并没有可信角色验证或数据权限语义。

依赖列表没有 embedding 客户端、向量存储或文档解析器。AI 服务 README 也说明服务暂不连接 PostgreSQL。因而“AI 模块已建立”表示基础服务和模型调用代码已建立，并不表示 RAG 已建成。

## 建议的模块边界

- `knowledge/documents/` 是人工维护、审核的唯一问答语料来源；不得在启动时无条件递归读取整个项目。
- 新增 `app/rag/`，封装文档加载/元数据校验、按标题切分、embedding、检索、发布版本管理。离线导入脚本放在 `scripts/`，不要让每个 API 请求重复建立索引。
- 复用现有 `app/llm.py` 的聊天补全封装生成答案；embedding 独立配置模型与客户端。不要假设聊天模型提供 embedding API。
- 在 `app/agents/workflows.py` 为客服工作流增加 retrieval 和 grounded answer 节点；派单、报告和路线仍走现有路径，不应强制经过公共客服语料检索。
- 文档中的 `audience` 用于检索过滤。正式角色必须由 Spring Boot 以经过服务端认证的方式传入，不能从用户文本或请求中的自由字典信任角色。

## 请求和响应契约

当前服务间调用协议尚待与 Spring Boot 确认。建议先形成版本化契约：请求至少包含用户消息、可信角色、会话 ID（如需要）；仅当已通过后端权限校验时才传入最小化的订单事实。不要传递门锁密码、完整地址、病史或不必要的个人信息给模型。

响应保留 `reply`，并添加结构化 `citations`（文档 ID、标题、小节标题、版本）和回答状态（例如已依据知识回答／证据不足转人工）。引用只能由实际召回片段生成，不能让模型伪造文件名或来源。接入前同步更新 Pydantic schema、Spring Boot DTO、前端客服显示和 API 测试。

如果暂时不能修改 Spring Boot 的角色传递协议，可先让 RAG 的 API 只通过服务内的不可公开网络和服务间 API key 访问，检索范围固定为经发布的 `OWNER` 公共帮助文档。不要开放默认 `context` 驱动的内部角色检索。

## 实施次序

1. 确定 embedding 服务/模型与向量存储，并确认本机及部署机的下载、网络、磁盘和持久化条件。
2. 完成 Markdown 元数据解析和发布态校验，初始索引只允许已审核的 `published` 文档。现有文档全是 `draft`，正式索引应为空。
3. 增加入库命令：解析、验证、切分、embedding、幂等 upsert、过期 chunk 清理，并记录模型、语料版本和计数。
4. 以 `evaluation/questions.jsonl` 做召回评测，确认角色过滤、撤回和内容更新，再接入 chat 检索工作流。
5. 与 Spring Boot 约定身份传递、超时、重试、错误处理、引用展示和人工接管，再做端到端验证。

不要在当前项目只有 5 篇草稿时直接宣称客服已具备正式 RAG。可先在独立测试集合验证端到端链路，避免测试内容进入真实客服索引。

## 对现有说明的使用边界

`5.ai/README.md` 是 AI 服务运行和现有 API 的入口说明；本知识目录的 README 说明语料审核；`RAG建设方案.md` 说明通用索引和检索设计。本文记录现有代码与拟议集成之间的差距。代码变化后应同步更新接口路径、schema 和能力状态。
