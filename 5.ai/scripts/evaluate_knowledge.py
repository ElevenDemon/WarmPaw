"""Run local retrieval evaluation without calling the language model."""

import argparse
import asyncio
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.knowledge import KnowledgeBase, load_documents


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--include-drafts", action="store_true", help="Offline preview only; never changes online retrieval")
    parser.add_argument("--vector", action="store_true", help="Evaluate the pgvector index using live embeddings")
    args = parser.parse_args()
    chunks = load_documents(include_drafts=args.include_drafts)
    if args.vector:
        from app.vector_store import PgVectorKnowledgeBase
        kb = PgVectorKnowledgeBase(chunks)
    else:
        kb = KnowledgeBase(chunks)
    questions = Path(__file__).resolve().parents[1] / "knowledge/evaluation/questions.jsonl"
    passed = total = 0
    for line in questions.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        question = json.loads(line)
        hits = (asyncio.run(kb.search(question["question"], question["role"]))
                if args.vector else kb.search(question["question"], question["role"]))
        ids = {chunk.doc_id for chunk in hits}
        expected = set(question["expected_doc_ids"])
        ok = (not expected or bool(ids & expected))
        ok &= not bool(ids & set(question.get("forbidden_doc_ids", [])))
        ok &= not question.get("expect_no_results", False) or not hits
        ok &= all(question["role"] in chunk.audience for chunk in hits)
        total += 1
        passed += int(ok)
        print(f"{'PASS' if ok else 'FAIL'} {question['id']}: {', '.join(sorted(ids)) or '(no results)'}")
    print(f"{passed}/{total} passed; {len(chunks)} sections, {len({c.doc_id for c in chunks})} documents")
    print("Retrieval only: factual accuracy and model citation behavior still require answer evaluation.")
    return int(passed != total)


if __name__ == "__main__":
    raise SystemExit(main())
