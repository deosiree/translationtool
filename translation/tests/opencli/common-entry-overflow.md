# OpenCLI 抽检：公共库表格溢出

前置：本地前端已启动并以 **管理员** 登录；进入 **公共库** 词条列表，有长短翻译/备注。

## 1. 环境检查

```bash
opencli doctor
```

`doctor` 不绿则跳过以下步骤，并在故事 Validation 中写明原因（不得标通过）。

## 2. 绑定公共库

```bash
opencli browser common-entry bind
```

## 3. 浏览态

```bash
opencli browser common-entry extract ".cell-overflow-tooltip" --fields text,style
```

**断言：** 省略三件套。

## 4. 编辑态（管理员双击行）

```bash
opencli browser common-entry extract "textarea.ant-input" --fields style
```

**断言：**

- 翻译、备注为 textarea：`resize: none`，约 5 行封顶
- 唯一属性为 `input` 不是 `textarea`

## 5. 手工回归

非管理员双击不进入编辑；拖窄列宽无双滚动条。
