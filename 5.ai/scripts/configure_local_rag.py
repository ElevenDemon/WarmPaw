"""Configure only the documented local database, preserving all existing secrets."""
from pathlib import Path

path = Path(__file__).resolve().parents[1] / ".env"
content = path.read_text(encoding="utf-8") if path.exists() else ""
if not any(line.startswith("RAG_DATABASE_URL=") for line in content.splitlines()):
    with path.open("a", encoding="utf-8") as stream:
        stream.write("\nRAG_DATABASE_URL=postgresql://postgres:root@localhost:5432/pet_demo\n")
    print("Local RAG_DATABASE_URL configured without modifying model credentials")
else:
    print("Existing RAG_DATABASE_URL preserved")
