<template>
  <div class="pipeline-panel">
  <!--
    PipelinePanel：工作台流水线表格壳（任务信息 + 工具栏 + a-table）
    从各 modal 解耦出的共用表格渲染；列 dataIndex 与 bodyCell 分支对应关系见下方注释。
  -->
  <!-- 任务信息条（名称/产品/分类/语种） -->
  <div class="workbench-task-info">
    <div class="workbench-task-info__item">任务名称：{{ task.name }}</div>
    <div class="workbench-task-info__item">产品名称：{{ task.productName }}</div>
    <div class="workbench-task-info__item">上级分类名称：{{ task.classifyName }}</div>
    <div class="workbench-task-info__item">翻译语种：{{ task.translateType }}</div>
    <slot name="taskExtra" />
  </div>
  <slot name="beforeFormBar" />
  <div class="workbench-form-bar">
    <div class="workbench-form-bar__main">
      <slot />
      <div
        v-if="showInlineColumnActions"
        class="workbench-action-group workbench-action-group--offset"
      >
        <slot name="columnActions">
          <ColumnActions
            v-bind="columnActions"
            @update:model-value="$emit('update:modelValue', $event)"
            @change="$emit('columnsChange', $event)"
          />
        </slot>
      </div>
    </div>
    <div
      v-if="$slots.trailing"
      class="workbench-form-bar__trailing"
    >
      <slot name="trailing" />
    </div>
  </div>
  <slot name="subToolbar" />
  <TableViewport
    bordered
    class="ant-table-striped table-cell-overflow"
    :fill-height="true"
    :empty-overlay="true"
    :columns="columns"
    :layout-key="tableLayoutKey"
    :adaptive-column-keys="adaptiveColumnKeys"
    :data-source="dataSource"
    :row-key="(record) => record.id"
    :scroll="scroll"
    :pagination="pagination"
    :loading="loading"
    :row-selection="rowSelection"
    :row-class-name="onRowClassName"
    :custom-row="customRow"
    :show-expand-column="showExpandIcon"
    :expand-icon-column-index="expandIconColumnIndex"
    :children-column-name="childrenColumnName"
    v-bind="$attrs"
    @change="onTableChange"
  >
    <template #headerCell="{ title, column }">
      <TableHeaderHint
        v-if="['tag', 'comment'].includes(column.dataIndex)"
        :title="title"
        :column="column"
      />
      <!-- 列头：colValue 有值时省略号 + Tooltip -->
      <a-tooltip v-else-if="column.dataIndex === 'maxLength'">
        <template #title>
          译文字符数上限，最多可输入相应数量的字或字母；空格也计数，首尾空格不忽略。
        </template>
        <span>{{ title }} <InfoCircleOutlined /></span>
      </a-tooltip>
      <CellOverflowTooltip v-else-if="column.colValue" :content="title">
        {{ title }}
      </CellOverflowTooltip>
      <span v-else>{{ title }}</span>
    </template>
    <template #bodyCell="{ column, text, record }">
      <!-- entry：词条原文 -->
      <template v-if="column.dataIndex === 'entry'">
        <TableCellText :text="formatEntryText(text)" />
      </template>
      <!-- 普通文本列：浏览态由 TableCellText 内部统一使用 Tooltip -->
      <template v-else-if="isPlainTextColumn(column)">
        <TableCellText
          :text="formatCellText(text)"
          :value="editableData[record.id]?.[column.dataIndex] ?? ''"
          :editing="isEditableTextColumn(column) && isEditing(record)"
          :error-message="cellErrors[record.id]?.[column.dataIndex]"
          @update:value="$emit('cellInput', $event, record, column)"
        />
      </template>
      <!-- diFileName：DI 文件名（编辑态 InputIME） -->
      <template v-else-if="column.dataIndex === 'diFileName'">
        <template v-if="edit.includes('diFileName') && isEditing(record)">
          <InputIME
            :value="editableData[record.id][column.dataIndex]"
            @update:value="(val) => $emit('cellInput', val, record, column)"
          />
        </template>
        <template v-else>
          <CellOverflowTooltip :content="formatCellText(text)" />
        </template>
      </template>
      <!-- tag：Tag 标签（编辑态 InputIME；浏览态 a-tag 列表） -->
      <template v-else-if="column.dataIndex === 'tag'">
        <template v-if="edit.includes('tag') && isEditing(record)">
          <InputIME
            :value="editableData[record.id][column.dataIndex]"
            @update:value="(val) => $emit('cellInput', val, record, column)"
          />
          <a-tooltip placement="top">
            <template #title>
              <span>多个Tag按分号分割！</span>
            </template>
            <InfoCircleOutlined style="margin-left: 3px" />
          </a-tooltip>
        </template>
        <template v-else>
          <TableTagCell :text="text" />
        </template>
      </template>
      <!-- isExist：库内是否已存在（0 新建 / 1 已存在）→ IsExistBadge -->
      <template v-else-if="column.dataIndex === 'isExist'">
        <CellOverflowTooltip :content="getIsExistLabel(text)">
          <IsExistBadge :isExist="text" />
        </CellOverflowTooltip>
      </template>
      <!-- entryState：词条审核状态（0 新建 / 1 审核中 / 2 不通过 / 3 已审核 / -1 禁用）→ EntryStateBadge -->
      <template v-else-if="column.dataIndex === 'entryState'">
        <CellOverflowTooltip :content="getEntryStateLabel(text)">
          <EntryStateBadge :entryState="text" />
        </CellOverflowTooltip>
      </template>
      <!-- 各语种翻译状态列：englishState / chineseState / … 及 translateState → TransStateBadge -->
      <template v-else-if="translateStateList.includes(column.dataIndex)">
        <CellOverflowTooltip :content="getTranslateStateLabel(text)">
          <TransStateBadge :translateState="text" />
        </CellOverflowTooltip>
      </template>
      <!-- operation：审核操作列（通过 / 驳回）→ AuditTags -->
      <template v-else-if="column.dataIndex === 'operation'">
        <AuditTags
          :audit-state="record.auditState"
          :disabled="!!record.parentID"
          @pass="$emit('auditPass', record)"
          @reject="$emit('auditReject', record)"
        />
      </template>
      <!-- editOperation：行内编辑操作列（✓ 保存 / ✕ 取消） -->
      <template v-else-if="column.dataIndex === 'editOperation'">
        <div class="editable-row-operations">
          <span v-if="isEditing(record)">
            <a-tooltip placement="top">
              <template #title>
                <span>保存</span>
              </template>
              <CheckOutlined
                style="color: #369fff; margin-left: 8px"
                @click="$emit('saveEdit', record)"
              />
            </a-tooltip>
            <a-tooltip placement="top">
              <template #title>
                <span>取消</span>
              </template>
              <CloseOutlined
                style="color: red; margin-left: 8px"
                @click="$emit('cancelEdit', record)"
              />
            </a-tooltip>
          </span>
        </div>
      </template>
      <!-- maxLength：最大字符长度（编辑态 a-input-number；0 表示无限制） -->
      <template v-else-if="column.dataIndex === 'maxLength'">
        <template v-if="edit.includes('maxLength') && isEditing(record)">
          <a-input-number
            v-model:value="editableData[record.id].maxLength"
            :min="0"
            :precision="0"
            style="width: 100%"
            placeholder="0 表示无限制"
            @change="(val) => $emit('cellInput', val, record, column)"
          />
        </template>
        <template v-else>
          <CellOverflowTooltip :content="formatMaxLengthText(text)" />
        </template>
      </template>
      <!-- 兜底：其余普通文本列（排除 index 序号列）→ CellOverflowTooltip -->
      <template v-else-if="column.dataIndex && column.dataIndex !== 'index'">
        <CellOverflowTooltip :content="formatCellText(text)" />
      </template>
    </template>
    <!-- 树形子行展开图标（children 非空时显示） -->
    <template v-if="showExpandIcon" #expandIcon="props">
      <span v-if="expandHasChildren(props.record)">
        <div
          v-if="props.expanded"
          :style="expandIconWrapStyle"
          @click="(e) => onExpandToggle(props, e)"
        >
          <CaretDownOutlined />
        </div>
        <div
          v-else
          :style="expandIconWrapStyle"
          @click="(e) => onExpandToggle(props, e)"
        >
          <CaretRightOutlined />
        </div>
      </span>
      <span v-else :style="{ marginRight: EXPAND_PLACEHOLDER_MR }"></span>
    </template>
    <!-- 列头自定义筛选下拉（entry / entrySource 等 customFilterDropdown 列） -->
    <template #customFilterDropdown="slotProps">
      <div :style="{ padding: FILTER_PAD }">
        <a-input
          :placeholder="`搜索 ${slotProps.column.title}`"
          :value="slotProps.selectedKeys[0]"
          :style="{
            width: FILTER_INPUT_W,
            marginBottom: FILTER_PAD,
            display: 'block',
          }"
          @change="(e) => onFilterKeysChange(e, slotProps.setSelectedKeys)"
          @pressEnter="() => onFilterSearch(slotProps)"
        />
        <a-button
          type="primary"
          size="small"
          :style="{ width: FILTER_BTN_W, marginRight: FILTER_BTN_GAP }"
          @click="() => onFilterSearch(slotProps)"
        >
          <template #icon>
            <SearchOutlined />
          </template>
          搜索
        </a-button>
        <a-button
          size="small"
          :style="{ width: FILTER_BTN_W }"
          @click="() => onFilterReset(slotProps.clearFilters)"
        >
          重置
        </a-button>
      </div>
    </template>
    <template #customFilterIcon="{ filtered }">
      <SearchOutlined
        :style="{ color: filtered ? FILTER_ACTIVE : undefined }"
      />
    </template>
    <template #empty>
      <a-empty :image="simpleEmptyImage" />
    </template>
  </TableViewport>
  </div>
