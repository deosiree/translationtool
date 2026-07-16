# 0010 — 双后端：工作台新需求走 Python；仅扩展旧 API 才动 Java

Date: 2026-07-16

## Status

Accepted

## Context

ADR 0009 规定新能力默认落 Python、Java 仅维护。实践中仍易混淆两点：

1. **UI 在工作台 ≠ 后端必须是 Java。** 工作台页可以调 Python 新 API。
2. **「列表读路径今天是 Java」≠「新字段必须改 Java」。** 若是工作台**新能力**，应新增 Python API（真源可在 Agent 表，或由 Python 读受控数据源），前端对接 `:18002`；而不是默认去改遗留列表接口。

上一版草案曾把「工作台持久列」误写成「几乎必然 `java-maintain` Entity 透传」，与 0009 及本仓库演进方向冲突，故更正。

## Decision

### 后端落点（与 0009 对齐，口径收紧）

| 场景 | 后端面 | 说明 |
| --- | --- | --- |
| 工作台**新需求**、**新 API**、新展示字段/诊断能力 | `backend=python` | 在 `terminology-agent/` 增加路由/契约；前端打 Python（或 proxy） |
| Agent / 术语学习等本就 Python 的面 | `backend=python` | 默认 |
| **给旧 Java API 增加入参/出参**、修遗留 bug、安全与不得不兼容 | `backend=java-maintain` | 仅此等场景主动改 `translationtoolservice/` |
| 为省事去改旧列表 SQL「顺带加一列」承载新能力 | **禁止作默认** | 除非人类确认「必须挂在旧接口上」 |

### 持久化可见面（仍要分拣，但不等于绑死 Java）

| 层 | 含义 | 默认做法 |
| --- | --- | --- |
| **A. Agent 真源** | 新语义落 Agent 自有表 | Python 表 + 术语学习等 Agent UI |
| **B. 会话可见** | 预翻译当次 `agent_meta` / 弹窗行 | Python API + UI，零 Java |
| **C. 工作台持久可见** | 重进任务后工作台仍要看到 | **优先新 Python API**（读 A 或受控库）供工作台列/侧栏消费；**不是**默认改旧 Java 列表 |

Intake 口令：

1. 这是**新能力/新 API**，还是**必须改某个已有 Java 接口的入参/出参**？
2. 关弹窗后还要在工作台看到吗？→ 否：A（+B）；是：C，但仍优先 Python 新 API。
3. 仅当产品/兼容强制「必须挂在旧 Java API 上」时，才标 `java-maintain` 并论证。

## Alternatives Considered

1. 工作台凡持久列一律改 Java Entity — **否决作默认**（扩大他人主责面，违背 0009）。
2. 新能力只写 `t_entry_info`、指望旧列表 `select *` 自动带出且不改 Java — 不可靠；且把新语义塞进 Java 表易成隐式契约，优先 Agent 表 + Python 读接口。
3. 把长诊断塞进 `audit_suggest`（255）— 否决作结构化真源。

## Consequences

Positive:

- 工作台也可以是 Python 新 API 的一等消费者，不再误判「工作台 = Java」。
- Java 改动面收束到「旧 API 加参 / 维护」，与他人主责边界清晰。

Tradeoffs:

- 工作台页可能同时打 Java（存量）+ Python（新列/新能力），前端要接受双通道。
- 若强行复用旧列表接口承载新字段，才进入 `java-maintain`，须人类确认。

## Follow-Up

- `FEATURE_INTAKE.md` / `ARCHITECTURE.md` 按本口径修订（纠正「C ⇒ 必改 Java」）。
- 切分轨迹 MVP：A+B；若升级工作台持久可见：优先 Python 读接口，而非改旧列表。
