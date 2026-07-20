# 0006 — Phase 4 基准测试分诊

Date: 2026-05-31

## Status

Accepted

## Context

首次 Phase 4 benchmark 重跑发现，T4 authentication 未通过 `decision_recorded`，尽管 trace 中包含 decisions 文本。同一次运行还出现命令反复试错：Agent 尝试用 `yes` 与 `no` 调用 `story update` 的 proof flags，并尝试像 proof flags 那样使用 `story verify`。

## Decision

Harness 说明与 CLI help 必须区分 durable records 与 trace evidence，并在 Agent 需要时展示当前 Rust CLI 命令形态：

- 高风险行为变更须在 `docs/decisions/` 下有一份 markdown decision，并有一条 durable `decision` row。
- Trace 的 `--decisions` 是 trace quality 的证据，不是 decision log。
- `story update` 的 proof flags 使用 `1` 与 `0`。
- `story verify <id>` 仅运行配置的 `verify_command`；proof flags 留在 `story update`。

## Alternatives Considered

1. 依赖 trace auto-scoring 捕获缺失的 T4 decision — 否决：trace scoring 可确认 trace 内容细节，但无法证明 durable decision record 存在。
2. 修改 CLI 接受 `yes` 与 `no` — 暂缓：v0.1.5 已有 numeric command contract，当前 benchmark 问题是过时指引，而非缺失 parser 能力。

## Consequences

Positive:

- 高风险 Agent 在收尾前获得明确的 decision-log 说明。
- 命令示例与 Rust CLI v0.1.5 parser 对齐。
- 文档中 `story verify` 与 `story update` 有分离的心智模型。

Tradeoffs:

- 文档现重复少量命令示例，使常见路径可见，无需反复查 help。

## Follow-Up

- 重跑 Phase 4 benchmark，检查 T4 是否记录 durable decision。
- 继续关注 `story update` 与 `story verify` 周围的命令 churn。
