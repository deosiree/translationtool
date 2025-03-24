<template>
  <CustomModal :modalWidth="modalWidth" modalTitle="批量选择" :visible="visible" @handleClose="handleClose" @handleOK="handleOK">
    <div class="table">
      <div>已选词条：</div>
      <a-config-provider :locale="locale">
        <a-table class="ant-table-striped" :columns="localColumns" :data-source="dataSource" :scroll="{x:'100vw' , y: '60vh'}"
          :pagination="pagination" :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="batchSelectable" bordered>
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
    dataSource: {
      type: Array,
    },
  },
  data() {
    return {
      locale: zh_CN,
      modalWidth: "60%",
      localColumns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 80,
          customRender: (text, record, index, column) => {
            return (
              text.index +
              1 +
              this.pagination.pageSize * (this.pagination.current - 1)
            );
          },
          fixed: "left",
          index: 0.1,
        },
        {
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 150,
          fixed: "left",
          resizable: true,
          index: 1,
        },
        {
          title: "翻译",
          dataIndex: "translate",
          align: "center",
          width: 150,
          resizable: true,
          index: 2,
        },
        {
          title: "翻译类型",
          dataIndex: "type",
          align: "center",
          width: 230,
          resizable: true,
          index: 3,
        },
        {
          title: "翻译状态",
          dataIndex: "translateState",
          align: "center",
          width: 180,
          resizable: true,
          index: 4,
        },
        {
          title: "翻译字符数",
          dataIndex: "charLength",
          align: "center",
          width: 150,
          index: 5,
        },
        {
          title: "可见范围",
          dataIndex: "visualRange",
          align: "center",
          width: 150,
          index: 6,
        },
        {
          title: "词条审核员",
          dataIndex: "entryAuditor",
          align: "center",
          width: 150,
          index: 7,
        },
        {
          title: "公开状态",
          dataIndex: "publicState",
          align: "center",
          width: 150,
          index: 8,
        },
        {
          title: "最大限制长度",
          dataIndex: "maxLength",
          align: "center",
          width: 150,
          index: 9,
        },
        {
          title: "审核意见",
          dataIndex: "auditSuggest",
          align: "center",
          width: 230,
          ellipsis: true,
          resizable: true,
          index: 10,
        },
        {
          title: "备注",
          dataIndex: "remark",
          align: "center",
          width: 200,
          index: 11,
        },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          width: 150,
          fixed: "right",
          index: 12,
        },
      ],
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
      this.$emit("removeEntry", record);
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
              this.pagination.current = 1; // 重置页码

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