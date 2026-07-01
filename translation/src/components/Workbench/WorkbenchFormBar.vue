<template>
  <div class="workbench-form-bar">
    <div class="workbench-form-bar__main">
      <slot />
    </div>
    <div
      v-if="$slots.trailing"
      class="workbench-form-bar__trailing"
    >
      <slot name="trailing" />
    </div>
  </div>
</template>

<script>
export default {
  name: "WorkbenchFormBar",
};
</script>

<style scoped>
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
</style>
