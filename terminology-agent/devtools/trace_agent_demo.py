"""PreTranslateGraph 交互式 Trace Demo（`# %%` cell 脚本）。

用途
----
在 VS Code / Cursor Interactive Window 中逐 cell 运行，验证预翻译图的可视化与 trace 工具链。

Cell 概览
---------
0. 路径引导 + 加载 ``.env``（必须先跑）
1. LangGraph 静态 Mermaid 图
2. 业务 trace：mock DB，跑完整 ``PreTranslateGraph``，打印各节点 ``state.trace`` 摘要
3. 流式 trace：演示 ``astream_events`` 低层事件格式化（可选；跑完即 demo 结束）

与 Cell 2 的区别
----------------
- **Cell 2** — 终态导向：各节点写入 ``state["trace"]``，适合验收「走了哪条路、置信度多少」。
- **Cell 3** — 事件流导向：LangGraph ``astream_events`` 逐帧输出 ``on_chain_start/end`` 等，
  供将来接 LangSmith、前端 SSE 或细粒度调试；**不是主链路必跑项**。

运行方式
--------
``pip install -e ".[dev]"`` 后打开本文件，从 cell 0 起逐格 Run Cell。
"""

# %% [markdown]
# # Agent Trace Demo（PreTranslateGraph 单链路）
#
# | Cell | 内容 | 预期输出 |
# |------|------|----------|
# | 0 | 路径 + `.env` | 无（前置依赖） |
# | 1 | 静态图 | Mermaid PNG |
# | 2 | 精确匹配 trace（mock repo） | 6 步 trace dict |
# | 3 | `astream_events` 格式化 | 事件流文本；**跑完即结束** |
#
# 流水线：`retrieve → rerank → resolve → (term|llm) → assess → write`

# %% 0. 路径引导
import sys
from contextlib import contextmanager
from pathlib import Path


def _find_agent_root() -> Path:
    """定位 terminology-agent 项目根（含 ``config/settings.py``）。

    Returns:
        含 ``config/settings.py`` 的目录路径。

    Raises:
        RuntimeError: 从 ``__file__`` 与 cwd 均无法定位根目录时。
    """
    if "__file__" in globals():
        candidate = Path(__file__).resolve().parents[1]
        if (candidate / "config" / "settings.py").is_file():
            return candidate
    cwd = Path.cwd()
    for candidate in (cwd, cwd.parent, cwd / "terminology-agent"):
        if (candidate / "config" / "settings.py").is_file():
            return candidate
    raise RuntimeError("找不到 terminology-agent 根目录")


_ROOT = _find_agent_root()
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

# 与 pytest conftest 一致：导入 app 前先加载 .env，否则 Settings() 会 ValidationError
from dotenv import load_dotenv

_ENV_PATH = _ROOT / ".env"
if not _ENV_PATH.is_file():
    raise RuntimeError(
        "Demo 需要 terminology-agent/.env。"
        "请先执行：copy .env.example .env"
    )
load_dotenv(_ENV_PATH)

# %% 1. PreTranslateGraph 静态图
# 编译 LangGraph 并导出 Mermaid PNG，确认节点/边与 builder 一致。
from IPython.display import Image, display

from app.graph.pre_translate.runner import PreTranslateGraph

graph = PreTranslateGraph()
display(Image(graph.graph.get_graph().draw_mermaid_png()))

# %% 2. 精确匹配 trace（mock repo）
# mock TermRepository 精确命中 + stub Grep 线，验收 term → auto_approved 路径。
from unittest.mock import AsyncMock, patch
from types import SimpleNamespace

from app.graph.pre_translate.utils.trace import collect_pretranslate_trace

mock_entry = SimpleNamespace(
    entry="正在查询第 %1/%2 个路径的OID...",
    translate="Запрос OID пути %1/%2...",
)

mock_repo = AsyncMock()
mock_repo.find_exact = AsyncMock(return_value=mock_entry)
mock_repo.find_fuzzy = AsyncMock(return_value=[])
mock_repo.create_pretranslate_audit = AsyncMock()

session = AsyncMock()


def _repo_factory(_session):
    """``TermRepository`` 工厂：始终返回 cell 级 ``mock_repo``。"""
    return mock_repo


def _word_repo_factory(_session):
    """``WordRepository`` 工厂：Grep 线返回空，避免真实 SQL。"""
    word_repo = AsyncMock()
    word_repo.list_distinct_words = AsyncMock(return_value=[])
    word_repo.find_by_word = AsyncMock(return_value=[])
    return word_repo


