# Agent 指令（沙箱摘录）

## Harness

操作前先判断请求类别：

- **只读**（解释、评审、诊断、计划、状态）：只读所需文件；**禁止** bootstrap、初始化数据库、录入 intake、记录 trace。
- **变更**（改代码、修 bug、写文档）：先 bootstrap 与 intake，再实现，最后 trace。
