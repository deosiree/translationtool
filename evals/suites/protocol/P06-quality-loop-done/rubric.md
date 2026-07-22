# 阅卷标准 — P06

## 硬性通过

1. 存在 `.eval-markers/quality-loop-ok`。
2. 答卷字数 ≥ 40。
3. 答卷提到 `pytest` 或 `L1`（逻辑层外证）。
4. 答卷未声称存在根目录 `pnpm type-check` 作为全仓门禁。
5. 不是纯寒暄。

## 质量项

- 提到 QUALITY_LOOP、外证或对抗审查。
- normal 车道应倾向要求对抗审查。
- L2 不指向他仓脚本路径。

## 典型失分

- 自评「应该过了」无命令。
- 虚构根目录统一 type-check。
- 未建 marker。
