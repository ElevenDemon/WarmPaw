from functools import lru_cache

from pydantic import AliasChoices, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Nuanzhua AI Service"
    app_env: str = "local"
    host: str = "0.0.0.0"
    port: int = 8090
    openai_base_url: str = Field(
        default="https://api.openai.com/v1",
        validation_alias=AliasChoices("HUALONG_BASE_URL", "OPENAI_BASE_URL"),
    )
    openai_api_key: str = Field(
        default="",
        validation_alias=AliasChoices("HUALONG_API_KEY", "OPENAI_API_KEY"),
    )
    openai_model: str = Field(
        default="gpt-4o-mini",
        validation_alias=AliasChoices("HUALONG_MODEL", "OPENAI_MODEL"),
    )
    openai_timeout_seconds: float = 45
    embedding_base_url: str = Field(
        default="",
        validation_alias=AliasChoices("HUALONG_EMBEDDING_BASE_URL", "OPENAI_EMBEDDING_BASE_URL"),
    )
    embedding_api_key: str = Field(
        default="",
        validation_alias=AliasChoices("HUALONG_EMBEDDING_API_KEY", "OPENAI_EMBEDDING_API_KEY"),
    )
    embedding_model: str = Field(
        default="text-embedding-3-small",
        validation_alias=AliasChoices("HUALONG_EMBEDDING_MODEL", "OPENAI_EMBEDDING_MODEL"),
    )
    embedding_timeout_seconds: float = 45
    rag_database_url: str = ""
    rag_index_name: str = "nuanzhua_knowledge"
    vector_min_similarity: float = Field(default=0.35, ge=0, le=1)
    ai_api_key: str = ""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")


@lru_cache
def get_settings() -> Settings:
    return Settings()

