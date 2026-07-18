<template>
  <span
    v-if="visible"
    class="operation-column-op-item"
    role="button"
    tabindex="0"
    :data-op-label="label"
    :data-op-icon="icon || undefined"
    :data-op-icon-class="iconClass || undefined"
    :data-op-type="type"
    @click="emitActivate"
    @keydown.enter.prevent="emitActivate"
    @keydown.space.prevent="emitActivate"
  >
    <OpItemContent
      :label="label"
      :icon="icon"
      :icon-class="iconClass"
      :type="type"
      variant="inline"
    />
  </span>
</template>

<script>
/**
 * @file 声明式操作槽：data-op-* 元数据、点击与键盘可达性。
 * @module OperationColumn/OpItem
 */
import OpItemContent from "./OpItemContent.vue";

export default {
  name: "OpItem",
  components: { OpItemContent },
  props: {
    label: { type: String, required: true },
    icon: { type: String, default: undefined },
    iconClass: { type: String, default: undefined },
    /** primary | danger | success | warning */
    type: { type: String, default: "primary" },
    /** 预留；本仓无权限体系时忽略，始终展示 */
    perm: { type: [String, Array], default: undefined },
  },
  emits: ["click"],
  data() {
    return {
      visible: true,
    };
  },
  methods: {
    /**
     * @param {MouseEvent | KeyboardEvent} event
     */
    emitActivate(event) {
      event?.stopPropagation?.();
      this.$emit("click", event);
    },
  },
};
</script>

<style scoped>
.operation-column-op-item {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 24px;
  vertical-align: middle;
  outline: none;
  border-radius: 2px;
}

.operation-column-op-item:hover:not(.operation-column-op-item--hidden)
  :deep(.operation-column-op-item__content--inline.operation-column-op-item__content--primary) {
  color: #40a9ff;
}

.operation-column-op-item:active:not(.operation-column-op-item--hidden)
  :deep(.operation-column-op-item__content--inline.operation-column-op-item__content--primary) {
  color: #096dd9;
}

.operation-column-op-item:hover:not(.operation-column-op-item--hidden)
  :deep(.operation-column-op-item__content--inline.operation-column-op-item__content--danger) {
  color: #ff7875;
}

.operation-column-op-item:active:not(.operation-column-op-item--hidden)
  :deep(.operation-column-op-item__content--inline.operation-column-op-item__content--danger) {
  color: #d9363e;
}

.operation-column-op-item:hover:not(.operation-column-op-item--hidden)
  :deep(.operation-column-op-item__content--inline.operation-column-op-item__content--success) {
  color: #5fd49a;
}

.operation-column-op-item:active:not(.operation-column-op-item--hidden)
  :deep(.operation-column-op-item__content--inline.operation-column-op-item__content--success) {
  color: #2aa86c;
}

.operation-column-op-item:hover:not(.operation-column-op-item--hidden)
  :deep(.operation-column-op-item__content--inline.operation-column-op-item__content--warning) {
  color: #fcc54c;
}

.operation-column-op-item:active:not(.operation-column-op-item--hidden)
  :deep(.operation-column-op-item__content--inline.operation-column-op-item__content--warning) {
  color: #e0a010;
}

.operation-column-op-item.operation-column-op-item--hidden {
  display: none !important;
}
</style>
