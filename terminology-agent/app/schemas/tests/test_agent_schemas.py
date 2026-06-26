"""Pydantic schema 契约测试。"""

from datetime import datetime

import pytest

from app.schemas.agent import AuditListData, AuditRecordData, PreTranslateBatchData


def test_audit_record_coerce_is_new_term():
    """MySQL TINYINT(1) 读出的 0/1 应转为 bool。"""
    now = datetime(2026, 6, 23, 10, 0, 0)
    record = AuditRecordData(
        id="a1",
        source_text="admin",
        is_new_term=1,
        review_status="pending",
        created_at=now,
        updated_at=now,
    )
    assert record.is_new_term is True


def test_audit_list_data_list_alias():
    """AuditListData entry_list 字段 JSON 序列化别名应为 list。"""
    now = datetime(2026, 6, 23, 10, 0, 0)
    record = AuditRecordData(
        id="a1",
        source_text="admin",
        is_new_term=1,
        review_status="pending",
        created_at=now,
        updated_at=now,
    )
    data = AuditListData(**{"list": [record], "total": 1})
    assert len(data.entry_list) == 1
    dumped = data.model_dump(by_alias=True)
    assert "list" in dumped
    assert dumped["total"] == 1
    assert dumped["list"][0]["id"] == "a1"


def test_pretranslate_batch_data_list_alias():
    """entry_list 字段 JSON 序列化别名应为 list。"""
    data = PreTranslateBatchData(
        **{"list": [{"id": "e1"}], "auto_count": 1, "pending_count": 0}
    )
    assert len(data.entry_list) == 1
    dumped = data.model_dump(by_alias=True)
    assert "list" in dumped
    assert dumped["list"][0]["id"] == "e1"
