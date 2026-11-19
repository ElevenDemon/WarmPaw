"""Embed published knowledge and atomically replace its pgvector index."""

import argparse
import asyncio
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.config import get_settings
from app.embeddings import EmbeddingError
from app.knowledge import load_documents
from app.vector_store import build_index, initialize_database, VectorStoreError


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--init-db", action="store_true", help="Enable vector extension and create the ai_knowledge schema")
    args = parser.parse_args()
    settings = get_settings()
    chunks = load_documents()
    if not chunks:
        parser.error("No published sections found")
    try:
        if args.init_db:
            initialize_database()
        result = asyncio.run(build_index(chunks))
    except (EmbeddingError, VectorStoreError, ValueError) as exc:
        print(f"Index build failed: {exc}", file=sys.stderr)
        return 1
    print(f"Indexed {len(chunks)} sections from {len({chunk.doc_id for chunk in chunks})} documents")
    print(f"Embedding model: {settings.embedding_model}")
    print(f"Index: {settings.rag_index_name}")
    print(f"Fingerprint: {result['fingerprint']}")
    print(f"Dimensions: {result['dimension']}; reused: {result['reused']}")
    print("Storage: PostgreSQL ai_knowledge.chunk (pgvector)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
