# 术语验数卡 · before_term_syk_verify

生成时间：2026-07-18  
库状态：keep（mon-cn-1.9.0 + develop）+ 本验数种子  
Darwin：`db-回滚数据库` Phase1 基线 **过期**（90.2 @ 2026-07-02）；本次**未改 skill**，只灌数 + backup。

## 登录 / 产品 / 任务

| 项 | 值 |
| --- | --- |
| 用户 | `admin` / 部门 `通用平台部` |
| 产品 id | `a2128cfc-14f2-46ab-930e-76350aaf0255` |
| 产品名 | **admin**（你新建的产品） |
| 任务 id | `verify-syk-admin-task` |
| 任务名 | **`verify-syk-admin`** |
| 语种 | 英文 |
| 人员 | creator / developer / entry_auditor / translator / translation_auditor **均为 `admin`** |
| 词条态 | `entry_state=3`（词条审核通过）；勿用 `0`，否则 `/taskManage/getTaskPending` 报系统服务异常 |
| 种子脚本 | [`db/opt/seed-verify-syk-admin-product.sql`](../opt/seed-verify-syk-admin-product.sql) |

## A. 工作台「术语库翻译」（Java SYK）

打开产品 **admin** → 任务 **`verify-syk-admin`** → 选下列词条 → 翻译引擎选 **术语库**。

| entry_info id | 原文 | 预期术语库译文 | 说明 |
| --- | --- | --- | --- |
| `verify-admin-syk-exact` | `VERIFY/SYK-exact-用户登录` | `SYK-HIT-User Login` | 精确命中（`t_translate` state=3） |
| `verify-admin-syk-exact2` | `VERIFY/SYK-exact-权限管理` | `SYK-HIT-Permission Mgmt` | 精确命中 |
| `verify-admin-syk-exact3` | `VERIFY/SYK-exact-数据备份` | `SYK-HIT-Data Backup` | 精确命中 |
| `verify-admin-syk-miss` | `VERIFY/SYK-miss-全新句子XYZ` | （无） | 对照：术语库不应命中 |

肉眼确认：命中行译文带 `SYK-HIT-` 前缀，而非百度/有道常态译文。

## B. Agent 多检索路径（已 `verify_adm_pretranslate --strict` 绿）

| 词条 | comment | 预期 retrieval | 预期 source | 预期 review |
| --- | --- | --- | --- | --- |
| `ADM/R01-RAG精确` | | exact | term | auto_approved |
| `ADM/R04-RAGGREP一致` | | exact | term | auto_approved |
| `ADM/S02-RAG模糊-用户管理系统` | ADM-S02 | fuzzy \| none | llm \| term | needs_human |
| `文件、系统、资源` | | decomposed | hybrid | needs_human \| auto_approved |
| `文件与系统` | | decomposed | hybrid | needs_human \| auto_approved |
| `T99-全新未收录` | ADM-T99 | none | llm | needs_human |

已审词片（`term_word`）：`文件`→File、`系统`→System、`资源`→Resource；R04 整句 Grep 种子已入库。

UI：术语学习菜单（id=16）已授权；可在预翻译/术语学习侧对照上述 6 行。

## 种子脚本

- [`db/opt/seed-verify-term-syk.sql`](../opt/seed-verify-term-syk.sql)
- 另已执行：`add-term-word.sql`、`add-term-agent-audit.sql`、`add-menu-terminology-agent.sql`
- 列补丁：`t_entry_info.segment_trace`、`term_agent_audit.segment_trace`、`term_agent_audit.entry_comment`

## 备份

见同目录 `translationtool_*_before_term_syk_verify.sql`（`.latest` 指向它）。  
测完回滚：

```powershell
$skill = "F:\Documents\Default-Obsidian\huiyanSkills\translateTool-skills\db-回滚数据库"
$root  = "F:\Documents\Repertory\Sieyuan\translationtool"
& "$skill\scripts\restore-database.ps1" -ProjectRoot $root -UseLatest -Force
```
