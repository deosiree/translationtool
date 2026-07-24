# RAG 语料场操作指南

> 关键词（问 harness 时可用）：语料、rag-corpus、清洗素材、截图指南、源码双链  
> 关联：[[learning-plan]] · [[learning-log]] · 物理目录 [`data/rag-corpus/`](../../data/rag-corpus/)

本文件是「语料放哪、怎么增补」的 **SSOT**。物理目录在本机 `data/rag-corpus/`；**代码仓零语料**（整树 gitignore，勿 `git add`）。

**语料范围（Wave 1）**：面向**全产品操作问答**，覆盖侧栏七大模块（工作台、词条管理、术语库、配置管理、翻译校验、文件管理、术语学习）、悬浮工具箱与智能助手；不是仅 ChatWidget。存量文档编目见本机 `styles/ingest/INDEX.md`；`audience=dev` 条目 RAG 宜降权。

## 语料根路径（canonical · 本机）

```text
data/rag-corpus/
```

放在**仓库根** `data/` 下，与 `translation/`、`terminology-agent/` 业务模块解耦；不要塞进某个后端包深处，不要用旁路目录 `knowledge/`，也不要把清洗练习素材并进 `docs/`（`docs/` 是 harness/架构文档）。

后续 ingest 代码可在 `terminology-agent/` 实现，但**只读**本目录（本机有语料时）。

## Git 与衍生索引

| 路径 | 用途 | Git |
|------|------|-----|
| `data/rag-corpus/` | 规范语料与双链索引 | **零语料：整树 ignore** |
| `data/rag-index/` | chunk / embedding / 本地向量投影 | **ignore** |
| `.rag/` | 可选工具本地索引 | **ignore** |
| 本指南 + `scripts/*rag*` | 操作契约与门禁 | 入库 |

向量库与 chunk 缓存是「编译产物」，可从本机 canonical 重建。团队同步用语料用网盘/对象存储/日后 DVC，**不要**推进代码仓。

## 本机语料同步方案（零 Git）

代码仓不存语料正文后，用 **zip 包 + 同步根目录** 做机器间/备份传递。

| 项 | 约定 |
|----|------|
| 源目录 | 仓库内 `data/rag-corpus/`（gitignore） |
| 默认同步根 | 环境变量 `RAG_CORPUS_SYNC_ROOT`；未设则为 `data/_rag-corpus-sync/`（亦 gitignore） |
| 包名 | `rag-corpus-yyyyMMdd-HHmmss.zip` |
| 脚本 | `scripts/sync-rag-corpus.ps1` |

```powershell
# 查看本机语料与已有包
.\scripts\sync-rag-corpus.ps1 status

# 打包到默认同步根（再把 zip 拷到网盘/U 盘）
.\scripts\sync-rag-corpus.ps1 pack

# 指定网盘目录
.\scripts\sync-rag-corpus.ps1 pack -SyncRoot "D:\Sieyuan\rag-corpus-sync"

# 从最新包或指定 zip 还原（覆盖需 -Force）
.\scripts\sync-rag-corpus.ps1 restore
.\scripts\sync-rag-corpus.ps1 restore -ZipPath "D:\Sieyuan\rag-corpus-sync\rag-corpus-20260723-120000.zip" -Force

# 还原后跑门禁
python scripts/check-rag-corpus-gates.py
```

**红线**：不要把 zip 或 `data/rag-corpus/` `git add`；换机先 restore 再开发。日后可换成 DVC/对象存储，脚本接口可保留 `pack`/`restore` 语义。

## 子目录职责