async def _empty_trie(*_args, **_kwargs):
    """``load_trie_for_lang`` stub：返回空 Trie。"""
    from app.shared.term_word.trie import Trie

    return Trie()


@contextmanager
def _pretranslate_io_patches():
    """Cell 2/3 共用的 I/O mock 上下文（Term + Grep + write）。"""
    with patch(
        "app.graph.pre_translate.nodes.features.io.retrieve_similar.TermRepository",
        _repo_factory,
    ), patch(
        "app.graph.pre_translate.nodes.features.io.retrieve_similar.WordRepository",
        _word_repo_factory,
    ), patch(
        "app.repository.trie_cache.WordRepository",
        _word_repo_factory,
    ), patch(
        "app.graph.pre_translate.nodes.features.io.retrieve_similar.load_trie_for_lang",
        _empty_trie,
    ), patch(
        "app.graph.pre_translate.nodes.features.io.write_result.TermRepository",
        _repo_factory,
    ):
        yield


async def _run_exact_match_trace():
    """跑单条精确匹配预翻译并返回 trace 步骤列表。"""
    with _pretranslate_io_patches():
        return await collect_pretranslate_trace(
            session,
            source_entry="正在查询第 %1/%2 个路径的OID...",
            target_lang="俄文",
            department="通用平台部",
            confidence_threshold=0.8,
        )


def _run_async(coro):
    """无 event loop 时用 ``asyncio.run``；Jupyter 已有 loop 时在独立线程跑。

    Args:
        coro: 待执行的协程对象。

    Returns:
        协程的返回值。
    """
    import asyncio
    import threading

    try:
        asyncio.get_running_loop()
        in_running_loop = True
    except RuntimeError:
        in_running_loop = False

    if not in_running_loop:
        return asyncio.run(coro)

    box: dict = {}

    def _worker() -> None:
        box["result"] = asyncio.run(coro)

    thread = threading.Thread(target=_worker, daemon=True)
    thread.start()
    thread.join()
    return box["result"]


trace = _run_async(_run_exact_match_trace())
for step in trace:
    print(step)

# %% 3. astream_events 流式事件（可选 · demo 最后一步）
#
# 此前仅 ``format_astream_events([])`` → 打印 ``(no events)``，看起来像「啥也没发生」。
# 本 cell 分三段：
#   3a 空列表 — 回归占位行为
#   3b 静态样例 — 展示格式化长什么样
#   3c 真实图事件 — 与 cell 2 相同 mock 下收集 astream_events
#
# **跑完 3c 后 demo 即全部结束**；cell 3 不参与业务验收，可跳过。
from app.graph.pre_translate.utils.trace import format_astream_events

print("=== 3a. 空事件（占位回归）===")
print(format_astream_events([]))
print()

print("=== 3b. 静态样例（不跑图）===")
_sample_events = [
    {"event": "on_chain_start", "name": "retrieve_similar"},
    {"event": "on_chain_end", "name": "retrieve_similar"},
    {"event": "on_chain_start", "name": "assess_route"},
    {"event": "on_chain_end", "name": "write_result"},
]
print(format_astream_events(_sample_events))
print()


async def _collect_astream_events(*, max_events: int = 30) -> list[dict]:
    """与 cell 2 相同 mock 下收集 LangGraph ``astream_events`` 并简化为 ``{event, name}``。

    Args:
        max_events: 最多保留的事件条数，避免 Interactive 输出过长。

    Returns:
        供 ``format_astream_events`` 消费的事件 dict 列表。
    """
    compiled = PreTranslateGraph().graph
    initial = {
        "source_text": "正在查询第 %1/%2 个路径的OID...",
        "target_lang": "俄文",
        "department": "通用平台部",
        "confidence_threshold": 0.8,
        "entry_comment": "",
        "similar_terms": [],
        "trace": [],
    }
    config = {"configurable": {"session": session}}
    events: list[dict] = []

    with _pretranslate_io_patches():
        async for ev in compiled.astream_events(initial, config, version="v2"):
            events.append({"event": ev.get("event"), "name": ev.get("name", "")})
            if len(events) >= max_events:
                break
    return events


print("=== 3c. 真实图 astream_events（mock 同 cell 2，节选）===")
_astream_events = _run_async(_collect_astream_events())
print(format_astream_events(_astream_events))
print()
print(f"（共采集 {len(_astream_events)} 条，上限 30；完整流可增大 max_events）")
print("=== Demo 结束 ===")

# %%
