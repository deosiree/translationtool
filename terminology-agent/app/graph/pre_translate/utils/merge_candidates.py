"""RAG 检索结果与 Grep 命中合并 — 确定性 rules（不用 LLM）。

合并后 ``similar_terms`` 每项带 ``retrieval_source``：``rag`` | ``grep`` | ``rag+grep``。
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from app.graph.pre_translate.utils.grep_retrieve import GrepHit, GrepRetrieveResult


@dataclass
class RagRetrieveResult:
    """RAG 线（t_translate）检索结构化结果。"""

    retrieval_method: str
    exact_hit: bool
    fuzzy_hit: bool
    suggested_translation: str | None
    similar_terms: list[dict[str, Any]]
    retrieval_confidence: float
    confidence: float


@dataclass
class MergeResult:
    """RAG ∥ Grep merge 后的检索 state 片段。"""

    retrieval_method: str
    exact_hit: bool
    fuzzy_hit: bool
    grep_hit: bool
    suggested_translation: str | None
    similar_terms: list[dict[str, Any]]
    retrieval_confidence: float
    confidence: float
    grep_hits: list[dict[str, Any]]
    rag_grep_conflict: bool


def _tag_rag_candidates(similar_terms: list[dict]) -> list[dict]:
    """为 RAG 候选打上 ``retrieval_source=rag``。"""
    tagged: list[dict] = []
    for item in similar_terms:
        row = dict(item)
        row.setdefault("retrieval_source", "rag")
        tagged.append(row)
    return tagged


def _grep_to_similar(hit: GrepHit) -> dict[str, Any]:
    """``GrepHit`` → ``similar_terms`` 行 dict。"""
    return {
        "entry": hit.word,
        "translate": hit.translate,
        "score": hit.score,
        "retrieval_source": "grep",
        "ambiguous": hit.ambiguous,
    }


def merge_candidates(
    rag: RagRetrieveResult,
    grep: GrepRetrieveResult | None,
) -> MergeResult:
    """合并 RAG 与 Grep 候选。

    规则摘要：
      - Grep 整句唯一命中且无 RAG exact → ``retrieval_method=grep``，``exact_hit=True``
      - RAG/Grep 整句同译 → ``retrieval_source=rag+grep``
      - 整句译法不一致 → ``rag_grep_conflict=True``，``exact_hit=False``
      - RAG fuzzy/none + Grep 词级命中 → 追加候选，必要时 ``hybrid``

    Args:
        rag: RAG 检索结果。
        grep: Grep 检索结果；``None`` 或空表示未启用/无命中。

    Returns:
        供 ``retrieve_similar_node`` 写回 state 的合并结果。
    """
    similar = _tag_rag_candidates(rag.similar_terms)
    grep_hits = [_grep_to_similar(h) for h in (grep.hits if grep else [])]

    exact_hit = rag.exact_hit
    suggested = rag.suggested_translation
    confidence = rag.confidence
    retrieval_confidence = rag.retrieval_confidence
    retrieval_method = rag.retrieval_method
    fuzzy_hit = rag.fuzzy_hit
    grep_hit = bool(grep and grep.hits)
    rag_grep_conflict = False

    if grep and grep.whole_sentence_exact and grep.whole_sentence_translate:
        grep_whole = {
            "entry": grep.hits[0].word if grep.hits else "",
            "translate": grep.whole_sentence_translate,
            "score": 1.0,
            "retrieval_source": "grep",
        }
        if rag.exact_hit and rag.suggested_translation:
            if rag.suggested_translation == grep.whole_sentence_translate:
                similar = [
                    {
                        "entry": grep_whole["entry"],
                        "translate": grep.whole_sentence_translate,
                        "score": 1.0,
                        "retrieval_source": "rag+grep",
                    }
                ]
                retrieval_method = "exact"
            else:
                rag_grep_conflict = True
                exact_hit = False
                similar = _tag_rag_candidates(rag.similar_terms) + [grep_whole]
                suggested = None
                confidence = 0.0
                retrieval_confidence = 0.0
                retrieval_method = "hybrid"
        else:
            exact_hit = True
            suggested = grep.whole_sentence_translate
            confidence = 1.0
            retrieval_confidence = 1.0
            retrieval_method = "grep"
            similar = [grep_whole]
            fuzzy_hit = False

    elif grep_hit and rag.retrieval_method in ("fuzzy", "none"):
        for gh in grep_hits:
            if gh not in similar and not gh.get("ambiguous"):
                similar.append(gh)
        if rag.retrieval_method == "fuzzy" and grep_hit:
            retrieval_method = "hybrid"
        elif rag.retrieval_method == "none" and grep_hit and not exact_hit:
            retrieval_method = "grep"

    return MergeResult(
        retrieval_method=retrieval_method,
        exact_hit=exact_hit,
        fuzzy_hit=fuzzy_hit,
        grep_hit=grep_hit,
        suggested_translation=suggested,
        similar_terms=similar,
        retrieval_confidence=retrieval_confidence,
        confidence=confidence,
        grep_hits=grep_hits,
        rag_grep_conflict=rag_grep_conflict,
    )
