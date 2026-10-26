<!-- @fileoverview 统一表格 tag/comment 列头说明图标与 Tooltip。 -->
<template>
  <a-tooltip v-if="resolvedHint" :trigger="['hover', 'focus']" placement="top">
    <template #title>{{ resolvedHint }}</template>
    <span
      class="table-header-hint"
      tabindex="0"
      role="img"
      :aria-label="`${title}：${resolvedHint}`"
    >
      <span class="table-header-hint__title">{{ title }}</span>
      <InfoCircleOutlined class="table-header-hint__icon" aria-hidden="true" />
    </span>
  </a-tooltip>
  <span v-else>{{ title }}</span>
</template>

<script>
import { InfoCircleOutlined } from "@ant-design/icons-vue";
import { getTableHeaderHint } from "./tableHeaderHints.js";

export default {
  name: "TableHeaderHint",
  components: { InfoCircleOutlined },
  props: {
    title: {
      type: [String, Number],
      default: "",
    },
    column: {
      type: Object,
      default: () => ({}),
    },
    hint: {
      type: String,
      default: "",
    },
  },
  computed: {
    /**
     * 根据显式提示或列字段返回列头说明。
     * @returns {string} 当前列头的提示文案。
     */
    resolvedHint() {
      return this.hint || getTableHeaderHint(this.column);
    },
  },
};
</script>

<style scoped lang="less">
.table-header-hint {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  max-width: 100%;
  cursor: help;
  outline: none;
}

.table-header-hint:focus-visible {
  border-radius: 2px;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.table-header-hint__title {
  min-width: 0;
}

.table-header-hint__icon {
  flex: 0 0 auto;
  margin-left: 4px;
  color: #8c8c8c;
  font-size: 12px;
}
</style>
