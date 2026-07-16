# US-3C-02 Phase 3c 验收后 P0/P1 卫生项

## Status

in_progress

## Lane

normal

## Module / Backend

- 模块面：全栈（`translation/` + `terminology-agent/devtools`）
- 后端面：`backend=python`；**不动 Java**
- 依赖：US-3C-01 implemented

## Product Contract

关闭 Phase 3c 验收后遗留卫生项：入库未提交改动、验收脚本噪音、permission 空 catch、pending 重复展示、S02 用例说明、devtools 连接析构噪音；并同步 Harness。

## Acceptance Criteria

- [ ] P0：登录/webpack 修复 + `verify_us3c01_api_matrix` + story/进度快照已提交
- [ ] P0：验收 batch **不带假工作台 entry id**（避免 `workbench_sync_error` 噪音）
- [ ] P1：`permission.js` catch 调用 `next`，不再挂死导航
- [ ] P1：pending 列表按词条键软去重（同页保留最新）
- [ ] P1：S02 用例注释说明 `fuzzy|none` 均可；ADM/API 脚本 engine dispose
- [ ] P1：cleanup 用法写进 E03 README
- [ ] `story verify` / harness complete

## Validation

| Layer | Proof |
| --- | --- |
| Unit | 相关 pytest / 前端不破 |
| Integration | `verify_us3c01_api_matrix` |
| E2E | 登录仍可进工作台（回归） |
