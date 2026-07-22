# Harness（协作运行时）

本项目目标：提供可复用的协作运行时（Harness），让人类与 Agent 能把产品规格安全、可验证地落地。

应用是用户接触的面；Harness 是 Agent 接触的面。

## 心智模型

```text
------------------+
| 人类意图         |
+------------------+
         |
         v
+------------------+
| 功能分拣 Intake  |
+------------------+
         |
         v
+------------------+
| 故事包 Story     |
+------------------+
         |
         v
+------------------+
| Agent 工作循环   |
+------------------+
         |
         v
+------------------+
| 产品增量         |
+------------------+
         |
         v
+------------------+
| 验证证明         |
+------------------+
         |
         v
+------------------+
| Harness 增量     |
+------------------+
         |
         v
+------------------+
| 下一意图         |
+------------------+
```

一次变更请求可以有两类产出：

1. **产品增量**：应用代码、测试、API 形态、数据模型或产品文档。
2. **Harness 增量**（必要时）：文档、模板、验证期望、backlog 项或决策记录，让下一次变更更容易。

## 语言约定

给人看的文案与机器标识分开处理。完整决策见 `docs/decisions/0011-harness-human-docs-zh.md`。

| 用简体中文 | 保留英文 |
| --- | --- |
| 文档正文、标题、表格说明、模板占位提示 | `harness-cli` 子命令与 flag（如 `query matrix --active`） |
| Agent 块、Eval `task.md` / `rubric.md` / review、脚本里给人看的日志/注释 | 题 ID、`env.yaml` 的 `type`、路径、JSON/SQLite 字段名、契约 schema key |
| Decision / Glossary / Maturity 等叙述 | 专有名词首次可「中文（English）」并存 |

合入上游 `repository-harness` 文档时，**不得**用英文原文整文件覆盖本仓库中文版；只移植语义变更后再译。

## Harness v0 范围

Harness v0 包含：

- Agent 入口。
- 空的产品文档结构。
- 功能分拣与风险车道。
- 故事模板。
- 决策日志模板。
- 验证报告模板。
- 基于 SQLite 的证明矩阵，以及遗留导入模板。
- Harness 成长 backlog。
- 持久层：SQLite 数据库与 CLI，用于运营记录。
- 上游契约测试与 PR/发布校验。

Harness v0 **刻意不包含**：

- 消费者项目专用的 `SPEC.md`。
- 预先切好的消费者产品域。
- 锁定的消费者应用技术栈。
- 消费者应用源码脚手架。
- 消费者包脚本与测试运行配置。
- 消费者 CI 工作流。

这些属于已安装项目，只应在某条选定故事需要时引入。上游 Harness 仓库自有 Rust 工作区、测试与 CI，因为 Harness CLI 与模板本身是需要可执行证明的产品。

## 持久层

策略文档描述「怎么做」；持久层记录「发生了什么」。

运营数据——intake 分类、故事状态、决策结果、backlog 项、执行 trace——存放在 SQLite 数据库（`harness.db`）中，由 `scripts/bin/harness-cli` 的 Rust Harness CLI 管理。Agent 与人类应使用该二进制做 Harness 工作。数据库对每个项目实例本地化，并已 `.gitignore`。schema 版本化存放在 `scripts/schema/`。

这种分离让策略文档保持稳定、可读，同时给 Agent 结构化、可查询的运营状态；也为未来的可观测与自动演进做准备，而不必再堆更多 markdown。

若数据库不存在，先初始化：

```bash
scripts/bin/harness-cli init
```

常用命令：

```bash
scripts/bin/harness-cli intake  --type <type> --summary <text> --lane <lane>
scripts/bin/harness-cli story   add --id <id> --title <text> --lane <lane>
scripts/bin/harness-cli story   update --id <id> --status <status>
scripts/bin/harness-cli story   update --id <id> --unit 1 --integration 1 --e2e 0 --platform 0
scripts/bin/harness-cli story   verify <id>
scripts/bin/harness-cli story   complete <id>
scripts/bin/harness-cli story   verify-all
scripts/bin/harness-cli decision add --id <id> --title <text> --doc docs/decisions/<file>.md
scripts/bin/harness-cli trace   --summary <text> --outcome <outcome>
scripts/bin/harness-cli score-trace
scripts/bin/harness-cli score-context <trace-id>
scripts/bin/harness-cli audit
scripts/bin/harness-cli propose
scripts/bin/harness-cli query   matrix
scripts/bin/harness-cli query   matrix --numeric
scripts/bin/harness-cli query   backlog
scripts/bin/harness-cli query   tools --summary
scripts/bin/harness-cli query   interventions
scripts/bin/harness-cli query   stats
scripts/bin/harness-cli --version
```

