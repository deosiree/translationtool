# US-3E-01 切分轨迹落库与双端展示

## Status

implemented

## Lane

normal

## Module / Backend

- 模块面：新后端 / Agent + 前端；Java 仅 Entity 出参透传
- 后端面：`backend=python` + `backend=java-maintain`（`EntryInfoEntity.segmentTrace`）
- 决策：`docs/decisions/0010-dual-backend-read-vs-write-persistence.md`

## Product Contract

凡走过 jieba/对齐切分的预翻译，将 `segment_trace` 写入 `term_agent_audit`（含 `auto_approved`），并同步 `t_entry_info.segment_trace`。术语学习与工作台各增加默认隐藏「切分」列；关预翻译弹窗后仍可从落盘字段查看。不换工作台列表主 API，不做 Python 批量二次 enrich。

## Acceptance Criteria

- [x] Migration：`term_agent_audit` + `t_entry_info` 增加 `segment_trace`
- [x] `build_segment_trace` + `write_result` 凡切分落 audit 并 sync entry
- [x] `agent_meta` / AuditRecord 透出；Java Entity 出参透传
- [x] 术语学习 / 工作台列默认隐藏，渲染 `display`
- [x] pytest + ADM `--strict` 绿（2026-07-16：155 passed / 6/6）

## Validation

| Layer | Expected proof | Result |
| --- | --- | --- |
| Unit | `test_segment_trace` + mapper/backfill | 通过 |
| Integration | `verify_adm_pretranslate --strict` | 6/6 OK |
| E2E | 可选：开列设置勾选「切分」 | 未要求 |

## Out of Scope

- 工作台列表整页换 Python API
- `POST /agent/segment-trace/batch`
- 改 Java 业务回填逻辑
