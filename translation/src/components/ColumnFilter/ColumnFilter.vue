<template>
  <a-popover
    trigger="click"
    :placement="placement"
    :overlayStyle="overlayStyle"
  >
    <template #content>
      <div class="column-filter-popover">
        <div class="column-filter-header">
          <span>展示列</span>
          <a-button
            size="small"
            @click="handleReset"
          >
            重置
          </a-button>
        </div>
        <a-checkbox-group
          :value="modelValue"
          @change="onChange"
        >
          <a-row
            v-for="item in columns"
            :key="item.value"
          >
            <a-col :span="24">
              <a-checkbox
                :value="item.value"
                :disabled="item.required"
              >
                {{ item.label }}
              </a-checkbox>
            </a-col>
          </a-row>
        </a-checkbox-group>
      </div>
    </template>
    <a-button
      type="primary"
      :size="buttonSize"
    >
      <template #icon>
        <SettingOutlined />
      </template>
      展示列
    </a-button>
  </a-popover>
</template>

<script>
import { SettingOutlined } from "@ant-design/icons-vue";
import { getDefaultColumnSelection, mergeColumnSelection } from "./columnTable.js";

export default {
  name: "ColumnFilter",
  components: {
    SettingOutlined,
  },
  props: {
    /** 当前勾选的列 value 数组 */
    modelValue: {
      type: Array,
      default: () => [],
    },
    /** 全量列定义，含 required / visible */
    columns: {
      type: Array,
      default: () => [],
    },
    /** 透传 a-popover overlayStyle */
    overlayStyle: {
      type: Object,
      default: () => ({}),
    },
    placement: {
      type: String,
      default: "leftTop",
    },
    buttonSize: {
      type: String,
      default: "small",
    },
  },
  emits: ["change"],
  methods: {
    onChange(checkedValue) {
      const merged = mergeColumnSelection(checkedValue, this.columns);
      this.$emit("change", merged);
    },
    handleReset() {
      this.onChange(getDefaultColumnSelection(this.columns));
    },
  },
};
</script>

<style scoped>
.column-filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  margin-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  font-weight: 500;
}
</style>
