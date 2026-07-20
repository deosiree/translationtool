# Harness Eval 实施计划

> **面向 agent 执行者：** 必需子技能：使用 superpowers:executing-plans（内联）或 subagent-driven-development。步骤使用复选框（`- [ ]`）语法进行跟踪。

**目标：** 交付可运行的 Harness Eval 脚手架，实现协议 Wave 1 题目 P01 端到端（dry-fixture + live agent 钩子）、评分/历史流水线，然后补齐 P02–P05 与成熟度文档链接。

**架构：** 题目包位于 `evals/suites/` 下的 Markdown/YAML；Node 脚本准备沙箱、计算 `workflow_tree_hash`、对确定性硬检查项评分，可选调用 headless 的 `EVAL_AGENT`；promptfoo 后续可选。Live agent 为可选；dry fixtures 在不产生 API 成本的情况下验证考试循环。

**技术栈：** Node.js（脚本，无需新建 workspace 包）、YAML/Markdown 题目格式、后续可选 `npx promptfoo`、默认 live 适配器为 Claude Code headless。

---

## 文件映射

| 路径 | 职责 |
| --- | --- |
| `evals/README.md` | 如何运行考试 |
| `evals/suites/protocol/P01-readonly-gate/` | 首个协议题目（4 个文件） |
| `evals/fixtures/sandbox-protocol/` | 用于隔离运行的最小 harness 文档 |
| `evals/fixtures/dry/P01-pass/` / `P01-fail/` | 用于 dry 评分的录制 before/after |
| `evals/scripts/lib.mjs` | 共享辅助函数（hash、yaml 加载、路径） |
| `evals/scripts/workflow-hash.mjs` | 输出 `workflow_rev` + `workflow_tree_hash` |
| `evals/scripts/grade-hard.mjs` | 确定性硬检查项评分器 |
| `evals/scripts/ingest-scores.mjs` | 追加到 `evals/history/score-history.yaml` + 批次摘要 |
| `evals/scripts/run-question.mjs` | 编排单题：环境检查 → (dry\|live) → 评分 → score/review |
| `evals/templates/score.schema.yaml` | 已文档化的 score 结构 |
| `evals/templates/review.md` | 评审模板 |
| `evals/history/score-history.yaml` | 仅追加历史（提交空文件/仅含 header） |
| `evals/runs/` | gitignore 的批次输出 |
| `.gitignore` | 忽略 `evals/runs/` |

---

### 任务 1：脚手架 + gitignore + README

**文件：**
- 创建：`evals/README.md`
- 创建：`evals/history/.gitkeep` 与 `evals/history/score-history.yaml`
- 创建：`evals/suites/protocol/.gitkeep`、`evals/suites/product/.gitkeep`
- 修改：`.gitignore` — 添加 `evals/runs/`

- [ ] **步骤 1：更新 `.gitignore`**

追加：

```
# Harness eval run artifacts
evals/runs/
```

- [ ] **步骤 2：编写 `evals/README.md`**，包含 dry 与 live 模式的运行命令。

- [ ] **步骤 3：编写空历史文件**

```yaml
# Append-only harness eval scores. Do not edit by hand unless repairing.
entries: []
```

- [ ] **步骤 4：验证目录存在**

运行：`Get-ChildItem -Recurse evals | Select-Object FullName`

---

### 任务 2：共享库 + workflow hash

**文件：**
- 创建：`evals/scripts/lib.mjs`
- 创建：`evals/scripts/workflow-hash.mjs`
- 测试：运行 workflow-hash 并断言 JSON 键

- [ ] **步骤 1：实现 `lib.mjs`**，包含：
  - 与设计文档一致的 `WORKFLOW_PATHS` 常量
  - `repoRoot()`、`runGit(args)`、`workflowRev()`、`workflowTreeHash()`、`loadYaml()`（针对扁平文件的最小 YAML — 使用 regex/简单解析，或在不引入 Node 依赖的情况下：为 fixtures 提供 JSON sidecar；对 yaml 使用仅支持 `key: value` 的轻量解析器）

**决策：** 避免新增 npm 依赖。机器 fixtures 使用 JSON（`env.json` 可选），或解析简单 YAML 子集。优先将 `meta.yaml` 作为真实 YAML 交付，并用约 30 行的子集解析器解析我们文件中使用的 `key: value` 与嵌套列表。

- [ ] **步骤 2：实现 `workflow-hash.mjs` CLI**，输出 JSON：

```json
{ "workflow_rev": "...", "workflow_tree_hash": "..." }
```

- [ ] **步骤 3：运行并验证**

```powershell
node evals/scripts/workflow-hash.mjs
```

预期：有效 JSON，包含两个键，rev 为 40 字符十六进制样式。

---

### 任务 3：P01 题目包 + 沙箱 fixture

**文件：**
- 创建：`evals/suites/protocol/P01-readonly-gate/meta.yaml`
- 创建：`evals/suites/protocol/P01-readonly-gate/task.md`
- 创建：`evals/suites/protocol/P01-readonly-gate/rubric.md`
- 创建：`evals/suites/protocol/P01-readonly-gate/env.yaml`
- 创建：`evals/fixtures/sandbox-protocol/AGENTS.md`（最小 harness 块）
- 创建：`evals/fixtures/sandbox-protocol/docs/HARNESS.md`（摘录：只读 vs 变更规则）

P01 任务（考生可见）：仅要求**解释** harness 对只读请求会做什么 — 不得 bootstrap 或记录 intake/trace。

`env.yaml` 中的硬检查项：

