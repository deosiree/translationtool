"""join_entry_info 单测 — comment 仅来自 entry_info。"""

from types import SimpleNamespace

import pytest

from app.shared.term_word.etl.join_entry_info import (
    build_term_word_payload,
    normalize_comment,
    word_status_from_translate_state,
    index_entry_infos_by_trans_id,
)


@pytest.mark.unit
def test_comment_from_entry_info_not_translate_remark():
    tr = SimpleNamespace(
        id="t1",
        entry="按钮",
        translate="Кнопка",
        type="俄文",
        visual_range="通用平台部",
        translate_state="3",
        remark="这是备注不是comment",
    )
    ei = SimpleNamespace(
        id="e1",
        entry="按钮",
        comment="按钮文案",
        task_id="task-1",
        product_id="prod-1",
        is_delete=0,
    )
    payload = build_term_word_payload(translate=tr, entry_info=ei)
    assert payload is not None
    assert payload["comment"] == "按钮文案"
    assert payload["comment"] != tr.remark


@pytest.mark.unit
def test_empty_comment_becomes_empty_string():
    assert normalize_comment(None) == ""
    assert normalize_comment("  ") == ""


@pytest.mark.unit
def test_status_mapping():
    assert word_status_from_translate_state("3") == "approved"
    assert word_status_from_translate_state("1") == "pending"


@pytest.mark.unit
def test_index_entry_infos_by_trans_id():
    ei = SimpleNamespace(
        id="e1",
        ru_trans_id="t-ru",
        en_trans_id=None,
        is_delete=0,
    )
    index = index_entry_infos_by_trans_id([ei])
    assert ("t-ru", "俄文") in index
    assert index[("t-ru", "俄文")][0].id == "e1"
