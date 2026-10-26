# TableTagCell

统一表格 Tag 浏览态展示、单行省略和完整内容 Tooltip。

## 用法

```vue
<TableTagCell :text="tagText" color="cyan" />
```

组件按中英文分号拆分 Tag，并在列宽不足时保持单行省略。Tooltip 使用归一化后的完整 Tag 文本；空 Tag 不生成空提示。

## Props

- `text`：原始 Tag 文本，使用 `;` 或 `；` 分隔。
- `color`：传给 `a-tag` 的颜色，默认 `cyan`。

业务页面负责编辑、筛选和保存，组件只负责浏览态展示。
