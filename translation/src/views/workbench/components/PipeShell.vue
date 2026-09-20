<template>
  <CustomModal
    :visible="visible"
    :modalVisible="visible"
    :modalTitle="modalTitle"
    :modalWidth="width"
    :wrapClassName="wrapCls"
    :fullFlag="fullFlag"
    :footer="footer"
    :showCancel="showCancel"
    :showOk="showOk"
    :cancelText="cancelText"
    :okText="okText"
    :okLoading="okLoading"
    @handleClose="$emit('handleClose')"
    @handleOK="$emit('handleOK')"
    @afterClose="$emit('afterClose')"
    @setTableHeight="(h, t) => $emit('setTableHeight', h, t)"
  >
    <slot />
    <template #leftBottomBtn>
      <slot name="leftBottomBtn" />
    </template>
  </CustomModal>
</template>

<script>
import CustomModal from "@/components/modal/index.vue";

/** 流水线阶段模态默认宽 */
const PIPE_W = "90%";
const WRAP = "pipe-shell";

export default {
  name: "PipeShell",
  components: { CustomModal },
  emits: ["handleClose", "handleOK", "afterClose", "setTableHeight"],
  props: {
    visible: { type: Boolean, default: false },
    modalTitle: { type: String, default: "" },
    modalWidth: { type: String, default: PIPE_W },
    fullFlag: { type: Boolean, default: true },
    footer: { type: Boolean, default: true },
    showCancel: { type: Boolean, default: true },
    showOk: { type: Boolean, default: true },
    cancelText: { type: String, default: "取消" },
    okText: { type: String, default: "确定" },
    okLoading: { type: Boolean, default: false },
  },
  computed: {
    width() {
      return this.modalWidth || PIPE_W;
    },
    wrapCls() {
      return WRAP;
    },
  },
};
</script>

<!-- portal 挂载，须非 scoped；PIPE_MIN 与样式保持一致 -->
<style lang="less">
.pipe-shell.ant-modal-centered::before {
  display: none;
}
.pipe-shell.ant-modal-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  overscroll-behavior: contain;
  text-align: left;
}
.pipe-shell.ant-modal-wrap > div {
  flex: 0 0 auto;
  max-width: 100%;
  margin: auto;
}
.pipe-shell .ant-modal {
  max-width: calc(100vw - 24px) !important;
  min-width: 0;
  top: 0;
  display: block;
  padding-bottom: 0;
  margin: 0;
}
.pipe-shell .ant-modal-content {
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
}
.pipe-shell .modalContent {
  overflow-x: auto;
  overflow-y: auto;
}
.pipe-shell .content {
  min-width: 1100px; /* PIPE_MIN */
}
body.ant-modal-open {
  overflow: hidden !important;
}
.pipe-shell.full-modal .ant-modal {
  max-width: 100% !important;
  width: 100%;
  min-width: 0;
}
.pipe-shell.full-modal .content {
  min-width: 0;
}
.pipe-shell.full-modal.ant-modal-wrap > div {
  width: 100%;
  max-width: 100%;
  margin: 0;
}
.pipe-shell .ant-table-selection-column {
  width: 48px !important;
  min-width: 48px !important;
  max-width: 48px !important;
}
</style>
