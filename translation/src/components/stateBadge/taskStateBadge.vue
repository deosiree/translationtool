<template>
  <template v-if="type === 'normal'">
    <template v-if="taskState === '0'">
      <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
    </template>
    <template v-else-if="taskState === '1'">
      <a-badge color="#FBB31F" /><span style="color:#FBB31F">词条待导入</span>
    </template>
    <template v-else-if="taskState === '2'">
      <a-badge color="#FBB31F" /><span style="color:#FBB31F">词条待审核</span>
    </template>
    <template v-else-if="taskState === '3'">
      <a-badge color="#FBB31F" /><span style="color:#FBB31F">词条待翻译</span>
    </template>
    <template v-else-if="taskState === '4'">
      <a-badge color="#FBB31F" /><span style="color:#FBB31F">翻译待审核</span>
    </template>
    <template v-else-if="taskState === '5'">
      <a-badge color="#FBB31F" /><span style="color:#FBB31F">待导出</span>
    </template>
    <template v-else>
      <a-badge color="#36BF7D" /><span style="color:#36BF7D">已完成</span>
    </template>
  </template>
  <template v-else-if="type === 'sum'">
    <template v-if="taskState === '0'">
      <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
    </template>
    <template v-else-if="taskState > '0' && taskState < '6'">
      <a-badge color="#FBB31F" /><span style="color:#FBB31F">流程中</span>
    </template>
    <template v-else-if="taskState === '6'">
      <a-badge color="#36BF7D" /><span style="color:#36BF7D">已归档</span>
    </template>
    <template v-else>
    </template>
  </template>
</template>

<script>
export default {
  props: {
    type: {
      type: String,
      required: true,
    },
    taskState: {
      type: [String, null], // 允许字符串或null
    },
  },
  methods: {},
};
</script>
