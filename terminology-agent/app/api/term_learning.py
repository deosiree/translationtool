"""术语审核端点 — 术语学习页待审核列表与确认/拒绝。"""

from typing import Optional

from fastapi import APIRouter, Depends, Query
from pydantic import ValidationError
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.response import ok, ResponseCode
from app.core.exceptions import ApiError
from app.models.database import get_session
from app.repository.term_repo import TermRepository
from app.repository.word_repo import WordRepository
from app.services.term_audit import TermAuditService
from app.schemas.agent import (
    TermReviewRequest,
    TermBatchReviewRequest,
    AuditListData,
    TermAuditListFilters,
    TermAuditSplitRequest,
    TermAuditSplitByIdsRequest,
    TermAuditUpdateRequest,
)
from app.shared.term_word.segment import segment_source_text
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


@router.post("/split", summary="已选术语切分预览")
async def split_selected_terms(
    body: TermAuditSplitRequest,
):
    """术语学习页 — 对已选术语执行 jieba 切分，返回候选词片列表供导入术语字典。

    每条的 ``entry`` 为词条原文、``translate`` 为建议译文（可选）。
    返回去重后的 ``[{word, translate?, target_lang, department}]``。
    """
    seen: set[str] = set()
    rows: list[dict] = []

    for item in body.items:
        tokens = [t for t, _s, _e in segment_source_text(item.entry)]
        # 去重
        for token in tokens:
            if token in seen:
                continue
            seen.add(token)
            row: dict = {
                "word": token,
                "target_lang": item.target_lang,
            }
            if item.department:
                row["department"] = item.department
            rows.append(row)

    return ok({"list": rows, "total": len(rows)})


@router.post("/split-by-ids", summary="按 audit_ids 切分并回填 segment_trace")
async def split_selected_terms_by_ids(
    body: TermAuditSplitByIdsRequest,
    session: AsyncSession = Depends(get_session),
):
    """术语学习页 — 对已选 audit 执行 jieba 切分，回填 segment_trace 字段。

    对每个 audit 的 ``source_text`` 做 jieba 分词，组装 ``{jieba, display}`` 结构，
    写入 ``term_agent_audit.segment_trace``。返回成功数。
    """
    repo = TermRepository(session)
    success = 0
    for audit_id in body.audit_ids:
        record = await repo.get_audit(audit_id)
        if not record or not record.source_text:
            continue
        jieba_tokens = [t for t, _s, _e in segment_source_text(record.source_text)]
        if not jieba_tokens:
            continue
        trace = {
            "jieba": jieba_tokens,
            "display": " | ".join(jieba_tokens),
        }
        await repo.create_pretranslate_audit(
            entry_info_id=record.entry_info_id,
            task_id=record.task_id,
            task_name=record.task_name,
            product_name=record.product_name,
            target_lang=record.target_lang or "",
            department=record.department,
            entry_comment=record.entry_comment,
            source_text=record.source_text,
            suggested_translation=record.suggested_translation,
            confidence=record.confidence,
            similar_terms=record.similar_terms,
            retrieval_method=record.retrieval_method,
            llm_reasoning=record.llm_reasoning,
            segment_trace=trace,
        )
        success += 1

    return ok({"success_count": success})


@router.post("/{audit_id}/edit", summary="编辑保存审核记录")
async def update_audit_fields(
    audit_id: str,
    body: TermAuditUpdateRequest,
    session: AsyncSession = Depends(get_session),
):
    """编辑保存——所有字段透传更新。

    前端控制编辑权限（仅开放 翻译 和 切分）及字段联动清空逻辑。
    """
    repo = TermRepository(session)
    record = await repo.get_audit(audit_id)
    if record is None:
        raise ApiError(f"审核记录 {audit_id} 不存在")

    # 只取前端显式传入的字段（含 null 清空），未传的字段保持原值
    update_fields = body.model_dump(exclude_unset=True)
    if not update_fields:
        raise ApiError("没有需要更新的字段", code=ResponseCode.PARAM_ERROR)

    updated = await repo.update_audit(audit_id, **update_fields)
    if updated is None:
        raise ApiError(f"更新审核记录 {audit_id} 失败")

    from app.schemas.converters import audit_to_data
    return ok(audit_to_data(updated))