## 信源层级

```text
用户提供的规格或提示
  首次建设或后续变更的输入材料

docs/product/*
  由已接受输入推导出的当前产品契约

docs/stories/*
  故事级工作包与历史证据

scripts/bin/harness-cli query matrix
  行为→证明控制面板，由持久层支撑

docs/decisions/*
  契约为何变更
```

实现前，产品文档描述意图；实现后，产品文档加上可执行测试成为活契约。

## 规格生命周期

Harness v0 起始时不跟踪项目规格。当人类提供规格时，将其视为**输入材料**，而不是永久操作手册。在首次建设中用它填充产品文档、故事包、架构决策与验证期望。

规格分解完成后，不要继续把它当作活的产品计划来扩展。持续工作应更新更小的产品文档、故事、持久证明记录与决策记录。

持续工作应以如下输入类型之一进入 Harness：

- **新规格**：需要变成产品文档与初始故事候选的项目规格。
- **规格切片**：已提供规格中的选定行为。
- **变更请求**：有界的行为变更、缺陷修复或产品细化。
- **新举措**：需要多条故事的更大产品域。
- **维护请求**：依赖、架构、性能、安全或运维工作。
- **Harness 改进**：流程、模板、证明或 Agent 指令变更。

规格到工作的循环：

```text
人类意图或提供的规格
  -> 分类输入类型
  -> 更新或创建产品契约
  -> 需要时创建故事包或举措说明
  -> 定义验证证明
  -> 实现或记录阻塞
  -> 更新产品文档、故事、持久证明与决策
  -> 捕获 Harness 摩擦
```

大产品域应使用有范围的举措说明，而不是第二份巨型规格。举措应说明目标、受影响产品文档、候选故事、验证形态、未决决策与退出标准。若举措工作成为重复模式，增加模板或用 `scripts/bin/harness-cli backlog add` 记录提案。

## 成长规则

Harness 因摩擦而成长。

当 Agent 困惑、重复手工推理、需要新验证命令、发现缺失规则，或看到重复失败模式时，必须直接改进 Harness，或记录摩擦：

```bash
scripts/bin/harness-cli backlog add --title "<短名称>" --pain "<难点>"
```

对预期会改变 Agent 行为或验证结果的改进，使用 backlog 结果闭环：

1. 创建 backlog 项时，用 `--predicted` 填写改进的可度量预期影响。
2. 关闭项时，用 `--outcome` 填写实际度量结果或评审证据。
3. 用 `scripts/bin/harness-cli query backlog --open` 查看提案与已接受项，用 `scripts/bin/harness-cli query backlog --closed` 在实现后对比预测与结果。

trace 上的 `harness_friction` 字段也会按任务捕获摩擦，便于后续查询：

```bash
scripts/bin/harness-cli query friction
```

backlog 风险使用与 intake、故事相同的车道词汇：`tiny`、`normal` 或 `high-risk`。低风险跟进用 `--risk tiny`；`low` 不是合法车道。

## 请求类别循环

在运行 Harness 命令前先判定权限。请求类别决定是否可变更仓库状态。

### 只读请求

回答、解释、评审、诊断、计划与状态类请求为只读。

1. 阅读 `AGENTS.md` 以及回答所需的文件或证据。
2. 需要时使用只读检查命令。
3. 不要 bootstrap、初始化或迁移数据库、录入 intake、更新故事或 backlog，也不要记录 trace。
4. 有具体仓库证据支撑答案后即可停止。

例如：诊断安装器测试失败时，可以检查测试、安装器与捕获输出；不得仅为了解释失败而去 bootstrap 缺失数据库或创建 intake 行。

### 变更请求

修改、构建、修复类请求授权正常的 Harness 变更闭环：

