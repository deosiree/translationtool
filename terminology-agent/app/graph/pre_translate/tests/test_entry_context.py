"""entry_context 单测 — resolve_entry_comment Grep 消歧键。"""

import pytest

from app.graph.pre_translate.utils.entry_context import resolve_entry_comment


@pytest.mark.unit
def test_resolve_entry_comment_prefers_entry():
    assert resolve_entry_comment(entry_comment="按钮文案", entry_info_comment="other") == "按钮文案"


@pytest.mark.unit
def test_resolve_entry_comment_fallback_entry_info():
    assert resolve_entry_comment(entry_info_comment=" 菜单项 ") == "菜单项"


@pytest.mark.unit
def test_resolve_entry_comment_empty():
    assert resolve_entry_comment() == ""
