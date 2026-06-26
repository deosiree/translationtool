"""FastAPI 路由契约测试 — httpx + dependency_overrides。"""

from types import SimpleNamespace
from unittest.mock import AsyncMock, MagicMock

import pytest

from app.core.exceptions import ApiError


@pytest.mark.api
async def test_health(api_client):
    """GET /agent/health 应返回 code=200 且 status=ok。"""
    resp = await api_client.get("/agent/health")
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    assert body["data"]["status"] == "ok"


@pytest.mark.api
async def test_batch_pretranslate_response_shape(api_client, monkeypatch):
    """POST /agent/pre-translate/batch 响应应含 list、auto_count、pending_count。"""
    async def fake_batch(self, **kwargs):
        return {
            "list": [{"id": "e1", "entry": "test", "agent_meta": {"confidence": 1.0, "review_status": "auto_approved", "suggested_translation": "t", "similar_terms": [], "retrieval_method": "exact", "reasoning": ""}}],
            "auto_count": 1,
            "pending_count": 0,
        }

    monkeypatch.setattr(
        "app.api.router.PreTranslateService.batch_pre_translate",
        fake_batch,
    )

    resp = await api_client.post(
        "/agent/pre-translate/batch?confidenceThreshold=0.8",
        json={"entries": [{"id": "e1", "entry": "test"}]},
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    data = body["data"]
    assert "list" in data
    assert data["auto_count"] == 1
    assert data["pending_count"] == 0


@pytest.mark.api
async def test_batch_pretranslate_rejects_array_body(api_client):
    """POST /agent/pre-translate/batch 纯数组 body 应返回 code=202 参数错误。"""
    resp = await api_client.post(
        "/agent/pre-translate/batch",
        json=[{"id": "e1", "entry": "test"}],
    )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 202


@pytest.mark.api
async def test_list_term_learning_audits_pagination(
    api_client, monkeypatch, sample_audit_record
):
    """GET /agent/term-learning/list 应返回 list、total 并传递分页参数。"""
    mock_service = MagicMock()
    mock_service.list_pending = AsyncMock(return_value=([sample_audit_record], 1))
    monkeypatch.setattr(
        "app.api.router.TermAuditService",
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
    mock_service.list_pending.assert_awaited_once_with(page=1, page_size=10)


@pytest.mark.api
async def test_review_approved_response_shape(api_client, monkeypatch, sample_audit_record):
    """审核 approved 应返回 code=200 且 data.review_status=approved。"""
    approved = SimpleNamespace(**{**vars(sample_audit_record), "review_status": "approved"})
    mock_service = MagicMock()
    mock_service.review = AsyncMock(return_value=approved)
    monkeypatch.setattr(
        "app.api.router.TermAuditService",
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
        "app.api.router.TermAuditService",
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
        "app.api.router.TermAuditService",
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
