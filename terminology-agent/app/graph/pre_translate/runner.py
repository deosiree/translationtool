"""PreTranslateGraph 执行入口 — 构造 initial state 并 ainvoke。"""

from __future__ import annotations

from sqlalchemy.ext.asyncio import AsyncSession

from app.graph.pre_translate.builder import build_pre_translate_graph
from app.graph.pre_translate.state import PreTranslateState


class PreTranslateGraph:
    """工作台预翻译图 runner：services 层调用的唯一入口。"""

    def __init__(self) -> None:
        """编译 StateGraph 并缓存 compiled graph 实例。"""
        self.graph = build_pre_translate_graph()

    async def run(
        self,
        *,
        source_text: str,
        target_lang: str | None,
        department: str | None,
        confidence_threshold: float,
        entry_info_id: str | None = None,
        task_id: str | None = None,
        task_name: str | None = None,
        product_name: str | None = None,
        session: AsyncSession,
    ) -> PreTranslateState:
        """执行单条预翻译图并返回终态 state。

        Args:
            source_text: 源词条文本。
            target_lang: 目标语种。
            department: 部门/可视范围。
            confidence_threshold: 自动批准置信度阈值。
            entry_info_id: 工作台词条 id。
            task_id: 翻译任务 id。
            task_name: 任务名称。
            product_name: 产品名称。
            session: SQLAlchemy 异步会话，经 config 注入各 I/O 节点。

        Returns:
            图执行完毕后的 PreTranslateState。
        """
        initial: PreTranslateState = {
            "source_text": source_text,
            "target_lang": target_lang,
            "department": department,
            "confidence_threshold": confidence_threshold,
            "entry_info_id": entry_info_id,
            "task_id": task_id,
            "task_name": task_name,
            "product_name": product_name,
            "similar_terms": [],
            "trace": [],
        }
        config = {"configurable": {"session": session}}
        return await self.graph.ainvoke(initial, config)
