<template>
  <div class="glossary-search-operate">
    <ResetButton
      :size="size"
      :search="search"
      :currentPage="currentPage"
      @resetData="onResetData"
    />
    <a-button type="primary" :size="size" @click="$emit('search')">查询</a-button>
  </div>
</template>

<script>
import ResetButton from "@/components/Button/resetButton.vue";

export default {
  name: "GlossarySearchOperate",
  components: { ResetButton },
  emits: ["search", "reset"],
  props: {
    search: { type: Object, required: true },
    currentPage: { type: Number, required: true },
    size: { type: String, default: "middle" },
  },
  methods: {
    /**
     * 转发重置事件给父组件
     * @param {Object} newSearch
     * @param {number} newPage
     * @returns {void}
     */
    onResetData(newSearch, newPage) {
      this.$emit("reset", newSearch, newPage);
    },
  },
};
</script>

<style scoped>
.glossary-search-operate {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
