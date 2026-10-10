# OpenCLI 抽检：工作台四阶段表格溢出

前置：本地前端已启动并登录；工作台打开对应阶段弹窗，表格有长短文本数据。

## 1. 环境检查

```bash
opencli doctor
```

`doctor` 不绿则跳过以下步骤，并在故事 Validation 中写明原因（不得标通过）。

## 2. 各阶段绑定与浏览态

会话名：`wb-import` / `wb-examine` / `wb-translate` / `wb-examine-trans`。打开对应阶段后执行：

```bash
opencli browser <session> bind
opencli browser <session> extract ".cell-overflow-tooltip" --fields text,style
```

**断言：**

- 样式含 `text-overflow: ellipsis`（或等效单行省略）
- 样式含 `white-space: nowrap`
- 样式含 `overflow: hidden`

## 3. 编辑态：定高文本域

双击翻译格进入编辑，再执行：

```bash
opencli browser <session> extract "textarea.ant-input" --fields style
```

**断言：**

- textarea 由 antd autoSize 封顶（约 5 行）
- `overflow-x: auto`（或 `overflow-x: scroll`）
- `resize: none`
- 编辑区内无浏览态 `.cell-overflow-tooltip` 包裹；td 无第二条纵向滚动条
- Enter 换行，不触发保存

### 导入阶段额外

编辑 tag 格后：

```bash
opencli browser wb-import extract ".ant-table-cell input.ant-input" --fields tag
```

**断言：** tag 编辑格是 `input` 不是 `textarea`。

## 4. 手工回归（doctor 跳过时仍建议）

1. 拖窄列宽出现省略；短文本悬停 2s 无气泡，长文本 1s 后全文。
2. 四阶段双击翻译仅 textarea 内侧一条滚动条。
3. 归档页行为未回归。
