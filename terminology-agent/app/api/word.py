"""调试端点 — term_word 关键词查询。"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.response import ok
from app.models.database import get_session
from app.repository.word_repo import WordRepository

router = APIRouter()


@router.get("/{word}", summary="Grep 线调试 — 按 word 查 term_word")
async def lookup_word(
    word: str,
    target_lang: str = Query(..., description="目标语种，如俄文"),
    comment: str | None = Query(None, description="消歧 comment；省略则不过滤"),
    department: str | None = Query(None, description="部门过滤，非消歧键"),
    session: AsyncSession = Depends(get_session),
):
    """Phase 2 调试端点 — live keyword lookup on term_word（仅 approved）。"""
    rows = await WordRepository(session).find_by_word(
        word,
        target_lang=target_lang,
        comment=comment,
        department=department,
    )
    data = [
        {
            "id": r.id,
            "word": r.word,
            "comment": r.comment,
            "translate": r.translate,
            "target_lang": r.target_lang,
            "department": r.department,
            "task_id": r.task_id,
            "product_id": r.product_id,
            "status": r.status,
        }
        for r in rows
    ]
    return ok({"items": data, "count": len(data)})
