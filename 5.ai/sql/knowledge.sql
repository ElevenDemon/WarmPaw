-- Run through scripts/ingest_knowledge.py --init-db using a provisioning account.
-- pgvector must already be installed on the PostgreSQL server.
CREATE EXTENSION IF NOT EXISTS vector;
CREATE SCHEMA IF NOT EXISTS ai_knowledge;

CREATE TABLE IF NOT EXISTS ai_knowledge.index_manifest (
    index_name text PRIMARY KEY,
    fingerprint text NOT NULL,
    embedding_identity text NOT NULL,
    dimension integer NOT NULL CHECK (dimension > 0),
    chunk_count integer NOT NULL CHECK (chunk_count >= 0),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_knowledge.chunk (
    index_name text NOT NULL,
    fingerprint text NOT NULL,
    chunk_id text NOT NULL,
    doc_id text NOT NULL,
    title text NOT NULL,
    section text NOT NULL,
    version text NOT NULL,
    audience text[] NOT NULL CHECK (cardinality(audience)>0 AND audience <@ ARRAY['OWNER','SITTER']::text[]),
    content text NOT NULL,
    source text NOT NULL,
    embedding vector NOT NULL,
    PRIMARY KEY (index_name, chunk_id)
);
CREATE INDEX IF NOT EXISTS ai_knowledge_chunk_corpus ON ai_knowledge.chunk(index_name, fingerprint);
CREATE INDEX IF NOT EXISTS ai_knowledge_chunk_audience ON ai_knowledge.chunk USING gin(audience);
