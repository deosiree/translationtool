# 工具注册表

Harness 涉及两类不同的「工具」，请严格区分。

| | 能力清单（出站） | 入站工具注册表 |
| --- | --- | --- |
| 方向 | Harness 提供给 Agent 使用 | 项目为 Harness 配备 |
| 示例 | 下文 `harness-cli` 子命令 | gitnexus、c3、linter、deploy check |
| 存在性 | 始终编译内置 | 可选；任意机器上可能缺失 |
| 若缺失 | 不适用（即 Harness 本身） | 干净跳过；绝不阻塞主流程 |

本文档描述二者。**入站注册表**是扩展基础：Harness 在此了解额外配备了哪些能力、用途为何、以及当前是否实际存在，使工作流步骤能根据已安装内容自适应，而核心从不依赖它。

## 入站注册表：注册工具

```bash
scripts/bin/harness-cli tool register \
  --name deploy-check \
  --kind cli \
  --capability deploy-verification \
  --command ./scripts/deploy-check.sh \
  --description "Verify deploy health before release" \
  --responsibility Verification \
  --args "env:enum:required:staging,production"
```

入站工具专有字段：

- `--kind` — 如何访问与探测工具。取值为 `cli`、`binary`、`mcp`、`skill`、`http` 之一。默认 `cli`。kind 告知各 Agent 运行时能编排什么（无法运行 `skill` 的非 Claude Agent 将其视为 absent），并告知 `tool check` 使用何种探测。
- `--capability` — 步骤按工作流用途查找工具时使用的标识。自由文本但规范化为 kebab-case，故 `Impact Analysis`、`impact_analysis`、`impact-analysis` 均注册为 `impact-analysis`。这是步骤与工具之间唯一的耦合；步骤引用 capability，从不引用工具名。
- `--scan` — 对 `mcp`/`skill`/`http`，声明式路径或 URL，供 `tool check` 解析以判断存在性（如 `.c3`、`~/.claude/skills/c3`、`https://localhost:8080/health）。`cli`/`binary` 通过其 command 探测。

`--force` 仅当 `cli`/`binary` 的 command 在当前机器上故意缺失时需要。`mcp`/`skill`/`http` 本质上不在 `PATH` 上，故注册时无需 `--force`；存在性稍后由 `tool check` 解析。

注册 MCP 服务器或 Claude skill（示例）：

```bash
scripts/bin/harness-cli tool register --name gitnexus --kind mcp \
  --capability impact-analysis --scan ".gitnexus" --command "mcp:gitnexus" \
  --description "Code-graph blast radius" --responsibility Verification
scripts/bin/harness-cli tool register --name c3 --kind skill \
  --capability impact-analysis --scan ".c3" --command "skill:c3" \
  --description "Component model and drift audit (Claude skill)" \
  --responsibility Verification
