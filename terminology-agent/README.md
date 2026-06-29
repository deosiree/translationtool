# Terminology Agent

AI 术语学习 Agent — 基于 **FastAPI + LangGraph**，为词条翻译工具提供 RAG 预翻译、术语发现与人工审核入库。

← [[README]] · [项目根 README](../README.md) | [[references/本地开发]] · [本地开发](../references/本地开发.md) | [[references/agent-testing]] · [测试与 Trace](../references/agent-testing.md)

---

## 技术栈

| 组件 | 技术 |
|------|------|
| Web 框架 | FastAPI |
| 工作流 | LangGraph |
| ORM | SQLAlchemy 2.x + aiomysql |
| 校验 | Pydantic v2 |

---

## 快速启动

```powershell
cd terminology-agent
copy .env.example .env          # 首次：填入 LLM_API_KEY
pip install -e ".[dev]"         # 含 pytest、ipython
uvicorn app.main:app --host 0.0.0.0 --port 18002 --reload
```

- OpenAPI：http://localhost:18002/docs
- 健康检查：http://localhost:18002/agent/health

> 本地 Agent 与 Docker 容器不能同时占 18002：`docker stop terminology-agent`

---

## 测试

```powershell
pytest -v                       # 全量用例（81），无需 MySQL / LLM Key
pytest app/services/pre_translate/tests -v
pytest app/graph/pre_translate/tests -v
```

完整用例清单、逐条运行命令、TDD 工作流见 [[references/agent-testing]] · [agent-testing.md](../references/agent-testing.md)。

---

## Trace 可视化

打开 `devtools/trace_agent_demo.py`，在编辑器中逐 `# %%` cell 运行，可查看：

- LangGraph **Mermaid 静态图**
- PreTranslate **逐步 trace**（confidence / route）

详见 [[references/agent-testing]] §6。

---

## 主要目录

| 路径 | 作用 |
|------|------|
| `app/services/pre_translate/` | 批量预翻译领域编排（`service.py` 入口） |
| `app/services/term_audit/` | 审核列表/详情/确认入库 |
| `app/graph/pre_translate/` | LangGraph 工作流域（State / Nodes / Edges / Builder / Runner） |
| [`app/graph/pre_translate/README.md`](app/graph/pre_translate/README.md) | PreTranslate 域 SSOT（**双轨 Mermaid**） |
| `app/graph/README.md` | Graph 层索引与三要素映射 |
| `app/api/router.py` | HTTP 接口（`/agent/*`） |
| `app/repository/term_repo.py` | 术语库 / audit 数据访问 |
| `app/repository/word_repo.py` | term_word Grep 线数据访问 + trie 缓存 |
| [`app/shared/term_word/README.md`](app/shared/term_word/README.md) | term_word 库代码（在线 trie/extract + 离线 etl/） |
| `scripts/build_word_index.py` | term_word 全量建库 CLI（`python -m scripts.build_word_index`） |
| `app/models/word.py` | term_word ORM（`TermWord` / `TermWordConflict`） |
| `app/schemas/agent.py` | 请求/响应 Pydantic 模型 |
| `app/conftest.py` | pytest 共享 fixtures |
| `app/**/tests/` | 共置测试（TDD 友好） |
| `devtools/trace_agent_demo.py` | Interactive Trace Demo |

## 全项目结构

```
terminology-agent/
├── app/                        # 运行时库代码（≈ 前端 src/）
│   ├── api/
│   ├── services/
│   ├── graph/
│   ├── shared/term_word/       # trie, extract, etl/
│   ├── repository/
│   └── ...
├── scripts/
│   └── build_word_index.py     # 建库 CLI 薄壳
└── devtools/
    └── trace_agent_demo.py     # 本地 trace 调试
```

**term_word 建库**（terminology-agent 根目录）：

```powershell
python -m scripts.build_word_index --dry-run
python -m scripts.build_word_index --rebuild
```

结构标准 skill：[`项目结构-py-langGraph`](F:/Documents/Default-Obsidian/huiyanSkills/proj-skills/项目结构-py-langGraph/SKILL.md)

**分层约定**：

- `router` — HTTP 薄壳
- `services/<domain>/service.py` — 领域编排（类比前端 `views/<domain>/index.vue`）
- `graph/<workflow>/runner.py` — LangGraph 单条执行入口
- `graph/<workflow>/builder.py` — StateGraph 组装与 compile
- `graph/<workflow>/nodes/` — 节点；`edges/` — 条件边
- `shared/term_word/` — 在线 Grep（trie/extract）+ 离线 ETL（`etl/`）；建库 CLI 见 `scripts/build_word_index.py`

---

## 相关文档

| 文档 | 说明 |
|------|------|
| [[references/本地开发]] | Agent 优先本地开发、场景 A/B |
| [[references/agent-testing]] | 测试用例与 Trace 可视化 |
| [[references/README]] | References 总索引 |
| [[README]] | 项目根 README |
