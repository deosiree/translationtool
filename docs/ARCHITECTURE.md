# Architecture — Translation Tool

本文件描述 **本仓库业务架构**（消费者侧）。上游 Harness CLI 本身的实现细节见 `docs/HARNESS.md`，不要与业务模块混淆。

## 产品一句话

词条翻译管理平台：词条管理、翻译、审核、版本/分支；前端 + **双后端**（遗留 Java 维护面 + Python 新后端/Agent）。

## 双后端策略（权威）

| 后端 | 路径 | 定位 | 何时改 |
| --- | --- | --- | --- |
| **Python（新后端）** | `terminology-agent/` | 后续开发的默认后端；FastAPI API + LangGraph Agent | **新需求、新能力、新接口默认落这里** |
| **Java（遗留后端）** | `translationtoolservice/` | 他人主责的既有服务；业务存量 API | **仅维护**：修 bug、安全补丁、不得不做的兼容；能不动就不动 |

详见 `docs/decisions/0009-python-backend-prefer-java-maintain.md`。

## 模块与调用面

```text
浏览器 / Electron UI (:18000)
        |
        +--(存量业务 API)--> Spring Boot  translationtoolservice (:18001)  [维护面]
        |                         +-----> MySQL 8 / Redis 7
        |
        +--(新能力 / Agent API)--> FastAPI+LangGraph  terminology-agent (:18002)  [默认开发面]
                                      +-----> LLM Provider（.env）
                                      +-----> （按需）调用遗留 Java 或受控数据源
```

| 模块 | 路径 | 职责 | 技术 |
| --- | --- | --- | --- |
| UI | `translation/` | 词条与流程界面、Electron 壳、开发代理 | Vue 3、Ant Design Vue、Electron、Webpack |
| 遗留后端 | `translationtoolservice/` | 存量业务 API、持久化、权限与流程（维护） | Spring Boot 2.7.7、MyBatis-Plus、Druid |
| 新后端 / Agent | `terminology-agent/` | 新后端能力、术语/Agent、状态机编排、对外 HTTP | FastAPI、LangGraph、pytest |

## 模块间关系（边界规则）

1. **新能力优先 Python**：新增后端行为默认在 `terminology-agent/` 实现；不要先开 Java 脚手架。
2. **Java 最小触碰**：除非明确是遗留维护，或人类点名必须改 Java，否则 Agent 不得主动扩展 `translationtoolservice/`。
3. **UI → 双后端**：存量走 Java；新能力走 Python（dev proxy / nginx）。前端禁止直连数据库。
4. **Python → Java / DB**：可按设计调用遗留接口或受控数据源；新增跨后端调用必须记入 `docs/decisions/`。
5. **共享契约**：破坏性变更走 high-risk lane；跨 Java↔Python 契约变更额外勾选 Cross-module contract。
6. **Docker 边界**：`docker-compose.yml` 编排 UI / Java / Python / MySQL / Redis；镜像与离线包见 `ENV_package/`。

## 仓库目录地图（真实，非脚手架）

```text
translationtool/
  translation/                 # Vue + Electron UI
  translationtoolservice/      # Spring Boot
  terminology-agent/           # FastAPI + LangGraph
  docker-compose.yml
  scripts/                     # 含 harness-cli、bootstrap、本地 dev 辅助
  docs/                        # Harness 文档 + 本架构
  references/                  # 本地开发、测试等说明（人类文档）
  AGENTS.md                    # Agent 入口
```

旁路/辅助（默认不改）：`translation-assistant/`、`translationtool_ai/`、`translation_check/`、`knowledge/`、`.reasonix/` 等。

## 改动落点启发式

| 现象 | 优先查看 |
| --- | --- |
| 页面、表格、弹窗、路由、Electron | `translation/src/` |
| **新后端 API / Graph / prompt / pytest** | `terminology-agent/`（默认） |
| 遗留 Java 修 bug / 兼容 / 他人接口维护 | `translationtoolservice/`（仅维护） |
| 端口、容器、依赖服务 | `docker-compose.yml`、根 `package.json` 的 `pnpm dev*` |
| AI 协作流程、验收矩阵、决策史 | `docs/` + `AGENTS.md` |

**默认决策顺序**：能否只改 Python？→ 能否 UI + Python？→ 是否必须动 Java（需人类确认）？

**工作台 ≠ Java**：工作台**新需求 / 新 API** 仍走 Python（`:18002`）；**仅**给旧 Java API 加参/出参等维护才动 `translationtoolservice/`。持久可见可分 A（Agent 表）/ B（`agent_meta` 会话）/ C（工作台持久，**优先新 Python 读接口**）——详见 `docs/decisions/0010-dual-backend-read-vs-write-persistence.md`。

## Discovery Before Shape（仍适用）

实现前确认：

- 触及哪些表面（UI / API / Agent / infra）
- **新字段要持久出现在哪张 UI**（Agent 页 / 预翻译会话 / 工作台主表）
- 是否跨模块契约变更
- 最小验证路径（见 `docs/TEST_MATRIX.md`）
- 是否已有决策可继承（`docs/decisions/`）

## 依赖规则（业务侧）

- 内层业务规则不依赖 UI 框架细节。
- UI 不嵌入 Java/Python 实现细节；只依赖稳定 API。
- Agent 图状态与 prompt 变更需可测（`terminology-agent` 下 pytest），避免只靠手工点点点验收。

## 风险热点

- 词条回填 / 文件管理类大弹窗（如 `translation/src/components/Button/fileManage/backFill/`）— UI 状态复杂，易回归。
- API 响应形状与错误处理 — 三端耦合。
- Agent 与 LLM 配置 — 密钥、超时、幂等与可观测性。
- DB schema / MyBatis XML — 高风险，需迁移与回滚意识。
