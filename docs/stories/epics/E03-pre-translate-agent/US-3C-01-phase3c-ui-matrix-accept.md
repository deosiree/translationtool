# US-3C-01 Phase 3c admin-proj UI 全矩阵验收

## Status

implemented

## Lane

normal

## Module / Backend

- 模块面：全栈（`translation/` 验收为主；缺口修复优先 `terminology-agent/`）
- 后端面：`backend=python`（默认；**不动 Java**）
- 决策继承：`docs/decisions/0009-python-backend-prefer-java-maintain.md`

## Product Contract

Phase 3c（jieba 切界 + `compose_suggest` LLM 受约束拼装）代码已合入。本 story 关闭验收缺口：在 admin-proj 工作台与术语学习页证明 exact / fuzzy / decomposed / none+LLM / 审核意见拷贝 / review 全路径可见且正确。若验收发现缺口，仅在 Python Agent 或 UI 做最小修复；不扩大到 Phase 4/5/6。

## Acceptance Criteria

- [x] DB `entry_comment` 列已存在
- [x] `pytest -q` 全绿
- [x] `verify_adm_pretranslate --strict` 全绿
- [x] API 矩阵：exact / S02 / decomposed=`File and System` / T99
- [x] 登录 → 工作台（修复 `__webpack_require__ is not defined`）
- [x] 术语学习页 + `GET /agent/term-learning/list`
- [x] reasoning→englishAuditSuggest（vitest）
- [x] review API rejected 冒烟
- [x] 缺口修复仅 `translation/`；Java 零改动

## Validation

| Layer | 2026-07-16 |
| --- | --- |
| Unit | 通过 |
| Integration | 通过 |
| E2E | 通过（openCLI：登录工作台 + 术语学习） |

## Evidence

### 缺口修复（translation/ only）

1. `src/router/index.js` — 同步引入 `Layout`
2. `src/views/layout/layout.vue` — `FloatingToolBox` → `defineAsyncComponent`
3. `vue.config.js` — 开发态 `devtool: cheap-module-source-map`
4. `src/router/asyncRouter.js` — 菜单 component 自动补 `.vue`

### 证明摘要

- Agent：pytest 142、ADM 6/6、API matrix、review API
- UI：登录进 workbench；术语学习页展示待审核 + Agent 说明列；Network 见 term-learning/list
