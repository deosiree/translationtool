# US-3C-01 Phase 3c admin-proj UI 全矩阵验收

## Status

planned

## Lane

normal

## Module / Backend

- 模块面：全栈（`translation/` 验收为主；缺口修复优先 `terminology-agent/`）
- 后端面：`backend=python`（默认；**不动 Java**）
- 决策继承：`docs/decisions/0009-python-backend-prefer-java-maintain.md`

## Product Contract

Phase 3c（jieba 切界 + `compose_suggest` LLM 受约束拼装）代码已合入。本 story 关闭验收缺口：在 admin-proj 工作台与术语学习页证明 exact / fuzzy / decomposed / none+LLM / 审核意见拷贝 / review 全路径可见且正确。若验收发现缺口，仅在 Python Agent 或 UI 做最小修复；不扩大到 Phase 4/5/6。

## Relevant Product Docs

- `.cursor/plans/pretranslategraph_阶段二_886a27fa.plan.md`（Phase 3c 设计）
- `.cursor/plans/pretranslategraph_进度快照.md`（2026-07-13 进度）
- `terminology-agent/app/graph/pre_translate/README.md`
- `docs/ARCHITECTURE.md`、`docs/TEST_MATRIX.md`

## Acceptance Criteria

- [ ] DB 已应用 `terminology-agent/scripts/migrations/001_add_entry_comment_to_term_agent_audit.sql`（或确认列已存在）。
- [ ] `cd terminology-agent && pytest -q` 全绿。
- [ ] `python -m devtools.verify_adm_pretranslate --strict` 全绿。
- [ ] UI 矩阵（`http://localhost:18000`，admin / admin123）：
  1. exact → 翻译列自动回填，`retrieval_method=exact`
  2. fuzzy 低置信 → pending + `term_agent_audit` 有行
  3. decomposed（如「文件与系统」）→ 建议译文为自然短语（如 `File and System`），检索方式「拆解拼装」，非 `FileSystem` 硬拼接
  4. none + LLM → `needs_human`，无 `[Agent]` 占位
  5. auto_approved → 对应语种审核意见列含 `agent_meta.reasoning`
  6. 术语学习页确认/拒绝 → review API 正常
- [ ] 若有缺口修复：diff 仅落在 `translation/` 和/或 `terminology-agent/`；`translationtoolservice/` 零改动。

## Design Notes

- Commands: `pnpm dev` / `pnpm dev:ui-agent`；Agent `:18002`；UI `:18000`
- Queries: `verify_adm_pretranslate`、可选 Network 抓 `/agent/pre-translate/batch`
- API: 不改 `agent_meta` 六字段契约（除非验收证明必须修 bug）
- Tables: 仅确认 `term_agent_audit.entry_comment`；本 story 不新开 schema
- Domain rules: jieba 切界 SSOT；术语库只 lookup；compose_ok → `compose_suggest`
- UI surfaces: 工作台预翻译弹窗、术语学习页、审核意见列

## Validation

| Layer | Expected proof |
| --- | --- |
| Unit | `pytest -q`（terminology-agent）；必要时前端相关 vitest |
| Integration | `verify_adm_pretranslate --strict` |
| E2E | 上表 UI 六场景手工清单（证据写入 Evidence） |
| Platform | 不要求 |
| Release | 不要求 |

## Out of Scope

- Phase 3d n-gram 合并 lookup（见 US-3D-01）
- Phase 4 矛盾治理（阻塞：lexicon skill；见 backlog）
- Phase 5 Judge / Phase 6 FAISS
- 任何 `translationtoolservice/` 改动

## Harness Delta

- Intake 记录本 story 为当前主线；验收通过后 `story update` + proof。

## Evidence

（验收后填写命令输出摘要 / 截图路径 / Network 摘录）
