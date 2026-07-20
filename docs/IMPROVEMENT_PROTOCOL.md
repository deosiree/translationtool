# 改进协议

Phase 5 启动自改进循环：

```text
friction + interventions + audit findings
  -> harness-cli propose
  -> human accepts or rejects one stable proposal key
  -> accepted backlog occurrence plus outcome-review schedule
  -> implementation with predicted impact
  -> close with implementation proof
  -> later append measured outcome observations
```

## 生成提案

```bash
scripts/bin/harness-cli propose
```

该命令基于规则。查找：

- 重复的 trace friction，
- 重复的 intervention 模式，
- 非零 audit category。

每条提案含 stable versioned key、lifecycle state、title、component、evidence、predicted impact、risk、suggested action、validation plan 与 confidence。不带 decision flag 运行 `propose` 为只读。

Lifecycle state 与 evidence 感知：

- `new`：尚无 keyed occurrence。
- `pending`：已有 proposed occurrence；显示既有 backlog id。
- `accepted`：active work 已存在，无法创建第二个 open occurrence。
- `suppressed`：`implemented` 或 `rejected` occurrence 已覆盖全部当前 stable evidence。默认隐藏。
- `regression`：`implemented` occurrence 之后出现 occurrence lineage 未覆盖的 evidence。
- `reconsideration`：`rejected` occurrence 之后出现 occurrence lineage 未覆盖的 evidence。

在不重新打开已处理 evidence 的情况下检查：

```bash
scripts/bin/harness-cli propose --show-suppressed
```

说明含 terminal occurrence、resolver、closure proof 及为何无 evidence 仍未覆盖。 plausible 无 key legacy 匹配报告为 `legacy-unclassified`，直至操作员运行显式 reconciliation。

## 协调 Legacy 改进

在修改前预览每条无 key 历史改进：

```bash
scripts/bin/harness-cli backlog reconcile \
  --action backfill-lifecycle-identity --dry-run
```

报告将每行标为 `derivable`、`manual`、`ambiguous` 或 `duplicate_candidate`。仅 `derivable` 行 eligible 显式 apply：

```bash
scripts/bin/harness-cli backlog reconcile \
  --action backfill-lifecycle-identity --apply
```

Apply 仅填充缺失 lifecycle identity，为无 UID 的 trace/intervention evidence 嵌入不可变 snapshot，并保留 terminal status、timestamps、raw evidence 与 `actual_outcome`。非空 legacy terminal outcome 一次性复制到中性 append-only `legacy_recorded` observation；非 measured confirmation。重复 apply 为 no-op。Manual、ambiguous 与 duplicate candidate 须人工选择，保持不变。

## 决策一条提案

```bash
scripts/bin/harness-cli propose --accept <proposal-key> --outcome-manual
scripts/bin/harness-cli propose --accept <proposal-key> --outcome-due <RFC3339>
scripts/bin/harness-cli propose --accept <proposal-key> --outcome-after-traces <positive-integer>

# Or retain a terminal human decision without creating implementation work.
scripts/bin/harness-cli propose --reject <proposal-key> --reason "Not worth the added complexity"
```

Acceptance 创建或复用一条 `accepted` backlog occurrence 并打印下一条 `harness_improvement` intake 命令。Rejection 记录一条 terminal reason 与 covered evidence，不创建 intake、story 或 orchestrated run。`propose --commit` 刻意拒绝；Harness 从不 bulk-write 当前显示的全部 suggestion。

Audit-backed 提案在任一决策前须 stable audit episode。若 preview 报告 unrecorded audit evidence，运行 `scripts/bin/harness-cli audit --record-evidence` 并决策新显示的 stable key；proposal 决策从不作为副作用创建 audit evidence。Rejection reason 按精确值存储与比较，故 prefix 或 superset 不视为幂等重试。

Accept 或 reject `regression` 或 `reconsideration` candidate 时追加新 occurrence：新 uid、相同 proposal key、紧邻 prior terminal occurrence 为 `predecessor_uid`、仅 uncovered stable evidence。Predecessor 从不 reopen 或 mutate。Recurrence candidate 保持只读，直至此显式 human decision。

人类用下列命令审查已接受工作：

```bash
scripts/bin/harness-cli query backlog --open
```

## 运行每日健康循环

从只读健康视图开始：

```bash
scripts/bin/harness-cli audit --record-evidence
scripts/bin/harness-cli query improvement-health
```

第一条命令显式记录 audit-evidence transition。第二条不写任何内容：按确定顺序组合当前 audit entropy、proposal decision、accepted work、scheduled outcome review、measured outcome 与 recurrence candidate。每行给出精确 next operator action。

例如：`implemented` occurrence 的 trace-count schedule 为 20、completion baseline 为 100 时，112 条 uid-bearing trace 为 `scheduled_not_due`，剩余 8。120 条 trace 时变为 `due`。当前计数 99 时为 `schedule_error`，因 durable count 低于 baseline 时 Harness 拒绝猜测。

## 完成已接受工作

实现后，resolving story 遵循一条显式序列：

```text
story enters in_progress or changed
  -> implementation finishes
  -> matching completed implementation trace is recorded
  -> story complete runs fresh verification
  -> passing proof marks the story implemented
  -> eligible accepted resolver backlog occurrences close in the same transaction
```

```bash
scripts/bin/harness-cli story complete <US-NNN>
```

失败时 story 仍 completion-eligible，不关闭任何项。重复或并发 completion 幂等。Resolution evidence 记录 story、proof command、completion identity 与 completion time；不声称 later measured outcome。

Resolver link 与新 trace 携带 replayed nanosecond ordering metadata。Completion 仅接受严格晚于最新 resolver link 的 qualifying trace。无 ordering metadata 的 legacy link 使用保守 strict timestamp 比较。Semantic replay 精确保留 link、verification、completion 与 closure timestamp，不以 rebuild time 替代。

## 记录 Measured Outcome

实现后记录实际发生情况，不改变 completion proof 或 legacy `actual_outcome` 字段：

```bash
scripts/bin/harness-cli backlog outcome record --id <local-id> \
  --status confirmed --outcome "Repeated friction fell from 4/5 to 0/5 traces" \
  --evidence "trace uids trc_... through trc_..."
```

允许的 status 为 `confirmed`、`ineffective`、`reverted`。每条命令 append 下一 per-occurrence ordinal。后续 `reverted` observation 成为 current assessment，先前 `confirmed` 行保持不可变。Accepted 或 proposed work 被拒绝，因须先有 passing implementation proof 才能声称 measured impact。Schedule 为提醒，故 evidence 可在 due date 或 trace target 之前记录。

## 审查规则

- Tiny 提案若仅澄清文档可直接实现。
- Normal 提案须 story packet 或明确 backlog acceptance。
- High-risk 提案在改变 source hierarchy、architecture direction、validation requirement 或 risk policy 前须持久 decision 记录。
- Keyed accepted work 由显式 story-completion lifecycle 关闭，非 `backlog close`；later outcome observation 与 implementation proof 分离。

## 验证

实现后，将 predicted impact 与下列项比较：

- `scripts/bin/harness-cli audit`，
- `scripts/bin/harness-cli query friction`，
- `scripts/bin/harness-cli query interventions`，
- benchmark trace quality 与 harness compliance（适用 benchmark proof 时）。
