<!-- @fileoverview Ant Design Vue 表格视口，提供不占布局空间的覆盖滚动条。 -->
<template>
  <div
    class="table-viewport"
    :class="{
      'table-viewport-dragging': dragging,
      'table-viewport-scrolling': scrolling,
      'table-viewport-empty': showEmptyOverlay,
      'table-viewport-fill-height': fillHeight,
    }"
    :style="viewportStyle"
  >
    <a-table
      ref="table"
      v-bind="$attrs"
      :class="{ 'table-native-empty-hidden': showEmptyOverlay }"
      :columns="resolvedColumns"
      :scroll="tableScroll"
      @resize-column="handleResizeColumn"
    >
      <template v-for="(_, slotName) in tableSlots" #[slotName]="slotProps">
        <slot :name="slotName" v-bind="slotProps || {}" />
      </template>
    </a-table>

    <div v-if="showEmptyOverlay" class="table-empty-overlay">
      <slot name="empty" />
    </div>

    <div
      v-if="hasVerticalScrollbar"
      class="table-overlay-scrollbar table-overlay-scrollbar-vertical"
      :style="verticalTrackStyle"
      role="scrollbar"
      aria-orientation="vertical"
      :aria-valuenow="Math.round(metrics.scrollTop)"
      :aria-valuemax="Math.round(metrics.scrollHeight - metrics.clientHeight)"
    >
      <span
        class="table-overlay-scrollbar-thumb"
        :style="verticalThumbStyle"
        @mousedown="startDrag('vertical', $event)"
      />
    </div>
    <div
      v-if="hasHorizontalScrollbar"
      class="table-overlay-scrollbar table-overlay-scrollbar-horizontal"
      :style="horizontalTrackStyle"
      role="scrollbar"
      aria-orientation="horizontal"
      :aria-valuenow="Math.round(metrics.scrollLeft)"
      :aria-valuemax="Math.round(metrics.scrollWidth - metrics.clientWidth)"
    >
      <span
        class="table-overlay-scrollbar-thumb"
        :style="horizontalThumbStyle"
        @mousedown="startDrag('horizontal', $event)"
      />
    </div>
  </div>
</template>

<script>
import { useAdaptiveColumnWidths } from "./useAdaptiveColumnWidths";
import { getAdaptiveColumnKeys, getColumnLayout } from "./columnLayout";

const TABLE_HEADER_HEIGHT = 40;
const TABLE_PAGINATION_HEIGHT = 48;
const DEFAULT_ADAPTIVE_COLUMN_WIDTH = 220;

/**
 * 将 Ant Table 的滚动参数转换为仅包含横向滚动的参数。
 * @param {Object|undefined} scroll - 调用方传入的滚动配置。
 * @returns {Object|undefined} 传递给 Ant Table 的滚动配置。
 */
function getTableScroll(scroll) {
  if (!scroll || scroll.x == null) return undefined;
  return { x: scroll.x };
}

/**
 * 将旧表格的内容区高度转换为包含表头和分页的视口高度。
 * @param {number|string|null} height 显式视口高度。
 * @param {Object} scroll 原 Ant Table 滚动配置。
 * @param {boolean} hasPagination 是否显示分页。
 * @returns {number|string} 表格视口高度。
 */
function resolveViewportHeight(height, scroll, hasPagination) {
  if (height != null && height !== "") return height;
  const bodyHeight = scroll?.y;
  if (bodyHeight == null || bodyHeight === "") return "100%";
  if (bodyHeight === "max-content") return "100%";

  const chromeHeight = TABLE_HEADER_HEIGHT + (hasPagination ? TABLE_PAGINATION_HEIGHT : 0);
  const numericHeight = typeof bodyHeight === "number"
    ? bodyHeight
    : /^\d+(?:\.\d+)?(?:px)?$/.test(String(bodyHeight))
      ? Number.parseFloat(bodyHeight)
      : null;
  if (numericHeight != null) return `${numericHeight + chromeHeight}px`;
  return `calc(${bodyHeight} + ${chromeHeight}px)`;
}

/**
 * 将表格内容区高度转换为 CSS 尺寸，供空态占位行保留可视区域。
 * @param {number|string|undefined} height 表格纵向滚动内容区高度。
 * @returns {string} 可写入 CSS 变量的内容区高度。
 */
