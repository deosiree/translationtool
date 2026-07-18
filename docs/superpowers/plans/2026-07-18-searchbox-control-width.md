# SearchBox 控件宽度统一 Implementation Plan

> **For agentic workers:** Tiny lane — patch directly. Steps use checkbox syntax.

**Goal:** SearchBox 为查询控件唯一宽度源；glossary 查询区对齐。

**Architecture:** CSS 变量 + 常量；页面删内联 width；CONTEXT_RULES 防复发。

**Tech Stack:** Vue 3 + Ant Design Vue + Less

---

### Task 1: 常量 + SearchBox

- [x] 新增 `translation/src/components/search/searchControlWidth.js`
- [x] `searchBox.vue` 使用 `--search-control-width`

### Task 2: glossary 查询区

- [x] `TermWordDictionary.vue` / `index.vue` / `CommentRules.vue` 去掉查询区内联 width

### Task 3: Harness 约定

- [x] `docs/CONTEXT_RULES.md` 增加 SearchBox 宽度约定 trigger