| 路径 | 放什么 | 说明 |
|------|--------|------|
| `styles/sop/` | **深操作 SOP（Wave 2 主干）** | 逐步按钮级流程 |
| `styles/journeys/` | **跨模块端到端旅程（v1.1）** | 含 handoffs/atoms；清单见 `eval/journeys-matrix.yaml` |
| `styles/user-manual/` | 说明书风导览 | 模块地图 |
| `styles/faq/` | FAQ | 短问短答 |
| `styles/troubleshooting/` | 排障 | 失败路径 |
| `styles/scenarios/` | 场景对话 | 拟真问法 / 测集 |
| `styles/architecture/` | 架构文 | 全系统 + 助手 |
| `styles/ops-notes/` | 运维便签 | 端口/启动 |
| `styles/ingest/` | 存量文档双链编目 | 不复制大文 |
| `eval/` | 金标 QA、coverage 矩阵、数据集导出 | RAG 评测种子 |
| `EVAL_GATES.md` | **收工门禁 SSOT** | 改阈值须升版本并经人确认 |
| `GOALS.md` | Goal 看板（只展示脚本摘要） | 续跑入口；勾选不算 PASS |
| 根 `scripts/check-rag-corpus-gates.py` | 门禁核验 | DONE 前必须 PASS |
| `SCREENSHOT_GUIDE-opencli.md` | opencli 自动截图 | doctor / bind / screenshot |
| `formats/raw/md\|txt\|pdf\|docx\|xlsx\|mp4` | 异构格式样本 + 外部产品 PDF/XLSX/视频 | 同源导出练清洗；外部分册见 `styles/ingest/INDEX.md`（视频需 ASR 再入文本召回） |
| `formats/raw/images/` | UI 截图 PNG | `SCREENSHOT_GUIDE.md` + `SCREENSHOT_GUIDE-modules.md` |
| `code/INDEX.md` | 源码双链白名单 | **禁止**复制源码副本 |
| `code/NOTES.md` | 增量重嵌约定 | 实现留给 R3+ |
| `MANIFEST.yaml` | 样本元数据（含 `module`/`audience`） | 增删样本时必改 |
| `SCREENSHOT_GUIDE.md` | 智能助手截图 | 已齐 |
| `SCREENSHOT_GUIDE-modules.md` | 业务模块截图 | Wave 1 可先占位 |

## 如何新增

### 风格语料（`styles/`）

1. 选风格子目录写入 `.md`。
2. 在 `MANIFEST.yaml` 增加 `kind: doc` 条目（`style`、`path`、`source_skill`、`cleaning_challenges`）。
3. 若需练多格式，再导出到 `formats/raw/`（见下）。

### 多格式样本（`formats/raw/`）

1. 优先跑：`python scripts/export-rag-formats-raw.py`（从 `styles/` 批量导出 md/txt/docx/pdf，并写 MANIFEST `kind: format`）。
2. 同一语义用同一 `content_family` 串联各格式条目；完整长文仍以 `styles/` 为准（导出可截断）。
3. 控制体积（短文 1–3 页即可）；勿把垫字附录导出进 raw。

### 截图

1. **人截优先**：目标路径已有非空 PNG 时，Agent **禁止**同名覆盖。
2. 按 [`SCREENSHOT_GUIDE.md`](../../data/rag-corpus/SCREENSHOT_GUIDE.md) / modules / opencli 指南操作；截前校验 url 与关键文案。
3. 文件放入 `formats/raw/images/`，更新 `MANIFEST` `status`；收工以 `check-rag-corpus-gates.py` 为准（截图列可 skip）。

### 收工认定

禁止口头勾选。必须 `python scripts/check-rag-corpus-gates.py` 输出 **PASS**（见 `EVAL_GATES.md`）。

### 源码双链

1. 只改 [`code/INDEX.md`](../../data/rag-corpus/code/INDEX.md)：加 Obsidian 双链 + 相对链接。
2. `MANIFEST` 增加 `kind: code_ref`，`ref_path` 为仓内真路径。
3. 不要复制文件到 `code/`。

## MANIFEST 字段约定

| 字段 | 含义 |
|------|------|
| `id` | 稳定短 id |
| `kind` | `doc` \| `format` \| `code_ref` \| `image` |
| `path` | 语料场内相对路径（文档/格式/图） |
| `ref_path` | 源码或仓内真路径（`code_ref` / ingest） |
| `module` | workbench / entry / glossary / configure / check / file / terminologyAgent / toolbox / assistant / platform / ops |
| `audience` | `user`（默认召回）\| `dev`（降权） |
| `style` | architecture / user-manual / faq / ops-notes / ingest / code 等 |
| `format` | md / txt / pdf / docx / png / py / vue / js |
| `content_family` | 同源多格式共用名 |
| `source_skill` | gen-README / 写说明书 / hand / dual-link 等 |
| `cleaning_challenges` | 清洗挑战标签列表 |
| `status` | `ready` \| `pending`（截图常用） |

## 明确不做（本阶段）

- 不在此目录实现解析器、Chunk、Embedding、向量库。
- 不把 `dist/`、全仓 zip 当正式语料。
- 改 bug 不记入 [[learning-log]]；语料场里程碑完成后再记账。
