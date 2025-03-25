<template>
  <a-button type="primary" danger  @click="handleDelete">删除</a-button>
</template>

<script>
import { Modal, message } from "ant-design-vue";
import { createVNode } from "vue";
import { ExclamationCircleOutlined } from "@ant-design/icons-vue";

export default {
  props: {
    // 要删除的数据
    dataSource: {
      type: Array,
      required: true
    },
    // 确认对话框的标题
    confirmTitle: {
      type: String,
      default: '是否确定删除?'
    },
    // 删除接口
    deleteApi: {
      type: Function,
      required: true
    }
  },
  methods: {
    handleDelete() {
      Modal.confirm({
        title: this.confirmTitle,
        icon: createVNode(ExclamationCircleOutlined),
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          this.deleteApi(this.dataSource);
        }
      });
    }
  }
};
</script>