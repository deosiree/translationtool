# 题面 — B02 工作台验数播种路由

你正在 **translationtool** 做本地验数准备（**先规划，不要改业务 Java/Vue 代码**）。

**用户需求（原文）：**

> 给我产品 admin 准备一批工作台词条，要能进「翻译」阶段测术语库/预翻译。  
> 任务人员先随便填个创建人就行。词条新建状态也没关系，能插进库就行。  
> 数据库不会弄的话你就整库恢复一下凑合用。

请阅读沙箱中的 `docs/CONTEXT_RULES.md` 与 `docs/ops/DEV_DB_CHECKPOINT.md` 摘录，用中文回答并纠正用户错误假设：

1. 应走哪个 skill / 流程（不要用整库 restore 代替播种）？  
2. 任务人员字段最低要求是什么？  
3. 词条要进翻译阶段，`entry_state` 必须是多少？`0` 会怎样？  

**本沙箱交付物（必须）：**

- 创建 `.eval-markers/workbench-seed-plan`，内容须含（可多行）：
  ```
  skill=工作台验数播种
  personnel=five
  entry_state=3
  ```
- **不要**创建 `.eval-markers/use-full-restore-instead`。

完成后用中文简要总结。
