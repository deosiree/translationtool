"""翻译来源枚举与 Agent 说明（llm_reasoning）格式化。"""

from __future__ import annotations

from enum import Enum


class TranslationSource(str, Enum):
    """预翻译结果来源 — term / llm / hybrid（Phase 3a 启用 hybrid）。"""

    TERM = "term"
    LLM = "llm"
    HYBRID = "hybrid"


SOURCE_LABEL: dict[TranslationSource, str] = {
    TranslationSource.TERM: "基于术语",
    TranslationSource.LLM: "基于LLM机翻",
    TranslationSource.HYBRID: "基于混合检索",
}


def format_agent_reasoning(
    source: TranslationSource,
    detail: str | None = None,
) -> str:
    """将 translation_source 与补充说明格式化为前端 Agent 说明列文本。

    Args:
        source: 翻译来源枚举。
        detail: 可选补充说明（如「精确匹配术语库」）。

    Returns:
        形如「基于术语：精确匹配术语库」的展示文本。
    """
    base = SOURCE_LABEL[source]
    return f"{base}：{detail}" if detail else base
