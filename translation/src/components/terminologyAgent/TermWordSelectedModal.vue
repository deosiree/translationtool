<template>
  <CustomModal
    :modalWidth="modalWidth"
    modalTitle="已选词片"
    :visible="visible"
    :showOk="false"
    :showCancel="false"
    @handleClose="handleClose"
    @handleOK="handleClose"
  >
    <div class="table">
      <div>已选词片：{{ dataSource.length }} 条</div>
      <a-config-provider :locale="locale">
        <a-table
          class="ant-table-striped"
          :columns="columns"
          :data-source="dataSource"
          :scroll="{ x: 'max-content', y: '60vh' }"
          :pagination="pagination"
          :row-class-name="
            (_record, index) => (index % 2 === 1 ? 'table-striped' : null)
          "
          row-key="id"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'status'">
              <TransStateBadge :translateState="record.status" />
            </template>
            <template v-else-if="column.dataIndex === 'operation'">
              <DeleteOutlined
                style="color: #369fff; font-size: 16px; cursor: pointer"
                title="取消选择"
                @click="remove(record)"
              />
            </template>
          </template>
        </a-table>
      </a-config-provider>
    </div>
    <template v-slot:leftBottomBtn>
      <a-button @click="confirmClearSelection">关闭</a-button>
      <a-button
        type="primary"
        danger
        :loading="deleteLoading"
        :disabled="!dataSource.length"
        @click="confirmBatchDelete"
      >
        批量删除
      </a-button>
      <a-button
        type="primary"
        class="resetBtn"
        :loading="reviewLoading"
        :disabled="!pendingCount"
        @click="confirmBatchReview('approved')"
      >
        通过
      </a-button>
      <a-button
        type="primary"
        class="yellowBtn"
        :loading="reviewLoading"
        :disabled="!pendingCount"
        @click="confirmBatchReview('rejected')"
      >
        驳回
      </a-button>
      <a-button
        type="primary"
        :disabled="!dataSource.length"
        @click="onExport"
      >
        导出
      </a-button>
    </template>
  </CustomModal>
</template>

