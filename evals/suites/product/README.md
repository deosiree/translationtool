# 业务套件（Phase 2）

业务题考 translationtool 真实分拣与落点。当前：

- **B01-intake-routing** — 混合需求（前端文案 + 新 API）须拆分 `translation/` 与 `terminology-agent/`，禁止新业务默认进 Java。
- **B02-workbench-verify-seed** — 工作台验数须走 skill `工作台验数播种`；五人员 + `entry_state=3`；禁止用整库 restore 代替播种。

```powershell
node evals/scripts/run-question.mjs --question B01-intake-routing --mode dry --fixture pass
node evals/scripts/run-question.mjs --question B02-workbench-verify-seed --mode dry --fixture pass
node evals/scripts/run-question.mjs --question B02-workbench-verify-seed --mode dry --fixture fail
node evals/scripts/run-suite.mjs --suite product --mode dry
```
