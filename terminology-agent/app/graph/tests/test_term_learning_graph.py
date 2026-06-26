"""LangGraph 条件路由单元测试。"""



import pytest

from langgraph.graph import END



from app.graph.routes import route_after_discover





@pytest.mark.graph

def test_route_after_discover_new_term():

    """新词 → 路由到 analyze_context 继续处理。"""

    state = {"is_new_term": True}

    assert route_after_discover(state) == "analyze_context"





@pytest.mark.graph

def test_route_after_discover_existing():

    """已有词 → 路由到 END 直接结束。"""

    state = {"is_new_term": False}

    assert route_after_discover(state) == END

