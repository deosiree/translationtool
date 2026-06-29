"""RAG 线检索 — t_translate exact + fuzzy（短语库，不做 chunk）。"""

from __future__ import annotations

from typing import Any

from app.graph.pre_translate.utils.merge_candidates import RagRetrieveResult
from app.repository.term_repo import TermRepository


async def rag_retrieve(
    repo: TermRepository,
    *,
    source_text: str,
    target_lang: str | None,
    department: str | None,
) -> RagRetrieveResult:
    """查 t_translate，返回结构化 RAG 检索结果。

    Args:
        repo: 术语库仓储（``find_exact`` / ``find_fuzzy``）。
        source_text: 待译词条，与 ``entry`` 整句匹配。
        target_lang: 目标语种，如「俄文」。
        department: 部门可见范围，对应 ``visual_range``。

    Returns:
        ``RagRetrieveResult``；``retrieval_method`` 为 ``exact`` | ``fuzzy`` | ``none``。
    """
    exact = await repo.find_exact(source_text, target_lang, department)
    if exact and exact.translate:
        return RagRetrieveResult(
            retrieval_method="exact",
            exact_hit=True,
            fuzzy_hit=False,
            suggested_translation=exact.translate,
            similar_terms=[
                {
                    "entry": exact.entry or source_text,
                    "translate": exact.translate,
                    "score": 1.0,
                }
            ],
            retrieval_confidence=1.0,
            confidence=1.0,
        )

    fuzzy_matches = await repo.find_fuzzy(
        source_text, target_lang, department, limit=5
    )
    raw_similar: list[dict[str, Any]] = []
    for match in fuzzy_matches:
        if not match.translate:
            continue
        raw_similar.append(
            {
                "entry": match.entry or "",
                "translate": match.translate,
                "entry_raw": match.entry or "",
            }
        )

    if raw_similar:
        return RagRetrieveResult(
            retrieval_method="fuzzy",
            exact_hit=False,
            fuzzy_hit=True,
            suggested_translation=None,
            similar_terms=raw_similar,
            retrieval_confidence=0.0,
            confidence=0.0,
        )

    return RagRetrieveResult(
        retrieval_method="none",
        exact_hit=False,
        fuzzy_hit=False,
        suggested_translation=None,
        similar_terms=[],
        retrieval_confidence=0.0,
        confidence=0.0,
    )
