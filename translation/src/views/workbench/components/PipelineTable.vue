<template>
  <!-- 流水线表格壳：headerCell / 列头放大镜 / 展开箭头内置；bodyCell 交回各阶段 -->
  <a-table
    bordered
    class="ant-table-striped table-cell-overflow"
    v-bind="$attrs"
  >
    <!-- 表头：动态列名省略 tooltip -->
    <template #headerCell="{ title, column }">
      <CellOverflowTooltip v-if="column.colValue" :content="title">
        {{ title }}
      </CellOverflowTooltip>
    </template>
    <!-- 本阶段单元格渲染 -->
    <template #bodyCell="slotProps">
      <slot name="bodyCell" v-bind="slotProps" />
    </template>
    <!-- 表头：展开/收起子行（record.children） -->
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
    <!-- 表头放大镜：词条/来源搜索框（needFilter=true 时 entry/entrySource 列才有） -->
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
    <!-- 表头放大镜图标 -->
    <template #customFilterIcon="{ filtered }">
      <SearchOutlined
        :style="{ color: filtered ? FILTER_ACTIVE : undefined }"
      />
    </template>
  </a-table>
</template>

<script>
/**
 * 工作台流水线表格壳：headerCell + 列头放大镜 + 展开箭头内联，bodyCell 交回各阶段。
 */
import {
  SearchOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
} from "@ant-design/icons-vue";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip.vue";

const FILTER_ACTIVE = "#108ee9";
const FILTER_PAD = "8px";
const FILTER_INPUT_W = "188px";
const FILTER_BTN_W = "90px";
const FILTER_BTN_GAP = "8px";
const EXPAND_ICON_MR = "10px";
const EXPAND_PLACEHOLDER_MR = "23px";

export default {
  name: "PipelineTable",
  inheritAttrs: false,
  components: {
    SearchOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    CellOverflowTooltip,
  },
  props: {
    /** 是否渲染自定义展开箭头（translate 等无 children 列时传 false） */
    showExpandIcon: {
      type: Boolean,
      default: true,
    },
  },
  emits: ["filterSearch", "filterReset"],
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
    };
  },
  methods: {
    /**
     * 是否有可展开子行
     * @param {Object} record 行数据
     * @returns {boolean}
     */
    expandHasChildren(record) {
      return record.children != null && record.children.length > 0;
    },
    /**
     * 转发 Ant onExpand
     * @param {Object} props expandIcon slot 参数
     * @returns {void}
     */
    onExpandToggle(props, e) {
      props.onExpand(props.record, e);
    },
    /**
     * 同步 Ant 注入的 selectedKeys
     * @param {Event} e 输入事件
     * @param {Function} setSelectedKeys Ant 回调
     * @returns {void}
     */
    onFilterKeysChange(e, setSelectedKeys) {
      setSelectedKeys(e.target.value ? [e.target.value] : []);
    },
    /**
     * 确认列头搜索，四参交回页面 handleSearch
     * @param {Object} slotProps customFilterDropdown slot 参数
     * @returns {void}
     */
    onFilterSearch(slotProps) {
      this.$emit(
        "filterSearch",
        slotProps.selectedKeys,
        slotProps.confirm,
        slotProps.column.dataIndex,
        slotProps.clearFilters
      );
    },
    /**
     * 重置列头筛选
     * @param {Function} clearFilters Ant 回调
     * @returns {void}
     */
    onFilterReset(clearFilters) {
      this.$emit("filterReset", clearFilters);
    },
  },
};
</script>
