# 质量 Loop（外证门禁）

产品代码与 harness 文档的变更，在**声称完成（DONE）之前**须走本 Loop。目标不是「再跑一次检查」，而是：**没有 admissible 外证就不能 DONE**（Proof-or-stop）。

人类审查入口：[`HARNESS_REVIEW.md`](./HARNESS_REVIEW.md)。模块证明词汇见 [`TEST_MATRIX.md`](./TEST_MATRIX.md)。

## 1. 何时触发

- 变更请求已写入代码、产品文档或 harness 策略（含 AGENTS / intake / ADR / Eval）。
- bug 修复、新能力开发、验收收尾。
- 只读问答 / 纯计划：**不**跑本 Loop。

## 2. 证据阶梯（先判最高层）

先判断本任务需要的**最高**证据层，再跑该层及以下。

| 层 | 何时 | 本仓手段 |
| --- | --- | --- |
| **L0** 机械 | 任何变更 | 按触及面跑快速检查（见下节门禁）。改 `AGENTS.md` / `FEATURE_INTAKE` / ADR / Eval → **必须** `node evals/scripts/ci-smoke.mjs`。失败先修，再谈更高层。 |
| **L1** 逻辑 | 领域规则、API、状态机、校验 | `terminology-agent/`：相关 `pytest`。前端若有针对改动的测 → 跑之。**没有**对应测 → **先补最小测再声称 L1**，禁止「没测但 L0 过了」。Java 维护面：编译通过 + 触及接口手测（见 TEST_MATRIX）。 |
| **L2** UI / 跨层 | 多步流程、权限、登录、跨 UI↔后端冒烟 | 按 [`TEST_MATRIX.md`](./TEST_MATRIX.md)：`pnpm dev` 或 `docker compose` 后走主路径手测；弱证明区加强清单。**禁止**用他仓脚本路径冒充本仓 L2。 |

根目录 `package.json` **没有**统一的 `type-check` / 全仓 `lint`；门禁按**触及面现有命令**执行，不虚构根脚本。

## 3. 对抗审查与停条件

### 3.1 对抗审查（独立审查）

以下任一成立时，DONE 前须做**独立 diff 对抗审查**（另一 subagent / 人工 code-review；brief 写清易失败模式）：

- 车道为 **normal** 或 **high-risk**；或
- 证据层达到 **L2**

**tiny** 且仅 L0/L1：可免对抗审查。

### 3.2 停条件（视觉标记必遵）

```text
Goal 全绿 → 退出并可声称 DONE
失败且同因次数 < 3 → 缩小修复面，再跑对应证据层
连续 ≥3 次同一失败（同证据层） → 🛑 STOP，问人
歧义 / 越权 / 未确认须动 Java 维护面 → 🔴 CHECKPOINT，不在本 Loop 硬闯
只读请求被误当成变更闭环 → 🛑 STOP，改回只读，不 bootstrap / intake / trace
```

**🔴 CHECKPOINT**：未确认 `backend=java-maintain`、跨模块契约、或删验证要求前，禁止继续改代码。

**🛑 STOP**：同因连败 ≥3，或用户未授权变更却要写仓库——立即停，等人。

### 3.3 失败分支（三段式）

| 触发条件 | 一线修复 | 仍失败兜底 |
| --- | --- | --- |
| L0 命令失败（lint / 编译 / ci-smoke） | 修本仓错误后重跑同一命令；禁止改产物充绿 | 同因 ≥3 → 🛑 STOP 问人 |
| 无对应测却要声称 L1 | 先补最小测再跑；禁止「L0 过了就算 L1」 | 🔴 CHECKPOINT：缩小范围或降为仅 L0 并说明缺口 |
| L2 手测路径不清 | 按 [`TEST_MATRIX.md`](./TEST_MATRIX.md) 模块行重写步骤 | 无本仓手段 → 🔴 CHECKPOINT，禁止抄他仓脚本路径 |
| normal/L2 未做对抗审查就要 DONE | 补独立 diff 审查后再 DONE | 用户明确豁免 → 记入 trace；否则 🛑 不得 DONE |
| 同因失败已累计 ≥3 | 停止改代码 | 🛑 STOP，带失败摘要问人 |
| 误把只读当变更 | 不 bootstrap / intake / trace | 🛑 STOP，改回只读答复 |

### 3.4 外证要求

声称 DONE 时须贴出命令与结果摘要（含 exit / pass 证据）。禁止自评「应该过了」充绿；禁止改构建产物、忽略失败、或只改测试期望来充绿。

## 4. 提交 / 推送门禁（与 DONE 分开）

与 §2 **L0** 分开：用户明确要求 **commit** / **push** 时，Agent 代提交前还须跑本节；**不是**每次 DONE 都强制全仓 lint。

根目录无统一 lint 脚本；按触及面：

| 动作 | 触及面 | 须跑 |
| --- | --- | --- |
| commit | `translation/` | `pnpm -C translation lint` |
| commit | `terminology-agent/` | 相关 `pytest`（至少改动相关用例） |
| commit | `translationtoolservice/` | 环境允许时 `mvn -f translationtoolservice package`（或等价编译） |
| commit | AGENTS / intake / ADR / Eval / harness 核心 docs | `node evals/scripts/ci-smoke.mjs` |
| push | 在 commit 门禁之上 | 触及 UI 时再加 `pnpm -C translation build`（环境允许）；触及 Java 时再确认编译门禁已过 |

多面同时改动：合并上表，逐面跑齐。

## 5. 失败入库路径

同因复发不要只修个案，按落点写入系统：

| 落点 | 何时 |
| --- | --- |
| [`IMPROVEMENT_PROTOCOL.md`](./IMPROVEMENT_PROTOCOL.md) + `harness-cli propose` | 重复摩擦、审计漂移 → 改进提案 |
| [`HARNESS_BACKLOG.md`](./HARNESS_BACKLOG.md) / `harness-cli backlog` | 已接受的 harness 能力缺口 |
| [`decisions/`](./decisions/) | 架构 / 契约 / 持久规则取舍 |
| [`../evals/`](../evals/) | 工作流行为须可回归的考试题 |

## 6. 车道与证据强度（简表）

| 车道 | 最低证据 | 对抗审查 |
| --- | --- | --- |
| tiny | L0；触及逻辑则 L1 | 免 |
| normal | 按任务最高层（常 L1，跨层则 L2） | 要 |
| high-risk | 故事 `validation.md` + 对应层外证 | 要 |

## 7. 反例黑名单（不要做）

| 不要做 | 要做 |
| --- | --- |
| 自评「应该过了」宣称 DONE | 贴命令 + exit/pass 摘要 |
| 虚构根目录全仓 `type-check` / 全仓 lint | 按触及面现有命令 |
| 用他仓 L2 脚本路径冒充本仓证明 | 只用 TEST_MATRIX / 本仓 `pnpm dev` 等 |
| 领域 skill 压过双后端 / 旁路目录硬约束 | 宪法优先，再调 skill |
| 只修个案、同因复发不入库 | 走 §5 失败入库 |
