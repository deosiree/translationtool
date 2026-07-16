<!-- HARNESS:BEGIN -->
## Harness

操作前先判断请求类别。

- 若结果只需回答、解释、评审、诊断、计划或状态报告：只读所需材料，保持只读。不要 bootstrap、初始化或迁移数据库、录入 intake，也不要记录 trace。
- 若用户明确要求修改、构建、修复或写入仓库产物：先在 macOS/Linux 运行 `scripts/bootstrap-harness.sh`，或在 Windows 运行 `.\scripts\bootstrap-harness.ps1`。然后按 `docs/FEATURE_INTAKE.md` 分类并录入请求；在 macOS/Linux 查询 `scripts/bin/harness-cli query matrix --active --summary`，或在 Windows 查询 `.\scripts\bin\harness-cli.exe query matrix --active --summary`；并只拉取 `docs/CONTEXT_RULES.md` 中与车道和任务相关的上下文。
<!-- HARNESS:END -->
