# 学习日志

> 维护规则：
> 1. 仅记里程碑（新能力 / 阶段完成）；改 bug 不入账。  
> 2. 新条目用「`YYYY-MM-DD` + 标题」插在正文最上方（倒序追加）。  
> 3. **默认：一次「记住里程碑并提交」= 一条里程碑 + 一次 git commit**（该次提交的全部相关改动总结进同一时间戳标题下）。  
> 4. **仅当用户明确说「分批次写里程碑/分批提交」时**，才拆成多条里程碑；且仍须 **一提交 ↔ 一日志里程碑** 一一对应。

## 2026-07-23 RAG 语料场：从素材到可门禁测集 + 代码仓零语料

围绕智能助手后续 RAG，完成本机语料场建设、诚实收工门禁、跨模块操作旅程、模块截图齐套，以及「语料不进 Git」的仓内契约与本机同步方案。语料正文仅本机 `data/rag-corpus/`；仓内入库指南、门禁/旅程/同步脚本与本日志。

### 做了什么

- **语料范围**：从仅助手 → 全产品侧栏七大模块 + 工具箱；说明书 / FAQ / 排障 / 场景 / SOP / 架构 / ops / ingest 双链  
- **诚实门禁**：`EVAL_GATES`（v1.0→v1.1）+ `check-rag-corpus-gates.py`（去垫体量、覆盖矩阵、金标 split/溯源、journeys）；废除垫字附录计入  
- **跨模块旅程**：`styles/journeys/**` + `journeys-matrix.yaml`（含工作台→Excel 人工译→更新翻译→翻译审核→归档/回写闭环及 handoff/atoms）；控件级加厚对齐 Vue 原文  
- **多格式与截图**：`export-rag-formats-raw.py`；`SCREENSHOT_GUIDE-modules` 目标 PNG **本机已齐**（人审）  
- **代码仓零语料**：`data/rag-corpus/` 整树 gitignore；禁止 `git add` 语料正文  
- **本机同步**：`scripts/sync-rag-corpus.ps1`（pack/restore/status）+ 指南「本机语料同步方案」；默认同步根 `RAG_CORPUS_SYNC_ROOT` 或 `data/_rag-corpus-sync/`  
- **仓内脚本**：`check-rag-corpus-gates.py`、`expand-rag-corpus-wave.py`、`export-rag-formats-raw.py`、`gen-rag-journeys*.py`、`sync-rag-corpus.ps1`

### Harness

- intake **#15**（Harness improvement / tiny）  
- decision **`0013-rag-corpus-zero-in-git`**（accepted）  
- intervention **#1**（approval）· trace **#10–#11**

### 学到什么（面试可讲）

- canonical 语料 ≠ 向量库：源材料本机/对象存储，索引可重建且 ignore  
- 门禁要防「有文件即 done / 垫字过体量」；跨模块旅程比单模块 SOP 更贴近真实操作  
- Git 适合契约与脚本；语料用 zip/同步根传递，而不是塞进代码仓

### 明确未做与下一步

- 向量 hit@k / Chunk / Embedding 未接  
- 下一步：R2 Chunk；或把 `RAG_CORPUS_SYNC_ROOT` 指到网盘做日常 pack

### 关联

- [[learning-plan]] · `docs/product/rag-corpus-guide.md` · `.gitignore` · decision `0013`

## 2026-07-23 ChatWidget 智能助手 MVP

阶段 0 落地：全局智能助手可对话，作为后续 RAG / 记忆 / Agent 的 UI 入口。

### 做了什么

- 前端三态浮动组件：`hidden`（悬浮球）→ `open`（对话窗）→ `minimized`（边缘标签）；关闭回到 `hidden`
- 悬浮球与面板均可拖拽；位置写入 `localStorage`
- 布局根挂载，登录后全站可用
- Python 侧 `POST /agent/chat`：见下方「后端手段」
- 学习方案与 ADR 0012 一并沉淀；harness intake #13（助手）、#14（学习方案）已录入

### 后端手段（重要）

**不是 Agent，是「硬编码 System Prompt + 单次 LLM 直调」。**

| 项 | 现状 |
|----|------|
| 编排 | 无 LangGraph / ReAct / Tool Calling；无 `bind_tools` |
| 调用链 | FastAPI → 拼消息 → `ChatOpenAI.ainvoke(messages)` → 返回 `reply` |
| System Prompt | 模块内常量 `_SYSTEM_PROMPT`（角色/职责/回答约束），写死在 `chat.py` |
| 客户端 system | 忽略；只用服务端硬编码提示词 |
| 历史 | 前端回传；服务端只生成/回传 `session_id`，不落库、不检索 |

本质：带固定人设的 chat completion 封装，路径名是 `/agent/chat`，实现上尚未形成 Agent。

### 代码落点

- `translation/src/components/ChatWidget/index.vue`
- `translation/src/views/layout/layout.vue`
- `translation/src/http/api/terminologyAgent.js`
- `terminology-agent/app/api/chat.py`
- `terminology-agent/app/schemas/chat.py`

### 学到什么（面试可讲）

- 双后端边界：新能力落 Python（`terminology-agent`），前端只对接 `/agent/*`
- 无状态对话 API：会话 ID + 客户端回传历史，服务端暂不落库
- 产品入口先行：先打通「能聊」，再叠 RAG / 记忆，避免一开始就上检索栈
- 「路径叫 agent ≠ 已是 Agent」：当前证据链只到 LLM 直调，Agent/Tool 在总 plan Phase 3

### 明确未做与下一步

**未做（相对完整智能助手）：**

- 无检索增强：不读 `docs/` / 术语库 / 业务库，回答全靠模型参数知识 + 硬编码提示词
- 无向量库 / Embedding / Chunk / Rerank
- 无短期记忆压缩、无长期记忆写入与召回
- 无 Tool / Function Calling、无 ReAct 循环
- 无流式输出（SSE/WebSocket）；整包 JSON 一次返回
- 无服务端会话持久化；`session_id` 仅作客户端关联键
- harness 仅有 intake #13/#14，未升 story / matrix / API 契约条目

**下一步（按 [[learning-plan]] Phase 1）：** 见上一条里程碑（语料场已推进）；再往 R2 Chunk / Embedding。

### 关联

- 路线图：[[learning-plan]]
- 决策：`docs/decisions/0012-learning-driven-development.md`
- Harness：intake #13、#14

# 总计划

[[learning-plan]]
