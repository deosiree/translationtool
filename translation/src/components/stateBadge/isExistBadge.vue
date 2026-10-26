<template>
  <template v-if="status">
    <a-badge :color="status.color" /><span :style="{ color: status.color }">{{ status.label }}</span>
  </template>
</template>

<script>
import { getIsExistStatus } from "@/constants/statusMaps.js";

export default {
  props: {
    isExist: {
      type: [Number, String, null],
    },
  },
  computed: {
    /**
     * 解析当前存在状态的公共展示定义。
     * @returns {{label: string, color: string}|undefined} 匹配的状态定义。
     */
    status() {
      return getIsExistStatus(this.isExist);
    },
  },
};
</script>
