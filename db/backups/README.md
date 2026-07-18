# 数据库备份目录

本地 `translationtool` MySQL 库的 mysqldump 备份存放于此。

## 命名规范

```
translationtool_YYYYMMDD_HHmmss.sql
translationtool_YYYYMMDD_HHmmss_before_admin_proj_test.sql   # 带 Label
```

## 指针 / 状态文件

| 文件 | 作用 |
| --- | --- |
| `.latest` | 最近一次 **encoding verify 通过** 的备份绝对路径 |
| `.retention-state.json` | 月度清理提醒（`nextDueAt`）；**不自动删文件** |
| `.gitkeep` / `README.md` | 保留目录结构 |

## 相关文档 / skill

- Harness 运维约定：[`docs/ops/DEV_DB_CHECKPOINT.md`](../../docs/ops/DEV_DB_CHECKPOINT.md)
- 外部 skill：`huiyanSkills/translateTool-skills/db-回滚数据库`

## 红灯 / 绿灯

- **禁止**用 PowerShell `>` / `Set-Content` / `Get-Content \| docker exec` 读写 dump（会破坏 UTF-8，备份不可 restore）
- **必须**用 skill 脚本：容器内 `mysqldump --result-file` + `docker cp`；restore 同为 `docker cp` + `mysql < file`
- COMMENT 已乱码的旧 `.sql` **作废**，勿再 restore
- 清理备份须人审：`prune-backups.ps1 -ConfirmDelete`；禁止静默删

## 当前保留的可用备份（2026-07-17 清理后）

| 文件 | 用途 |
| --- | --- |
| `schema_repaired_usable.sql` | **init**：修复后的 schema/配置种子 |
| `translationtool_*_after_schema_repaired_restore.sql` | **init** 灌库后的空/配置态快照 |
| `translationtool_*_after_keep_mon_cn_develop.sql` | **业务**：仅通用平台部 `mon-cn-1.9.0` + `develop` 子树 |
| `translationtool_*_keep_mon_cn_develop_current.sql` | 同上；当前 `.latest`（已做过 backup→restore 实跑） |

已删除：PS 管道损坏的大 dump、salvage 半残、extract/import 中间件、旧 pre_keep。  
源码侧 init schema 仍在 [`db/init/schema.sql`](../init/schema.sql)（不在本目录）。

## 推荐流程

```text
测试前 backup（verify 绿）→ 测试操作 → restore（verify 绿 + -Force）
```
