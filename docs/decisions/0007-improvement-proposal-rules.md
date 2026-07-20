# 0007 — 改进提案规则

Date: 2026-06-04

## Status

Accepted

## Context

Phase 5 增加 `harness-cli propose`，改变 harness 演进模型。该命令须有用，但不能成为 unchecked 的 scope creep 或 circular recommendations 来源。

## Decision

改进提案是 advisory、rule-based 且 evidence-backed。命令可汇总 repeated friction、repeated interventions 与 audit drift。仅当提供 `--commit` 时，才可创建 `proposed` backlog items。

每条提案必须包含：

- 受影响的 Harness 组件，
- 具体证据，
- 预测影响，
- risk lane，
- 建议动作，
- 验证计划，
- 置信度。

高风险提案的实现仍须 human review；若变更 source hierarchy、architecture direction、validation requirements 或 risk policy，还须 durable decision record。

## Alternatives Considered

1. 生成自由形式的 LLM recommendations — 否决：Phase 5 需要 deterministic 且可审计的演进角色。
2. 自动应用 proposed changes — 否决：harness 不得在未经 review 的情况下改写自身 policy。
3. 仅报告 audit findings — 否决：H5 要求 proposed improvements，而非仅 drift detection。

## Consequences

Positive:

- 重复的运维模式可变为 backlog items。
- 提案输出可解释、可测试。
- Human review 仍是 risky harness changes 的 gate。

Tradeoffs:

- Rule-based grouping 可能漏掉语义相近的表述。
- 基于 audit 的提案可能是 housekeeping，而非 strategic evolution。

## Follow-Up

- 在后续 phase 用 benchmark runs 与 closed backlog outcomes 提升 proposal quality。
