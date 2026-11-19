#!/usr/bin/env bash
# 暖爪 Linux 一键打包 / 部署。Ubuntu 22.04+、Debian 12+，需要联网。
set -Eeuo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$ROOT/deploy"
CONFIG="$DEPLOY_DIR/.env"
ACTION=deploy
IP_OVERRIDE=''
PORT_OVERRIDE=''
LOG_SERVICE=backend

say() { printf '\n[暖爪] %s\n' "$*"; }
die() { printf '\n[错误] %s\n' "$*" >&2; exit 1; }
usage() {
    cat <<'HELP'
用法：bash deploy.sh [命令] [--ip 服务器IPv4] [--port HTTP端口]
  deploy   安装 Docker/Compose，构建镜像、初始化数据库并启动（默认）
  init     仅生成 deploy/.env；可先填写模型和高德密钥
  start    使用已有镜像启动，启用 AI 时检查 / 更新知识索引
  stop     停止本项目服务，保留数据库和上传文件
  status   查看容器状态
  logs     查看后端日志（可通过 --service web/db/ai 切换）
  package  生成 release/nuanzhua-linux.tar.gz，不需要 root / Docker
  help     显示帮助
示例：sudo bash deploy.sh deploy --ip 你的公网IP --port 80
HELP
}
while (($#)); do
    case "$1" in
        deploy|init|start|stop|status|logs|package|help) ACTION="$1"; shift ;;
        --ip) (($# >= 2)) || die '--ip 缺少参数'; IP_OVERRIDE="$2"; shift 2 ;;
        --port) (($# >= 2)) || die '--port 缺少参数'; PORT_OVERRIDE="$2"; shift 2 ;;
        --service) (($# >= 2)) || die '--service 缺少参数'; LOG_SERVICE="$2"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *) die "未知参数：$1" ;;
    esac
done
[[ "$ACTION" != help ]] || { usage; exit 0; }

check_sources() {
    local p
    for p in 2.petVue/package-lock.json 3.petSpringboot/pom.xml 4.uni-app/package-lock.json 5.ai/pyproject.toml deploy/compose.yml; do
        [[ -f "$ROOT/$p" ]] || die "缺少 $p，请上传完整部署包，不要只上传 deploy.sh。"
    done
}
package_source() {
    check_sources
    mkdir -p "$ROOT/release"
    tar --exclude=node_modules --exclude=.npm-cache --exclude=.cache --exclude=unpackage --exclude=target --exclude=dist --exclude=.m2 \
        --exclude=.venv --exclude=.runtime-pg --exclude='*.egg-info' --exclude=venv --exclude=__pycache__ --exclude=.pytest_cache \
        --exclude=.git --exclude=.idea --exclude=.vscode --exclude=.codex --exclude=.agents \
        --exclude=logs --exclude=uploads --exclude=test-results --exclude=backups \
        --exclude='.env*' --exclude='application-ai-local*' --exclude='*.log' \
        --exclude='.deploy.lock' --exclude='*.zip' --exclude='*.tar.gz' \
        -czf "$ROOT/release/nuanzhua-linux.tar.gz" -C "$ROOT" \
        deploy.sh .dockerignore deploy 2.petVue 3.petSpringboot 4.uni-app 5.ai
    say "源码部署包已生成：$ROOT/release/nuanzhua-linux.tar.gz"
}
if [[ "$ACTION" == package ]]; then package_source; exit 0; fi
[[ "${EUID:-$(id -u)}" == 0 ]] || die '请使用 sudo bash deploy.sh 执行。'
check_sources
umask 077

random_secret() { od -An -N24 -tx1 /dev/urandom | tr -d ' \n'; }
# 读取简单 dotenv 值，不 source / eval 配置，避免配置作为 root shell 代码执行。
config_value() {
    local key="$1" line value
    while IFS= read -r line || [[ -n "$line" ]]; do
        line="${line%$'\r'}"
        [[ "$line" == "$key="* ]] || continue
        value="${line#*=}"
        if [[ "$value" == \"*\" || "$value" == \'*\' ]]; then value="${value:1:${#value}-2}"; fi
        printf '%s' "$value"; return
    done < "$CONFIG"
}
set_config() {
    local key="$1" value="$2"
    # 此函数只写入已校验的 IPv4 与数值端口，不能用于任意文本 / 密钥。
    if grep -q "^${key}=" "$CONFIG"; then
        sed -i "s/^${key}=.*/${key}=${value}/" "$CONFIG"
    else printf '\n%s=%s\n' "$key" "$value" >> "$CONFIG"; fi
}
init_config() {
    if [[ ! -f "$CONFIG" ]]; then
        cat > "$CONFIG" <<EOF
# 本文件包含服务端密钥；禁止提交、打包或发给前端。值不需要 export。
SERVER_IP=127.0.0.1
HTTP_PORT=80
BACKEND_HEALTH_PORT=18080
POSTGRES_PASSWORD=$(random_secret)
APP_DB_PASSWORD=$(random_secret)
AI_DB_PASSWORD=$(random_secret)
BOOTSTRAP_PASSWORD=$(random_secret)
AI_SERVICE_API_KEY=$(random_secret)
# 仅正式业务模式。需要演示账号 / 演示支付时改为 true。
DEMO_ENABLED=false
# 外部模型凭据无法自动生成，配置后改为 true 并再次 deploy。
AI_ENABLED=false
HUALONG_BASE_URL=https://api.hualong-ai.com/v1
HUALONG_API_KEY=
HUALONG_MODEL=gpt-6.1-sol
HUALONG_EMBEDDING_BASE_URL=
HUALONG_EMBEDDING_API_KEY=
HUALONG_EMBEDDING_MODEL=text-embedding-3-small
VECTOR_MIN_SIMILARITY=0.35
AMAP_WEB_KEY=
EOF
        say "已生成随机密码配置：$CONFIG；后续运行保留原配置。"
    fi
    chmod 600 "$CONFIG"
}
valid_ip() {
    local part
    local -a octets
    [[ "$1" =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]] || return 1
    IFS=. read -r -a octets <<< "$1"
    for part in "${octets[@]}"; do ((10#$part <= 255)) || return 1; done
}
valid_port() { [[ "$1" =~ ^[1-9][0-9]{0,4}$ ]] && ((10#$1 <= 65535)); }
init_config
if [[ -n "$IP_OVERRIDE" ]]; then
    valid_ip "$IP_OVERRIDE" || die '--ip 必须是有效 IPv4 地址'
    set_config SERVER_IP "$IP_OVERRIDE"
fi
if [[ -n "$PORT_OVERRIDE" ]]; then
    valid_port "$PORT_OVERRIDE" || die '--port 必须在 1–65535 之间'
    set_config HTTP_PORT "$PORT_OVERRIDE"
fi
if [[ "$ACTION" == init ]]; then
    say "配置文件：$CONFIG；填写需要的密钥后执行 sudo bash deploy.sh deploy --ip 你的公网IP"
    exit 0
fi

install_runtime() {
    [[ -f /etc/os-release ]] || die '无法识别 Linux 发行版'
    # 系统维护的发行版信息。
    . /etc/os-release
    case "$ID" in
        ubuntu) (( ${VERSION_ID%%.*} >= 22 )) || die '请使用 Ubuntu 22.04 或更新版本' ;;
        debian) (( ${VERSION_ID%%.*} >= 12 )) || die '请使用 Debian 12 或更新版本' ;;
        *) die "当前自动安装支持 Ubuntu 22.04+ / Debian 12+，检测到：$ID $VERSION_ID" ;;
    esac
    if command -v docker >/dev/null && command -v curl >/dev/null && command -v flock >/dev/null && docker compose version >/dev/null 2>&1; then
        systemctl enable --now docker
        docker info >/dev/null
        return
    fi
    export DEBIAN_FRONTEND=noninteractive
    apt-get update
    apt-get install -y ca-certificates curl util-linux
    if ! command -v docker >/dev/null || ! docker compose version >/dev/null 2>&1; then
        say '安装 Docker 官方软件源及 Compose 插件'
        install -m 0755 -d /etc/apt/keyrings
        curl --fail --show-error --silent --location --retry 3 \
            "https://download.docker.com/linux/$ID/gpg" -o /etc/apt/keyrings/docker.asc
        chmod a+r /etc/apt/keyrings/docker.asc
        cat > /etc/apt/sources.list.d/nuanzhua-docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/$ID
Suites: ${UBUNTU_CODENAME:-$VERSION_CODENAME}
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
        apt-get update
        if ! command -v docker >/dev/null; then
            apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
        else
            apt-get install -y docker-buildx-plugin docker-compose-plugin
        fi
    fi
    command -v systemctl >/dev/null || die '需要支持 systemd 的 Linux 主机'
    systemctl enable --now docker
    docker info >/dev/null
    docker compose version >/dev/null
}
compose() { docker compose --project-name nuanzhua --env-file "$CONFIG" -f "$DEPLOY_DIR/compose.yml" "$@"; }
if [[ "$ACTION" == deploy || "$ACTION" == start ]]; then install_runtime; fi
command -v docker >/dev/null || die '未安装 Docker，请先执行 deploy'
docker compose version >/dev/null 2>&1 || die '缺少 Docker Compose，请先执行 deploy'

case "$ACTION" in
    stop) compose --profile ai stop; exit 0 ;;
    status) compose --profile ai ps -a; exit 0 ;;
    logs)
        case "$LOG_SERVICE" in backend|web|db|ai|ai-init) ;; *) die '日志服务只支持 backend/web/db/ai/ai-init' ;; esac
        compose --profile ai logs --tail=100 -f "$LOG_SERVICE"; exit 0 ;;
esac

exec 9> "$DEPLOY_DIR/.deploy.lock"
flock -n 9 || die '另一个部署正在进行，请等待完成'
export HTTP_PORT="$(config_value HTTP_PORT)"
export SERVER_IP="$(config_value SERVER_IP)"
export BACKEND_HEALTH_PORT="$(config_value BACKEND_HEALTH_PORT)"
AI_ENABLED="$(config_value AI_ENABLED)"
[[ "$AI_ENABLED" == true || "$AI_ENABLED" == false ]] || die 'AI_ENABLED 必须是 true 或 false'
valid_port "$HTTP_PORT" && valid_port "$BACKEND_HEALTH_PORT" || die '配置中的端口无效'
[[ "$HTTP_PORT" != "$BACKEND_HEALTH_PORT" ]] || die 'HTTP_PORT 和 BACKEND_HEALTH_PORT 不能相同'
if [[ "$SERVER_IP" == 127.0.0.1 && -z "$IP_OVERRIDE" ]]; then
    detected="$(hostname -I | awk '{print $1}')"
    if valid_ip "$detected"; then SERVER_IP="$detected"; set_config SERVER_IP "$SERVER_IP"; fi
fi
valid_ip "$SERVER_IP" || die 'SERVER_IP 必须是服务器 IPv4；云服务器请用 --ip 指定公网IP'
if [[ "$AI_ENABLED" == true ]]; then
    [[ -n "${HUALONG_API_KEY:-$(config_value HUALONG_API_KEY)}" ]] || die '启用 AI 前请在 deploy/.env 填写 HUALONG_API_KEY'
fi

trap 'printf "\n[部署失败] 第 %s 行。服务日志：sudo bash deploy.sh logs；不会删除持久化数据。\n" "$LINENO" >&2' ERR
say '验证部署配置'
# quiet 避免把解析后的密码和 API Key 输出到终端。
compose --profile ai config --quiet
if [[ "$ACTION" == deploy ]]; then
    say '构建 Java 21、Node 22 前端与运行镜像（首次下载可能较慢）'
    compose build backend web
    if [[ "$AI_ENABLED" == true ]]; then compose --profile ai build ai-init; fi
fi
say '启动 PostgreSQL 16 + pgvector'
compose up -d --wait --wait-timeout 180 db

if [[ "$AI_ENABLED" == true ]]; then
    say '初始化知识库（语料未变化时复用已有向量），启动 AI 服务'
    compose --profile ai run --rm --no-deps ai-init
    compose --profile ai up -d --wait --wait-timeout 180 ai
else
    # 切换为关闭 AI 时也停止上次已启动的 AI，保留数据库与业务后端。
    compose --profile ai stop ai
    say '未启用 AI：客户端、后台和基础业务继续部署；配置模型密钥后可再次 deploy'
fi

say '启动后端并检查数据库迁移 / 健康状态'
compose up -d backend
ready=false
for ((i=0; i<120; i++)); do
    if curl -fsS --connect-timeout 2 --max-time 3 "http://127.0.0.1:$BACKEND_HEALTH_PORT/api/public/health" -o /dev/null 2>/dev/null; then
        ready=true; break
    fi
    sleep 2
done
[[ "$ready" == true ]] || die '后端未就绪，请执行 sudo bash deploy.sh logs 排查'
say '启动 Nginx，检查 H5、后台和统一 API 入口'
compose up -d --wait --wait-timeout 120 web
for endpoint in / /admin/ /api/public/health; do
    curl --fail --silent --show-error --max-time 15 "http://127.0.0.1:$HTTP_PORT$endpoint" -o /dev/null
done

# 仅开放用户选择的 HTTP 端口；云平台安全组需要在云控制台放行。
if command -v ufw >/dev/null && ufw status | grep -q '^Status: active'; then
    ufw allow "$HTTP_PORT/tcp"
fi
if command -v firewall-cmd >/dev/null && firewall-cmd --state >/dev/null 2>&1; then
    firewall-cmd --permanent --add-port="$HTTP_PORT/tcp"
    firewall-cmd --add-port="$HTTP_PORT/tcp"
fi
suffix=":$HTTP_PORT"; [[ "$HTTP_PORT" != 80 ]] || suffix=''
say "部署完成
客户端：http://$SERVER_IP$suffix/
运营后台：http://$SERVER_IP$suffix/admin/
健康检查：http://$SERVER_IP$suffix/api/public/health
后台账号：admin
初始密码：查看 $CONFIG 中的 BOOTSTRAP_PASSWORD（仅首次建号生效）
AI_ENABLED=$AI_ENABLED
云服务器需在安全组放行 TCP $HTTP_PORT。数据保留在 Docker 命名卷中。
若识别到的是内网IP，请使用 --ip 你的公网IP 再次运行。"
