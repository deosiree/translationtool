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
              placeholder="请选择更新字段" allowClear style="flex: 1; margin-right: 8px;" />
            <a-button type="link" size="small" @click="selectAllBackfillFields" style="
              font-size: smaller;margin-top:0">全选</a-button>
          </div>
        </a-form-item>

        <!-- 校验模式专用：去重前Excel（只有校验时才需要，用于和去重后Excel+映射文件生成的总文件比较） -->
        <a-form-item v-if="formModel.enableValidate" label="去重前Excel" name="originExcel"
          :rules="[{ required: true, validator: validateOriginExcel }]">
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
          <a-form-item label="校验字段" name="checkFields"
            :rules="formModel.enableValidate ? [{ required: true, message: '请选择!' }] : []">
            <a-select mode="multiple" v-model:value="formModel.checkFields" :options="checkFieldOptions"
              placeholder="请选择校验字段" allowClear />
          </a-form-item>

          <!-- 校验选项勾选框（flex布局，尽量一行，摆不下自动换行） -->
          <a-form-item label="校验选项">
            <a-row style="width:100%;display:flex;gap:8px;flex-wrap:wrap;">
              <a-checkbox v-model:checked="formModel.checkSpecialChar">特殊字符校验</a-checkbox>
              <a-checkbox v-model:checked="formModel.checkMaxLength">长度超限校验</a-checkbox>
            </a-row>
          </a-form-item>
        </template>
      </a-form>
    </div>
  </CustomModal>

  <!-- 失败信息模态框 -->
  <CustomModal :visible="failedInfoVisible"
    :modalTitle="formModel.enableValidate ? '校验结果' : (overallStatus === 'success' ? '更新结果' : '更新结果（部分失败）')"
    :showOk="false" @handleClose="failedInfoClose">
    <div class="failed-content">
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
      <div v-if="formModel.enableValidate && validationIssues && validationIssues.length > 0"
        style="margin-bottom: 16px;">
        <div style="margin-bottom: 8px; font-weight: 500;">校验问题列表：</div>
        <a-table :columns="issueColumns" :dataSource="validationIssues" :pagination="{ pageSize: 10 }" size="small"
          :scroll="{ y: 200 }">
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
      <div
        v-if="formModel.enableValidate && validationAttachments && Array.isArray(validationAttachments.issueLog) && validationAttachments.issueLog.length > 0"
        style="margin-bottom: 16px;">
        <div v-if="validationAttachments.invalidExcel" style="margin-bottom: 8px;">
          <span style="margin-right: 8px;">{{ validationAttachments.invalidExcel.fileName }}：</span>
          <a-button type="link"
            @click="downloadAttachment(validationAttachments.invalidExcel.downloadUrl, validationAttachments.invalidExcel.fileName)">下载</a-button>
        </div>
        <div v-if="validationAttachments.issueLog">
          <span style="margin-right: 8px;">{{ validationAttachments.issueLog.fileName }}：</span>
          <a-button type="link"
            @click="downloadAttachment(validationAttachments.issueLog.downloadUrl, validationAttachments.issueLog.fileName)">下载</a-button>
        </div>
      </div>

      <!-- 导入模式：按语种分组的失败信息展示 -->
      <template
        v-if="!formModel.enableValidate && failedEntryInfosByLang && Object.keys(failedEntryInfosByLang).length > 0">
        <a-tabs v-model:activeKey="activeFailedLangTab" type="card" class="failure-tabs">
          <a-tab-pane v-for="(failedInfos, langKey) in failedEntryInfosByLang" :key="langKey">
            <template #tab>
              <span style="display:inline-flex;align-items:center;gap:6px;">
                <span :style="{ color: langCodeByLang[langKey] === 200 ? '#52c41a' : '#ff4d4f', fontWeight: 500 }">
                  {{ langKey }}
                </span>
              </span>
            </template>
            <!-- 更新结果的globalMessage -->
            <div v-if="globalMessageByLang[langKey]"
              :style="{ marginBottom: '16px', color: langCodeByLang[langKey] === 200 ? '#52c41a' : '#ff4d4f', fontWeight: 500 }">
              <a-alert :description="globalMessageByLang[langKey]"
                :type="langCodeByLang[langKey] === 200 ? 'success' : 'error'" show-icon />
            </div>
            <!-- 更新结果的错误信息下载 -->
            <a-card class="failure-lang-card" v-if="langCodeByLang[langKey] === 201" :bordered="true">
              <template #title>
                <div class="failure-card-title">
                  <exclamation-circle-outlined class="failure-icon" />
                  <span class="lang-name">{{ langKey }}</span>
                  <a-badge :count="(failedInfos?.length || 0) + (exceptionVosByLang[langKey]?.length || 0)"
                    :number-style="{ backgroundColor: langCodeByLang[langKey] === 200 ? '#52c41a' : '#ff4d4f' }"
                    :overflow-count="999" />
                </div>
              </template>

              <div class="failure-card-content">
                <div v-if="failedInfos && failedInfos.length > 0" class="failure-item">
                  <div class="failure-label">
                    <file-excel-outlined class="item-icon" />
                    <span>部分更新失败的词条</span>
                    <a-tag color="error" style="margin-left: 8px;">{{ failedInfos.length }} 条</a-tag>
                  </div>
                  <a-button type="primary" size="small" danger @click="downloadFailedEntriesByLang(langKey)">
                    <template #icon><download-outlined /></template>
                    下载
                  </a-button>
                </div>

                <div v-if="exceptionVosByLang[langKey] && exceptionVosByLang[langKey].length > 0" class="failure-item">
                  <div class="failure-label">
                    <warning-outlined class="item-icon" />
                    <span>全部异常信息</span>
                    <a-tag color="warning" style="margin-left: 8px;">{{ exceptionVosByLang[langKey].length }} 条</a-tag>
                  </div>
                  <a-button type="primary" size="small" danger @click="downloadExceptionInfosByLang(langKey)">
                    <template #icon><download-outlined /></template>
                    下载
                  </a-button>
                </div>
              </div>
            </a-card>
          </a-tab-pane>
        </a-tabs>
      </template>
    </div>
  </CustomModal>

  <!-- 隐藏的 ExportButton，用于导出失败词条 -->
  <ExportButton ref="failedExportRef" :fileNamePrefix="failedExportLang + '更新失败_'" :dataSource="failedExportDataSource"
    :fieldOptions_="fieldOptions" :defaultStatusCheck="false" :hideButton="true"
    @afterClose="handleFailedExportAfterClose" />
