"""field_limits 单测。"""

import pytest

from app.shared.field_limits import (
    AUDIT_SUGGEST_MAX,
    LLM_REASONING_DETAIL_MAX,
    TRANSLATE_MAX,
    assert_fits_audit_suggest,
    audit_suggest_budget,
    fits_audit_suggest,
)


@pytest.mark.unit
def test_constants_align_with_schema():
    assert TRANSLATE_MAX == 1024
    assert AUDIT_SUGGEST_MAX == 255
    assert LLM_REASONING_DETAIL_MAX == 248
    assert audit_suggest_budget() == 248


@pytest.mark.unit
def test_fits_audit_suggest():
    assert fits_audit_suggest("a" * 255)
    assert not fits_audit_suggest("a" * 256)


@pytest.mark.unit
def test_assert_fits_audit_suggest_ok():
    text = "基于混合检索：词片覆盖100%，LLM 受约束拼装"
    assert assert_fits_audit_suggest(text) == text


@pytest.mark.unit
def test_assert_fits_audit_suggest_raises():
    with pytest.raises(AssertionError):
        assert_fits_audit_suggest("x" * 256)
