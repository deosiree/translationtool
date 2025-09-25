<template>
  <template v-if="translateState === '0'">
    <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">未翻译</span>
  </template>
  <template v-else-if="translateState === '1'">
    <a-badge color="#FBB31F" /><span style="color:#FBB31F">待审核</span>
  </template>
  <template v-else-if="translateState === '2'">
    <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
  </template>
  <template v-else-if="translateState === '3'">
    <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
  </template>
  <template v-else>
    <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">未翻译</span>
  </template>
</template>

<script>
export default {
  props: {
    translateState: {
      type: [String, null], // 允许字符串或null
    },
  },
  methods: {},
};
</script>
