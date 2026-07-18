# Feature Intake

This intake gate applies to change, build, and fix requests before code or
durable Harness state changes. A new project spec also enters through this gate
before it becomes product docs, stories, or implementation work.

Answer, explain, review, diagnose, plan, and status requests stay read-only.
They do not bootstrap or initialize Harness, record intake, update a story or
backlog item, or record a trace. If the user later asks to implement a proposed
change, that new change request enters this gate.

The human does not need to classify risk. The harness does.

## Intake Flow

```text
User prompt
    |
    v
Classify input type
    |
    v
Restate as work item
    |
    v
Find affected product docs and stories
    |
    v
Run risk checklist
    |
    v
Choose lane: tiny, normal, or high-risk
```

## Input Types

Use the input type to decide where the work should land before choosing the risk
lane.

| Type | Use when | Typical artifact |
| --- | --- | --- |
| New spec | Turning a user-provided project spec into harness-ready docs | Product docs, candidate epics, decisions |
| Spec slice | Implementing selected behavior from an accepted spec | Story packet |
| Change request | Changing, fixing, or refining accepted behavior | Story packet or direct patch |
| New initiative | Adding a larger product area that needs multiple stories | Initiative notes plus story packets |
| Maintenance request | Changing technical, operational, or dependency behavior | Story packet, validation report, or decision |
| Harness improvement | Improving how humans and agents collaborate | Direct docs update or `scripts/bin/harness-cli backlog add` |

Do not create or extend a monolithic spec by default after intake. Use product
docs, stories, decisions, and initiative notes as the living surface.

## Lanes

### Tiny

Use for low-risk docs, copy, names, or narrow edits.

Also use for initial project setup when the work is limited to installing
declared dependencies, wiring a server entrypoint, adding a health/smoke
endpoint, or opening a local development database connection without creating
domain schema, CRUD behavior, auth, authorization, provider integration, or
data migration. A health endpoint in a new benchmark or scaffolded project is
smoke proof, not a public contract escalation by itself.

Requirements:

- Record the intake row before implementation; tiny work skips story packet
  overhead, not durable task classification.
- Patch directly.
- Keep affected docs current.
- Run available quick checks.
- Update the harness only if friction was found.

### Normal

Use for story-sized behavior with bounded blast radius.

Requirements:

- Create or update one story file from `docs/templates/story.md`.
- Link relevant product docs.
- Add or update validation expectations.
- Implement the smallest vertical slice when implementation exists.
- Record or update proof status with `scripts/bin/harness-cli story add` and
  `scripts/bin/harness-cli story update`.

### High-Risk

Use when the work can affect security, data, scope, contracts, or multiple
roles/platforms.

Requirements:

- Create a story folder using `docs/templates/high-risk-story/`.
- Fill in `execplan.md`, `overview.md`, `design.md`, and `validation.md`.
- Ask for human confirmation before implementation if direction is ambiguous.
- Record a durable decision when behavior, architecture, authorization, data
  ownership, API shape, or validation requirements change meaningfully. Use a
  `docs/decisions/NNNN-*.md` file from `docs/templates/decision.md`, then add
  or refresh the durable row with `scripts/bin/harness-cli decision add`.
  Decision text in a trace is not a durable decision record.

## Risk Checklist

Mark one flag for each item that applies:

| Risk flag | Applies when the work touches |
| --- | --- |
| Auth | login, logout, sessions, JWT, password, refresh token |
| Authorization | roles, permissions, tenant or company scope |
| Data model | schema, migrations, uniqueness, deletion, retention |
| Audit/security | audit logs, privacy, sensitive data, access logs |
| External systems | email, payments, cloud services, provider SDKs, queues, webhooks |
| Public contracts | API shape, response envelope, client-visible behavior |
| Cross-platform | desktop/mobile/browser split, native shell behavior, deep links |
| Existing behavior | already implemented or test-covered behavior changes |
| Weak proof | unclear or missing tests around the affected area |
| Multi-domain | more than one product domain changes at once |

## Classification

```text
0-1 flags:
  tiny or normal, based on code impact

2-3 flags:
  normal with stronger validation

4+ flags:
  high-risk

Any hard gate:
  high-risk unless the human explicitly narrows scope
```

Hard gates:

- Auth.
- Authorization.
- Data loss or migration.
- Audit/security.
- External provider behavior.
- Removing or weakening validation requirements.

## Output

At the end of intake, the agent should be able to say:

```text
Lane: normal
Reason: touches authorization, API contract, and audit behavior.
Docs: permissions, account-settings, audit-log.
Story: docs/stories/epics/E02-access-control/US-014-manager-updates-role.md.
Validation: unit, integration, E2E.
```

## Translation Tool — 模块分拣（本仓库）

在通用 lane（tiny / normal / high-risk）之外，**必须先标模块面与后端面**：

### 后端面（强制）

