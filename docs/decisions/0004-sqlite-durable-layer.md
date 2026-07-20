# 0004 — SQLite 持久层

Date: 2026-05-22

## Status

Accepted

## Context

Harness v0 将所有运维数据存于 markdown 文件：`TEST_MATRIX.md` 行、`HARNESS_BACKLOG.md` 项、决策记录与 story 状态。对人阅读友好，但对 Agent 造成摩擦：

- 编辑 markdown 表格易错且难校验。
- 缺少结构化方式查询历史 intake、trace 或 friction report。
- harness 缺少面向未来演进的 observability 基础。

近期 harness engineering 研究（arXiv:2604.25850、arXiv:2605.13357、arXiv:2603.28052）将 observability 与结构化 trace 视为 harness 改进的基础。三种路径均需要可查询的运维数据，而非散文式文档。

## Decision

增加 SQLite 数据库（`harness.db`）与薄 CLI（`scripts/bin/harness-cli`），作为运维 harness 数据的持久层。

数据库存储：

- **Intake records**：入站工作的分类。
- **Stories**：工作包及其验证 proof 状态（取代手工维护的 `TEST_MATRIX.md` 行）。
- **Decisions**：持久记录，可选 verification commands。
- **Backlog items**：harness 改进提案，含 predicted 与 actual impact。
- **Traces**：Agent 执行记录，含 actions、files、errors、outcome 与 harness friction。

schema 版本控制于 `scripts/schema/`。数据库文件 `.gitignore`，因每个项目实例生成各自运维数据。

策略文档（`HARNESS.md`、`FEATURE_INTAKE.md`、`ARCHITECTURE.md`）仍作人类可读参考。数据库存 Agent 产出，而非 Agent 应遵循的规范。

## Alternatives Considered

1. 全部保留在 markdown — 否决：无法结构化查询、observability 不可行，且迫使 Agent 编辑脆弱表格。
2. 使用 JSON 文件 — 否决：并发写入不安全，查询需自建工具。
3. 使用完整数据库服务器 — 否决：部署复杂度与 Harness v0 范围不匹配。

## Consequences

Positive:

- Agent 记录结构化数据，而非编辑 markdown 表格。
- Intake、story、decision、backlog 与 trace 数据可查询。
- harness 具备面向未来演进的 observability 基础。
- schema migration 使持久层可随 harness 成长。

Tradeoffs:

- 环境须可用 `sqlite3`。
- 数据库未纳入版本控制，每个实例从空库开始。
- 若 Agent 只用其一，markdown 文档与数据库可能漂移。

## Follow-Up

- init 时将既有决策（0001–0003）seed 进数据库。
- 按任务类型与 risk lane 增加 context engineering 规则。
- 持久层证明有用后，再增加 harness maturity ladder（H0–H4）。
