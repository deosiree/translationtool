# 本地开发：数据库检查点（测试前备份 / 测后整库恢复）

开发中常有「预翻译几条 / 术语同意几条 / 随便点点」后要**整段撤销**的需求。本仓库约定：**先整库备份，再操作，再按备份恢复**；不要默认靠逐条 DELETE。

权威操作 skill（外部）：`huiyanSkills/translateTool-skills/db-回滚数据库`  
本仓备份目录：[db/backups/](../../db/backups/README.md)

## 红灯 / 绿灯（编码）

| 红灯（禁止） | 后果 |
| --- | --- |
| PowerShell `>` / `Out-File` / `Set-Content` 接 `mysqldump` | OEM 解码 → UTF-16 或中文 COMMENT 截断 → import **1064** |
| `Get-Content \| docker exec mysql` | 二次破坏 UTF-8 |

| 绿灯（锁定） | 说明 |
| --- | --- |
| 容器内 `mysqldump --result-file` + `docker cp` | 脚本默认路径 |
| 恢复：`docker cp` + `mysql --default-character-set=utf8mb4 < file` | 禁止 PS 管道喂入 |
| backup/restore 前 `verify-dump-encoding` | 失败则 **不得声称成功**；坏文件删除且不写 `.latest` |

**已损坏的 `db/backups/*.sql`（COMMENT 乱码）作废**，无法可靠转码救回；须用新脚本重新备份。空库需完好源或业务重灌后再建检查点。

## 何时建检查点

| 场景 | 动作 |
| --- | --- |
| 即将手工预翻译 / 术语学习同意 / 改工作台译文 | **先 backup** |
| Agent 说「备份数据库 / 准备回滚点 / 测试前备份」 | 执行 `backup-database.ps1`，把 `backupPath` 回给用户；若 retention 到期先问清理 |
| 用户说「操作完了，回滚」 | 展示 `.latest`（或指定文件）→ **人类确认** → restore `-Force`（先 verify） |
| 用服务器备份还原通用平台部 mon-cn-1.9.0 / develop | `keep_classify_restore`：`restore-keep-classifies.ps1`（先 extract，禁 all-DB 直灌） |
| 仅 ADM 矩阵污染（retrieval 全变 exact） | 优先 `adm_matrix_reset`，不必整库 DROP |
| 按时间窗清术语学习（非整库） | `term_day_cleanup` dry-run → 确认 |
| 生产 / 远程库 | **禁止**自动 restore；只输出命令 |

## 标准口令（对话）

```text
备份数据库 / 准备回滚点 / 测试前备份
（用户操作）
操作完了，恢复到刚才的备份 / 回滚
```

Agent 必须**实际执行** skill 脚本（`backup-database.ps1` / `restore-database.ps1`），不可手写管道命令，不可只贴命令假装完成。  
verify 失败或 import 失败时：**明确报失败**，不得声称「已备份 / 已回滚」。

## 本地验数任务：人员字段（硬约束）

向 `t_task_info` **INSERT/手工建任务**（含 SQL 种子、验数脚本）时，下列字段**一律填齐**，禁止只写 `creator`：

| 字段 | 含义 | 本地验数默认 |
| --- | --- | --- |
| `creator` | 创建人 | `admin` |
| `developer` | 开发员 | `admin` |
| `entry_auditor` | 词条审核员 | `admin` |
| `translator` | 翻译员 | `admin` |
| `translation_auditor` | 翻译审核员 | `admin` |

缺任一角色会导致 UI 任务流/权限过滤异常。参考种子：[`db/opt/seed-verify-term-syk.sql`](../../db/opt/seed-verify-term-syk.sql)。

完整流程（建任务 → 设计词条 → 挂产品 → 下发进翻译 → 验证）见 skill **`工作台验数播种`**（`huiyanSkills/translateTool-skills/工作台验数播种`）。

## 本地验数词条：entry_state（硬约束）

向 `t_entry_info` **INSERT/种子**且要进工作台「翻译」时：

| 字段 | 要求 |
| --- | --- |
| `entry_state` | 必须为 **`3`（词条审核通过）**；禁止 `0`（新建） |
| 目标语种 `*_trans_id` | 待译时保持 `NULL` |

`entry_state=0` 会令 `/taskManage/getTaskPending` → `TaskStateEntity.convertFrom` 抛 RuntimeException，前端表现为 `code:201`「系统服务异常」。

完整流程见 skill **`工作台验数播种`**。

## 多检索验数矩阵（工作台 + Agent）

针对预翻译多种索引/检索路径，本地标准矩阵与 `terminology-agent/devtools/verify_adm_pretranslate.py` 一致：

| 原文（工作台词条） | 预期 retrieval | 预期 source | 说明 |
| --- | --- | --- | --- |
| `ADM/R01-RAG精确` | exact | term | RAG 精确 |
| `ADM/R04-RAGGREP一致` | exact | term | Grep/整句一致 |
| `ADM/S02-RAG模糊-用户管理系统` | fuzzy \| none | llm \| term | 模糊/未命中，需人审 |
| `文件、系统、资源` | decomposed | hybrid | 切分 + term_word |
| `文件与系统` | decomposed | hybrid | 切分 |
| `T99-全新未收录` | none | llm | 全新句 |

产品 **admin** 一键挂载（五人员 + `entry_state=3` + relation）：

