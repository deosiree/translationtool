# 0002 — 种子规格之后的产品生命周期

Date: 2026-05-05

## Status

Superseded by `0003-generic-spec-intake-harness.md`

## Context

Harness v0 最初假定仓库会包含一份用于首个产品的种子规格文件（seed specification）。本决策说明 Agent 应如何将这份初始规格分解为产品文档、story packet、实现与验证证据，并在种子规格耗尽后继续工作。

该方式适合单一项目，但降低了 Harness 的可复用性。

## Decision

将初始规格视为种子与历史快照，而非永久存续的 living product plan。

初始规格耗尽后，新工作应通过同一 harness 循环进入，输入类型为下列之一：

- Change request。
- New initiative。
- Maintenance request。
- Harness improvement。

`docs/product/` 下的产品文档、`docs/stories/` 下的 story packet、`docs/TEST_MATRIX.md` 中的验证证据，以及 `docs/decisions/` 下的决策记录，构成日常运作面。

未来大型产品域应以范围化的 initiative 笔记捕获，而非追加到种子规格或重写为第二份单体规格。

## Consequences

Positive:

- 原始规格保持稳定，作为历史上下文。
- 产品事实迁入更小、更当前、更易维护的文件。
- 后续工作继续使用同一 intake、story、proof 与 harness 演进循环。
- 大型想法仍可规划，而无需再造一份 oversized spec。

Tradeoffs:

- 若大型新产品域变常见，仓库最终可能需要 initiative 模板。
- Agent 须谨慎更新产品文档与测试，而非在初始 buildout 后仍依赖种子规格。
