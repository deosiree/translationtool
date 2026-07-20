# 0011 — Harness 给人看的文案使用简体中文

Date: 2026-07-20

## Status

Accepted

## Context

本仓库自 `repository-harness` 接入后，运维工作流文档（`FEATURE_INTAKE`、`CONTEXT_RULES`、`HARNESS` 正文等）多为上游英文。此前仅中文化了 Agent 入口块与 Eval 展示面，导致日常阅读路径「入口中文、正文英文」。团队协作与 Agent 代读均以简体中文为准，需要把语言边界写进决策，避免再次被上游英文模板覆盖后无据可查。

## Decision

1. **给人看的 Harness 文案一律使用简体中文**：文档正文与标题、表格说明、模板占位提示、Agent harness 块、Eval 的 `task.md` / `rubric.md` / review 文案、脚本中面向人的日志与注释。
2. **机器标识保留英文**：`harness-cli` 子命令与 flag、题 ID、`env.yaml` 的 `type`、路径、JSON/SQLite 字段名、编排契约 schema key。
3. 专有名词首次可写「中文（English）」，后文优先用中文；命令与路径字面量不译。
4. [`docs/contracts/harness-orchestration-v1.md`](../contracts/harness-orchestration-v1.md) 作为机器契约：标识符与结构保持英文，仅允许文首中文导读。
5. 语言约定同时写入 [`docs/HARNESS.md`](../HARNESS.md)「语言约定」一节，作为日常操作入口。

## Alternatives Considered

1. 保持上游英文、仅中文化 Agent 块 — 否决：Agent 必读路径仍掉进英文正文，体验割裂。
2. 连 CLI 参数与契约字段一并中文化 — 否决：破坏脚本、CI 与外部编排兼容。

## Consequences

Positive:

- 人类与 Agent 共用同一套中文工作流说明。
- 与 Git 提交说明中文约定、Eval 人工审查中文约定一致。

Tradeoffs:

- 与上游 `repository-harness` 英文原文会漂移；合入上游时需人工合并译文，不可整文件覆盖。

## Follow-Up

- 按计划完成 `docs/` 运维文档、模板、上游 decision `0001`–`0007` 的中文化，以及 Eval/脚本给人看文案补漏。
- 合入上游 harness 文档时，默认保留本仓库中文版本，仅移植语义变更。
