# Harness Eval 设计：可回归的工作流考试体系

Date: 2026-07-16  
Status: Accepted — implementation in progress (`evals/`)
Goal: D（两期）— 先协议题补齐 H3/H5，再业务题覆盖 translationtool 真实场景

## 1. 问题与原则

### 问题

本仓库已有 durable harness（intake / story / trace / `score-trace` / `audit` / `propose` / `story verify`），但缺少对「改 AGENTS.md / rules / skills / CONTEXT_RULES 之后，Agent 行为是否变好」的可回归度量。H3/H5 在 `docs/HARNESS_MATURITY.md` 中明确卡在：**无 component-level benchmark attribution、无重复 benchmark 证明 propose 闭环有效**。

现状是运营观测层；缺的是行为考试层。

### 原则（继承腾讯 Harness Eval 方法论，不绑定其未开源实现）

1. **可重复 > 精确**：同一题多次跑，看分布与趋势，不迷信单次满分。
2. **可归因 > 高分**：失败必须标 `[workflow]` / `[eval]` / `[capability]`。
3. **闭环 > 成绩单**：分数写入历史，并驱动 backlog / `propose`，不只出报告。
4. **考试 ≠ 产品单测**：`story verify` 继续验产品证明；Eval 验 harness 行为质量。

### 非目标（YAGNI）

- 不复刻腾讯 Go CLI 全量（worktree symlink 编排可后期再加）。
- 不引入 Braintrust 等 SaaS 作为一期硬依赖。
- 不做完整 Examiner 多轮 LLM 用户模拟（一期用固定剧本 + 有限追问模板；二期再增强）。
- 不把 Java 遗留后端纳入新业务题默认落点（仍遵守双后端策略）。

## 2. 总体架构

```text
evals/
  suites/protocol/          # Phase 1 协议题
  suites/product/           # Phase 2 业务题（后期）
  fixtures/                 # 沙箱仓库切片 / 变量
  rubrics/                  # 可复用判分片段（可选）
  scripts/                  # 跑题、压缩 transcript、落库桥接
promptfooconfig*.yaml       # promptfoo 入口
runs/                       # gitignored：每次 run 产物
harness.db                  # 可选：eval_run / eval_score 表或先写 YAML 再导入
```

角色：

| 角色 | 一期实现 | 说明 |
| --- | --- | --- |
| 题库 | Markdown + YAML（文章四件套） | `meta.yaml` / `task.md` / `rubric.md` / `env.yaml` |
| 考生 | Cursor Agent 或 Claude Code headless（可配置） | 在隔离目录执行 `task.md` 用户可见题面 |
| 考官 | 轻量：固定开场 + 可选脚本化追问 | 一期不做独立 Examiner LLM；需要时用 promptfoo 多轮 messages |
| 裁判 | promptfoo assertions + LLM-as-judge | 硬性项确定性；质量项 rubric；独立于答题上下文 |
| 引擎 | promptfoo | CI 友好、MIT、轨迹断言成熟 |
| 闭环 | `workflow_rev` + score history → backlog/`propose` | 补齐 H3/H5 证据链 |

与现有 harness 边界：

- **不替代** `story verify` / TEST_MATRIX（产品证明）。
- **扩展** observability：benchmark 结果可归因到 `docs/HARNESS_COMPONENTS.md` 责任面。
- **喂养** improvement loop：`[workflow]` 建议 → backlog；`[eval]` → 改题；`[capability]` → 记录但不假装靠改规则能修。

### 语言约定（人工审查）

与 `docs/decisions/0011-harness-human-docs-zh.md`、`docs/HARNESS.md`「语言约定」一致：

- **中文**：`task.md`、`rubric.md`、live prompt、脚本行注释、`review.md` / 批次汇总、改进建议；以及 Harness 运维文档与模板给人看的正文。
- **英文保留**：脚本标识符、`env.yaml` 的 `type` 字段、题目 ID、git 路径、`harness-cli` 参数——便于解析与 CI。

## 3. 题库格式

每道题一个目录：`evals/suites/<suite>/<question-id>/`

```text
meta.yaml      # id, version, suite, wave, difficulty, component, purpose
task.md        # 考生可见题面（可含「考官剧本」仅给 runner，不对考生暴露 rubric）
rubric.md      # 硬性通过项 / 质量项 / 典型失分
env.yaml       # checks、所需文件、占位符变量、隔离策略
fixtures/      # 可选
```

