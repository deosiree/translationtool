"""TermLearningRunService 行为契约测试 — Mock Repo / Graph，不连 MySQL / LLM。"""

from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock

import pytest

from app.services.term_learning_run_service import TermLearningRunService


@pytest.mark.service
async def test_existing_term_short_circuit(mock_repo):
    """词条已存在时应短路返回 completed，不创建 audit 也不跑 Graph。"""
    mock_repo.find_by_chinese = AsyncMock(
        return_value=[SimpleNamespace(translate="Администратор")]
    )
    session = AsyncMock()
    service = TermLearningRunService(session, graph_factory=MagicMock())
    service._repo = mock_repo

    result = await service.run(source_text="admin", context=None)

    assert result.task_id == ""
    assert result.status == "completed"
    assert "已存在" in result.message
    mock_repo.create_audit.assert_not_called()


@pytest.mark.service
async def test_new_term_graph_pending(mock_repo):
    """新词应创建 audit、执行 Graph 并返回 pending 状态。"""
    mock_repo.find_by_chinese = AsyncMock(return_value=[])
    mock_repo.create_audit = AsyncMock(return_value=SimpleNamespace(id="audit-new"))
    mock_graph = MagicMock()
    mock_graph.run = AsyncMock(
        return_value={
            "review_status": "pending",
            "suggested_translation": "[Agent] 新词",
        }
    )
    session = AsyncMock()
    service = TermLearningRunService(session, graph_factory=lambda: mock_graph)
    service._repo = mock_repo

    result = await service.run(source_text="全新词条", context="上下文")

    assert result.task_id == "audit-new"
    assert result.status == "pending"
    assert "待人工审核" in result.message
    mock_graph.run.assert_awaited_once_with(
        source_text="全新词条",
        context="上下文",
        audit_id="audit-new",
        session=session,
    )
