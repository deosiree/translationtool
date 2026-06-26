"""TermAuditService 行为契约测试 — Mock Repo，不连 MySQL。"""

from types import SimpleNamespace
from unittest.mock import AsyncMock

import pytest

from app.core.exceptions import ApiError


@pytest.mark.service
async def test_list_pending_pagination(term_audit_service, mock_repo, sample_audit_record):
    """list_pending 应传递分页参数并返回 records 与 total。"""
    mock_repo.list_pending_audits.return_value = ([sample_audit_record], 1)

    records, total = await term_audit_service.list_pending(page=2, page_size=10)

    assert len(records) == 1
    assert total == 1
    mock_repo.list_pending_audits.assert_awaited_once_with(page=2, page_size=10)


@pytest.mark.service
async def test_get_audit_not_found(term_audit_service, mock_repo):
    """audit 不存在时 get_audit_or_raise 应抛 ApiError。"""
    mock_repo.get_audit.return_value = None

    with pytest.raises(ApiError, match="不存在"):
        await term_audit_service.get_audit_or_raise("missing-id")


@pytest.mark.service
async def test_review_approved_merge_to_store(term_audit_service, mock_repo, sample_audit_record):
    """review approved 且术语库无重复时应调用 insert_translate 入库。"""
    pending = sample_audit_record
    approved = SimpleNamespace(**{**vars(pending), "review_status": "approved"})
    mock_repo.get_audit = AsyncMock(side_effect=[pending, approved])
    mock_repo.find_exact.return_value = None

    result = await term_audit_service.review(
        "audit-001", action="approved", comment="确认入库"
    )

    assert result.review_status == "approved"
    mock_repo.update_audit.assert_awaited_once_with(
        "audit-001",
        review_status="approved",
        review_comment="确认入库",
    )
    mock_repo.insert_translate.assert_awaited_once_with(
        entry=pending.source_text,
        translate=pending.suggested_translation,
        target_lang=pending.target_lang,
        department=pending.department,
    )


@pytest.mark.service
async def test_review_already_finalized(term_audit_service, mock_repo, sample_audit_record):
    """已是终态的 audit 再次 review 应抛 ApiError 且不更新。"""
    finalized = SimpleNamespace(**{**vars(sample_audit_record), "review_status": "approved"})
    mock_repo.get_audit.return_value = finalized

    with pytest.raises(ApiError, match="无法重复审核"):
        await term_audit_service.review("audit-001", action="approved", comment=None)

    mock_repo.update_audit.assert_not_called()


@pytest.mark.service
async def test_review_approved_skips_insert_when_exact_exists(
    term_audit_service, mock_repo, sample_audit_record, mock_translate_entry
):
    """review approved 但术语库已有精确匹配时应跳过 insert_translate。"""
    pending = sample_audit_record
    approved = SimpleNamespace(**{**vars(pending), "review_status": "approved"})
    mock_repo.get_audit = AsyncMock(side_effect=[pending, approved])
    mock_repo.find_exact.return_value = mock_translate_entry

    await term_audit_service.review("audit-001", action="approved", comment=None)

    mock_repo.insert_translate.assert_not_called()