约定：

- 考生只看到渲染后的 `task.md`；永远看不到 `rubric.md`。
- 路径/服务名/需求 ID 用占位符；`evals/fixtures/vars.<project>.yaml` 注入。
- `meta.component` 必须映射到 `HARNESS_COMPONENTS.md` 责任之一（或 `product:<area>` 业务标签）。

## 4. 评分模型

每道题每次 run 产出：

**score.yaml（结构化）**

```yaml
question_id: protocol-01-readonly-gate
workflow_rev: <git sha of HEAD; also record workflow_tree_hash of harness-relevant paths>
result: pass | fail
compliance: 0-5          # 流程遵循度
execution_quality: 0-5   # 执行/交付质量
overall: 0-5
agent: cursor-agent|claude
n_trials: 1              # 同 batch 内该题重复次数的序号
summary: "..."
```

**review.md（证据 + 改进）**

- `evidence[]`：必须引用 transcript / 文件系统证据摘要。
- `improvements[]`：每条前缀 `[workflow]|[eval]|[capability]`。

硬性项失败 → `result: fail`，不论质量分高低。

批次汇总：

- `runs/<batch-id>/latest.md` + `latest-stats.yaml`
- 追加 `evals/history/score-history.yaml`（或后续导入 SQLite）
- `batch-insights.md`：按三标签聚类

## 5. Phase 1 — 协议题（关闭 H3 缺口）

目标：固定题集可重复跑，分数可按 `workflow_rev` 对比，失败可归因到 harness 组件。

### Wave 1 — 主干闭环（首批必做，建议 5 题）

| ID | 考察点 | 硬性证据示例 | component |
| --- | --- | --- | --- |
| P01 | 只读请求不 bootstrap / 不写 intake/trace | 无 `harness.db` 新建或无 intake 行增加 | Permissions / Task specification |
| P02 | 变更请求走 intake + 正确 lane | 存在 intake 记录且 type/lane 合理 | Task specification |
| P03 | 上下文按 `CONTEXT_RULES` 收敛 | 未整库乱读；关键 docs 被读 | Context selection |
| P04 | 关闭前 `trace` + 可接受 score-trace | 有 trace；字段达 tier | Observability |
| P05 | 新功能默认落 Python，不擅自堆 Java | 变更路径在 `terminology-agent/` 或明确 java-maintain | Task specification / Permissions |

### Wave 2+（Phase 1 后期）

- 状态机/门禁：高风险需确认、范围锁定。
- `story verify` / `verify-all` 使用正确。
- friction → backlog predicted/outcome 字段习惯。
- `propose` 只读 vs accept 边界。

### Phase 1 成功标准

- 同一 `workflow_rev` 下协议套件连续 3 个 batch，通过率标准差可报告（文档化，不要求统计检验）。
- 至少一次故意退步实验：弱化某条 AGENTS 规则后，对应题分数下降且归因到正确 component。
- `HARNESS_MATURITY.md` 可据此将 H3 标为 Achieved（或附 benchmark 报告链接）。

## 6. Phase 2 — 业务题（translationtool）

在协议套件稳定后启用 `evals/suites/product/`。

波次建议：

1. Intake 分拣：frontend vs python vs java-maintain。
2. 窄前端修复（如文案/校验）不碰后端。
3. Python agent 小改 + 对应测试期望。
4. 明确拒绝：把新业务塞进 Java 的诱导题（韧性）。
5. （可选）回填/文件管理相关 UI 任务 — 用 fixture，避免依赖完整本地栈时降级为「计划+落点正确」题。

业务题复用同一引擎与评分模型；`vars.translationtool.yaml` 提供模块路径与端口契约。

## 7. 运行时与 CI

### 本地

```bash
# 示例（实现计划中固化确切命令）
npx promptfoo eval -c evals/promptfooconfig.protocol.yaml
node evals/scripts/ingest-scores.js --batch <id> --workflow-rev $(git rev-parse HEAD)
```

隔离：

- 一期：每题 `git worktree` 或临时目录拷贝 `evals/fixtures/sandbox-protocol`（最小假仓库含 AGENTS/Harness 文档子集）。
- 完整 monorepo 考试仅用于标明 `env.requires_full_repo: true` 的题。

### CI（建议，非一期阻塞）