```

移除工具：

```bash
scripts/bin/harness-cli tool remove --name deploy-check
```

## 入站注册表：检查存在性

注册记录意图。`tool check` 扫描各已注册工具并将裁决（`status` 与 `checked_at`）持久化，使意图与现实对齐。在 intake 开始时运行，使 status 反映当前现实。

```bash
scripts/bin/harness-cli tool check            # scan all registered tools
scripts/bin/harness-cli tool check --name c3  # scan one
scripts/bin/harness-cli tool check --json     # machine-readable for agents
```

按 kind 探测：

| Kind | 探测 | `present` 含义 |
| --- | --- | --- |
| `cli`, `binary` | command 在 `PATH` 上或作为路径可解析 | 已安装且可运行 |
| `mcp`, `skill` | `scan_target` 路径可解析（`~` 展开） | 磁盘上已配备/已配置 |
| `http` | `scan_target` 经 TCP 可达（2s），否则为路径 | 端点有响应 |

`tool check` 始终以 `0` 退出：缺失的扩展是待报告的事实，而非 CLI 失败。`cli`/`binary` 在可运行时即为 `present`。`mcp`/`skill`/`http` 的 `present` 表示**已配备**（配置/文件可解析），而非**本会话存活** — Agent 仍须在调用时确认实际可用，因为只有 Agent 运行时能判断其 MCP 服务器是否真正连接。无 `scan_target` 时 status 为 `unknown`，须由 Agent 确认。

## 入站注册表：按 Capability 查找

工作流步骤问「此用途下有什么可用？」而非点名工具：

```bash
scripts/bin/harness-cli query tools --capability impact-analysis
scripts/bin/harness-cli query tools --capability impact-analysis --status present
```

结果为 provider 集合。多个工具可提供同一 capability（gitnexus 与 c3 均服务 `impact-analysis` 且互补），故步骤读取集合并按 present 比例降级。

### 降级阶梯

CLI 报告事实（`status`）；Agent 应用策略。通用规则，按某 capability 的 present provider 数量：

| Present provider 数 | 姿态 | Agent 行为 |
| --- | --- | --- |
| 无注册 | Inactive | 干净跳过；在 trace 中注明 `capability X: inactive`。非 drift。 |
| 已注册但 none/some present | Degraded | 用已解析者运行；设置 `Weak proof` 标志；注明缺口。 |
| 全部 present | Full | 正常操作。 |

已注册但扫描为 `missing` 的工具是有效性门失败，非跳过。无注册 provider 的 capability 仅为 inactive，无惩罚跳过 — 这使核心在全新安装上无缝运行。

### 推荐 Capability 词汇

Capability 开放（新增无需改代码），但步骤与其 provider 须约定精确字符串。适用时优先复用下列项，再 coin 新的 kebab-case 项：

```
impact-analysis · deploy-verification · coverage · security-scan
performance-benchmark · documentation-lookup
```

## 检查注册表

```bash
scripts/bin/harness-cli query tools --summary
scripts/bin/harness-cli query tools --json
scripts/bin/harness-cli query tools --responsibility Verification
```

JSON 记录携带 `kind`、`capability`、`scan_target`、`status`、`checked_at` 及既有字段，任意 Agent 可读注册表而无需解析人类表格。

## 编译 Harness 命令（出站清单）

| Command | Responsibility | Purpose | Arguments |
| --- | --- | --- | --- |
| `init` | Task state | 创建 Harness 数据库。 | none |
| `migrate` | Task state | 应用待处理 schema 迁移。 | none |
| `import brownfield` | Project memory | 从 markdown 状态播种持久记录。 | none |
| `intake` | Task specification | 记录功能 intake 分类。 | `--type`, `--summary`, `--lane` |
| `story add` | Task state | 创建持久 story 记录。 | `--id`, `--title`, `--lane`, optional `--verify` |
| `story update` | Task state | 更新非完成 story 的 status、proof 标志、evidence 或 verification command；`implemented` 须用 `story complete`。 | `--id`, optional proof/status fields |
| `story update --json` | Task state | 以机器可读方式执行非完成 status 更新，含事务 compare-and-set/runnable 前置条件。 | `--id`, `--status`, `--expected-status`, optional `--require-runnable` |
| `story dependency add` | Task state | 添加无环持久依赖边。 | `--blocker`, `--blocked` |
| `story dependency remove` | Task state | 移除持久依赖边；缺失边不变。 | `--blocker`, `--blocked` |
| `story hierarchy add` | Task state | 添加幂等、无环 parent/child 边。 | `--parent`, `--child`, optional `--json` |
| `story hierarchy remove` | Task state | 移除幂等 parent/child 边。 | `--parent`, `--child`, optional `--json` |
| `story backlog link` | Task state | 添加可重放的 `resolves` 或 `references` 链接至稳定 backlog occurrence。 | `--story`, `--backlog`, `--relationship` |
| `story backlog unlink` | Task state | 移除关系；已关闭 resolver 溯源不可变。 | `--story`, `--backlog` |
| `story backlog list` | Task state | 显示 story 与 backlog 关系。 | optional `--story`, `--backlog` |
| `story verify` | Verification | 运行一条 story `verify_command` 并记录 pass/fail。 | story id |
| `story complete` | Task state | 运行 fresh proof 并原子实现 eligible story 及已接受 resolver backlog 工作。 | story id |
| `story verify-all` | Verification | 运行所有已配置 story verification command，跳过无 command 的 story。 | none |
| `decision add` | Project memory | 创建持久 decision 记录。 | `--id`, `--title`, optional `--doc`, `--verify` |
| `decision verify` | Verification | 运行一条 decision verification command。 | decision id |
| `backlog add` | Entropy auditing | 记录 Harness 改进提案。 | `--title`, optional pain/suggestion/risk/predicted fields |
| `backlog close` | Entropy auditing | 以 outcome evidence 关闭 backlog 项。 | `--id`, optional `--status`, `--outcome` |
| `backlog reconcile` | Entropy auditing | 预览或应用保守 legacy lifecycle identity 回填。 | `--action backfill-lifecycle-identity`, exactly one of `--dry-run` or `--apply` |
| `backlog outcome record` | Entropy auditing | 向已实现的 keyed occurrence 追加 measured outcome。 | `--id`, `--status`, `--outcome`, optional `--evidence` |
| `tool register` | Tool access | 注册外部项目工具。 | `--name`, `--command`, `--description`, `--responsibility`, optional `--kind`, `--capability`, `--scan`, `--args`, `--force` |
| `tool check` | Tool access | 扫描已注册工具并持久化 present/missing/unknown status。 | optional `--name`, `--json` |
| `tool remove` | Tool access | 移除已注册外部工具。 | `--name` |
| `intervention add` | Intervention recording | 记录 human、reviewer、CI 或 agent intervention。 | `--type`, `--description`, `--source`, optional `--trace`, `--story`, `--impact` |
| `trace` | Observability | 记录 Agent 执行 trace 并打印 trace quality。 | `--summary`, optional trace fields |
| `score-trace` | Observability | 按 lane 要求对 trace detail 评分。 | optional `--id` |
| `score-context` | Context selection | 按编译 context rules 对 trace reads 评分。 | trace id |
| `audit` | Entropy auditing | 运行 drift 检查并计算 entropy score。 | none |
| `propose` | Entropy auditing | 读取确定性改进提案，或显式 accept/reject 一个 stable key。 | `--accept <key>` plus one outcome schedule, or `--reject <key> --reason <text>` |
| `query matrix` | Task state | 显示持久 story proof matrix，可选聚焦 active、runnable 或单一 story，不含长 evidence 文本。 | optional `--numeric`, `--active`, `--runnable`, `--story <id>`, `--summary` |
| `query contract` | Tool access | 发现 protocol、capabilities、supported schema range 与 DB state，无写入。 | required `--json` |
| `query stories` | Task state | 返回稳定 orchestration story 记录。 | required `--json` |
| `query work-graph` | Task state | 返回一条事务一致 story/dependency/hierarchy 图及 revision。 | required `--json` |
| `query dependencies` | Task state | 显示 story dependency 边。 | optional `--story` |
| `query hierarchy` | Task state | 显示确定性 parent/child 边。 | optional `--story`, optional `--json` |
| `query backlog` | Entropy auditing | 显示 Harness 改进 backlog；带 `--id` 时含其关系。 | optional `--open`, `--closed`, `--id` |
| `query decisions` | Project memory | 显示持久 decision 记录。 | none |
| `query intakes` | Task specification | 显示最近 intake 记录。 | none |
| `query traces` | Observability | 显示最近 trace 记录。 | none |
| `query friction` | Failure attribution | 显示含 Harness friction 的 trace。 | none |
| `query tools` | Tool access | 显示编译与已注册 tool 条目。 | optional `--json`, `--summary`, `--responsibility`, `--capability`, `--status` |
| `query interventions` | Intervention recording | 显示 intervention 记录。 | optional `--trace`, `--story`, `--type` |
| `query stats` | Task state | 显示持久记录计数。 | none |
| `query sql` | Tool access | 对 `harness.db` 运行一条只读 SQL。 | SQL text |
| `db changeset apply` | Task state | 幂等应用一条 semantic changeset。 | changeset path |
| `db changeset status` | Task state | 解析并检查 changeset ID/content SHA/applied state，无写入。 | changeset path, required `--json` |
| `db snapshot` | Task state | 创建 integrity-checked 原子 SQLite online-backup snapshot。 | `--output`, required `--json` |
| `db rebuild` | Task state | 从 semantic changesets 重建全新 `harness.db`。 | `--from` changeset directory |

精确的 protocol-v1 envelope、exit code、runnable 定义、timeout 与 cancellation 规则及 JSON schema 以 `docs/contracts/harness-orchestration-v1.md` 为规范。注册表仅为人类命令索引。

## 校验规则

- 工具名在已注册工具中须唯一。
- Description 须 10–200 字符。
- Responsibility 须匹配 Runtime Substrate responsibility 列表。
- `--kind` 须为 `cli`、`binary`、`mcp`、`skill`、`http` 之一。
- `--capability` 须为 kebab-case（小写字母、数字、单连字符）；空格与下划线规范化为连字符。
- `--args` 条目须用 `name:type:required` 或 `name:type:required:help`，第三字段为 `required` 或 `optional`。
- 对 `cli`/`binary`，command 须作为路径存在或在 `PATH` 上，除非提供 `--force`。`mcp`/`skill`/`http` 跳过此检查。
