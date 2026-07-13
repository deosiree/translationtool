"""术语审核端点 — 术语学习页待审核列表与确认/拒绝。"""

from typing import Optional

from fastapi import APIRouter, Depends, Query
from pydantic import ValidationError
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.response import ok, ResponseCode
from app.core.exceptions import ApiError
from app.models.database import get_session
from app.services.term_audit import TermAuditService
from app.schemas.agent import (
    TermReviewRequest,
    TermBatchReviewRequest,
    AuditListData,
    TermAuditListFilters,
)
from app.schemas.converters import audit_to_data

router = APIRouter()


def _build_list_filters(
    *,
    source_text: Optional[str] = None,
    target_lang: Optional[str] = None,
    task_name: Optional[str] = None,
    product_name: Optional[str] = None,
    department: Optional[str] = None,
    confidence_min: Optional[float] = None,
    confidence_max: Optional[float] = None,
    retrieval_method: Optional[str] = None,
    entry_comment: Optional[str] = None,
) -> TermAuditListFilters | None:
    """组装筛选 DTO；全空时返回 None。"""
    try:
        filters = TermAuditListFilters(
            source_text=source_text,
            target_lang=target_lang,
            task_name=task_name,
            product_name=product_name,
            department=department,
            confidence_min=confidence_min,
            confidence_max=confidence_max,
            retrieval_method=retrieval_method,
            entry_comment=entry_comment,
        )
    except ValidationError as exc:
        first = exc.errors()[0] if exc.errors() else {}
        msg = first.get("msg", "筛选参数不正确")
        raise ApiError(str(msg), code=ResponseCode.PARAM_ERROR) from exc
    if filters.model_dump(exclude_none=True):
        return filters
    return None


@router.get("/list", summary="待审核列表")
async def list_pending(
    page: int = 1,
    pageSize: int = Query(20, alias="pageSize"),
    sourceText: Optional[str] = Query(None, alias="sourceText"),
    targetLang: Optional[str] = Query(None, alias="targetLang"),
    taskName: Optional[str] = Query(None, alias="taskName"),
    productName: Optional[str] = Query(None, alias="productName"),
    department: Optional[str] = Query(None),
    confidenceMin: Optional[float] = Query(None, alias="confidenceMin"),
    confidenceMax: Optional[float] = Query(None, alias="confidenceMax"),
    retrievalMethod: Optional[str] = Query(None, alias="retrievalMethod"),
    entryComment: Optional[str] = Query(None, alias="entryComment"),
    session: AsyncSession = Depends(get_session),
):
    """术语学习页 — 待人工确认列表（分页 + 可选筛选）。"""
    filters = _build_list_filters(
        source_text=sourceText,
        target_lang=targetLang,
        task_name=taskName,
        product_name=productName,
        department=department,
        confidence_min=confidenceMin,
        confidence_max=confidenceMax,
        retrieval_method=retrievalMethod,
        entry_comment=entryComment,
    )
    records, total = await TermAuditService(session).list_pending(
        page=page,
        page_size=pageSize,
        filters=filters,
    )
    items = [audit_to_data(r) for r in records]
    return ok(AuditListData(entry_list=items, total=total))


@router.post("/batch/review", summary="批量审核确认/拒绝")
async def batch_review_terms(
    body: TermBatchReviewRequest,
    session: AsyncSession = Depends(get_session),
):
    """术语学习页 — 批量确认或拒绝。

    逐条调用 review；单条失败不中断，返回 success_count / failed_count / failures。
    """
    result = await TermAuditService(session).batch_review(
        body.ids, action=body.action, comment=body.comment
    )
    return ok(result)


@router.get("/{audit_id}", summary="审核详情")
async def get_audit_status(audit_id: str, session: AsyncSession = Depends(get_session)):
    """单条 audit 详情（轮询 / 调试）。"""
    record = await TermAuditService(session).get_audit_or_raise(audit_id)
    return ok(audit_to_data(record))


@router.post("/{audit_id}/review", summary="审核确认/拒绝")
async def review_term(
    audit_id: str,
    body: TermReviewRequest,
    session: AsyncSession = Depends(get_session),
):
    """术语学习页 — 确认或拒绝。

    approved 且术语库尚无精确匹配时，调用 insert_translate 写入 t_translate（MergeToStore）。
    """
    record = await TermAuditService(session).review(
        audit_id, action=body.action, comment=body.comment
    )
    return ok(audit_to_data(record))
