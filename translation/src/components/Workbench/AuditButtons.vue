<template>
  <span class="audit-buttons">
    <a-button
      type="primary"
      size="small"
      style="margin-left: 8px"
      class="resetBtn"
      @click="$emit('pass')"
    >
      通过
    </a-button>
    <a-button
      type="primary"
      size="small"
      style="margin-left: 8px"
      class="rejectBtn"
      @click="$emit('reject')"
    >
      驳回
    </a-button>
  </span>
</template>

<script>
export default {
  name: "AuditButtons",
  emits: ["pass", "reject"],
};
</script>

<style scoped lang="less">
.rejectBtn {
  background: #fbb31f;
  border-color: #fbb31f;
}
.rejectBtn:hover {
  background: #fbb31f;
  border-color: #fbb31f;
}
.rejectBtn:focus {
  background: #fbb31f;
  border-color: #fbb31f;
}
</style>
