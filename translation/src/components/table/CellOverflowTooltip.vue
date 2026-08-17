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
      default: "-",
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
    onMouseEnter(event) {
      const el = event.currentTarget;
      this.isOverflow = Boolean(this.tooltipContent) && isTextOverflow(el);
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
