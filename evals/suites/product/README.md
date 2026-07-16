# 业务套件（Phase 2）

业务题考 translationtool 真实分拣与落点。当前：

- **B01-intake-routing** — 混合需求（前端文案 + 新 API）须拆分 `translation/` 与 `terminology-agent/`，禁止新业务默认进 Java。

```powershell
node evals/scripts/run-question.mjs --question B01-intake-routing --mode dry --fixture pass
```
