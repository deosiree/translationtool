# TableCellTextArea

表格文本单元格使用的多行编辑器壳层，复用项目现有 `textarea_IME` 输入组件。

## 用法

```vue
<TableCellTextArea
  :value="value"
  :auto-size="{ minRows: 1, maxRows: 5 }"
  @update:value="value = $event"
  @blur="onBlur"
/>
```

## Props 与事件

- `value`：编辑器值。
- `autoSize`：输入框行数配置。
- `errorMessage`：非空时显示错误提示。
- 事件：`update:value`、`blur`。

该组件只负责编辑态，不负责 Tooltip 或列宽管理。
