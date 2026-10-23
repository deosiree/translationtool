# 选择工具栏

`selectToolBar` 为词条管理、术语库等表格提供统一的工具栏布局、按钮样式和行选择状态辅助方法。组件负责呈现，选择清单与业务操作仍由调用页面持有。

## 文件职责

| 文件 | 职责 |
| --- | --- |
| `SelectionToolbar.vue` | 通过默认插槽排列工具栏内容，统一垂直对齐、自动换行和 `10px` 间距。 |
| `SelectionToolbarButton.vue` | 提供统一的 `small` 按钮，支持按钮类型与语义颜色 preset、图标、徽标、禁用和 loading。 |
| `composables/useSelectionToolbarState.js` | 根据已选行和 loading 状态计算数量、徽标数量及清空按钮禁用状态。 |
| `composables/useSelectedEntries.js` | 提供按稳定行 ID 合并、更新、移除和清空选择清单的操作。新行对象会覆盖相同 ID 的旧对象。 |
| `composables/useSelectionOperations.js` | 管理异步全量选择/取勾的临时投影、单行操作顺序、成功应用、失败回滚和操作失效。 |

## 按钮属性与事件

`SelectionToolbarButton` 接收以下属性：

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `text` | `string` | 必填 | 按钮文案。 |
| `preset` | `string` | `default` | 按钮样式：`primary`、`default`、`text`、`selected`（绿色选中态）或 `warning`（橙色警示态）。 |
| `icon` | `Object\|Function` | `null` | 可选的图标组件。 |
| `disabled` | `boolean` | `false` | 禁用按钮。 |
| `loading` | `boolean` | `false` | 显示居中 spinner、隐藏徽标并禁用按钮，同时保留文案占位以稳定宽度。 |
| `badgeCount` | `number` | `null` | 非零时显示数量徽标。 |
| `overflowCount` | `number` | `99` | 徽标超过此值时显示的上限文案。 |
| `ariaLabel` | `string` | `text` | 覆盖按钮的无障碍名称。 |
| `buttonClass` | `string` | `""` | 追加按钮样式类；颜色优先使用 `preset`，避免页面重复维护。 |

按钮通过 `click` 事件转发原生点击事件。图标、徽标以外的内容由调用方的 `text` 属性提供。

## 使用示例

```vue
<SelectionToolbar>
  <SelectionToolbarButton
    text="已选词条"
    preset="selected"
    :badge-count="selectedCount"
    :loading="selectionLoading"
    @click="openSelectedEntries"
  />
  <SelectionToolbarButton
    text="取消选择"
    :disabled="selectedCount === 0 && !selectionLoading"
    @click="clearSelection"
  />
</SelectionToolbar>
```

页面按自身业务决定点击行为、清空时要同步的状态，以及其他按钮的顺序；容器只对插槽内容做布局。

## 选择 composables

### `useSelectionToolbarState(selectedEntries, loading = false)`

返回 `{ count, badgeCount, loading, clearDisabled }` 状态快照。`badgeCount` 在 loading 时为 `0`，`clearDisabled` 在没有已选项且没有全量操作时为 `true`。调用页面应在 computed 等响应式计算中传入最新值。

### `useSelectedEntries()`

返回以下按 ID 操作：

- `merge(existingRows, incomingRows)`：合并并去重，同 ID 的新行覆盖旧行。
- `update(rows, row)`：将一条最新行合并到清单。
- `remove(existingRows, removedRows)`：按行对象或 ID 移除清单项。
- `apply(selectedRows, resultRows, selected)`：按 `selected` 合并或移除结果行。
- `clear()`：返回空数组，由调用页面赋回清单状态。

独立表格的选择清单应由该表格页面持有。不同业务使用此 composable 时，应确认其 ID 在清单范围内稳定且唯一。

### `useSelectionOperations()`

用于异步查询完整结果的表格选择。每次全量选择或取勾都用稳定查询范围调用 `begin(scope, selected)`；`scope` 应包含产品、任务及筛选条件，但不包含页码和页大小。分页变化因此不影响待处理投影，切换查询范围时由页面调用 `invalidate()`。

| 方法/属性 | 用途 |
| --- | --- |
| `begin(scope, selected)` | 登记一次待处理全量选择或取勾，返回操作令牌。 |
| `record(id, selected, scope)` | 记录后续单行操作，使同 ID 最后一次用户操作优先。 |
| `refresh(row)` | 请求期间缓存某行的新对象，避免旧全量响应覆盖更新值。 |
| `project(id, scope, committed)` | 计算行在待处理操作下应呈现的勾选状态。 |
| `isCurrent(operation, currentScope)` | 判断令牌是否仍有效且属于当前范围。 |
| `resolve(operation, rows, selectedRows, currentScope)` | 成功时按行 ID 应用全量结果并返回新清单。 |
| `reject(operation)` | 失败时移除待处理投影，清单由调用方保持不变并负责提示错误。 |
| `invalidate()` | 使当前待处理操作失效，返回新的代次。 |
| `pendingCount` / `revision` | 读取待处理操作数与操作状态版本。 |

请求期间由页面用 `project` 显示即时状态；成功后将 `resolve` 返回值写回清单，失败后调用 `reject`。列表请求 loading 与选择操作 loading 分开管理。
