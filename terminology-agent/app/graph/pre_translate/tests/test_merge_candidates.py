"""merge_candidates 单测 — RAG/Grep 合并规则。"""

import pytest

from app.graph.pre_translate.utils.grep_retrieve import GrepHit, GrepRetrieveResult
from app.graph.pre_translate.utils.merge_candidates import RagRetrieveResult, merge_candidates


@pytest.mark.unit
def test_grep_whole_sentence_only():
    rag = RagRetrieveResult("none", False, False, None, [], 0.0, 0.0)
    grep = GrepRetrieveResult(
        hits=[GrepHit("按钮", "Кнопка", 1.0)],
        whole_sentence_exact=True,
        whole_sentence_translate="Кнопка",
        ambiguous_words=[],
    )
    merged = merge_candidates(rag, grep)
    assert merged.retrieval_method == "grep"
    assert merged.exact_hit is True
    assert merged.suggested_translation == "Кнопка"
    assert merged.similar_terms[0]["retrieval_source"] == "grep"


@pytest.mark.unit
def test_rag_grep_same_translate():
    rag = RagRetrieveResult(
        "exact", True, False, "Кнопка",
        [{"entry": "按钮", "translate": "Кнопка", "score": 1.0}],
        1.0, 1.0,
    )
    grep = GrepRetrieveResult(
        hits=[GrepHit("按钮", "Кнопка", 1.0)],
        whole_sentence_exact=True,
        whole_sentence_translate="Кнопка",
        ambiguous_words=[],
    )
    merged = merge_candidates(rag, grep)
    assert merged.retrieval_method == "exact"
    assert merged.similar_terms[0]["retrieval_source"] == "rag+grep"


@pytest.mark.unit
def test_rag_grep_translate_conflict():
    rag = RagRetrieveResult(
        "exact", True, False, "A",
        [{"entry": "按钮", "translate": "A", "score": 1.0}],
        1.0, 1.0,
    )
    grep = GrepRetrieveResult(
        hits=[GrepHit("按钮", "B", 1.0)],
        whole_sentence_exact=True,
        whole_sentence_translate="B",
        ambiguous_words=[],
    )
    merged = merge_candidates(rag, grep)
    assert merged.rag_grep_conflict is True
    assert merged.exact_hit is False
