# TableHeaderHint

统一渲染表格列头说明图标、Tooltip 和键盘可访问语义。

## 用法

```vue
<TableHeaderHint
  :title="title"
  :column="column"
  hint="可选的显式说明"
/>
```

未传 `hint` 时，根据 `tableHeaderHints.js` 中的字段映射解析提示；未知字段退化为普通列名。当前内置 `tag` 和 `comment` 提示，不改变列宽。

## Props

- `title`：列名。
- `column`：Ant Design Vue 列定义。
- `hint`：显式提示文案，优先级高于字段映射。