</template>

<script>
import {
  SearchOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
  CheckOutlined,
  CloseOutlined,
  InfoCircleOutlined,
} from "@ant-design/icons-vue";
import Empty from "ant-design-vue/es/empty";
import commonParam from "@/constants/commonParam.js";
import {
  getEntryStateLabel,
  getIsExistLabel,
  getTranslateStateLabel,
} from "@/constants/statusMaps.js";
import ColumnActions from "./ColumnActions.vue";
import AuditTags from "./AuditTags.vue";
import InputIME from "@/components/cellEditor/input_IME.vue";
import TableTagCell from "@/components/table/TableTagCell/TableTagCell.vue";
import TableViewport from "@/components/table/TableViewport/TableViewport.vue";
import TableCellText from "@/components/table/TableCellText/TableCellText.vue";
import TableHeaderHint from "@/components/table/TableHeaderHint/TableHeaderHint.vue";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip/CellOverflowTooltip.vue";
import { getAdaptiveColumnKeys } from "@/components/table/columnLayout.js";
import IsExistBadge from "@/components/stateBadge/isExistBadge.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import { formatEntryText, formatCellText, formatMaxLengthText } from "@/components/table/cellText";
import { handleFilterSearch } from "@/views/workbench/composables/filterClear";
import {
  handleReset as tableHandleReset,
  handleTableChange,
  getRowClassName,
} from "@/utils/tableUtils";
import { invalidateSelectionWhenFiltersChange } from "@/utils/selectionUtils";

