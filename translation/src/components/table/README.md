# 表格共享组件

表格共享能力按组件目录组织，每个目录包含组件源码和自己的 `README.md`：

| 目录 | 职责 |
| --- | --- |
| `CellOverflowTooltip/` | 浏览态文本省略、真实溢出检测和 Tooltip |
| `TableCellText/` | 文本单元格浏览态/编辑态组合 |
| `TableCellTextArea/` | 多行文本编辑器壳层 |
| `TableHeaderHint/` | 列头说明图标、Tooltip 和无障碍语义 |
| `TableTagCell/` | Tag 拆分、展示、省略和 Tooltip |
| `TableViewport/` | 表格视口、列宽自适应、resize 和内部滚动 |

## 根目录工具

- `cellText.js`：跨组件复用的文本、Tag 和最大长度格式化函数。
- `columnLayout.js`：跨页面复用的列语义档位与 adaptive 元数据。

组件之间通过明确的目录路径引用，页面不依赖根目录下的同名组件文件。列宽状态只由 `TableViewport/useTableColumnLayout.js` 管理；展示组件不写入页面列定义。
