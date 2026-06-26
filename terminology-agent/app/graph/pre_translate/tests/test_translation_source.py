"""translation_source 格式化单元测试。"""

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
def test_format_agent_reasoning_hybrid_reserved():
    assert SOURCE_LABEL[TranslationSource.HYBRID] == "基于术语+LLM机翻"
    assert format_agent_reasoning(TranslationSource.HYBRID, "部分 span") == (
        "基于术语+LLM机翻：部分 span"
    )