```powershell
$skill = "F:\Documents\Default-Obsidian\huiyanSkills\translateTool-skills\工作台验数播种"
$root  = "F:\Documents\Repertory\Sieyuan\translationtool"
& "$skill\scripts\apply-workbench-verify-seed.ps1" -ProjectRoot $root -SeedProfile custom `
  -SeedSqlPath "$root\db\opt\seed-verify-admin-retrieval.sql"
& "$skill\scripts\verify-workbench-translate-ready.ps1" -ProjectRoot $root `
  -TaskId "verify-admin-retrieval-task" -ProductId "a2128cfc-14f2-46ab-930e-76350aaf0255" -ExpectedEntryCount 6
# Agent 路径验收（需术语库/term_word 种子已在）：
cd "$root\terminology-agent"; python -m devtools.verify_adm_pretranslate --strict
```

术语库污染时先 `db-回滚数据库` 的 `reset-adm-matrix.ps1 -Apply`，再跑上表播种。

Harness 行为考试：`evals/suites/product/B02-workbench-verify-seed/`（考察是否走播种 skill、而非整库 restore / 漏人员 / entry_state=0）。

## 命令（Windows / 本地 docker MySQL）

```powershell
$skill = "F:\Documents\Default-Obsidian\huiyanSkills\translateTool-skills\db-回滚数据库"
$root  = "F:\Documents\Repertory\Sieyuan\translationtool"

# 备份（result-file + docker cp + encoding verify）
& "$skill\scripts\backup-database.ps1" -ProjectRoot $root -Label "before_<short_reason>"

# 编码冒烟（可选更严）
& "$skill\scripts\verify-dump-encoding.ps1" -BackupPath "<path.sql>" -SmokeImport

# 看清单（可能弹出月度保留提醒）
& "$skill\scripts\list-backups.ps1" -ProjectRoot $root

# 恢复（须用户确认后再 -Force；脚本内会先 verify）
& "$skill\scripts\restore-database.ps1" -ProjectRoot $root -Force

# 月提醒：拒绝清理则 snooze；确认后 prune
& "$skill\scripts\remind-backup-retention.ps1" -ProjectRoot $root -SnoozeDays 30
& "$skill\scripts\prune-backups.ps1" -ProjectRoot $root -OlderThanDays 30 -ConfirmDelete

# 编码 roundtrip 验收（独立 smoke 库，不碰业务数据语义）
& "$skill\scripts\roundtrip-encoding-smoke.ps1" -ProjectRoot $root

# 指定分类保留还原（服务器 all-databases → 抽出 → keep 子树）
# 禁止把 --all-databases 整文件交给 restore-database.ps1
& "$skill\scripts\inspect-classify-keep.ps1" -ProjectRoot $root -Database translationtool
& "$skill\scripts\restore-keep-classifies.ps1" -ProjectRoot $root `
  -DumpPath "$root\db\backup-scripts-and-latest\mysqlBackup\backup-2026-07-13-17-30-01.sql" `
  -ClassifyNames @("mon-cn-1.9.0","develop") -Department "-" -Force
```

## 服务器 dump 对照（Harness）

| 能力 | 服务器脚本（`db/backup-scripts-and-latest/`） | 本仓 harness | 策略 |
| --- | --- | --- | --- |
| 备份 | `--all-databases` + shell `>` | 单库 `--result-file` + `docker cp` | **保持单库**；服务器 dump 仅作数据源 |
| 删旧备份 | 可静默删 | 人审 `prune-backups` | **保持人审** |
| 多节点 scp | 有（脚本有变量/引号风险） | 无 | **不做** |
| 还原 | 无 | `restore-database` + verify | + **`keep_classify_restore`** |
| 查询 | 无 | `list-backups` | + `inspect-classify-keep.ps1` |

**红灯补充**：`--all-databases`（含 `USE mysql`）禁止直灌本机；抽出用 `extract-database-from-all-dump.ps1`。抽出段缺少 `@OLD_TIME_ZONE` 初始化时，导入尾部会 `SET time_zone=NULL` 失败——`rewrite_dump_database.py` 已消毒。

可选 Windows 计划任务（**只提醒、不删文件**）：

```powershell
& "$skill\scripts\remind-backup-retention.ps1" -RegisterTaskHint
```

## 每月备份清理（人审）

- 状态文件：`db/backups/.retention-state.json`（`nextDueAt` 默认 +30 天）
- Agent 在 backup / list-backups 发现到期时：**必须先问用户**是否删除旧/全部 `.sql`
- 用户确认 → `prune-backups.ps1 -ConfirmDelete`；拒绝 → snooze 30 天
- **禁止静默删除**

## 预翻译手工验收会碰到的表（供理解，回滚仍推荐整库）

US-3E-01 之后，走过切分的预翻译可能写入：

- `term_agent_audit`（含 `auto_approved` + `segment_trace`）
- `t_entry_info.segment_trace`
- `t_translate`（auto_approved 工作台 sync）

逐表手撕易漏；**默认 backup + restore**。

## Harness 关系

- 只读问答：不要擅自 backup/restore。
- 用户明确要求建检查点或恢复：可写 `db/backups/`（已 gitignore `*.sql`），并在回复中给出路径与大小。
- Intake 不因此升 lane；属运维辅助，不是业务 story。若反复需要，保持本文件为 SSOT，勿在多处复制长脚本。
