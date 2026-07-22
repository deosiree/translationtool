# 业务 API 契约怎么找

本文件是 Harness / Agent 找 **API 字段与路径** 的入口。改请求/响应形状前先查本页指向的 SSOT，禁止臆造字段。

## 唯一真相源（SSOT）

| 业务面 | SSOT | 说明 |
| --- | --- | --- |
| **Python 新后端**（`:18002`） | 运行中 OpenAPI：`http://localhost:18002/docs`；源码 [`../terminology-agent/app/api/`](../terminology-agent/app/api/) | 新需求 / 新 API 默认落此面。服务未起时以路由与 pydantic 模型源码为准，启动后以 `/docs`（及 `/openapi.json`）为准。 |
| **Java 存量后端**（`:18001`） | 服务端 Swagger（见 `translationtoolservice` 的 `SwaggerConfig`）；Controller 源码为实现对齐参考 | **仅维护面**。字段是否存在 / required 以启动后 swagger 为准；未启动时读对应 Controller / DTO，并在决策或 story 中注明「待 swagger 复核」。 |
| **Harness 编排**（非业务 HTTP） | [`contracts/harness-orchestration-v1.md`](./contracts/harness-orchestration-v1.md) | 给 `harness-cli` / orchestrator 用；**不是**词条业务 API 的 SSOT。 |

## 约定

1. 问「有没有某字段 / 是否必填 / 路径是什么」时：**只信**上表对应 SSOT。
2. 前端 `translation/src/http`（或等价 API 封装）是客户端适配，**不能**单独当字段权威；与 SSOT 冲突时先修契约或适配，再改实现。
3. 跨 Java↔Python 或破坏性变更：先 [`FEATURE_INTAKE.md`](./FEATURE_INTAKE.md)（含 `backend=*`），再必要时写 [`decisions/`](./decisions/)，再改代码。
4. [`contracts/`](./contracts/) 目录仅 harness 机器契约；勿把业务 HTTP 约定只写在那里却不更新 OpenAPI / Swagger。

### 失败分支

| 触发 | 一线 | 兜底 |
| --- | --- | --- |
| 服务未起、打不开 `:18002/docs` | 读 `terminology-agent/app/api/` 源码，并在答复中注明「待 OpenAPI 复核」 | 🔴 CHECKPOINT：需要字段权威时先起服务 |
| Java swagger 未开 | 读 Controller / DTO，注明「待 swagger 复核」 | 维护面须人类确认时 🛑 不停猜字段 |
| 前端与 SSOT 冲突 | 以 SSOT 为准改适配层 | 禁止静默改 SSOT 去迁就前端 |

### 反例黑名单

| 不要做 | 要做 |
| --- | --- |
| 只看前端 http 封装定字段 | 查本页 SSOT 表 |
| 把 `docs/contracts/` 当业务 HTTP SSOT | 仅用于 harness-cli 编排 |
| 臆造字段再补文档 | 先 SSOT，再实现 |

## 查法

1. 用本页确认业务面与 SSOT。
2. Python：打开 `:18002/docs` → Paths / Schemas；或读 `terminology-agent/app/api/*.py`。
3. Java：打开服务 Swagger UI（以当前环境配置为准）→ paths / schemas；或读 `translationtoolservice/.../controller` 与实体。
4. 仅当需要「业务规则为何如此」时再读 Service / Agent 图实现；**字段定义**仍以 swagger / OpenAPI 为准。

## 与其它文档的关系

| 位置 | 角色 |
| --- | --- |
| 本文件 | Agent / 人类找 SSOT 的**入口** |
| `terminology-agent` OpenAPI | Python 业务 API **SSOT** |
| Java Swagger | 存量业务 API **SSOT** |
| `docs/contracts/` | Harness CLI 编排协议，非业务 HTTP |
| [`ARCHITECTURE.md`](./ARCHITECTURE.md) | 模块边界与端口，不替代字段级契约 |
| [`QUALITY_LOOP.md`](./QUALITY_LOOP.md) | 改契约后如何证 DONE |

## 常见问法

| 问题 | 先查 |
| --- | --- |
| 新预翻译 / Agent 接口字段 | Python `:18002/docs` 或 `terminology-agent/app/api/` |
| 旧工作台 / 词条存量接口 | Java Swagger + 对应 Controller |
| `harness-cli` JSON 字段 | `contracts/harness-orchestration-v1.md` |
| 该改 Java 还是 Python | [`FEATURE_INTAKE.md`](./FEATURE_INTAKE.md) + ADR `0009` / `0010` |
