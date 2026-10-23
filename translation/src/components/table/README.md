# 表格共享组件

## `TableViewport`

`TableViewport` 为 Ant Design Vue `a-table` 提供固定高度的内容视口和表格内部覆盖滚动条。

- `height`：可选的整个表格视口高度，支持数字或 CSS 高度。未提供时，组件用旧 `scroll.y` 加表头和分页高度计算视口；没有 `scroll.y` 时为 `100%`。
- `scroll`：接收表格滚动参数，只把 `scroll.x` 传给 `a-table`；纵向滚动由内部内容区承载。
- `columns`、`adaptiveColumnKeys`：可选的列定义及自适应列标识。视口有剩余宽度时按列基础宽度比例分配给指定列；拖拽后的宽度按列 ID 固定，最小为 `50px`。
- `adaptiveColumns`：兼容旧页面的语义回退开关。开启后优先使用列上的 `adaptive: true`，没有显式标记时再按 `columnLayout.js` 的字段语义识别；普通 `resizable` 列不会自动扩展。
- `adaptiveBaseWidth`：未配置列宽时的自适应列基础宽度，默认 `220px`。
- `reservedWidth`：列宽计算时额外预留的宽度；选择列宽会按 `rowSelection.columnWidth` 自动计入。
- 其余表格属性、事件和插槽转发给内部 `a-table`。

组件不传 `scroll.y`，避免 Ant Table 生成滚动条补偿列。表头通过 sticky 定位固定在同一滚动视口内。原生滚动条隐藏，覆盖滚动条位于内容视口内部，不占表格宽度，在悬停、滚动或拖动时显示。

```vue
<TableViewport
  :height="'100%'"
  :scroll="{ x: 'max-content' }"
  :columns="columns"
  :data-source="rows"
  :pagination="pagination"
>
  <template #bodyCell="scope">
    <CellOverflowTooltip :content="scope.text" />
  </template>
</TableViewport>
```

## `TableTagCell`

`TableTagCell` 统一表格 Tag 的浏览态：按中英文分号拆分为标签，在列宽不足时单行省略并通过 `CellOverflowTooltip` 显示完整 Tag 内容。`text` 接收原始 Tag 文本，`color` 可覆盖默认的 `cyan` 标签颜色。

Comment 不使用标签外观；浏览态由 `TableCellText` 或 `CellOverflowTooltip` 呈现，编辑态仍由业务页面决定。这样 Tag 和 Comment 的 Tooltip 均只会在真实溢出时出现。

组件不包含业务选择、分页请求或 loading 语义；这些状态由页面负责。

## `TableHeaderHint`

`TableHeaderHint` 统一渲染列名后的说明图标。当前内置 `tag` 和 `comment` 两个字段的业务提示；未知字段退化为普通列名。组件使用 `tabindex="0"` 和 `aria-label` 支持键盘聚焦，不改变列宽。

## `useAdaptiveColumnWidths`

用于按表格可用宽度扩展指定列。调用方提供当前可见列、基础宽度、自适应列键、手动宽度和固定预留宽度；剩余空间按自适应列基础宽度比例分配。手动拖拽宽度从自动扩展集合中移除，视口不足时不压缩列宽，交由横向滚动显示。

共享语义档位由 `columnLayout.js` 维护：序号 `56px`、选择列 `48px`、状态 `96px`、语种 `88px`、人员 `96px`、版本 `120px`、日期 `104px`、日期时间 `168px`、词条 `220px`、译文 `260px`、释义/备注 `180px`、Comment `180px`、来源 `200px`、操作 `96px`。长文本档位默认 `adaptive: true`；词条、译文、释义与备注左对齐，Comment 居中，其他字段保持居中。

术语库与工作台使用 `220px` 的词条基础宽度、`260px` 的翻译基础宽度，并为选择列预留 `48px`。其他页面可通过 `TableViewport` 的 `adaptiveColumnKeys` 配置长文本列；未配置列宽时使用 `adaptiveBaseWidth`。

列定义可以显式设置 `width`、`minWidth`、`align`、`resizable` 和 `adaptive`。宽屏时只把剩余空间按基础宽度比例分配给 `adaptive` 列；窄屏时保留各列基准宽度并使用表格内部横向覆盖滚动条。用户拖拽后的宽度仅在当前组件实例内固定，不会被后续视口测量覆盖。

```js
const widthState = useAdaptiveColumnWidths(50);
widthState.setManualWidth("entry", 300);
const widths = widthState.resolve(viewportWidth, visibleColumns, ["entry", "translate"], 48);
```

## `TableCellText`

`TableCellText` 统一呈现表格文本的浏览态和编辑态。编辑状态由父页面通过 `editing` 控制，组件只负责呈现并向上发出交互事件。

- `text`：浏览态展示内容，溢出时使用省略号，并在内容溢出后显示 Tooltip。
- `value`：编辑态文本值。
- `editing`：是否显示文本域，默认为 `false`。
- `autoSize`：文本域行数配置，默认 `{ minRows: 1, maxRows: 5 }`。
- `emptyText`、`errorMessage`：空内容占位和编辑错误信息。
- `dblclick`、`update:value`、`blur`：双击、文本变更和失焦事件。

组件内容本身允许收缩；需要允许列拖到 50px 时，应由列定义的 `minWidth: 50` 控制真实列宽下限。

```vue
<TableCellText
  :text="text"
  :value="editableValue"
  :editing="isEditing"
  @dblclick="startEditing"
  @update:value="editableValue = $event"
/>
```
