# Harness 成熟度阶梯

本阶梯定义 `repository-harness` 应如何从静态 Agent 指令演进为可度量的 Harness 改进。

各级别有意设计为可验证。仅当标准可在仓库文件、持久 Harness 记录或 benchmark 输出中检查时，才视为达成该级。

## Benchmark（Harness Eval）

Protocol exam 位于 `evals/`。设计：`docs/superpowers/specs/2026-07-16-harness-eval-design.md`。运行 dry suite：

```powershell
node evals/scripts/selftest-grade.mjs
node evals/scripts/run-suite.mjs --suite protocol --mode dry
```

见 `evals/README.md`。

## 级别

### H0 - 裸环境

模型在无仓库 Harness 下运行。收到 prompt 可能产出 patch，但仓库不告知如何分类、验证或记录工作。

标准：

- 不存在 `AGENTS.md` Harness 块。
- 无 feature intake policy。
- 无 story、decision、validation 或 trace artifact。

必需文件：

- 无。

Benchmark 指标：

- Functional score 为唯一有意义 metric。
- Harness compliance：0%。
- Trace quality：0/3。

当前 status：

- 已通过。本仓库已超越 H0。

激活的 responsibilities：

- 无。

### H1 - 脚手架与策略

仓库含静态操作指令、template、risk lane 与 source-of-truth rule。Agent 可遵循 documented workflow，但 durable state 仍可能手工或不完整。

标准：

- `AGENTS.md` 指向 Harness 操作文档。
- 存在 `docs/HARNESS.md`、`docs/FEATURE_INTAKE.md`、`docs/ARCHITECTURE.md`。
- `docs/templates/` 下存在 story、decision 与 validation template。
- `docs/TEST_MATRIX.md` 定义 proof column 与 status 含义。

必需文件：

- `AGENTS.md`
- `docs/HARNESS.md`
- `docs/FEATURE_INTAKE.md`
- `docs/ARCHITECTURE.md`
- `docs/TEST_MATRIX.md`
- `docs/templates/story.md`
- `docs/templates/decision.md`
- `docs/templates/validation-report.md`

Benchmark 指标：

- Harness compliance：20–40%。
- Agent 阅读 intake policy 时 lane accuracy 提升。
- 除非单独要求 trace，trace quality 仍低。

当前 status：

- 已达成。H1 文件存在且被当前 Harness 指令使用。

激活的 responsibilities：

- Task specification。
- Permissions。
- Project memory。
- Verification。

### H2 - 持久状态与可观测性

仓库有结构化操作记录与显式 observation rule。Agent 可记录发生内容、将工作与 story 关联，并以可预测深度写 trace。

标准：

- `scripts/bin/harness-cli` 可在 `harness.db` 记录 intake、story、decision、backlog 与 trace 数据。
- `scripts/schema/001-init.sql` 定义 intake、story、decision、backlog 与 trace 的 durable table。
- `docs/HARNESS_COMPONENTS.md` 映射文件与 responsibility。
- `docs/HARNESS_MATURITY.md` 定义 H0–H5 与可度量标准。
- `docs/TRACE_SPEC.md` 定义 trace 字段、quality tier 与 friction 捕获。
- `docs/CONTEXT_RULES.md` 定义 phase-by-lane context rule。
- `AGENTS.md` 与 `docs/HARNESS.md` 引用 Phase 2 操作文档。

必需文件：

- `scripts/bin/harness-cli`
- `scripts/schema/001-init.sql`
- `docs/HARNESS_COMPONENTS.md`
- `docs/HARNESS_MATURITY.md`
- `docs/TRACE_SPEC.md`
- `docs/CONTEXT_RULES.md`

Benchmark 指标：

- Harness compliance：75–90%。
- Trace quality：normal-lane 任务至少 2.0/3。
- Lane accuracy：当前 benchmark suite 6/6。
- Friction captured：存在 friction 时至少 4/6 benchmark 任务。

当前 status：

- 已达成。Durable state 存在，Phase 2 文档定义可观测性与 context 规范。Phase 3 主动 scoring 建立于此层。

