# Harness Eval 操作手册（你与 AI 如何配合）

## 角色分工

| 谁 | 做什么 |
| --- | --- |
| **GitHub Actions** | 每次 PR/push（命中路径时）自动跑 dry smoke，无需 API |
| **你（本地）** | 改 Harness 规则后的 live 回归、审阅 `review.md`、决定是否更新基线、push 分支 |
| **AI** | 改题面/判卷/脚本/文档；按你的批次结果更新 `workflow-baseline.yaml`；排查 CI 失败 |

---

## 场景 A：日常改业务代码（不动 Harness）

- **你：** 正常开发，无需跑 eval。
- **CI：** 不触发（workflow 有 path filter）。

---

## 场景 B：改了 `AGENTS.md` / `docs/HARNESS*.md` / `evals/**` 等

### 你本地（合并前建议）

```powershell
node evals/scripts/ci-smoke.mjs
```

通过后再 push / 开 PR。

### 你 push 后

1. 打开 GitHub → Actions → **Harness Eval 冒烟测试**
2. 绿 = dry 门禁 OK；红 = 点开日志，看是判卷自测、某题 dry 还是基线未达标

### 我需要你提供（若 CI 红了）

- 失败 job 的日志片段，或 PR 链接
- 你改了哪些 Harness 相关文件

---

## 场景 C：改了 Harness 规则（`workflow_tree_hash` 会变）

CI **仍可能绿**（指纹变更只告警，不单独阻断）。你必须做 live 回归。

### 你本地执行

```powershell
# 1. 看指纹是否变了
node evals/scripts/workflow-hash.mjs

# 2. live 稳定性（需已登录 Claude Code CLI）
$env:EVAL_AGENT = "claude"
node evals/scripts/run-suite.mjs --suite protocol --mode live --trials 3

# 可选：业务题
node evals/scripts/run-suite.mjs --suite product --mode live

# 3. 可选：弱化沙箱探索（不作唯一门禁，见 regression-results.md）
node evals/scripts/run-regression-weaken.mjs
```

### 你审阅

打开 `evals/runs/<batchId>/`：

- 每题 `review.md` — 中文证据与建议
- `stability-report.md` — 通过率

### 你告诉我（或自己执行）

- **全部通过：**「live 回归通过，批次 `<batchId>`，请更新基线」
- **有失败：** 贴 `review.md` 或题号，让我改规则/题面/判卷

更新基线（live 通过后）：

```powershell
# 若 live 用同一前缀跑了 protocol + product
node evals/scripts/compare-baseline.mjs --update-baseline --batch-prefix <batchId>

# 或只跑了单套件
node evals/scripts/compare-baseline.mjs --update-baseline --batch <batchId>__protocol
```

然后把 `evals/history/workflow-baseline.yaml` 一并提交。

---

## 场景 D：新增/修改考题

### 你告诉我

1. 考什么行为（一句话）
2. 属于 protocol 还是 product
3. 期望 pass 时 workspace 长什么样、fail 时长什么样

### 我来做

- 四件套 `meta/task/rubric/env`
- `fixtures/dry/<题号>/pass|fail`
- 跑 dry selftest + 必要时 live 试跑

### 你配合

- 读 `task.md` / `rubric.md` 是否合理
- 本地 `node evals/scripts/run-question.mjs --question <题号> --mode dry --fixture pass`

---

## 场景 E：第一次把 eval 推到远端

当前分支 `docker2` 领先 `origin/docker2` **11 个提交**（含 eval 全套 + Phase 2）。

```powershell
git push -u origin docker2
```

push 后你在 Actions 里应看到 **Harness Eval 冒烟测试** 跑绿。

---

## 快速命令索引

| 目的 | 命令 |
| --- | --- |
| 本地 = CI | `node evals/scripts/ci-smoke.mjs` |
| 当前指纹 | `node evals/scripts/workflow-hash.mjs` |
| 对比基线 | `node evals/scripts/compare-baseline.mjs --print` |
| live 稳定性 | `run-suite.mjs --suite protocol --mode live --trials 3` |
| 更新基线 | `compare-baseline.mjs --update-baseline --batch-prefix <前缀>` |

---

## 当前基线（2026-07-16）

- `workflow_tree_hash`: `024f35c525515a7d`
- protocol dry: 5/5
- product dry: 1/1（B01）

指纹变更且 live 未回归前，**不要** `--update-baseline`。
