# 0005 — 预构建 Rust Harness CLI

Date: 2026-05-23

## Status

Accepted, amended 2026-05-31, amended 2026-06-09, amended 2026-07-13

## Context

持久层最初是围绕 SQLite 的薄 shell 包装。该包装已大到足以承载有意义的架构风险：在同一脚本中混合命令解析、SQL 构造、migration、import 行为、query 渲染与 help 文本。

此前 installer 将 shell wrapper 复制到目标仓库。这使 Harness 易于安装，但也意味着 Rust 重写不仅是实现变更，更改变了每个接收 Harness 的项目的分发契约。

## Decision

Harness CLI 的未来 Rust 实现应作为预构建二进制，由 installer 下载交付。

用户与 Agent 的命令路径为已安装的 Rust 二进制：

```bash
scripts/bin/harness-cli <command>
```

在 Windows 上，仓库本地二进制安装为：

```powershell
.\scripts\bin\harness-cli.exe <command>
```

installer 应下载、校验并在该路径直接安装平台特定的 Rust 二进制。不应再存在 shell wrapper 命令契约。

Rust CLI 应遵循既有架构规则：

- Domain：harness records、statuses、lanes 与 value types。
- Application：intake、stories、decisions、backlog、traces 与 queries 的 use cases。
- Infrastructure：SQLite repositories 与 schema migrations。
- Interface：命令行解析、终端输出与 installer 集成。

发布自动化现遵循同一分发契约。PR 合并到 `main` 后，post-merge maintenance workflow 更新 `CHANGELOG.md`。若合并 PR 变更了 Rust CLI 源码、schema、Cargo 元数据或 CLI release packaging，则同时 bump CLI patch 版本、更新 installer release tag pin，并对精确版本化的 maintenance commit 调用可复用的 Harness CLI release workflow。workflow 在最终 promotion job 创建 annotated `harness-cli-v*` tag 并发布 assets 之前，须证明全部五个平台 artifact 以及 pinned old-to-current upgrade transition。direct tag push 不是发布路径，因为已存在的 tag 无法 proof-before-promotion。

历史 upgrade-source 二进制对其自身版本运行 frozen contract；构建并安装的 candidate 运行 current contract。失败的 release tag 不可变且已消耗：自动化推进到更高 patch 版本，而非删除、移动、复用或发布原始 proof run 失败的 tag。决策 `0010-proof-before-cli-release-promotion.md` 定义 promotion 与 recovery 细节。

## Alternatives Considered

1. 永久保留 shell CLI — 否决：脚本已从薄 wrapper 成长为测试性弱的应用面。
2. 将 Rust 源码复制到每个目标项目并本地构建 — 否决：使 Harness 安装依赖本地 Rust 工具链，提高仅需 harness 的项目的 setup 摩擦。
3. 要求用户单独安装全局 `harness` 二进制 — 否决：Harness 应对 Agent 保持仓库本地。
4. 通过 installer 下载预构建二进制 — 接受：目标仓库保持简单，同时 CLI 内部可 typed、可测试、平台感知。

## Consequences

Positive:

- 持久层 CLI 可迁移到 typed 命令解析与经测试的 use cases。
- 目标项目使用 Harness 无需 Rust 工具链。
- `scripts/bin/harness-cli` 是 macOS/Linux 上 Agent 的稳定入口；Windows 使用相同仓库本地路径加 `.exe` 后缀。
- 预构建 release 可包含已知 SQLite linkage 策略。

Tradeoffs:

- installer 须具备平台检测与二进制下载行为。
- release artifact 需要 checksum 或其他完整性校验。
- 不支持的平台须有清晰错误路径。
- 项目需要可重复的受支持平台 release 流程。
- 失败的版本即使从未收到 release assets 也视为已消耗。

## Follow-Up

- 通过 `US-002 Rust Harness CLI` 完成迁移。
- 从已安装项目 payload 中移除旧 shell wrapper。
- 为下载二进制增加 checksum 校验。
- 将 Rust CLI 视为持久层 primary implementation。
