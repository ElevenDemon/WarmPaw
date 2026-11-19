import uvicorn

from app.api import app
from app.config import get_settings


if __name__ == "__main__":
    settings = get_settings()
    uvicorn.run(app, host=settings.host, port=settings.port)