激活的 responsibilities：

- Task state。
- Observability。
- Failure attribution。
- Context selection。
- Entropy auditing。

### H3 - 主动可观测性与演进

Harness 可评估自身操作数据，并将重复 failure 转为 prioritized improvement。

标准：

- Trace quality 可由可重复 command 或 benchmark step 评分。
- Harness friction 可按 `docs/HARNESS_COMPONENTS.md` 的 component 分组。
- Backlog 项在完成后含 predicted impact 与 actual outcome。
- Benchmark 比较输出标识哪项 harness responsibility 移动或 regression。

必需文件：

- H2 文件。
- 引用 maturity level 的 benchmark protocol 或 report。
- 已文档化的 trace quality scoring 方法。
- 已文档化的 friction-to-backlog 审查循环。

Benchmark 指标：

- Harness compliance：85–95%。
- Trace quality：2.3–2.7/3。
- 多数 failed 或 awkward 任务的 friction 被捕获并按 component 分类。
- Regression 含 attributed harness component。

当前 status：

- Phase 3 部分达成。`scripts/bin/harness-cli score-trace` 按 tier rule 对 trace quality 评分；`query friction` 含 linked intake 上下文；`trace` 命令写入时打印该 score；backlog outcome loop 文档化 predicted impact 与 actual outcome。`evals/` 下 Harness Eval 提供 protocol + product dry exam、`workflow_tree_hash` baseline（`evals/history/workflow-baseline.yaml`）、`compare-baseline.mjs` gate 与 GitHub Actions smoke（`.github/workflows/harness-eval-smoke.yml`）。Live/agent benchmark attribution 的 regression 文档于 `evals/docs/regression-experiment.md` 与 `evals/docs/regression-results.md`；完整 H3 closure 仍须在 baseline 更新前定期 live regression。

激活的 responsibilities：

- Observability。
- Failure attribution。
- Entropy auditing。
- Intervention recording。

### H4 - 自动验证

Harness 可一致运行或编排 proof check，并在 final response 前 reject 或 flag 不完整工作。

标准：

- 已文档化的 verification command 或 protocol 对所选 story 与 lane 运行预期检查。
- Story 可存储并执行 `verify_command`。
- Trace 记录在 linked story 的 verification command 未 pass 时警告。
- 任务标为 `implemented` 前 surface 缺失 validation evidence。

必需文件：

- H3 文件。
- Verification protocol 或 command 引用。
- 与 story proof column 绑定的 validation report 示例。
- Story verification command 文档。

Benchmark 指标：

- Functional score 保持稳定。
- Harness compliance：至少 90%。
- Benchmark review 中 false「done」声明更少。
- Merge 或 final response 前检测到缺失 proof。

当前 status：

- Phase 5 已达成。`scripts/bin/harness-cli story verify <id>` 运行 story-level proof command 并记录 pass/fail；`trace --story` 在 verification 未 pass 时于 close 前警告；`scripts/bin/harness-cli story verify-all` 一次运行全部已配置 story proof command。Proof-column automation 仍为 future enhancement，但 H4 要求的 automated verification gate 已存在。

激活的 responsibilities：

- Verification。
- Task state。
- Permissions。
- Intervention recording。

### H5 - 自改进 Harness

Harness 可用 trace、benchmark result 与 backlog outcome 提出或应用对自身安全的改进。

标准：

- 重复 friction 模式摘要为 proposed harness change。
- Proposed change 含 predicted impact、risk、validation plan 与 rollback criteria。
- 已完成 change 比较 predicted impact 与 actual benchmark 或 trace outcome。
- High-risk harness change 在改变 source hierarchy、architecture direction 或 validation requirement 前暂停等待 human confirmation。

必需文件：

- H4 文件。
- Self-improvement protocol。
- 历史 improvement report。
- Backlog outcome review。

Benchmark 指标：

- 重复 benchmark run 间 harness compliance 至少 90%。
- Trace quality 至少 2.5/3。
- Improvement 显示可度量正向 delta 或显式 revert。
- Policy 捕获 scope creep 与 validation weakening。

当前 status：

