# SearchBox 查询控件宽度统一（B2）

## Goal

查询条件区内控件宽度一致；宽度只由 `SearchBox` 提供，页面不再写零散 `style="width: …"`。

## Non-goals

- 不新建栅格布局壳
- 不改弹窗/表格内 `width: 100%` 表单项
- 不强制全站所有非 SearchBox 页面立刻改造

## Design

1. `searchBox.vue` 使用 CSS 变量 `--search-control-width: 186px`，继续 `:deep(.ant-form-item-control)` 统一宽度。
2. 导出 JS 常量 `SEARCH_CONTROL_WIDTH`（壳外极少数复用）；查询表单内默认不用。
3. glossary 查询区（术语字典 / 术语词典 / Comment 规则）去掉内联 width。
4. `docs/CONTEXT_RULES.md` 增加 retrieval trigger：SearchBox 内禁止零散改宽。

## Acceptance

- 术语字典查询区各控件视觉宽度一致（≈186px）
- 新增查询字段无需再写 width；按钮仍走 `operate` 插槽
