<template>
  <CustomModal :modalWidth="modalWidth" modalTitle="批量选择" :visible="visible" @handleClose="handleClose" @handleOK="handleOK">
    <div class="table">
      <div>已选词条：</div>
      <a-config-provider :locale="locale">
        <a-table class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="{x:'max-content' , y: '60vh'}"
          :pagination="pagination" :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="batchSelectable" bordered>
          <template #bodyCell="{ column, record, text }">
            <template v-if="column.dataIndex === 'translateState'">
              <TransStateBadge :translateState="text" />
            </template>
            <template v-if="column.dataIndex === 'publicState'">
              <EntryStateBadge :entryState="text" />
            </template>
            <template v-if="column.dataIndex === 'operation'">
              <div class="editable-row-operations">
                <DeleteOutlined style="color:#369FFF;font-size:16px" @click="remove(record)" title="取消选择" />
              </div>
            </template>
          </template>
        </a-table>
      </a-config-provider>
    </div>
    <template v-slot:leftBottomBtn>
      <a-button @click="batchSelectCancel">关闭</a-button>
      <DeleteButton :dataSource="dataSource" :deleteApi="deleteSykEntry"></DeleteButton>
    </template>
  </CustomModal>
</template>
<script>
import CustomModal from "@/components/modal/index.vue";
import DeleteButton from "@/components/Button/deleteButton.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import zh_CN from "ant-design-vue/es/locale/zh_CN";
import {
  ExclamationCircleOutlined,
  DeleteOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import { getSykNotUsed, deleteSykEntry } from "@/http/api/glossary";
import { pageChange } from "@/utils/selectionUtils";
import { getColPref } from "@/utils/tableUtils";
import { glossaryParams } from "@/utils/commonParam.js";
export default {
  components: {
    CustomModal,
    DeleteButton,
    EntryStateBadge,
    TransStateBadge,
    ExclamationCircleOutlined,
    DeleteOutlined,
  },
  emits: [
    "batchSelectClose",
    "batchSelectCancel",
    "refresh",
    "update:dataSource", // 添加 update:dataSource 事件
    "update:selectedRowKeys", // 添加 update:selectedRowKeys 事件
    "update:selectedRows", // 添加 update:selectedRows 事件
  ],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    // columns: {
    //   type: Array,
    //   default: () => [],
    // },
    dataSource: {
      type: Array,
      default: () => [],
    },
    selectedRows: {
      type: Array,
      default: () => [],
    },
    selectedRowKeys: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      locale: zh_CN,
      modalWidth: "60%",
      columns: [],
      pagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
    };
  },
  watch: {
    visible: {
      async handler(newVal) {
        // console.log("visible changed:", newVal);
        if (newVal) {
          console.log("columns0:", this.columns);
          this.columns = [
            {
              title: "序号",
              dataIndex: "index",
              align: "center",
              width: 50,
              customRender: (text, record, index, column) => {
                return (
                  text.index +
                  1 +
                  this.pagination.pageSize * (this.pagination.current - 1)
                );
              },
              fixed: "left",
              resizable: true,
              index: 0,
            },
            {
              title: "词条",
              dataIndex: "entry",
              align: "center",
              width: 100,
              fixed: "left",
              resizable: true,
              index: 1,
            },
            {
              title: "操作",
              dataIndex: "operation",
              align: "center",
              width: 50,
              fixed: "right",
              resizable: true,
              index: 101,
            },
          ];
          // console.log("columns1:", this.columns);
          try {
            // 读取本地存储的用户偏好
            await getColPref(
              "colPref-glossary",
              100,
              this,
              false,
              glossaryParams
            ); // 等待 getColPref 执行完成
            // console.log("columns2:", this.columns);
          } catch (error) {
            console.error("获取列偏好失败:", error);
          }
        }
      },
      immediate: false, // 不立即执行
    },
  },
  methods: {
    // 移除某条已选词条（每行最右边的按钮
    remove(record) {
      const newDataSource = this.dataSource.filter((item) => {
        return item.id != record.id;
      });
      const newSelectedRowKeys = this.selectedRowKeys.filter((item) => {
        return item.id != record.id;
      });
      const newSelectedRows = this.selectedRows.filter((item) => {
        return item.id != record.id;
      });
      this.$emit("update:dataSource", newDataSource); // 重新更新dataSource，触发table重新渲染，已选词条列表会自动更新，不需要重新请求接口获取数据了，减少接口调用次数，提升性能
      this.$emit("update:selectedRowKeys", newSelectedRowKeys); // 重新更新selectedRowKeys，触发table重新渲染，已选词条列表会自动更新，不需要重新请求接口获取数据了，减少接口调用次数，提升性能
      this.$emit("update:selectedRows", newSelectedRows); // 重新更新selectedRows，触发table重新渲染，已选词条列表会自动更新，不需要重新请求接口获取数据了，减少接口调用次数，提升性能
    },
    // 取消按钮
    handleClose() {
      this.pagination.current = 1; // 重置页码
      this.$emit("batchSelectClose");
    },
    // 确定按钮
    handleOK() {
      this.pagination.current = 1; // 重置页码
      this.$emit("batchSelectClose");
    },
    // 关闭按钮（移除所有已选词条）
    batchSelectCancel() {
      Modal.confirm({
        title: "是否确认关闭?",
        icon: createVNode(ExclamationCircleOutlined),
        content: "确认关闭后，已选择的词条将被清空",
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          this.pagination.current = 1; // 重置页码
          this.$emit("batchSelectCancel");
        },
      });
    },
    // 删除按钮（删除所有所选术语）
    deleteSykEntry() {
      deleteSykEntry(this.dataSource)
        .then((res) => {
          message.success("删除成功!");
          // console.log("删除成功", res);
          this.pagination.current = 1; // 重置页码

          this.$emit("batchSelectCancel");
        })
        .catch((err) => {
          message.error("删除失败！", err.message);
          // console.log("术语删除失败！", err);
        })
        .finally(() => {
          this.$emit("refresh"); // 让父组件执行refresh函数
          // console.log("刷新");
        });
    },
    // 分页切换
    pageChange(page, pageSize) {
      pageChange(this, page, pageSize);
    },
  },
};
</script>
<style lang="less" scoped>
.table {
  width: 100%;
  margin-top: 5px;
  position: relative;
}
.ant-form-inline .ant-form-item-with-help {
  margin-bottom: 0px;
}
</style>