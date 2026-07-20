# 文档索引

本目录是 **Translation Tool** 仓库的人类与 Agent 共用说明：业务架构、协作约定（Harness）、决策与故事包。

先读根目录 `AGENTS.md` 与本文件，再按任务打开下列文档。

## 核心文件

| 文件 | 用途 |
| --- | --- |
| `HARNESS.md` | 人与 Agent 如何协作；含语言约定与 Git 提交说明（简体中文） |
| `FEATURE_INTAKE.md` | 需求分拣：tiny / normal / 高风险，以及 backend 落点 |
| `ARCHITECTURE.md` | 业务模块边界：前端、Java 维护面、Python 新后端 |
| `TEST_MATRIX.md` | 历史证明矩阵说明；当前状态用 `harness-cli query matrix` 查询 |
| `HARNESS_BACKLOG.md` | 历史改进清单说明；当前记录用 `harness-cli backlog` |
| `HARNESS_MATURITY.md` | Harness 成熟度（H0–H5）与当前进度 |
| `GLOSSARY.md` | 共享术语 |
| `contracts/` | 可选的机器可读契约（给外部编排器用） |

## 目录

| 目录 | 用途 |
| --- | --- |
| `product/` | 产品侧契约（有消费者规格后再填） |
| `stories/` | 功能包与 backlog |
| `decisions/` | 持久决策与取舍（改契约前先查） |
| `demo/` | Harness 从输入到可执行工作的示例走查 |
| `templates/` | 规格 intake、story、计划、决策、验收等模板 |
| `superpowers/` | 设计稿与实施计划（如 Harness Eval） |

## Harness Eval（考试与回归）

可回归的工作流评测在仓库根目录 `evals/`，入口见 `evals/README.md`。

- 本地一键 smoke：`node evals/scripts/ci-smoke.mjs`
- 人与 AI 怎么配合：`evals/docs/operator-playbook.md`
- CI：`.github/workflows/harness-eval-smoke.yml`

## 本仓库当前状态

- **产品**：词条翻译管理平台；双后端（Java 维护面 + Python 新能力默认落点）。权威说明见 `ARCHITECTURE.md` 与 `decisions/0009-*.md`。
- **Harness**：已落地 intake、story、trace、评分与 backlog CLI；Eval dry 套件与 `workflow_tree_hash` 基线已接入 CI smoke。给人看的 Harness 文案统一简体中文（决策 `0011-harness-human-docs-zh`）；CLI 参数与机器契约标识保留英文。
- **变更 intake**：本仓库的变更 intake 流程已通过 Harness 分类（变更请求 / tiny 车道）验证。

上游 Harness 模板仓库另有通用 CLI/安装器说明；**不要**据此假设本业务仓已具备与模板 demo 相同的应用栈或部署流水线——以本仓库 `ARCHITECTURE.md` 与模块 README 为准。
