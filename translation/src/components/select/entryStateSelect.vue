<template>
  <!-- 用计算属性entryState_代替直接修改prop(子组件不能直接修改父组件传递的 prop​​) -->
  <a-select v-model:value="entryState_" placeholder="请选择" :size="size" :style="style" @click="clickInput" allowClear>
    <template v-for="option in entryStateOptions" :key="option.value">
      <a-select-option v-if="!filter.has(option.value)" :value="option.value">
        {{ option.label }}
      </a-select-option>
    </template>
  </a-select>
</template>
<script>
import { getEntryStateOptions } from "@/constants/statusMaps.js";

export default {
  emits: ["update:entryState"],
  props: {
    size: {
      type: String,
    },
    style: {
      type: String,
    },
    entryState: {
      type: [Number, String, null], // 允许数字或null
    },
    filter: {
      type: Set,
      default: new Set(),
    },
    showForbbiden: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    /**
     * 获取按禁用状态开关过滤后的词条审核状态选项。
     * @returns {{label: string, value: string}[]} 词条审核状态选项。
     */
    entryStateOptions() {
      return getEntryStateOptions(this.showForbbiden);
    },
    // 计算属性实现双向绑定
    entryState_: {
      get() {
        return this.entryState; // 读取prop的值
      },
      set(val) {
        this.$emit("update:entryState", val); // 修改时通知父组件
      },
    },
    showForbbiden_: {
      get() {
        return this.showForbbiden; // 读取prop的值
      },
      set(val) {
        this.$emit("update:showForbbiden", val); // 修改时通知父组件
      },
    },
  },
  methods: {
    /**
     * 阻止下拉点击事件冒泡到外层行点击逻辑。
     * @param {MouseEvent} event - 下拉点击事件。
     * @returns {void} 无返回值。
     */
    clickInput(event) {
      event.stopPropagation();
    },
  },
};
</script>
