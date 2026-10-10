# OpenCLI 抽检：术语库表格溢出

前置：本地前端已启动并登录；进入 **术语库** 主表，有长短翻译数据。

## 1. 环境检查

```bash
opencli doctor
```

`doctor` 不绿则跳过以下步骤，并在故事 Validation 中写明原因（不得标通过）。

## 2. 绑定术语库

```bash
opencli browser glossary bind
```

## 3. 浏览态

```bash
opencli browser glossary extract ".cell-overflow-tooltip" --fields text,style
```

**断言：** 省略三件套（`ellipsis` / `nowrap` / `overflow: hidden`）。

## 4. 编辑态

双击翻译格进入编辑：

```bash
opencli browser glossary extract "textarea.ant-input" --fields style
```

**断言：**

- `resize: none`；约 5 行封顶
- 编辑格无 `.cell-overflow-tooltip`
- Enter 换行；保存走「保存」按钮，不因 Enter 提交

## 5. 手工回归

操作列「详情」不受 Tooltip 包裹；拖窄列宽无双滚动条。