</template>

<script>
import { message, notification } from "ant-design-vue";
import {
  ExclamationCircleOutlined,
  FileExcelOutlined,
  WarningOutlined,
  DownloadOutlined
} from '@ant-design/icons-vue';
import CustomModal from "@/components/modal/index.vue";
import ExportButton from "@/components/Button/exportButton.vue";
import { entryBatchImportExcel_V1_5, entryValidate_v2 } from "@/utils/excelUtils";
import { downloadJsonFile, downloadBlobResponse } from "@/utils/fileUtils";
import { downloadFileFromUrl } from "@/http/api/download";
import { entryParams } from "@/constants/commonParam.js";
import { setModalAriaHidden, stopDomEvent } from "@/utils/domUtils";
import { mapLabelToValue, mapValueToLabel } from "@/utils/dataStructureUtils";
export default {
  name: "BackFillModal_v1_5",
  components: {
    CustomModal,
    ExportButton,
    ExclamationCircleOutlined,
    FileExcelOutlined,
    WarningOutlined,
    DownloadOutlined,
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
        relationFileList: [],// id映射文件
        backFillFileList: [],// 去重后文件
        importType: null, // 文件类型（csv/excel）
        backfillFields: [],// 更新字段
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
      failedEntryInfosByLang: {}, // 按语种分组的失败词条信息 { "英文": [...], "俄文": [...] }
      exceptionVosByLang: {},     // 按语种分组的异常信息 { "英文": [...], "俄文": [...] }
      globalMessage: "", // 总体错误提示信息
      globalMessageByLang: {}, // 按语种分组的globalMessage { "英文": "...", "俄文": "..." }
      langCodeByLang: {}, // 按语种分组的响应 code { "英文": 200, "西文": 201 }
      overallStatus: "partial-error", // 导入结果整体状态：success / partial-error
      failedInfoVisible: false, // 控制失败信息模态框显示
      failedExportDataSource: [], // 用于导出失败词条的数据源
      failedExportLang: "", // 用于导出失败词条的文件名前缀
      failedExportDataSourceBackup: null, // 导出失败词条时临时备份原数据源
      activeFailedLangTab: '', // 当前激活的失败语种标签
      languageNames: ["英文", "俄文", "西文", "法文", "中文"], // v1.5支持的语种名称
      fieldOptions: [], // 初始值，会在mounted中从entryParams.exportFields更新并过滤
      // 校验字段选项（硬编码，通常为["entry", "comment", "tag"]）
      checkFieldOptions: [],
      // 校验结果相关状态
      validationSummary: null, // 校验摘要信息
      validationIssues: [], // 校验问题列表
      validationAttachments: { issueLog: [], invalidExcel: null }, // 校验附件信息
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
    // 初始化字段选项：v1.5只保留语种字段，直接使用语种名称作为label
    this.$nextTick(() => {
      this.checkFieldOptions = entryParams && entryParams.exportFields ? entryParams.exportFields : [];
      // 从exportFields中查找语种字段，然后创建新的选项，label使用语种名称
      this.fieldOptions = this.languageNames.map(langName => {
        // 找到对应的字段标签（如"英文翻译"）
        const fieldLabel = langName + "翻译";
        // 从exportFields中查找对应的字段
        const field = this.checkFieldOptions.find(item => item.label === fieldLabel);
        // 返回新选项：label使用语种名称（如"英文"），value使用字段值（如"english"）
        return {
          label: langName,
          value: field.value
        };
      });
      console.log("fieldOptions", this.fieldOptions, "this.checkFieldOptions", this.checkFieldOptions)

    });
    console.log("fieldOptions", this.fieldOptions)
  },
  methods: {
    // // 将“英文翻译”旧格式转换为语种名称，保持与API期望一致
    // normalizeBackfillFieldLabel(label) {
    //   if (!label) return "";
    //   return label.endsWith("翻译") ? label.slice(0, -2) : label;
    // },
    normalizeBackfillFields(fields) {
      return mapLabelToValue(fields, this.fieldOptions);
      // return (fields || []).map((item) => this.normalizeBackfillFieldLabel(item)).filter(Boolean);
    },
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

          // 保存用户偏好到 localStorage
          const languageNames = mapValueToLabel(this.formModel.backfillFields, this.fieldOptions);//（使用语种名称格式）
          let data = {
            backfillFields: languageNames.join(","),
            emptyStringAsValue: this.formModel.emptyStringAsValue,
            failFast: this.formModel.failFast,
            enableValidate: this.formModel.enableValidate,
            checkFields: this.formModel.checkFields.join(","),
            checkSpecialChar: this.formModel.checkSpecialChar,
            checkMaxLength: this.formModel.checkMaxLength,
          };
          localStorage.setItem("backfillFieldsPref", JSON.stringify(data));

          this.loading = true;
          console.log("handleOK", this.formModel.enableValidate);
          let validSuccess = true;
          // 校验模式：校验是否通过
          if (this.formModel.enableValidate) {
            try {
              validSuccess = await this.handleValidation();
              console.log("validSuccess", validSuccess)
            } catch (error) {
              const errorData = error?.response?.data || error?.data || error;
              notification.error({
                message: "校验过程发生异常！",
                description: errorData?.message || error.message || "未知错误",
                duration: 0,
              });
            }
          }
          // 校验通过，执行回填操作
          if (validSuccess) {
            try {
              await this.handleBatchUpdate();
            } catch (error) {
              const errorData = error?.response?.data || error?.data || error;
              notification.error({
                message: "更新/回填过程发生异常！",
                description: errorData?.message || error.message || "未知错误",
                duration: 0,
              });
            }
          }

          // 按钮模式需要 emit importSuccess 事件
          if (this.mode === "button") {
            this.$emit("importSuccess");
          }

          // 先关闭当前弹窗
          this.handleCloseInternal();

          this.loading = false;
        })
        .catch((err) => {
          console.log("表单校验失败", err);
        });
    },

    // 处理校验响应
    async handleValidation() {
      let result = await entryValidate_v2(
        this.formModel.originExcel,// 去重前Excel
        this.formModel.backFillFile,// 去重后Excel
        this.needRelationFile ? this.formModel.relationFile : null,// 词条映射.json
        this.formModel.checkFields,// 校验字段
        this.formModel.backfillFields, // 更新字段
        {
          emptyStringAsValue: this.formModel.emptyStringAsValue,
          failFast: this.formModel.failFast,
          checkSpecialChar: this.formModel.checkSpecialChar,
          checkMaxLength: this.formModel.checkMaxLength
        }// options配置
      );

      this.validationSummary = result.summary || null;
      this.validationIssues = result.issues || [];
      this.validationAttachments = result.attachments || { issueLog: [], invalidExcel: null };
      console.log("校验的下载模态框", this.validationAttachments.issueLog.length)
      if (result.success && result.canBackFill) {
        // 校验通过，可回填
        notification.success({
          message: "校验通过",
          // description: `共校验 ${result.summary?.totalOriginRows || 0} 条数据，${result.summary?.affectedRows || 0} 条将受影响`,
          duration: 0,
        });
        this.formModel.enableValidate = false;// 校验流程结束，关闭校验模式
      } else {
        this.failedInfoVisible = true;
        if (result.success && !result.canBackFill) {
          // 校验通过但不可回填（有WARN级别问题）
          notification.error({
            message: "校验告警",
            description: "校验完成，但存在警告，不允许回填",
            duration: 0,
          });
        } else {
          // 校验失败（有FATAL级别错误）
          notification.error({
            message: "校验失败",
            description: "校验失败，存在致命错误",
            duration: 0,
          });
        }
      }

      console.log("校验的响应体V1.5", result, "globalMessage", this.globalMessage)
      return !this.failedInfoVisible;// 若未开启失败信息模态框，则返回true，否则返回false
    },

    // 处理导入响应
    async handleBatchUpdate() {
      let notifyTask = null;

      // 构建FormData（v1 API需要FormData格式）
      const formData = new FormData();
      formData.append("file", this.formModel.backFillFile);
      if (this.needRelationFile && this.formModel.relationFile) {
        formData.append("relationFile", this.formModel.relationFile);
      }

      // 将选择的字段标准化为语种名称（兼容“英文翻译”旧值）
      // const languageNames = this.normalizeBackfillFields(this.formModel.backfillFields);
      const languageNames = mapValueToLabel(this.formModel.backfillFields, this.fieldOptions);

      // 调用v1.5的批量导入API
      let result = await entryBatchImportExcel_V1_5(
        languageNames,// 更新语种名称数组
        formData
      );

      // 输出完整响应体用于调试
      console.log("去重回填的响应体V1.5", result);


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
        // 新响应体：仅解析 msgBylang（无需兼容旧字段）
        const msgBylang = Array.isArray(result.msgBylang) ? result.msgBylang : [];
        const failedEntryInfosByLang = {};
        const exceptionVosByLang = {};
        const globalMessageByLang = {};
        const langCodeByLang = {};
        let firstFailedLang = "";

        msgBylang.forEach((item) => {
          const langKey = item?.lang;
          if (!langKey) return;

          const code = item?.code ?? 201;
          langCodeByLang[langKey] = code;

          const detail = item || {};
          failedEntryInfosByLang[langKey] = detail.failedEntryInfos || [];
          exceptionVosByLang[langKey] = detail.exceptionVOs || detail.exceptionVos || [];
          globalMessageByLang[langKey] = detail.globalMessage || "";

          if (!firstFailedLang && code === 201) {
            firstFailedLang = langKey;
          }
        });

        // 若没有任何可展示项：走通知兜底
        if (Object.keys(failedEntryInfosByLang).length === 0) {
          const firstItem = msgBylang[0] || {};
          const firstDetail = firstItem.data || {};
          const desc =
            firstDetail.globalMessage ||
            firstItem.message ||
            "更新存在异常（无可展示的分语种详情）";
          notifyTask = () => {
            notification.error({
              message: "更新存在异常",
              description: desc,
              duration: 0,
            });
          };
        } else {
          // 写回状态：弹窗展示所有语言（成功绿/失败红）
          this.failedEntryInfosByLang = failedEntryInfosByLang;
          this.exceptionVosByLang = exceptionVosByLang;
          this.globalMessageByLang = globalMessageByLang;
          this.langCodeByLang = langCodeByLang;
          this.failedEntryInfos = [];
          this.exceptionVos = [];
          this.globalMessage = "";

          this.overallStatus = firstFailedLang ? "partial-error" : "success";
          this.activeFailedLangTab = firstFailedLang || Object.keys(failedEntryInfosByLang)[0];
          this.failedInfoVisible = true;
        }
      } else {
        // 兜底分支：未知 code，当作失败处理
        // 新响应格式：数据直接在 result 上，没有 result.data
        const desc = result.globalMessage || "导入失败";
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
      this.failedEntryInfosByLang = {};
      this.exceptionVosByLang = {};
      this.globalMessage = "";
      this.globalMessageByLang = {};
      this.langCodeByLang = {};
      this.overallStatus = "partial-error";
      this.failedInfoVisible = false;
      this.failedExportDataSource = [];
      this.activeFailedLangTab = '';
      // 重置校验结果相关状态
      this.validationSummary = null;
      this.validationIssues = [];
      this.validationAttachments = { issueLog: [], invalidExcel: null };
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
      this.failedEntryInfosByLang = {};
      this.exceptionVosByLang = {};
      this.globalMessage = "";
      this.globalMessageByLang = {};
      this.langCodeByLang = {};
      this.overallStatus = "partial-error";
      this.failedExportDataSource = [];
      this.activeFailedLangTab = '';
      // 重置校验结果相关状态
      this.validationSummary = null;
      this.validationIssues = [];
      this.validationAttachments = { issueLog: [], invalidExcel: null };
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
    // 按语种下载失败词条
    downloadFailedEntriesByLang(lang) {
      const failedInfos = this.failedEntryInfosByLang[lang] || [];
      if (failedInfos.length === 0) {
        message.warning(`没有${lang}的失败词条数据可导出`);
        return;
      }
      console.log("下载失败词条", failedInfos);

      // 临时设置数据源并触发导出
      const originalDataSource = this.failedExportDataSource;
      if (this.failedExportDataSourceBackup == null) {
        this.failedExportDataSourceBackup = originalDataSource;
      }
      this.failedExportDataSource = failedInfos;
      this.failedExportLang = lang;

      console.log("把下载词条传递给exportButton", this.$refs.failedExportRef, this.failedExportDataSource);
      if (this.$refs.failedExportRef) {
        console.log("传递给exportButton", this.failedExportDataSource);
        // 等待 prop 更新到子组件后再打开弹窗，否则子组件可能读到旧的空数组
        this.$nextTick(() => {
          this.$refs.failedExportRef.showExportModal();
        });
      } else {
        message.error("导出功能初始化失败");
      }
    },
    handleFailedExportAfterClose() {
      if (this.failedExportDataSourceBackup != null) {
        this.failedExportDataSource = this.failedExportDataSourceBackup;
        this.failedExportDataSourceBackup = null;
      }
    },
    // 按语种下载异常信息
    downloadExceptionInfosByLang(lang) {
      const exceptionVos = this.exceptionVosByLang[lang] || [];
      if (exceptionVos.length === 0) {
        message.warning(`没有${lang}的异常信息可下载`);
        return;
      }

      downloadJsonFile(exceptionVos, `${lang}更新异常_`, false);
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
      // 读取本地存储的用户偏好
      const storedPreferences = localStorage.getItem("backfillFieldsPref");
      console.log("读取用户偏好", storedPreferences);
      if (storedPreferences) {
        const preferences = JSON.parse(storedPreferences);
        // 应用偏好-更新字段
        if (preferences.backfillFields != null && preferences.backfillFields != "") {
          const rawFields = preferences.backfillFields.split(",");
          this.formModel.backfillFields = this.normalizeBackfillFields(rawFields);
        }
        // 应用偏好-导入空值
        if (preferences.emptyStringAsValue !== undefined) {
          this.formModel.emptyStringAsValue = preferences.emptyStringAsValue;
        }
        // 应用偏好-失败中止
        if (preferences.failFast !== undefined) {
          this.formModel.failFast = preferences.failFast;
        }
        // 应用偏好-校验
        if (preferences.enableValidate !== undefined) {
          this.formModel.enableValidate = preferences.enableValidate;
        }
        // 应用偏好-校验字段
        if (preferences.checkFields != null && preferences.checkFields !== "") {
          this.formModel.checkFields = preferences.checkFields.split(",");
        }
        // 应用偏好-特殊字符校验
        if (preferences.checkSpecialChar !== undefined) {
          this.formModel.checkSpecialChar = preferences.checkSpecialChar;
        }
        // 应用偏好-长度超限校验
        if (preferences.checkMaxLength !== undefined) {
          this.formModel.checkMaxLength = preferences.checkMaxLength;
        }
      }
    },
    // 全选更新字段方法
    selectAllBackfillFields() {
      this.formModel.backfillFields = this.fieldOptions.map((item) => item.value);
    },
  },
};
</script>

