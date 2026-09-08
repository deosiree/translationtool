<template>
  <!-- 预翻译：词条管理-已选词条-预翻译配置弹窗（语种多选 + 翻译优先级 + 校验规则） -->
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
        <a-form-item label="校验规则" name="rules">
          <RulesDropdown :options="rulesOptions" @update:options="rulesOptions = $event" />
          <a-tooltip placement="top">
            <template #title>
              <span>预翻译返回后按勾选规则校验译文：通过的直接写入对应语种列；不通过的保持编辑态并标红</span>
            </template>
            <QuestionCircleOutlined style="color:#00000066;float:right;margin-top:3px" />
          </a-tooltip>
        </a-form-item>
      </a-form>
    </div>
  </CustomModal>
</template>
<script>
import CustomModal from "@/components/modal/index.vue";
import RulesDropdown from "@/components/Dropdown/rulesDropdown.vue";
import { QuestionCircleOutlined } from "@ant-design/icons-vue";
import { TRANSLATE_PRIORITY_OPTIONS } from "@/constants/translatePriority.js";
import commonParam from "@/constants/commonParam.js";
import { setModalAriaHidden } from "@/utils/domUtils";
import { cloneDeep } from "lodash-es";
export default {
  components: { CustomModal, RulesDropdown, QuestionCircleOutlined },
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
      // 深拷贝一份局部规则，避免与工作台全局 rulesOptions 共享引用互相污染
      rulesOptions: commonParam.rulesOptions.map((item) => ({ ...item })),
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
    handleClose() {
      this.$emit("handleClose");
    },
    async handleOK() {
      try {
        await this.$refs.preTranslateForm.validate();
        // 语种偏好缓存；勾选的校验规则 key 列表一并提交
        localStorage.setItem(
          "preTranslateLanguages",
          JSON.stringify(this.preTran.language)
        );
        this.$emit("submit", {
          language: cloneDeep(this.preTran.language),
          priority: this.preTran.priority,
          verifyMethods: this.rulesOptions
            .filter((item) => item.checked)
            .map((item) => item.key),
        });
      } catch (err) {
        // 校验失败保持弹窗打开
      }
    },
    afterClose() {
      this.preTran = {
        language: this.preTran.language,
        priority: "shuyuku",
      };
    },
  },
};
</script>
