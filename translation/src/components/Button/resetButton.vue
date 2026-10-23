<template>
  <!-- 将 size 属性绑定到 a-button 上 -->
  <a-button
    type="primary"
    :size="size"
    :disabled="disabled"
    class="resetBtn"
    @click="handleReset"
  >重置</a-button>
</template>

<script>
export default {
  props: {
    search: {
      type: Object,
      required: true
    },
    currentPage: {
      type: Number,
      required: true
    },
    fetchData: {
      type: Function
    },
    size: {
      type: String,
      default: 'middle'
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    /**
     * 清空查询条件并通知父组件重新查询。
     * @returns {void} 无返回值。
     */
    handleReset() {
      if (this.disabled) return;
      const newSearch = {};
      for (let key in this.search) {
        if (this.search.hasOwnProperty(key)) {
          newSearch[key] = null;
        }
      }
      const newPage = 1;
      // 触发自定义事件通知父组件重置搜索条件和页码
      this.$emit('resetData', newSearch, newPage);
      // 检查 fetchData 是否为函数，如果是则调用
      if (typeof this.fetchData === 'function') {
        this.fetchData();
      }
    }
  }
};
</script>
