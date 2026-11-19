"""List embedding model names only; never prints credentials or model response bodies."""
import asyncio
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import httpx
from app.config import get_settings

async def main():
    settings = get_settings()
    async with httpx.AsyncClient(timeout=20) as client:
        response = await client.get((settings.embedding_base_url or settings.openai_base_url).rstrip('/') + '/models',
                                    headers={'Authorization': 'Bearer ' + (settings.embedding_api_key or settings.openai_api_key)})
        print('Model catalog HTTP status:', response.status_code)
        if response.status_code == 200:
            ids = [item.get('id', '') for item in response.json().get('data', [])]
            print('Embedding candidates:', [name for name in ids if any(term in name.lower() for term in ('embed', 'bge', 'e5'))])

if __name__ == '__main__':
    asyncio.run(main())
