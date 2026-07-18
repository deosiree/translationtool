# E03 — PreTranslate Agent（Phase 3c+）

本 epic 承接 i18n Agent 路线图 Phase 3c 验收及后续可选增强。

| Story | 标题 | Lane | Status | 说明 |
| --- | --- | --- | --- | --- |
| [US-3C-01](./US-3C-01-phase3c-ui-matrix-accept.md) | Phase 3c UI 全矩阵验收 | normal | implemented | 已关闭 |
| [US-3C-02](./US-3C-02-phase3c-post-accept-hygiene.md) | Phase 3c 验收后 P0/P1 卫生 | normal | implemented | 已关闭 |
| [US-3D-01](./US-3D-01-ngram-term-align.md) | Phase 3d n-gram 术语对齐 | normal | implemented | 已关闭（可选增强） |
| [US-3E-01](./US-3E-01-segment-trace-persist.md) | 切分轨迹落库与双端展示 | normal | implemented | audit + entry；Java Entity 透传 |
| [US-3E-02](./US-3E-02-segment-trace-ux.md) | 切分轨迹对象/Tag/占位符合并 | normal | planned | API 对象、Tag UI、`%N` 合并 |

Phase 4（矛盾治理）待用户提供 lexicon skill，暂挂 Harness backlog，不在本 epic 开 high-risk 包。

### ADM / 验收脚本

```powershell
cd terminology-agent
python -m devtools.verify_adm_pretranslate --strict
python -m devtools.verify_us3c01_api_matrix   # 需 :18002
# pending / exact 污染时：
python -m devtools.cleanup_adm_test_data --dry-run
python -m devtools.cleanup_adm_test_data --apply
python -m devtools.fix_adm_test_data --apply
```

路线图：`.cursor/plans/pretranslategraph_阶段二_886a27fa.plan.md`  
进度快照：`.cursor/plans/pretranslategraph_进度快照.md`
