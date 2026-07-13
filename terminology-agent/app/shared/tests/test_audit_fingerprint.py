"""audit 写入指纹单元测试。"""

import pytest

from app.shared.audit_fingerprint import audit_write_fingerprint, fingerprint_from_audit_record


@pytest.mark.unit
def test_audit_write_fingerprint_normalizes_whitespace():
    fp1 = audit_write_fingerprint(
        source_text=" admin ",
        entry_comment=" 注释 ",
        suggested_translation="译",
        target_lang="俄文",
        department="通用平台部",
        retrieval_method="grep",
        confidence=0.6200001,
    )
    fp2 = audit_write_fingerprint(
        source_text="admin",
        entry_comment="注释",
        suggested_translation="译",
        target_lang="俄文",
        department="通用平台部",
        retrieval_method="grep",
        confidence=0.62,
    )
    assert fp1 == fp2


@pytest.mark.unit
def test_fingerprint_differs_on_entry_comment():
    base = dict(
        source_text="ADM",
        suggested_translation="ADM",
        target_lang="英文",
        department="ADM",
        retrieval_method="grep",
        confidence=0.8,
    )
    fp_a = audit_write_fingerprint(entry_comment="comment A", **base)
    fp_b = audit_write_fingerprint(entry_comment="comment B", **base)
    assert fp_a != fp_b


@pytest.mark.unit
def test_fingerprint_differs_on_suggested_translation():
    base = dict(
        source_text="ADM",
        entry_comment="same",
        target_lang="英文",
        department="ADM",
        retrieval_method="grep",
        confidence=0.8,
    )
    fp_a = audit_write_fingerprint(suggested_translation="A", **base)
    fp_b = audit_write_fingerprint(suggested_translation="B", **base)
    assert fp_a != fp_b


@pytest.mark.unit
def test_fingerprint_from_audit_record_dict():
    record = {
        "source_text": "x",
        "entry_comment": "c",
        "suggested_translation": "y",
        "target_lang": "俄文",
        "department": "d",
        "retrieval_method": "exact",
        "confidence": 0.5,
    }
    assert fingerprint_from_audit_record(record) == audit_write_fingerprint(**record)
