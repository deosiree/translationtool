# 学习日志

> 维护规则：仅记里程碑（新能力 / 阶段完成）；改 bug 不入账。
> 新条目用「`YYYY-MM-DD` + 标题」插在正文最上方（倒序追加）。

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

**下一步（按 [[learning-plan]] Phase 1，下个可记账里程碑方向）：**

1. **R1 语料清洗**：把平台文档整理成可入库素材
2. **R2–R3 Chunk + Embedding**：切分策略 + 向量库（如 FAISS / Milvus Lite）
3. **R4–R5 多路召回 + 精排**：Dense+BM25 → RRF，再 Cross-Encoder
4. **把检索结果注入现有 `chat` 提示词**：从「纯 LLM」升级为「RAG 增强对话」（仍可先不引入 Agent）
5. Agent/Tool（Phase 3）、长短期记忆（Phase 2）排在 RAG 入口打通之后

### 关联

- 路线图：[[learning-plan]]
- 决策：`docs/decisions/0012-learning-driven-development.md`
- Harness：intake #13、#14

# 总计划

[[learning-plan]]
