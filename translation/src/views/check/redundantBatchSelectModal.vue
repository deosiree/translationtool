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
      <DeleteButton :dataSource="dataSource" :deleteApi="deleteNotUseEntry"></DeleteButton>
    </template>
  </CustomModal>
</template>
<script>
import CustomModal from "@/components/modal/index.vue";
import DeleteButton from "@/components/Button/deleteButton.vue";
import zh_CN from "ant-design-vue/es/locale/zh_CN";
import {
  ExclamationCircleOutlined,
  DeleteOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import { deleteNotUseEntry } from "@/http/api/check";
import { pageChange } from "@/utils/tableUtils";
export default {
  components: {
    CustomModal,
    DeleteButton,
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
    columns: {
      type: Array,
      default: () => [],
    },
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
    search: {
      type: Object,
      default: () => ({}),
    },
  },
  data() {
    return {
      locale: zh_CN,
      modalWidth: "60%",
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
          this.$emit("batchSelectCancel");
        },
      });
    },
    // 删除按钮（删除所有所选术语）
    deleteNotUseEntry() {
      // console.log("要删的词条是", this.dataSource);
      let data = [];
      for (let item of this.dataSource) {
        data.push(String(item.id));
      }
      // console.log("要删的词条id是", data);
      let params = {
        i18nURL: this.search.i18nURL,
        classfyID: this.search.classfyID,
        // pageIndex: this.search.pageIndex,
        // pageSize: this.search.pageSize,
      };
      deleteNotUseEntry(params, data)
        .then((res) => {
          message.success("删除成功!");
          // console.log("删除成功", res);
          this.pagination.total = 0; // 重置总条数
          this.$emit("batchSelectCancel");
        })
        .catch((err) => {
          message.error("删除失败！",err.message);
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