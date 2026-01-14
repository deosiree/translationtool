<template>
  <a-button type="primary" :size="size" @click="batchSelectOpen" v-if="!batchSelectFlag">批量选择</a-button>
  <a-button type="primary" :size="size" @click="batchSelectCancel" class="yellowBtn" v-if="batchSelectFlag">取消选择</a-button>
  <a-button type="primary" :size="size" @click="overTask" ghost v-if="batchSelectFlag">结束任务</a-button>
</template>

<script>
import { setModalAriaHidden } from "@/utils/domUtils";
import { message, Modal } from "ant-design-vue";
import { ExclamationCircleOutlined } from "@ant-design/icons-vue";
import { updateTaskInfo } from "@/http/api/task";
import { getCurrentFormattedTime } from "@/utils/dateUtils";
import { createVNode } from "vue";
export default {
  emits: [
    "update:selectEntry",
    "update:selectedRows",
    "update:selectedRowKeys",
    "update:batchSelectFlag",
  ],
  props: {
    size: {
      type: String,
      default: "middle",
    },
    getSearch: { type: Function }, // 回调函数-一般是查询接口（结束任务后任务列表发生变化，所以需要重新查询）
    selectEntry: {
      type: Map,
      default: () => new Map(),
    },
    selectedRows: {
      type: Array,
      default: () => [],
    },
    selectedRowKeys: {
      type: Array,
      default: () => [],
    },
    batchSelectFlag: {
      type: Boolean,
      default: false,
    },
  },
  // watch: {
  //   // 监听批量选择的展开状态变化
  //   selectedRows(newVal, oldVal) {
  //     if (newVal != oldVal) {
  //       console.log("selectedRows", newVal);
  //     }
  //   },
  // },
  methods: {
    // 结束任务按钮点击事件
    overTask() {
      Modal.confirm({
        title: "是否批量结束任务？",
        content: '词条状态将更新为"已归档"',
        icon: createVNode(ExclamationCircleOutlined),
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          const promise = [];
          for (let i = 0; i < this.selectedRows.length; i++) {
            if (!this.selectedRows[i].isBranch) {
              this.selectedRows[i].state = "6";
              this.selectedRows[i].endTime = getCurrentFormattedTime();
              promise.push(updateTaskInfo(this.selectedRows[i]));
            }
          }
          Promise.allSettled(promise).then((results) => {
            let flg = true;
            results.forEach((result) => {
              if (result.status === "rejected") {
                console.log("失败:", result.value);
                flg = false;
              }
            });
            message.success(
              `已批量结束任务！${flg ? "" : "(部分任务失败，请查看控制台)"}`
            );
            this.batchSelectCancel();
          });
        },
      });
    },
    // 批量选择展开
    batchSelectOpen() {
      // if (this.batchSelectOnChange(this.getSearch))
      //   // 判断搜索条件是否变化，如果变化则重新查询
      //   return;
      this.$emit("update:batchSelectFlag", true);
      this.$emit("update:selectEntry", new Map());
      this.$emit("update:selectedRows", []);
      this.$emit("update:selectedRowKeys", []);
    },
    // 取消选择（批量选择取消展开，清空已选任务）
    batchSelectCancel() {
      this.$emit("update:selectEntry", new Map());
      this.$emit("update:selectedRows", []);
      this.$emit("update:selectedRowKeys", []);
      this.$emit("update:batchSelectFlag", false); // 关闭批量选择的展开，变成只显示批量选择按钮
      if (typeof this.getSearch === "function") {
        this.getSearch();
      }
    },
  },
};
</script>

<style scoped lang="less">
/* 可以在这里添加组件的样式 */
</style>