<template>
  <a-button type="primary" :size="size" @click="batchSelectOpen" v-if="!batchSelectFlag">批量选择</a-button>
  <a-button type="primary" :size="size" @click="batchSelectAll" v-if="batchSelectFlag" :loading="loading">选择全部</a-button>
  <a-button type="primary" :size="size" @click="batchSelectCancel" class="yellowBtn" v-if="batchSelectFlag">取消选择</a-button>
  <a-badge :count="selectEntry.length" :overflow-count="99" v-if="batchSelectFlag">
    <a-button type="primary" :size="size" class="resetBtn" @click="viewSelectEntry">已选词条</a-button>
  </a-badge>
  <BatchSelectModal :visible="batchSelectVisible" :dataSource="selectEntry" @update:dataSource="selectEntry = $event"
    :selectedRowKeys="selectedRowKeys" @update:selectedRowKeys="selectedRowKeys = $event" :selectedRows="selectedRows"
    @update:selectedRows="selectedRows = $event" :columns="columns" @batchSelectClose="batchSelectClose" @batchSelectCancel="batchSelectCancel"
    @refresh="getSearch" />
</template>

<script>
import { setModalAriaHidden } from "@/utils/commonUtils";
import BatchSelectModal from "@/components/Button/batchSelectModal.vue";
export default {
  components: {
    BatchSelectModal,
  },
  emits: [
    "update:loading",
    "update:search",
    "update:lastSearch",
    "update:selectEntry",
    "update:selectedRows",
    "update:selectedRowKeys",
    "update:batchSelectFlag",
    "update:batchSelectVisible",
  ],
  props: {
    size: {
      type: String,
      default: "middle",
    },
    columns: {
      type: Array,
      default: () => [],
    },
    search: {
      type: Object,
      default: false,
    },
    lastSearch: {
      type: Object,
      default: false,
    },
    dataSource: {
      type: Array,
      default: () => [],
    },
    getSearch: { type: Function },
    // 添加其他在模板中使用但未定义的属性
    loading: {
      type: Boolean,
      default: false,
    },
    selectEntry: {
      type: Array,
      default: () => [],
    },
    selectedRows: {
      type: Array,
      default: () => [],
    },
    selectedRowKeys: {
      type: Array,
      default: () => [],
    },
    batchSelectFlag: {
      type: Boolean,
      default: false,
    },
    batchSelectVisible: {
      type: Boolean,
      default: false,
    },
  },
  methods: {
    // 批量选择展开
    batchSelectOpen() {
      if (this.batchSelectOnChange(this.getSearch))
        // 判断搜索条件是否变化，如果变化则重新查询
        return;
      this.$emit("update:batchSelectFlag", true);
      this.$emit("update:selectEntry", []);
      this.$emit("update:selectedRows", []);
      this.$emit("update:selectedRowKeys", []);
    },
    // 全部选择
    batchSelectAll() {
      if (this.batchSelectOnChange(this.getSearch))
        // 判断搜索条件是否变化，如果变化则重新查询
        return;
      if (Object.keys(this.dataSource).length === 0) {
        return;
      }
      this.$emit("update:loading", true);
      this.$emit("update:selectEntry", this.dataSource);
      this.$emit("update:selectedRows", this.dataSource);
      const keys = [];
      this.dataSource.forEach((item) => {
        keys.push(item.id);
      });
      this.$emit("update:selectedRowKeys", keys);
      this.$emit("update:loading", false);
    },
    // 取消选择（批量选择取消展开，清空已选词条）
    batchSelectCancel() {
      this.$emit("update:selectEntry", []);
      this.$emit("update:selectedRows", []);
      this.$emit("update:selectedRowKeys", []);
      this.$emit("update:batchSelectFlag", false); // 关闭批量选择的展开，变成只显示批量选择按钮
      this.batchSelectClose(); // 关闭已选词条弹窗
      // if (typeof this.getSearch === "function") {
      //   // 有的接口太慢了，先不刷新
      //   this.getSearch();
      // }
    },
    // 已选词条
    viewSelectEntry() {
      if (this.batchSelectOnChange(this.batchSelectOpen))
        // 判断搜索条件是否变化，如果变化则重新打开批量选择
        return;
      this.$emit("update:batchSelectVisible", true); // 打开已选词条的弹窗
      setModalAriaHidden(this, document);
    },
    // 关闭已选词条
    batchSelectClose() {
      this.$emit("update:batchSelectVisible", false); // 关闭已选词条的弹窗
    },
    // 判断搜索条件是否变化，如果变化则调用fetchData
    batchSelectOnChange(fetchData) {
      // console.log(
      //   "search",
      //   this.search,
      //   "this.lastSearch",
      //   this.lastSearch,
      //   JSON.stringify(this.lastSearch) !== JSON.stringify(this.search)
      // );
      if (JSON.stringify(this.lastSearch) !== JSON.stringify(this.search)) {
        // 搜索条件有变化，重新选择
        if (typeof fetchData === "function") {
          fetchData();
        }
        return true;
      }
      return false;
    },
  },
};
</script>

<style scoped lang="less">
/* 可以在这里添加组件的样式 */
</style>