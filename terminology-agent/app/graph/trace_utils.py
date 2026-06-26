"""LangGraph / PreTranslate trace 工具 — 测试与 devtools Demo 共用。

本模块把 Agent 内部决策拆成「可读的逐步快照」，方便新手对照 Demo / HTML 报告理解。

Trace 步骤字典字段说明（PreTranslate）::

    stage               当前阶段名
    retrieval_method    exact | fuzzy | hybrid
    confidence          0~1，越高越可信
    suggested_translation  建议译文
    similar_terms_count  参考术语条数
    threshold           （AssessConfidence 阶段）前端 Slider 阈值
    route               auto_approved | needs_human
"""

from __future__ import annotations

from typing import Any


def render_graph_png(compiled_graph) -> bytes:
    """将 LangGraph 编译图导出为 Mermaid PNG 字节流。

    参数:
        compiled_graph: 已 compile 的 LangGraph 对象（如 TermLearningGraph().graph）。

    返回:
        PNG 图片二进制，可交给 IPython.display.Image 显示。
    """
    return compiled_graph.get_graph().draw_mermaid_png()


def build_pretranslate_trace_steps(retrieval: dict, threshold: float) -> list[dict[str, Any]]:
    """根据 RetrieveSimilar 结果，构建 PreTranslate 的两步 trace。

    对应工作台预翻译 MVP 流程::

        RetrieveSimilar → AssessConfidence → route

    参数:
        retrieval: ``_retrieve_similar`` 返回值，含 confidence、suggested_translation 等。
        threshold: 置信度阈值（前端 Slider，默认 0.8）。

    返回:
        两步快照列表：RetrieveSimilar 摘要 + AssessConfidence 路由决策。
    """
    confidence = retrieval["confidence"]
    review_status = "auto_approved" if confidence >= threshold else "needs_human"
    return [
        {
            "stage": "RetrieveSimilar",
            "retrieval_method": retrieval.get("retrieval_method"),
            "confidence": confidence,
            "suggested_translation": retrieval.get("suggested_translation"),
            "similar_terms_count": len(retrieval.get("similar_terms") or []),
        },
        {
            "stage": "AssessConfidence",
            "threshold": threshold,
            "confidence": confidence,
            "route": review_status,  # auto_approved=自动回填译文；needs_human=写入待审核表
        },
    ]


async def collect_pretranslate_trace(
    service,
    *,
    source_entry: str,
    target_lang: str | None,
    department: str | None,
    confidence_threshold: float,
) -> list[dict[str, Any]]:
    """对单条词条执行 RetrieveSimilar，并返回完整 trace 步骤。

    参数:
        service: PreTranslateService 实例（Demo 可注入 mock repo）。
        source_entry: 工作台中文词条原文。
        target_lang: 目标语种，如「俄文」。
        department: 部门 / visual_range。
        confidence_threshold: 自动通过阈值。

    返回:
        与 :func:`build_pretranslate_trace_steps` 相同结构的两步列表。
    """
    retrieval = await service._retrieve_similar(
        source_entry=source_entry,
        target_lang=target_lang,
        department=department,
    )
    return build_pretranslate_trace_steps(retrieval, confidence_threshold)


def format_astream_events(events: list[dict[str, Any]]) -> str:
    """将 LangGraph ``astream_events`` 事件列表格式化为可读文本。

    用于观察 TermLearningGraph 运行时「哪个节点刚执行完」。

    参数:
        events: ``graph.astream_events(...)`` 收集的 dict 列表。

    返回:
        多行文本，每行形如 ``[on_chain_start] discover``；空列表返回 ``(no events)``。
    """
    lines: list[str] = []
    for event in events:
        kind = event.get("event", "unknown")
        name = event.get("name", "")
        lines.append(f"[{kind}] {name}")
    return "\n".join(lines) if lines else "(no events)"
