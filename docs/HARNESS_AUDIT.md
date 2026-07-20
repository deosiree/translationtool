# Harness 审计

`scripts/bin/harness-cli audit` 检测持久 Harness 状态中的 drift，并打印 entropy score。分数越低越好。

## 检查项

| Category | 含义 | Weight |
| --- | --- | --- |
| Orphaned stories | `planned` 或 in-progress 且无关联 trace 的 story。 | 10 |
| Unverified stories | Active 或 `implemented` 且有 `verify_command` 但无已记录 verification result 的 story。Retired story 为历史记录，不计入。 | 5 |
| Unverified decisions | 有 `verify_command` 但无已记录 verification result 的 decision。 | 5 |
| Open backlog without outcomes | 无 append-only outcome observation 的 keyed implemented occurrence，以及无 `actual_outcome` 的无 key legacy implemented 项。 | 2 |
| Stale stories | 未实现且最近关联 trace 超过 30 天的 story。 | 3 |
| Broken tools | 已注册但 command 在磁盘或 `PATH` 上找不到的工具。 | 8 |

## 分数

```text
score = orphaned_stories * 10
      + unverified_stories * 5
      + unverified_decisions * 5
      + backlog_without_outcomes * 2
      + stale_stories * 3
      + broken_tools * 8
```

分数上限为 100。

| Range | 解读 |
| --- | --- |
| 0 | 完美：记录已 trace、已 verify 且健康。 |
| 1-25 | 健康：仅剩少量 housekeeping。 |
| 26-50 | 需关注：drift 在累积。 |
| 51-100 | 须行动：陈旧状态削弱 Harness 价值。 |

审计发现输入 `scripts/bin/harness-cli propose`，可将重复 drift 转为 proposed backlog 项。

实现证明与 measured impact 刻意分离。对 keyed improvement occurrence， `backlog_outcome_observation` 中任意一行即满足审计检查，包括 legacy reconciliation 保留的中性 `legacy_recorded` 行。可变 legacy `actual_outcome` 列仅对无 key 兼容行查询。
