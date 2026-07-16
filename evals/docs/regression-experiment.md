# 回归实验记录模板

> 最新实跑：`regression-results.md`

## 已验证能力

| 检查 | 命令 | 预期 |
| --- | --- | --- |
| 判卷能抓违规 | `node evals/scripts/run-question.mjs --question P01-readonly-gate --mode dry --fixture fail` | 退出码 1 |
| 判卷能放合规 | `... --fixture pass` | 退出码 0 |
| workflow 指纹 | `node evals/scripts/workflow-hash.mjs` | 输出 `workflow_rev` + `workflow_tree_hash` |
| CI smoke | `node evals/scripts/ci-smoke.mjs` | 自测 + protocol/product dry + 基线门禁 |
| 基线对比 | `node evals/scripts/compare-baseline.mjs --print` | 指纹与基线一致/变更告警 |

操作分工见 **`operator-playbook.md`**。

## Live 行为回归（限制说明）

弱化沙箱 `AGENTS.md` 后，Agent **仍可能**按 `task.md` 只读题面作答而不写 intake marker（**假阴性**）。  
H3 门禁应组合：

1. **`workflow_tree_hash` 变更** → 重跑 eval 并对比分数  
2. **dry fail 夹具** → 证明判卷能识别违规副作用  
3. **live 弱化** → 探索用，不作唯一门禁  

```powershell
$env:EVAL_AGENT = "claude"
node evals/scripts/run-regression-weaken.mjs
```

## 记录表

| 日期 | 改动 | dry fail | CI smoke | live 弱化 | live 恢复 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-07-16 | 沙箱强制 intake | 未通过 ✓ | 本地通过 ✓ | 仍通过（假阴性） | 通过 ✓ | 见 regression-results.md |
| 2026-07-16 | Darwin Phase 2 基线+GHA | — | `ci-smoke.mjs` 6/6 ✓ | — | 待 push 后远端验证 | 见 operator-playbook.md |
