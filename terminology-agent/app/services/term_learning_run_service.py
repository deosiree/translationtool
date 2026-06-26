"""单条术语发现编排 — LangGraph 入口与词条已存在短路。"""

from __future__ import annotations

from typing import Callable

from sqlalchemy.ext.asyncio import AsyncSession

from app.graph.graph import TermLearningGraph
from app.repository.term_repo import TermRepository
from app.schemas.agent import TermLearningRunData


class TermLearningRunService:
    """旧版 POST /term-learning/run 业务编排。"""

    def __init__(
        self,
        session: AsyncSession,
        graph_factory: Callable[[], TermLearningGraph] = TermLearningGraph,
    ):
        """注入数据库会话与 LangGraph 工厂（单测可替换 mock）。"""
        self._session = session
        self._repo = TermRepository(session)
        self._graph_factory = graph_factory

    async def run(self, *, source_text: str, context: str | None) -> TermLearningRunData:
        """执行单条术语发现：已存在则短路，新词走 LangGraph 并创建 audit。

        Args:
            source_text: 待检查的中文词条。
            context: 可选上下文，供 Graph 消歧。

        Returns:
            含 task_id、status、message 的 API 响应 data 模型。
        """
        existing = await self._repo.find_by_chinese(source_text)

        if existing:
            best = existing[0]
            return TermLearningRunData(
                task_id="",
                status="completed",
                message=(
                    f"词条「{source_text}」已存在，"
                    f"译文为「{best.translate}」（置信度=1.0）"
                ),
            )

        audit = await self._repo.create_audit(source_text=source_text, context=context)

        graph = self._graph_factory()
        final_state = await graph.run(
            source_text=source_text,
            context=context,
            audit_id=audit.id,
            session=self._session,
        )

        review_status = final_state.get("review_status", "pending")
        if review_status == "pending":
            suggested = final_state.get("suggested_translation") or "暂无"
            msg = (
                f"词条「{source_text}」为新词。"
                f"Agent 建议译文：「{suggested}」。"
                f"待人工审核（task_id={audit.id}）"
            )
        else:
            msg = f"词条「{source_text}」处理完成（status={review_status}）"

        return TermLearningRunData(task_id=audit.id, status=review_status, message=msg)
