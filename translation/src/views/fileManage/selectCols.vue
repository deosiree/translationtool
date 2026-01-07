<template>
  <CustomModal
    :visible="visible"
    :okLoading="loading"
    modalTitle="去重导出"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
  >
    <div class="deduplicate-content">
      <div class="deduplicate-section">
        <div class="section-title">选择去重列：</div>
        <a-checkbox-group v-model:value="selectedColumns" class="column-checkbox-group">
          <a-checkbox
            v-for="col in availableColumns"
            :key="col.key"
            :value="col.key"
            class="column-checkbox"
          >
            {{ col.title }}
          </a-checkbox>
        </a-checkbox-group>
      </div>
      
      <div class="deduplicate-tip">
        <a-alert
          message="提示"
          description="至少选择一列用于去重，系统将根据选择的列组合判断重复项"
          type="info"
          show-icon
        />
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
    };
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.initAvailableColumns();
      }
    },
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
      this.$emit("confirm", this.selectedColumns);
    },
    afterClose() {
      this.selectedColumns = [];
    },
  },
};
</script>

<style scoped>
.deduplicate-content {
  padding: 8px 0;
}

.deduplicate-section {
  margin-bottom: 24px;
}

.section-title {
  font-weight: 500;
  margin-bottom: 12px;
  color: rgba(0, 0, 0, 0.85);
}

.column-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.column-checkbox {
  margin-right: 0;
}

.deduplicate-tip {
  margin-top: 16px;
}
</style>
