# 0009 — 新后端走 Python，Java 仅维护

Date: 2026-07-16

## Status

Accepted

## Context

`translationtoolservice/`（Java / Spring Boot）由他人主责开发与演进。本侧正在把 `terminology-agent/`（Python FastAPI + LangGraph）建设为可承载业务能力的新后端。若 Agent 默认把新需求堆进 Java，会造成协作冲突、评审成本高、且偏离本侧演进方向。

## Decision

1. **仓库存在双后端**：遗留 Java（`:18001`）+ 新 Python（`:18002`）。
2. **后续新需求 / 新能力默认落在 Python**（`terminology-agent/`）。
3. **Java 仅用于遗留维护**（修 bug、安全、不得不做的兼容）；原则是 **能不动就不动**。
4. Feature Intake 必须标明 `backend=python`（默认）或 `backend=java-maintain`。
5. 任何主动扩大 Java 改动面的方案，须在 intake 中论证「为何不能只改 Python」，并优先请人类确认。

## Alternatives Considered

1. 继续以 Java 为唯一后端 — 否决：他人主责，本侧推进慢、冲突多。
2. 立刻废弃 Java — 否决：存量业务与 UI 仍依赖 Java，需共存过渡。

## Consequences

Positive:

- Agent 默认落点清晰，减少误改他人代码。
- 新能力可在 Python 侧快速迭代（API + Agent 同仓）。

Tradeoffs:

- 一段时间内 UI 需同时对接两套后端；跨后端契约需显式决策。
- 部分能力若强依赖 Java 存量接口，Python 需适配调用而非重写 Java。

## Follow-Up

- 新功能优先在 `terminology-agent/` 增加路由/用例与 pytest。
- 前端接新能力时优先打到 `:18002`（或对应 proxy），避免无必要改 Java。
- 若出现「必须改 Java」的硬依赖，单独开 decision 记录原因与范围。
