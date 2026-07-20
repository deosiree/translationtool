# Harness 组件

本分类将当前 `repository-harness` 仓库映射到 Phase 2 使用、Phase 3 主动可观测性工作更新的两套组件框架：

- Runtime Substrate responsibilities：Harness 应覆盖的 11 项 responsibility 领域。
- NexAU 分解：影响 Agent 行为的七个实现面。

Status 取值：

- **Covered**：仓库对该 responsibility 有显式文件、命令或记录。
- **Partial**：有部分支持，但不完整、仍依赖手工或未度量。
- **Missing**：尚无有意义支持。

## Responsibility 映射

| # | Responsibility | Status | Harness Files | Evidence | Gap |
| --- | --- | --- | --- | --- | --- |
| 1 | Task specification | Covered | `AGENTS.md`, `docs/FEATURE_INTAKE.md`, `docs/templates/story.md`, `docs/templates/spec-intake.md`, `docs/templates/high-risk-story/*`, `docs/stories/*`, `intake` table, `story` table | 请求在实现前按 type 与 lane 分类；normal 与 high-risk 工作有 template 与持久 story 行。 | 保持 story packet 与未来产品文档同步。 |
| 2 | Context selection | Covered | `AGENTS.md`, `docs/CONTEXT_RULES.md`, `docs/ARCHITECTURE.md`, `docs/decisions/*`, `docs/product/README.md`, `scripts/bin/harness-cli score-context` | Phase 2 增加按 phase-by-lane context rule 与 retrieval trigger；Phase 5 增加对已记录 trace read 的 context scoring。 | 未来自动化可 enforcement context selection，而非仅度量。 |
| 3 | Tool access | Covered | `scripts/bin/harness-cli`, `docs/TOOL_REGISTRY.md`, `tool` table, `crates/harness-cli/*`, `scripts/install-harness.sh`, `scripts/build-harness-cli-release.sh` | Harness CLI 暴露操作命令，经 `query tools` 提供机器可读 tool manifest；外部工具可注册与移除。 | Permission profile 与 usage analytics 仍为未来工作。 |
| 4 | Project memory | Covered | `docs/HARNESS.md`, `docs/decisions/*`, `docs/GLOSSARY.md`, `docs/HARNESS_BACKLOG.md`, `docs/stories/*`, `harness.db`, `decision`, `backlog`, and `trace` tables | Decision、backlog、story 与 trace 跨任务保留持久知识。 | 未来应增加 staleness check 并摘要旧 trace。 |
| 5 | Task state | Covered | `scripts/bin/harness-cli query matrix`, `docs/TEST_MATRIX.md`, `intake` table, `story` table, `trace` table | 持久记录跟踪 intake、story status、proof column 与 task trace。 | 增加 lifecycle check，避免 in-progress story 被遗忘。 |
| 6 | Observability | Partial | `docs/TRACE_SPEC.md`, `trace` table, `scripts/bin/harness-cli trace`, `scripts/bin/harness-cli score-trace`, `scripts/bin/harness-cli query traces`, `scripts/bin/harness-cli query friction`, `docs/HARNESS_MATURITY.md`, `evals/` (Harness Eval dry suite) | Trace 写入时自动评分，可命令重评，可结合 friction 上下文审查。Protocol exam dry-run pipeline 在 `evals/` 下。 | Live agent exam batch 与 dashboard ingestion 仍开放。 |
| 7 | Failure attribution | Partial | `docs/HARNESS_COMPONENTS.md`, `docs/TRACE_SPEC.md`, `trace.errors`, `trace.harness_friction`, `docs/HARNESS_BACKLOG.md`, `backlog` table, `scripts/bin/harness-cli query friction` | 失败可关联文件、component、friction、backlog 提案与 linked intake lane/type 上下文。 | 尚无从 benchmark failure 到 harness component 的自动 attribution。 |
| 8 | Verification | Covered | `docs/TEST_MATRIX.md`, `scripts/bin/harness-cli query matrix`, `scripts/bin/harness-cli story verify`, `scripts/bin/harness-cli story verify-all`, `scripts/bin/harness-cli trace`, `scripts/bin/harness-cli score-trace`, `story.verify_command`, `story.last_verified_result`, `.github/workflows/harness-cli-release.yml`, `docs/templates/validation-report.md` | Story 可存储并单独或批量运行 mechanical proof command；trace 在 linked story verification 未 pass 时警告；trace quality 可机械检查；release workflow 验证 Rust CLI release。 | Benchmark ingestion 仍为未来工作。 |
| 9 | Permissions | Partial | `AGENTS.md`, `docs/HARNESS.md`, `docs/FEATURE_INTAKE.md`, `docs/ARCHITECTURE.md`, installer conflict handling in `scripts/install-harness.sh` | 策略描述 Agent 何时可更新文档、何时在 architecture 或 workflow 变更前询问。 | Permission 仅 instruction 级；无 enforced policy layer 或 command allowlist。 |
| 10 | Entropy auditing | Covered | `docs/HARNESS_BACKLOG.md`, `docs/HARNESS_AUDIT.md`, `docs/IMPROVEMENT_PROTOCOL.md`, `backlog` table, `trace.harness_friction`, `scripts/bin/harness-cli audit`, `scripts/bin/harness-cli propose`, `docs/HARNESS_MATURITY.md` | Growth rule 捕获 friction；audit 检测 drift 与 entropy score；backlog 项比较 predicted impact 与 actual outcome；proposal generation 可创建可审查 backlog 项。 | 自动 repair 仍为未来工作。 |
| 11 | Intervention recording | Covered | `intervention` table, `scripts/bin/harness-cli intervention add`, `scripts/bin/harness-cli query interventions`, `trace` table, `docs/decisions/*`, `docs/stories/*`, `docs/HARNESS.md` | Human、reviewer、CI 与 agent intervention 为独立持久记录，可按 trace、story 或 type 过滤。 | 捕获仍为手工且 advisory。 |

