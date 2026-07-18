# US-3E-02 切分轨迹调试体验与占位符合并

## Status

planned

## Goal

解决 US-3E-01 验收反馈：API/列表里 `segmentTrace` 像「字符串化的 JSON」、列展示不醒目、`%1` 被切成 `%`+`1`。

## 需求 → 方案

### 1. API 返回对象而非 JSON 字符串

**根因**：`EntryInfoEntity.segmentTrace` 声明为 `String`，MyBatis 读出 JSON 列后 Jackson 再序列化 → 前端看到带引号的字符串，需二次 `JSON.parse`，调试差。

**改法（锁定）**：

| 层 | 动作 |
| --- | --- |
| Java | `segmentTrace` 改为 `Map<String, Object>`（或 `Object`）+ `@TableField(typeHandler = JacksonTypeHandler.class)` + `@TableName(autoResultMap = true)`（若类上尚无） |
| Python | 契约不变：仍写 JSON 对象；`agent_meta.segment_trace` 已是对象 |
| 前端 | `parseSegmentTrace(value)` 统一：string → parse；object → 原样；失败 → null。列表/Agent 列都走它 |

### 2. UI：词片用小 Tag

- 术语学习 / 工作台「切分」列：对 `jieba[]`（或无则从 `display` 拆）逐个渲染 `<a-tag size="small">`，不再只显示一整段 `display` 文本。
- `columnBuilder` 的 `customRender` 对 Ant Design Vue 3 只能返回 VNode/文本，**工作台**改用表格 `#bodyCell` 模板（与 `terminologyAgent` 一致），或抽小组件 `SegmentTraceTags.vue`。
- 锁定：新建 `translation/src/components/SegmentTraceTags.vue`，两处复用。

### 3. 占位符 `%N` 与无意义连续片段合并

落点：[`segment_source_text`](terminology-agent/app/shared/term_word/segment.py) 在 jieba 之后做 **后处理合并**（Grep/decompose 共用，一处修全链路）。

规则（从左到右贪心）：

1. `%` + 连续数字 → 合并为 `%1`、`%12`
2. 已是 `%\d+` 的相邻 token，中间仅为 `/`、`_`、`-`、`.` 之一 → 合并为 `%1/%2` 等
3. 不改变其它中文词界；不 `load_userdict`

单测样例：

| 输入 | 期望含 |
| --- | --- |
| `第%1页` | `%1` 单 token，无单独 `%` |
| `%1/%2` | 一个 token `%1/%2` |
| `文件%1` | `文件`、`%1` |

## 验证

- pytest：`segment` 后处理 + `build_segment_trace`
- vitest：`parseSegmentTrace` / Tag 组件 shallow（若有）
- 手工：预翻译含 `%1/%2` 的词条，API 中 `segmentTrace` 为对象；列上为多个 tag

## Out of Scope

- 改切界 SSOT 为术语驱动
- 整页换工作台列表 API
