"""PreTranslateGraph 条件路由单元测试。"""

import pytest

from app.graph.pre_translate.edges.after_resolve_source import route_after_resolve_source
from app.graph.pre_translate.domain.translation_source import TranslationSource


@pytest.mark.graph
def test_route_term_path():
    state = {"translation_source": TranslationSource.TERM.value}
    assert route_after_resolve_source(state) == "term_path"


@pytest.mark.graph
def test_route_llm_path():
    state = {"translation_source": TranslationSource.LLM.value}
    assert route_after_resolve_source(state) == "llm_path"


@pytest.mark.graph
def test_route_hybrid_path_stub():
    state = {"translation_source": TranslationSource.HYBRID.value}
    assert route_after_resolve_source(state) == "hybrid_path"
