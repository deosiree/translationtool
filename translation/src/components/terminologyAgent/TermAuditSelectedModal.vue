<template>
  <CustomModal
    :modalWidth="modalWidth"
    modalTitle="已选术语"
    :visible="visible"
    :showOk="false"
    :showCancel="false"
    @handleClose="handleClose"
    @handleOK="handleClose"
  >
    <div class="table">
      <div>已选术语：{{ dataSource.length }} 条</div>
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
            <template v-if="column.dataIndex === 'source_text'">
              <span v-text="formatEntryText(record.source_text)"></span>
            </template>
            <template v-if="column.dataIndex === 'entry_comment'">
              <SpanByTipsFill
                :content="record.entry_comment"
                :max-width="column.width"
              />
            </template>
            <template v-if="column.dataIndex === 'suggested_translation'">
              <a-tag color="blue">{{
                record.suggested_translation || "未生成"
              }}</a-tag>
            </template>
            <template v-if="column.dataIndex === 'confidence'">
              <a-tag :color="confidenceColor(record.confidence)">
                {{ formatConfidence(record.confidence) }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'operation'">
              <DeleteOutlined
                style="color: #369fff; font-size: 16px"
                title="取消选择"
                @click="remove(record)"
              />
            </template>
          </template>
        </a-table>
      </a-config-provider>
    </div>
    <template v-slot:leftBottomBtn>
      <a-button @click="handleClose">关闭</a-button>
      <a-button
        type="primary"
        danger
        :loading="batchLoading"
        :disabled="!dataSource.length"
        @click="confirmBatchReview('rejected')"
      >
        批量拒绝
      </a-button>
      <a-button
        type="primary"
        :loading="batchLoading"
        :disabled="!dataSource.length"
        @click="confirmBatchReview('approved')"
      >
        批量同意
      </a-button>
      <a-button
        style="margin-left: 8px"
        :loading="splitLoading"
        :disabled="!dataSource.length"
        @click="handleSplitImport"
      >
        切分
      </a-button>
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
import { createVNode } from "vue";
import { batchReviewTerms, splitTermAuditItemsByIds } from "@/http/api/terminologyAgent";
import {
  formatConfidence,
  formatEntryText,
  removeLocalPendingAudit,
} from "@/utils/agentPendingAudits";
import { pageChange } from "@/utils/selectionUtils";
import SpanByTipsFill from "@/components/SpanByTips/SpanByTipsFill/index.vue";
import { ellipsisCustomCell } from "@/components/ColumnFilter/columnBuilder.js";

export default {
  components: {
    CustomModal,
    DeleteOutlined,
    SpanByTipsFill,
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
      columns: [
        {
          title: "词条",
          dataIndex: "source_text",
          ellipsis: true,
          width: 180,
        },
        {
          title: "Comment",
          dataIndex: "entry_comment",
          ellipsis: true,
          width: 100,
          customCell: ellipsisCustomCell(100),
        },
        {
          title: "建议翻译",
          dataIndex: "suggested_translation",
          width: 160,
        },
        {
          title: "目标语种",
          dataIndex: "target_lang",
          width: 90,
        },
        {
          title: "置信度",
          dataIndex: "confidence",
          width: 80,
        },
        {
          title: "操作",
          dataIndex: "operation",
          width: 60,
          fixed: "right",
        },
      ],
      batchLoading: false,
      splitLoading: false,
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
    dataSource: {
      handler(list) {
        this.pagination.total = list.length;
      },
      immediate: true,
    },
  },
  methods: {
    formatConfidence,
    formatEntryText,
    confidenceColor(confidence) {
      if (confidence == null) return "default";
      return Number(confidence) >= 0.8 ? "green" : "orange";
    },
    remove(record) {
      const newDataSource = this.dataSource.filter(
        (item) => item.id !== record.id,
      );
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
    confirmBatchReview(action) {
      const label = action === "approved" ? "同意" : "拒绝";
      Modal.confirm({
        title: `是否确认批量${label}？`,
        icon: createVNode(ExclamationCircleOutlined),
        content: `将对 ${this.dataSource.length} 条术语执行批量${label}`,
        okText: "确认",
        cancelText: "取消",
        style: { top: "30%" },
        onOk: () => this.runBatchReview(action),
      });
    },
    async runBatchReview(action) {
      if (!this.dataSource.length) return;
      this.batchLoading = true;
      const localItems = this.dataSource.filter(
        (item) => item._local || item._mock,
      );
      const remoteItems = this.dataSource.filter(
        (item) => !item._local && !item._mock,
      );
      let successCount = localItems.length;
      let failedCount = 0;
      const failures = [];

      localItems.forEach((item) => {
        removeLocalPendingAudit(item.id);
      });

      if (remoteItems.length) {
        try {
          const res = await batchReviewTerms(
            remoteItems.map((item) => item.id),
            action,
          );
          const data = res.data || {};
          successCount += data.success_count ?? 0;
          failedCount += data.failed_count ?? 0;
          if (Array.isArray(data.failures)) {
            failures.push(...data.failures);
          }
        } catch (err) {
          message.error("批量操作失败");
          console.error(err);
          this.batchLoading = false;
          return;
        }
      }

      const label = action === "approved" ? "同意" : "拒绝";
      if (failedCount > 0) {
        const detail = failures
          .slice(0, 3)
          .map((item) => `${item.id}: ${item.reason}`)
          .join("；");
        message.warning(
          `批量${label}完成：成功 ${successCount} 条，失败 ${failedCount} 条${detail ? `（${detail}）` : ""}`,
        );
      } else if (action === "approved") {
        message.success(
          `批量同意成功，共 ${successCount} 条，已回写工作台可在翻译审核中查看`,
        );
      } else {
        message.success(`批量${label}成功，共 ${successCount} 条`);
      }

      this.batchLoading = false;
      this.pagination.current = 1;
      this.$emit("cancelSelect");
      this.$emit("refresh");
    },
    pageChange(page, pageSize) {
      pageChange(this, page, pageSize);
    },
    async handleSplitImport() {
      if (!this.dataSource.length) return;
      this.splitLoading = true;
      try {
        const ids = this.dataSource.map((item) => item.id).filter(Boolean);
        if (!ids.length) {
          message.warning("已选术语缺少 ID，无法切分");
          return;
        }
        const res = await splitTermAuditItemsByIds(ids);
        const count = res?.data?.success_count ?? 0;
        message.success(`切分完成，共处理 ${count} 条术语`);
        this.handleClose();
        this.$emit("refresh");
      } catch (err) {
        message.error(err?.message || "切分失败");
      } finally {
        this.splitLoading = false;
      }
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
