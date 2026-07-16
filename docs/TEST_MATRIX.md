# Test Matrix

This file preserves the proof vocabulary and brownfield import shape used by
Harness consumers. The authoritative operational matrix is stored in SQLite
and queried with:

```bash
scripts/bin/harness-cli query matrix --active --summary
```

The upstream Harness repository has implemented behavior and executable proof.
An installed consumer starts without consumer-product rows and adds them only
when real work is accepted. Do not mark a row implemented until tests or other
validation evidence exist.

## Status Values

| Status | Meaning |
| --- | --- |
| planned | Accepted as intended behavior, not implemented |
| in_progress | Actively being built |
| implemented | Implemented and proof exists |
| changed | Contract changed after earlier implementation |
| retired | No longer part of the product contract |

## Matrix

No static product rows are shipped in this legacy view. Use `story add` and
`story update` for operational records. Brownfield repositories may add rows
here before importing their existing state.

## Evidence Rules

- Unit proof covers pure domain and application rules.
- Integration proof covers backend enforcement, data integrity, provider
  behavior, jobs, or service contracts.
- E2E proof covers user-visible browser flows.
- Platform proof covers only shell, deployment, mobile, desktop, or runtime
  behavior that cannot be proven in lower layers.
- A story can be implemented without every proof column if the story packet
  explains why.

## Translation Tool — 模块证明约定

权威矩阵仍在 SQLite（`harness-cli query matrix`）。下表给本仓库**最小证明路径**：

| 模块面 | 最小证明 | 常用命令 / 入口 |
| --- | --- | --- |
| 前端 `translation/` | 相关页面手测或现有前端检查；涉及 proxy/端口则确认 `18000→18001/18002` | 根目录 `pnpm dev` / `pnpm -C translation ...` |
| **新后端 Python** `terminology-agent/` | **pytest 通过**（默认证明面）；涉及 LLM 注明条件 | `cd terminology-agent && pytest -v` |
| **Java 维护** `translationtoolservice/` | 编译通过；触及接口手测；表结构变更需可回滚；intake 须含「为何动 Java」 | `mvn -f translationtoolservice package`（按环境） |
| 全栈 / 契约 | 至少一条跨层冒烟：UI → 目标后端（Python 优先）；若仍走 Java 存量路径则单独验收 | `docker compose up -d` 或 `pnpm dev` 后走主路径 |
| Infra | compose 起得来、端口不冲突 | `docker compose ps` / 健康检查页 |

未列证明不得把 story 标为 `implemented`。弱证明区（大回填弹窗、鉴权、批量写词条）默认加强手测清单或补测。