function resolveEmptyBodyHeight(height) {
  if (typeof height === "number") return `${height}px`;
  if (/^\d+(?:\.\d+)?$/.test(String(height))) return `${height}px`;
  return height || "0px";
}

/**
 * 获取列宽状态使用的稳定列标识。
 * @param {Object} column Ant Table 列定义。
 * @returns {string|undefined} 列标识。
 */
function getColumnKey(column) {
  const key = column?.colValue ?? column?.dataIndex ?? column?.key;
  return key == null ? undefined : String(key);
}

/**
 * 将分组列展开为叶子列，供剩余宽度计算使用。
 * @param {Array} columns Ant Table 列定义。
 * @returns {Array} 叶子列集合。
 */
function flattenColumns(columns) {
  return (columns || []).flatMap((column) =>
    column.children?.length ? flattenColumns(column.children) : [column]
  );
}

/**
 * 覆盖自适应列的固定尺寸回调，避免原列配置锁死拖拽最小宽度。
 * @param {Function|undefined} original 原列尺寸回调。
 * @returns {Function} 仅保留最小宽度的尺寸回调。
 */
function createFluidCellSize(original) {
  return (...args) => {
    const cell = original?.(...args) || {};
    const style = { ...(cell.style || {}) };
    delete style.width;
    delete style.minWidth;
    delete style.maxWidth;
    return { ...cell, style: { ...style, minWidth: "50px", maxWidth: "none" } };
  };
}

