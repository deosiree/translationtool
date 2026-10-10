<template>
  <!-- 用计算属性translateState_代替直接修改prop(子组件不能直接修改父组件传递的 prop​​) -->
  <a-select v-model:value="translateState_" :placeholder="placeholder" :size="size" :style="style" @click="clickInput" allowClear>
    <a-select-option value="0" v-if="!filter.has('0')">未翻译</a-select-option>
    <a-select-option value="1" v-if="!filter.has('1')">待审核</a-select-option>
    <a-select-option value="2" v-if="!filter.has('2')">审核不通过</a-select-option>
    <a-select-option value="3" v-if="!filter.has('3')">已审核</a-select-option>
  </a-select>
</template>
<script>
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
      type: [String, null], // 允许数字或null
    },
    filter: {
      type: Set,
      default: new Set(),
    },
  },
  computed: {
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
    clickInput(event) {
      event.stopPropagation();
    },
  },
};
</script>
