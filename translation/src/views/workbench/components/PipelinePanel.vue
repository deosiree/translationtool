<template>
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
  <a-table
    bordered
    class="ant-table-striped table-cell-overflow"
    :columns="columns"
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
    @resize-column="handleResize"
    @change="onTableChange"
  >
    <template #headerCell="{ title, column }">
      <!-- 列头：colValue 有值时省略号 + Tooltip -->
      <CellOverflowTooltip v-if="column.colValue" :content="title">
        {{ title }}
      </CellOverflowTooltip>
    </template>
    <template #bodyCell="{ column, text, record }">
      <!-- entry：词条原文 -->
      <template v-if="column.dataIndex === 'entry'">
        <CellOverflowTooltip :content="formatEntryText(text)">
          {{ formatEntryText(text) }}
        </CellOverflowTooltip>
      </template>
      <!-- 可编辑文本列：翻译列 / comment / 释义等（edit prop 中除 diFileName/tag/maxLength 外的列） -->
      <template v-else-if="editableTextColumns.includes(column.dataIndex)">
        <template v-if="isEditing(record)">
          <TableCellTextArea
            :value="editableData[record.id][column.dataIndex] ?? ''"
            :error-message="cellErrors[record.id]?.[column.dataIndex]"
            @update:value="(val) => $emit('cellInput', val, record, column)"
          />
        </template>
        <template v-else>
          <CellOverflowTooltip :content="formatCellText(text)" />
        </template>
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
          <CellOverflowTooltip :content="formatTagText(text)">
            <span>
              <a-tag
                v-for="(tag, index) in companyCut(text)"
                :key="index"
                color="cyan"
                class="tag-content"
              >
                {{ tag }}
              </a-tag>
            </span>
          </CellOverflowTooltip>
        </template>
      </template>
      <!-- isExist：库内是否已存在（0 新建 / 1 已存在）→ IsExistBadge -->
      <template v-else-if="column.dataIndex === 'isExist'">
        <CellOverflowTooltip :content="isExistLabel(text)">
          <IsExistBadge :isExist="text" />
        </CellOverflowTooltip>
      </template>
      <!-- entryState：词条审核状态（0 新建 / 1 审核中 / 2 不通过 / 3 已审核 / -1 禁用）→ EntryStateBadge -->
      <template v-else-if="column.dataIndex === 'entryState'">
        <CellOverflowTooltip :content="entryStateLabel(text)">
          <EntryStateBadge :entryState="text" />
        </CellOverflowTooltip>
      </template>
      <!-- 各语种翻译状态列：englishState / chineseState / … 及 translateState → TransStateBadge -->
      <template v-else-if="translateStateList.includes(column.dataIndex)">
        <CellOverflowTooltip :content="translateStateLabel(text)">
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
  </a-table>
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
import commonParam from "@/constants/commonParam.js";
import ColumnActions from "./ColumnActions.vue";
import AuditTags from "./AuditTags.vue";
import InputIME from "@/components/cellEditor/input_IME.vue";
import TableCellTextArea from "@/components/table/TableCellTextArea.vue";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip.vue";
import IsExistBadge from "@/components/stateBadge/isExistBadge.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import { formatEntryText, formatCellText, formatMaxLengthText } from "@/components/table/cellText";
import { companyCut, formatTagText } from "@/views/workbench/utils/tagFmt";
import { handleFilterSearch } from "@/views/workbench/composables/filterClear";
import {
  handleReset as tableHandleReset,
  handleTableChange,
  handleResizeColumn,
  getRowClassName,
} from "@/utils/tableUtils";

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
    TableCellTextArea,
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
  data() {
    return {
      FILTER_ACTIVE,
      FILTER_PAD,
      FILTER_INPUT_W,
      FILTER_BTN_W,
      FILTER_BTN_GAP,
      EXPAND_PLACEHOLDER_MR,
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
        (col) => !["diFileName", "tag", "maxLength"].includes(col)
      );
    },
    showInlineColumnActions() {
      if (this.$slots.trailing) {
        return false;
      }
      return this.columnActions != null || !!this.$slots.columnActions;
    },
  },
  methods: {
    formatEntryText,
    formatCellText,
    formatMaxLengthText,
    formatTagText,
    companyCut,
    isExistLabel(value) {
      if (value === 0) return "新建";
      if (value === 1) return "已存在";
      return "";
    },
    entryStateLabel(value) {
      const map = {
        0: "新建",
        1: "审核中",
        2: "审核不通过",
        3: "已审核",
        "-1": "禁用",
      };
      return map[value] ?? "";
    },
    translateStateLabel(value) {
      const map = {
        0: "未翻译",
        1: "待审核",
        2: "审核不通过",
        3: "已审核",
      };
      return map[value] ?? "未翻译";
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
    handleResize(w, col) {
      handleResizeColumn(w, col);
    },
    onRowClassName(record, index) {
      return getRowClassName(record, index, this.tableHost?.selectedRowIndex);
    },
    onTableChange(pagination, filters, sorter, extra) {
      if (this.tableHost) {
        handleTableChange(pagination, filters, this.tableHost);
      }
      this.$emit("change", pagination, filters, sorter, extra);
    },
  },
};
</script>

<style scoped>
.workbench-task-info {
  display: flex;
  padding: 4px 0;
  align-items: center;
  gap: 32px;
  align-self: stretch;
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
}
.workbench-form-bar__main {
  display: flex;
  align-items: center;
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