<style scoped lang="less">
.content {
  padding: 20px;
}

.failed-content {
  padding: 0px;
}

.failure-lang-card {
  margin-top: 16px;
  border-radius: 8px;
  border-color: #ff7875;
  background-color: #fff2f0;
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.15);
  transition: all 0.3s ease;
}

.failure-lang-card:hover {
  box-shadow: 0 4px 16px rgba(255, 77, 79, 0.25);
  transform: translateY(-2px);
}

.failure-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #cf1322;
}

.failure-icon {
  font-size: 18px;
  color: #ff4d4f;
}

.lang-name {
  flex: 1;
}

.failure-card-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.failure-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background-color: #ffffff;
  border-radius: 6px;
  border: 1px solid #ffccc7;
}

.failure-label {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  font-size: 14px;
  color: #262626;
}

.item-icon {
  font-size: 16px;
  color: #ff7875;
}

/* 向后兼容的旧样式 */
.failure-lang-card :deep(.ant-card-head) {
  background-color: #fff1f0;
  border-bottom: 1px solid #ffa39e;
}

.failure-lang-card :deep(.ant-card-body) {
  padding: 16px;
}

/* 标签页容器样式 */
.failure-tabs {
  margin-top: 16px;
}

.failure-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}

.failure-tabs :deep(.ant-tabs-tab) {
  border-color: #d9d9d9;
  background-color: #ffffff;
  transition: all 0.3s ease;
}

.failure-tabs :deep(.ant-tabs-tab:hover) {
  border-color: #bfbfbf;
}

.failure-tabs :deep(.ant-tabs-tab-active) {
  background-color: #ffffff;
  border-bottom-color: #ffffff;
  font-weight: 600;
}

.failure-tabs :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: inherit;
}

/* 调整卡片在标签页内的样式 */
.failure-tabs .failure-lang-card {
  margin-top: 0;
  border-top-left-radius: 0;
  border-top-right-radius: 8px;
}
</style>
