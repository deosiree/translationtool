# 学习方案：AI 大模型应用落地实战

> 创建：2025-07-17 | 状态：进行中 | 关联决策：`docs/decisions/0012-learning-driven-development.md`
>
> 进度里程碑（倒序）见 [[learning-log]]。本文只维护总路线图与推进规则。

## 目标

以「思源电气翻译工具」为实战项目，系统学习 AI 大模型应用落地的全流程，重点深挖 **RAG** 和 **长短期记忆** 两个方向，目标是应对大模型应用/算法工程师面试。

## 面试策略

- **全流程广度**：Transformer → 大模型选型 → Prompt 工程 → RAG → Agent/Tool → 记忆系统 → 推理优化 → 微调对齐，每个环节都能讲清楚原理和实战经验
- **两个深挖点**：RAG（多路召回 + 融合 + 精排 + 评估）+ 长短期记忆（写入策略 + 召回 + 反思）
- **训练类**（SFT/RLHF/LoRA）：只读论文和博客，面试能讲原理即可，不做实战

## 当前进度（摘要）

- **已完成**：阶段 0 — ChatWidget MVP + `POST /agent/chat`（硬编码 System Prompt + LLM 直调，非 Agent）。详情见 [[learning-log]]
- **进行中 / 下一步**：Phase 1 RAG（R1 语料清洗起）
- 勿在本文追加日期条目；新里程碑只写入 [[learning-log]] 文首

## 待做（按优先级排列）

### Phase 1：RAG 核心（深挖点一）

| # | 任务 | 覆盖知识点 |
|---|------|-----------|
| R1 | 数据清洗管线：将 `docs/` + `README.md` + 源码注释整理为结构化的文档素材 | 数据清洗 |
| R2 | Chunk 策略：实现固定大小切分 + SmallToBig 父子档，对比效果 | Chunk 设计 |
| R3 | Embedding 接入：BGE-M3 / 千问 Embedding → Milvus Lite / FAISS | Embedding 模型 |
| R4 | 多路召回：Dense（向量）+ Sparse（BM25）→ RRF 融合 | 混合检索 |
| R5 | 精排层：Cross-Encoder Reranker（BGE-Reranker）对 Top-N 重排 | Rerank |
| R6 | Query 优化：Query 改写 + HyDE（先生成假答案再检索） | HyDE |
| R7 | RAG 评估：RAGAs 跑 faithfulness / context recall / precision / relevancy | 评估体系 |

### Phase 2：长短期记忆（深挖点二）

| # | 任务 | 覆盖知识点 |
|---|------|-----------|
| M1 | 短期记忆管理：滑动窗口 + Token Truncation + LLMLingua 压缩 | 上下文工程 |
| M2 | 长期记忆写入：LLM 打分判断 + 结构化抽取 `{type, content, importance}` | 记忆写入 |
| M3 | 长期记忆召回：向量检索 + 重要性过滤 → 注入对话上下文 | 记忆召回 |
| M4 | 记忆反思：N 轮对话后自动总结 → 抽象为高层记忆 | 反思写入 |

### Phase 3：Agent 与工具调用（广度）

| # | 任务 | 覆盖知识点 |
|---|------|-----------|
| A1 | Tool Schema 注册：`@tool` 装饰器 Function Calling | Tool 注入 |
| A2 | ReAct Agent：思考→行动→观察→思考 循环 | Agent 范式 |
| A3 | Tool 异常处理：重试 / fallback / 参数校验 / 超时 | Tool 鲁棒性 |

### Phase 4：提示词工程（广度）

| # | 任务 | 覆盖知识点 |
|---|------|-----------|
| P1 | 提示词模板化：角色 + 指令 + Few-shot + 约束 | Prompt 体系 |
| P2 | 提示词 A/B 测试框架 | 工程化管理 |

### Phase 5：理论补强（只读不写）

| # | 任务 | 覆盖知识点 |
|---|------|-----------|
| T1 | Transformer 核心机制：Attention / FFN / RMSNorm / RoPE / GQA | 架构原理 |
| T2 | 大模型架构范式：MoE / Sparse Attention / SSM | 架构对比 |
| T3 | 推理优化：FlashAttention / KV Cache / PagedAttention / Continuous Batching | 推理引擎 |
| T4 | 微调对齐：SFT / LoRA / QLoRA / RLHF / DPO / GRPO | 训练方法 |

## 推进规则

当用户说「继续学习」「下一步做什么」或类似触发词时，Agent 应：

1. 读 `docs/product/learning-plan.md` 获取路线图
2. 读 `docs/product/learning-log.md` 获取最新里程碑
3. 读 `scripts/bin/harness-cli query matrix --active --summary` 获取活跃任务
4. 检查 ChatWidget 当前状态（是否需要 bugfix、是否需要推进下一阶段）
5. 给出 2-3 个具体的下一步选项，让用户选择方向
6. 选中后，创建 plan（story 或 task），开始实施；完成后在 [[learning-log]] 文首追加里程碑（改 bug 不入账）

## 关联资源

- 学习日志：[[learning-log]]
- 面试知识体系：`F:\Documents\Default-Obsidian\语言\AI面经\葵花宝典.md`
- 项目架构：`docs/ARCHITECTURE.md`
- 质量门禁：`docs/QUALITY_LOOP.md`
- ChatWidget 组件：`translation/src/components/ChatWidget/index.vue`
- Chat API：`terminology-agent/app/api/chat.py`
