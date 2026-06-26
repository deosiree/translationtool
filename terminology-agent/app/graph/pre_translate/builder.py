"""PreTranslateGraph 组装与编译 — StateGraph 注册节点/边。"""

from __future__ import annotations

from langgraph.graph import END, StateGraph

from app.graph.pre_translate.edges.after_resolve_source import route_after_resolve_source
from app.graph.pre_translate.nodes.features.io.retrieve_similar import retrieve_similar_node
from app.graph.pre_translate.nodes.features.io.write_result import write_result_node
from app.graph.pre_translate.nodes.features.llm.translate_suggest import translate_suggest_node
from app.graph.pre_translate.nodes.features.rules.rerank_candidates import rerank_candidates_node
from app.graph.pre_translate.nodes.features.workflow.assess_route import assess_route_node
from app.graph.pre_translate.nodes.intentions.resolve_translation_source import (
    resolve_translation_source_node,
)
from app.graph.pre_translate.state import PreTranslateState


def build_pre_translate_graph():
    """构建并编译 PreTranslate 预翻译 StateGraph。

    流水线：retrieve → rerank → resolve → (term|llm) → assess → write。

    Returns:
        ``builder.compile()`` 返回的已编译 LangGraph 对象。
    """
    builder = StateGraph(PreTranslateState)

    builder.add_node("retrieve_similar", retrieve_similar_node)
    builder.add_node("rerank_candidates", rerank_candidates_node)
    builder.add_node("resolve_translation_source", resolve_translation_source_node)
    builder.add_node("translate_suggest", translate_suggest_node)
    builder.add_node("assess_route", assess_route_node)
    builder.add_node("write_result", write_result_node)

    builder.set_entry_point("retrieve_similar")
    builder.add_edge("retrieve_similar", "rerank_candidates")
    builder.add_edge("rerank_candidates", "resolve_translation_source")
    builder.add_conditional_edges(
        "resolve_translation_source",
        route_after_resolve_source,
        {
            "term_path": "assess_route",
            "llm_path": "translate_suggest",
            # P1 stub：hybrid 未启用，直接 assess（Phase 2 改连 decompose_compose）
            "hybrid_path": "assess_route",
        },
    )
    builder.add_edge("translate_suggest", "assess_route")
    builder.add_edge("assess_route", "write_result")
    builder.add_edge("write_result", END)

    return builder.compile()