export default {
  name: "TableViewport",
  inheritAttrs: false,
  props: {
    height: {
      type: [Number, String],
      default: null,
    },
    scroll: {
      type: Object,
      default: () => ({}),
    },
    columns: {
      type: Array,
      default: () => [],
    },
    adaptiveColumnKeys: {
      type: Array,
      default: () => [],
    },
    adaptiveColumns: {
      type: Boolean,
      default: false,
    },
    adaptiveBaseWidth: {
      type: Number,
      default: DEFAULT_ADAPTIVE_COLUMN_WIDTH,
    },
    reservedWidth: {
      type: Number,
      default: 0,
    },
    emptyOverlay: {
      type: Boolean,
      default: false,
    },
    fillHeight: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["resizeColumn"],
  /**
   * 初始化滚动状态和视口测量值。
   * @param {void} none - 无参数。
   * @returns {Object} 组件初始状态。
   */
  data() {
    return {
      scrollTarget: null,
      resizeObserver: null,
      dragging: false,
      scrolling: false,
      dragState: null,
      scrollbarTimer: null,
      adaptiveWidthRevision: 0,
      baseColumnWidths: {},
      adaptiveWidthState: useAdaptiveColumnWidths(),
      metrics: {
        scrollTop: 0,
        scrollLeft: 0,
        scrollHeight: 0,
        scrollWidth: 0,
        clientHeight: 0,
        clientWidth: 0,
        offsetTop: 0,
        offsetLeft: 0,
        viewportHeight: 0,
        viewportWidth: 0,
      },
    };
  },
  computed: {
    /**
     * 返回传给 Ant Table 的业务插槽，空态插槽由共享视口自行消费。
     * @returns {Object} 不含 empty 的具名插槽集合。
     */
    tableSlots() {
      return Object.fromEntries(
        Object.entries(this.$slots).filter(([name]) => name !== "empty")
      );
    },
    /**
     * 判断当前表格是否渲染分页区域。
     * @returns {boolean} 是否需要为分页预留视口高度。
     */
    hasPagination() {
      return this.$attrs.pagination !== false;
    },
    /**
     * 判断调用方是否明确启用了带空态插槽的空数据视图。
     * @returns {boolean} 是否显示共享空态覆盖层。
     */
    showEmptyOverlay() {
      const dataSource = this.$attrs.dataSource ?? this.$attrs["data-source"];
      return this.emptyOverlay && Array.isArray(dataSource) && dataSource.length === 0 && !!this.$slots.empty;
    },
    /**
     * 返回不包含纵向滚动配置的 Ant Table 参数。
     * @param {void} none - 无参数。
     * @returns {Object|undefined} 表格横向滚动配置。
     */
    tableScroll() {
      return getTableScroll(this.scroll);
    },
    /**
     * 根据可用表格宽度解析长文本列宽，并保持其他列定义不变。
     * @returns {Array} 传递给 Ant Table 的列定义。
     */
    resolvedColumns() {
      this.adaptiveWidthRevision;
      const leaves = flattenColumns(this.columns);
      const requestedKeys = new Set(this.adaptiveColumnKeys.map(String));
      if (requestedKeys.size === 0 && this.adaptiveColumns) {
        getAdaptiveColumnKeys(leaves).forEach((key) => requestedKeys.add(key));
        if (requestedKeys.size === 0) {
          leaves.forEach((column) => {
            const key = getColumnKey(column);
            if (key && getColumnLayout(key).adaptive) {
              requestedKeys.add(key);
            }
          });
        }
      }
      if (requestedKeys.size === 0) return this.columns;

      const adaptiveKeys = new Set();
      const widthColumns = leaves.map((column) => {
        const key = getColumnKey(column);
        if (!key) return null;
        const matches = [column.colValue, column.dataIndex, column.key]
          .some((value) => value != null && requestedKeys.has(String(value)));
        const baseWidth = this.getBaseColumnWidth(column, key);
        if (matches) adaptiveKeys.add(key);
        return {
          colValue: key,
          width: baseWidth,
          minWidth: matches ? 50 : column.minWidth,
        };
      }).filter(Boolean);

      const rowSelection = this.$attrs.rowSelection || this.$attrs["row-selection"];
      const rowSelectionWidth = rowSelection
        ? Number.parseFloat(rowSelection.columnWidth) || 48
        : 0;
      const widths = this.adaptiveWidthState.resolve(
        this.metrics.clientWidth || this.$el?.clientWidth || 0,
        widthColumns,
        Array.from(adaptiveKeys),
        this.reservedWidth + rowSelectionWidth
      );
      return this.applyResolvedWidths(this.columns, widths, adaptiveKeys);
    },
    /**
     * 返回视口高度 CSS 变量。
     * @param {void} none - 无参数。
     * @returns {Object} 视口样式。
     */
    viewportStyle() {
      const height = this.fillHeight
        ? "auto"
        : resolveViewportHeight(this.height, this.scroll, this.hasPagination);
      return {
        "--table-viewport-height": typeof height === "number" ? `${height}px` : height,
        "--table-empty-body-height": resolveEmptyBodyHeight(this.scroll?.y),
        "--table-empty-overlay-bottom": this.hasPagination ? "48px" : "0px",
      };
    },
    /**
     * 判断当前内容是否需要纵向覆盖滚动条。
     * @param {void} none - 无参数。
     * @returns {boolean} 是否存在纵向滚动范围。
     */
    hasVerticalScrollbar() {
      return this.metrics.scrollHeight > this.metrics.clientHeight;
    },
    /**
     * 判断当前内容是否需要横向覆盖滚动条。
     * @param {void} none - 无参数。
     * @returns {boolean} 是否存在横向滚动范围。
     */
    hasHorizontalScrollbar() {
      return this.metrics.scrollWidth > this.metrics.clientWidth;
    },
    /**
     * 返回纵向滑块位置和尺寸。
     * @param {void} none - 无参数。
     * @returns {Object} 纵向滑块样式。
     */
    verticalThumbStyle() {
      const { clientHeight, scrollHeight, scrollTop } = this.metrics;
      const trackHeight = Math.max(clientHeight - 8, 0);
      const thumbHeight = Math.max(
        Math.min(trackHeight, trackHeight * clientHeight / Math.max(scrollHeight, 1)),
        24
      );
      const maxTop = Math.max(trackHeight - thumbHeight, 0);
      const top = scrollHeight > clientHeight
        ? maxTop * scrollTop / (scrollHeight - clientHeight)
        : 0;
      return {
        height: `${thumbHeight}px`,
        transform: `translateY(${top}px)`,
      };
    },
    /**
     * 返回纵向覆盖滚动条在内容视口中的位置。
     * @param {void} none - 无参数。
     * @returns {Object} 纵向轨道定位样式。
     */
    verticalTrackStyle() {
      const { offsetTop, offsetLeft, clientWidth, viewportHeight, viewportWidth } = this.metrics;
      return {
        top: `${offsetTop + 4}px`,
        right: `${Math.max(viewportWidth - offsetLeft - clientWidth, 0) + 2}px`,
        bottom: `${Math.max(viewportHeight - offsetTop - this.metrics.clientHeight, 0) + 4}px`,
      };
    },
    /**
     * 返回横向滑块位置和尺寸。
     * @param {void} none - 无参数。
     * @returns {Object} 横向滑块样式。
     */
    horizontalThumbStyle() {
      const { clientWidth, scrollWidth, scrollLeft } = this.metrics;
      const trackWidth = Math.max(clientWidth - 8, 0);
      const thumbWidth = Math.max(
        Math.min(trackWidth, trackWidth * clientWidth / Math.max(scrollWidth, 1)),
        24
      );
      const maxLeft = Math.max(trackWidth - thumbWidth, 0);
      const left = scrollWidth > clientWidth
        ? maxLeft * scrollLeft / (scrollWidth - clientWidth)
        : 0;
      return {
        width: `${thumbWidth}px`,
        transform: `translateX(${left}px)`,
      };
    },
    /**
     * 返回横向覆盖滚动条在内容视口中的位置。
     * @param {void} none - 无参数。
     * @returns {Object} 横向轨道定位样式。
     */
    horizontalTrackStyle() {
      const { offsetTop, offsetLeft, clientHeight, clientWidth, viewportHeight, viewportWidth } = this.metrics;
      return {
        left: `${offsetLeft + 4}px`,
        right: `${Math.max(viewportWidth - offsetLeft - clientWidth, 0) + 4}px`,
        bottom: `${Math.max(viewportHeight - offsetTop - clientHeight, 0) + 2}px`,
      };
    },
  },
  /**
   * 在组件挂载后绑定 Ant Table 内容视口。
   * @param {void} none - 无参数。
   * @returns {void} 无返回值。
   */
  mounted() {
    this.$nextTick(() => this.bindScrollTarget());
  },
  /**
   * 在表格重渲染后确认滚动目标仍有效。
   * @param {void} none - 无参数。
   * @returns {void} 无返回值。
   */
  updated() {
    this.$nextTick(() => this.bindScrollTarget());
  },
  /**
   * 清理滚动监听、尺寸观察器和定时器。
   * @param {void} none - 无参数。
   * @returns {void} 无返回值。
   */
  beforeUnmount() {
    this.unbindScrollTarget();
    this.stopDrag();
    clearTimeout(this.scrollbarTimer);
  },
  methods: {
    /**
     * 保留每列初始宽度作为后续自适应分配的基准。
     * @param {Object} column Ant Table 列定义。
     * @param {string} key 稳定列标识。
     * @returns {number} 列基准宽度。
     */
    getBaseColumnWidth(column, key) {
      if (this.baseColumnWidths[key] == null) {
        const semanticLayout = getColumnLayout(key);
        const width =
          this.adaptiveColumns &&
          column.adaptive !== true &&
          column.adaptive !== false &&
          semanticLayout.adaptive
            ? semanticLayout.width
            : Number.parseFloat(column.width ?? semanticLayout.width);
        this.baseColumnWidths[key] = Number.isFinite(width) && width > 0
          ? width
          : this.adaptiveBaseWidth;
      }
      return this.baseColumnWidths[key];
    },
    /**
     * 将已解析宽度应用到列副本，允许自适应列收窄至 50px。
     * @param {Array} columns 原 Ant Table 列定义。
     * @param {Object<string, number>} widths 按列标识索引的解析宽度。
     * @param {Set<string>} adaptiveKeys 自适应列标识。
     * @returns {Array} 更新宽度后的列定义。
     */
    applyResolvedWidths(columns, widths, adaptiveKeys) {
      return (columns || []).map((column) => {
        if (column.children?.length) {
          return {
            ...column,
            children: this.applyResolvedWidths(column.children, widths, adaptiveKeys),
          };
        }
        const key = getColumnKey(column);
        if (!key || widths[key] == null) return column;
        const adaptive = adaptiveKeys.has(key);
        return {
          ...column,
          width: widths[key],
          ...(adaptive
            ? {
                minWidth: 50,
                customCell: createFluidCellSize(column.customCell),
                customHeaderCell: createFluidCellSize(column.customHeaderCell),
              }
            : {}),
        };
      });
    },
    /**
     * 记录拖拽后的列宽并将尺寸事件交回调用页面。
     * @param {number} width 拖拽后的列宽。
     * @param {Object} column Ant Table 返回的列定义。
     * @returns {void} 无返回值。
     */
    handleResizeColumn(width, column) {
      const key = getColumnKey(column);
      const sourceColumn = flattenColumns(this.columns).find(
        (item) => getColumnKey(item) === key
      );
      if (key) {
        this.adaptiveWidthState.setManualWidth(key, width);
        this.adaptiveWidthRevision += 1;
      }
      this.$emit("resizeColumn", width, sourceColumn || column);
    },
    /**
     * 查找 Ant Table 内容滚动容器并安装监听器。
     * @param {void} none - 无参数。
     * @returns {void} 无返回值。
     */
    bindScrollTarget() {
      const tableElement = this.$refs.table?.$el || this.$refs.table;
      const target = tableElement?.querySelector?.(".ant-table-content");
      if (!target || target === this.scrollTarget) return;
      this.unbindScrollTarget();
      this.scrollTarget = target;
      this.scrollTarget.addEventListener("scroll", this.handleScroll, { passive: true });
      if (typeof ResizeObserver !== "undefined") {
        this.resizeObserver = new ResizeObserver(this.syncMetrics);
        this.resizeObserver.observe(this.scrollTarget);
        this.resizeObserver.observe(this.$el);
        const table = this.scrollTarget.querySelector("table");
        if (table) this.resizeObserver.observe(table);
      }
      this.syncMetrics();
    },
    /**
     * 移除滚动和尺寸监听器。
     * @param {void} none - 无参数。
     * @returns {void} 无返回值。
     */
    unbindScrollTarget() {
      if (this.scrollTarget) {
        this.scrollTarget.removeEventListener("scroll", this.handleScroll);
      }
      this.resizeObserver?.disconnect();
      this.resizeObserver = null;
      this.scrollTarget = null;
    },
    /**
     * 从真实滚动容器同步覆盖滚动条尺寸和位置。
     * @param {void} none - 无参数。
     * @returns {void} 无返回值。
     */
    syncMetrics() {
      const target = this.scrollTarget;
      if (!target) return;
      const targetRect = target.getBoundingClientRect();
      const viewportRect = this.$el.getBoundingClientRect();
      this.metrics = {
        scrollTop: target.scrollTop,
        scrollLeft: target.scrollLeft,
        scrollHeight: target.scrollHeight,
        scrollWidth: target.scrollWidth,
        clientHeight: target.clientHeight,
        clientWidth: target.clientWidth,
        offsetTop: targetRect.top - viewportRect.top,
        offsetLeft: targetRect.left - viewportRect.left,
        viewportHeight: this.$el.clientHeight,
        viewportWidth: this.$el.clientWidth,
      };
    },
    /**
     * 同步滚动位置并短暂显示覆盖滚动条。
     * @param {void} none - 无参数。
     * @returns {void} 无返回值。
     */
    handleScroll() {
      this.syncMetrics();
      this.scrolling = true;
      clearTimeout(this.scrollbarTimer);
      this.scrollbarTimer = setTimeout(this.hideScrollbar, 650);
    },
    /**
     * 隐藏停止滚动后的覆盖滚动条。
     * @param {void} none - 无参数。
     * @returns {void} 无返回值。
     */
    hideScrollbar() {
      this.scrolling = false;
    },
    /**
     * 开始拖动指定方向的覆盖滚动滑块。
     * @param {"vertical"|"horizontal"} axis - 拖动方向。
     * @param {MouseEvent} event - 鼠标按下事件。
     * @returns {void} 无返回值。
     */
    startDrag(axis, event) {
      if (!this.scrollTarget) return;
      event.preventDefault();
      this.dragging = true;
      this.dragState = {
        axis,
        pointer: axis === "vertical" ? event.clientY : event.clientX,
        scroll: axis === "vertical" ? this.scrollTarget.scrollTop : this.scrollTarget.scrollLeft,
      };
      document.addEventListener("mousemove", this.handleDrag);
      document.addEventListener("mouseup", this.stopDrag);
    },
    /**
     * 根据鼠标移动距离更新真实滚动位置。
     * @param {MouseEvent} event - 鼠标移动事件。
     * @returns {void} 无返回值。
     */
    handleDrag(event) {
      if (!this.dragState || !this.scrollTarget) return;
      const { axis, pointer, scroll } = this.dragState;
      const delta = (axis === "vertical" ? event.clientY : event.clientX) - pointer;
      const viewport = axis === "vertical"
        ? this.metrics.clientHeight
        : this.metrics.clientWidth;
      const content = axis === "vertical"
        ? this.metrics.scrollHeight
        : this.metrics.scrollWidth;
      const thumb = axis === "vertical"
        ? parseFloat(this.verticalThumbStyle.height)
        : parseFloat(this.horizontalThumbStyle.width);
      const track = Math.max(viewport - 8 - thumb, 1);
      const nextScroll = scroll + delta * Math.max(content - viewport, 0) / track;
      if (axis === "vertical") this.scrollTarget.scrollTop = nextScroll;
      else this.scrollTarget.scrollLeft = nextScroll;
      this.syncMetrics();
    },
    /**
     * 结束覆盖滚动条拖动并清理文档监听器。
     * @param {void} none - 无参数。
     * @returns {void} 无返回值。
     */
    stopDrag() {
      document.removeEventListener("mousemove", this.handleDrag);
      document.removeEventListener("mouseup", this.stopDrag);
      this.dragging = false;
      this.dragState = null;
    },
  },
};
</script>

<style scoped lang="less">
.table-viewport {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: var(--table-viewport-height, 100%);
  min-width: 0;
  overflow: hidden;
}

.table-viewport-fill-height {
  flex: 1 1 0;
}

.table-viewport :deep(.ant-table-wrapper) {
  display: flex;
  flex: 1 1 0;
  height: 0;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.table-viewport :deep(.ant-spin-nested-loading),
.table-viewport :deep(.ant-spin-container) {
  display: flex;
  flex: 1 1 0;
  height: 0;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.table-viewport :deep(.ant-table) {
  display: flex;
  flex: 1 1 0;
  height: 0;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.table-viewport :deep(.ant-table-container) {
  display: flex;
  flex: 1 1 0;
  height: 0;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.table-viewport :deep(.ant-table-content) {
  flex: 1 1 0;
  min-height: 0;
  height: 0;
  max-height: none;
  overflow-x: auto !important;
  overflow-y: auto !important;
  scrollbar-width: none;
  scrollbar-color: transparent transparent;
}

.table-viewport :deep(.ant-table-content > table > thead > tr > th) {
  position: sticky;
  z-index: 3;
  top: 0;
  background: #fafafa;
}

.table-viewport :deep(.ant-table-content > table > thead > tr > th.ant-table-cell-fix-left),
.table-viewport :deep(.ant-table-content > table > thead > tr > th.ant-table-cell-fix-right) {
  z-index: 4;
}

.table-viewport :deep(.ant-table-header) {
  margin-right: 0 !important;
}

.table-viewport :deep(.ant-table-content::-webkit-scrollbar) {
  width: 0;
  height: 0;
}

.table-viewport :deep(.ant-table-pagination) {
  flex: 0 0 auto;
}

.table-viewport-empty :deep(.ant-table-placeholder > td) {
  height: var(--table-empty-body-height);
  vertical-align: middle;
}

.table-viewport-empty :deep(.table-native-empty-hidden .ant-table-placeholder) {
  visibility: hidden;
}

.table-empty-overlay {
  position: absolute;
  z-index: 1;
  top: 40px;
  right: 0;
  bottom: var(--table-empty-overlay-bottom);
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.table-empty-overlay :deep(.ant-empty) {
  margin: 0;
}

.table-overlay-scrollbar {
  position: absolute;
  z-index: 20;
  opacity: 0;
  pointer-events: none;
  transition: opacity 120ms ease;
}

.table-viewport:hover .table-overlay-scrollbar,
.table-viewport-dragging .table-overlay-scrollbar,
.table-viewport-scrolling .table-overlay-scrollbar {
  opacity: 1;
}

.table-overlay-scrollbar-vertical {
  width: 6px;
}

.table-overlay-scrollbar-horizontal {
  height: 6px;
}

.table-overlay-scrollbar-thumb {
  position: absolute;
  display: block;
  box-sizing: border-box;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.28);
  pointer-events: auto;
  cursor: grab;
}

.table-overlay-scrollbar-thumb:hover,
.table-viewport-dragging .table-overlay-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.5);
}

.table-overlay-scrollbar-horizontal .table-overlay-scrollbar-thumb {
  top: 0;
  bottom: 0;
}

.table-overlay-scrollbar-vertical .table-overlay-scrollbar-thumb {
  right: 0;
  left: 0;
}
</style>
