# 部门配置与编码修复

## 问题

登录后提示：

> 未找到用户部门"é€šç"¨å¹³å°éƒ¨"的配置，已设置为默认部门

原因是 MySQL 中 `t_user.department` 字段存储了**双重 UTF-8 编码**（Mojibake）的"通用平台部"。

## 原因

通过 SQL 文件写入中文时，Windows 命令行管道导致 UTF-8 字节被二次编码：

```
预期 UTF-8 字节:  E9 80 9A E7 94 A8 E5 B9 B3 E5 8F B0 E9 83 A8
实际存储的字节:  C3 A9 E2 82 AC C5 A1 ...  （双重编码）
```

前端 `commonParam.departmentMap` 中的 key 是正确编码的 `"通用平台部"`，而数据库中存的是乱码字节，字符串比较失败。

## 修复

```bash
docker exec -i translation-mysql mysql -uroot -p123456 translationtool \
  -e "UPDATE t_user SET department = UNHEX('E9809AE794A8E5B9B3E58FB0E983A8') WHERE user_name = 'admin'"
```

## 前端部门配置

`translation/src/constants/commonParam.js` 的 `departmentMap` 中已预定义了以下部门：

| 部门 | value | classifyId |
|---|---|---|
| 通用平台部 | common | 1 |
| 监控系统部 | jk | 6 |
| 装置开发部 | zz | 2 |
| 人工智能部 | zn | — |
| 柔性输电系统部 | rx | 101 |
| 公共库（默认） | default | 3 |

## 注意

- 后续通过 SQL 写中文时，用 `UNHEX('...')` 方式或确保管道编码为 UTF-8
- MySQL 容器默认字符集为 `utf8mb4`，表字段也是 `utf8mb4_bin`
