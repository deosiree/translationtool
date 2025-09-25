<template>
  <template v-if="isExist === 0">
    <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
  </template>
  <template v-else-if="isExist === 1">
    <a-badge color="#FBB31F" /><span style="color:#FBB31F">已存在</span>
  </template>
  <template v-else>
  </template>
</template>



<script>
export default {
  props: {
    isExist: {
      type: [Number, null], // 允许数字或null
    },
  },
  methods: {},
};
</script>
