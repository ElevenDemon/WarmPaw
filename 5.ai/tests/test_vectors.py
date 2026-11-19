import math
import unittest
from types import SimpleNamespace
from unittest.mock import AsyncMock, patch

import httpx

from app.embeddings import EmbeddingError, embed_texts
from app.knowledge import Chunk, KnowledgeBase
from app.vector_store import PgVectorKnowledgeBase, VectorStoreError, fingerprint, validate_vectors


class EmbeddingTests(unittest.IsolatedAsyncioTestCase):
    async def call_provider(self, data):
        response = httpx.Response(200, json={"data": data}, request=httpx.Request("POST", "https://example.test/embeddings"))
        client = AsyncMock()
        client.post.return_value = response
        settings = SimpleNamespace(embedding_base_url="https://example.test", openai_base_url="",
                                   embedding_api_key="test", openai_api_key="", embedding_timeout_seconds=2,
                                   embedding_model="test-model")
        with patch("app.embeddings.get_settings", return_value=settings), patch("app.embeddings.httpx.AsyncClient") as factory:
            factory.return_value.__aenter__.return_value = client
            return await embed_texts(["first", "second"])

    async def test_ordered_by_response_index(self):
        vectors = await self.call_provider([{"index": 1, "embedding": [0, 1]}, {"index": 0, "embedding": [1, 0]}])
        self.assertEqual(vectors, [[1., 0.], [0., 1.]])

    async def test_malformed_vectors_and_duplicate_indexes_rejected(self):
        for data in ([{"index": 0, "embedding": [1]}, {"index": 0, "embedding": [1]}],
                     [{"index": 0, "embedding": [1]}, {"index": 1, "embedding": [1, 2]}],
                     [{"index": 0, "embedding": [0]}, {"index": 1, "embedding": [1]}],
                     [{"index": 0, "embedding": ["Infinity"]}, {"index": 1, "embedding": [1]}]):
            with self.assertRaises(EmbeddingError):
                await self.call_provider(data)

    async def test_role_rejected_before_embedding_and_hybrid_keeps_semantic_cutoff(self):
        chunk = Chunk("test", "预约", "预约说明", "1", ("OWNER",), "预约上门地址", "test.md")
        kb = object.__new__(PgVectorKnowledgeBase)
        kb.dimension = 2
        kb.lexical = KnowledgeBase((chunk,))
        with patch("app.vector_store.embed_texts", new=AsyncMock(return_value=[[1, 0]])) as embedding:
            self.assertEqual(await kb.search("预约", "ADMIN"), [])
            embedding.assert_not_called()
            with patch.object(kb, "_query", return_value=[]):
                self.assertEqual(await kb.search("预约上门", "OWNER"), [])
            with patch.object(kb, "_query", return_value=[chunk]):
                self.assertEqual(await kb.search("预约上门", "OWNER"), [chunk])


class VectorValidationTests(unittest.TestCase):
    def test_dimension_and_fingerprint(self):
        self.assertEqual(validate_vectors([[1, 0]], 1), 2)
        for vectors, count, dimension in (([[1]], 2, None), ([[0, 0]], 1, None), ([[1]], 1, 2)):
            with self.assertRaises(VectorStoreError):
                validate_vectors(vectors, count, dimension)
        self.assertNotEqual(fingerprint((), "provider-a/model"), fingerprint((), "provider-b/model"))


if __name__ == "__main__":
    unittest.main()
