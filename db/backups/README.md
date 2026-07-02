# 数据库备份目录

本地 `translationtool` MySQL 库的 mysqldump 备份存放于此。

## 命名规范

```
translationtool_YYYYMMDD_HHmmss.sql
translationtool_YYYYMMDD_HHmmss_before_admin_proj_test.sql   # 带 Label
```

## 指针文件

- `.latest` — 文本文件，内容为最近一次备份的绝对路径（由 skill 脚本写入）

## 注意

- `*.sql` 已加入 `.gitignore`，**勿提交**到 git
- restore 为 **DROP DATABASE** 级操作，执行前请确认备份文件
- 推荐流程：测试前备份 → 测试操作 → 恢复到 `.latest`

## 相关 skill

`huiyanSkills/translateTool-skills/db-回滚数据库`
