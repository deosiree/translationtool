# 术语表（Glossary）

## Agent（智能体）

在仓库内协作的 AI 编码协作者。

## Harness（协作运行时）

仓库级操作系统：告诉人类与 Agent 如何把意图变成安全的产品变更。

## Product Contract（产品契约）

产品当前的期望行为。一旦有实现，产品文档加上可执行测试成为活契约。

## Story Packet（故事包）

故事级工作文件或文件夹，描述产品契约、受影响文档、设计说明与验证期望。

## Feature Intake（功能分拣）

实现前把提示分类为 tiny、normal 或 high-risk 工作的步骤。

## Component Taxonomy（组件分类法）

从 Harness 文件与能力到其所服务职责的映射，用于评估覆盖、归因失败、识别缺失的 Harness 能力。

## Maturity Level（成熟度等级）

Harness 能力的可验证阶段，从 H0 裸环境到 H5 自改进 Harness。每级有必需文件、标准与 benchmark 指标。

## Trace Quality Tier（Trace 质量层级）

任务 trace 的期望深度：tiny 为 minimal，normal 为 standard，high-risk 为 detailed。

## Verification Gate（验证门禁）

在任务关闭前运行或检查机械证明的建议性 Harness 检查。Phase 4 中，`story verify <id>` 执行故事的 `verify_command`，`story verify-all` 跑全部已配置故事证明命令，`trace --story <id>` 在该故事验证未通过时发出警告。

## Tool Registry（工具注册表）

由 `scripts/bin/harness-cli query tools` 暴露的已编译并注册的工具清单。Agent 可借此发现可用命令、参数、职责与自定义项目工具。

## Intervention（干预）

人类、评审者、CI 或 Agent 纠正、覆盖、升级或批准工作的持久记录。干预与 trace 分开存储，并喂给改进提案。

## Context Score（上下文分）

`scripts/bin/harness-cli score-context <trace-id>` 的建议性结果。将 trace 记录的 `files_read` 与已编译上下文规则及检索触发器对比。

## Entropy Score（熵分）

`scripts/bin/harness-cli audit` 打印的漂移分数。越低越好。统计陈旧或不完整的持久记录，如孤儿故事、未验证的证明命令、缺失的 backlog 结果、损坏的已注册工具。

## Improvement Proposal（改进提案）

由 `scripts/bin/harness-cli propose` 根据重复摩擦、干预模式与审计发现生成的结构化建议。提案为只读，直到人类用结果日程显式接受一个稳定 key，或带原因拒绝一个稳定 key。

## Context Phase（上下文阶段）

Agent 任务中改变「应读什么上下文」的阶段，如分拣、计划、实现、验证或记录 trace。

## Retrieval Trigger（检索触发器）

告诉 Agent 拉取额外上下文的条件，如触及数据库 schema、变更公开契约，或发现缺失验证。

## Harness Delta（Harness 增量）

使未来 Agent 工作更安全或更容易的文档、模板、验证、backlog 或决策更新。

## Backlog Outcome Loop（Backlog 结果闭环）

Harness 改进的反馈工作流：创建 backlog 项时记录预期影响，关闭时记录实际度量结果，便于后续 Agent 对比预期与结果。

## Durable Layer（持久层）

存储运营记录（intake、故事、决策、backlog、trace）的 SQLite 数据库与 CLI（`scripts/bin/harness-cli`），数据结构化、可查询。策略文档描述怎么做；持久层记录发生了什么。

## Product Delta（产品增量）

面向产品的变更，如代码、测试、API 形态、数据模型或产品文档。

## Trace（执行轨迹）

Agent 在任务中做了什么的结构化记录：采取的动作、已读文件、已改文件、决策、错误、结果，以及发现的任何 Harness 摩擦。
