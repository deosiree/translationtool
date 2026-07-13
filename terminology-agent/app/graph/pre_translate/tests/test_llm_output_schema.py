"""LLM 输出 Pydantic Schema 单测。"""

import pytest
from pydantic import ValidationError

from app.graph.pre_translate.schemas.llm_output import (
    ComposeSuggestLlmOutput,
    PreTranslateLlmOutput,
)
from app.shared.field_limits import LLM_REASONING_DETAIL_MAX, TRANSLATE_MAX


@pytest.mark.unit
def test_pre_translate_output_accepts_valid():
    model = PreTranslateLlmOutput.model_validate(
        {"translation": "File and System", "reasoning": "词片覆盖后受约束拼装"}
    )
    assert model.translation == "File and System"


@pytest.mark.unit
def test_pre_translate_output_rejects_long_reasoning():
    with pytest.raises(ValidationError):
        PreTranslateLlmOutput.model_validate(
            {
                "translation": "File",
                "reasoning": "x" * (LLM_REASONING_DETAIL_MAX + 1),
            }
        )


@pytest.mark.unit
def test_pre_translate_output_rejects_long_translation():
    with pytest.raises(ValidationError):
        PreTranslateLlmOutput.model_validate(
            {
                "translation": "x" * (TRANSLATE_MAX + 1),
                "reasoning": "ok",
            }
        )


@pytest.mark.unit
def test_compose_output_accepts_terms_used():
    model = ComposeSuggestLlmOutput.model_validate(
        {
            "translation": "File and System",
            "reasoning": "使用 File、System 术语",
            "terms_used": ["File", "System"],
        }
    )
    assert model.terms_used == ["File", "System"]
