<template>
  <div ref="box" class="box">
    <a-tabs v-model:activeKey="activeKey" ref="tab" @change="changeTab">
      <a-tab-pane key="filterExcel" tab="送翻去重">
        <FilterExcel :boxHeight="boxHeight" />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>
<script>
import FilterExcel from "./filterExcel.vue";

export default {
  name: "fileManage",
  components: {
    FilterExcel,
  },
  data() {
    return {
      activeKey: "filterExcel",
      boxHeight: 0,
    };
  },
  mounted() {
    let _this = this;
    this.$nextTick(() => {
      this.$store.commit("setTabActive", this.activeKey);
      _this.boxHeight = _this.$refs.box.offsetHeight;
      /** 控制table的高度 */
      window.onresize = function () {
        _this.boxHeight = _this.$refs.box.offsetHeight;
      };
    });
  },
  methods: {
    changeTab(activeKey) {
      this.$store.commit("setTabActive", activeKey);
    },
  },
};
</script>
<style lang="less" scoped>
.box {
  width: 100%;
  height: 100%;
  padding: 8px 24px 24px 24px;
}

:deep(.ant-tabs-nav) {
  margin: 0 0 10px 0;
}

:deep(.ant-tabs) {
  height: 100%;
}

:deep(.ant-tabs-content) {
  height: 100%;
  position: relative;
}

:deep(.ant-tabs-tab-btn) {
  font-size: 12px;
}
</style>
