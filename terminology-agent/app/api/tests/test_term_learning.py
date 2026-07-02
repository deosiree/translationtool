"""术语审核端点契约测试。"""

from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock

import pytest

from app.core.exceptions import ApiError


@pytest.mark.api
async def test_list_term_learning_audits_pagination(
    api_client, monkeypatch, sample_audit_record
):
    """GET /agent/term-learning/list 应返回 list、total 并传递分页参数。"""
    mock_service = MagicMock()
    mock_service.list_pending = AsyncMock(return_value=([sample_audit_record], 1))
    monkeypatch.setattr(
        "app.api.term_learning.TermAuditService",
        lambda session: mock_service,
    )

    resp = await api_client.get("/agent/term-learning/list?page=1&pageSize=10")
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    data = body["data"]
    assert "list" in data
    assert len(data["list"]) == 1
    assert data["list"][0]["id"] == "audit-001"
    assert data["total"] == 1
    mock_service.list_pending.assert_awaited_once_with(page=1, page_size=10, filters=None)


@pytest.mark.api
async def test_list_term_learning_audits_with_filters(
    api_client, monkeypatch, sample_audit_record
):
    """GET /list 带筛选 query 时应组装 filters 并传入 service。"""
    from app.schemas.agent import TermAuditListFilters

    mock_service = MagicMock()
    mock_service.list_pending = AsyncMock(return_value=([sample_audit_record], 1))
    monkeypatch.setattr(
        "app.api.term_learning.TermAuditService",
        lambda session: mock_service,
    )

    resp = await api_client.get(
        "/agent/term-learning/list"
        "?page=1&pageSize=10"
        "&taskName=admin-proj&targetLang=英文&confidenceMin=0.8&confidenceMax=1"
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200

    mock_service.list_pending.assert_awaited_once()
    call_kwargs = mock_service.list_pending.await_args.kwargs
    assert call_kwargs["page"] == 1
    assert call_kwargs["page_size"] == 10
    filters = call_kwargs["filters"]
    assert isinstance(filters, TermAuditListFilters)
    assert filters.task_name == "admin-proj"
    assert filters.target_lang == "英文"
    assert filters.confidence_min == 0.8
    assert filters.confidence_max == 1.0


@pytest.mark.api
async def test_review_approved_response_shape(api_client, monkeypatch, sample_audit_record):
    """审核 approved 应返回 code=200 且 data.review_status=approved。"""
    approved = SimpleNamespace(**{**vars(sample_audit_record), "review_status": "approved"})
    mock_service = MagicMock()
    mock_service.review = AsyncMock(return_value=approved)
    monkeypatch.setattr(
        "app.api.term_learning.TermAuditService",
        lambda session: mock_service,
    )

    resp = await api_client.post(
        "/agent/term-learning/audit-001/review",
        json={"action": "approved", "comment": "确认入库"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    assert body["data"]["review_status"] == "approved"
    mock_service.review.assert_awaited_once_with(
        "audit-001", action="approved", comment="确认入库"
    )


@pytest.mark.api
async def test_get_audit_not_found(api_client, monkeypatch):
    """GET 不存在的 audit 应返回 HTTP 200 + code=201。"""
    mock_service = MagicMock()
    mock_service.get_audit_or_raise = AsyncMock(
        side_effect=ApiError("审核记录 missing-id 不存在")
    )
    monkeypatch.setattr(
        "app.api.term_learning.TermAuditService",
        lambda session: mock_service,
    )

    resp = await api_client.get("/agent/term-learning/missing-id")
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 201
    assert "不存在" in body["message"]


@pytest.mark.api
async def test_review_already_finalized(api_client, monkeypatch):
    """重复 review 应返回 HTTP 200 + code=201。"""
    mock_service = MagicMock()
    mock_service.review = AsyncMock(
        side_effect=ApiError("审核记录 audit-001 已是终态「approved」，无法重复审核")
    )
    monkeypatch.setattr(
        "app.api.term_learning.TermAuditService",
        lambda session: mock_service,
    )

    resp = await api_client.post(
        "/agent/term-learning/audit-001/review",
        json={"action": "approved"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 201
    assert "无法重复审核" in body["message"]


@pytest.mark.api
async def test_review_invalid_action(api_client):
    """非法 action 应返回 HTTP 200 + code=202。"""
    resp = await api_client.post(
        "/agent/term-learning/audit-001/review",
        json={"action": "invalid"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 202


@pytest.mark.api
async def test_batch_review_rejected_all_success(api_client, monkeypatch):
    """POST batch/review rejected 应返回 success_count 与 failures。"""
    from app.schemas.agent import TermBatchReviewResult

    mock_service = MagicMock()
    mock_service.batch_review = AsyncMock(
        return_value=TermBatchReviewResult(
            success_count=2,
            failed_count=0,
            failures=[],
        )
    )
    monkeypatch.setattr(
        "app.api.term_learning.TermAuditService",
        lambda session: mock_service,
    )

    resp = await api_client.post(
        "/agent/term-learning/batch/review",
        json={"ids": ["audit-001", "audit-002"], "action": "rejected"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    data = body["data"]
    assert data["success_count"] == 2
    assert data["failed_count"] == 0
    assert data["failures"] == []
    mock_service.batch_review.assert_awaited_once_with(
        ["audit-001", "audit-002"], action="rejected", comment=None
    )


@pytest.mark.api
async def test_batch_review_partial_failure(api_client, monkeypatch):
    """POST batch/review 部分失败应返回 failures 明细。"""
    from app.schemas.agent import TermBatchReviewFailure, TermBatchReviewResult

    mock_service = MagicMock()
    mock_service.batch_review = AsyncMock(
        return_value=TermBatchReviewResult(
            success_count=1,
            failed_count=1,
            failures=[
                TermBatchReviewFailure(
                    id="missing-id",
                    reason="审核记录 missing-id 不存在",
                )
            ],
        )
    )
    monkeypatch.setattr(
        "app.api.term_learning.TermAuditService",
        lambda session: mock_service,
    )

    resp = await api_client.post(
        "/agent/term-learning/batch/review",
        json={"ids": ["audit-001", "missing-id"], "action": "approved"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    data = body["data"]
    assert data["success_count"] == 1
    assert data["failed_count"] == 1
    assert len(data["failures"]) == 1
    assert data["failures"][0]["id"] == "missing-id"


@pytest.mark.api
async def test_batch_review_empty_ids(api_client):
    """POST batch/review 空 ids 应返回 code=202。"""
    resp = await api_client.post(
        "/agent/term-learning/batch/review",
        json={"ids": [], "action": "rejected"},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 202
