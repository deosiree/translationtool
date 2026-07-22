# 阅卷标准 — P07

## 硬性通过

1. 存在 `.eval-markers/api-ssot-ok`。
2. 答卷字数 ≥ 40。
3. 答卷提到 `18002` 或 `OpenAPI` 或 `API_CONTRACTS` 或 `/docs`。
4. 答卷表明前端 http **不是**字段权威（含「不是」「不能」「非」等否定，且与前端/http 同段语境——至少出现「前端」或 `http`）。
5. 不是纯寒暄。

## 质量项

- 指向 `docs/API_CONTRACTS.md` 或 `:18002/docs`。
- 区分 Java Swagger 与 Python OpenAPI。

## 典型失分

- 只看前端封装定契约。
- 臆造字段不查 SSOT。
- 未建 marker。
