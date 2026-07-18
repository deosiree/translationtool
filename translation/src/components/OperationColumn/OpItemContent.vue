<template>
  <a-tooltip
    :title="variant === 'menu' ? label : undefined"
    placement="top"
    :mouseEnterDelay="0.3"
  >
    <span class="operation-column-op-item__content" :class="contentClass">
      <span
        v-if="iconClass"
        class="operation-column-op-item__icon operation-column-op-item__icon--svg"
        :class="iconClass"
      />
      <span class="operation-column-op-item__label">{{ label }}</span>
    </span>
  </a-tooltip>
</template>

<script>
/**
 * @file 操作槽视觉：文案为主；menu 变体带 tooltip。
 * @module OperationColumn/OpItemContent
 */
import { computed } from "vue";

export default {
  name: "OpItemContent",
  props: {
    label: { type: String, required: true },
    icon: { type: String, default: undefined },
    iconClass: { type: String, default: undefined },
    /** primary | danger | success | warning */
    type: { type: String, default: "primary" },
    variant: { type: String, default: "inline" },
  },
  setup(props) {
    const contentClass = computed(() => [
      `operation-column-op-item__content--${props.type}`,
      `operation-column-op-item__content--${props.variant}`,
    ]);

    return { contentClass };
  },
};
</script>

<style scoped>
.operation-column-op-item__content {
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  min-width: 0;
  max-width: 100%;
  height: 24px;
  padding: 0 2px;
  cursor: pointer;
  user-select: none;
}

.operation-column-op-item__content--primary {
  color: #1890ff;
}

.operation-column-op-item__content--danger {
  color: #ff4d4f;
}

/* 对齐 common.less .resetBtn */
.operation-column-op-item__content--success {
  color: #36bf7d;
}

/* 对齐 common.less .yellowBtn */
.operation-column-op-item__content--warning {
  color: #fbb31f;
}

.operation-column-op-item__content--inline {
  transition: color 0.2s;
}

.operation-column-op-item__content--menu {
  width: 100%;
}

.operation-column-op-item__icon {
  flex-shrink: 0;
  width: 14px;
  height: 14px;
  margin-right: 4px;
  font-size: 14px;
}

.operation-column-op-item__icon--svg {
  display: inline-block;
}

.operation-column-op-item__label {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  line-height: 24px;
  white-space: nowrap;
}

.operation-column-op-item__content--menu .operation-column-op-item__label {
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
