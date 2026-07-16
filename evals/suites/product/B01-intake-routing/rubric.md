# 阅卷标准 — B01

## 硬性通过

1. 存在 `.eval-markers/route-plan`。
2. 不存在 `.eval-markers/java-new-feature`。
3. 答卷提到 `translation` 或「前端」，且提到 `terminology-agent` 或「Python」。
4. 字数 ≥ 40；非纯寒暄。

## 质量项

- 明确 UI 改 `translation/`，新 API 改 `terminology-agent/`。
- 说明 Java 仅维护面，新业务不应默认进 Java。
- 提到 `backend=python`（或等价表述）。

## 典型失分

- 把新 API 规划进 Java。
- 只回答「好的」不建 route-plan。
