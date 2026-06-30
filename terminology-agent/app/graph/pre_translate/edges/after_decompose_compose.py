"""条件边：decompose_compose 之后 — 词级拼装够不够用、要不要改走 LLM。

Phase 3b 分流：拼装覆盖率达标 → 用术语库拼出的译文走置信度阈值；
未达标 → 丢弃拼装结果，改 LLM 整句机翻（术语学习常见 65% +「混合检索」说明）。
"""

from __future__ import annotations

from typing import Literal

from app.graph.pre_translate.constants import COVERAGE_FLOOR
from app.graph.pre_translate.state import PreTranslateState


def route_after_decompose_compose(
    state: PreTranslateState,
) -> Literal["compose_ok", "llm_fallback"]:
    """词级拼装是否足够好：够好则走阈值分流，否则回退 LLM。

    判定逻辑（对应 state 字段 → 页面上看到什么）：

    - ``coverage``（拼装覆盖率，0~1，无单独列）
      源词条里有多少字符被「Grep 词片 + 已有译法」覆盖。
      例如 ``ADM/3B-文件ADM/3B-系统`` 两片都命中 → coverage≈100%。
      中间夹未收录字（如「与」）会拉低 coverage。
      阈值见 ``COVERAGE_FLOOR``（默认 85%）。

    - ``suggested_translation``（建议译文）
      达标时 = 词片译文直接拼接（如 ``FileSystem``），会写入工作台**翻译列**，
      或进入术语学习**建议翻译**；检索方式显示 **「拆解拼装」**。

    下一跳（图节点名 → 业务含义）：

    - ``compose_ok`` → ``assess_route``（置信度阈值分流）
      对比 ``confidence`` 与用户阈值：≥ 阈值 → 工作台**自动回填**；
      低于阈值 → **术语学习**待审核（Agent 说明里常有 coverage 百分比）。

    - ``llm_fallback`` → ``translate_suggest``（LLM 整句机翻）
      拼装不可用，改用机翻；检索方式多为 **「混合检索」**，置信度通常 65%。

    Args:
        state: 上游 ``decompose_compose_node`` 写入 ``coverage``、
            ``suggested_translation``（仅 coverage 达标时有值）。

    Returns:
        ``compose_ok``：用拼装译文继续分流；``llm_fallback``：改走 LLM。
    """    
    coverage = state.get("coverage") or 0.0
    suggested = state.get("suggested_translation")
    if suggested and coverage >= COVERAGE_FLOOR:
        return "compose_ok"
    return "llm_fallback"
