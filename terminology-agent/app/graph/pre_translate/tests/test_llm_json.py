"""llm_json 解析单测。"""

import pytest

from app.graph.pre_translate.schemas.llm_output import PreTranslateLlmOutput
from app.graph.pre_translate.utils.llm_json import parse_llm_output, strip_markdown_fence
from app.shared.field_limits import LLM_REASONING_DETAIL_MAX


@pytest.mark.unit
def test_strip_markdown_fence():
    raw = '```json\n{"translation":"A","reasoning":"短说明"}\n```'
    assert strip_markdown_fence(raw).startswith("{")


@pytest.mark.unit
def test_parse_llm_output_valid():
    content = '{"translation":"File","reasoning":"术语库未命中"}'
    parsed = parse_llm_output(content, PreTranslateLlmOutput)
    assert parsed is not None
    assert parsed.translation == "File"


@pytest.mark.unit
def test_parse_llm_output_rejects_overlong_reasoning():
    content = (
        '{"translation":"File","reasoning":"'
        + ("x" * (LLM_REASONING_DETAIL_MAX + 1))
        + '"}'
    )
    assert parse_llm_output(content, PreTranslateLlmOutput) is None


@pytest.mark.unit
def test_parse_llm_output_invalid_json():
    assert parse_llm_output("not json", PreTranslateLlmOutput) is None
