<template>
  <!-- 任务信息条（名称/产品/分类/语种） -->
  <div class="workbench-task-info">
    <div class="workbench-task-info__item">任务名称：{{ task.name }}</div>
    <div class="workbench-task-info__item">产品名称：{{ task.productName }}</div>
    <div class="workbench-task-info__item">上级分类名称：{{ task.classifyName }}</div>
    <div class="workbench-task-info__item">翻译语种：{{ task.translateType }}</div>
    <!-- 各阶段扩展区（如 RulesDropdown） -->
    <slot name="taskExtra" />
  </div>
  <!-- 工具栏上方扩展（如 import 的数据源导入区） -->
  <slot name="beforeFormBar" />
  <!-- 表格上方工具栏 -->
  <div class="workbench-form-bar">
    <div class="workbench-form-bar__main">
      <!-- 默认槽：查询/审核/删除等本阶段按钮 -->
      <slot />
      <!-- 展示列（工具栏右侧内联；与表头放大镜无关） -->
      <div
        v-if="showInlineColumnActions"
        class="workbench-action-group workbench-action-group--offset"
      >
        <slot name="columnActions">
          <ColumnActions
            v-bind="columnActions"
            @update:model-value="$emit('update:modelValue', $event)"
            @change="$emit('columnsChange', $event)"
          />
        </slot>
      </div>
    </div>
    <!-- 工具栏最右区（如 import 的展示列 + CoverButton） -->
    <div
      v-if="$slots.trailing"
      class="workbench-form-bar__trailing"
    >
      <slot name="trailing" />
    </div>
  </div>
  <!-- 工具栏下方扩展（如 examine 的过滤语种行） -->
  <slot name="subToolbar" />
</template>

<script>
/**
 * 工作台流水线工具栏壳：TaskInfo + FormBar + ActionGroup 内联，展示列可选。
 */
import ColumnActions from "./ColumnActions.vue";

export default {
  name: "PipelineToolbar",
  components: { ColumnActions },
  props: {
    task: {
      type: Object,
      default: () => ({}),
    },
    /** 传给 ColumnActions；为 null 且未提供 #columnActions / #trailing 时不渲染内联展示列 */
    columnActions: {
      type: Object,
      default: null,
    },
  },
  emits: ["columnsChange", "update:modelValue"],
  computed: {
    /**
     * 内联末尾展示列：有 columnActions prop 或 #columnActions 槽，且非 trailing 布局
     * @returns {boolean}
     */
    showInlineColumnActions() {
      if (this.$slots.trailing) {
        return false;
      }
      return this.columnActions != null || !!this.$slots.columnActions;
    },
  },
};
</script>

<style scoped>
.workbench-task-info {
  display: flex;
  padding: 4px 0;
  align-items: center;
  gap: 32px;
  align-self: stretch;
}
.workbench-task-info__item {
  display: flex;
  align-items: center;
  flex: 1 0 0;
}
.workbench-form-bar {
  display: flex;
  align-items: center;
  align-self: stretch;
  width: 100%;
}
.workbench-form-bar__main {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}
.workbench-form-bar__trailing {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.workbench-action-group {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.workbench-action-group--offset {
  margin-left: 8px;
}
</style>