<script>
import CustomModal from "@/components/modal/index.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import zh_CN from "ant-design-vue/es/locale/zh_CN";
import {
  ExclamationCircleOutlined,
  DeleteOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import { createVNode } from "vue";
import { batchDeleteTermWords, exportTermWords, batchReviewTermWords } from "@/http/api/terminologyAgent";
import { pageChange } from "@/utils/selectionUtils";
import { downloadBlobResponse } from "@/utils/fileUtils";

export default {
  name: "TermWordSelectedModal",
  components: {
    CustomModal,
    TransStateBadge,
    DeleteOutlined,
  },
  emits: [
    "close",
    "cancelSelect",
    "refresh",
    "update:dataSource",
    "update:selectedRowKeys",
    "update:selectedRows",
  ],
  props: {
    visible: {
      type: Boolean,
      default: false,
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
  },
  data() {
    return {
      locale: zh_CN,
      modalWidth: "60%",
      deleteLoading: false,
      reviewLoading: false,
      columns: [
        { title: "词片", dataIndex: "word", ellipsis: true, width: 160 },
        { title: "翻译", dataIndex: "translate", ellipsis: true, width: 180 },
        { title: "翻译类型", dataIndex: "target_lang", width: 100 },
        { title: "可见范围", dataIndex: "department", ellipsis: true, width: 120 },
        { title: "comment", dataIndex: "comment", ellipsis: true, width: 120 },
        { title: "翻译状态", dataIndex: "status", width: 100 },
        {
          title: "操作",
          dataIndex: "operation",
          width: 60,
          fixed: "right",
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
  computed: {
    /**
     * 已选中待审核词片数
     * @returns {number}
     */
    pendingCount() {
      return (this.dataSource || []).filter((r) => String(r.status) === "1").length;
    },
  },
  watch: {
    dataSource: {
      handler(list) {
        this.pagination.total = (list || []).length;
      },
      immediate: true,
    },
  },
  methods: {
    remove(record) {
      const newDataSource = this.dataSource.filter((item) => item.id !== record.id);
      const newSelectedRowKeys = this.selectedRowKeys.filter(
        (item) => item !== record.id,
      );
      const newSelectedRows = this.selectedRows.filter(
        (item) => item.id !== record.id,
      );
      this.$emit("update:dataSource", newDataSource);
      this.$emit("update:selectedRowKeys", newSelectedRowKeys);
      this.$emit("update:selectedRows", newSelectedRows);
    },
    handleClose() {
      this.pagination.current = 1;
      this.$emit("close");
    },
    confirmClearSelection() {
      if (!this.dataSource.length) {
        this.handleClose();
        return;
      }
      Modal.confirm({
        title: "是否确认关闭?",
        icon: createVNode(ExclamationCircleOutlined),
        content: "确认关闭后，已选择的词片将被清空",
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          this.pagination.current = 1;
          this.$emit("cancelSelect");
        },
      });
    },
    confirmBatchDelete() {
      if (!this.dataSource.length) return;
      Modal.confirm({
        title: "是否确认批量删除？",
        icon: createVNode(ExclamationCircleOutlined),
        content: `将对 ${this.dataSource.length} 条词片执行物理删除，不可恢复`,
        okText: "确认",
        cancelText: "取消",
        okType: "danger",
        style: { top: "30%" },
        onOk: () => this.runBatchDelete(),
      });
    },
    async runBatchDelete() {
      const ids = this.dataSource.map((item) => item.id).filter(Boolean);
      if (!ids.length) return;
      this.deleteLoading = true;
      try {
        await batchDeleteTermWords(ids);
        message.success(`批量删除成功，共 ${ids.length} 条`);
        this.pagination.current = 1;
        this.$emit("cancelSelect");
        this.$emit("refresh");
      } catch (err) {
        message.error(err?.message || "批量删除失败");
      } finally {
        this.deleteLoading = false;
      }
    },
    /**
     * @param {"approved"|"rejected"} action
     * @returns {void}
     */
    confirmBatchReview(action) {
      const pending = (this.dataSource || []).filter(
        (r) => String(r.status) === "1" && r.id,
      );
      if (!pending.length) {
        message.warning("已选词片中没有待审核项");
        return;
      }
      const approve = action === "approved";
      Modal.confirm({
        title: approve ? "批量通过？" : "批量驳回？",
        icon: createVNode(ExclamationCircleOutlined),
        content: `将对 ${pending.length} 条待审核词片设为「${
          approve ? "已审核" : "审核不通过"
        }」`,
        okText: "确认",
        cancelText: "取消",
        okType: approve ? "primary" : "danger",
        style: { top: "30%" },
        onOk: () => this.runBatchReview(action, pending.map((r) => r.id)),
      });
    },
    /**
     * @param {"approved"|"rejected"} action
     * @param {string[]} ids
     * @returns {Promise<void>}
     */
    async runBatchReview(action, ids) {
      this.reviewLoading = true;
      try {
        const res = await batchReviewTermWords(ids, action);
        const data = res?.data ?? res;
        const updated = data?.updated ?? 0;
        const skipped = data?.skipped ?? 0;
        message.success(
          `${action === "approved" ? "通过" : "驳回"}完成：更新 ${updated}，跳过 ${skipped}`,
        );
        this.$emit("refresh");
        // 从已选列表移除已审阅项（状态已变）
        const done = new Set(ids);
        const remain = (this.dataSource || []).filter((r) => !done.has(r.id));
        const remainKeys = remain.map((r) => r.id);
        this.$emit("update:dataSource", remain);
        this.$emit("update:selectedRows", remain);
        this.$emit("update:selectedRowKeys", remainKeys);
        if (!remain.length) {
          this.pagination.current = 1;
          this.$emit("close");
        }
      } catch (err) {
        message.error(err?.message || "批量审阅失败");
      } finally {
        this.reviewLoading = false;
      }
    },
    /**
     * 导出已选词片为标准 Excel
     * @returns {Promise<void>}
     */
    async onExport() {
      const ids = this.dataSource.map((item) => item.id).filter(Boolean);
      if (!ids.length) {
        message.warning("没有可导出的词片");
        return;
      }
      try {
        const resp = await exportTermWords({ ids });
        const saved = await downloadBlobResponse(resp, "term_word_export.xlsx");
        if (saved) message.success(`已导出 ${ids.length} 条`);
      } catch (err) {
        message.error(err?.message || "导出失败");
      }
    },
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
</style>
