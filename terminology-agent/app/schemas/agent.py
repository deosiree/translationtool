"""Pydantic schemas for the terminology learning agent API."""

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


# ── Request schemas ──


class TermLearningRunRequest(BaseModel):
    """Request body for POST /agent/term-learning/run."""
    source_text: str = Field(..., min_length=1, max_length=1024, description="The Chinese term to check")
    context: Optional[str] = Field(None, max_length=2048, description="Optional surrounding context (sentence, UI area, etc.)")


class TermReviewRequest(BaseModel):
    """Request body for POST /agent/term-learning/{id}/review."""
    action: str = Field(..., pattern="^(approved|rejected)$", description="Review decision: 'approved' or 'rejected'")
    comment: Optional[str] = Field(None, max_length=512, description="Optional reviewer comment")


# ── Response schemas ──


class TermLearningRunResponse(BaseModel):
    """Response for a submitted terminology learning request."""
    task_id: str = Field(..., description="Audit record ID for tracking")
    status: str = Field(..., description="Current state: 'completed' if term existed, 'pending_review' if new term")
    message: str = Field(..., description="Human-readable summary")


class AuditRecordResponse(BaseModel):
    """Detailed audit record returned by GET /agent/term-learning/{id}."""
    id: str
    source_text: str
    context: Optional[str] = None
    matched_term: Optional[str] = None
    match_confidence: Optional[float] = None
    is_new_term: bool
    suggested_translation: Optional[str] = None
    llm_reasoning: Optional[str] = None
    review_status: str
    review_comment: Optional[str] = None
    error: Optional[str] = None
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class AuditListResponse(BaseModel):
    """List of pending audit records."""
    items: list[AuditRecordResponse]
    total: int


class HealthResponse(BaseModel):
    """Health check response."""
    status: str = "ok"
    service: str = "Terminology Learning Agent"