- Path filter：`AGENTS.md`、`docs/HARNESS*.md`、`docs/FEATURE_INTAKE.md`、`docs/CONTEXT_RULES.md`、`.cursor/**`、`evals/**`。
- Job：跑协议 smoke 子集（成本可控的 2–3 题）或 nightly 全量。
- 门禁语义借鉴 SkillCI：相对上次 `workflow_rev` baseline，若核心题 overall 下降超过阈值则 fail（阈值写在 config，默认 1.0 分）。

### Agent 调用

- 抽象为 `evals/scripts/run-candidate.sh`，通过环境变量 `EVAL_AGENT=cursor|claude` 切换。
- **默认一期适配器：Claude Code headless**（`claude -p`）；Cursor CLI 作为可选第二适配器（SkillCI 亦标为 best-effort）。
- Transient 失败：对话最多重试 2 次；判卷最多 3 次；超时不重试。

### `workflow_rev` 与 `workflow_tree_hash`

- `workflow_rev`：整仓 `git rev-parse HEAD`（便于对照日常提交）。
- `workflow_tree_hash`：仅对 harness 相关路径做 `git rev-parse HEAD:<path>` 或等价 tree hash 汇总，路径集合固定为：
  - `AGENTS.md`
  - `docs/HARNESS.md`、`docs/HARNESS_*.md`、`docs/FEATURE_INTAKE.md`、`docs/CONTEXT_RULES.md`、`docs/TRACE_SPEC.md`、`docs/IMPROVEMENT_PROTOCOL.md`
  - `scripts/agent-harness-block.md`、`scripts/claude-harness-block.md`（若存在）
  - `.cursor/rules/**`、`.cursor/skills/**`（若存在）
- 对比「改规则是否退步」时以 `workflow_tree_hash` 为主；`workflow_rev` 用于追溯完整提交。

## 8. 与 harness-cli 的对接

一期最小对接（避免大改 Rust CLI）：

1. 分数写入 `evals/history/score-history.yaml`（版本控制可选：只提交汇总，不提交原始 transcript）。
2. `docs/HARNESS_MATURITY.md` / `HARNESS_COMPONENTS.md` 增加「Benchmark 入口」指向本设计与最新报告。
3. 批量 insights 中 `[workflow]` 项人工或脚本转为 `harness-cli backlog add --predicted ...`。

二期可选：

- SQLite 表 `eval_run` / `eval_score` + `harness-cli query evals`。
- `propose` 规则增加「benchmark regression」证据类。

## 9. 依赖选型冻结

| 选用 | 理由 |
| --- | --- |
| promptfoo | 成熟、本地优先、CI、trajectory/LLM judge |
| 文章四件套题库 | 出题门槛低、题面与阅卷分离 |
| 不整仓依赖 SkillCI/agenteval | 过新、Cursor 支持不稳定；只借鉴 baseline/candidate 门禁语义 |
| 不依赖腾讯闭源 CLI | 不可用 |

## 10. 实施切片（供后续 writing-plans）

1. 脚手架：`evals/` 目录、gitignore `runs/`、promptfoo 最小 config、1 道 P01 端到端通跑。
2. 补齐 Wave 1 五题 + Judge prompt + score/review 模板。
3. history + batch-insights + 故意退步实验文档。
4. CI smoke（可选）+ maturity 文档更新。
5. Phase 2 第一道业务题（分拣落点）。

每片可独立合并；片 1 为垂直切片。

## 11. 风险与缓解

| 风险 | 缓解 |
| --- | --- |
| Headless agent 不稳定 / 贵 | 协议题用最小 sandbox；CI 只跑子集；本地可 `--dry-fixture` 用录制 transcript 测 Judge |
| Judge 漂移 | 固定 judge 模型与 prompt 版本哈希；硬性项尽量确定性 |
| 题面歧义导致假失败 | `[eval]` 归因强制；坏题优先修题不修 workflow |
| 与产品测试混淆 | 文档与目录隔离；命名 `evals/` 不用 `tests/` |

## 12. 验收清单

- [ ] 协议 Wave 1 五题可一键跑并产出 score + review。
- [ ] `score-history` 含 `workflow_rev`，可对比两版 harness。
- [ ] 故意弱化规则后对应题退步可观测。
- [ ] improvements 三标签齐全；至少一条进入 backlog 闭环演示。
- [ ] `HARNESS_MATURITY` H3（及通向 H5 的证据路径）文档更新。
- [ ] Phase 2 至少 1 道业务题模板就位（可后合并）。
