# LangGraph 工作流层

本目录按 **图域包** 组织 LangGraph：`State` / `Nodes` / `Edges` / `Builder` / `Runner`。

不含 HTTP 编排与批量循环 — 那些职责在 [`../services/`](../services/)。

---

## 域索引

| 工作域 | README | 业务 |
|--------|--------|------|
| `pre_translate/` | [`pre_translate/README.md`](pre_translate/README.md) | 单条词条 RAG 预翻译 |

新增工作域时：创建 sibling 包 + **必写** 域级 README（含双轨 Mermaid），并在此表登记。

---

## LangGraph 三要素目录映射

| LangGraph 概念 | 目录 / 文件 | 职责 |
|----------------|-------------|------|
| **State** | `<workflow>/state.py` | TypedDict schema |
| **Nodes** | `<workflow>/nodes/` | 执行逻辑（可 I/O、LLM） |
| **Edges** | `<workflow>/edges/` | 条件路由（只读 state） |
| **Compile** | `<workflow>/builder.py` | StateGraph 注册 + compile |
| **Invoke** | `<workflow>/runner.py` | 对外 run 入口 |
| **Domain** | `<workflow>/domain/` | 枚举、格式化 |
| **Utils** | `<workflow>/utils/` | 纯函数 |
| **Prompts** | `<workflow>/prompts/` | LLM 模板 |
| **Shared** | [`shared/`](shared/) | 跨图 devtools 工具 |

---

## 与 services 层边界

- `services/<domain>/service.py` — 批量循环、过滤、计数（Application 编排）
- `graph/<workflow>/runner.py` — 单条 LangGraph 执行

---

## Mermaid 双轨规范

每个 `graph/<workflow>/README.md` 中，**每张流程图必须成对**：

| 轨道 | 标题 | 节点标签 |
|------|------|----------|
| 源码对照 | `### 源码对照（与 builder 一致）` | 函数/节点名，如 `retrieve_similar` |
| 业务可读 | `### 业务说明（人类阅读）` | 中文动作，如「检索术语库」 |

详见 skill：[`项目结构-py-langGraph`](F:/Documents/Default-Obsidian/huiyanSkills/proj-skills/项目结构-py-langGraph/references/Mermaid双轨写作规范.md)

---

## 新增文件决策树

1. 会改 state 并执行 I/O 或 LLM？ → `nodes/features/<kind>/`
2. 只读 state 决定下一节点？ → `edges/after_<node>.py`
3. 只读 state 写策略字段？ → `nodes/intentions/`
4. 纯函数、无副作用、**仅本 workflow**？ → `<workflow>/utils/`
5. 枚举 / 格式化？ → `<workflow>/domain/`
6. 注册到 StateGraph？ → `builder.py`
7. 跨 workflow 的 graph 工具（tracing 等）？ → [`shared/`](shared/)
8. 跨 **graph + repository** 的在线纯逻辑？ → [`../shared/<topic>/`](../shared/)（如 [`term_word`](../shared/term_word/README.md)）
9. 离线建库库代码？ → [`../shared/<topic>/etl/`](../shared/)；CLI 入口 → [`../../scripts/`](../../scripts/)；**含 DB/I/O 则进 `repository/`**

---

## 反模式（禁止）

- graph 根目录平铺 `routes.py`、`*_graph.py`、`trace_utils.py`
- 条件边函数放进 `nodes/`
- 新建 `graph/<workflow>/` 却不写 README
- 在 `app/` 根新建与实体同名的包（如 `app/word/`）；跨层共享用 `app/shared/<topic>/`
