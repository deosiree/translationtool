# 上下文工程规则（Context Engineering Rules）

上下文规则帮助 Agent 决定读什么、何时读、何时停止。`AGENTS.md` 是有界权限入口；它先选定请求类别，再由本文扩展检索。

目标不是最大化上下文，而是把当前任务阶段与风险车道所需的正确信息放进模型。

## 权限门禁

请求类别同时决定变更权限与默认上下文。

| 请求类别 | 示例 | Harness 变更 | 默认上下文 |
| --- | --- | --- | --- |
| 只读 | 回答、解释、评审、诊断、计划、状态 | 无。不要 bootstrap、初始化/迁移、录入 intake、更新持久状态或 trace。 | `AGENTS.md`、请求点名的文件或输出，再加支撑答案所需的最小相邻信源。 |
| 变更 | 修改、构建、修复 | 先 bootstrap，再按所选车道需要做 intake、故事/证明、trace、backlog 变更。 | `AGENTS.md`、`docs/FEATURE_INTAKE.md`、聚焦的活跃矩阵摘要，再按下方车道与触发器拉取。 |

因果：诊断可能发现缺 schema 迁移，但发现本身不授权去创建迁移。随后要求修复该迁移才是变更请求，须先 bootstrap 与 intake。同理，「评审并应用修复」因用户明确要求改仓库，属于变更请求；由请求结果而非单个关键词定权限。

## 上下文阶段

### 分拣阶段（Intake）

仅适用于变更请求。读材料以分类请求、找受影响面、选车道。

| 文档或信源 | Tiny | Normal | High-Risk |
| --- | --- | --- | --- |
| `AGENTS.md` | 必须 | 必须 | 必须 |
| `docs/FEATURE_INTAKE.md` | 必须 | 必须 | 必须 |
| `scripts/bin/harness-cli query matrix --active --summary` | 必须 | 必须 | 必须 |
| `README.md` | 应当 | 必须 | 必须 |
| `docs/HARNESS.md` | 应当 | 必须 | 必须 |
| `docs/ARCHITECTURE.md` | 跳过 | 应当 | 必须 |
| 相关 `docs/product/*` | 无关则跳过 | 产品行为变则必须 | 必须 |
| 相关 `docs/stories/*` | 无关则跳过 | 已有故事则必须 | 必须 |
| `docs/decisions/*` | 跳过 | 触及架构或持久规则时应当 | 必须 |
| `docs/HARNESS_COMPONENTS.md` | 跳过 | Harness 改进时应当 | 可观测或 benchmark 工作时必须 |

### 计划阶段（Planning）

读材料以决定最小安全方案与期望证明。

| 文档或信源 | Tiny | Normal | High-Risk |
| --- | --- | --- | --- |
| 当前要改的文件 | 必须 | 必须 | 必须 |
| `docs/templates/story.md` | 跳过 | 创建/更新故事时必须 | 应当 |
| `docs/templates/high-risk-story/*` | 跳过 | 除非风险升级否则跳过 | 必须 |
| `docs/ARCHITECTURE.md` | 跳过 | 代码或边界变更时应当 | 必须 |
| `docs/TEST_MATRIX.md` 或 `scripts/bin/harness-cli query matrix` | 应当 | 必须 | 必须 |
| 相关决策 | 跳过 | 应当 | 必须 |
| `docs/HARNESS_MATURITY.md` | 跳过 | Harness 改进时应当 | 成熟度或流程变更时必须 |
| `docs/HARNESS_BACKLOG.md` 与 `scripts/bin/harness-cli query backlog` | 跳过 | 摩擦重复时应当 | 改变 Harness 行为时必须 |

### 实现阶段（Implementation）

改动时阅读。本阶段限定在直接影响所选故事的文件。

| 文档或信源 | Tiny | Normal | High-Risk |
| --- | --- | --- | --- |
| 正在改的文件 | 必须 | 必须 | 必须 |
| 同模式的相邻文件 | 应当 | 必须 | 必须 |
| 相关产品文档 | 仅文案则跳过 | 行为变则必须 | 必须 |
| 相关故事包 | 无需故事则跳过 | 必须 | 必须 |
| 相关模板 | 跳过 | 加文档时应当 | 必须 |
| `docs/ARCHITECTURE.md` | 跳过 | 结构变更时应当 | 必须 |
| 提供方/API/安全文档 | 跳过 | 触及则应当 | 必须 |
| 无关文档与历史 trace | 跳过 | 跳过 | 仅当影响决策时应当 |

### 验证阶段（Validation）

读材料以证明变更，避免声称无支撑的完成。

