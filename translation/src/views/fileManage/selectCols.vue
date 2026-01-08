<template>
  <CustomModal :visible="visible" :okLoading="loading" modalTitle="去重导出" @handleClose="handleClose" @handleOK="handleOK"
    @afterClose="afterClose">
    <div class="deduplicate-content">
      <div class="deduplicate-layout">
        <div class="deduplicate-layout-left">
          <div class="deduplicate-section">
            <div class="section-title">选择去重列：</div>
            <a-table ref="deduplicateTable" :columns="tableColumns" :data-source="tableData" :row-selection="rowSelection"
              :pagination="false" :scroll="tableHeight ? { y: tableHeight } : undefined" :bordered="true" size="small"
              class="deduplicate-table" />
          </div>
        </div>

        <div class="deduplicate-layout-right">
          <div class="rules-section">
            <div class="section-title">选择其他规则：</div>
            <a-table ref="rulesTable" :columns="rulesColumns" :data-source="rulesData" :row-selection="rulesRowSelection"
              :pagination="false" :scroll="tableHeight ? { y: tableHeight } : undefined" :bordered="true" size="small"
              class="rules-table" />
          </div>
        </div>
      </div>

      <div class="tip">
        <a-alert message="提示" description="至少选择一列用于去重，系统将根据选择的列组合判断重复项" type="info" show-icon />
      </div>
    </div>
  </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";

export default {
  components: {
    CustomModal,
  },
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    columns: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      selectedColumns: [],
      availableColumns: [],
      tableHeight: undefined,
      selectedRules: [],
    };
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.initAvailableColumns();
        this.$nextTick(() => {
          this.calculateTableHeight();
        });
      }
    },
  },
  computed: {
    tableColumns() {
      return [
        {
          title: '列名',
          dataIndex: 'title',
          key: 'title'
        }
      ];
    },
    tableData() {
      return this.availableColumns.map(col => ({
        key: col.key,
        title: col.title,
        dataIndex: col.dataIndex
      }));
    },
    rowSelection() {
      return {
        selectedRowKeys: this.selectedColumns,
        onChange: (selectedRowKeys) => {
          this.selectedColumns = selectedRowKeys;
        }
      };
    },
    rulesColumns() {
      return [
        {
          title: '规则名称',
          dataIndex: 'name',
          key: 'name'
        }
      ];
    },
    rulesData() {
      return [
        { key: 'special_chars', name: '全部特殊字符' },
        { key: 'all_numbers', name: '全部数字' },
        { key: 'contains_newline', name: '包含换行符' },
        { key: 'contains_spaces', name: '包含连续空格' }
      ];
    },
    rulesRowSelection() {
      return {
        selectedRowKeys: this.selectedRules,
        onChange: (selectedRowKeys) => {
          this.selectedRules = selectedRowKeys;
        }
      };
    }
  },
  methods: {
    initAvailableColumns() {
      this.availableColumns = this.columns
        .filter((col) => col.dataIndex && !["operation", "selection"].includes(col.dataIndex))
        .map((col) => ({
          key: col.dataIndex,
          title: col.title,
          width: col.width,
        }));
      this.selectedColumns = [];
    },
    handleClose() {
      this.$emit("update:visible", false);
    },
    handleOK() {
      if (this.selectedColumns.length === 0) {
        message.error("请至少选择一列用于去重");
        return;
      }
      this.$emit("confirm", {
        columns: this.selectedColumns,
        rules: this.selectedRules
      });
    },
    afterClose() {
      this.selectedColumns = [];
      this.selectedRules = [];
    },
    calculateTableHeight() {
      this.$nextTick(() => {
        const tableElement = this.$refs.deduplicateTable?.$el;
        if (!tableElement) return;

        const tableBody = tableElement.querySelector('.ant-table-container');
        const scrollHeight = tableBody?.scrollHeight || 0;

        console.log("当前表单高度", scrollHeight)
        if (scrollHeight > 300) {
          this.tableHeight = 300;
        } else {
          this.tableHeight = undefined;
        }
      });
    },
  },
};
</script>

<style scoped>
.deduplicate-content {
  padding: 8px 0;
}

.deduplicate-layout {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}

.deduplicate-layout-left {
  flex: 1;
}

.deduplicate-layout-right {
  flex: 1;
}

.deduplicate-section {
  margin-bottom: 24px;
}

.rules-section {
  margin-bottom: 24px;
}

.section-title {
  font-weight: 500;
  margin-bottom: 12px;
  color: rgba(0, 0, 0, 0.85);
}

.deduplicate-table :deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 500;
}

.deduplicate-table :deep(.ant-table-tbody > tr:hover > td) {
  background-color: #f5f5f5;
}

.rules-table :deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 500;
}

.rules-table :deep(.ant-table-tbody > tr:hover > td) {
  background-color: #f5f5f5;
}

.tip {
  margin-top: 16px;
}
</style>

<style>
.ant-modal-content {
  border-radius: 8px !important;
}
</style>
