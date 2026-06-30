"""after_decompose_compose 路由单测。"""

import pytest

from app.graph.pre_translate.constants import COVERAGE_FLOOR
from app.graph.pre_translate.edges.after_decompose_compose import route_after_decompose_compose


@pytest.mark.unit
def test_route_compose_ok():
    assert (
        route_after_decompose_compose(
            {
                "coverage": COVERAGE_FLOOR,
                "suggested_translation": "FileSystem",
            }
        )
        == "compose_ok"
    )


@pytest.mark.unit
def test_route_llm_fallback_low_coverage():
    assert (
        route_after_decompose_compose(
            {
                "coverage": 0.5,
                "suggested_translation": None,
            }
        )
        == "llm_fallback"
    )
