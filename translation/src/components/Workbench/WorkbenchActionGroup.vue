<template>
  <div
    class="workbench-action-group"
    :class="{ 'workbench-action-group--offset': inlineOffset }"
  >
    <slot />
  </div>
</template>

<script>
export default {
  name: "WorkbenchActionGroup",
  props: {
    /** 是否在组前加 margin-left: 8px（inline 末尾按钮组） */
    inlineOffset: {
      type: Boolean,
      default: false,
    },
  },
};
</script>

<style scoped>
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
