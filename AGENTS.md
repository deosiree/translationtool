# Agent Instructions

## 本仓库速览（先读这里）

企业级多语种词条翻译管理平台（Translation Tool）。**双后端**全栈仓库：遗留 Java + 新 Python 后端并存，改代码前先确认落点模块。

| 模块 | 目录 | 角色 | 技术栈 | 默认端口 |
| --- | --- | --- | --- | --- |
| 前端 / Electron UI | `translation/` | UI | Vue 3 + Ant Design Vue + Electron + Webpack | `18000` |
| 遗留后端（Java） | `translationtoolservice/` | **维护面**（他人主责） | Spring Boot 2.7.7 + MyBatis-Plus + Druid | `18001` |
| 新后端 / Agent（Python） | `terminology-agent/` | **新功能默认落点** | Python FastAPI + LangGraph（Python ≥ 3.11） | `18002` |

配套：MySQL 8 / Redis 7（`docker-compose.yml`）；本地一键见根目录 `pnpm dev*` 与 `README.md`。决策见 `docs/decisions/0009-python-backend-prefer-java-maintain.md`。

### 开工必读

1. 本文件 + `docs/FEATURE_INTAKE.md`（工作分拣）
2. `docs/ARCHITECTURE.md`（模块边界与调用关系）
3. `docs/TEST_MATRIX.md`（怎么算做完）
4. `docs/decisions/`（历史决策，改契约前先查）
5. 模块内 README：`translation/`、`terminology-agent/README.md`；Java 仅在维护任务时读 `translationtoolservice/`

### 硬约束

- **双后端策略（最高优先级）**：
  - **新需求 / 新能力 → 默认走 Python**（`terminology-agent/`），可不动 Java 就不动。
  - **Java（`translationtoolservice/`）仅用于遗留后端的维护、修 bug、不得不改的兼容**；他人主责，禁止把新业务默认堆进 Java。
  - Intake 时必须标明：`backend=python`（默认）或 `backend=java-maintain`（仅维护）。
- **按模块改**：前端只动 `translation/`；新后端/Agent 只动 `terminology-agent/`；Java 仅在维护面打开。跨模块需求先分 story，再改代码。
- **不要把通用脚手架当成业务**：`docs/ARCHITECTURE.md` 已描述本项目真实目录，勿再按 `app/domain/` 去新建无关目录。
- **本地全栈**：优先根目录 `pnpm dev`（需 Windows Terminal）。端口契约：UI `18000` → Java `18001` → Python `18002`。
- **密钥与环境**：`terminology-agent/.env`、DB 口令等不得提交；参考各模块 `.env.example`。
- **旁路目录**：`translation-assistant/`、`translationtool_ai/`、`translation_check/`、`knowledge/`、`references/` 多为辅助/文档/实验，非默认改动面；除非需求点名，否则不要改。
- **Git 提交说明**：创建 commit 时冒号后的摘要与正文默认用**简体中文**，详见 `docs/HARNESS.md`「Git 提交说明」。

<!-- HARNESS:BEGIN -->
## Harness

操作前先判断请求类别。

- 若结果只需回答、解释、评审、诊断、计划或状态报告：只读所需材料，保持只读。不要 bootstrap、初始化或迁移数据库、录入 intake，也不要记录 trace。
- 若用户明确要求修改、构建、修复或写入仓库产物：先在 macOS/Linux 运行 `scripts/bootstrap-harness.sh`，或在 Windows 运行 `.\scripts\bootstrap-harness.ps1`。然后按 `docs/FEATURE_INTAKE.md` 分类并录入请求；在 macOS/Linux 查询 `scripts/bin/harness-cli query matrix --active --summary`，或在 Windows 查询 `.\scripts\bin\harness-cli.exe query matrix --active --summary`；并只拉取 `docs/CONTEXT_RULES.md` 中与车道和任务相关的上下文。
<!-- HARNESS:END -->
