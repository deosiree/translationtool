<template>
  <CustomModal :modalWidth="modalWidth" modalTitle="批量选择" :visible="visible" @handleClose="handleClose" @handleOK="handleOK">
    <div class="table">
      <div>已选词条：</div>
      <a-config-provider :locale="locale">
        <a-table class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="{x:'100vw' , y: '60vh'}" :pagination="pagination"
          :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="batchSelectable" bordered>
          <template #bodyCell="{ column, record, text }">
            <template v-if="column.dataIndex === 'translateState'">
              <template v-if="text === '0'">
                <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">未翻译</span>
              </template>
              <template v-if="text === '1'">
                <a-badge color="#FBB31F" /><span style="color:#FBB31F">待审核</span>
              </template>
              <template v-if="text === '2'">
                <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
              </template>
              <template v-if="text === '3'">
                <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
              </template>
            </template>
            <template v-if="column.dataIndex === 'publicState'">
              <template v-if="text === 0">
                <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
              </template>
              <template v-if="text === 1">
                <a-badge color="#FBB31F" /><span style="color:#FBB31F">审核中</span>
              </template>
              <template v-if="text === 2">
                <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
              </template>
              <template v-if="text === 3">
                <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
              </template>
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
      <a-button type="primary" danger @click="deleteSykEntry">删除</a-button>
    </template>
  </CustomModal>
</template>
<script>
import CustomModal from "@/components/modal/index.vue";
import zh_CN from "ant-design-vue/es/locale/zh_CN";
import {
  ExclamationCircleOutlined,
  DeleteOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import { getSykNotUsed, deleteSykEntry } from "@/http/api/glossary";
import { pageChange } from "@/utils/tableUtils";
export default {
  components: {
    CustomModal,
    ExclamationCircleOutlined,
    DeleteOutlined,
  },
  emits: ["batchSelectClose", "removeEntry", "batchSelectCancel", "refresh"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    columns: {
      type: Array,
    },
    dataSource: {
      type: Array,
    },
  },
  data() {
    return {
      locale: zh_CN,
      modalWidth: "60%",
      localColumns: [...this.columns], // 创建 columns 的副本
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
  // watch: {
  //   columns: {
  //     deep: true,
  //     handler(newColumns) {
  //       this.localColumns = [...newColumns]; // 当 props 变化时更新副本
  //       this.localColumns.forEach((column) => {
  //         column.width = 100;
  //       });
  //     },
  //   },
  // },
  methods: {
    // 移除某条已选词条（每行最右边的按钮
    remove(record) {
      this.$emit("removeEntry", record);
    },
    // 取消按钮
    handleClose() {
      this.$emit("batchSelectClose");
    },
    // 确定按钮
    handleOK() {
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
          this.$emit("batchSelectCancel");
        },
      });
    },
    // 删除按钮（删除所有所选术语）
    deleteSykEntry() {
      Modal.confirm({
        title: "是否确定删除?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          // console.log("已选词条", this.dataSource);
          deleteSykEntry(this.dataSource)
            .then((res) => {
              message.success("删除成功!");
              // console.log("删除成功", res);
              this.$emit("batchSelectCancel");
            })
            .catch((err) => {
              message.error("删除失败！");
              // console.log("术语删除失败！", err);
            })
            .finally(() => {
              this.$emit("refresh");
            });
        },
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