# 测试矩阵（Test Matrix）

本文件保留 Harness 消费者使用的证明词汇与棕地导入形态。权威运营矩阵存放在 SQLite，查询方式：

```bash
scripts/bin/harness-cli query matrix --active --summary
```

上游 Harness 仓库已有已实现行为与可执行证明。已安装的消费者起始时没有消费者产品行，仅在真实工作被接受后添加。在测试或其他验证证据存在之前，不要把行标为已实现。

## 状态值

| 状态 | 含义 |
| --- | --- |
| planned | 已接受为意图行为，尚未实现 |
| in_progress | 正在建设 |
| implemented | 已实现且存在证明 |
| changed | 先前实现之后契约又变更 |
| retired | 不再属于产品契约 |

## 矩阵

本遗留视图不附带静态产品行。运营记录用 `story add` 与 `story update`。棕地仓库可在导入既有状态前在此添加行。

## 证据规则

- **单元证明**：覆盖纯领域与应用规则。
- **集成证明**：覆盖后端强制、数据完整性、提供方行为、任务或服务契约。
- **E2E 证明**：覆盖用户可见的浏览器流程。
- **平台证明**：仅覆盖无法在更低层证明的壳层、部署、移动、桌面或运行时行为。
- 若故事包说明了原因，故事可以在并非每列证明齐全时标为已实现。

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
