# US-3C-02 Phase 3c 验收后 P0/P1 卫生项

## Status

implemented

## Lane

normal

## Module / Backend

- 模块面：全栈（`translation/` + `terminology-agent/`）
- 后端面：`backend=python`；**不动 Java**
- 依赖：US-3C-01 implemented

## Acceptance Criteria

- [x] P0：登录/webpack 修复 + 验收脚本 + 文档已提交（`987f84a`）
- [x] P0：验收 batch 不带假工作台 entry id
- [x] P1：`permission.js` catch 调用 `next` + 提示重新登录
- [x] P1：pending 同页软去重（源词条+comment+语种+部门）
- [x] P1：S02 用例注释；`verify_adm_pretranslate` engine.dispose
- [x] P1：cleanup 用法写入 E03 README
- [x] harness story complete

## Evidence

- `pytest app/services/term_audit/tests` 11 passed
- `verify_adm_pretranslate --strict` 6/6
- `verify_us3c01_api_matrix` 无 workbench_sync_error
