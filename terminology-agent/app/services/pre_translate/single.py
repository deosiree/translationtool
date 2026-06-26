"""单条预翻译 — 调 LangGraph runner 并映射 agent_meta。"""

from __future__ import annotations

from sqlalchemy.ext.asyncio import AsyncSession

from app.graph.pre_translate.runner import PreTranslateGraph
from app.services.pre_translate.mappers import (
    apply_auto_approved_translation,
    guess_lang_field,
    map_graph_state_to_agent_meta,
)


async def run_single_pre_translate(
    session: AsyncSession,
    *,
    entry: dict,
    task_id: str | None,
    task_name: str | None,
    product_name: str | None,
    target_lang: str | None,
    department: str | None,
    confidence_threshold: float,
) -> dict:
    """对单条词条执行预翻译，返回含 agent_meta 的结果 dict。

    Args:
        session: SQLAlchemy 异步会话。
        entry: 工作台词条 dict，须含 ``entry`` 字段。
        task_id: 翻译任务 id。
        task_name: 任务名称。
        product_name: 产品名称。
        target_lang: 目标语种。
        department: 部门/可视范围。
        confidence_threshold: 自动批准置信度阈值。

    Returns:
        原 entry 字段 + ``agent_meta``；auto_approved 时回填译文。
    """
    source_entry = entry.get("entry") or ""
    graph = PreTranslateGraph()
    final = await graph.run(
        source_text=source_entry,
        target_lang=target_lang,
        department=department,
        confidence_threshold=confidence_threshold,
        entry_info_id=entry.get("id"),
        task_id=task_id,
        task_name=task_name,
        product_name=product_name,
        session=session,
    )

    agent_meta = map_graph_state_to_agent_meta(final)
    result_item = {**entry, "agent_meta": agent_meta}

    if final.get("review_status") == "auto_approved":
        apply_auto_approved_translation(
            result_item,
            suggested=final.get("suggested_translation"),
            lang_key=guess_lang_field(entry, target_lang),
        )

    return result_item
