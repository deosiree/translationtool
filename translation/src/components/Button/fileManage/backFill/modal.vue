<template>
  <a-button v-if="mode === 'button'" type="primary" @click="handleButtonClick" :size="size">
    {{ buttonTitle }}
  </a-button>

  <CustomModal :visible="internalVisible" :okLoading="loading" :modalTitle="modalTitle" @handleClose="handleClose" @handleOK="handleOK">
    <div class="content">
      <a-form ref="backFillForm" :model="formModel">
        <a-form-item v-if="showFileTypeSelect" label="文件类型" name="importType" :rules="[{ required: true, message: '请选择!' }]">
          <a-select v-model:value="formModel.importType" placeholder="请选择文件类型" :options='importTypes' allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="语种" name="language" :rules="[{ required: true, message: '请选择!' }]">
          <a-select mode="multiple" v-model:value="formModel.language" placeholder="请选择语种" :options='translateTypes'
            :fieldNames="{ label: 'name', value: 'name' }" allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="文件" name="backFillFile" :rules="[{ required: true, validator: validateBackFillFile }]">
          <a-upload name="file" :beforeUpload="beforeUpload" :accept="accept" :max-count="1" :fileList="formModel.backFillFileList"
            @change="handleBackFillUpload" @remove="removeBackFillFile"
            :disabled="showFileTypeSelect && (!formModel.importType || !formModel.language || formModel.language.length === 0)">
            <a-button type="primary" size="small">选择</a-button>
          </a-upload>
        </a-form-item>
        <a-form-item v-if="needRelationFile" label="词条映射" name="relationFile" :rules="[{ required: true, validator: validateIdMappingFile }]">
          <a-upload name="file" :beforeUpload="beforeUpload" :accept="idMappingAccept" :max-count="1" :fileList="formModel.relationFileList"
            @change="handleIdMappingUpload" @remove="removeIdMappingFile">
            <a-button type="primary" size="small">
              选择
            </a-button>
          </a-upload>
        </a-form-item>
      </a-form>
    </div>
  </CustomModal>

  <!-- 失败信息模态框 -->
  <CustomModal :visible="failedInfoVisible" :modalTitle="'失败信息'" :showOk="false" @handleClose="failedInfoClose">
    <div class="content">
      <!-- 第一行：显示 globalMessage -->
      <div v-if="globalMessage" style="margin-bottom: 16px; color: #ff4d4f; font-weight: 500;">
        <a-alert :description="globalMessage" type="info" show-icon />
      </div>

      <!-- 部分更新失败的词条下载 -->
      <div v-if="failedEntryInfos && failedEntryInfos.length > 0" style="margin-bottom: 12px;">
        <span style="margin-right: 8px;">部分更新失败的词条：</span>
        <a-button type="link" @click="downloadFailedEntries">下载</a-button>
      </div>

      <!-- 全部异常信息下载 -->
      <div v-if="exceptionVos && exceptionVos.length > 0">
        <span style="margin-right: 8px;">全部异常信息：</span>
        <a-button type="link" @click="downloadExceptionInfos">下载</a-button>
      </div>
    </div>
  </CustomModal>

  <!-- 隐藏的 ExportButton，用于导出失败词条 -->
  <ExportButton ref="failedExportRef" :dataSource="failedExportDataSource" :fieldOptions_="fieldOptions"
    :defaultStatusCheck="false" :hideButton="true" />
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import ExportButton from "@/components/Button/exportButton.vue";
import { entryBatchImportExcel } from "@/utils/excelUtils";
import { downloadJsonFile } from "@/utils/fileUtils";
import { entryParams } from "@/constants/commonParam.js";
import { setModalAriaHidden } from "@/utils/domUtils";
export default {
  components: {
    CustomModal,
    ExportButton,
  },
  emits: ["handleClose", "handleOK", "importSuccess"],
  props: {
    visible: {
      type: Boolean,
      default: false,
      required: false, // 按钮模式下不需要 visible prop
    },
    translateTypes: {
      type: Array,
      default: () => [],
    },
    mode: {
      type: String,
      default: "modal", // 'button' 或 'modal'
    },
    showFileTypeSelect: {
      type: Boolean,
      default: false,
    },
    defaultFileType: {
      type: String,
      default: null, // 'csv' 或 'excel'，可选
    },
    needRelationFile: {
      type: Boolean,
      default: false,
    },
    buttonTitle: {
      type: String,
      default: "导入",
    },
    size: {
      type: String,
      default: "small",
    },
    modalTitle: {
      type: String,
      default: "导入回填",
    },
    defaultAccept: {
      type: String,
      default: null, // 默认 accept 值，由父组件传递（如 ".csv"），不传则为 null
    },
  },
  data() {
    return {
      internalVisible: false, // 内部控制的 visible（按钮模式使用）
      formModel: {
        relationFile: null,
        backFillFile: null,
        language: [],
        relationFileList: [],
        backFillFileList: [],
        importType: null, // 文件类型（csv/excel）
      },
      loading: false,
      idMappingAccept: ".json",
      accept: null, // 回填文件的 accept（由 defaultAccept prop 初始化，当 showFileTypeSelect 为 true 时动态更新）
      importTypes: [
        { label: "csv", value: "csv", accept: ".csv" },
        { label: "excel", value: "excel", accept: ".xls,.xlsx" },
      ],
      // 失败信息相关状态
      failedEntryInfos: [], // 可重试失败词条数组
      exceptionVos: [], // 异常信息数组
      globalMessage: "", // 总体错误提示信息
      failedInfoVisible: false, // 控制失败信息模态框显示
      failedExportDataSource: [], // 用于导出失败词条的数据源
      fieldOptions: entryParams.exportFields,
    };
  },
  watch: {
    visible(newVal) {
      if (this.mode === "modal") {
        this.internalVisible = newVal;
        if (newVal) {
          this.resetForm();
        }
      }
    },
    "formModel.importType": {
      handler(newValue, oldValue) {
        if (newValue !== oldValue && this.showFileTypeSelect) {
          // 文件类型变化时，清空已选择的文件
          this.removeBackFillFile();
          // 如果文件类型为空，重置 accept 为默认值
          if (!newValue) {
            this.accept = this.defaultAccept;
          } else {
            // 如果文件类型有值，根据文件类型更新 accept
            this.updateAccept();
          }
        }
      },
      immediate: false,
    },
  },
  mounted() {
    // 初始化 accept 为父组件传递的默认值
    this.accept = this.defaultAccept;
    // 初始化 defaultFileType
    if (this.defaultFileType && this.showFileTypeSelect) {
      this.formModel.importType = this.defaultFileType;
      this.updateAccept();
    }
    // 同步 visible
    if (this.mode === "modal") {
      this.internalVisible = this.visible;
    } else {
      // 按钮模式下，internalVisible 初始为 false
      this.internalVisible = false;
    }
  },
  methods: {
    // 按钮模式：点击按钮打开模态框
    handleButtonClick() {
      this.internalVisible = true;
      setModalAriaHidden(this, document);
    },
    // 根据文件类型更新 accept（仅在文件类型有值时调用）
    updateAccept() {
      if (!this.formModel.importType) {
        return; // 文件类型为空时不应该调用此方法
      }
      for (let key in this.importTypes) {
        if (this.formModel.importType === this.importTypes[key].value) {
          this.accept = this.importTypes[key].accept;
          break;
        }
      }
    },
    // 通用文件上传处理
    handleFileUpload(info, fileKey, fileListKey) {
      this.formModel[fileListKey] = info.fileList;
      if (info.fileList.length === 0) {
        this.formModel[fileKey] = null;
      } else {
        this.formModel[fileKey] = info.file;
      }
    },

    handleIdMappingUpload(info) {
      this.handleFileUpload(info, "relationFile", "relationFileList");
    },

    handleBackFillUpload(info) {
      this.handleFileUpload(info, "backFillFile", "backFillFileList");
    },

    beforeUpload() {
      return false;
    },

    // 通用文件移除处理
    removeFile(fileKey, fileListKey) {
      this.formModel[fileKey] = null;
      this.formModel[fileListKey] = [];
      return true;
    },

    removeIdMappingFile() {
      return this.removeFile("relationFile", "relationFileList");
    },

    removeBackFillFile() {
      return this.removeFile("backFillFile", "backFillFileList");
    },

    validateIdMappingFile() {
      if (!this.formModel.relationFile) {
        return Promise.reject("请选择 词条映射.json 文件！");
      }
      if (!this.formModel.relationFile.name.endsWith(".json")) {
        return Promise.reject("请选择 .json 格式的文件！");
      }
      return Promise.resolve();
    },

    validateBackFillFile() {
      if (!this.formModel.backFillFile) {
        return Promise.reject("请选择文件！");
      }
      // 如果启用了文件类型选择，根据选择的类型验证文件扩展名
      if (this.showFileTypeSelect && this.formModel.importType) {
        const selectedType = this.importTypes.find(
          (type) => type.value === this.formModel.importType
        );
        if (selectedType) {
          const extensions = selectedType.accept.split(",").map((ext) => ext.trim());
          const fileName = this.formModel.backFillFile.name.toLowerCase();
          const isValid = extensions.some((ext) => fileName.endsWith(ext.replace(".", "")));
          if (!isValid) {
            return Promise.reject(`请选择 ${selectedType.accept} 格式的文件！`);
          }
        }
      } else {
        // 如果没有启用文件类型选择，使用 defaultAccept 验证
        if (this.defaultAccept) {
          const extensions = this.defaultAccept.split(",").map((ext) => ext.trim());
          const fileName = this.formModel.backFillFile.name.toLowerCase();
          const isValid = extensions.some((ext) => fileName.endsWith(ext.replace(".", "")));
          if (!isValid) {
            return Promise.reject(`请选择 ${this.defaultAccept} 格式的文件！`);
          }
        }
        // 如果没有 defaultAccept，不验证文件扩展名（允许任何文件）
      }
      return Promise.resolve();
    },

    async handleOK() {
      if (!this.$refs.backFillForm) return;

      this.$refs.backFillForm
        .validate()
        .then(async () => {
          // validate() 已经验证了所有 rules，无需再次手动验证
          this.$emit("handleOK");

          const formData = new FormData();
          // 始终添加回填文件
          formData.append("file", this.formModel.backFillFile);
          // 仅在 needRelationFile 为 true 时添加映射文件
          if (this.needRelationFile && this.formModel.relationFile) {
            formData.append("relationFile", this.formModel.relationFile);
          }

          this.loading = true;
          try {
            // const res = await entryBackFill({}, formData);
            // if (res.type === "SUCCESS") {
            //     // console.log("回填成功数据", res.data.list);
            //     this.$emit("backFillSuccess", res.data.list);
            //     this.$emit("handleClose");
            // } else {
            //     message.error("回填失败：" + res.message);
            // }

            const result = await entryBatchImportExcel(
              this.formModel.language,
              formData
            );
            console.log("导入结果", result);

            // 按钮模式需要 emit importSuccess 事件
            if (this.mode === "button") {
              this.$emit("importSuccess");
            }

            // 处理结果
            if (result.code === 201) {
              // code=201 表示有失败信息，显示失败信息模态框
              // 保存失败信息
              this.failedEntryInfos = result.failedEntryInfos || [];
              this.exceptionVos = result.exceptionVos || [];
              this.globalMessage = result.globalMessage || "";

              // 提取失败词条数据用于导出（如果有数据）
              if (this.failedEntryInfos.length > 0) {
                this.extractFailedEntriesData();
              }

              // 关闭导入回填弹窗，打开失败信息模态框
              this.handleCloseInternal();
              this.failedInfoVisible = true;
            } else if (result.code === 200) {
              // 完全成功
              this.handleCloseInternal();
              if (this.mode === "modal") {
                message.success("回填成功！");
              }
            } else {
              // 其他情况
              this.handleCloseInternal();
            }
          } catch (error) {
            console.error("导入过程发生异常：", error);
            message.error("导入失败：" + (error.message || "未知错误"));
          } finally {
            this.loading = false;
          }
        })
        .catch((err) => {
          console.log("表单校验失败", err);
        });
    },

    // 内部关闭处理（不重置表单）
    handleCloseInternal() {
      if (this.mode === "button") {
        this.internalVisible = false;
      } else {
        this.$emit("handleClose");
      }
    },

    // 关闭处理（重置表单）
    handleClose() {
      this.resetForm();
      this.handleCloseInternal();
    },
    resetForm() {
      // 重置 importType：如果有 defaultFileType 则使用默认值，否则为 null
      const importType = this.defaultFileType && this.showFileTypeSelect ? this.defaultFileType : null;
      
      this.formModel = {
        relationFile: null,
        backFillFile: null,
        language: [],
        relationFileList: [],
        backFillFileList: [],
        importType: importType,
      };
      this.loading = false;
      // 重置 accept
      this.accept = this.defaultAccept; // 重置为父组件传递的默认值
      if (importType && this.showFileTypeSelect) {
        this.updateAccept(); // 如果有默认文件类型，根据它更新 accept
      }
      // 重置失败信息相关状态
      this.failedEntryInfos = [];
      this.exceptionVos = [];
      this.globalMessage = "";
      this.failedInfoVisible = false;
      this.failedExportDataSource = [];
      if (this.$refs.backFillForm) {
        this.$refs.backFillForm.clearValidate();
      }
    },
    // 关闭失败信息模态框
    failedInfoClose() {
      this.failedInfoVisible = false;
      // 重置失败信息相关状态
      this.failedEntryInfos = [];
      this.exceptionVos = [];
      this.globalMessage = "";
      this.failedExportDataSource = [];
    },
    // 提取失败词条数据用于导出
    extractFailedEntriesData() {
      const entriesMap = new Map();

      // 遍历 failedEntryInfos，提取所有词条数据
      this.failedEntryInfos.forEach((item) => {
        // 兼容两种数据结构：
        // 1. 直接是词条对象：{id: 1, entry: "...", ...}
        // 2. 包含 entryInfoVO 的对象：{entryInfoVO: {entryInfoEntitie: [...]}}
        if (item.id) {
          // 情况1：直接是词条对象
          if (!entriesMap.has(item.id)) {
            entriesMap.set(item.id, item);
          }
        } else if (item.entryInfoVO) {
          // 情况2：包含 entryInfoVO
          const entryInfoVO = item.entryInfoVO || {};
          const entryList = entryInfoVO.entryInfoEntitie || entryInfoVO.entryInfoEntities || [];

          entryList.forEach((entry) => {
            // 使用 id 作为 key 去重
            if (entry.id && !entriesMap.has(entry.id)) {
              entriesMap.set(entry.id, entry);
            }
          });
        }
      });

      this.failedExportDataSource = Array.from(entriesMap.values());
    },
    // 下载部分更新失败的词条
    downloadFailedEntries() {
      if (this.failedExportDataSource.length === 0) {
        message.warning("没有可导出的失败词条数据");
        return;
      }

      // 通过 ref 调用隐藏的 ExportButton 的 showExportModal 方法
      if (this.$refs.failedExportRef) {
        this.$refs.failedExportRef.showExportModal();
      } else {
        message.error("导出功能初始化失败");
      }
    },
    // 下载全部异常信息
    downloadExceptionInfos() {
      if (this.exceptionVos.length === 0) {
        message.warning("没有异常信息可下载");
        return;
      }

      downloadJsonFile(this.exceptionVos, "entry_import_exceptions", false);
    },
  },
};
</script>

<style scoped lang="less">
.content {
  padding: 20px;
}
</style>
