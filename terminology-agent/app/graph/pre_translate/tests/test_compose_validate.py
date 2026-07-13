"""compose_validate 单测。"""

import pytest

from app.graph.pre_translate.utils.compose_validate import validate_mandatory_terms
from app.graph.pre_translate.utils.decompose import Span


@pytest.mark.unit
def test_validate_mandatory_terms_pass():
    spans = [
        Span("文件", 0, 2, translate="File"),
        Span("系统", 2, 4, translate="System"),
    ]
    ok, missing = validate_mandatory_terms("File System resources", spans)
    assert ok is True
    assert missing == []


@pytest.mark.unit
def test_validate_mandatory_terms_fail():
    spans = [Span("文件", 0, 2, translate="File")]
    ok, missing = validate_mandatory_terms("System only", spans)
    assert ok is False
    assert missing == ["File"]
