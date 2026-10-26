<template>
  <template v-if="status">
    <a-badge :color="status.color" /><span :style="{ color: status.color }">{{ status.label }}</span>
  </template>
</template>

<script>
import { getEntryStateStatus } from "@/constants/statusMaps.js";

export default {
  props: {
    entryState: {
      type: [Number, String, null],
    },
  },
  computed: {
    /**
     * 解析当前词条审核状态的公共展示定义。
     * @returns {{label: string, color: string}|undefined} 匹配的状态定义。
     */
    status() {
      return getEntryStateStatus(this.entryState);
    },
  },
};
</script>
