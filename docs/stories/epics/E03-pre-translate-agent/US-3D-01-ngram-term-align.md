# US-3D-01 Phase 3d 相邻 span 合并 lookup（可选）

## Status

planned

## Lane

normal

## Module / Backend

- 模块面：新后端 / Agent
- 后端面：`backend=python`
- **依赖**：US-3C-01 验收通过后再启动

## Product Contract

jieba 切界后，对相邻 token 尝试合并再 `find_by_word`（如「文件」+「系统」→「文件系统」）；仅当合并串在 `term_word` 存在时合并。**不改变** jieba 原始切界 trace；术语库仍不主导切界。

## Acceptance Criteria

- [ ] 合并 lookup 有单测（命中合并 / 不命中保持原 spans）
- [ ] Grep 与 decompose 共用同一对齐策略或明确文档差异
- [ ] 主图 decomposed 路径回归：`verify_adm_pretranslate` 仍绿

## Validation

| Layer | Expected proof |
| --- | --- |
| Unit | pytest 覆盖合并 lookup |
| Integration | ADM pretranslate 矩阵 |
| E2E | 可选一条 UI 复合词条 |

## Out of Scope

- `load_userdict(term_word)` 主导切界
- Phase 4 矛盾治理
