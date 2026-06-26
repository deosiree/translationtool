"""节点：Discover — 在术语库中检查词条是否已存在。"""

from langgraph.types import RunnableConfig

from app.graph.state import TermState
from app.repository.term_repo import TermRepository


async def discover_node(state: TermState, config: RunnableConfig) -> TermState:
    """查询术语库，判断 ``source_text`` 是否已有收录。

    若已存在：
      - matched_term ← 已有译文
      - is_new_term ← False
      - next_node ← "end"（无需继续后续节点）
    若不存在：
      - is_new_term ← True
      - next_node ← "analyze_context"（进入上下文分析）
    """
    session = config["configurable"]["session"]
    repo = TermRepository(session)
    matches = await repo.find_by_chinese(state["source_text"])

    if matches:
        best = matches[0]
        state["matched_term"] = best.translate
        state["match_confidence"] = 1.0
        state["is_new_term"] = False
        state["next_node"] = "end"
    else:
        state["matched_term"] = None
        state["match_confidence"] = None
        state["is_new_term"] = True
        state["next_node"] = "analyze_context"

    return state
