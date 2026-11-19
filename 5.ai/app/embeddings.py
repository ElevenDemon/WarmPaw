"""OpenAI-compatible embedding client with strict response validation."""

import asyncio
import math

import httpx

from app.config import get_settings


class EmbeddingError(RuntimeError):
    pass


async def embed_texts(texts: list[str]) -> list[list[float]]:
    if not texts:
        return []
    settings = get_settings()
    base_url = settings.embedding_base_url or settings.openai_base_url
    api_key = settings.embedding_api_key or settings.openai_api_key
    if not api_key:
        raise EmbeddingError("Embedding API key is not configured")
    try:
        async with httpx.AsyncClient(timeout=settings.embedding_timeout_seconds) as client:
            response = await client.post(
                f"{base_url.rstrip('/')}/embeddings",
                headers={"Authorization": f"Bearer {api_key}"},
                json={"model": settings.embedding_model, "input": texts},
            )
            response.raise_for_status()
            data = response.json()["data"]
        if not isinstance(data, list) or len(data) != len(texts):
            raise ValueError("Embedding response count does not match request")
        ordered = sorted(data, key=lambda item: item["index"])
        if [item["index"] for item in ordered] != list(range(len(texts))):
            raise ValueError("Embedding indexes are invalid")
        vectors = []
        dimension = None
        for item in ordered:
            vector = [float(value) for value in item["embedding"]]
            if not vector or not all(math.isfinite(value) for value in vector):
                raise ValueError("Embedding vector is empty or non-finite")
            if not any(vector):
                raise ValueError("Embedding vector is zero")
            if dimension is None:
                dimension = len(vector)
            if len(vector) != dimension:
                raise ValueError("Embedding dimensions are inconsistent")
            vectors.append(vector)
        return vectors
    except httpx.HTTPStatusError as exc:
        raise EmbeddingError(f"Embedding provider returned HTTP {exc.response.status_code}; verify endpoint, model and account access") from exc
    except (httpx.HTTPError, KeyError, IndexError, TypeError, ValueError) as exc:
        raise EmbeddingError("Embedding request failed or returned an invalid response") from exc


def embed_batch(texts: list[str]) -> list[list[float]]:
    return asyncio.run(embed_texts(texts))
