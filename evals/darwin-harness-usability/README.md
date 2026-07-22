# Darwin 评估：升级后 harness 是否更好用

评估模式：`evaluate-only`（不出优化循环）。资产：本仓 harness 文档 + 协议题库。

## 测试题（Phase 0.5）

见 [`test-prompts.json`](./test-prompts.json)。已入库：

| 题号 | 覆盖能力 |
| --- | --- |
| `P06-quality-loop-done` | DONE 外证阶梯、对抗审查、禁虚构根 type-check |
| `P07-api-ssot-entry` | API_CONTRACTS / `:18002/docs`；前端非字段权威 |

## 对照结果（独立子 agent）

| 条件 | dim8 (1–10) | 三题命中 expected |
| --- | --- | --- |
| 无新文档（升级前） | 3 | 0/3 完整命中 |
| 有 QUALITY_LOOP + HARNESS_REVIEW + API_CONTRACTS | 10 | 3/3 |

粗算「可用性」加权分：升级前约 **52** → 升级后约 **88**（Δ **+36**）。日志：[`results.tsv`](./results.tsv)。

## 题库回归

- protocol dry：7/7（含 P06/P07）
- product dry：2/2
- `node evals/scripts/ci-smoke.mjs`：通过（批次 `ci-smoke-2026-07-22T10-02-22`）

## 结论

**是，升级后更好用**：DONE 怎么证、审查一页纸、API 字段找哪，从「拼旧文档猜」变成「有权威入口」。评估体系已用 P06/P07 锁住回归。

## Phase 2 优化（至 HL-4）

| 轮次 | 改动焦点 | 总分 | Δ |
| --- | --- | --- | --- |
| 结构基线 | — | 75.8 | — |
| 批量 | QUALITY_LOOP 三段式失败表 + 🔴/🛑 + 黑名单；CONTEXT/INTAKE/API 挂链 | 81.7 | +5.9 |
| 微轮 | FEATURE_INTAKE 完成证明检查点 | 82.3 | +0.6 |
| 微轮 | HARNESS_REVIEW 旁证链 | 82.7 | +0.4 |

连续两轮 Δ&lt;2 → **HL-4 触顶**，停止文档 hill-climbing。

## 🔴 CHECKPOINT

本轮 **evaluate-only + optimize→HL-4** 结束。建议收手于 harness 文档润色；若继续，优先**题库 full_test**（如 P08），勿再堆链接。
