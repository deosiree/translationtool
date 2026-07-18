"""GET/POST/PUT/DELETE /agent/word — 术语字典 CRUD。"""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock, patch

import pytest


def _mock_row(**kwargs):
    row = MagicMock()
    defaults = {
        "id": "w1",
        "word": "文件",
        "comment": "",
        "translate": "File",
        "target_lang": "英文",
        "department": "通用平台部",
        "source_translate_id": "",
        "source_entry_info_id": None,
        "task_id": None,
        "product_id": None,
        "status": "3",
        "created_at": None,
        "updated_at": None,
    }
    defaults.update(kwargs)
    for k, v in defaults.items():
        setattr(row, k, v)
    return row


@pytest.mark.asyncio
async def test_list_words_ok(api_client):
    row = _mock_row()
    with patch(
        "app.api.word.WordRepository.list_words",
        new_callable=AsyncMock,
        return_value=([row], 1),
    ):
        resp = await api_client.get("/agent/word/list?page=1&pageSize=10&status=3")
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    assert body["data"]["total"] == 1
    assert body["data"]["list"][0]["word"] == "文件"
    assert body["data"]["list"][0]["translate"] == "File"
    assert body["data"]["list"][0]["status"] == "3"


@pytest.mark.asyncio
async def test_create_word_ok(api_client):
    row = _mock_row(id="new1", status="1")
    with (
        patch(
            "app.api.word.WordRepository.create_word",
            new_callable=AsyncMock,
            return_value=row,
        ),
        patch("app.api.word.WordRepository.commit", new_callable=AsyncMock),
        patch("app.api.word.clear_trie_cache"),
    ):
        resp = await api_client.post(
            "/agent/word",
            json={
                "word": "文件",
                "translate": "File",
                "target_lang": "英文",
                "department": "通用平台部",
                "status": "1",
            },
        )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    assert body["data"]["id"] == "new1"
    assert body["data"]["status"] == "1"


@pytest.mark.asyncio
async def test_update_word_ok(api_client):
    row = _mock_row(status="1")
    updated = _mock_row(status="3")
    with (
        patch(
            "app.api.word.WordRepository.get_by_id",
            new_callable=AsyncMock,
            return_value=row,
        ),
        patch(
            "app.api.word.WordRepository.update_word",
            new_callable=AsyncMock,
            return_value=updated,
        ),
        patch("app.api.word.WordRepository.commit", new_callable=AsyncMock),
        patch("app.api.word.clear_trie_cache"),
    ):
        resp = await api_client.put("/agent/word/w1", json={"status": "3"})
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    assert body["data"]["status"] == "3"


@pytest.mark.asyncio
async def test_update_word_not_found(api_client):
    with patch(
        "app.api.word.WordRepository.get_by_id",
        new_callable=AsyncMock,
        return_value=None,
    ):
        resp = await api_client.put("/agent/word/missing", json={"status": "3"})
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] != 200


@pytest.mark.asyncio
async def test_delete_word_ok(api_client):
    with (
        patch(
            "app.api.word.WordRepository.delete_by_ids",
            new_callable=AsyncMock,
            return_value=1,
        ),
        patch("app.api.word.WordRepository.commit", new_callable=AsyncMock),
        patch("app.api.word.clear_trie_cache"),
    ):
        resp = await api_client.delete("/agent/word/w1")
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    assert body["data"]["deleted"] == 1


@pytest.mark.asyncio
async def test_batch_delete_ok(api_client):
    with (
        patch(
            "app.api.word.WordRepository.delete_by_ids",
            new_callable=AsyncMock,
            return_value=2,
        ),
        patch("app.api.word.WordRepository.commit", new_callable=AsyncMock),
        patch("app.api.word.clear_trie_cache"),
    ):
        resp = await api_client.post(
            "/agent/word/batch-delete",
            json={"ids": ["w1", "w2"]},
        )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    assert body["data"]["deleted"] == 2


@pytest.mark.asyncio
async def test_export_rows_ok_with_chinese_utf8_filename(api_client):
    """Content-Disposition 须可 latin-1 编码；body 为真 xlsx。"""
    resp = await api_client.post(
        "/agent/word/export-rows",
        json={
            "rows": [
                {
                    "word": "图元",
                    "translate": "icon",
                    "target_lang": "英文",
                    "comment": "",
                    "use_llm": False,
                    "status": "1",
                }
            ],
            "forcePending": True,
        },
    )
    assert resp.status_code == 200
    assert resp.content[:2] == b"PK"
    cd = resp.headers.get("content-disposition") or ""
    cd.encode("latin-1")  # 不得抛 UnicodeEncodeError
    assert 'filename="split_export.xlsx"' in cd
    assert "filename*=UTF-8''" in cd


@pytest.mark.asyncio
async def test_import_rows_ok(api_client):
    row = _mock_row(id="imp1", status="1")
    with (
        patch(
            "app.api.word.WordRepository.find_by_key",
            new_callable=AsyncMock,
            return_value=None,
        ),
        patch(
            "app.api.word.WordRepository.create_word",
            new_callable=AsyncMock,
            return_value=row,
        ),
        patch("app.api.word.WordRepository.commit", new_callable=AsyncMock),
        patch("app.api.word.clear_trie_cache"),
    ):
        resp = await api_client.post(
            "/agent/word/import-rows",
            json={
                "rows": [
                    {
                        "word": "图元",
                        "translate": "icon",
                        "target_lang": "英文",
                        "comment": "",
                    }
                ],
                "forcePending": True,
            },
        )
    assert resp.status_code == 200
    body = resp.json()
    assert body["code"] == 200
    assert body["data"]["created"] == 1
    assert body["data"]["skipped"] == 0
