<template>
  <a-button type="primary" :size="size" @click="gitOpen" v-if="!gitFlag" class="yellowBtn">Git操作</a-button>
  <!-- <GitPushButton :dataSource="[]" :fieldOptions="[]" size="middle" buttonTitle="推送push" v-if="gitFlag" /> -->
  <!-- <a-button type="primary" :size="size" @click="gitAll" v-if="gitFlag" :loading="loading" class="resetBtn">创建分支</a-button>
  <a-button type="primary" :size="size" @click="gitAll" v-if="gitFlag" :loading="loading" class="resetBtn">切换分支</a-button> -->


  <!-- <a-badge :count="selectEntry.length" :overflow-count="99" v-if="gitFlag" style="margin-left: 10px;">
    <a-button type="primary" :size="size" class="resetBtn" @click="viewSelectEntry">切换分支</a-button>
  </a-badge> -->

  <a-button type="primary" :size="size" @click="gitCancel" v-if="gitFlag" class="yellowBtn">取消Git操作</a-button>
</template>

<script>
import { setModalAriaHidden } from "@/utils/commonUtils";
import GitCommitButton from "@/components/Button/gitCommitButton.vue";
export default {
  components: {
    GitCommitButton,
  },
  emits: ["update:loading"],
  props: {
    size: {
      type: String,
      default: "middle",
    },
    // 添加其他在模板中使用但未定义的属性
    loading: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      gitFlag: false,
      gitVisible: false,
    };
  },
  methods: {
    // 批量选择展开
    gitOpen() {
      this.gitFlag = true;
    },
    // 取消选择（批量选择取消展开，清空已选词条）
    gitCancel() {
      this.gitFlag = false;
    },
    // 已选词条
    viewSelectEntry() {
      this.gitVisible = true; // 打开已选词条的弹窗
      setModalAriaHidden(this, document);
    },
  },
};
</script>

<style scoped lang="less">
/* 可以在这里添加组件的样式 */
</style>