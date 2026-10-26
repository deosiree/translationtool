# TableCellText

统一表格文本单元格的浏览态省略 Tooltip 与编辑态多行文本编辑器。

## 用法

```vue
<TableCellText
  :text="text"
  :value="value"
  :editing="editing"
  @dblclick="startEditing"
  @update:value="value = $event"
  @blur="finishEditing"
/>
```

浏览态由 `CellOverflowTooltip` 负责真实溢出检测；编辑态由 `TableCellTextArea` 负责输入，不显示浏览态 Tooltip。

## Props 与事件

- `text`：浏览态文本。
- `value`：编辑态值。
- `editing`：是否进入编辑态，默认 `false`。
- `emptyText`：空文本占位。
- `autoSize`：编辑器行数配置。
- `errorMessage`：编辑错误提示。
- 事件：`dblclick`、`update:value`、`blur`。
