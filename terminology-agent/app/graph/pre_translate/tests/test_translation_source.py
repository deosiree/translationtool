"""translation_source 枚举与 format_agent_reasoning 单测。"""

import pytest

from app.graph.pre_translate.domain.translation_source import (
    SOURCE_LABEL,
    TranslationSource,
    format_agent_reasoning,
)


@pytest.mark.unit
def test_format_agent_reasoning_term():
    assert format_agent_reasoning(TranslationSource.TERM, "精确匹配") == "基于术语：精确匹配"


@pytest.mark.unit
def test_format_agent_reasoning_llm():
    assert format_agent_reasoning(TranslationSource.LLM) == "基于LLM机翻"


@pytest.mark.unit
def test_format_agent_reasoning_hybrid():
    """Phase 3a：HYBRID 标签为「基于混合检索」。"""
    assert SOURCE_LABEL[TranslationSource.HYBRID] == "基于混合检索"
    assert format_agent_reasoning(TranslationSource.HYBRID, "部分 span") == (
        "基于混合检索：部分 span"
    )
