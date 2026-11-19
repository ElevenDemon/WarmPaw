#!/usr/bin/env bash
set -Eeuo pipefail
# 只在 PostgreSQL 数据卷首次初始化时执行，SQL 参数通过 psql 变量转义。
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=ON_ERROR_STOP=1 \
    --set=app_password="$APP_DB_PASSWORD" --set=ai_password="$AI_DB_PASSWORD" <<'SQL'
CREATE ROLE pet_app LOGIN PASSWORD :'app_password';
GRANT ALL PRIVILEGES ON DATABASE pet_demo TO pet_app;
GRANT ALL ON SCHEMA public TO pet_app;
CREATE ROLE pet_ai LOGIN PASSWORD :'ai_password';
CREATE DATABASE pet_knowledge OWNER pet_ai;
\connect pet_knowledge
CREATE EXTENSION IF NOT EXISTS vector;
CREATE SCHEMA IF NOT EXISTS ai_knowledge AUTHORIZATION pet_ai;
SQL