1. 用 `scripts/bootstrap-harness.sh`（macOS/Linux）或 `.\scripts\bootstrap-harness.ps1`（Windows）bootstrap 本地忽略的运行时。
2. 按 `docs/FEATURE_INTAKE.md` 分类请求，并用 `scripts/bin/harness-cli intake` 记录分类。
3. 用 `scripts/bin/harness-cli query matrix --active --summary` 查看聚焦证明状态；若选定故事，再用 `scripts/bin/harness-cli query matrix --story <id>`。
4. 仅按 `docs/CONTEXT_RULES.md` 中所选车道要求，拉取受影响的产品、故事、决策与实现文件。
5. 在该车道内实现并验证：`tiny`、`normal` 或 `high-risk`。
6. 结束前自问：产品真相、验证期望、架构规则、重复失败模式或下一 Agent 指令是否已变。
7. 用 `scripts/bin/harness-cli trace` 记录 trace，字段深度遵循 `docs/TRACE_SPEC.md` 的 trace 层级，并审阅打印的分数。
8. 若发现 Harness 摩擦，就地修复或用 `scripts/bin/harness-cli backlog add` 记录。

## 故事验证

故事可携带机械证明命令：

```bash
scripts/bin/harness-cli story add --id US-012 --title "Story verification" --lane normal --verify "cargo test --workspace"
scripts/bin/harness-cli story update --id US-012 --verify "cargo test --workspace"
scripts/bin/harness-cli story verify US-012
```

`story verify` 从仓库根运行命令，记录 `last_verified_at` 与 `last_verified_result`，通过退出 0、失败退出 1。当 `trace --story <id>` 关联到从未通过验证命令的故事时，trace 仍会记录，但关闭前打印建议性警告。

合并、成熟度声明与 benchmark 跑前使用 `story verify-all`。它对每个已配置验证命令的故事各跑一次，逐故事打印结果，跳过无 `verify_command` 的故事，任一失败则退出 1。

`story verify` 只接受故事 id。用 `story add --verify` 或 `story update --verify` 配置命令。用 `story update` 记录证明布尔值：`1` 表示是，`0` 表示否。Rust CLI 拒绝 `yes`/`no` 等文本值。

用 `scripts/bin/harness-cli query matrix --numeric` 把证明值抄回 `story update`。默认矩阵输出为人可读的 `yes`/`no`；数值输出与 CLI 输入一致。

用 `query matrix --active --summary` 省略已完成历史与长证据文本，仍保留车道、可运行状态与证明列。`--runnable` 使用与协议故事发现相同的「已计划 / 非空验证 / 未阻塞」规则；`--story <id>` 精确选一条故事。过滤器为 AND 语义。无过滤的矩阵仍是完整持久证明视图。

`story complete <id>` 是已完成工作的显式生命周期转换。它要求故事为 `in_progress` 或 `changed`，跑新鲜证明，仅当证明通过时标为已实现。解析器（resolver）类故事额外要求：稳定关联的 Harness 改进 intake，以及在最新 resolver 链接之后记录的、已完成的匹配实现 trace。通过时，故事证明与符合条件的已接受 backlog 关闭会原子、可回放地提交。普通文本更新与 JSON compare-and-set 更新会拒绝以 `implemented` 为目标，并引导使用 `story complete`；其他生命周期、证明、证据与验证命令更新仍可用。普通 `story verify` 与 `story verify-all` 仍仅为证明。

## Phase 5 演进命令

工具发现：

```bash
scripts/bin/harness-cli query tools --summary
scripts/bin/harness-cli query tools --json
scripts/bin/harness-cli tool register --name <name> --command <cmd> --description <text> --responsibility Verification
```

上下文与漂移检查：

```bash
scripts/bin/harness-cli score-context <trace-id>
scripts/bin/harness-cli audit
```

`score-context` 为建议性：报告上下文规则覆盖，不改动 trace。`audit` 报告漂移类别与熵分，见 `docs/HARNESS_AUDIT.md`。

干预（intervention）与 trace 分离：

```bash
scripts/bin/harness-cli intervention add --trace <id> --type correction --description <text> --source human
scripts/bin/harness-cli query interventions --story US-024
```

当人类、评审者、CI 或其他 Agent 纠正、覆盖、升级或批准工作时，记录干预。

