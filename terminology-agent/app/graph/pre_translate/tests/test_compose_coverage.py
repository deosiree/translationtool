"""compose + coverage 单测。"""

import json
from pathlib import Path

import pytest

from app.graph.pre_translate.utils.compose import compose_translation
from app.graph.pre_translate.utils.coverage import (
    compute_coverage,
    coverage_to_confidence,
    meets_coverage_floor,
)
from app.graph.pre_translate.utils.decompose import Span, decompose_to_spans
from app.shared.term_word.trie import Trie

_CASES_PATH = Path(__file__).resolve().parents[3] / "evals" / "trajectory_cases.json"


@pytest.mark.unit
def test_compose_uses_translate_when_present():
    spans = [
        Span("文件", 0, 2, translate="File"),
        Span("系统", 2, 4, translate="System"),
    ]
    assert compose_translation(spans) == "FileSystem"


@pytest.mark.unit
def test_compose_keeps_source_for_glue():
    spans = [
        Span("文件", 0, 2, translate="File"),
        Span("与", 2, 3),
        Span("系统", 3, 5, translate="System"),
    ]
    assert compose_translation(spans) == "File与System"


@pytest.mark.unit
def test_coverage_full_hit():
    spans = [
        Span("文件", 0, 2, translate="File"),
        Span("系统", 2, 4, translate="System"),
    ]
    assert compute_coverage(spans, "文件系统") == 1.0
    assert meets_coverage_floor(1.0)
    assert coverage_to_confidence(1.0) >= 0.9


@pytest.mark.unit
def test_coverage_ambiguous_span_not_counted():
    spans = [
        Span("按钮", 0, 2, translate="A", ambiguous=True),
        Span("文案", 2, 4),
    ]
    assert compute_coverage(spans, "按钮文案") == 0.0


@pytest.mark.unit
def test_trajectory_cases_file_system_resource():
    cases = json.loads(_CASES_PATH.read_text(encoding="utf-8"))
    case = next(c for c in cases if c["id"] == "decompose-file-system-resource")
    trie = Trie()
    trie.build_from_entries(list(case["lexeme_translations"].keys()))
    spans = decompose_to_spans(case["source_text"], trie)
    for span in spans:
        trans = case["lexeme_translations"].get(span.text)
        if trans:
            span.translate = trans
    composed = compose_translation(spans)
    cov = compute_coverage(spans, case["source_text"])
    assert cov >= case["min_coverage"]
    for token in case["expected_partial_compose_contains"]:
        assert token in composed
