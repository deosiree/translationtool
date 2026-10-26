<template>
  <template v-if="status">
    <a-badge :color="status.color" /><span :style="{ color: status.color }">{{ status.label }}</span>
  </template>
</template>

<script>
import { getTranslateStateStatus } from "@/constants/statusMaps.js";

export default {
  props: {
    translateState: {
      type: [Number, String, null],
    },
  },
  computed: {
    /**
     * 解析当前翻译审核状态的公共展示定义，未知状态按未翻译处理。
     * @returns {{label: string, color: string}} 翻译状态定义。
     */
    status() {
      return getTranslateStateStatus(this.translateState) || getTranslateStateStatus("0");
    },
  },
};
</script>
