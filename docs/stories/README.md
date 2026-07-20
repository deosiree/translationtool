# 故事（Stories）

故事是工作包。它们把产品意图变成有界的实现与验证工作。

当前活跃史诗包：

- `docs/stories/epics/E03-pre-translate-agent/` — PreTranslate Phase 3c 验收与可选 3d（见该目录 README）

## Normal 故事

普通功能工作使用 `docs/templates/story.md`。

建议路径：

```text
docs/stories/epics/E01-domain-name/US-001-short-story-title.md
```

## High-Risk 故事

功能分拣将工作判为 high-risk 时，使用 `docs/templates/high-risk-story/`。

建议路径：

```text
docs/stories/epics/E02-risky-domain/US-012-risky-story-title/
  execplan.md
  overview.md
  design.md
  validation.md
```

## 状态流

```text
planned -> in_progress -> implemented
                  |
                  v
               changed
                  |
                  v
               retired
```
