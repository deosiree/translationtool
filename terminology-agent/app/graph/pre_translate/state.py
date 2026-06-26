"""PreTranslate LangGraph 工作流状态定义。"""

from __future__ import annotations

import operator
from typing import Annotated, Literal, TypedDict


class PreTranslateState(TypedDict, total=False):
    """预翻译图在各节点间传递的共享状态 schema。"""

    # ── 输入 ──
    source_text: str
    target_lang: str | None
    department: str | None
    confidence_threshold: float
    entry_info_id: str | None
    task_id: str | None
    task_name: str | None
    product_name: str | None

    # ── 检索 ──
    retrieval_method: Literal["exact", "fuzzy", "none"]
    retrieval_confidence: float
    similar_terms: list
    exact_hit: bool
    fuzzy_hit: bool

    # ── 意图 / 译文 ──
    translation_source: Literal["term", "llm", "hybrid"]
    suggested_translation: str | None
    llm_detail: str | None
    confidence: float

    # ── 输出 ──
    llm_reasoning: str | None
    review_status: Literal["auto_approved", "needs_human"]
    error: str | None
    trace: Annotated[list, operator.add]


# 旧 TermLearningGraph 兼容别名（Phase 3+ analyze_context 等复用）
TermState = PreTranslateState
