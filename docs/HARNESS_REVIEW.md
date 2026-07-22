# Harness 审查导览（人类入口）

本页是**认识并审查本仓 Harness** 的入口：五模块权威文件、历史怎么查、心智放哪、新心智怎么写入。

Agent 日常仍从根目录 [`../AGENTS.md`](../AGENTS.md) 与 [`FEATURE_INTAKE.md`](./FEATURE_INTAKE.md) 起步；人类要「总览一眼 / 改一处」时优先打开本页。

## 1. 五模块权威图

| 模块 | 权威文件 | 旁证 |
| --- | --- | --- |
| **Task**（任务与分拣） | [`../AGENTS.md`](../AGENTS.md)、[`FEATURE_INTAKE.md`](./FEATURE_INTAKE.md) | [`templates/`](./templates/)、[`stories/`](./stories/)、`harness-cli intake` / `story` |
| **Environment**（可操作环境） | [`ARCHITECTURE.md`](./ARCHITECTURE.md)；模块 README（`translation/`、`terminology-agent/README.md`） | 源码目录；Eval 沙盒 [`../evals/fixtures/`](../evals/fixtures/) |
| **Tools**（工具接口） | [`TOOL_REGISTRY.md`](./TOOL_REGISTRY.md) | `scripts/bin/harness-cli`；领域 skills 见 `huiyanSkills/translateTool-skills`（外部）；检索何时读何文档见 [`CONTEXT_RULES.md`](./CONTEXT_RULES.md) |
| **Trace**（执行记录） | [`TRACE_SPEC.md`](./TRACE_SPEC.md) | `harness-cli trace`；Eval 每题 [`../evals/runs/`](../evals/runs/) 下 `transcript.txt` |
| **Grader**（评分与回归） | [`../evals/README.md`](../evals/README.md) | 题库 [`../evals/suites/`](../evals/suites/)；可用性对照 [`../evals/darwin-harness-usability/`](../evals/darwin-harness-usability/)；历史 [`../evals/history/score-history.yaml`](../evals/history/score-history.yaml) |
| **质量 Loop**（产品 DONE 外证） | [`QUALITY_LOOP.md`](./QUALITY_LOOP.md) | 证据阶梯、对抗审查、停条件；[`TEST_MATRIX.md`](./TEST_MATRIX.md) |

组件分类法另一视角：[`HARNESS_COMPONENTS.md`](./HARNESS_COMPONENTS.md)。协作细则：[`HARNESS.md`](./HARNESS.md)。

## 2. 历史与回归怎么查

改 Harness 宪法类文件后，默认跑：

```powershell
cd F:\Documents\Repertory\Sieyuan\translationtool
node evals/scripts/ci-smoke.mjs
```

| 要找什么 | 去哪 |
| --- | --- |
| 某次冒烟批次 / 基线对比 | [`../evals/runs/`](../evals/runs/)；`compare-baseline.mjs` 输出 |
| 单题答卷与阅卷 | 同目录下 `transcript.txt`、`review.md`、`score.yaml` |
| 分数 ingest 轨迹 | [`../evals/history/score-history.yaml`](../evals/history/score-history.yaml) |
| 人与 AI 怎么配合跑题 | [`../evals/docs/operator-playbook.md`](../evals/docs/operator-playbook.md) |

改 `AGENTS.md` / `FEATURE_INTAKE` / ADR / Eval 协议后：**先 smoke**，失败禁止声称「文档已升级完成」。

**🔴 CHECKPOINT**：`ci-smoke` 未绿时，不得关闭 Harness 文档升级任务。  
**🛑 STOP**：为让 smoke 变绿而改 `evals/history` 充数或删题——禁止；应修题/夹具或修文档。

## 3. 心智放哪

| 问题类型 | 写入处 |
| --- | --- |
| 模块边界 / 端口 / 双后端 / 主域排查 | [`ARCHITECTURE.md`](./ARCHITECTURE.md)、[`../AGENTS.md`](../AGENTS.md)、[`FEATURE_INTAKE.md`](./FEATURE_INTAKE.md) |
| API 字段与路径真相源 | [`API_CONTRACTS.md`](./API_CONTRACTS.md) |
| 产品行为契约 | [`product/`](./product/)、相关 [`stories/`](./stories/) |
| 持久取舍（ADR） | [`decisions/`](./decisions/) |
| 领域可复用操作（备份库、工作台播种等） | 外部 `huiyanSkills/translateTool-skills`（不得压过本仓宪法） |
| 工作流是否变坏 | [`../evals/`](../evals/) |
| 产品变更如何证 DONE | [`QUALITY_LOOP.md`](./QUALITY_LOOP.md) |
| 术语与语言约定 | [`GLOSSARY.md`](./GLOSSARY.md)、本目录 [`README.md`](./README.md) |

## 4. 新心智怎么写入 Harness

```text
用一句话说清「为什么」
  → 默认范围 / 硬约束 → 更新 AGENTS 或 FEATURE_INTAKE 模块分拣
  → 架构 / 契约 / 持久 → 新建或修订 ADR（templates/decision.md）+ 必要时 story
  → 可复用操作流程 → 外部 translateTool-skills（勿把业务特例塞进宪法）
  → 完成证明 / 验证手段 → QUALITY_LOOP.md；更新 TEST_MATRIX 模块行
  → 要可回归 → 加/改 evals 题（并跑 ci-smoke）
写完后：回本页「心智放哪」确认链接可点；宪法面变更跑 ci-smoke
```

模板：[`templates/decision.md`](./templates/decision.md)、[`templates/story.md`](./templates/story.md)。

## 5. 人类快检清单

- [ ] 五模块表都能点到本仓权威文件
- [ ] 主域与排查分层在 ARCHITECTURE / AGENTS 有据，不靠对话口头约定
- [ ] 改过 AGENTS / FEATURE_INTAKE / ADR / Eval → 已跑 `ci-smoke`
- [ ] 领域 skill 未压过宪法（双后端策略、旁路目录、密钥不入库）
- [ ] 产品变更 DONE 标准与 [`QUALITY_LOOP.md`](./QUALITY_LOOP.md) 一致
