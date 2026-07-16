# US-3D-01 Phase 3d 相邻 span 合并 lookup（可选）

## Status

implemented

## Lane

normal

## Module / Backend

- 模块面：新后端 / Agent
- 后端面：`backend=python`
- **依赖**：US-3C-01 验收通过后再启动

## Product Contract

jieba 切界后，对相邻 token 尝试合并再 `find_by_word`（如「文件」+「系统」→「文件系统」）；仅当合并串在 `term_word` 有**唯一**译法时合并。**不改变** jieba 原始切界 SSOT；合并后 `Span.jieba_parts` 保留原始 token。术语库仍不主导切界。

## Acceptance Criteria

- [x] 合并 lookup 有单测（命中合并 / 不命中保持原 spans / 歧义不合并）
- [x] Grep 与 decompose 共用 `align_spans_with_lexicon`
- [x] 主图 decomposed 路径回归：`verify_adm_pretranslate --strict` 仍绿

## Validation

| Layer | Expected proof | Result (2026-07-16) |
| --- | --- | --- |
| Unit | pytest 覆盖合并 lookup | `test_align_spans` + lookup/grep；全仓 **151 passed** |
| Integration | ADM pretranslate 矩阵 | **6/6 OK**（`--strict`） |
| E2E | 可选一条 UI 复合词条 | 未要求（本 story 跳过） |

## Implementation notes

| 文件 | 职责 |
|------|------|
| `utils/align_spans.py` | 贪心 n-gram 对齐（`ALIGN_MAX_NGRAM=3`） |
| `nodes/features/io/lookup_lexemes.py` | 预取候选 + align |
| `utils/grep_retrieve.py` | 同策略 |
| `decompose_compose.py` | trace：`jieba_span_count` / `ngram_aligned` |

## Out of Scope

- `load_userdict(term_word)` 主导切界
- Phase 4 矛盾治理
