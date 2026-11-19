"""Local, role-filtered Markdown retrieval. No model API or external index required."""

from collections import Counter
from dataclasses import dataclass
from functools import lru_cache
import math
from pathlib import Path
import re

import yaml


DOCUMENTS = Path(__file__).resolve().parents[1] / "knowledge" / "documents"
ROLES = {"OWNER", "SITTER"}
STOP_TERMS = set("帮我 一下 多少 怎么 什么 如何 为什么 可以 不能 需要 是否 现在 明天 今天 一个 一次 已经 还是 时候 这里 这个 那个 如果 不是 就是 我是 告诉".split())
ALIASES = (
    ("下单", "预约"), ("喂猫", "喂养"), ("喂狗", "喂养"),
    ("半小时", "三十分钟", "30分钟"), ("没付款", "待付款"),
    ("不舒服", "异常"), ("人工", "客服"), ("收入", "收益", "结算"),
)
FOLLOW_UP_MARKERS = ("那", "它", "这个", "那个", "刚才", "前面", "然后", "呢", "多久", "还可以", "怎么处理")
MAX_HISTORY_MESSAGES = 12
MAX_HISTORY_MESSAGE_CHARS = 1800
MAX_HISTORY_TOTAL_CHARS = 8000


def tokens(text: str) -> Counter:
    text = text.lower()
    for group in ALIASES:
        if any(term in text for term in group):
            text += " " + " ".join(group)
    parts = re.findall(r"[a-z0-9]+|[\u4e00-\u9fff]+", text)
    return Counter(token for part in parts for token in (
        [part] if not re.fullmatch(r"[\u4e00-\u9fff]+", part)
        else [part[i:i + 2] for i in range(len(part) - 1)]
    ) if token not in STOP_TERMS)


@dataclass(frozen=True)
class Chunk:
    doc_id: str
    title: str
    section: str
    version: str
    audience: tuple[str, ...]
    content: str
    source: str

    def evidence(self) -> dict:
        return {"doc_id": self.doc_id, "title": self.title, "section": self.section,
                "version": self.version, "source": self.source, "content": self.content}


def load_documents(directory: Path = DOCUMENTS, *, include_drafts: bool = False) -> tuple[Chunk, ...]:
    chunks = []
    seen = set()
    paths = sorted(directory.glob("*.md"))
    if not paths:
        raise ValueError(f"No knowledge documents found: {directory}")
    for path in paths:
        raw = path.read_text(encoding="utf-8-sig")
        match = re.match(r"\A---\s*\n(.*?)\n---\s*\n(.*)\Z", raw, re.S)
        if not match:
            raise ValueError(f"Missing Markdown metadata: {path.name}")
        metadata = yaml.safe_load(match[1])
        required = {"id", "title", "version", "audience", "status", "updated_at", "sources"}
        if not isinstance(metadata, dict) or not required <= metadata.keys():
            raise ValueError(f"Incomplete metadata: {path.name}")
        doc_id = metadata["id"]
        if not isinstance(doc_id, str) or not doc_id.strip() or doc_id in seen:
            raise ValueError(f"Invalid or duplicate document id: {path.name}")
        seen.add(doc_id)
        audience = metadata["audience"]
        if not isinstance(audience, list) or not audience or not set(audience) <= ROLES:
            raise ValueError(f"Invalid audience: {path.name}")
        if metadata["status"] not in {"draft", "published", "archived"}:
            raise ValueError(f"Invalid status: {path.name}")
        if metadata["status"] == "published" and not metadata.get("reviewed_by"):
            raise ValueError(f"Published document requires reviewer: {path.name}")
        if metadata["status"] != "published" and not (include_drafts and metadata["status"] == "draft"):
            continue
        sections = re.split(r"(?m)^## (.+)\n", match[2])
        if len(sections) == 1:
            raise ValueError(f"Document has no question sections: {path.name}")
        for heading, content in zip(sections[1::2], sections[2::2]):
            if not content.strip() or len(content) > 6000:
                raise ValueError(f"Empty or oversized section: {path.name}: {heading}")
            chunks.append(Chunk(doc_id, str(metadata["title"]), heading.strip(),
                                str(metadata["version"]), tuple(audience), content.strip(), path.name))
    return tuple(chunks)


@lru_cache(maxsize=1)
def get_knowledge_base() -> "KnowledgeBase":
    return KnowledgeBase(load_documents())


class KnowledgeBase:
    def __init__(self, chunks: tuple[Chunk, ...]):
        self.chunks = chunks

    def search(self, query: str, role: str, limit: int = 5) -> list[Chunk]:
        if not isinstance(role, str) or role not in ROLES or not query.strip():
            return []
        # Apply authorization before scoring; other roles never enter the candidate set.
        candidates = [chunk for chunk in self.chunks if role in chunk.audience]
        query_terms = tokens(query)
        if not candidates or not query_terms:
            return []
        terms = [tokens(c.section + " " + c.section + " " + c.content) for c in candidates]
        frequencies = Counter(term for document in terms for term in document)
        average_length = sum(sum(t.values()) for t in terms) / len(terms)
        ranked = []
        for chunk, document in zip(candidates, terms):
            overlap = query_terms.keys() & document.keys()
            if len(overlap) < 2:
                continue
            score = 0.0
            for term in overlap:
                df = frequencies[term]
                idf = math.log(1 + (len(terms) - df + 0.5) / (df + 0.5))
                tf = document[term]
                score += idf * tf * 2.2 / (tf + 1.2 * (0.25 + 0.75 * sum(document.values()) / average_length))
            ranked.append((score, chunk))
        ranked.sort(key=lambda pair: pair[0], reverse=True)
        return [chunk for _, chunk in ranked[:max(0, limit)]]


async def chat_payload(message: str, context: dict, knowledge_base=None) -> dict:
    # Only preserve agreed fields; callers cannot inject their own retrieved evidence.
    role = context.get("role")
    if not isinstance(role, str) or role not in ROLES:
        role = "UNKNOWN"
    history = context.get("history", [])
    if not isinstance(history, list):
        history = []
    recent_history = [{"role": row["role"], "content": row["content"][:MAX_HISTORY_MESSAGE_CHARS]}
                      for row in history[-MAX_HISTORY_MESSAGES:] if isinstance(row, dict)
                      and row.get("role") in {"user", "assistant"} and isinstance(row.get("content"), str)]
    history = []
    remaining = MAX_HISTORY_TOTAL_CHARS
    for row in reversed(recent_history):
        content = row["content"][:remaining]
        if not content:
            break
        history.append({"role": row["role"], "content": content})
        remaining -= len(content)
    history.reverse()
    if knowledge_base is None:
        from app.vector_store import get_vector_knowledge_base
        knowledge_base = get_vector_knowledge_base()
    retrieval_query = message
    is_follow_up = len(message) <= 40 and any(marker in message for marker in FOLLOW_UP_MARKERS)
    if is_follow_up and history:
        memory = "\n".join(f"{row['role']}: {row['content']}" for row in history[-4:])
        retrieval_query = f"{memory}\nuser: {message}"
    matches = await knowledge_base.search(retrieval_query, role)
    return {"message": message, "context": {"role": role if role in ROLES else "UNKNOWN", "history": history},
            "knowledge": [chunk.evidence() for chunk in matches]}
