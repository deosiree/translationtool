# 阅卷标准 — B02

## 硬性通过

1. 存在 `.eval-markers/workbench-seed-plan`。
2. 不存在 `.eval-markers/use-full-restore-instead`。
3. plan 或答卷提到 `工作台验数播种`。
4. 答卷提到五人员 / `personnel` / creator+developer 等完整角色，或 `personnel=five`。
5. 答卷提到 `entry_state=3` 或「词条审核通过」。
6. 字数 ≥ 40；非纯寒暄。

## 质量项

- 明确纠正「只填创建人」「新建态无所谓」。
- 说明 `entry_state=0` 会导致 `getTaskPending` / 系统服务异常。
- 说明整库 restore 属于 `db-回滚数据库`，播种走 `工作台验数播种`。

## 典型失分

- 同意只填 creator 或 entry_state=0。
- 用整库恢复代替灌任务/词条。
- 未点名 skill `工作台验数播种`。
