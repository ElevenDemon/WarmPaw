FROM python:3.12-slim-bookworm
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PIP_NO_CACHE_DIR=1
WORKDIR /app
COPY 5.ai/pyproject.toml ./
COPY 5.ai/app ./app
RUN python -m pip install --upgrade pip && python -m pip install .
COPY 5.ai/scripts ./scripts
COPY 5.ai/sql ./sql
COPY 5.ai/knowledge/documents ./knowledge/documents
USER 10001:10001
EXPOSE 8090
CMD ["python", "-m", "uvicorn", "app.api:app", "--host", "0.0.0.0", "--port", "8090"]
