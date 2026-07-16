# 题面 — B01 分拣与落点规划

你正在 **translationtool** 仓库做 Harness **变更请求** 的分拣（先规划，**不要改任何业务代码**）。

**用户需求（原文）：**

> 1. 回填弹窗 `modal_v3` 里校验提示文案不对，要改前端展示。  
> 2. 另外要新增一个「按词条 ID 批量查询术语」的 HTTP API，给 UI 调用。

请阅读沙箱中的 `docs/ARCHITECTURE.md`，用中文回答：

1. 这两块分别应落在哪个目录/模块？  
2. 新 API 能否默认写进 `translationtoolservice/`？为什么？  
3. 若需要 intake，你会标什么 `backend=`？

**本沙箱交付物（必须）：**

- 创建文件 `.eval-markers/route-plan`，内容示例：
  ```
  frontend=translation/
  python=terminology-agent/
  java=none
  ```
- **不要**创建 `.eval-markers/java-new-feature`。

完成后用中文简要总结。
