"""健康检查端点契约测试。"""

import pytest


@pytest.mark.api
async def test_health(api_client):
    """GET /agent/health 应返回 code=200 且 status=ok。"""
    resp = await api_client.get("/agent/health")
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    assert body["data"]["status"] == "ok"
