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
pytest -v                       # 全量用例，无需 MySQL / LLM Key
pytest app/services/tests -v    # 只跑 Service 层
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
| `app/services/pre_translate_service.py` | 工作台批量 RAG 预翻译 |
| `app/services/term_audit_service.py` | 审核列表/详情/确认入库 |
| `app/services/term_learning_run_service.py` | 单条术语发现编排 |
| `app/graph/` | LangGraph 工作流与节点 |
| `app/graph/trace_utils.py` | Trace 工具（测试与 Demo 共用） |
| `app/api/router.py` | HTTP 接口（`/agent/*`） |
| `app/repository/term_repo.py` | 术语库 / audit 数据访问 |
| `app/schemas/agent.py` | 请求/响应 Pydantic 模型 |
| `app/conftest.py` | pytest 共享 fixtures |
| `app/**/tests/` | 共置测试（TDD 友好） |
| `devtools/trace_agent_demo.py` | Interactive Trace Demo |

**分层约定**：`router` 只做 HTTP 解析、委托与响应包装；业务逻辑进 `app/services/`。

---

## 相关文档

| 文档 | 说明 |
|------|------|
| [[references/本地开发]] | Agent 优先本地开发、场景 A/B |
| [[references/agent-testing]] | 测试用例与 Trace 可视化 |
| [[references/README]] | References 总索引 |
| [[README]] | 项目根 README |