| 文档或信源 | Tiny | Normal | High-Risk |
| --- | --- | --- | --- |
| 故事验收标准 | 应当 | 必须 | 必须 |
| `docs/TEST_MATRIX.md` 或 `scripts/bin/harness-cli query matrix` | 应当 | 必须 | 必须 |
| 故事包验证节 | 无故事则跳过 | 必须 | 必须 |
| `docs/templates/validation-report.md` | 跳过 | 重要证明时应当 | 高风险证明时必须 |
| README/包文档中的相关命令 | 应当 | 必须 | 必须 |
| Benchmark 协议或外部 benchmark 仓库 | 跳过 | 除非被要求否则跳过 | 故事依赖 benchmark 证明时必须 |
| `docs/HARNESS_MATURITY.md` | 跳过 | Harness 改进时应当 | 成熟度声明时必须 |

### Trace 阶段

读材料以便为下一 Agent 与 benchmark 评分留下有用证据。

| 文档或信源 | Tiny | Normal | High-Risk |
| --- | --- | --- | --- |
| `docs/TRACE_SPEC.md` | 应当 | 必须 | 必须 |
| `scripts/bin/harness-cli query matrix` | 应当 | 必须 | 必须 |
| `scripts/bin/harness-cli query backlog` | 跳过 | 发生摩擦时应当 | 必须 |
| `git status --short` 的变更文件列表 | 必须 | 必须 | 必须 |
| 验证命令输出 | 应当 | 必须 | 必须 |
| 故事包或进度日志 | 无故事则跳过 | 必须 | 必须 |
| `docs/HARNESS_COMPONENTS.md` | 跳过 | 归因摩擦时应当 | 需要失败归因时必须 |

## 设计与验收原则（变更请求）

| 原则 | 要求 |
| --- | --- |
| 源头重构 / 剃刀 / 单一职责 | 能用更简单所有权模型消掉 bug 时，重构源头，禁止在错误模型上叠补丁（例：列表与 `a-tag closable` 双主人时，自绘 chip，而非 `preventDefault`+换 key）。规则只留一处 SSOT，禁止前后端各写一套过滤。 |
| 页面验收优先 opencli | 测试环境可用且涉及用户可见 UI 时，自觉设计/运行 opencli（或仓库约定的 browser UX）集成测，不只靠单元测试与口头手工。 |

## 检索触发器

