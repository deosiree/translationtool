<template>
  <div class="workbench-task-info">
    <div class="workbench-task-info__item">任务名称：{{ task.name }}</div>
    <div class="workbench-task-info__item">产品名称：{{ task.productName }}</div>
    <div class="workbench-task-info__item">上级分类名称：{{ task.classifyName }}</div>
    <div class="workbench-task-info__item">翻译语种：{{ task.translateType }}</div>
    <slot name="extra" />
  </div>
</template>

<script>
export default {
  name: "WorkbenchTaskInfo",
  props: {
    task: {
      type: Object,
      default: () => ({}),
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
</style>
