<template>
  <!-- 预翻译：词条管理-已选词条-预翻译配置弹窗（语种多选 + 翻译优先级） -->
  <CustomModal modalTitle="预翻译" modalWidth="500px" :modalVisible="visible" :okLoading="loading" @handleClose="handleClose"
    @handleOK="handleOK" @afterClose="afterClose">
    <div style="width:100%;height:100%">
      <a-form :model="preTran" autocomplete="off" ref="preTranslateForm" :label-col="{ span: 6 }">
        <a-form-item label="翻译语种" name="language" :rules="[{ required: true, message: '请选择翻译语种!' }]">
          <a-select mode="multiple" v-model:value="preTran.language" :options="langOptions" placeholder="请选择"
            allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="翻译优先级" name="priority">
          <a-select v-model:value="preTran.priority" :options="translatePriorityOptions" placeholder="请选择">
          </a-select>
        </a-form-item>
      </a-form>
    </div>
  </CustomModal>
</template>
<script>
import CustomModal from "@/components/modal/index.vue";
import { TRANSLATE_PRIORITY_OPTIONS } from "@/constants/translatePriority.js";
import commonParam from "@/constants/commonParam.js";
import { setModalAriaHidden } from "@/utils/domUtils";
import { cloneDeep } from "lodash-es";
export default {
  components: { CustomModal },
  emits: ["handleClose", "submit"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    // 预翻译语种偏好（与回写语种偏好独立缓存）
    const cachedPreTranslateLanguages = localStorage.getItem("preTranslateLanguages");
    return {
      langOptions: Object.values(commonParam.languageMap).map((lang) => ({
        label: lang.name,
        value: lang.name,
      })),
      translatePriorityOptions: TRANSLATE_PRIORITY_OPTIONS,
      preTran: {
        language: cachedPreTranslateLanguages
          ? JSON.parse(cachedPreTranslateLanguages)
          : commonParam.langNameList, // 默认全选或从缓存读取
        priority: "shuyuku",
      },
    };
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        setModalAriaHidden(this, document);
      }
    },
  },
  methods: {
    /**
     * 关闭配置弹窗。
     * @returns {void}
     */
    handleClose() {
      this.$emit("handleClose");
    },
    /**
     * 校验表单并提交预翻译配置。
     * @returns {Promise<void>}
     */
    async handleOK() {
      try {
        await this.$refs.preTranslateForm.validate();
        // 语种偏好缓存
        localStorage.setItem(
          "preTranslateLanguages",
          JSON.stringify(this.preTran.language)
        );
        this.$emit("submit", {
          language: cloneDeep(this.preTran.language),
          priority: this.preTran.priority,
        });
      } catch (err) {
        // 校验失败保持弹窗打开
      }
    },
    /**
     * 关闭后重置优先级。
     * @returns {void}
     */
    afterClose() {
      this.preTran = {
        language: this.preTran.language,
        priority: "shuyuku",
      };
    },
  },
};
</script>
