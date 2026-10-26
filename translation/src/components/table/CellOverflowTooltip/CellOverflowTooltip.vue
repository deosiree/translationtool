<template>
  <a-tooltip
    :title="isOverflow ? tooltipContent : undefined"
    placement="top"
    :mouseEnterDelay="1"
    :mouseLeaveDelay="0.1"
    :overlayStyle="{ maxWidth: '400px' }"
    :getPopupContainer="getPopupContainer"
  >
    <span
      class="cell-overflow-tooltip"
      @mouseenter="onMouseEnter"
      @mouseleave="onMouseLeave"
    >
      <slot>{{ display }}</slot>
    </span>
  </a-tooltip>
</template>

<script>
import { isTextOverflow } from "./isTextOverflow.js";

export default {
  name: "CellOverflowTooltip",
  props: {
    content: {
      type: [String, Number],
      default: "",
    },
    emptyText: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      isOverflow: false,
    };
  },
  computed: {
    /**
     * 返回保留原始字符的 Tooltip 内容，避免首尾空格在浏览态丢失。
     * @returns {string} 用于展示完整文本的内容。
     */
    tooltipContent() {
      const raw = this.content == null ? "" : String(this.content);
      return raw;
    },
    /**
     * 判断当前内容是否包含可展示字符，避免空文本生成 Tooltip。
     * @returns {boolean} 是否允许显示 Tooltip。
     */
    hasTooltipContent() {
      return Boolean(this.tooltipContent.trim());
    },
    /**
     * 返回单元格浏览态显示文本，空白内容保持为空。
     * @returns {string} 单元格中显示的文本。
     */
    display() {
      return this.hasTooltipContent ? this.tooltipContent : this.emptyText;
    },
  },
  methods: {
    getPopupContainer() {
      return document.body;
    },
    /**
     * 根据提示容器或其内部文本节点的溢出状态决定是否显示完整文本。
     * @param {MouseEvent} event - 当前提示文本节点的鼠标进入事件。
     * @returns {void} 更新 Tooltip 显示状态。
     */
    onMouseEnter(event) {
      const el = event.currentTarget;
      const contentEl = el.firstElementChild || el;
      this.isOverflow =
        this.hasTooltipContent &&
        (isTextOverflow(el) || isTextOverflow(contentEl));
    },
    onMouseLeave() {
      this.isOverflow = false;
    },
  },
};
</script>

<style scoped lang="less">
.cell-overflow-tooltip {
  display: block;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
