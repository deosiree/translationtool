<template>
  <span
    class="selection-header-checkbox"
    :aria-busy="isBusy ? 'true' : 'false'"
  >
    <Checkbox
      :checked="state === 'checked'"
      :indeterminate="state === 'indeterminate'"
      :disabled="disabled || isBusy"
      aria-label="全选当前查询结果"
      @change="$emit('change', $event.target.checked)"
    />
    <LoadingOutlined v-if="isBusy" spin aria-hidden="true" />
    <span
      v-if="isBusy"
      class="selection-header-checkbox-status"
      role="status"
      aria-live="polite"
    >
      {{ state === 'querying' ? '查询已提交，等待重算表头状态' : '正在判断全选状态' }}
    </span>
  </span>
</template>

<script>
import Checkbox from "ant-design-vue/es/checkbox";
import { LoadingOutlined } from "@ant-design/icons-vue";

export default {
  name: "SelectionHeaderCheckbox",
  components: { Checkbox, LoadingOutlined },
  props: {
    state: { type: String, default: "unchecked" },
    disabled: { type: Boolean, default: false },
  },
  computed: {
    /**
     * 判断表头是否正在等待查询结果或执行跨页核对。
     * @returns {boolean} 表头是否处于忙碌状态。
     */
    isBusy() {
      return this.state === "checking" || this.state === "querying";
    },
  },
  emits: ["change"],
};
</script>

<style scoped>
.selection-header-checkbox {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.selection-header-checkbox-status {
  position: absolute;
  z-index: 20;
  top: calc(100% + 4px);
  left: 0;
  padding: 3px 6px;
  color: #262626;
  background: #fff;
  border: 1px solid #d9d9d9;
  box-shadow: 0 2px 6px rgb(0 0 0 / 12%);
  width: max-content;
  max-width: 220px;
  line-height: 20px;
  white-space: normal;
  pointer-events: none;
}
</style>
