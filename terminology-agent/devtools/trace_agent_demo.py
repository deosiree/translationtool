# %% [markdown]
# # Agent Trace Demo（新手导读）
#
# 这个文件**不是自动化测试**，而是帮你「看见」Agent 在干什么。
#
# ## 本 Demo 演示两条 Agent 链路
#
# | 链路 | 对应 cell | 业务场景 |
# |------|-----------|----------|
# | **TermLearningGraph** | cell 1 | 术语学习：发现新词 → LLM 建议 → 人工审核 → 入库 |
# | **PreTranslate** | cell 2 / 2a / 2b | 工作台预翻译：查术语库 → 算置信度 → 自动回填 or 人工审核 |
#
# ## 怎么跑
#
# 1. 从 **cell 0** 开始，按顺序逐格运行（`# %%` 上方的 Run Cell）
# 2. cell 0 解决 import 路径；cell 2a 用 `await`（不要用 `asyncio.run`）
#
# ## 怎么读 `print` 出来的字典
#
# PreTranslate trace 每一步是一个 `dict`，常见字段：
#
# - `stage`：当前阶段名（`RetrieveSimilar` = 查术语库，`AssessConfidence` = 判断是否自动通过）
# - `confidence`：置信度 0~1，越高越可信
# - `route`：路由结果 `auto_approved`（自动回填）或 `needs_human`（进待审核列表）
# - `retrieval_method`：`exact` 精确匹配 / `fuzzy` 模糊 / `hybrid` 无命中兜底
#
# 导出 HTML 报告：运行全部 cell 后，Interactive 窗口可 Export as HTML（即 `docs/agents/trace_agent_demo.html`）。

# %% 0. 路径引导
"""
Cell 0 — 让 Python 找到 terminology-agent 根目录。

为什么需要：
  Interactive 窗口的工作目录可能是仓库根 `translationtool/` 或 `devtools/`，
  而 `config/`、`app/` 都在 `terminology-agent/` 下。不先把根目录加入 sys.path，
  后面 `from app...` / `from config...` 会报 ModuleNotFoundError。
"""
import sys
from pathlib import Path


def _find_agent_root() -> Path:
    """
    自动定位 terminology-agent 根目录（内含 config/settings.py）。

    Returns:
        Path: 例如 .../translationtool/terminology-agent
    """
    # Interactive 里通常有 __file__，指向本 demo 文件
    if "__file__" in globals():
        candidate = Path(__file__).resolve().parents[1]  # devtools/ 的上一级
        if (candidate / "config" / "settings.py").is_file():
            return candidate
    # 兜底：从当前工作目录向上找
    cwd = Path.cwd()
    for candidate in (cwd, cwd.parent, cwd / "terminology-agent"):
        if (candidate / "config" / "settings.py").is_file():
            return candidate
    raise RuntimeError(
        "找不到 terminology-agent 根目录（需含 config/settings.py）。"
        "请 Open Folder 打开 terminology-agent，或从该目录启动 Interactive。"
    )


_ROOT = _find_agent_root()
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))  # 加入后 import app / config 才能成功

# %%
"""
Cell 1 准备 — 导入画图用的 IPython 组件和 LangGraph 工作流类。

TermLearningGraph：
  封装「术语学习」完整流程（discover → analyze_context → llm_suggest → review → update_termstore）。
  这里还没真正跑工作流，只是为下一格画静态流程图做准备。
"""
from IPython.display import Image, display

from app.graph.graph import TermLearningGraph

# %% 1. LangGraph 静态图
"""
Cell 1 — 画出 TermLearningGraph 的节点与连线（Mermaid PNG）。

你看到的那张流程图表示：
  - discover：在术语库里查这个词是否已存在
  - 若 is_new_term=True → analyze_context → llm_suggest → review → update_termstore
  - 若已存在 → 直接结束

注意：这里只画「蓝图」，不连数据库、不调 LLM。
"""
graph = TermLearningGraph()
# draw_mermaid_png() 把图渲染成 PNG 字节；display(Image(...)) 在 Interactive 里显示
display(Image(graph.graph.get_graph().draw_mermaid_png()))

