"""Read-only database/embedding configuration check; never prints credentials."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import psycopg
from app.config import get_settings

settings = get_settings()
print("Embedding model:", settings.embedding_model)
print("Embedding credential configured:", bool(settings.embedding_api_key or settings.openai_api_key))
dsn = settings.rag_database_url
if not dsn:
    print("RAG_DATABASE_URL missing; checking documented local development database")
    dsn = "postgresql://postgres:root@localhost:5432/pet_demo"
try:
    with psycopg.connect(dsn, connect_timeout=5) as connection:
        print("PostgreSQL connected")
        print("pgvector available:", connection.execute("SELECT name,installed_version FROM pg_available_extensions WHERE name='vector'").fetchall())
        print("PostgreSQL version:", connection.execute("SHOW server_version").fetchone()[0])
except psycopg.Error as exc:
    print("Database check failed:", type(exc).__name__)
    sys.exit(1)
