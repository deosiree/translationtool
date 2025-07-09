<template>
  <a-button type="primary" @click="importEntry" :size="size">导入</a-button>

  <CustomModal :visible="importVisible" :okLoading="importLoading" modalTitle="导入" @handleClose="importClose" @handleOK="importOK"
    @afterClose="importAfterClose">
    <div class="content">
      <a-form ref="formRef" :model="importModal">
        <a-form-item label="文件类型" name="importType" :rules="[{ required: true, message: '请选择!' }]">
          <a-select v-model:value="importModal.importType" placeholder="请选择文件类型" :options='importTypes' allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="语言" name="language" :rules="[{ required: true, message: '请选择!' }]">
          <a-select v-model:value="importModal.language" placeholder="请选择语言" :options='translateTypes' :fieldNames="{label:'name',value:'name'}"
            allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="文件" name="file" :rules="[{required: true, validator: this.checkFile() }]">
          <a-upload name="file" :accept="accept" :max-count="1" :fileList="fileList" @change="handleChange" @remove="removeFile"
            :disabled="!importModal.language || !importModal.importType">
            <a-button type="primary" size="small" @click="getAccept">选择</a-button>
          </a-upload>
        </a-form-item>
      </a-form>
    </div>
  </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import { entryImportExcle } from "@/http/api/entryManage";
import { setModalAriaHidden } from "@/utils/commonUtils";
export default {
  components: {
    CustomModal, // 注册 CustomModal 组件
  },
  emits: ["importSuccess"],
  props: {
    translateTypes: {
      type: Array,
      default: () => [],
    },
    size: {
      type: String,
      default: "small",
    },
  },
  data() {
    return {
      accept: null,
      importVisible: false,
      importLoading: false,
      importModal: {
        language: null,
        importType: null,
      },
      importTypes: [
        { label: "csv", value: "csv", accept: ".csv" },
        { label: "excel", value: "excel", accept: ".xls,.xlsx" },
      ],
      fileList: [],
      importFile: null,
    };
  },
  watch: {
    "importModal.importType": {
      handler(newValue, oldValue) {
        if (newValue !== oldValue) {
          console.log("newValue", newValue);
          console.log("oldValue", oldValue);
          this.removeFile();
        }
      },
      immediate: false,
    },
  },
  methods: {
    // 导入词条
    importEntry() {
      this.importVisible = true;
      setModalAriaHidden(this, document);
    },
    // 关闭导入模态框
    importClose() {
      this.importVisible = false;
    },
    // 确认导入
    importOK() {
      if (!this.$refs.formRef) return;
      this.$refs.formRef
        .validate()
        .then(() => {
          this.importLoading = true;
          const formData = new FormData();
          formData.append("file", this.importFile);
          formData.append("transType", this.importModal.language);
          formData.append("importType", this.importModal.importType);

          console.log("formData", formData);
          console.log("file", this.importFile);
          console.log("transType", this.importModal.language);
          console.log("importType", this.importModal.importType);
          // 使用 entries() 方法查看 FormData 的属性
          for (const [key, value] of formData.entries()) {
            console.log(`${key}: ${value}`);
          }

          entryImportExcle(formData)
            .then((res) => {
              message.success("导入成功！");
              this.$emit("importSuccess");
              this.importVisible = false;
              this.importLoading = false;
            })
            .catch((err) => {
              message.error(`导入失败！${err.message}`);
              this.importLoading = false;
            });
        })
        .catch((err) => {
          message.error("未选择文件", err.message);
        });
    },
    // 导入模态框关闭后回调
    importAfterClose() {
      this.importModal.language = null;
      this.importFile = null;
      this.fileList = [];
      if (this.$refs.formRef) {
        this.$refs.formRef.clearValidate();
      }
    },
    // 文件变化处理
    handleChange(info) {
      this.fileList = info.fileList;
      if (info.fileList.length === 0) {
        this.importFile = null;
      } else {
        this.importFile = info.file;
      }
    },
    // 移除文件
    removeFile(file) {
      this.importFile = null;
      return true;
    },
    // 获得导入文件类型
    getAccept() {
      if (!this.importModal.importType) {
        message.error("请选择文件类型！");
        return;
      }
      if (!this.importModal.language) {
        message.error("请选择语言！");
        return;
      }
      for (let key in this.importTypes) {
        if (this.importModal.importType === this.importTypes[key].value) {
          this.accept = this.importTypes[key].accept;
          break;
        }
      }
    },
    // 校验上传文件是否为空
    checkFile() {
      return (rule, value) => {
        if (!this.importFile) {
          return Promise.reject("请选择文件！");
        }
        return Promise.resolve();
      };
    },
  },
};
</script>