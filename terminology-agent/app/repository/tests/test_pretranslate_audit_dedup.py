"""create_pretranslate_audit 写入去重单元测试。"""

from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock

import pytest

from app.repository.term_repo import TermRepository


def _make_existing_record(**overrides):
    base = dict(
        id="audit-existing",
        source_text="ADM",
        entry_comment="comment A",
        suggested_translation="ADM trans",
        target_lang="英文",
        department="ADM",
        retrieval_method="grep",
        confidence=0.85,
        review_status="pending",
    )
    base.update(overrides)
    return SimpleNamespace(**base)


@pytest.mark.unit
async def test_create_pretranslate_audit_skips_duplicate_fingerprint():
    existing = _make_existing_record()
    session = MagicMock()
    session.add = MagicMock()
    session.commit = AsyncMock()
    session.refresh = AsyncMock()
    session.execute = AsyncMock(
        return_value=MagicMock(scalars=MagicMock(return_value=MagicMock(all=lambda: [existing])))
    )

    repo = TermRepository(session)
    result = await repo.create_pretranslate_audit(
        entry_info_id="e1",
        task_id="t1",
        task_name="task",
        product_name="prod",
        target_lang="英文",
        department="ADM",
        source_text="ADM",
        entry_comment="comment A",
        suggested_translation="ADM trans",
        confidence=0.85,
        similar_terms=[],
        retrieval_method="grep",
        llm_reasoning="reason",
    )

    assert result is existing
    session.add.assert_not_called()
    session.commit.assert_not_called()


@pytest.mark.unit
async def test_create_pretranslate_audit_inserts_when_translation_differs():
    existing = _make_existing_record(suggested_translation="other trans")
    session = MagicMock()
    session.add = MagicMock()
    session.commit = AsyncMock()
    session.refresh = AsyncMock()

    async def fake_refresh(record):
        record.id = "audit-new"

    session.refresh.side_effect = fake_refresh
    session.execute = AsyncMock(
        return_value=MagicMock(scalars=MagicMock(return_value=MagicMock(all=lambda: [existing])))
    )

    repo = TermRepository(session)
    result = await repo.create_pretranslate_audit(
        entry_info_id="e1",
        task_id="t1",
        task_name="task",
        product_name="prod",
        target_lang="英文",
        department="ADM",
        source_text="ADM",
        entry_comment="comment A",
        suggested_translation="ADM trans",
        confidence=0.85,
        similar_terms=[],
        retrieval_method="grep",
        llm_reasoning="reason",
    )

    session.add.assert_called_once()
    session.commit.assert_awaited_once()
    assert result.source_text == "ADM"


@pytest.mark.unit
async def test_create_pretranslate_audit_inserts_when_entry_comment_differs():
    existing = _make_existing_record(entry_comment="comment B")
    session = MagicMock()
    session.add = MagicMock()
    session.commit = AsyncMock()
    session.refresh = AsyncMock()
    session.execute = AsyncMock(
        return_value=MagicMock(scalars=MagicMock(return_value=MagicMock(all=lambda: [existing])))
    )

    repo = TermRepository(session)
    await repo.create_pretranslate_audit(
        entry_info_id="e1",
        task_id="t1",
        task_name="task",
        product_name="prod",
        target_lang="英文",
        department="ADM",
        source_text="ADM",
        entry_comment="comment A",
        suggested_translation="ADM trans",
        confidence=0.85,
        similar_terms=[],
        retrieval_method="grep",
        llm_reasoning="reason",
    )

    session.add.assert_called_once()
