# Trace 规范

`trace` 表记录 Harness 任务期间发生的内容。本文档定义各字段的预期深度与格式，使 trace 可用于审查、benchmark 评分、failure attribution 与未来 Harness 演进。

当前 schema 位于 `scripts/schema/001-init.sql` 的 `trace` 表。Phase 2 不修改 schema。

## 字段参考

| Field | Type | Required | Format | Example |
| --- | --- | --- | --- | --- |
| `id` | INTEGER | Automatic | SQLite autoincrement primary key。勿手动设置。 | `42` |
| `created_at` | TEXT | Automatic | SQLite `datetime('now')`。勿手动设置。 | `2026-05-27 09:24:37` |
| `task_summary` | TEXT | Yes | 一句，至少 10 字符，说明 outcome 或 attempted outcome。 | `Completed Phase 2 docs-only observability and taxonomy specification` |
| `intake_id` | INTEGER | Standard+（已记录 intake 时） | 相关 `intake` 行的整数 id。 | `36` |
| `story_id` | TEXT | Standard+（工作映射到一条 story 时） | `story` 表的 story id。一条 trace 覆盖多条时用主 story；其余列于 `notes`。 | `US-004` |
| `agent` | TEXT | Minimal 可选；Standard+ 预期 | 简短 agent/tool 名。 | `codex` |
| `actions_taken` | TEXT | Standard+ | JSON array 文本。当前 CLI 下传逗号分隔列表，CLI 存 JSON 文本。 | `["read PHASE2.md","drafted TRACE_SPEC.md","updated HARNESS.md"]` |
| `files_read` | TEXT | Standard+ | 路径或命令名的 JSON array 文本。当前 CLI 下传逗号分隔列表。 | `["PHASE2.md","docs/HARNESS.md","scripts/bin/harness-cli query matrix"]` |
| `files_changed` | TEXT | Standard+ | 已改文件路径的 JSON array 文本。当前 CLI 下传逗号分隔列表；无文件变更时省略。 | `["docs/TRACE_SPEC.md","docs/HARNESS.md"]` |
| `decisions_made` | TEXT | Detailed | decision 字符串的 JSON array 文本。含 scope decision、validation choice、显式 non-goal。 | `["Kept Phase 2 docs-only; installer propagation remains out of scope"]` |
| `errors` | TEXT | 有错误时 Standard+；Detailed 始终 | 错误或 blocker 字符串的 JSON array 文本。CLI 暂不支持空 array 时，Detailed trace 需显式无错误证据用 `none`。 | `["git diff --check failed before whitespace fix"]` |
| `outcome` | TEXT | final response 前必填 | 取 `completed`、`blocked`、`partial`、`failed` 之一。 | `completed` |
| `duration_seconds` | INTEGER | Detailed（可用时） | 正整数估计或实测 duration。未知则留 null。 | `1800` |
| `token_estimate` | INTEGER | Detailed（可用时） | 正整数估计。未知则留 null。 | `24000` |
| `harness_friction` | TEXT | 有 friction 时 Standard+；Detailed 始终 | 自由文本，说明困难、缺失、模糊或重复之处。仅当 Agent 主动检查且无 friction 时用 `none`。 | `New Phase 2 docs are not in installer copy list; recorded as out-of-scope follow-up.` |
| `notes` | TEXT | Optional | 不适配其他字段的审查上下文自由文本。 | `Trace covers US-003, US-004, US-005, and US-006.` |

## 质量层级

### Minimal（score: 1）

最低字段：

- `task_summary` 已填且至少 10 字符。
- final response 前 `outcome` 已填。

适用于：

- 无文件变更或仅 low-risk 文案/文档编辑的 Tiny-lane 任务。

不适用于：

- Normal 或 high-risk 工作。
- 发现 friction、error 或缺失 validation path 的任何工作。

### Standard（score: 2）

最低字段：

- 全部 Minimal 字段。
- 已记录 intake 时填 `intake_id`。
- 工作清晰映射一条 story 时填 `story_id`。
- `agent`。
- `actions_taken` 为 JSON array 文本。
- `files_read` 为 JSON array 文本。
- `files_changed` 为 JSON array 文本。
- `errors` 或 `harness_friction` 至少其一。

必需于：

- Normal-lane 任务。
- 变更 Harness 指令、validation expectation 或 durable record 的 Tiny 任务。

Standard trace 在 `duration_seconds`、`token_estimate`、`decisions_made` 无用时可为空。

### Detailed（score: 3）

最低字段：

