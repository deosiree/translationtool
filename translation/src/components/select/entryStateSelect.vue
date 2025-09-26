<template>
  <!-- 用计算属性entryState_代替直接修改prop(子组件不能直接修改父组件传递的 prop​​) -->
  <a-select v-model:value="entryState_" placeholder="请选择" :size="size" :style="style" @click="clickInput" allowClear>
    <a-select-option value="0" v-if="!filter.has('0')">新建</a-select-option>
    <a-select-option value="1" v-if="!filter.has('1')">审核中</a-select-option>
    <a-select-option value="2" v-if="!filter.has('2')">审核不通过</a-select-option>
    <a-select-option value="3" v-if="!filter.has('3')">已审核</a-select-option>
    <a-select-option value=-1 v-if="showForbbiden_&&!filter.has(-1)">禁用</a-select-option>
  </a-select>
</template>
<script>
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
    clickInput(event) {
      event.stopPropagation();
    },
  },
};
</script>