| 触发条件 | 动作 |
| --- | --- |
| 任务触及 **切分词片 / segment_trace.jieba / 回写 term_word** | 规范化只走 `normalize_cn_lexemes`（先保序去重、再滤停用词）；写入 `segment_trace` 必须经此门禁；前端不维护停用词表。 |
| 任务增加/编辑 **SearchBox 查询条件**字段或按钮（术语库/词条管理等） | 宽度只改 `translation/src/components/search/searchBox.vue` 的 `--search-control-width`（或 `searchControlWidth.js`）；**禁止**在 `SearchBox` 的 `form` 插槽内写零散 `style="width: …"`。按钮放 `operate` 插槽（或独立操作行），勿为加按钮改字段宽。见 `docs/superpowers/specs/2026-07-18-searchbox-control-width-design.md`。 |
| 任务做 **术语学习页 / 切分 Tag 编辑** 等 UI 验收 | 环境可用时用 opencli 做页面集成测（删 chip、保存后无顿号/无重复），见原则「页面验收优先 opencli」。 |
| 任务触及数据库 schema、持久记录或迁移 | 计划前阅读 `docs/decisions/0004-sqlite-durable-layer.md`、`scripts/schema/` 与相关 CLI 代码。 |
| 任务 **执行 MySQL DDL/DML/种子**（含 `docker exec … mysql` 写入中文） | **必须**加 `--default-character-set=utf8mb4`：`docker exec -i translation-mysql mysql --default-character-set=utf8mb4 …`。禁止不带字符集参数的 `docker exec … mysql` 写入含中文的 SQL（Windows 主机默认 GBK 会乱码）。 |
| 用户要求 **备份数据库 / 准备回滚点 / 回滚 / 恢复备份**（或本地脏库检查点） | 阅读 `docs/ops/DEV_DB_CHECKPOINT.md`，只跑 skill `db-回滚数据库` 脚本。**禁止** PowerShell 管道/`Set-Content` 写 mysqldump；锁定 `--result-file` + `docker cp` + `verify-dump-encoding`。 |
| 用户要求 **精简术语库 / 删空挂 / 去重 / 中文占比不足删词条 / 后台改数再实跑**（本地非上线） | 阅读 `docs/ops/DEV_DB_CHECKPOINT.md`「本地直改数据 / 术语库精简」：先 `backup`，再跑 `db/opt/cleanup-syk-*.sql`；只软删；中文占比阈值默认 **80%**；禁止主观乱删与生产库操作。 |
| Agent **INSERT/种子创建 `t_task_info` 验数任务**（或用户要求补任务人员） | 阅读 `docs/ops/DEV_DB_CHECKPOINT.md`「本地验数任务：人员字段」：`creator`/`developer`/`entry_auditor`/`translator`/`translation_auditor` **全部填写**（本地默认 `admin`），禁止只写 creator。 |
| Agent **INSERT/种子创建 `t_entry_info` 验数词条**（或 `/taskManage/getTaskPending` 报系统服务异常） | 阅读 `docs/ops/DEV_DB_CHECKPOINT.md`「本地验数词条：entry_state」：进翻译阶段必须 `entry_state=3`；禁止 `0`（新建会触发 `TaskStateEntity` 抛错 → 前端 201）。完整四步流程读 skill `工作台验数播种`（huiyanSkills/translateTool-skills）。 |
| Agent **灌工作台验数**（建任务、挂产品词条、下发进翻译阶段、产品 admin 术语库验数） | 走 skill `工作台验数播种`：分析目标 → 编排就绪；硬约束仍以 `docs/ops/DEV_DB_CHECKPOINT.md` 为准；备份委托 `db-回滚数据库`。 |
| Agent **多检索/多索引验数词条**（exact / fuzzy / decomposed / none，或产品 admin 挂 ADM 矩阵） | 阅读 `docs/ops/DEV_DB_CHECKPOINT.md`「多检索验数矩阵」；用 skill `工作台验数播种` 应用 `db/opt/seed-verify-admin-retrieval.sql`（或 custom）；写完后 `verify_adm_pretranslate --strict` + `verify-workbench-translate-ready.ps1`。禁止只用整库 restore 凑数。 |
| Harness Eval 业务题 **B02** / 工作台播种路由考试 | 阅读 `evals/suites/product/B02-workbench-verify-seed/`；dry：`node evals/scripts/run-question.mjs --question B02-workbench-verify-seed --mode dry --fixture pass`。 |
| 任务触及 CLI 命令行为或安装器分发 | 阅读 `docs/decisions/0005-prebuilt-rust-harness-cli.md`、`scripts/README.md`、相关 `crates/harness-cli/*`、CLI 帮助与安装器文档。 |
| 任务触及鉴权、授权、审计/安全、数据丢失或外部提供方 | 按 high-risk 处理，阅读 `docs/templates/high-risk-story/*`，实现前检查既有决策。 |
| 任务改变公开 API 形态、产品行为或用户可见工作流 | 编辑前阅读相关 `docs/product/*`、故事包与验证期望。 |
| 任务改变 Harness 策略、信源层级、风险分类或验证要求 | 阅读 `docs/HARNESS.md`、`docs/FEATURE_INTAKE.md`、`docs/ARCHITECTURE.md`、`docs/decisions/*`；方向模糊时暂停。 |
| 任务发现重复困惑、过时文档或缺失证明 | 阅读 `docs/HARNESS_BACKLOG.md`，记录 `harness_friction`，修复超出范围时增加 backlog 项。 |
| 任务做成熟度、可观测、trace 质量或 benchmark 声明 | 阅读 `docs/HARNESS_COMPONENTS.md`、`docs/HARNESS_MATURITY.md`、`docs/TRACE_SPEC.md`。 |
| 任务为 normal 或 high-risk 且跨多次迭代 | 在 `docs/stories/` 下创建或更新故事/进度文件并保持最新。 |
| 正在准备变更请求的最终回复 | 记录最终 trace 前重读验证证据、`git status --short` 与 `docs/TRACE_SPEC.md`。 |

## Token 预算指引

| 车道 | 目标 Harness 上下文预算 | 阅读形态 | 理由 |
| --- | --- | --- | --- |
| Tiny | 约 2K tokens | `AGENTS.md`、`docs/FEATURE_INTAKE.md`、聚焦活跃矩阵摘要，以及正在改的确切文件。 | Tiny 工作花在策略上的上下文不应超过编辑本身。 |
| Normal | 约 5K tokens | 分拣文档、相关产品/故事文档、结构变更时的架构、验证期望，结束时的 trace 规范。 | Normal 需要足够上下文以保住契约并记录证明，而不必读完所有历史文件。 |
| High-risk | 约 10K tokens | 完整分拣、架构、相关决策、高风险模板、产品文档、验证文档、trace 规范；Harness 行为变更时再加组件/成熟度文档。 | 高风险工作在实现前需要信源层级、既有决策与证明期望在上下文中。 |

预算规则：

- 优先针对性 `rg` 搜索，避免整批通读。
- 只读能回答当前阶段问题的最小小节。
- 检索触发器命中时再扩大上下文。
- 车道、受影响文件与验证路径已清晰后，不要继续读无关历史。

## 有界检索行为

不要预加载全部 Harness 文档。只读请求在答案有支撑后停止。变更请求由 `AGENTS.md` 指向分拣与聚焦矩阵摘要；本文仅在车道、阶段或检索触发器要求时扩展上下文。

## 评审清单

变更请求实现前：

- 已按 `docs/FEATURE_INTAKE.md` 选定车道。
- 已识别相关产品文档或故事包。
- 任何高风险触发器已处理。

变更请求最终回复前：

- 已阅读验证证据。
- normal/high-risk 任务已阅读 `docs/TRACE_SPEC.md`。
- 最终 trace 包含已读文件、已改文件、结果，以及适用时的摩擦。
