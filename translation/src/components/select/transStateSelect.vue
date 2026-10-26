<template>
  <!-- 用计算属性translateState_代替直接修改prop(子组件不能直接修改父组件传递的 prop​​) -->
  <a-select v-model:value="translateState_" :placeholder="placeholder" :size="size" :style="style" @click="clickInput" allowClear>
    <template v-for="option in translateStateOptions" :key="option.value">
      <a-select-option v-if="!filter.has(option.value)" :value="option.value">
        {{ option.label }}
      </a-select-option>
    </template>
  </a-select>
</template>
<script>
import { getTranslateStateOptions } from "@/constants/statusMaps.js";

export default {
  emits: ["update:translateState"],
  props: {
    size: {
      type: String,
    },
    style: {
      type: String,
    },
    placeholder: {
      type: String,
      default:"请选择",
    },
    translateState: {
      type: [Number, String, null],
    },
    filter: {
      type: Set,
      default: new Set(),
    },
  },
  computed: {
    /**
     * 获取翻译审核状态选项。
     * @returns {{label: string, value: string}[]} 翻译审核状态选项。
     */
    translateStateOptions() {
      return getTranslateStateOptions();
    },
    // 计算属性实现双向绑定
    translateState_: {
      get() {
        return this.translateState; // 读取prop的值
      },
      set(val) {
        this.$emit("update:translateState", val); // 修改时通知父组件
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