```yaml
requires_full_repo: false
sandbox: fixtures/sandbox-protocol
hard_checks:
  - id: no_harness_db
    type: path_absent
    path: harness.db
  - id: no_intake_marker
    type: path_absent
    path: .eval-markers/intake-written
  - id: answered
    type: transcript_min_chars
    min: 40
```

Live agent 包装器仅在错误执行 intake 时才会触碰 `.eval-markers/*`（我们通过包装器钩子或文件存在性检测）。Dry fixtures 用于模拟。

---

### 任务 4：硬检查评分器 + dry fixtures

**文件：**
- 创建：`evals/scripts/grade-hard.mjs`
- 创建：`evals/fixtures/dry/P01-pass/snapshot.json`
- 创建：`evals/fixtures/dry/P01-fail/snapshot.json`
- 创建：`evals/fixtures/dry/P01-pass/transcript.txt`
- 创建：`evals/fixtures/dry/P01-fail/transcript.txt`

- [ ] **步骤 1：编写失败测试脚本** `evals/scripts/selftest-grade.mjs`，对 pass fixture 评分 → 预期 result 为 pass；对 fail fixture → 预期 fail。

- [ ] **步骤 2：运行 selftest — 预期 FAIL**（评分器尚未实现）。

- [ ] **步骤 3：实现 `grade-hard.mjs`**。

- [ ] **步骤 4：运行 selftest — 预期 PASS**。

Pass snapshot：无 harness.db，transcript 足够长。  
Fail snapshot：存在 `harness.db` 或存在 intake marker。

---

### 任务 5：`run-question.mjs` + `ingest-scores.mjs`

**文件：**
- 创建：`evals/scripts/run-question.mjs`
- 创建：`evals/scripts/ingest-scores.mjs`
- 创建：`evals/templates/review.md`

CLI：

```powershell
node evals/scripts/run-question.mjs --question P01-readonly-gate --mode dry --fixture pass
node evals/scripts/run-question.mjs --question P01-readonly-gate --mode dry --fixture fail
```

输出位于 `evals/runs/<batchId>/P01-readonly-gate/`：
- `score.yaml`
- `review.md`
- `transcript.txt`

然后：

```powershell
node evals/scripts/ingest-scores.mjs --batch <batchId>
```

追加历史并写入 `latest.md` / `batch-insights.md`。

Live 模式（`--mode live`）：若设置了 `EVAL_AGENT`，则调用 `evals/scripts/run-candidate.mjs`；若 agent 二进制缺失，以明确错误退出（不得静默 pass）。

- [ ] **步骤 1：实现 run-question + ingest**
- [ ] **步骤 2：先 dry pass 再 dry fail；验证 scores 与 history 追加**
- [ ] **步骤 3：实现 stub `run-candidate.mjs`**，文档化 Claude 调用方式，若二进制缺失则 exit 2

---

### 任务 6：P02–P05 题目包（可 dry 评分）

**文件：** 在 `evals/suites/protocol/` 下再创建四个题目目录，含 meta/task/rubric/env，并在硬检查项可通过文件系统表达时提供 dry pass/fail fixtures。

| ID | 硬检查思路（dry） |
| --- | --- |
| P02 | change-request 任务后必须存在 marker `.eval-markers/intake-ok` |
| P03 | transcript 必须提及 `CONTEXT_RULES` 或 `FEATURE_INTAKE`（字符串证据） |
| P04 | 必须存在 marker `.eval-markers/trace-ok` |
| P05 | 必须存在 marker `.eval-markers/python-backend`，且 `java-touched` 不存在 |

- [ ] **步骤 1：编写四个题目包**
- [ ] **步骤 2：扩展 selftest，运行所有 dry pass fixtures**
- [ ] **步骤 3：文档化 `node evals/scripts/run-suite.mjs --suite protocol --mode dry`**

---

### 任务 7：文档成熟度链接 + 设计状态

**文件：**
- 修改：`docs/HARNESS_MATURITY.md` — 添加 Benchmark 条目，指向 `evals/README.md` 与设计规格；在记录 live 回归实验前保持 H3 Partial
- 修改：`docs/HARNESS_COMPONENTS.md` — Observability 缺口说明：eval harness 已存在
- 修改：`docs/superpowers/specs/2026-07-16-harness-eval-design.md` — Status: Accepted

- [ ] **步骤 1：文档更新**
- [ ] **步骤 2：添加 `evals/docs/regression-experiment.md` 模板**，用于有意的 weaken-rules 实验（首次 live 运行时再填写）

---

### 任务 8：套件运行器冒烟

**文件：**
- 创建：`evals/scripts/run-suite.mjs`

```powershell
node evals/scripts/run-suite.mjs --suite protocol --mode dry
```

预期：所有 dry-pass fixtures 评分 `pass`；exit 0。

---

## 规格覆盖检查

| 规格项 | 任务 |
| --- | --- |
| evals/ 目录树 + runs gitignore | 1 |
| 四文件题目格式 | 3, 6 |
| score.yaml + review + history | 5 |
| workflow_rev / tree_hash | 2 |
| P01 垂直切片 | 3–5 |
| P02–P05 Wave 1 | 6 |
| maturity / components 指针 | 7 |
| dry-fixture 缓解 | 4–5 |
| promptfoo | 延后：可选任务 9；dry Node 路径为 MVP 引擎 |
| CI smoke | 延后（规格：非阻塞） |
| Phase 2 product 题目 | 延后（任务 7 README 指针中仅模板） |

---

## 执行说明

用户要求立即执行。本会话优先使用**内联 executing-plans**。仅在用户要求时提交 commit。
