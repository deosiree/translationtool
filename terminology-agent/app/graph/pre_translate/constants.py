"""PreTranslateGraph 可调常量。

含 RAG 阈值、LLM 默认置信度、Phase 3a Grep 线及 Phase 3b coverage 阈值。
"""

from __future__ import annotations

# fuzzy 候选置信度 ≥ 此值且整句取自术语 → translation_source=term
FUZZY_AUTO_FLOOR: float = 0.95

# LLM 机翻默认置信度（通常低于工作台默认阈值 0.8 → needs_human）
LLM_DEFAULT_CONFIDENCE: float = 0.65

# ── Grep 线（Phase 3a）──
GREP_WORD_SCORE: float = 0.9  # 词级命中默认 score（整句 exact 为 1.0）
TRIE_CACHE_TTL_SEC: int = 300  # term_word Trie 进程内缓存 TTL
ENABLE_GREP_RETRIEVE: bool = True  # False 时退化为纯 RAG 检索

# ── 拆解拼装（Phase 3b）──
COVERAGE_FLOOR: float = 0.85  # span 覆盖率 ≥ 此值 → decomposed 自动回填
