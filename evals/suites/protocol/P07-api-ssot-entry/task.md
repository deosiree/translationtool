# 题面 — P07 API 契约 SSOT

用户问：「预翻译相关新接口要加一个响应字段，字段是否存在、是否必填，以哪里为准？能不能只看前端 `translation/src/http` 定契约？」

按本仓 Harness（`docs/API_CONTRACTS.md`）：

1. 指出 Python 新后端的 SSOT（OpenAPI /docs 或源码路径）。
2. 明确前端 http 封装是否可作为字段权威。
3. 创建标记文件 `.eval-markers/api-ssot-ok`，内容一行，例如：`ssot=python-openapi frontend=not-authority`。

请用中文回复摘要。
