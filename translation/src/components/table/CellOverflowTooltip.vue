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
    tooltipContent() {
      const raw = this.content == null ? "" : String(this.content);
      return raw.trim();
    },
    display() {
      return this.tooltipContent || this.emptyText;
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
        Boolean(this.tooltipContent) &&
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
