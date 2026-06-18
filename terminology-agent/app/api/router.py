"""FastAPI router for terminology learning agent endpoints."""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.database import get_session
from app.repository.term_repo import TermRepository
from app.graph.graph import TermLearningGraph
from app.schemas.agent import (
    TermLearningRunRequest,
    TermReviewRequest,
    JavaResponse,
    TermLearningRunData,
    AuditRecordData,
    AuditListData,
    HealthData,
)

router = APIRouter(tags=["terminology-learning"])


def _ok(data) -> JavaResponse:
    """Wrap response data in the format expected by the frontend axios interceptor."""
    return JavaResponse(code=200, message="success", data=data)


@router.get("/health")
async def health():
    """Health check endpoint."""
    return _ok(HealthData())


@router.post("/term-learning/run", status_code=201)
async def run_term_learning(
    body: TermLearningRunRequest,
    session: AsyncSession = Depends(get_session),
):
    """Submit a Chinese term for terminology learning.

    The workflow:
        1. Checks if the term already exists in the terminology store.
        2. If found → returns immediately with existing translation.
        3. If new → runs context analysis + LLM suggestion, then
           creates an audit record pending human review.
    """
    repo = TermRepository(session)
    existing = await repo.find_by_chinese(body.source_text)

    if existing:
        best = existing[0]
        return _ok(TermLearningRunData(
            task_id="",
            status="completed",
            message=f"Term '{body.source_text}' already exists → '{best.translate}' (confidence=1.0)",
        ))

    audit = await repo.create_audit(
        source_text=body.source_text,
        context=body.context,
    )

    graph = TermLearningGraph()
    final_state = await graph.run(
        source_text=body.source_text,
        context=body.context,
        audit_id=audit.id,
        session=session,
    )

    review_status = final_state.get("review_status", "pending")

    if review_status == "pending":
        msg = (
            f"Term '{body.source_text}' is new. "
            f"LLM suggested: '{final_state.get('suggested_translation', 'N/A')}'. "
            f"Awaiting human review (task_id={audit.id})."
        )
    else:
        msg = f"Term '{body.source_text}' processing completed (status={review_status})."

    return _ok(TermLearningRunData(
        task_id=audit.id,
        status=review_status,
        message=msg,
    ))


@router.get("/term-learning/pending")
async def list_pending(
    limit: int = 50,
    session: AsyncSession = Depends(get_session),
):
    """List all audit records awaiting human review."""
    repo = TermRepository(session)
    records = await repo.list_pending_audits(limit=limit)
    items = [AuditRecordData.model_validate(r) for r in records]
    return _ok(AuditListData(items=items, total=len(items)))


@router.get("/term-learning/{audit_id}")
async def get_audit_status(
    audit_id: str,
    session: AsyncSession = Depends(get_session),
):
    """Get the full audit record for a specific learning request."""
    repo = TermRepository(session)
    record = await repo.get_audit(audit_id)
    if record is None:
        raise HTTPException(status_code=404, detail=f"Audit record {audit_id} not found")
    return _ok(AuditRecordData.model_validate(record))


@router.post("/term-learning/{audit_id}/review")
async def review_term(
    audit_id: str,
    body: TermReviewRequest,
    session: AsyncSession = Depends(get_session),
):
    """Submit a human review decision for a pending term suggestion."""
    repo = TermRepository(session)
    record = await repo.get_audit(audit_id)
    if record is None:
        raise HTTPException(status_code=404, detail=f"Audit record {audit_id} not found")
    if record.review_status != "pending":
        raise HTTPException(
            status_code=409,
            detail=f"Audit record {audit_id} already has final status '{record.review_status}'",
        )

    await repo.update_audit(
        audit_id,
        review_status=body.action,
        review_comment=body.comment,
    )

    updated = await repo.get_audit(audit_id)
    return _ok(AuditRecordData.model_validate(updated))
