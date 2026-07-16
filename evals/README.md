# Harness 评测（Eval）

可回归的 Harness 工作流考试：改 rules / AGENTS / intake 策略后，用固定题库量化是否进步。

**语言约定：** 题面（`task.md`）、阅卷标准（`rubric.md`）、人工审查用的 prompt/注释/汇总报告一律使用**中文**；脚本标识符与 `env.yaml` 检查类型名保持英文以便解析。

设计：`docs/superpowers/specs/2026-07-16-harness-eval-design.md`  
计划：`docs/superpowers/plans/2026-07-16-harness-eval.md`

## 快速开始（dry，无需 Agent API）

```powershell
# 单题 — 预期通过夹具
node evals/scripts/run-question.mjs --question P01-readonly-gate --mode dry --fixture pass

# 单题 — 预期失败夹具
node evals/scripts/run-question.mjs --question P01-readonly-gate --mode dry --fixture fail

# 协议套件全部 dry-pass + 写入历史
node evals/scripts/run-suite.mjs --suite protocol --mode dry

# 稳定性：live 每题连跑 3 次（生成 stability-report.md）
$env:EVAL_AGENT = "claude"
node evals/scripts/run-suite.mjs --suite protocol --mode live --trials 3

# 业务题 B01
node evals/scripts/run-question.mjs --question B01-intake-routing --mode dry --fixture pass

# 自测判卷逻辑
node evals/scripts/selftest-grade.mjs

# 工作流指纹
node evals/scripts/workflow-hash.mjs

# CI smoke（自测 + dry 套件 + 基线门禁）
node evals/scripts/ci-smoke.mjs

# 基线对比（Darwin Phase 2）
node evals/scripts/compare-baseline.mjs --print
node evals/scripts/compare-baseline.mjs --gate --batch-prefix <批次前缀>
# live 回归通过后更新基线
node evals/scripts/compare-baseline.mjs --update-baseline --batch-prefix <批次前缀>
```

产物在 `evals/runs/<batchId>/`（已 gitignore）。每题含 `transcript.txt`（答卷）、`review.md`（中文阅卷）、`score.yaml`。

## Darwin Phase 2 — 基线与 CI

| 文件 | 作用 |
| --- | --- |
| `evals/history/workflow-baseline.yaml` | 记录 `workflow_tree_hash` 与各 dry 套件最低通过数 |
| `evals/scripts/compare-baseline.mjs` | 对比指纹 + dry 门禁；`--update-baseline` 在 live 回归后人工更新 |
| `evals/scripts/ci-smoke.mjs` | 本地/CI 一键 smoke |
| `.github/workflows/harness-eval-smoke.yml` | PR/push 触发 dry smoke（无需 Agent API） |

**约定：** `workflow_tree_hash` 变更时 CI **仅告警不阻断**；合并前须本地 `live --trials 3` 回归，再 `--update-baseline`。dry 未达基线则 CI 失败。

**你与 AI 如何配合：** `evals/docs/operator-playbook.md`

## Live 实跑（Claude Code）

```powershell
$env:EVAL_AGENT = "claude"
node evals/scripts/run-question.mjs --question P01-readonly-gate --mode live
node evals/scripts/ingest-scores.mjs --batch <上一步输出的 batch_id>
```

**实跑要点（已修复）：**

- prompt 经 **stdin** 传给 `claude -p`，避免 Windows shell 截断题面。
- 沙箱写 marker 需 `--dangerously-skip-permissions`。
- 硬性检查含 **not_greeting_only**，防止「只寒暄不答题」假通过。

## 题库结构

- `evals/suites/protocol/` — Phase 1 协议题 P01–P05
- `evals/suites/product/` — Phase 2 业务题（占位）

每题四件套：`meta.yaml` / `task.md` / `rubric.md` / `env.yaml`。考生只见 `task.md`，不见 `rubric.md`。

## 人工审查看哪里

| 文件 | 内容 |
| --- | --- |
| `task.md` | 中文题面（你可直接读并判断是否合理） |
| `rubric.md` | 中文阅卷标准 |
| `runs/.../review.md` | 中文证据 + 改进建议 |
| `runs/.../prompt.txt` | live 模式实际发给 Agent 的中文指令 |
| `batch-insights.md` | 批次改进建议汇总 |
