"""PreTranslate LangGraph 工作流状态定义。

Phase 3a 扩展：``entry_comment``、Grep 检索字段及 ``retrieval_source`` 候选标记。
Phase 3b 扩展：``spans``、``coverage``、``decomposed_translation``。
"""

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
    entry_comment: str  # Grep 消歧键；来自 entry.comment 或 entry_info
    task_id: str | None
    task_name: str | None
    product_name: str | None

    # ── 检索 ──
    retrieval_method: Literal["exact", "fuzzy", "grep", "hybrid", "decomposed", "none"]
    retrieval_confidence: float
    similar_terms: list  # 每项可含 retrieval_source: rag | grep | rag+grep
    exact_hit: bool
    fuzzy_hit: bool
    grep_hit: bool  # Grep 线是否有任意命中
    grep_hits: list  # Grep 候选快照（trace / 调试）
    rag_grep_conflict: bool  # RAG 与 Grep 整句译法不一致

    # ── 拆解拼装（Phase 3b）──
    spans: list  # [{text, start, end, translate?, ambiguous?}]
    coverage: float
    decomposed_translation: str | None

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