改进提案：

```bash
scripts/bin/harness-cli propose
scripts/bin/harness-cli propose --accept <proposal-key> --outcome-after-traces 20
scripts/bin/harness-cli propose --reject <proposal-key> --reason "Not worth the added complexity"
```

`propose` 根据重复摩擦、干预与审计漂移打印确定性、只读的提案。人类用恰好一个结果日程显式接受一个 key，或带原因拒绝一个 key。旧的批量 `--commit` 路径被拒绝，以免展示中的提案意外变成工作项。

## 决策记录

高风险工作在改变行为或架构时需要持久决策。对鉴权、授权、数据所有权、API 形态、审计/安全或验证变更，两边都要记录：

1. 按 `docs/templates/decision.md` 在 `docs/decisions/` 增加 markdown 文件。
2. 增加或刷新持久记录：

```bash
scripts/bin/harness-cli decision add \
  --id 0008-auth-boundary \
  --title "Auth Boundary" \
  --doc docs/decisions/0008-auth-boundary.md \
  --notes "Accepted during T4 authentication work."
```

trace 的 `--decisions` 字段是有用证据，但不是决策日志。不要把 trace 里的决策文字当作满足持久决策记录要求。

## Harness 变更策略

Agent 可直接更新：

- 非完成态故事状态与证据（`scripts/bin/harness-cli story update`）；到达 `implemented` 用 `story complete`。
- 测试矩阵行（`story add` / `story update`）。
- 故事包到产品文档的链接。
- 验证说明与报告。
- 与当前任务绑定的小澄清。
- intake、trace、backlog（经 `harness-cli`）。

以下变更应先请人类确认：

- 改变架构方向。
- 移除验证要求。
- 改变信源层级。
- 改变风险分类规则。
- 替换功能工作流。

## Git 提交说明（团队约定）

创建 git commit 时，**冒号后的摘要说明与正文一律使用简体中文**，便于人类审查与追溯。

推荐格式：

```text
<type>(<scope>): 中文摘要（一句话）

- 改动原因与影响（中文）
- 未纳入本次提交的范围（如有）
```

约定：

- **type / scope** 可保留英文短标签（如 `feat(harness)`），与常见工具链兼容。
- **冒号 `:` 之后的内容**（含摘要、正文、列表）默认写中文，不要写英文长段落。
- Agent 代用户提交且未指定语言时，按本约定写中文说明。
- 仅当用户明确要求英文提交说明时，才可使用英文正文。

## 完成定义

只读请求完成条件：回答有仓库证据支撑，清楚区分事实与推断，且未改动仓库与 Harness 状态。

变更请求完成条件：

- 请求的变更已完成，或阻塞已文档化。
- 相关文档、故事与测试矩阵条目保持最新。
- **外证已按 [`QUALITY_LOOP.md`](./QUALITY_LOOP.md) 跑过并可贴出**（禁止自评充绿）；模块证明词汇见 [`TEST_MATRIX.md`](./TEST_MATRIX.md)。
- 已用 `scripts/bin/harness-cli trace` 记录 trace。
- 相关时，缺失的 Harness 能力已用 `scripts/bin/harness-cli backlog add` 记录。
- 最终回复说明改了什么、以及未尝试什么。

人类审查总览：[`HARNESS_REVIEW.md`](./HARNESS_REVIEW.md)。

## 验证阶梯（权威在 QUALITY_LOOP）

历史上曾用「未来验证阶梯」占位。**当前权威**为 [`QUALITY_LOOP.md`](./QUALITY_LOOP.md) 的证据阶梯（L0 / L1 / L2）与提交/推送门禁；模块面最小证明见 [`TEST_MATRIX.md`](./TEST_MATRIX.md)。

概念对应（便于读旧文）：

```text
L0 机械     ≈ validate:quick（按触及面现有 lint / 编译 / ci-smoke）
L1 逻辑     ≈ 单元/集成测（本仓以 agent pytest 等为主）
L2 UI/跨层  ≈ e2e / 平台冒烟（本仓以 pnpm dev / compose 主路径手测为准）
```

在对应命令**已实际运行并贴出外证**之前，Agent 不得声称该层已通过。根目录无统一 `type-check` 脚本时，不得虚构全仓 type-check 已绿。