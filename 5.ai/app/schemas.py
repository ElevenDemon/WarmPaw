from pydantic import BaseModel, Field


class DispatchCandidate(BaseModel):
    sitter_id: int
    score: float = Field(ge=0, le=100)
    summary: str = ""


class DispatchRequest(BaseModel):
    order_id: int
    order_summary: str = Field(min_length=1, max_length=4000)
    candidates: list[DispatchCandidate] = Field(min_length=1, max_length=100)


class DispatchResponse(BaseModel):
    order_id: int
    sitter_id: int
    reason: str


class ReportRequest(BaseModel):
    order_id: int
    source_snapshot: dict


class ReportResponse(BaseModel):
    order_id: int
    content: str


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=4000)
    context: dict = Field(default_factory=dict)


class ChatResponse(BaseModel):
    reply: str


class RouteStop(BaseModel):
    id: str
    address: str
    latitude: float
    longitude: float
    service_minutes: int = Field(default=30, ge=0, le=720)
    time_window: str | None = None


class RouteRequest(BaseModel):
    stops: list[RouteStop] = Field(min_length=1, max_length=50)
    start_location: str | None = None


class RouteResponse(BaseModel):
    ordered_stop_ids: list[str]
    rationale: str
    limitation: str

