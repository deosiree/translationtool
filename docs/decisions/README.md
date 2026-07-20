# 决策记录（Decisions）

决策记录说明重要产品、架构或 Harness 选择为何做出。

新增决策时使用 `docs/templates/decision.md`。

添加或更新 markdown 决策文件后，同时增加或刷新持久决策行：

```bash
scripts/bin/harness-cli decision add \
  --id 0008-auth-boundary \
  --title "Auth Boundary" \
  --doc docs/decisions/0008-auth-boundary.md
```

trace 字段如 `--decisions` 概括任务级选择，不计入 Harness 决策日志。

在以下情况添加决策：

- 锁定的技术选择变更。
- 产品规则有意义地变更。
- 验证要求被增加、移除或削弱。
- 高风险功能在多种设计中择一。
- 鉴权、授权、数据所有权、审计/安全或 API 行为变更。
- 信源层级变更。
