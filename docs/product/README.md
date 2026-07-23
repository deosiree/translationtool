# 产品文档（Product Docs）

本目录在 Harness v0 中有意保持通用且基本为空。

当用户提供项目规格时，在此推导较小的产品契约文件，而不是把一份大规格当作活计划。按规格中真实存在的产品域命名，例如 `overview.md`、`billing.md`、`workflows.md`、`permissions.md` 或 `api-conventions.md`。

规格尚未提供前，不要为了「填目录」而创建域文件。空结构比虚假产品真相更健康。

## 当前产品契约

本通用目录不附带消费者项目专用产品契约。上游 `repository-harness` 的产品契约位于根 README、Harness 操作文档、版本化编排契约、故事包与可执行测试中。

例外（学习驱动开发，非产品域契约）：`learning-plan.md`（总路线图）、`learning-log.md`（倒序里程碑日志）。

## 更新规则

行为变更时：

1. 更新受影响的产品文档。
2. 更新或创建故事包。
3. 用 `scripts/bin/harness-cli story add` 或 `scripts/bin/harness-cli story update` 更新持久证明状态。
4. 若变更影响架构、范围、风险或既有产品规则，记录决策。