- Phase 5 部分达成。`scripts/bin/harness-cli audit` 检测 durable-state drift；`scripts/bin/harness-cli propose` 从 friction、intervention 与 audit result 生成 structured improvement proposal；`docs/IMPROVEMENT_PROTOCOL.md` 定义 review loop。H5 未完全达成，直至重复 benchmark outcome 证明 proposed improvement 产生可度量正向 delta 或显式 revert。

激活的 responsibilities：

- Entropy auditing。
- Failure attribution。
- Intervention recording。
- Permissions。

## 当前评估

| Level | Status | Evidence |
| --- | --- | --- |
| H0 | Passed | Harness 文档、template 与 durable record 存在。 |
| H1 | Achieved | 存在 `AGENTS.md`、`docs/HARNESS.md`、`docs/FEATURE_INTAKE.md`、`docs/ARCHITECTURE.md`、`docs/templates/*`、`docs/TEST_MATRIX.md`。 |
| H2 | Achieved | `scripts/bin/harness-cli`、`scripts/schema/001-init.sql`、持久 story record、`docs/HARNESS_COMPONENTS.md`、`docs/HARNESS_MATURITY.md`、`docs/TRACE_SPEC.md`、`docs/CONTEXT_RULES.md` 定义 Phase 2 面。 |
| H3 | Partial | Phase 3 增加 `scripts/bin/harness-cli score-trace`、enriched friction context 与 backlog outcome loop；Phase 4 写入时自动 score trace。`evals/` 增加 protocol Wave 1 dry exam + score history；live component-level benchmark attribution 仍开放。 |
| H4 | Achieved | Phase 4 增加 story-level `verify_command`、`story verify` 与 trace-time verification warning。Phase 5 增加 `story verify-all` 批量 story proof。 |
| H5 | Partial | Phase 5 增加 `audit`、`score-context`、`intervention add/query`、`propose`、`docs/HARNESS_AUDIT.md`、`docs/IMPROVEMENT_PROTOCOL.md`；重复 benchmark outcome proof 仍开放。 |

## Responsibility 激活

| Responsibility | H0 | H1 | H2 | H3 | H4 | H5 |
| --- | --- | --- | --- | --- | --- | --- |
| Task specification | Missing | Covered | Covered | Covered | Covered | Covered |
| Context selection | Missing | Partial | Covered | Covered | Covered | Covered |
| Tool access | Missing | Partial | Partial | Partial | Covered | Covered |
| Project memory | Missing | Covered | Covered | Covered | Covered | Covered |
| Task state | Missing | Partial | Covered | Covered | Covered | Covered |
| Observability | Missing | Missing | Partial | Covered | Covered | Covered |
| Failure attribution | Missing | Missing | Partial | Covered | Covered | Covered |
| Verification | Missing | Partial | Partial | Partial | Covered | Covered |
| Permissions | Missing | Partial | Partial | Partial | Covered | Covered |
| Entropy auditing | Missing | Missing | Partial | Covered | Covered | Covered |
| Intervention recording | Missing | Partial | Partial | Covered | Covered | Covered |

## Phase 3 解读

Phase 3 启动 H2 到 H3 过渡。声称 active trace scoring 与 documented improvement feedback loop，但不声称完整 H3，因 benchmark comparison 与 component-level regression attribution 明确在本仓库 Phase 3 scope 之外。

## Phase 4 解读

Phase 4 启动 H3 到 H4 过渡。为 story 提供与 decision 相同的 mechanical verification pattern，在 durable layer 记录 story verification result，写入 trace 时自动 score，并在 linked story verification 未 pass 时于 close 前警告。不声称完整 H4，因 benchmark execution、batch verification 与 automatic proof-column update 仍为独立工作。

## Phase 5 解读

Phase 5 以 batch story verification 完成 H4，并以 tool discovery、intervention record、context scoring、drift audit 与 deterministic proposal generation 启动 H5。仅当这些 command 与文档存在且已验证时仓库可声称 H5 partial；在 benchmark run 或 trace outcome 证明 proposal loop 随时间改进 harness 之前，不得声称完整 H5。
