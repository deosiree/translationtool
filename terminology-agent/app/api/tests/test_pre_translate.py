"""预翻译端点契约测试。"""

import pytest


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
        "app.api.pre_translate.PreTranslateService.run_batch",
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
