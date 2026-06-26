"""条件边：resolve_translation_source 之后的路径分流。"""

from __future__ import annotations

from typing import Literal

from app.graph.pre_translate.domain.translation_source import TranslationSource
from app.graph.pre_translate.state import PreTranslateState


def route_after_resolve_source(
    state: PreTranslateState,
) -> Literal["term_path", "llm_path", "hybrid_path"]:
    """根据意图节点写入的 translation_source 选择功能路径。

    P1 仅连接 term_path / llm_path；hybrid_path 预留 Phase 2。

    Args:
        state: 当前图状态，须含 ``translation_source``。

    Returns:
        下一跳路径键，映射见 ``builder.add_conditional_edges``。
    """
    source = state.get("translation_source")
    if source == TranslationSource.HYBRID.value:
        return "hybrid_path"
    if source == TranslationSource.LLM.value:
        return "llm_path"
    return "term_path"