| 后端面 | 含义 | 目录 | 默认 |
| --- | --- | --- | --- |
| `backend=python` | **新需求、新 API**（含工作台新能力）、Agent/编排 | `terminology-agent/` | **是（默认）** |
| `backend=java-maintain` | **扩展旧 Java API**（加参/出参）、修 bug、安全、不得不兼容 | `translationtoolservice/` | 否；需人类确认或需求点名 |

规则：

- **工作台新需求 / 新 API → 仍是 Python**；「页面在工作台」不等于「后端用 Java」。
- 新功能**禁止**默认落到 Java；先论证「为何不能做在 Python」。
- **只有**要给**已有旧 API**增加入参/出参（或等价遗留修补）时，才标 `java-maintain`。
- Java 他人主责：**能不动就不动**；主动扩大 Java 改动面视为流程违规，intake 须写明原因。
- 若方案被迫改 Java，lane 至少 **normal**，并建议请人类确认后再实现。

### 持久化可见面（强制追问，继承 ADR 0010）

在标 `backend=*` 之后，若需求含「展示 / 落库 / 给人工查」，先定可见面，再定接口形态：

| 层级 | 含义 | 默认做法 |
| --- | --- | --- |
| **A. Agent 真源** | 新语义落 Agent 表 | Python 自有表 + Agent UI |
| **B. 会话可见** | 预翻译当次 / `agent_meta` | Python API + 前端 |
| **C. 工作台持久可见** | 重进任务后工作台仍要看到 | **优先 Python 新 API** 供工作台消费；**不要**默认去改旧 Java 列表 |

检查口令（intake 输出里写一句即可）：

1. 新能力/新 API，还是**必须改某个已有 Java 接口的入参/出参**？→ 后者才 `java-maintain`。
2. 关弹窗后还要在工作台看到吗？→ 否：A（+B）；是：C，仍优先 Python 新读接口。
3. 是否误把「工作台页」当成「必须改 Java」？

权威决策：`docs/decisions/0010-dual-backend-read-vs-write-persistence.md`。

### 模块面

| 模块面 | 典型改动 | 目录 | 规范提示 |
| --- | --- | --- | --- |
| 前端需求 | 页面、组件、路由、Electron、proxy | `translation/` | Vue 3 + Ant Design Vue；大弹窗/回填类组件注意状态回归 |
| **新后端 / Agent 需求** | FastAPI 路由、Graph、节点、prompt、pytest | `terminology-agent/` | **默认后端落点**；LangGraph 可测；密钥走 `.env` |
| **Java 维护需求** | 遗留 Controller/Service/Mapper/SQL/鉴权修补 | `translationtoolservice/` | 最小 diff；破坏性 API/表结构 → high-risk + 人类确认 |
| 全栈需求 | UI + Python（常见）或含 Java 维护 | 多目录 | 拆 story；先定契约；含 Java 时单独标 `java-maintain` |
| Infra / 本地开发 | compose、根 `pnpm dev*`、JDK 脚本 | 根目录 / `docker-compose.yml` | 端口契约 `18000/18001/18002` 勿随意改 |

### 本仓库附加风险旗标

| 旗标 | 何时勾选 |
| --- | --- |
| Java touch | 任何对 `translationtoolservice/` 的改动（即使很小） |
| Cross-module contract | UI ↔ Java ↔ Python 的请求路径、字段、错误码任一变更 |
| Agent / LLM | prompt、图拓扑、模型供应商、超时与重试策略 |
| Electron / desktop | 壳层、文件对话框、本地路径、与 Web 行为分叉 |
| Terminology / 词条数据 | 回填、导入导出、词典文件、可能丢数据的批量写 |

### 本地手工验收与脏库（运维辅助）

用户说 **「备份数据库 / 准备回滚点 / 测试前备份 / 回滚 / 恢复到备份」**，或即将**手工预翻译 / 术语学习同意 / 工作台改译**并可能事后回滚时：

1. **必须**按 `docs/ops/DEV_DB_CHECKPOINT.md` + skill `db-回滚数据库` 的脚本执行（`backup-database.ps1` / `restore-database.ps1`）。
2. **禁止**自写 PowerShell：`>` / `Out-File` / `Set-Content` / `Get-Content | docker exec` 接 mysqldump/mysql（会截断中文 COMMENT，dump 作废、restore 1064）。
3. **锁定路径**：容器内 `mysqldump --result-file` + `docker cp`；恢复同为 `docker cp` + `mysql --default-character-set=utf8mb4 < file`；须过 `verify-dump-encoding`，失败不得声称成功。
4. backup 成功后回传 `backupPath`；restore 须人类确认后再 `-Force`。
5. 仅 ADM 矩阵污染时用 `adm_matrix_reset`，不必整库 DROP。

外部 skill：`huiyanSkills/translateTool-skills/db-回滚数据库`。

任一项成立时，至少按 **normal**；同时触及 Auth / Data model / Public contracts 则按上文 hard gate 升为 **high-risk**。勾选 **Java touch** 时，输出中必须说明「为何不能只改 Python」。
