<template>
  <template v-if="entryState === 0">
    <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
  </template>
  <template v-else-if="entryState === 1">
    <a-badge color="#FBB31F" /><span style="color:#FBB31F">审核中</span>
  </template>
  <template v-else-if="entryState === 2">
    <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
  </template>
  <template v-else-if="entryState === 3">
    <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
  </template>
  <template v-else-if="entryState === -1">
    <a-badge color="#9a9a9a" /><span style="color:#9a9a9a">禁用</span>
  </template>
  <template v-else>
  </template>
</template>



<script>
export default {
  props: {
    entryState: {
      type: [Number, null], // 允许数字或null
    },
  },
  methods: {},
};
</script>
