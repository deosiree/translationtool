# CellOverflowTooltip

统一处理表格浏览态文本的单行省略和溢出 Tooltip。

## 用法

```vue
<CellOverflowTooltip :content="text" />
```

当文本真实发生横向溢出时，Tooltip 内容使用完整原始文本；未溢出、空文本或纯空白文本不会展示空 Tooltip。组件默认使用 `document.body` 作为弹层容器，并保留现有延迟配置。

## Props

- `content`：浏览态原始文本，支持 `String`、`Number`。
- `emptyText`：内容为空时的显示文本，默认空字符串。

`isTextOverflow.js` 是该组件的内部溢出检测 helper，不作为页面公共 API 使用。
