# 暖爪 Linux 部署

支持 **Ubuntu 22.04 / 24.04 及更新版本、Debian 12 / 13 及更新版本**，使用 systemd；推荐至少 2 核、4GB 内存、20GB 可用磁盘。首次安装、构建需要服务器能访问 Docker 官方源 / 镜像仓库、Maven Central、npm 与 PyPI。

脚本自动安装 Docker Engine、Buildx 和 Compose；容器包含 Java 21、Maven、Node 22、Python 3.12、Nginx、PostgreSQL 16 和 pgvector，无需在服务器分别安装这些运行环境。

## 从当前 Windows 项目打包上传

在项目根目录 PowerShell 中运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\打包部署包.ps1
```

得到 `release/nuanzhua-linux.tar.gz`。部署包包含源代码、依赖锁文件及部署配置；排除 node_modules、构建缓存、本地 .env、模型配置、日志、上传文件和本地数据库。首次构建在 Linux 上完成，因此不需要 Windows 的 Java / Python / npm 环境。

也可以在已有 Linux 源码目录运行 `bash deploy.sh package` 生成同样结构的包。不要只上传 deploy.sh，也不要将本地带密钥的配置文件混入上传包。

## Linux 一次部署

将部署包上传到服务器后运行（把示例 IP 换成实际公网 IP）：

```bash
mkdir -p /opt/nuanzhua
tar -xzf nuanzhua-linux.tar.gz -C /opt/nuanzhua
cd /opt/nuanzhua
sudo bash deploy.sh deploy --ip 203.0.113.10
```

部署完成后：

| 地址 | 内容 |
| --- | --- |
| http://服务器IP/ | 宠主 / 喂养师 H5 客户端，按账号角色进入 |
| http://服务器IP/admin/ | 运营后台 |
| http://服务器IP/api/public/health | 业务健康检查 |

后台账号为 `admin`。初始密码首次运行时随机生成，保存在服务器 `deploy/.env` 的 `BOOTSTRAP_PASSWORD` 中，该文件权限为 600；脚本不打印密码。在服务器通过 `sudo nano deploy/.env` 查看 / 修改。修改初始密码配置不会重置已有管理员。

需要其他访问端口时使用 `sudo bash deploy.sh deploy --ip 203.0.113.10 --port 8088`，地址变为 `http://服务器IP:8088/`。宿主机只公开该 HTTP 端口；后端探测端口默认 18080 仅绑定 127.0.0.1，数据库和 AI 不发布宿主机端口。脚本为已开启的 UFW / firewalld 放行 HTTP 端口；云平台安全组需在云控制台放行同一 TCP 端口。

## AI、高德和演示模式

外部模型 / 高德密钥需要使用自己的服务账号，无法由安装脚本生成。基础业务默认正常启动，AI 默认关闭；可以先执行以下命令生成配置、填写密钥，再首次部署：

```bash
sudo bash deploy.sh init
sudo nano deploy/.env
sudo bash deploy.sh deploy --ip 203.0.113.10
```

| 配置项 | 说明 |
| --- | --- |
| AI_ENABLED | `true` 开启 AI 服务、自动构建与入库；默认 `false` |
| HUALONG_BASE_URL / HUALONG_API_KEY / HUALONG_MODEL | OpenAI 兼容模型服务 URL、密钥及可用聊天模型；派单与客服使用同一配置 |
| HUALONG_EMBEDDING_BASE_URL / HUALONG_EMBEDDING_API_KEY | 留空时复用聊天模型地址 / 密钥；独立 embedding 服务时分别填写 |
| HUALONG_EMBEDDING_MODEL | 服务商实际支持的 embedding 模型，默认 text-embedding-3-small |
| AMAP_WEB_KEY | 高德 Web 服务 Key，配置后可用地点搜索与地址解析 |
| DEMO_ENABLED | 默认 `false`；改为 `true` 可创建 owner / sitter 演示账号并开启演示支付 / 退款，演示账号初始密码同 BOOTSTRAP_PASSWORD |

启用 AI 时自动准备独立 `pet_knowledge` 数据库、vector 扩展和知识 schema，再运行已发布语料入库，成功后启动 AI。首次入库或语料 / embedding 模型变化会调用外部 embedding API；相同指纹复用现有向量。调用失败会停止本次部署并返回错误，不将 AI 标记为成功。业务数据库与知识库使用不同账号；模型密钥不打进镜像、不进入浏览器构建。

上线后通过 HTTP IP 使用的是 H5；微信小程序正式请求和设备浏览器精确定位通常需要 HTTPS / 合法域名，这部分需绑定域名与证书。真实支付、录像 / 直播、第三方消息不会因为部署脚本运行而自动接入。

## 更新、开机启动与排错

容器随 Docker 开机启动。更新源码后在原目录执行 `sudo bash deploy.sh deploy --ip 服务器IP`，复用现有 `deploy/.env` 和 Docker 数据卷；上传新部署包也不会覆盖已有 .env。更新包含数据库迁移时应先备份数据库。

```bash
sudo bash deploy.sh status
sudo bash deploy.sh logs
sudo bash deploy.sh logs --service web
sudo bash deploy.sh logs --service ai
sudo bash deploy.sh stop       # 停止服务，保留数据
sudo bash deploy.sh start      # 使用已有镜像启动；缺少镜像时先 deploy
```

PostgreSQL、媒体及后端日志分别保存于 `nuanzhua_postgres-data`、`nuanzhua_media-data`、`nuanzhua_backend-logs` 命名卷。不要执行 `docker compose down -v` 删除数据库。首次数据库启动才创建账号，之后不要直接改 .env 的数据库密码；轮换密码需同步修改数据库角色。若主机已有 Docker 数据，不会被脚本清理。

如果 Docker 官方源 / Docker Hub 在服务器网络不可达，需先配置可用的镜像源或代理；脚本不会替换为来源不明的镜像。80 端口被占用时使用 `--port` 换端口；18080 冲突时在 .env 调整 BACKEND_HEALTH_PORT。构建失败保留错误退出码；后端启动失败查看 backend / db 日志，AI 入库失败检查模型 / embedding 权限和出站网络。

交付验证：已通过两份 Bash 脚本语法检查、5 个服务的 Compose YAML 结构与路径检查、运营后台 /admin/ 生产构建、H5 生产构建，以及部署包排除项和关键文件一致性检查。本机没有可运行的 Linux Docker 环境，完整容器安装、镜像拉取、数据库初始化及公网访问仍需在目标 Linux 服务器执行验证。
