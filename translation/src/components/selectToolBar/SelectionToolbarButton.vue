<!-- @fileoverview 统一选择工具栏按钮的尺寸、预设样式、徽标与加载状态。 -->
<template>
  <a-badge
    :count="loading ? 0 : badgeCount || 0"
    :overflow-count="overflowCount"
    class="select-toolbar-button-badge"
  >
    <a-button
      :type="buttonType"
      size="small"
      :disabled="disabled || loading"
      :class="['select-toolbar-button', presetClass, buttonClass]"
      :aria-label="ariaLabel || text"
      :aria-busy="loading"
      @click="handleClick"
    >
      <span
        class="select-toolbar-button-content"
        :class="{ 'select-toolbar-button-content-hidden': loading }"
      >
        <component :is="icon" v-if="icon" />
        <span>{{ text }}</span>
      </span>
      <span v-if="loading" class="select-toolbar-button-spinner" aria-hidden="true">
        <LoadingOutlined />
      </span>
    </a-button>
  </a-badge>
</template>

<script>
import { LoadingOutlined } from "@ant-design/icons-vue";

const PRESET_CONFIG = {
  primary: { buttonType: "primary", buttonClass: "" },
  default: { buttonType: "default", buttonClass: "" },
  text: { buttonType: "text", buttonClass: "" },
  selected: { buttonType: "primary", buttonClass: "resetBtn" },
  warning: { buttonType: "primary", buttonClass: "yellowBtn" },
};

export default {
  name: "SelectionToolbarButton",
  components: { LoadingOutlined },
  props: {
    text: { type: String, required: true },
    preset: {
      type: String,
      default: "default",
      validator: (value) => Object.prototype.hasOwnProperty.call(PRESET_CONFIG, value),
    },
    icon: { type: [Object, Function], default: null },
    disabled: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
    badgeCount: { type: Number, default: null },
    overflowCount: { type: Number, default: 99 },
    ariaLabel: { type: String, default: "" },
    buttonClass: { type: String, default: "" },
  },
  emits: ["click"],
  computed: {
    /**
     * 返回当前预设的按钮配置，统一维护按钮类型和语义颜色。
     * @returns {Object} 当前按钮预设配置。
     */
    presetConfig() {
      return PRESET_CONFIG[this.preset] || PRESET_CONFIG.default;
    },
    /**
     * 将按钮预设转换为 Ant Design Vue 按钮类型。
     * @returns {string} Ant Design Vue 按钮类型。
     */
    buttonType() {
      return this.presetConfig.buttonType;
    },
    /**
     * 返回按钮预设附带的语义颜色类名。
     * @returns {string} 预设颜色类名。
     */
    presetClass() {
      return this.presetConfig.buttonClass;
    },
  },
  methods: {
    /**
     * 将按钮点击事件转发给业务页面。
     * @param {Event} event - 原生按钮点击事件。
     * @returns {void} 无返回值。
     */
    handleClick(event) {
      this.$emit("click", event);
    },
  },
};
</script>

<style scoped lang="less">
.select-toolbar-button-badge {
  line-height: 1;
}

.select-toolbar-button-badge :deep(.ant-badge-count) {
  z-index: 999;
}

.select-toolbar-button {
  white-space: nowrap;
}

.select-toolbar-button-content {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 22px;
}

.select-toolbar-button-content-hidden {
  visibility: hidden;
}

.select-toolbar-button-spinner {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
</style>
