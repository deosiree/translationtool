# TableViewport

Ant Design Vue 表格的统一视口、列宽和 resize 管理组件。

## 用法

```vue
<TableViewport
  :columns="columns"
  :data-source="rows"
  :scroll="{ y: 400 }"
  adaptive-columns
>
  <template #bodyCell="scope">
    <CellOverflowTooltip :content="scope.text" />
  </template>
</TableViewport>
```

组件根据当前列布局计算数值型 `scroll.x`，调用方无需传入旧的 `"max-content"`。纵向高度从 `scroll.y` 读取；纵向和横向滚动均由内部内容区承载。宽屏时仅向 adaptive 列分配剩余宽度；列宽不足时使用固定总宽度和内部横向滚动。resize 移动超过 `3px` 才保存手动列宽，点击句柄不改变布局，真实拖拽结束后只拦截一次排序 click。

## 主要 Props

- `columns`、`adaptiveColumnKeys`、`adaptiveColumns`：列定义和自适应列配置。
- `adaptiveBaseWidth`：未配置宽度的 adaptive 列基础宽度，默认 `220px`。
- `reservedWidth`：选择列等额外预留宽度。
- `layoutKey`：列布局重建标识，变化时清理当前实例手动宽度。
- `height`、`fillHeight`、`emptyOverlay`：视口高度和空态配置。

`useTableColumnLayout.js` 是该组件内部的唯一列宽状态来源；`columnLayout.js` 仍是跨页面共享的列语义配置。
