# 功能分拣（Feature Intake）

本分拣门禁适用于在改代码或持久 Harness 状态之前的修改、构建、修复类请求。新的项目规格也经此门禁，再变成产品文档、故事或实现工作。

回答、解释、评审、诊断、计划与状态类请求保持只读。它们不 bootstrap 或初始化 Harness、不录入 intake、不更新故事或 backlog、不记录 trace。若用户随后要求实现某项提案，该新变更请求再进入本门禁。

人类不必自行分类风险；由 Harness 分类。

## 分拣流程

```text
用户提示
    |
    v
分类输入类型
    |
    v
重述为工作项
    |
    v
定位受影响的产品文档与故事
    |
    v
跑风险检查清单
    |
    v
选择车道：tiny、normal 或 high-risk
```

## 输入类型

先用输入类型决定工作落点，再选风险车道。

| 类型 | 何时使用 | 典型产物 |
| --- | --- | --- |
| 新规格（New spec） | 把用户提供的项目规格变成 Harness 可用文档 | 产品文档、候选史诗、决策 |
| 规格切片（Spec slice） | 实现已接受规格中的选定行为 | 故事包 |
| 变更请求（Change request） | 变更、修复或细化已接受行为 | 故事包或直接补丁 |
| 新举措（New initiative） | 增加需要多条故事的更大产品域 | 举措说明 + 故事包 |
| 维护请求（Maintenance request） | 变更技术、运维或依赖行为 | 故事包、验证报告或决策 |
| Harness 改进（Harness improvement） | 改进人与 Agent 的协作方式 | 直接改文档，或 `scripts/bin/harness-cli backlog add` |

分拣后默认不要创建或扩展巨型规格。以产品文档、故事、决策与举措说明作为活表面。

## 车道

### Tiny

用于低风险文档、文案、命名或窄范围编辑。

也用于初始项目搭建：仅限于安装已声明依赖、接线服务入口、增加健康/冒烟端点，或打开本地开发库连接，且不创建领域 schema、CRUD、鉴权、授权、提供方集成或数据迁移。新 benchmark 或脚手架项目中的健康端点是冒烟证明，本身不构成公开契约升级。

要求：

- 实现前录入 intake 行；tiny 工作跳过故事包开销，但不跳过持久任务分类。
- 直接打补丁。
- 保持受影响文档最新。
- 运行可用的快速检查。
- 仅在发现摩擦时更新 Harness。

### Normal

用于爆炸半径有界的故事级行为。

要求：

- 按 `docs/templates/story.md` 创建或更新一份故事文件。
- 链接相关产品文档。
- 增加或更新验证期望。
- 在已有实现时做最小垂直切片。
- 用 `scripts/bin/harness-cli story add` 与 `scripts/bin/harness-cli story update` 记录或更新证明状态。

### High-Risk

当工作可能影响安全、数据、范围、契约或多角色/多平台时使用。

要求：

- 用 `docs/templates/high-risk-story/` 创建故事文件夹。
- 填写 `execplan.md`、`overview.md`、`design.md`、`validation.md`。
- 方向模糊时，实现前请人类确认。
- 当行为、架构、授权、数据所有权、API 形态或验证要求有意义地变更时，记录持久决策：按 `docs/templates/decision.md` 写 `docs/decisions/NNNN-*.md`，再用 `scripts/bin/harness-cli decision add` 增加或刷新持久行。trace 中的决策文字不算持久决策记录。

## 风险检查清单

对适用项各标一个旗标：

| 风险旗标 | 触及以下内容时勾选 |
| --- | --- |
| Auth | 登录、登出、会话、JWT、密码、刷新令牌 |
| Authorization | 角色、权限、租户或公司范围 |
| Data model | schema、迁移、唯一性、删除、留存 |
| Audit/security | 审计日志、隐私、敏感数据、访问日志 |
| External systems | 邮件、支付、云服务、提供方 SDK、队列、webhook |
| Public contracts | API 形态、响应信封、客户端可见行为 |
| Cross-platform | 桌面/移动/浏览器分叉、原生壳行为、深链 |
| Existing behavior | 已实现或已被测试覆盖的行为变更 |
| Weak proof | 受影响区域测试不清或缺失 |
| Multi-domain | 同时变更多个产品域 |

## 分类

```text
0-1 个旗标：
  tiny 或 normal，按代码影响决定

2-3 个旗标：
  normal，并加强验证

4+ 个旗标：
  high-risk

任一硬门禁：
  high-risk，除非人类显式收窄范围
```

硬门禁：

- Auth。
- Authorization。
- 数据丢失或迁移。
- Audit/security。
- 外部提供方行为。
- 移除或削弱验证要求。

## 输出

分拣结束时，Agent 应能说出：

```text
Lane: normal
Reason: touches authorization, API contract, and audit behavior.
Docs: permissions, account-settings, audit-log.
Story: docs/stories/epics/E02-access-control/US-014-manager-updates-role.md.
Validation: unit, integration, E2E.
```

## Translation Tool — 模块分拣（本仓库）

在通用车道（tiny / normal / high-risk）之外，**必须先标模块面与后端面**：

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

任一项成立时，至少按 **normal**；同时触及 Auth / Data model / Public contracts 则按上文硬门禁升为 **high-risk**。勾选 **Java touch** 时，输出中必须说明「为何不能只改 Python」。
