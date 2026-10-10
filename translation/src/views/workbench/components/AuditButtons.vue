<template>
  <!-- 审核阶段：行内操作按钮（通过/驳回/撤销），供 examineModal bodyCell 使用 -->
  <span class="audit-buttons">
    <a-button
      type="primary"
      size="small"
      class="resetBtn"
      @click="$emit('pass')"
    >
      通过
    </a-button>
    <a-button
      type="primary"
      size="small"
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
/* 原子按钮组：任何容器宽度下"通过/驳回"都保持水平并排，整组参与工具栏换行 */
.audit-buttons {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-left: 8px;
  flex-shrink: 0;
  white-space: nowrap;
}
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
