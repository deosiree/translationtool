"""预翻译端点 — 工作台 Agent 批量预翻译。"""

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.response import ok
from app.models.database import get_session
from app.services.pre_translate import PreTranslateService
from app.schemas.agent import PreTranslateBatchRequest, PreTranslateBatchData

router = APIRouter()


@router.post("/batch", summary="批量预翻译")
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
