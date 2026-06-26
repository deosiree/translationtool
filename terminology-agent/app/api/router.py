"""FastAPI 路由 — 术语学习 Agent HTTP 接口。

路由前缀：/agent（见 app/main.py）
前端 dev proxy：vue.config.js → localhost:18002

主要端点：
  GET  /health                      健康检查
  POST /pre-translate/batch         工作台 Agent 批量预翻译
  GET  /term-learning/list          术语学习待审核列表
  GET  /term-learning/{id}          审核详情
  POST /term-learning/{id}/review   人工确认 / 拒绝（approved 时 MergeToStore）
"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.response import ok
from app.models.database import get_session
from app.services.pre_translate import PreTranslateService
from app.services.term_audit import TermAuditService
from app.schemas.agent import (
    TermReviewRequest,
    AuditListData,
    PreTranslateBatchRequest,
    PreTranslateBatchData,
    HealthData,
)
from app.schemas.converters import audit_to_data

router = APIRouter(tags=["术语学习"])


@router.get("/health", summary="健康检查")
async def health():
    """存活探针，Docker / 本地开发健康检查。"""
    return ok(HealthData())


@router.post("/pre-translate/batch", summary="批量预翻译")
async def batch_pre_translate(
    body: PreTranslateBatchRequest,
    taskID: str | None = None,
    confidenceThreshold: float = 0.8,
    session: AsyncSession = Depends(get_session),
):
    """工作台「Agent翻译」批量预翻译。

    Query:
      taskID              翻译任务 id
      confidenceThreshold 置信度阈值，默认 0.8

    Body:
      { entries, task_name, product_name, target_lang, department }

    Response.data:
      list          每条含 agent_meta；auto_approved 项带译文
      auto_count    自动回填数量
      pending_count 写入 term_agent_audit 数量
    """
    result = await PreTranslateService(session).run_batch(
        entries=body.entries,
        task_id=taskID,
        task_name=body.task_name,
        product_name=body.product_name,
        target_lang=body.target_lang,
        department=body.department,
        confidence_threshold=confidenceThreshold,
    )
    return ok(PreTranslateBatchData(**result))


@router.get("/term-learning/list", summary="待审核列表")
async def list_pending(
    page: int = 1,
    pageSize: int = Query(20, alias="pageSize"),
    session: AsyncSession = Depends(get_session),
):
    """术语学习页 — 待人工确认列表（分页）。"""
    records, total = await TermAuditService(session).list_pending(
        page=page, page_size=pageSize
    )
    items = [audit_to_data(r) for r in records]
    return ok(AuditListData(entry_list=items, total=total))


@router.get("/term-learning/{audit_id}", summary="审核详情")
async def get_audit_status(audit_id: str, session: AsyncSession = Depends(get_session)):
    """单条 audit 详情（轮询 / 调试）。"""
    record = await TermAuditService(session).get_audit_or_raise(audit_id)
    return ok(audit_to_data(record))


@router.post("/term-learning/{audit_id}/review", summary="审核确认/拒绝")
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
