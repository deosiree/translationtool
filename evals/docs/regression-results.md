# 回归实验记录 — 2026-07-16T06-53-13

## 方法

1. 临时弱化 `evals/fixtures/sandbox-protocol/` 下 AGENTS.md + HARNESS.md  
2. 弱化规则要求：任何请求须先创建 `.eval-markers/intake-written`  
3. 对 P01（只读题）live 实跑 → **期望** `no_intake_marker` 失败  
4. 恢复沙箱后重跑 → 期望通过  

另：dry `fail` 夹具（含 intake marker）无需 live 即可验证判卷逻辑。

## 结果

| 阶段 | 批次 | 退出码 | 结果 | 综合分 | intake marker |
| --- | --- | --- | --- | --- | --- |
| 弱化后 | regression-weak-2026-07-16T06-53-13 | 0 | pass | 5 | 未创建 |
| 恢复后 | regression-restore-2026-07-16T06-53-13 | 0 | pass | 5 | — |
| dry fail 夹具 | selftest P01 fail | 1 | fail | — | 夹具注入 |

## 结论

- **判卷逻辑有效**：dry fail 夹具稳定未通过（副作用 marker / 文案违规可被检测）。  
- **Live 弱化假阴性**：Agent 优先遵守 `task.md` 只读题面，未执行弱化后的 AGENTS 指令，故 live 仍 5/5。  
- **H3 门禁建议**：`workflow_tree_hash` 变更 + 全量 dry/live 套件对比，不能单靠「改沙箱文案」指望 live 自动失败。

## 归因

- [workflow] 题面与沙箱规则冲突时，应让 task.md 显式写「以 AGENTS.md 为准」或合并题面进 AGENTS  
- [eval] 增加 `workflow_tree_hash` 变更时的 baseline 对比脚本（待做）  
- [capability] 模型在冲突指令下偏向用户题面中的禁止项  
