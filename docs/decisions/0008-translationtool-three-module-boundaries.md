# 0008 — Translation Tool 三模块边界

## Status

Accepted

## Context

仓库同时包含 Vue/Electron UI、Spring Boot API、FastAPI+LangGraph Agent。Agent 容易跨目录乱改或把 Harness 通用脚手架目录当成业务结构。

## Decision

1. 业务落点固定为：
   - UI → `translation/`
   - Backend → `translationtoolservice/`
   - Agent → `terminology-agent/`
2. 端口契约：UI `18000`、Java `18001`、Agent `18002`。
3. 跨模块契约变更必须先 intake，再写 `docs/decisions/`，再改代码。
4. `docs/ARCHITECTURE.md` 描述本产品真实结构，不以上游 Harness 的 `app/domain/` 模板脚手架化本仓库。

## Consequences

- Agent 开工先读 `AGENTS.md` 与 `docs/ARCHITECTURE.md`。
- 全栈需求拆 story，避免单次 PR 无边界横扫三端。
- 旁路目录（`translation-assistant/` 等）默认只读，除非需求点名。
