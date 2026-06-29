"""conflict 单测 — scope 不含 department。"""

from types import SimpleNamespace

import pytest

from app.word.conflict import detect_translate_mismatches, disambiguation_key


@pytest.mark.unit
def test_same_key_different_department_same_group():
    r1 = SimpleNamespace(
        id="w1",
        word="按钮",
        comment="",
        target_lang="俄文",
        department="部门A",
        translate="A",
        task_id="t1",
        product_id="p1",
        source_entry_info_id="e1",
    )
    r2 = SimpleNamespace(
        id="w2",
        word="按钮",
        comment="",
        target_lang="俄文",
        department="部门B",
        translate="B",
        task_id="t2",
        product_id="p2",
        source_entry_info_id="e2",
    )
    assert disambiguation_key(r1) == disambiguation_key(r2)
    conflicts = detect_translate_mismatches([r1, r2])
    assert len(conflicts) == 1
    payload = conflicts[0].to_conflict_payload()
    assert set(payload["task_ids"]) == {"t1", "t2"}
    assert set(payload["product_ids"]) == {"p1", "p2"}
    assert set(payload["source_entry_info_ids"]) == {"e1", "e2"}


@pytest.mark.unit
def test_same_translate_no_conflict():
    r1 = SimpleNamespace(
        id="w1", word="X", comment="", target_lang="俄文", translate="Same", department="A"
    )
    r2 = SimpleNamespace(
        id="w2", word="X", comment="", target_lang="俄文", translate="Same", department="B"
    )
    assert detect_translate_mismatches([r1, r2]) == []
