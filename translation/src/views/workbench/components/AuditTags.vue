<template>
  <!-- 审核阶段：行内审核状态标签（通过/驳回），供 examineModal bodyCell 使用 -->
  <div class="editable-row-operations">
    <span>
      <a-checkable-tag
        :checked="auditState === 1"
        :class="auditState === 1 ? 'passTagChecked' : 'passTag'"
        @change="onPass"
      >
        通过
      </a-checkable-tag>
      <a-checkable-tag
        :checked="auditState === 0"
        :class="auditState === 0 ? 'rejectTagChecked' : 'rejectTag'"
        @change="onReject"
      >
        驳回
      </a-checkable-tag>
    </span>
  </div>
</template>

<script>
export default {
  name: "AuditTags",
  props: {
    auditState: {
      type: Number,
      required: true,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["pass", "reject"],
  methods: {
    onPass() {
      if (this.disabled) {
        return;
      }
      this.$emit("pass");
    },
    onReject() {
      if (this.disabled) {
        return;
      }
      this.$emit("reject");
    },
  },
};
</script>

<style scoped lang="less">
.passTag {
  border: 1px solid #36bf7d;
  color: #36bf7d;
}
.passTagChecked {
  background-color: #36bf7d;
  color: white;
}
.rejectTag {
  border: 1px solid #fbb31f;
  color: #fbb31f;
}
.rejectTagChecked {
  background-color: #fbb31f;
  color: white;
}
</style>