## NexAU 交叉引用

| Component | Harness Equivalent | Status | Notes |
| --- | --- | --- | --- |
| System prompts | `AGENTS.md` plus Harness policy docs | Covered | `AGENTS.md` 为稳定 shim；`docs/HARNESS.md`、`docs/FEATURE_INTAKE.md`、`docs/CONTEXT_RULES.md` 承载演进中的操作指令。 |
| Tool descriptions | `docs/TOOL_REGISTRY.md`, `scripts/README.md`, `docs/HARNESS.md`, `docs/TRACE_SPEC.md`, CLI help from `crates/harness-cli/src/interface.rs`, `scripts/bin/harness-cli query tools` | Covered | 命令在独立 registry 中文档化，并作为 compiled 与 registered tool manifest 条目暴露。 |
| Tool implementations | `scripts/bin/harness-cli`, `crates/harness-cli/*`, `scripts/schema/001-init.sql`, `scripts/schema/002-story-verify.sql` | Covered | Rust CLI 为 primary durable-layer 实现与稳定 repo-local 入口。 |
| Middleware | installer safety logic, feature intake workflow | Partial | Installer 与 intake 流程 mediate 工作，但无 runtime middleware enforcement policy。 |
| Skills | `docs/templates/*`, `docs/FEATURE_INTAKE.md`, `docs/CONTEXT_RULES.md`, `docs/TRACE_SPEC.md` | Partial | 可复用 procedure 以 markdown 存在，非可执行或可安装 agent skill。 |
| Sub-agents | None in this repository | Missing | 无 delegated specialist agent 或 sub-agent protocol。 |
| Long-term memory | `harness.db`, `docs/decisions/*`, `docs/stories/*`, `docs/HARNESS_BACKLOG.md`, `docs/GLOSSARY.md` | Covered | 持久记录与 markdown decision 保留 task history 与项目词汇。 |

## 文件清单

每个已跟踪项目文件及 Phase 2 输入文件至少映射到一个 Runtime Substrate responsibility。

