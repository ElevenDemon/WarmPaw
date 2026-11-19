"""Transactional, versioned PostgreSQL / pgvector knowledge storage."""

import asyncio
from dataclasses import asdict
from functools import lru_cache
import hashlib
import json
import math
from pathlib import Path

from app.config import get_settings
from app.embeddings import embed_texts
from app.knowledge import Chunk, KnowledgeBase, ROLES

SQL_PATH = Path(__file__).resolve().parents[1] / "sql" / "knowledge.sql"


class VectorStoreError(RuntimeError):
    pass


def embedding_identity() -> str:
    settings = get_settings()
    # Model aliases on different providers may produce incompatible vectors.
    return f"{(settings.embedding_base_url or settings.openai_base_url).rstrip('/')}|{settings.embedding_model}"


def fingerprint(chunks: tuple[Chunk, ...], identity: str) -> str:
    payload = json.dumps({"format": 1, "embedding": identity, "chunks": [asdict(c) for c in chunks]},
                         ensure_ascii=False, sort_keys=True)
    return hashlib.sha256(payload.encode()).hexdigest()


def chunk_id(chunk: Chunk) -> str:
    return hashlib.sha256(f"{chunk.doc_id}\0{chunk.section}".encode()).hexdigest()


def validate_vectors(vectors: list[list[float]], count: int, dimension: int | None = None) -> int:
    if len(vectors) != count or not vectors:
        raise VectorStoreError("Embedding response count mismatch")
    dimension = dimension or len(vectors[0])
    for vector in vectors:
        if len(vector) != dimension or not dimension or not all(math.isfinite(v) for v in vector):
            raise VectorStoreError("Embedding dimensions or values are invalid")
        if not any(vector):
            raise VectorStoreError("Zero embedding vector is invalid for cosine search")
    return dimension


def vector_literal(vector: list[float]) -> str:
    return "[" + ",".join(str(float(value)) for value in vector) + "]"


def connect():
    import psycopg
    settings = get_settings()
    if not settings.rag_database_url:
        raise VectorStoreError("RAG_DATABASE_URL is not configured")
    try:
        return psycopg.connect(settings.rag_database_url, connect_timeout=5)
    except psycopg.Error:
        raise VectorStoreError("Cannot connect to RAG PostgreSQL database") from None


def initialize_database() -> None:
    import psycopg
    try:
        with connect() as connection:
            connection.execute(SQL_PATH.read_text(encoding="utf-8"))
    except psycopg.Error:
        raise VectorStoreError("RAG schema setup failed; install pgvector on the PostgreSQL server and enable CREATE EXTENSION vector with an authorized database account") from None


async def build_index(chunks: tuple[Chunk, ...], *, embedder=embed_texts, batch_size: int = 32) -> dict:
    if batch_size < 1:
        raise ValueError("batch_size must be positive")
    if not chunks:
        raise VectorStoreError("No published sections to index")
    settings = get_settings()
    identity = embedding_identity()
    digest = fingerprint(chunks, identity)
    # Schema must be explicitly initialized by the deployment / ingest command.
    with connect() as connection:
        row = connection.execute(
            "SELECT fingerprint, dimension, chunk_count FROM ai_knowledge.index_manifest WHERE index_name=%s",
            (settings.rag_index_name,),
        ).fetchone()
    if row and row[0] == digest and row[2] == len(chunks):
        return {"fingerprint": digest, "sections": row[2], "dimension": row[1], "reused": True}
    vectors = []
    dimension = None
    for start in range(0, len(chunks), batch_size):
        batch = chunks[start:start + batch_size]
        output = await embedder([f"{c.title}\n{c.section}\n{c.content}" for c in batch])
        dimension = validate_vectors(output, len(batch), dimension)
        vectors.extend(output)
    # Network calls finish before starting the write transaction. Replacement is atomic;
    # failed embedding/SQL preserves the complete old index and its manifest.
    with connect() as connection:
        connection.execute("SELECT pg_advisory_xact_lock(hashtext(%s))", (settings.rag_index_name,))
        connection.execute("DELETE FROM ai_knowledge.chunk WHERE index_name=%s", (settings.rag_index_name,))
        with connection.cursor() as cursor:
            cursor.executemany(
                """INSERT INTO ai_knowledge.chunk
                (index_name, fingerprint, chunk_id, doc_id, title, section, version, audience, content, source, embedding)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s::vector)""",
                [(settings.rag_index_name, digest, chunk_id(c), c.doc_id, c.title, c.section, c.version,
                  list(c.audience), c.content, c.source, vector_literal(v)) for c, v in zip(chunks, vectors)],
            )
        connection.execute(
            """INSERT INTO ai_knowledge.index_manifest
            (index_name, fingerprint, embedding_identity, dimension, chunk_count)
            VALUES (%s,%s,%s,%s,%s) ON CONFLICT (index_name) DO UPDATE SET
            fingerprint=excluded.fingerprint, embedding_identity=excluded.embedding_identity,
            dimension=excluded.dimension, chunk_count=excluded.chunk_count, updated_at=now()""",
            (settings.rag_index_name, digest, identity, dimension, len(chunks)),
        )
    return {"fingerprint": digest, "sections": len(chunks), "dimension": dimension, "reused": False}