# %% 2. PreTranslate trace（mock repo，无需 MySQL）
"""
Cell 2 — 搭建「工作台预翻译」的模拟环境。

PreTranslate 是另一条链路（和上面的 TermLearningGraph 不同）：
  用户在工作台选中一批中文词条 → Agent 去术语库 RAG 检索 → 给出译文和置信度。

下面用 Mock（假对象）代替真实 MySQL，这样 Demo 不需要起数据库。
"""
from unittest.mock import AsyncMock
from types import SimpleNamespace

from app.graph.trace_utils import collect_pretranslate_trace, build_pretranslate_trace_steps
from app.services.pre_translate_service import PreTranslateService

# 假装术语库里已有这么一条精确匹配记录（对应 t_translate 表一行）
mock_entry = SimpleNamespace(
    entry="正在查询第 %1/%2 个路径的OID...",  # 中文原文
    translate="Запрос OID пути %1/%2...",  # 已有俄文译文
)

session = AsyncMock()  # 假的数据库会话（本 Demo 不会真连 MySQL）
service = PreTranslateService(session)
service._repo = AsyncMock()
# find_exact：精确匹配时返回 mock_entry；find_fuzzy：本场景故意返回空列表
service._repo.find_exact = AsyncMock(return_value=mock_entry)
service._repo.find_fuzzy = AsyncMock(return_value=[])

# %% 2a. 精确匹配 trace
"""
Cell 2a — 跑一条「精确匹配」词条，打印 Agent 决策轨迹。

输入：工作台词条「正在查询第 %1/%2 个路径的OID...」
预期：
  1) RetrieveSimilar → retrieval_method=exact, confidence=1.0
  2) AssessConfidence → confidence(1.0) >= threshold(0.8) → route=auto_approved

print 的每一行是一个 dict，就是 trace 的一个「站点快照」。
"""
# Interactive 里已有事件循环，必须用 await，不能用 asyncio.run()
trace = await collect_pretranslate_trace(
    service,
    source_entry="正在查询第 %1/%2 个路径的OID...",
    target_lang="俄文",  # 限定在俄文术语里检索
    department="通用平台部",  # 对应术语库 visual_range 字段
    confidence_threshold=0.8,  # 前端 Slider 阈值，>=0.8 才自动回填
)
for step in trace:
    print(step)  # 例如 {'stage': 'RetrieveSimilar', 'confidence': 1.0, ...}

# %% 2b. 无命中 trace（纯函数演示）
"""
Cell 2b — 演示「术语库完全没命中」时 trace 长什么样。

这里不调用 service，直接构造 retrieval 结果 dict，
用 build_pretranslate_trace_steps 只看「评估置信度 → 路由」这一步。

预期 print：
  - confidence=0.45（固定低置信度）
  - route=needs_human（低于阈值 0.8，进 term_agent_audit 待人工确认）
"""
no_match = {
    "confidence": 0.45,
    "suggested_translation": "[Agent] 全新词条",  # 无命中时的占位译文
    "similar_terms": [],
    "retrieval_method": "hybrid",  # hybrid = 精确+模糊都没找到
    "reasoning": "未找到相似术语，建议人工审核",
}
for step in build_pretranslate_trace_steps(no_match, 0.8):
    print(step)

# %% 3. astream_events 占位
"""
Cell 3 — LangGraph 运行时事件流（占位）。

真实跑 TermLearningGraph.run() 时，可用 graph.astream_events() 逐节点看：
  哪个 node 执行了、LLM 返回了什么。

当前 events=[] 所以 print 出 "(no events)"。
后续可在此 mock LLM 后填入真实 events 列表。
"""
from app.graph.trace_utils import format_astream_events

print(format_astream_events([]))  # 无事件时输出 "(no events)"

# %%
