# OpenCLI 抽检：词条管理浏览省略与编辑文本域

前置：本地前端已启动；登录后进入 **词条管理**，选中某产品/模块，表格有数据（含长短词条与各语种翻译）。

## 1. 环境检查

```bash
opencli doctor
```

`doctor` 不绿则跳过以下步骤，并在结论中说明原因。

## 2. 绑定词条管理页

```bash
opencli browser entry-mgmt bind
```

## 3. 浏览态：省略与 Tooltip 容器

```bash
opencli browser entry-mgmt extract ".cell-overflow-tooltip" --fields text,style
```

**断言：**

- 样式含 `text-overflow: ellipsis`（或等效单行省略）
- 样式含 `white-space: nowrap`
- 样式含 `overflow: hidden`

## 4. 编辑态：定高文本域

双击词条或英文翻译格进入编辑，再执行：

```bash
opencli browser entry-mgmt extract "textarea.ant-input" --fields style
```

**断言：**

- textarea 由 antd autoSize 封顶（约 5 行，超出行数后仅 textarea 内侧出现**一条**纵向滚动条）
- `overflow-x: auto`（或 `overflow-x: scroll`）
- `resize: none`（无右下角原生拖拽手柄）
- 编辑区内无浏览态 `.cell-overflow-tooltip` 包裹；td 单元格无第二条纵向滚动条

## 5. 手工回归（doctor 跳过时仍建议）

1. 短文本悬停 2s 不出气泡；长文本拖窄列宽后出现省略号，悬停 1s 显示全文。
2. 编辑态词条/翻译列约 5 行封顶，极长无空格串可横滚；`comment` 等 `inputColumn` 仍为单行 Input。
3. tag 列 hover 不再折行；工作台-归档页行为未回归。
