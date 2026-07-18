# 验数卡 · 产品 admin 多检索矩阵

生成：2026-07-18  
种子：[`db/opt/seed-verify-admin-retrieval.sql`](../opt/seed-verify-admin-retrieval.sql)  
Skill：`工作台验数播种`（custom apply）  
Harness 题：`evals/suites/product/B02-workbench-verify-seed`

## 登录 / 产品 / 任务

| 项 | 值 |
| --- | --- |
| 用户 | `admin` / 通用平台部 |
| 产品 | **admin** / `a2128cfc-14f2-46ab-930e-76350aaf0255` |
| 任务 | **verify-admin-retrieval** / `verify-admin-retrieval-task` |
| 词条态 | `entry_state=3`，`en_trans_id=NULL` |
| 人员 | 五角色均为 `admin` |

## 矩阵（与 verify_adm_pretranslate 对齐）

| 原文 | 预期 retrieval | 预期 source | review |
| --- | --- | --- | --- |
| ADM/R01-RAG精确 | exact | term | auto_approved |
| ADM/R04-RAGGREP一致 | exact | term | auto_approved |
| ADM/S02-RAG模糊-用户管理系统 | fuzzy \| none | llm \| term | needs_human |
| 文件、系统、资源 | decomposed | hybrid | needs_human \| auto_approved |
| 文件与系统 | decomposed | hybrid | needs_human \| auto_approved |
| T99-全新未收录 | none | llm | needs_human |

## 一键

见 `docs/ops/DEV_DB_CHECKPOINT.md`「多检索验数矩阵」。
