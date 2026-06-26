# %% [markdown]
# # Agent Trace Demo（PreTranslateGraph 单链路）
#
# 演示工作台预翻译图：retrieve → rerank → resolve → (term|llm) → assess → write

# %% 0. 路径引导
import sys
from pathlib import Path


def _find_agent_root() -> Path:
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

# %% 1. PreTranslateGraph 静态图
from IPython.display import Image, display

from app.graph.pre_translate.runner import PreTranslateGraph

graph = PreTranslateGraph()
display(Image(graph.graph.get_graph().draw_mermaid_png()))

# %% 2. 精确匹配 trace（mock repo）
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
    return mock_repo


with patch(
    "app.graph.pre_translate.nodes.features.io.retrieve_similar.TermRepository",
    _repo_factory,
), patch(
    "app.graph.pre_translate.nodes.features.io.write_result.TermRepository",
    _repo_factory,
):
    trace = await collect_pretranslate_trace(
        session,
        source_entry="正在查询第 %1/%2 个路径的OID...",
        target_lang="俄文",
        department="通用平台部",
        confidence_threshold=0.8,
    )
for step in trace:
    print(step)

# %% 3. astream_events 占位
from app.graph.pre_translate.utils.trace import format_astream_events

print(format_astream_events([]))

# %%
