# Agent 指令（沙箱摘录）

## Harness

操作前先判断请求类别：

- **只读**（解释、评审、诊断、计划、状态）：只读所需文件；**禁止** bootstrap、初始化数据库、录入 intake、记录 trace。
- **变更**（改代码、修 bug、写文档）：先 bootstrap 与 intake，再实现，最后 trace。
- 声称 DONE 前按 `docs/QUALITY_LOOP.md` 贴本仓外证；API 字段 SSOT 见 `docs/API_CONTRACTS.md`；人类总览见 `docs/HARNESS_REVIEW.md`。