class PgVectorKnowledgeBase:
    def __init__(self, chunks: tuple[Chunk, ...]):
        self.chunks = chunks
        self.identity = embedding_identity()
        self.digest = fingerprint(chunks, self.identity)
        self.index_name = get_settings().rag_index_name
        with connect() as connection:
            row = connection.execute(
                "SELECT fingerprint, dimension, chunk_count FROM ai_knowledge.index_manifest WHERE index_name=%s",
                (self.index_name,),
            ).fetchone()
        if not row or row[0] != self.digest or row[2] != len(chunks):
            raise VectorStoreError("Knowledge index missing or stale; run scripts/ingest_knowledge.py")
        self.dimension = row[1]
        self.lexical = KnowledgeBase(chunks)

    def _query(self, vector: list[float], role: str, limit: int) -> list[Chunk]:
        settings = get_settings()
        # Role and corpus fingerprint are filtered in SQL before ranking.
        # Exact cosine scan is intentional for the current 73 sections; no ANN recall loss.
        with connect() as connection:
            rows = connection.execute(
                """SELECT doc_id,title,section,version,audience,content,source
                FROM ai_knowledge.chunk
                WHERE index_name=%s AND fingerprint=%s AND %s=ANY(audience)
                  AND 1-(embedding <=> %s::vector) >= %s
                ORDER BY embedding <=> %s::vector LIMIT %s""",
                (self.index_name, self.digest, role, vector_literal(vector), settings.vector_min_similarity,
                 vector_literal(vector), limit),
            ).fetchall()
            # A running process with an old corpus fails closed after an index replacement.
            current = connection.execute("SELECT fingerprint FROM ai_knowledge.index_manifest WHERE index_name=%s",
                                         (self.index_name,)).fetchone()
        if not current or current[0] != self.digest:
            raise VectorStoreError("Knowledge index changed; restart the AI service")
        return [Chunk(row[0], row[1], row[2], row[3], tuple(row[4]), row[5], row[6]) for row in rows]

    async def search(self, query: str, role: str, limit: int = 5) -> list[Chunk]:
        if not isinstance(role, str) or role not in ROLES or not query.strip() or limit <= 0:
            return []
        vector = (await embed_texts([query]))[0]
        validate_vectors([vector], 1, self.dimension)
        dense = await asyncio.to_thread(self._query, vector, role, max(limit * 3, 10))
        # Require semantic evidence before adding lexical candidates. BM25 cannot
        # bypass the similarity cutoff or SQL role filter.
        if not dense:
            return []
        scores = {}
        candidates = {(c.doc_id, c.section): c for c in dense}
        for ranking in (dense, self.lexical.search(query, role, max(limit * 3, 10))):
            for rank, chunk in enumerate(ranking, 1):
                key = (chunk.doc_id, chunk.section)
                if key in candidates:
                    scores[key] = scores.get(key, 0) + 1 / (60 + rank)
        return [candidates[key] for key in sorted(scores, key=scores.get, reverse=True)[:limit]]


@lru_cache(maxsize=1)
def get_vector_knowledge_base() -> PgVectorKnowledgeBase:
    from app.knowledge import load_documents
    return PgVectorKnowledgeBase(load_documents())
