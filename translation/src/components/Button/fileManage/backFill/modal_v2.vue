<template>
  <a-button v-if="mode === 'button'" type="primary" @click="handleButtonClick" :size="size">
    {{ buttonTitle }}
  </a-button>

  <CustomModal :visible="internalVisible" :okLoading="loading" :modalTitle="modalTitle" @handleClose="handleClose"
    @handleOK="handleOK">
    <div class="content">
      <a-form ref="backFillForm" :model="formModel">
        <!-- 共同部分：文件类型 -->
        <a-form-item v-if="showFileTypeSelect" label="文件类型" name="importType"
          :rules="[{ required: true, message: '请选择!' }]">
          <a-select v-model:value="formModel.importType" placeholder="请选择文件类型" :options='importTypes' allowClear>
          </a-select>
        </a-form-item>
        
        <!-- 共同部分：更新字段（回填字段） -->
        <a-form-item label="更新字段" name="backfillFields" :rules="[{ required: true, message: '请选择!' }]">
          <div style="display: flex; justify-content: space-between;">
            <a-select mode="multiple" v-model:value="formModel.backfillFields" :options="fieldOptions"
              :fieldNames="{ label: 'label', value: 'label' }" placeholder="请选择更新字段"
              allowClear style="flex: 1; margin-right: 8px;" />
            <a-button type="link" size="small" @click="selectAllBackfillFields" style="
              font-size: smaller;margin-top:0">全选</a-button>
          </div>
        </a-form-item>

        <!-- 校验模式专用：去重前Excel（只有校验时才需要，用于和去重后Excel+映射文件生成的总文件比较） -->
        <a-form-item v-if="formModel.enableValidate" label="去重前Excel" name="originExcel" :rules="[{ required: true, validator: validateOriginExcel }]">
          <a-upload name="file" :beforeUpload="beforeUpload" :accept="backFillAccept" :max-count="1"
            :fileList="formModel.originExcelList" @change="handleOriginExcelUpload" @remove="removeOriginExcel">
            <a-button type="primary" size="small" @click="handleOriginExcelChooseClick">选择</a-button>
          </a-upload>
        </a-form-item>

        <!-- 共同部分：去重后Excel（待回填文件） -->
        <a-form-item label="去重后Excel" name="dedupExcel" :rules="[{ required: true, validator: validateBackFillFile }]">
          <a-upload name="file" :beforeUpload="beforeUpload" :accept="backFillAccept" :max-count="1"
            :fileList="formModel.backFillFileList" @change="handleBackFillUpload" @remove="removeBackFillFile">
            <a-button type="primary" size="small" @click="handleBackFillChooseClick">选择</a-button>
          </a-upload>
        </a-form-item>

        <!-- 共同部分：词条映射 -->
        <a-form-item v-if="needRelationFile" label="词条映射" name="relationFile"
          :rules="[{ required: true, validator: validateIdMappingFile }]">
          <a-upload name="file" :beforeUpload="beforeUpload" :accept="idMappingAccept" :max-count="1"
            :fileList="formModel.relationFileList" @change="handleIdMappingUpload" @remove="removeIdMappingFile">
            <a-button type="primary" size="small">
              选择
            </a-button>
          </a-upload>
        </a-form-item>

        <!-- 共同部分：勾选框（flex布局，尽量一行，摆不下自动换行） -->
        <a-form-item label="选项">
          <a-row style="width:100%;display:flex;gap:8px;flex-wrap:wrap;">
            <a-checkbox v-model:checked="formModel.emptyStringAsValue">导入空值</a-checkbox>
            <a-checkbox v-model:checked="formModel.failFast">失败中止</a-checkbox>
            <a-checkbox v-model:checked="formModel.enableValidate">校验</a-checkbox>
          </a-row>
        </a-form-item>

        <!-- 校验模式专用：勾选了"校验"后才显示 -->
        <template v-if="formModel.enableValidate">
          <!-- 校验字段 -->
          <a-form-item label="校验字段" name="checkFields" :rules="formModel.enableValidate ? [{ required: true, message: '请选择!' }] : []">
            <a-select mode="multiple" v-model:value="formModel.checkFields" :options="checkFieldOptions"
              placeholder="请选择校验字段" allowClear />
          </a-form-item>
          
          <!-- 校验选项勾选框（flex布局，尽量一行，摆不下自动换行） -->
          <a-form-item label="校验选项">
            <a-row style="width:100%;display:flex;gap:8px;flex-wrap:wrap;">
              <a-checkbox v-model:checked="formModel.checkSpecialChar">checkSpecialChar</a-checkbox>
              <a-checkbox v-model:checked="formModel.checkMaxLength">checkMaxLength</a-checkbox>
            </a-row>
          </a-form-item>
        </template>
      </a-form>
    </div>
  </CustomModal>

  <!-- 失败信息模态框 -->
  <CustomModal :visible="failedInfoVisible" :modalTitle="formModel.enableValidate ? '校验结果' : '失败信息'" :showOk="false" @handleClose="failedInfoClose">
    <div class="content">
      <!-- 第一行：显示 globalMessage -->
      <div v-if="globalMessage" style="margin-bottom: 16px; color: #ff4d4f; font-weight: 500;">
        <a-alert :description="globalMessage" type="info" show-icon />
      </div>

      <!-- 校验模式：显示summary信息 -->
      <div v-if="formModel.enableValidate && validationSummary" style="margin-bottom: 16px;">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="去重前行数">{{ validationSummary.totalOriginRows || 0 }}</a-descriptions-item>
          <a-descriptions-item label="去重后行数">{{ validationSummary.totalDedupRows || 0 }}</a-descriptions-item>
          <a-descriptions-item label="受影响行数">{{ validationSummary.affectedRows || 0 }}</a-descriptions-item>
          <a-descriptions-item label="将更新单元格数">{{ validationSummary.willUpdateCells || 0 }}</a-descriptions-item>
        </a-descriptions>
      </div>

      <!-- 校验模式：显示issues列表 -->
      <div v-if="formModel.enableValidate && validationIssues && validationIssues.length > 0" style="margin-bottom: 16px;">
        <div style="margin-bottom: 8px; font-weight: 500;">校验问题列表：</div>
        <a-table 
          :columns="issueColumns" 
          :dataSource="validationIssues" 
          :pagination="{ pageSize: 10 }"
          size="small"
          :scroll="{ y: 200 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'level'">
              <a-tag :color="record.level === 'FATAL' ? 'red' : record.level === 'WARN' ? 'orange' : 'blue'">
                {{ record.level }}
              </a-tag>
            </template>
          </template>
        </a-table>
        <div style="margin-top: 8px;">
          <a-button type="link" @click="downloadValidationIssues">下载校验问题JSON</a-button>
        </div>
      </div>

      <!-- 校验模式：显示attachments下载 -->
      <div v-if="formModel.enableValidate && validationAttachments" style="margin-bottom: 16px;">
        <div style="margin-bottom: 8px; font-weight: 500;">校验附件：</div>
        <div v-if="validationAttachments.invalidExcel" style="margin-bottom: 8px;">
          <span style="margin-right: 8px;">{{ validationAttachments.invalidExcel.fileName }}：</span>
          <a-button type="link" @click="downloadAttachment(validationAttachments.invalidExcel.downloadUrl, validationAttachments.invalidExcel.fileName)">下载</a-button>
        </div>
        <div v-if="validationAttachments.issueLog">
          <span style="margin-right: 8px;">{{ validationAttachments.issueLog.fileName }}：</span>
          <a-button type="link" @click="downloadAttachment(validationAttachments.issueLog.downloadUrl, validationAttachments.issueLog.fileName)">下载</a-button>
        </div>
      </div>

      <!-- 导入模式：部分更新失败的词条下载 -->
      <div v-if="!formModel.enableValidate && failedEntryInfos && failedEntryInfos.length > 0" style="margin-bottom: 12px;">
        <span style="margin-right: 8px;">部分更新失败的词条：</span>
        <a-button type="link" @click="downloadFailedEntries">下载</a-button>
      </div>

      <!-- 导入模式：全部异常信息下载 -->
      <div v-if="!formModel.enableValidate && exceptionVos && exceptionVos.length > 0">
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
import { message, notification } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import ExportButton from "@/components/Button/exportButton.vue";
import { entryBatchImportExcel_v2, entryValidate_v2 } from "@/utils/excelUtils";
import { downloadJsonFile, downloadBlobResponse } from "@/utils/fileUtils";
import { downloadFileFromUrl } from "@/http/api/download";
import { entryParams } from "@/constants/commonParam.js";
import { setModalAriaHidden, stopDomEvent } from "@/utils/domUtils";
import {
  queryUserPartiality,
  updateUserPartiality,
} from "@/http/api/userPartiality";
export default {
  name: "BackFillModal_v2",
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
    functionMode: {
      type: String,
      default: "updateTranslation", // 'updateTranslation' 或 'validate'
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
      default: "去重回填",
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
        // 新API通用字段（两种模式都需要）
        originExcel: null,        // 去重前Excel
        originExcelList: [],      // 去重前Excel文件列表
        // 选项勾选框
        emptyStringAsValue: true,  // 导入空值
        failFast: false,          // 失败中止
        enableValidate: false,    // 校验（勾选后才显示校验相关字段）
        // 校验模式专用字段（勾选了enableValidate后才使用）
        checkFields: [],          // 校验字段（如["entry", "comment", "tag"]）
        checkSpecialChar: false,  // 特殊字符校验
        checkMaxLength: false,    // 长度校验
      },
      loading: false,
      idMappingAccept: ".json",
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
      fieldOptions: [], // 初始值，会在mounted中从entryParams.exportFields更新并过滤
      // 校验字段选项（硬编码，通常为["entry", "comment", "tag"]）
      checkFieldOptions: [
        { label: "entry", value: "entry" },
        { label: "comment", value: "comment" },
        { label: "tag", value: "tag" },
      ],
      // 校验结果相关状态
      validationSummary: null, // 校验摘要信息
      validationIssues: [], // 校验问题列表
      validationAttachments: null, // 校验附件信息
      // issues表格列定义
      issueColumns: [
        { title: '级别', dataIndex: 'level', key: 'level', width: 80 },
        { title: '类型', dataIndex: 'type', key: 'type', width: 150 },
        { title: '词条ID', dataIndex: 'id', key: 'id', width: 120 },
        { title: '字段', dataIndex: 'fieldKey', key: 'fieldKey', width: 120 },
        { title: '描述', dataIndex: 'message', key: 'message' },
      ],
    };
  },
  computed: {
    // 回填文件的 accept：以 importType 为准（有选择器时），否则使用 defaultAccept
    backFillAccept() {
      if (this.showFileTypeSelect) {
        if (!this.formModel.importType) return null;
        const selectedType = this.importTypes.find(
          (type) => type.value === this.formModel.importType
        );
        return selectedType ? selectedType.accept : null;
      }
      return this.defaultAccept;
    },
  },
    watch: {
    visible(newVal) {
      if (this.mode === "modal") {
        this.internalVisible = newVal;
        if (newVal) {
          this.resetForm();
          // 获取用户偏好
          this.queryBackfillFieldsPreference();
        }
      }
    },
    "formModel.importType": {
      handler(newValue, oldValue) {
        if (newValue !== oldValue && this.showFileTypeSelect) {
          // 文件类型变化时，清空已选择的文件
          this.removeBackFillFile();
        }
      },
      immediate: false,
    },
  },
  mounted() {
    // 初始化 defaultFileType
    if (this.defaultFileType && this.showFileTypeSelect) {
      this.formModel.importType = this.defaultFileType;
    }
    // 根据functionMode初始化enableValidate（向后兼容）
    if (this.functionMode === "validate") {
      this.formModel.enableValidate = true;
    }
    // 同步 visible
    if (this.mode === "modal") {
      this.internalVisible = this.visible;
      // 如果初始 visible 为 true，也需要获取用户偏好（因为 watch 只在值变化时触发）
      if (this.visible) {
        this.resetForm();
        this.queryBackfillFieldsPreference();
      }
    } else {
      // 按钮模式下，internalVisible 初始为 false
      this.internalVisible = false;
    }
    // 初始化字段选项：过滤掉不能更新的字段（如翻译id字段）
    this.$nextTick(() => {
      const filterColNames = [
        "英文翻译id",
        "俄文翻译id",
        "西文翻译id",
        "法文翻译id",
        "中文翻译id",
      ];
      const sourceOptions = entryParams && entryParams.exportFields ? entryParams.exportFields : [];
      this.fieldOptions = sourceOptions.filter(
        (item) => !filterColNames.includes(item.label)
      );
    });
  },
  methods: {
    // 按钮模式：点击按钮打开模态框
    handleButtonClick() {
      this.internalVisible = true;
      setModalAriaHidden(this, document);
      // 获取用户偏好
      this.queryBackfillFieldsPreference();
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

    handleOriginExcelUpload(info) {
      this.handleFileUpload(info, "originExcel", "originExcelList");
    },

    handleOriginExcelChooseClick(e) {
      // accept 未就绪时拦截，避免弹出系统文件选择框
      if (!this.backFillAccept) {
        message.warning("请先选择文件类型");
        stopDomEvent(e);
      }
    },

    handleBackFillChooseClick(e) {
      // accept 未就绪时拦截，避免弹出系统文件选择框
      if (!this.backFillAccept) {
        message.warning("请先选择文件类型");
        // 使用通用 DOM 工具函数统一处理默认行为和冒泡
        stopDomEvent(e);
      }
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

    removeOriginExcel() {
      return this.removeFile("originExcel", "originExcelList");
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
      const file = this.formModel.backFillFile;
      if (!file) {
        return Promise.reject("请选择文件！");
      }
      return this.validateFileExtension(file);
    },

    validateOriginExcel() {
      const file = this.formModel.originExcel;
      if (!file) {
        return Promise.reject("请选择去重前Excel文件！");
      }
      return this.validateFileExtension(file);
    },

    // 通用文件扩展名验证方法
    validateFileExtension(file) {
      // 如果启用了文件类型选择，根据选择的类型验证文件扩展名
      if (this.showFileTypeSelect && this.formModel.importType) {
        const selectedType = this.importTypes.find(
          (type) => type.value === this.formModel.importType
        );
        if (selectedType) {
          const extensions = selectedType.accept.split(",").map((ext) => ext.trim());
          const fileName = file.name.toLowerCase();
          const isValid = extensions.some((ext) => fileName.endsWith(ext.replace(".", "")));
          if (!isValid) {
            return Promise.reject(`请选择 ${selectedType.accept} 格式的文件！`);
          }
        }
      } else {
        // 如果没有启用文件类型选择，使用 defaultAccept 验证
        if (this.defaultAccept) {
          const extensions = this.defaultAccept.split(",").map((ext) => ext.trim());
          const fileName = file.name.toLowerCase();
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

          this.loading = true;
          try {
            let result;
            // 根据是否勾选"校验"选择不同的API
            if (this.formModel.enableValidate) {
              // 校验模式
              result = await entryValidate_v2(
                this.formModel.originExcel,
                this.formModel.backFillFile,
                this.needRelationFile ? this.formModel.relationFile : null,
                this.formModel.checkFields,
                this.formModel.backfillFields,
                {
                  emptyStringAsValue: this.formModel.emptyStringAsValue,
                  failFast: this.formModel.failFast,
                  checkSpecialChar: this.formModel.checkSpecialChar,
                  checkMaxLength: this.formModel.checkMaxLength
                }
              );
              
              // 输出完整响应体用于调试
              console.log("=== 校验接口完整响应体 ===");
              console.log("原始响应 result:", result);
              console.log("响应类型:", typeof result);
              console.log("响应结构:", JSON.stringify(result, null, 2));
              console.log("result.code:", result?.code);
              console.log("result.message:", result?.message);
              console.log("result.data:", result?.data);
              // 处理两种情况：完整响应（有code）或data部分（有success）
              if (result?.code && result?.data) {
                // 完整响应格式
                console.log("响应格式: 完整响应（包含 code 和 data）");
                console.log("result.data.success:", result.data.success);
                console.log("result.data.canBackfill:", result.data.canBackfill);
                console.log("result.data.summary:", result.data.summary);
                console.log("result.data.issues:", result.data.issues);
                console.log("result.data.preview:", result.data.preview);
                console.log("result.data.attachments:", result.data.attachments);
              } else if (result?.success !== undefined) {
                // data 部分格式
                console.log("响应格式: data 部分（entryValidate_v2 返回了 response.data）");
                console.log("result.success:", result.success);
                console.log("result.canBackfill:", result.canBackfill);
                console.log("result.summary:", result.summary);
                console.log("result.issues:", result.issues);
                console.log("result.preview:", result.preview);
                console.log("result.attachments:", result.attachments);
              }
              console.log("===========================");
              
              // 处理校验结果
              this.handleValidationResponse(result);
            } else {
              // updateTranslation 模式：只包含backfillFields任务
              // 注意：更新接口不需要去重前Excel，后端只需要去重后Excel和映射文件就能回填
              result = await entryBatchImportExcel_v2(
                this.formModel.backFillFile,
                this.needRelationFile ? this.formModel.relationFile : null,
                this.formModel.backfillFields,
                {
                  emptyStringAsValue: this.formModel.emptyStringAsValue
                }
              );
              // 保存用户偏好
              this.updateBackfillFieldsPreference(this.formModel.backfillFields);
              
              // 输出完整响应体用于调试
              console.log("=== 导入接口完整响应体 ===");
              console.log("原始响应 result:", result);
              console.log("响应类型:", typeof result);
              console.log("响应结构:", JSON.stringify(result, null, 2));
              console.log("result.code:", result?.code);
              console.log("result.success:", result?.success);
              console.log("result.data:", result?.data);
              console.log("===========================");
              
              // 处理导入结果
              this.handleImportResponse(result);
            }

            console.log("操作结果", result);

            // 按钮模式需要 emit importSuccess 事件
            if (this.mode === "button") {
              this.$emit("importSuccess");
            }

            // 先关闭当前弹窗
            this.handleCloseInternal();
          } catch (error) {
            console.error("操作过程发生异常：", error);
            // 尝试从error中提取响应数据
            const errorData = error?.response?.data || error?.data || error;
            
            if (this.formModel.enableValidate) {
              // 校验模式：尝试处理错误响应
              if (errorData && (errorData.success !== undefined || errorData.canBackfill !== undefined)) {
                this.handleValidationResponse(errorData);
                this.handleCloseInternal();
              } else {
                notification.error({
                  message: "校验过程发生异常！",
                  description: errorData?.message || error.message || "未知错误",
                  duration: 0,
                });
              }
            } else {
              // 导入模式：尝试处理错误响应
              if (errorData && errorData.code) {
                this.handleImportResponse(errorData);
                this.handleCloseInternal();
              } else {
                notification.error({
                  message: "导入过程发生异常！",
                  description: errorData?.message || error.message || "未知错误",
                  duration: 0,
                });
              }
            }
          } finally {
            this.loading = false;
          }
        })
        .catch((err) => {
          console.log("表单校验失败", err);
        });
    },

    // 处理校验响应
    handleValidationResponse(result) {
      if (result.success && result.canBackfill) {
        // 校验通过，可回填
        this.validationSummary = result.summary || null;
        this.validationIssues = result.issues || [];
        this.validationAttachments = result.attachments || null;
        
        notification.success({
          message: "校验通过",
          description: `共校验 ${result.summary?.totalOriginRows || 0} 条数据，${result.summary?.affectedRows || 0} 条将受影响`,
          duration: 0,
        });
        
        // 如果有issues或attachments，显示失败信息模态框
        if (this.validationIssues.length > 0 || this.validationAttachments) {
          this.globalMessage = "校验通过，但存在警告信息";
          this.failedInfoVisible = true;
        }
      } else if (result.success && !result.canBackfill) {
        // 校验通过但不可回填（有WARN级别问题）
        this.handleValidationWarnings(result);
      } else {
        // 校验失败（有FATAL级别错误）
        this.handleValidationErrors(result);
      }
    },

    // 处理校验警告
    handleValidationWarnings(result) {
      this.validationSummary = result.summary || null;
      this.validationIssues = result.issues || [];
      this.validationAttachments = result.attachments || null;
      this.globalMessage = "校验完成，但存在警告，不允许回填";
      this.failedInfoVisible = true;
    },

    // 处理校验错误
    handleValidationErrors(result) {
      this.validationSummary = result.summary || null;
      this.validationIssues = result.issues || [];
      this.validationAttachments = result.attachments || null;
      this.globalMessage = result.issues?.[0]?.message || "校验失败，存在致命错误";
      this.failedInfoVisible = true;
      
      notification.error({
        message: "校验失败",
        description: this.globalMessage,
        duration: 0,
      });
    },

    // 处理导入响应
    handleImportResponse(result) {
      let notifyTask = null;

      if (result.code === 200) {
        // 完全成功：仅展示成功通知
        const successLangs = result.success || [];
        const desc =
          successLangs.length > 0
            ? `${successLangs.join(", ")} 导入成功！`
            : "导入成功！";
        notifyTask = () => {
          notification.success({
            message: "导入成功！",
            description: desc,
            duration: 0,
          });
        };
      } else if (result.code === 201) {
        // code 201 时，失败信息在 result.data 中
        const data = result.data || {};
        const failedEntryInfos = data.failedEntryInfos || [];
        const exceptionVos = data.exceptionVos || [];
        const globalMessage = data.globalMessage || "";
        
        if (failedEntryInfos.length > 0) {
          // 有具体的失败词条信息：使用失败信息模态框展示
          this.failedEntryInfos = failedEntryInfos;
          this.exceptionVos = exceptionVos;
          this.globalMessage = globalMessage;

          // 提取失败词条数据用于导出（如果有数据）
          if (this.failedEntryInfos.length > 0) {
            this.extractFailedEntriesData();
          }

          this.failedInfoVisible = true;
        } else {
          // 有失败但没有可展示的失败详情：用通知给出总体说明
          const hasFailedMap = result.failed && result.failed.size > 0;
          const desc =
            globalMessage ||
            (hasFailedMap ? "导入存在失败或异常信息" : "导入存在失败");
          notifyTask = () => {
            notification.error({
              message: "导入存在失败",
              description: desc,
              duration: 0,
            });
          };
        }
      } else {
        // 兜底分支：未知 code，当作失败处理
        const data = result.data || {};
        const desc = data.globalMessage || result.globalMessage || "导入失败";
        notifyTask = () => {
          notification.error({
            message: "导入失败",
            description: desc,
            duration: 0,
          });
        };
      }

      if (notifyTask) {
        this.$nextTick(() => {
          notifyTask();
        });
      }
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
      // 根据functionMode初始化enableValidate（向后兼容）
      const enableValidate = this.functionMode === "validate";

      this.formModel = {
        relationFile: null,
        backFillFile: null,
        backfillFields: [],
        relationFileList: [],
        backFillFileList: [],
        importType: importType,
        originExcel: null,
        originExcelList: [],
        emptyStringAsValue: true,
        failFast: false,
        enableValidate: enableValidate,
        checkFields: [],
        checkSpecialChar: false,
        checkMaxLength: false,
      };
      this.loading = false;
      // 重置失败信息相关状态
      this.failedEntryInfos = [];
      this.exceptionVos = [];
      this.globalMessage = "";
      this.failedInfoVisible = false;
      this.failedExportDataSource = [];
      // 重置校验结果相关状态
      this.validationSummary = null;
      this.validationIssues = [];
      this.validationAttachments = null;
      if (this.$refs.backFillForm && typeof this.$refs.backFillForm.clearValidate === 'function') {
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
      // 重置校验结果相关状态
      this.validationSummary = null;
      this.validationIssues = [];
      this.validationAttachments = null;
    },
    // 提取失败词条数据用于导出
    extractFailedEntriesData() {
      const entriesMap = new Map();

      // 遍历 failedEntryInfos，提取所有词条数据
      // 注意：新API的响应体结构可能不同，需要根据实际响应结构调整
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

    // 下载校验问题JSON
    downloadValidationIssues() {
      if (this.validationIssues.length === 0) {
        message.warning("没有校验问题可下载");
        return;
      }

      downloadJsonFile(this.validationIssues, "backfill_validation_issues", false);
    },

    // 下载附件
    async downloadAttachment(downloadUrl, fileName) {
      if (!downloadUrl) {
        message.warning("下载链接不存在");
        return;
      }

      try {
        const response = await downloadFileFromUrl(downloadUrl);
        downloadBlobResponse(response, fileName);
      } catch (error) {
        console.error("下载附件失败：", error);
        message.error("下载附件失败");
      }
    },
    // 获取用户偏好
    queryBackfillFieldsPreference() {
      queryUserPartiality().then((res) => {
        if (res.data.list && res.data.list.length > 0) {
          let backfillFields = res.data.list[0].backfillFields;
          if (backfillFields != null && backfillFields != "") {
            this.formModel.backfillFields = backfillFields.split(",");
          }
        }
      });
    },
    // 保存用户偏好
    updateBackfillFieldsPreference(value) {
      let data = {
        backfillFields: value.join(","),
      };
      updateUserPartiality(data).then((res) => { });
    },
    // 全选更新字段方法
    selectAllBackfillFields() {
      this.formModel.backfillFields = this.fieldOptions.map((item) => item.label);
    },
  },
};
</script>

<style scoped lang="less">
.content {
  padding: 20px;
}
</style>
