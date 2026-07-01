<template>
  <a-popover
    trigger="click"
    :placement="placement"
    :overlayStyle="overlayStyle"
  >
    <template #content>
      <div class="column-filter-popover">
        <div class="column-filter-header">
          <span>{{ title }}</span>
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
            v-for="item in visibleColumns"
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
      :ghost="ghost"
    >
      <template #icon>
        <SettingOutlined />
      </template>
      {{ buttonText }}
    </a-button>
  </a-popover>
</template>

<script>
import { SettingOutlined } from "@ant-design/icons-vue";
import {
  getDefaultColumnSelection,
  mergeColumnSelection,
  changeColumn,
  findTableHost,
  persistSelectionPref,
} from "./columnTable.js";

export default {
  name: "ColumnFilter",
  components: {
    SettingOutlined,
  },
  props: {
    /** 当前勾选的列 value 数组（v-model） */
    modelValue: {
      type: Array,
      default: () => [],
    },
    /** 全量列定义，含 required / visible */
    columns: {
      type: Array,
      default: () => [],
    },
    /** localStorage 键名；有值时组件内持久化列偏好 */
    colPrefName: {
      type: String,
      default: "",
    },
    /** 动态增列时的默认宽度 */
    normalWidth: {
      type: Number,
      default: 100,
    },
    /** 是否启用列头筛选行为 */
    needFilter: {
      type: Boolean,
      default: false,
    },
    /** 表格 host vm；缺省时向上查找含 columnSettingsList 的祖先 */
    tableHost: {
      type: Object,
      default: null,
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
    /** popover 标题 */
    title: {
      type: String,
      default: "展示列",
    },
    /** 触发按钮文案 */
    buttonText: {
      type: String,
      default: "展示列",
    },
    /** 触发按钮 ghost 样式 */
    ghost: {
      type: Boolean,
      default: false,
    },
    /** 持久化模式：tableColumn 改表格列；selectionOnly 仅持久化勾选 */
    persistMode: {
      type: String,
      default: "tableColumn",
      validator: (v) => ["tableColumn", "selectionOnly"].includes(v),
    },
  },
  emits: ["change", "update:modelValue"],
  computed: {
    visibleColumns() {
      return (this.columns || []).filter((c) => !c.hidden);
    },
  },
  methods: {
    /**
     * 解析持久化配置：props 优先，否则读 tableHost.$columnFilterPref
     * @returns {{ colPrefName: string, normalWidth: number, needFilter: boolean }}
     */
    resolvePersistConfig(host) {
      const pref = host?.$columnFilterPref;
      const colPrefName = this.colPrefName || pref?.colPrefName || "";
      if (this.colPrefName) {
        return {
          colPrefName,
          normalWidth: this.normalWidth,
          needFilter: this.needFilter,
        };
      }
      return {
        colPrefName,
        normalWidth: pref?.normalWidth ?? this.normalWidth,
        needFilter: pref?.needFilter ?? this.needFilter,
      };
    },
    /**
     * 勾选变更：合并必选列、持久化并更新表格 host
     * @param {string[]} checkedValue checkbox-group 返回值
     */
    onChange(checkedValue) {
      const merged = mergeColumnSelection(checkedValue, this.columns);
      const host = this.tableHost ?? findTableHost(this.$parent);
      const { colPrefName, normalWidth, needFilter } =
        this.resolvePersistConfig(host);

      if (this.persistMode === "selectionOnly") {
        if (colPrefName) {
          persistSelectionPref(colPrefName, merged, this.columns);
        }
        this.$emit("update:modelValue", merged);
        this.$emit("change", merged);
        return;
      }

      if (colPrefName && host) {
        changeColumn(
          colPrefName,
          normalWidth,
          merged,
          host,
          needFilter,
          this.columns
        );
      }

      this.$emit("update:modelValue", merged);
      this.$emit("change", merged);
    },
    /** 重置为 columnSettingsList 默认勾选 */
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
