<template>
  <a-button type="primary" @click="importEntry" :size="size">{{ buttonTitle }}</a-button>

  <CustomModal :visible="importVisible" :okLoading="importLoading" :modalTitle="buttonTitle" @handleClose="importClose" @handleOK="importOK"
    @afterClose="importAfterClose">
    <div class="content">
      <a-form ref="importForm" :model="importModal">
        <a-form-item label="文件类型" name="importType" :rules="[{ required: true, message: '请选择!' }]">
          <a-select v-model:value="importModal.importType" placeholder="请选择文件类型" :options='importTypes' allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="语言" name="language" :rules="[{ required: true, message: '请选择!' }]">
          <a-select mode="multiple" v-model:value="importModal.language" placeholder="请选择语言" :options='translateTypes'
            :fieldNames="{label:'name',value:'name'}" allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="文件" name="file" :rules="[{required: true, validator: this.checkFile() }]">
          <a-upload name="file" :beforeUpload="beforeUpload" :accept="accept" :max-count="1" :fileList="fileList" @change="handleChange"
            @remove="removeFile" :disabled="!importModal.importType||(!this.importModal.language || this.importModal.language.length == 0)">
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
    buttonTitle: {
      type: String,
      default: "导入", // 设置默认值为 "导入"
    },
  },
  data() {
    return {
      importVisible: false,
      importLoading: false,
      importModal: {
        importFile: null,
        language: [],
        importType: null,
      },
      importTypes: [
        { label: "csv", value: "csv", accept: ".csv" },
        { label: "excel", value: "excel", accept: ".xls,.xlsx" },
      ],
      accept: null,
      fileList: [],
    };
  },
  watch: {
    "importModal.importType": {
      handler(newValue, oldValue) {
        if (newValue !== oldValue) {
          this.accept = null;
          this.removeFile(); // 切换文件类型后清空文件
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
      if (!this.$refs.importForm) return;
      this.$refs.importForm
        .validate()
        .then(async () => {
          this.importLoading = true;
          const formData = new FormData();
          const promises = [];
          const msg = { success: [], failed: [] };
          formData.append("file", this.importModal.importFile);
          // console.log("formData", formData);
          // console.log("file", this.importModal.importFile);
          // console.log("transType", this.importModal.language);
          // console.log("importType", this.importModal.importType);
          // // 使用 entries() 方法查看 FormData 的属性
          // for (const [key, value] of formData.entries()) {
          //   console.log(`${key}: ${value}`);
          // }
          function entryImportFn(lang, formData) {
            // 每种翻译语言的导入
            const params = {
              transType: lang,
              // importType: this.importModal.importType,// 后端不需要这个参数
            };
            return entryImportExcle(params, formData)
              .then((res) => {
                msg.success.push(lang);
              })
              .catch((err) => {
                console.log("导入失败原因", err);
                msg.success.push(lang);
              });
          }

          for (const lang of this.importModal.language) {
            promises.push(entryImportFn(lang, formData));
          }
          await Promise.allSettled(promises).then(() => {
            this.$emit("importSuccess");
            if (msg.success.length > 0) {
              message.success(msg.success.join("，") + "导入成功！", 3);
              // console.log(msg.success.join(","), "!");
            }
            if (msg.failed.length > 0) {
              message.error(
                msg.failed.join("，"),
                `导入失败！注意，请使用在词条管理中导出的文件进行导入。`,
                3
              );
            } else {
              this.importVisible = false;
            }
          });
        })
        .catch((err) => {
          // console.log("未选择文件", err, this.importModal.language);
          message.error("未选择文件", err);
        })
        .finally(() => {
          this.importLoading = false;
        });
    },
    // 导入模态框关闭后回调
    importAfterClose() {
      this.importModal = {
        importFile: null,
        language: [],
        importType: null,
      };
      this.fileList = [];
      if (this.$refs.importForm) {
        this.$refs.importForm.clearValidate();
      }
    },
    // 导入词条(在文件开始上传之前阻止文件上传操作)
    beforeUpload(file, fileList) {
      // console.log("before");
      return false;
    },
    // 文件变化处理
    handleChange(info) {
      this.fileList = info.fileList; // max-count=1,一个文件一个文件地上传
      if (info.fileList.length === 0) {
        this.importModal.importFile = null;
      } else {
        this.importModal.importFile = info.file;
      }
    },
    // 移除文件
    removeFile(file) {
      this.importModal.importFile = null;
      this.fileList = [];
      return true;
    },
    // 获得导入文件类型
    getAccept() {
      if (!this.importModal.importType) {
        message.error("请选择文件类型！");
        return;
      }
      if (!this.importModal.language || this.importModal.language.length == 0) {
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
        if (!this.importModal.importFile) {
          return Promise.reject("请选择文件！");
        }
        return Promise.resolve();
      };
    },
  },
};
</script>