| File | Primary Responsibility | Secondary Responsibilities |
| --- | --- | --- |
| `.gitignore` | Tool access | Task state |
| `AGENTS.md` | Context selection | Task specification, permissions |
| `README.md` | Task specification | Project memory |
| `CONTRIBUTING.md` | Intervention recording | Project memory |
| `Cargo.toml` | Tool access | Verification |
| `Cargo.lock` | Tool access | Verification |
| `PHASE2.md` | Task specification | Observability, context selection |
| `PHASE3.md` | Task specification | Observability, verification, entropy auditing |
| `PHASE4.md` | Task specification | Verification, observability, task state |
| `PHASE5.md` | Task specification | Verification, entropy auditing, intervention recording |
| `crates/harness-cli/Cargo.toml` | Tool access | Verification |
| `crates/harness-cli/src/main.rs` | Tool access | Tool implementation |
| `crates/harness-cli/src/domain.rs` | Tool access | Task state, verification |
| `crates/harness-cli/src/application.rs` | Tool access | Task state |
| `crates/harness-cli/src/infrastructure.rs` | Tool access | Project memory, task state, observability |
| `crates/harness-cli/src/interface.rs` | Tool access | Context selection, verification |
| `docs/ARCHITECTURE.md` | Permissions | Context selection, task specification |
| `docs/FEATURE_INTAKE.md` | Task specification | Permissions, context selection |
| `docs/GLOSSARY.md` | Project memory | Context selection |
| `docs/HARNESS.md` | Task specification | Project memory, task state, permissions |
| `docs/HARNESS_BACKLOG.md` | Entropy auditing | Project memory, failure attribution |
| `docs/HARNESS_COMPONENTS.md` | Failure attribution | Observability, entropy auditing |
| `docs/HARNESS_MATURITY.md` | Entropy auditing | Observability, verification |
| `docs/HARNESS_AUDIT.md` | Entropy auditing | Verification, task state |
| `docs/IMPROVEMENT_PROTOCOL.md` | Entropy auditing | Failure attribution, permissions |
| `docs/CONTEXT_RULES.md` | Context selection | Permissions, task specification |
| `docs/TRACE_SPEC.md` | Observability | Failure attribution, intervention recording |
| `docs/TOOL_REGISTRY.md` | Tool access | Context selection, verification |
| `docs/README.md` | Project memory | Context selection |
| `docs/TEST_MATRIX.md` | Verification | Task state |
| `docs/decisions/0001-harness-first-development.md` | Project memory | Permissions |
| `docs/decisions/0002-post-spec-product-lifecycle.md` | Project memory | Task specification |
| `docs/decisions/0003-generic-spec-intake-harness.md` | Project memory | Task specification |
| `docs/decisions/0004-sqlite-durable-layer.md` | Project memory | Observability, task state |
| `docs/decisions/0005-prebuilt-rust-harness-cli.md` | Project memory | Tool access |
| `docs/decisions/0006-phase-4-benchmark-triage.md` | Project memory | Verification |
| `docs/decisions/0007-improvement-proposal-rules.md` | Project memory | Entropy auditing, permissions |
| `docs/decisions/README.md` | Project memory | Context selection |
| `docs/demo/README.md` | Task specification | Project memory |
| `docs/product/README.md` | Task specification | Project memory |
| `docs/review-fixes-1d30bf62-to-main.md` | Intervention recording | Failure attribution, verification |
| `docs/stories/README.md` | Task specification | Project memory |
| `docs/stories/US-001-install-harness.md` | Task specification | Verification, intervention recording |
| `docs/stories/US-008-trace-quality-scoring.md` | Task specification | Observability, verification |
| `docs/stories/US-009-enriched-friction-query.md` | Task specification | Failure attribution, observability |
| `docs/stories/US-011-backlog-outcome-workflow.md` | Task specification | Entropy auditing, project memory |
| `docs/stories/US-012-story-verify-command-field.md` | Task specification | Verification |
| `docs/stories/US-015-story-verify-command.md` | Task specification | Verification |
| `docs/stories/US-016-auto-trace-scoring-on-write.md` | Task specification | Observability, verification |
| `docs/stories/US-017-pre-close-verification-gate.md` | Task specification | Verification, permissions |
| `docs/stories/US-018-phase4-cli-ux-hardening.md` | Task specification | Tool access, verification |
| `docs/stories/US-019-machine-readable-tool-registry.md` | Task specification | Tool access |
| `docs/stories/US-020-batch-story-verification.md` | Task specification | Verification |
| `docs/stories/US-021-intervention-recording-schema.md` | Task specification | Intervention recording |
| `docs/stories/US-022-context-rule-measurement.md` | Task specification | Context selection |
| `docs/stories/US-023-drift-detection-entropy-score.md` | Task specification | Entropy auditing |
| `docs/stories/US-024-improvement-proposal-pipeline.md` | Task specification | Entropy auditing, permissions |
| `docs/stories/backlog.md` | Task specification | Project memory |
| `docs/stories/epics/README.md` | Task specification | Project memory |
| `docs/stories/epics/E01-durable-layer/US-002-rust-harness-cli/overview.md` | Task specification | Project memory |
| `docs/stories/epics/E01-durable-layer/US-002-rust-harness-cli/design.md` | Task specification | Tool access, permissions |
| `docs/stories/epics/E01-durable-layer/US-002-rust-harness-cli/execplan.md` | Task specification | Verification, task state |
| `docs/stories/epics/E01-durable-layer/US-002-rust-harness-cli/validation.md` | Verification | Intervention recording |
| `docs/stories/epics/E02-phase-2-observability-taxonomy/phase-2-progress.md` | Task state | Intervention recording |
| `docs/stories/epics/E03-phase-5-evolution-infrastructure/phase-5-progress.md` | Task state | Verification, entropy auditing |
| `docs/templates/decision.md` | Project memory | Task specification |
| `docs/templates/spec-intake.md` | Task specification | Context selection |
| `docs/templates/story.md` | Task specification | Verification |
| `docs/templates/validation-report.md` | Verification | Intervention recording |
| `docs/templates/high-risk-story/overview.md` | Task specification | Context selection |
| `docs/templates/high-risk-story/design.md` | Task specification | Permissions |
| `docs/templates/high-risk-story/execplan.md` | Task state | Verification |
| `docs/templates/high-risk-story/validation.md` | Verification | Failure attribution |
| `scripts/README.md` | Tool access | Context selection |
| `scripts/bin/harness-cli` | Tool access | Task state, observability |
| `scripts/bin/harness-cli` | Tool access | Task state, observability |
| `scripts/install-harness.sh` | Tool access | Permissions |
| `scripts/build-harness-cli-release.sh` | Verification | Tool access |
| `scripts/schema/001-init.sql` | Task state | Observability, project memory |
| `scripts/schema/002-story-verify.sql` | Verification | Task state, project memory |
| `scripts/schema/003-tool-registry.sql` | Tool access | Project memory |
| `scripts/schema/004-intervention.sql` | Intervention recording | Failure attribution |
| `.github/ISSUE_TEMPLATE/agent-failure-case.md` | Failure attribution | Entropy auditing |
| `.github/ISSUE_TEMPLATE/pattern-request.md` | Entropy auditing | Intervention recording |
| `.github/ISSUE_TEMPLATE/real-world-example.md` | Project memory | Intervention recording |
| `.github/workflows/harness-cli-release.yml` | Verification | Tool access |

## 覆盖摘要

- Covered：11 项 responsibility 中 8 项。
- Partial：3 项。
- Missing：0 项。

Covered responsibilities：

- Task specification。
- Context selection。
- Tool access。
- Project memory。
- Task state。
- Verification。
- Entropy auditing。
- Intervention recording。

Partial responsibilities：

- Observability。
- Failure attribution。
- Permissions。

Phase 5 以 registry、drift audit、proposal loop 与 intervention schema 将 tool access、entropy auditing 与 intervention recording 转为 Covered。后续 phase 应聚焦 benchmark ingestion、component-level attribution、permission enforcement 与 tool usage analytics。