- 全部 Standard 字段。
- `decisions_made` 为 JSON array 文本。
- `errors` 为 JSON array 文本；当前 CLI 无错误时用 `none`。
- `harness_friction`；检查后无 friction 才用 `none`。
- `duration_seconds`，或说明 duration 不可用的 note。
- `token_estimate`，或说明 token estimate 不可用的 note。
- 一条 trace 覆盖多条 story、多个 risk flag 或跳过 validation 时填 `notes`。

必需于：

- High-risk 任务。
- 触及 architecture direction、source-of-truth hierarchy、validation requirement、auth、authorization、data loss、audit/security 或 external provider behavior 的变更。
- 后续审查需精确 proof 的 benchmark 或 release 工作。

High-risk 工作中 trace 的 `decisions_made` 摘要已做决策，不替代持久 decision 记录。若工作改变 behavior、architecture、authorization、data ownership、API shape 或 validation requirement，添加 `docs/decisions/NNNN-*.md` 并用 `scripts/bin/harness-cli decision add` 记录。

## Lane 映射

| Lane | Expected Tier | 最低 Trace 行为 |
| --- | --- | --- |
| Tiny | Minimal | 记录 summary 与 outcome；有 friction 或 Harness 文档变更时用 Standard。 |
| Normal | Standard | 记录 intake、actions、files read、files changed、outcome 与 friction/errors。 |
| High-risk | Detailed | 记录全部字段，或显式说明不可用的 duration/token estimate。 |

## Friction 捕获协议

下列任一情况时填写 `harness_friction`：

- Agent 须推断缺失 rule 或 source of truth。
- 所需 validation 不清晰、不可用或运行成本过高。
- 文档、durable record 或 story packet 陈旧或矛盾。
- 任务暴露应变为 template、command 或 checklist 的重复手工步骤。
- 请求变更 out of scope 但日后可能重要。
- benchmark 或 review 失败无法归因到 component。

如何写 friction：

- 写具体痛点，非模糊情绪。
- 含缺失 capability 或矛盾。
- 若 friction 应变为工作，另用 `scripts/bin/harness-cli backlog add` 添加或更新 backlog 项。
- 无 friction 时仅 Detailed trace 用 `none`。

良好 friction：

```text
New Phase 2 docs are not copied by scripts/install-harness.sh, but installer
propagation is out of scope for docs-only Phase 2.
```

薄弱 friction：

```text
docs confusing
```

## 示例

### 良好 Trace（Detailed）

```bash
scripts/bin/harness-cli trace \
  --summary "Completed high-risk auth role migration with audit proof" \
  --intake 51 \
  --story US-014 \
  --agent codex \
  --outcome completed \
  --duration 4200 \
  --tokens 52000 \
  --actions "read access-control docs,created migration,updated audit tests,ran integration suite" \
  --read "docs/product/permissions.md,docs/decisions/0008-auth-boundary.md,src/auth/roles.ts" \
  --changed "src/auth/roles.ts,src/audit/events.ts,tests/auth-roles.test.ts" \
  --decisions "kept manager role scoped to workspace,recorded audit event on every role change" \
  --errors "none" \
  --friction "Existing permission docs did not define delegated admin; added backlog item for role glossary." \
  --notes "Detailed trace required because the task touched authorization and audit behavior."
```

### 合格 Trace（Standard）

```bash
scripts/bin/harness-cli trace \
  --summary "Added Phase 2 trace specification and Harness reference" \
  --intake 36 \
  --story US-004 \
  --agent codex \
  --outcome completed \
  --actions "read PHASE2.md,drafted TRACE_SPEC.md,updated HARNESS.md,ran rg checks" \
  --read "PHASE2.md,docs/HARNESS.md,scripts/schema/001-init.sql" \
  --changed "docs/TRACE_SPEC.md,docs/HARNESS.md" \
  --friction "none"
```

### 不足 Trace

```bash
scripts/bin/harness-cli trace \
  --summary "did phase 2" \
  --outcome completed
```

对 normal-lane Phase 2 工作不足的原因：

- 未标识 actions。
- 未列出 files read 或 changed。
- 未关联 intake 或 story。
- 无 friction 或 error 信号。

## 审查清单

final response 前检查：

- trace tier 与 lane 匹配。
- 查看 `scripts/bin/harness-cli trace` 自动打印的 score。重查历史 trace 用 `scripts/bin/harness-cli score-trace --id N`。
- `files_changed` 在有用粒度上与实际变更文件集一致。
- `errors` 命名真实 blocker，或 Detailed trace 在当前 CLI 下为 `none`。
- `harness_friction` 命名具体问题或有意为 `none`。
- 应变为未来工作的 friction 已记入 backlog。