const FILTER_ACTIVE = "#108ee9";
const FILTER_PAD = "8px";
const FILTER_INPUT_W = "188px";
const FILTER_BTN_W = "90px";
const FILTER_BTN_GAP = "8px";
const EXPAND_ICON_MR = "10px";
const EXPAND_PLACEHOLDER_MR = "23px";
/** 匹配「翻译状态」列 dataIndex：各语种 *State + 通用 translateState */
const translateStateList = [
  ...commonParam.langTranslateStateList,
  "translateState",
];
const NON_TEXT_EDITABLE_COLUMNS = new Set([
  "diFileName",
  "tag",
  "maxLength",
  "isExist",
  "entryState",
  "operation",
  "editOperation",
  "index",
  ...translateStateList,
]);

export default {
  name: "PipelinePanel",
  inheritAttrs: false,
  components: {
    SearchOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    CheckOutlined,
    CloseOutlined,
    InfoCircleOutlined,
    ColumnActions,
    AuditTags,
    InputIME,
    TableTagCell,
    TableViewport,
    TableCellText,
    TableHeaderHint,
    CellOverflowTooltip,
    IsExistBadge,
    EntryStateBadge,
    TransStateBadge,
  },
  props: {
    task: {
      type: Object,
      default: () => ({}),
    },
    tableHost: {
      type: Object,
      default: null,
    },
    columns: {
      type: Array,
      default: () => [],
    },
    dataSource: {
      type: Array,
      default: () => [],
    },
    loading: {
      type: Boolean,
      default: false,
    },
    pagination: {
      type: Object,
      default: () => ({}),
    },
    scroll: {
      type: Object,
      default: () => ({}),
    },
    rowSelection: {
      type: Object,
      default: null,
    },
    customRow: {
      type: Function,
      default: null,
    },
    columnActions: {
      type: Object,
      default: null,
    },
    edit: {
      type: Array,
      default: () => [],
    },
    editableData: {
      type: Object,
      default: () => ({}),
    },
    cellErrors: {
      type: Object,
      default: () => ({}),
    },
    showExpandIcon: {
      type: Boolean,
      default: true,
    },
    expandIconColumnIndex: {
      type: Number,
      default: 2,
    },
    childrenColumnName: {
      type: String,
      default: undefined,
    },
  },
  emits: [
    "columnsChange",
    "update:modelValue",
    "filterSearch",
    "filterReset",
    "change",
    "cellInput",
    "saveEdit",
    "cancelEdit",
    "auditPass",
    "auditReject",
  ],
  /**
   * 初始化流水线表格壳的展示常量。
   * @returns {Object} 模板使用的展示常量。
   */
  data() {
    return {
      FILTER_ACTIVE,
      FILTER_PAD,
      FILTER_INPUT_W,
      FILTER_BTN_W,
      FILTER_BTN_GAP,
      EXPAND_PLACEHOLDER_MR,
      layoutRevision: 0,
      simpleEmptyImage: Empty.PRESENTED_IMAGE_SIMPLE,
      expandIconWrapStyle: {
        display: "inline-block",
        marginRight: EXPAND_ICON_MR,
      },
      translateStateList,
    };
  },
  computed: {
    /** edit prop 中可 TextArea 编辑的列（排除 diFileName / tag / maxLength 专用分支） */
    editableTextColumns() {
      return this.edit.filter(
        (col) => !NON_TEXT_EDITABLE_COLUMNS.has(col)
      );
    },
    /**
     * 识别流水线表格中的长文本列，并交给共享视口分配剩余宽度。
     * @returns {Array<string>} 自适应列标识。
     */
    adaptiveColumnKeys() {
      return getAdaptiveColumnKeys(
        (this.columns || []).filter((column) => !column.fixed)
      );
    },
    /**
     * 为共享视口生成随任务和列集重建而变化的布局标识。
     * @returns {string} 当前表格布局标识。
     */
    tableLayoutKey() {
      return `${this.task?.id ?? ""}:${this.layoutRevision}`;
    },
    showInlineColumnActions() {
      if (this.$slots.trailing) {
        return false;
      }
      return this.columnActions != null || !!this.$slots.columnActions;
    },
  },
  watch: {
    /**
     * 仅在调用方替换列数组时标记新布局，避免列拖拽修改宽度后被立即重置。
     * @param {Array} newColumns - 当前列数组。
     * @param {Array} oldColumns - 上一个列数组。
     * @returns {void} 无返回值。
     */
    columns(newColumns, oldColumns) {
      if (newColumns !== oldColumns) this.layoutRevision += 1;
    },
    /**
     * 切换任务时标记新布局，确保隐藏后复用的弹窗不会保留横向偏移。
     * @param {Object} newTask - 当前任务。
     * @param {Object} oldTask - 上一个任务。
     * @returns {void} 无返回值。
     */
    task(newTask, oldTask) {
      if (newTask?.id !== oldTask?.id) this.layoutRevision += 1;
    },
  },
  methods: {
    formatEntryText,
    formatCellText,
    formatMaxLengthText,
    getIsExistLabel,
    getEntryStateLabel,
    getTranslateStateLabel,
    /**
     * 判断当前列是否为可由文本编辑器处理的字段。
     * @param {Object} column 当前表格列配置。
     * @returns {boolean} 是否应在编辑态显示文本编辑器。
     */
    isEditableTextColumn(column) {
      return (
        this.editableTextColumns.includes(column?.dataIndex) &&
        !NON_TEXT_EDITABLE_COLUMNS.has(column?.dataIndex)
      );
    },
    /**
     * 判断当前列是否为不依赖业务徽标或操作控件的普通文本列。
     * @param {Object} column 当前表格列配置。
     * @returns {boolean} 是否使用统一文本省略和 Tooltip 渲染。
     */
    isPlainTextColumn(column) {
      const dataIndex = column?.dataIndex;
      return Boolean(dataIndex) && !NON_TEXT_EDITABLE_COLUMNS.has(dataIndex);
    },
    isEditing(record) {
      return !!this.editableData?.[record?.id];
    },
    expandHasChildren(record) {
      return record.children != null && record.children.length > 0;
    },
    onExpandToggle(props, e) {
      props.onExpand(props.record, e);
    },
    onFilterKeysChange(e, setSelectedKeys) {
      setSelectedKeys(e.target.value ? [e.target.value] : []);
    },
    onFilterSearch(slotProps) {
      if (this.tableHost) {
        handleFilterSearch(
          slotProps.selectedKeys,
          slotProps.confirm,
          slotProps.column.dataIndex,
          slotProps.clearFilters,
          this.tableHost
        );
      }
      this.$emit(
        "filterSearch",
        slotProps.selectedKeys,
        slotProps.confirm,
        slotProps.column.dataIndex,
        slotProps.clearFilters
      );
    },
    onFilterReset(clearFilters) {
      if (this.tableHost) {
        tableHandleReset(clearFilters, this.tableHost);
      }
      this.$emit("filterReset", clearFilters);
    },
    onRowClassName(record, index) {
      return getRowClassName(record, index, this.tableHost?.selectedRowIndex);
    },
    /**
     * 将表格分页与筛选变化同步到宿主列表，并清空过期筛选选择。
     * @param {Object} pagination - 表格分页状态。
     * @param {Object} filters - 表格列筛选值。
     * @param {Object} sorter - 表格排序状态。
     * @param {Object} extra - 表格变更动作信息。
     * @returns {void} 无返回值。
     */
    onTableChange(pagination, filters, sorter, extra) {
      if (this.tableHost) {
        invalidateSelectionWhenFiltersChange(this.tableHost, filters);
        handleTableChange(pagination, filters, this.tableHost);
      }
      this.$emit("change", pagination, filters, sorter, extra);
    },
  },
};
</script>

<style scoped>
.pipeline-panel {
  box-sizing: border-box;
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  align-self: stretch;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  height: 100%;
}
.pipeline-panel > .table-viewport {
  flex: 1 1 0;
  min-height: 0;
}
.workbench-task-info {
  display: flex;
  padding: 4px 0;
  align-items: center;
  gap: 32px;
  align-self: stretch;
  flex: 0 0 auto;
}
.workbench-task-info__item {
  display: flex;
  align-items: center;
  flex: 1 0 0;
}
.workbench-form-bar {
  display: flex;
  align-items: center;
  align-self: stretch;
  width: 100%;
  flex: 0 0 auto;
}
.workbench-form-bar__main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  row-gap: 8px;
  flex: 1;
  min-width: 0;
}
.workbench-form-bar__trailing {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.workbench-action-group {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.workbench-action-group--offset {
  margin-left: 8px;
}
</style>
