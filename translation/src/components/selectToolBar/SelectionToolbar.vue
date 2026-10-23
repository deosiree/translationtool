<!-- @fileoverview 选择工具栏布局容器，仅负责插槽内容的对齐、换行与间距。 -->
<template>
  <div class="select-toolbar">
    <slot />
  </div>
</template>

<style scoped lang="less">
.select-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
</style>
