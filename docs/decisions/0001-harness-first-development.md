# 0001 — Harness 优先开发

Date: 2026-05-05

## Status

Accepted

## Context

仓库当前仅有产品 README 与一份体量很大的产品规格说明，尚无应用实现。

项目很可能在较长时间内由人类定方向、Agent 负责实现，且需求会以多条演进中的 story 形式持续变化。单靠一份巨型规格说明不足以支撑安全的 Agent 协作，因为难以定位当前事实、风险、验证依据与变更历史。

## Decision

在搭建产品代码脚手架之前，先创建 Harness v0。

Harness v0 定义：

- Agent 入口。
- 产品文档拆分。
- Feature intake 与风险车道（risk lanes）。
- Story packet 模板。
- 决策记录（decision records）。
- 测试矩阵（test matrix）。
- Harness backlog。

本决策不创建应用代码、占位脚本、CI 或测试。

## Consequences

Positive:

- 实现开始前，Agent 即有清晰的操作模型。
- 产品事实可从巨型规格中拆出。
- 高风险工作在改代码前进入更慢车道。
- Harness 自身的演进纳入日常工作。

Tradeoffs:

- 部分文档在真实 story 跑通前仅为占位。
- 验证命令在实现开始前只是契约。
- Harness 须保持足够精简，以便根据真实摩擦持续修订。
