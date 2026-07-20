# 0003 — 通用规格 Intake Harness

Date: 2026-05-05

## Status

Accepted

## Context

Harness v0 最初随项目特定的 `SPEC.md`、产品文档、候选 epic、架构假设与验证示例一并交付。这使 harness 对首个项目有用，但过于具体，难以作为新项目的外壳复用。

期望方向是：默认 harness 可等待任意用户提供的规格，从中推导产品文档，然后继续同一 intake、story、proof 与 decision 循环。

## Decision

从 Harness v0 中移除已跟踪的项目特定规格与预切分产品域。

harness 现以如下状态启动：

- 无内置 `SPEC.md`。
- 产品文档为空，仅保留 intake 指引。
- 通用 story 与 epic 示例。
- 与栈无关的架构发现规则。
- 与栈无关的验证列。
- 源层级（source hierarchy）将未来用户提供的规格视为输入材料，而非永久存续的产品事实。

## Alternatives Considered

1. 保留原始 `SPEC.md` 作为示例 — 否决：示例可能被误当作当前产品事实。
2. 将原始产品文档移入 examples 文件夹 — 暂否：用户要求干净的默认 harness。

## Consequences

Positive:

- 仓库更易复用于任意新项目。
- 未来规格可定义各自产品域与栈。
- Agent 更少混淆模板事实与产品事实。

Tradeoffs:

- 在下一份规格提供前，harness 具体示例更少。
- 首次 spec intake 须先创建产品文档与候选 epic，实现规划才能精确。

## Follow-Up

- 若重复项目暴露出稳定格式，再增加 spec-intake 模板。
