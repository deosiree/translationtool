# Scripts

本目录包含本仓库 Harness 自动化工具。Harness 附着在**仓库根**（本目录的上一级）：`harness.db`、`scripts/bin/`、`scripts/schema/`、`scripts/harness-cli-release-tag` 均在仓库根，**不在** `translation/` 子目录。

## Harness CLI

Rust Harness CLI 是持久层（SQLite `harness.db`）的主要接口。Windows 预构建二进制在 `scripts/bin/harness-cli.exe`，macOS/Linux 在 `scripts/bin/harness-cli`。

请求类别（只读 vs 变更）的 SSOT 见 `docs/HARNESS.md` §请求类别循环。**只读请求**（回答/解释/评审/诊断/计划/状态）**禁止** bootstrap、初始化/迁移数据库、录入 intake 或记录 trace。

### 安装 CLI 二进制

`scripts/bin/harness-cli(.exe)` 被 `.gitignore` 忽略，因此**全新 clone 后没有 CLI**。首次 bootstrap 前需先从 pinned release 下载并校验：

```powershell
.\scripts\install-harness-cli.ps1
```

```bash
scripts/install-harness-cli.sh
```

脚本读取 `scripts/harness-cli-release-tag` 指定的版本，下载对应平台二进制、做 `.sha256` 校验后放入 `scripts/bin/`。若默认上游 release 地址不可达，可设 `HARNESS_CLI_BASE_URL` 指向其它 artifact 目录。

### 启动（bootstrap）

变更请求在查询或改持久状态前，先 bootstrap 本地忽略的运行时：

```powershell
.\scripts\bootstrap-harness.ps1
```

```bash
scripts/bootstrap-harness.sh
```

bootstrap 会：校验 CLI 版本与 pinned release 一致 → 探测数据库 schema 状态（`missing`→`init`、`needs_migration`→`migrate`、`current`→跳过）→ 拒绝 supported-range 之外的 schema。

### 常用命令

```powershell
.\scripts\bin\harness-cli.exe init          # 建库
.\scripts\bin\harness-cli.exe migrate       # 应用待处理 schema 迁移
.\scripts\bin\harness-cli.exe intake ...    # 录入功能 intake 分类
.\scripts\bin\harness-cli.exe story ...     # 增改 story（测试矩阵行）
.\scripts\bin\harness-cli.exe decision ...  # 增改 decision
.\scripts\bin\harness-cli.exe backlog ...   # 增关 backlog 项
.\scripts\bin\harness-cli.exe propose       # 对重复摩擦/审计证据分类提案
.\scripts\bin\harness-cli.exe trace ...     # 记录并自动评分 agent trace
.\scripts\bin\harness-cli.exe score-trace   # 按 TRACE_SPEC.md 分层对 trace 评分
.\scripts\bin\harness-cli.exe query ...     # 查询 harness 数据
.\scripts\bin\harness-cli.exe --version     # 打印已安装 CLI 版本
```

完整用法：`.\scripts\bin\harness-cli.exe help` 或 `.\scripts\bin\harness-cli.exe query help`。

### 说明

- `story update` 的 proof 旗标为数值布尔：是 `1`、否 `0`。`story verify <id>` 只跑配置的 `verify_command`，不接受 proof 旗标；先用 `story add/update --verify` 配置命令，跑 `story verify <id>`，再用 `story update` 更新 proof 旗标。
- `story update --status implemented` 会被拒绝。应先把活跃工作移到 `in_progress` / `changed`，再用 `story complete <id>` 让「fresh proof + implemented 转态」原子完成。
- `backlog --risk` 用 harness 车道词而非严重度词：`tiny` / `normal` / `high-risk`（用 `tiny` 而非 `low`）。
- `query matrix` 默认人类可读 `yes`/`no`；复制到 `story update` 时用 `query matrix --numeric`。
- `query sql` 接受一条只读 SQLite 语句，连接层强制只读；写操作走类型化 harness 命令、迁移或 semantic changeset。
- `scripts/bin/harness-cli import brownfield` 可从既有 markdown（`docs/TEST_MATRIX.md`、`docs/decisions/`、`docs/HARNESS_BACKLOG.md`）播种或刷新数据库，适合让已装 harness 的仓库不丢运营文档地落到 Rust CLI 路径。

`schema/` 下的迁移文件版本受控（`NNN-描述.sql`）；`harness.db` 已 gitignore，每实例各自生成运维数据。数据库直查仍可用 SQLite 工具，但正常 harness 操作应走 Rust CLI。

设置 `HARNESS_DB_PATH=/path/to/harness.db` 可让 `harness-cli` 操作隔离的复制库（优先级高于旧 `HARNESS_DB` 覆盖项；两者都未设时用仓库根 `harness.db`）。设置 `HARNESS_RUN_ID=<run-id>` 时，持久写命令会把 semantic 操作记录追加到 `.harness/changesets/<run-id>.changeset.jsonl`。

## 版本与上游

本仓 pin 在 Protocol V1 遗留（`harness-cli-v0.1.17`，见 `scripts/harness-cli-release-tag`）。上游 `repository-harness` 已把 SQLite CLI / protocol v1 于 2026-08-10 停止支持（最后兼容版 `harness-cli-v0.1.22`），但不会自动删除消费者仓库里的旧二进制 / DB / schema。本仓按「pin 保留」稳态使用；未来如需上游新能力再评估迁移到 repository-protocol（V2）。
