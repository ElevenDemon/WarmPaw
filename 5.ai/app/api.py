import json
from contextlib import asynccontextmanager

from fastapi import FastAPI, Header, HTTPException

from app.agents.workflows import agent_graph, dispatch, report
from app.config import get_settings
from app.llm import LLMError
from app.vector_store import get_vector_knowledge_base
from app.embeddings import EmbeddingError
from app.vector_store import VectorStoreError
from app.schemas import (
    ChatRequest,
    ChatResponse,
    DispatchRequest,
    DispatchResponse,
    ReportRequest,
    ReportResponse,
    RouteRequest,
    RouteResponse,
)

settings = get_settings()
@asynccontextmanager
async def lifespan(app: FastAPI):
    get_vector_knowledge_base()  # Validate that the published corpus has a matching vector index.
    yield


app = FastAPI(title=settings.app_name, version="0.1.0", lifespan=lifespan)


def check_api_key(provided: str | None) -> None:
    expected = get_settings().ai_api_key
    if expected and provided != expected:
        raise HTTPException(status_code=401, detail="Invalid AI service API key")


@app.get("/health")
async def health():
    return {"status": "ok", "service": "ai"}


@app.post("/api/ai/dispatch", response_model=DispatchResponse)
async def dispatch_endpoint(request: DispatchRequest, x_ai_api_key: str | None = Header(default=None)):
    check_api_key(x_ai_api_key)
    try:
        return await dispatch(request)
    except LLMError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@app.post("/api/ai/report", response_model=ReportResponse)
async def report_endpoint(request: ReportRequest, x_ai_api_key: str | None = Header(default=None)):
    check_api_key(x_ai_api_key)
    try:
        return await report(request)
    except LLMError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc


@app.post("/api/ai/chat", response_model=ChatResponse)
async def chat_endpoint(request: ChatRequest, x_ai_api_key: str | None = Header(default=None)):
    check_api_key(x_ai_api_key)
    try:
        from app.knowledge import chat_payload
        payload = request.model_dump()
        payload = await chat_payload(payload["message"], payload.get("context", {}))
        result = await agent_graph.ainvoke({"kind": "chat", "payload": payload, "result": ""})
        return ChatResponse(reply=result["result"].strip())
    except (EmbeddingError, VectorStoreError) as exc:
        raise HTTPException(status_code=503, detail="知识检索暂时不可用，请稍后重试或联系人工客服") from exc
    except LLMError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc


@app.post("/api/ai/route", response_model=RouteResponse)
async def route_endpoint(request: RouteRequest, x_ai_api_key: str | None = Header(default=None)):
    check_api_key(x_ai_api_key)
    try:
        result = await agent_graph.ainvoke({"kind": "route", "payload": request.model_dump(), "result": ""})
        parsed = json.loads(result["result"])
        ids = [stop.id for stop in request.stops]
        ordered = parsed["ordered_stop_ids"]
        if len(ordered) != len(ids) or set(ordered) != set(ids):
            raise ValueError("Model returned an invalid stop ordering")
        return RouteResponse(
            ordered_stop_ids=ordered,
            rationale=parsed.get("rationale", ""),
            limitation="仅依据输入信息排序，不包含实时路况或道路导航。",
        )
    except LLMError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except (ValueError, KeyError, TypeError, json.JSONDecodeError) as exc:
        raise HTTPException(status_code=502, detail="Model returned an invalid route ordering") from exc

