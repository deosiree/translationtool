<template>
  <a-button v-if="mode === 'button'" type="primary" @click="handleButtonClick" :size="size">
    {{ buttonTitle }}
  </a-button>

  <CustomModal :visible="internalVisible" :okLoading="loading" :modalTitle="modalTitle" @handleClose="handleClose"
    @handleOK="handleOK" style="width: 800px;">
    <div class="content">
      <a-form ref="backFillForm" :model="formModel">
        <!-- 共同部分：文件类型 -->
        <a-form-item v-if="showFileTypeSelect" label="文件类型" name="importType"
          :rules="[{ required: true, message: '请选择!' }]">
          <a-select v-model:value="formModel.importType" placeholder="请选择文件类型" :options='importTypes' allowClear>
          </a-select>
        </a-form-item>

        <!-- 共同部分：回填字段（回填字段） -->
        <a-form-item label="回填字段" name="backfillFields" :rules="[{ required: true, message: '请选择!' }]">
          <div style="display: flex; justify-content: space-between;">
            <a-select mode="multiple" v-model:value="formModel.backfillFields" :options="languageOptions"
              :fieldNames="{ label: 'name', value: 'value' }" placeholder="请选择回填字段" allowClear
              style="flex: 1; margin-right: 8px;" />
            <a-button type="link" size="small" @click="selectAllBackfillFields" style="
              font-size: smaller;margin-top:0">全选</a-button>
          </div>
        </a-form-item>

        <!-- 校验模式专用：去重后送翻前文件（只有校验时才需要，用于和去重后送翻后文件+映射文件生成的总文件比较） -->
        <a-form-item v-if="formModel.enableValidate" label="去重文件（去重后，送翻前）" name="dedupOriginExcel"
          :rules="[{ required: true, validator: validatededupOriginExcel }]">
          <a-upload name="file" :beforeUpload="beforeUpload" :accept="backFillAccept" :max-count="1"
            :fileList="formModel.dedupOriginExcelList" @change="handlededupOriginExcelUpload"
            @remove="removededupOriginExcel">
            <a-button type="primary" size="small" @click="handlededupOriginExcelChooseClick">选择</a-button>
          </a-upload>
        </a-form-item>

        <!-- 共同部分：去重后送翻后文件（待回填文件） -->
        <a-form-item label="回填文件（去重后，送翻后）" name="dedupUpdateExcel"
          :rules="[{ required: true, validator: validateBackFillFile }]">
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
            <a-checkbox v-model:checked="formModel.enableValidate">校验</a-checkbox>
            <a-checkbox v-model:checked="formModel.emptyStringAsValue">导入空值</a-checkbox>
            <a-checkbox v-model:checked="formModel.failFast">失败中止</a-checkbox>
          </a-row>
        </a-form-item>

        <!-- 校验模式专用：勾选了"校验"后才显示 -->
        <template v-if="formModel.enableValidate">
          <!-- 校验字段 -->
          <a-form-item label="校验字段" name="checkFields"
            :rules="formModel.enableValidate ? [{ required: true, message: '请选择!' }] : []">
            <a-select mode="multiple" v-model:value="formModel.checkFields" :options="fieldOptions"
              placeholder="请选择校验字段" allowClear />
          </a-form-item>

          <!-- 校验选项勾选框（flex布局，尽量一行，摆不下自动换行） -->
          <a-form-item label="校验选项">
            <a-row style="width:100%;display:flex;gap:8px;flex-wrap:wrap;">
              <a-checkbox v-model:checked="formModel.checkSpecialChar">特殊字符校验</a-checkbox>
              <a-checkbox v-model:checked="formModel.checkMaxLength">长度超限校验</a-checkbox>
            </a-row>
          </a-form-item>

          <!-- 校验选项共用：任一规则勾选时展示“翻译列语种” -->
          <a-form-item v-if="formModel.checkSpecialChar || formModel.checkMaxLength" label="翻译列语种"
            name="translateAttrs">
            <div style="display: flex; justify-content: space-between;">
              <a-select mode="multiple" v-model:value="formModel.translateAttrs" :options="languageOptions"
                :fieldNames="{ label: 'name', value: 'value' }" placeholder="请选择翻译列语种" allowClear
                style="flex: 1; margin-right: 8px;" />
              <a-button type="link" size="small" @click="selectAllValidateTranslateAttrs" style="
                font-size: smaller;margin-top:0">全选</a-button>
            </div>
          </a-form-item>
        </template>
      </a-form>
    </div>
  </CustomModal>

  <!-- 校验结果模态框 -->
  <CustomModal :visible="validationVisible" modalTitle="校验结果" :showOk="false" @handleClose="validationClose">
    <div class="validation-content">
      <!-- 显示校验通过提示 -->
      <div v-if="isValidationSuccess" style="margin-bottom: 16px;">
        <a-alert message="校验通过" type="success" show-icon description="所有校验项目均通过，可以继续回填" />
      </div>

      <!-- 显示summary信息 -->
      <div v-if="validation.summary" style="margin-bottom: 16px;">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="去重前行数">{{ validation.summary.totalOriginRows || 0 }}</a-descriptions-item>
          <a-descriptions-item label="去重后行数">{{ validation.summary.totalDedupRows || 0 }}</a-descriptions-item>
          <a-descriptions-item label="受影响行数">{{ validation.summary.affectedRows || 0 }}</a-descriptions-item>
          <a-descriptions-item label="将更新单元格数">{{ validation.summary.willUpdateCells || 0 }}</a-descriptions-item>
        </a-descriptions>
      </div>

      <!-- 显示issues列表 -->
      <div v-if="validation.issues && validation.issues.length > 0" style="margin-bottom: 16px;">
        <div style="margin-bottom: 8px; font-weight: 500;">校验问题列表：</div>
        <a-table :columns="issueColumns" :dataSource="validation.issues" :pagination="{ pageSize: 10 }" size="small"
          :scroll="{ y: 200 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'level'">
              <a-tag :color="record.level === 'FATAL' ? 'red' : record.level === 'WARN' ? 'orange' : 'blue'">
                {{ record.level }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </div>

      <!-- 显示attachments下载 -->
      <div v-if="hasValidationInfo" style="margin-bottom: 16px;">
        <div>
          <span style="margin-right: 8px;">异常信息（全部）：</span>
          <a-button type="link"
            @click="downloadAttachment(validation.attachments.issueLog.downloadUrl, validation.attachments.issueLog.fileName)">下载</a-button>
        </div>
        <div v-if="validation.attachments.invalidExcel && validation.attachments.invalidExcel.downloadUrl"
          style="margin-bottom: 8px;">
          <span style="margin-right: 8px;">失败词条（部分）：</span>
          <a-button type="link"
            @click="downloadAttachment(validation.attachments.invalidExcel.downloadUrl, validation.attachments.invalidExcel.fileName)">下载</a-button>
        </div>
      </div>
    </div>
    <template #leftBottomBtn>
      <a-button v-if="validation.canBackFill" type="primary" :loading="loading" @click="handleContinueBackFill">
        继续回填
      </a-button>
    </template>
  </CustomModal>

  <!-- 更新结果模态框 -->
  <CustomModal :visible="updateVisible" modalTitle="更新结果" :showOk="false" @handleClose="updateClose">
    <div class="update-content">
      <!-- 按语种分组的失败信息展示 -->
      <a-tabs v-if="detailsByLang && Object.keys(detailsByLang).length > 0" v-model:activeKey="activeFailedLangTab"
        type="card" class="failure-tabs">
        <a-tab-pane v-for="(detail, langKey) in detailsByLang" :key="langKey">
          <template #tab>
            <span style="display:inline-flex;align-items:center;gap:6px;">
              <span :style="{ color: (detail.code || 201) === 200 ? '#52c41a' : '#ff4d4f', fontWeight: 500 }">
                {{ langKey }}
              </span>
            </span>
          </template>
          <!-- 更新结果的globalMessage -->
          <div v-if="detail.globalMessage"
            :style="{ marginBottom: '16px', color: (detail.code || 201) === 200 ? '#52c41a' : '#ff4d4f', fontWeight: 500 }">
            <a-alert :description="detail.globalMessage" :type="(detail.code || 201) === 200 ? 'success' : 'error'"
              show-icon />
          </div>
          <!-- 更新结果的错误信息下载 -->
          <a-card class="failure-lang-card" v-if="(detail.code || 201) === 201" :bordered="true">
            <template #title>
              <div class="failure-card-title">
                <exclamation-circle-outlined class="failure-icon" />
                <span class="lang-name">{{ langKey }}</span>
                <a-badge :count="(detail.failedEntryInfos?.length || 0) + (detail.exceptionVos?.length || 0)"
                  :number-style="{ backgroundColor: (detail.code || 201) === 200 ? '#52c41a' : '#ff4d4f' }"
                  :overflow-count="999" />
              </div>
            </template>

            <div class="failure-card-content">
              <div v-if="detail.exceptionVos && detail.exceptionVos.length > 0" class="failure-item">
                <div class="failure-label">
                  <warning-outlined class="item-icon" />
                  <span>异常信息（全部）</span>
                  <a-tag color="warning" style="margin-left: 8px;">{{ detail.exceptionVos.length }} 条</a-tag>
                </div>
                <a-button type="primary" size="small" danger @click="downloadExceptionInfosByLang(langKey)">
                  <template #icon><download-outlined /></template>
                  下载
                </a-button>
              </div>
              <div v-if="detail.failedEntryInfos && detail.failedEntryInfos.length > 0" class="failure-item">
                <div class="failure-label">
                  <file-excel-outlined class="item-icon" />
                  <span>失败词条（部分）</span>
                  <a-tag color="error" style="margin-left: 8px;">{{ detail.failedEntryInfos.length }} 条</a-tag>
                </div>
                <a-button type="primary" size="small" danger @click="downloadFailedEntriesByLang(langKey)">
                  <template #icon><download-outlined /></template>
                  下载
                </a-button>
              </div>
            </div>
          </a-card>
        </a-tab-pane>
      </a-tabs>
    </div>
  </CustomModal>

  <!-- 隐藏的 ExportButton，用于导出失败词条 -->
  <ExportButton ref="failedExportRef" :fileNamePrefix="failedExportLang + '更新失败_'" :dataSource="failedExportDataSource"
    :fieldOptions_="languageOptions" :defaultStatusCheck="false" :hideButton="true"
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
import { entryBatchImportExcel_V1_5, entryValidate_v2, resolveImportTypeFromAccept } from "@/utils/excelUtils";
import { downloadJsonFile, downloadBlobResponse, handleFileUpload, removeFile } from "@/utils/fileUtils";
import { downloadFileFromUrl } from "@/http/api/download";
import commonParam from "@/constants/commonParam.js";
import { entryParams } from "@/constants/commonParam.js";
import { setModalAriaHidden, stopDomEvent } from "@/utils/domUtils";
import { mapValueToName } from "@/utils/dataStructureUtils";
import { handleErrorNotification } from "@/utils/notificationUtils";
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
    mode: {
      type: String,
      default: "modal", // 'button' 或 'modal'
    },
    showFileTypeSelect: {
      type: Boolean,
      default: false,
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
      default: null, // 如 ".csv"、".xls,.xlsx"。有选择器时据此从 importTypes 反查 value 作默认选中；无选择器时直接作上传 accept 及扩展名校验。
    },
  },
  data() {
    return {
      internalVisible: false, // 内部控制的 visible（按钮模式使用）
      formModel: {
        importType: null, // 文件类型（csv/excel）
        relationFile: null,
        relationFileList: [],// id映射文件
        backFillFile: null,
        backFillFileList: [],// 去重后文件
        backfillFields: [],// 回填字段
        // 选项勾选框
        enableValidate: true,    // 校验（勾选后才显示校验相关字段）
        emptyStringAsValue: false,  // 导入空值
        failFast: false,          // 失败中止
        // 校验模式专用字段（勾选了enableValidate后才使用）
        dedupOriginExcel: null,        // 去重后送翻前文件
        dedupOriginExcelList: [],      // 去重后送翻前文件列表
        checkFields: [],          // 校验字段（如["entry", "comment", "tag"]）
        checkSpecialChar: false,  // 特殊字符校验
        checkMaxLength: false,    // 长度校验
        translateAttrs: [],// 翻译列语种
      },
      loading: false,
      idMappingAccept: ".json",
      importTypes: [
        { label: "csv", value: "csv", accept: ".csv" },
        { label: "excel", value: "excel", accept: ".xls,.xlsx" },
      ],
      validationVisible: false, // 控制校验结果模态框显示
      // 校验结果相关状态
      validation: {
        summary: null,        // 校验摘要信息
        issues: [],           // 校验问题列表
        attachments: { issueLog: [], invalidExcel: null }, // 校验附件信息
        canBackFill: false,   // 是否可继续回填
      },
      // issues表格列定义
      issueColumns: [
        { title: '级别', dataIndex: 'level', key: 'level', width: 80 },
        { title: '描述', dataIndex: 'message', key: 'message', width: 300 },
        { title: '类型', dataIndex: 'type', key: 'type', width: 150 },
      ],
      updateVisible: false, // 控制更新结果模态框显示
      detailsByLang: {}, // 失败信息相关状态（按语种聚合）{ "英文": { code, globalMessage, failedEntryInfos, exceptionVos } }
      activeFailedLangTab: '', // 当前激活的失败语种标签
      failedExportDataSource: [], // 用于导出失败词条的数据源
      failedExportLang: "", // 用于导出失败词条的文件名前缀
      // 回填字段(后续拓展后同校验字段),翻译列语种（语种名称，来自公共常量,name/value）
      languageOptions: (commonParam && Array.isArray(commonParam.languageList)) ? commonParam.languageList : [],
    };
  },
  computed: {
    // 校验字段选项（字段名称，来自公共常量,label/value），过滤掉翻译id字段
    fieldOptions() {
      const fields = (entryParams && entryParams.exportFields) ? entryParams.exportFields : [];
      // 过滤掉包含"翻译id"的字段
      return fields.filter(field => !field.label.includes('翻译id') && !field.value.includes('TransId'));
    },
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
    // 判断校验是否完全成功（无任何问题）
    isValidationSuccess() {
      return this.validation.canBackFill && (!this.validation.issues || this.validation.issues.length === 0);
    },
    // 判断校验是否有异常信息
    hasValidationInfo() {
      return this.validation.attachments && (this.validation.attachments.invalidExcel || (this.validation.attachments.issueLog && this.validation.attachments.issueLog.downloadUrl));
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
    // 校验结果弹窗显示后，修复 aria-hidden 导致的焦点可访问性告警
    validationVisible(newVal) {
      if (newVal) {
        setModalAriaHidden(this, document);
      }
    },
    // 更新结果弹窗显示后，修复 aria-hidden 导致的焦点可访问性告警
    updateVisible(newVal) {
      if (newVal) {
        setModalAriaHidden(this, document);
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
    // 有选择器且传了 defaultAccept：根据 accept 从 importTypes 反查 value，作为默认选中
    if (this.defaultAccept && this.showFileTypeSelect) {
      const v = resolveImportTypeFromAccept(this.defaultAccept, this.importTypes);
      if (v) this.formModel.importType = v;
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
  },
  methods: {
    // ==================== 模态框控制 ====================
    // 按钮模式：点击按钮打开模态框
    handleButtonClick() {
      this.internalVisible = true;
      setModalAriaHidden(this, document);
      // 获取用户偏好
      this.queryBackfillFieldsPreference();
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
      // 关闭校验模态框（如果打开的话）
      if (this.validationVisible) {
        this.validationVisible = false;
      }
      // 关闭更新模态框（如果打开的话）
      if (this.updateVisible) {
        this.updateVisible = false;
      }
    },
    resetForm() {
      // 重置 importType：有选择器且传了 defaultAccept 时，根据 accept 反查 value，否则为 null
      const importType = this.defaultAccept && this.showFileTypeSelect
        ? resolveImportTypeFromAccept(this.defaultAccept, this.importTypes) : null;

      this.formModel = {
        relationFile: null,
        backFillFile: null,
        backfillFields: [],
        relationFileList: [],
        backFillFileList: [],
        importType: importType,
        dedupOriginExcel: null,
        dedupOriginExcelList: [],
        emptyStringAsValue: true,
        failFast: false,
        enableValidate: true,
        checkFields: [],
        checkSpecialChar: false,
        checkMaxLength: false,
        translateAttrs: [],
      };
      this.loading = false;
      // 重置模态框的相关属性
      this.validationClose();
      this.updateClose();
      if (this.$refs.backFillForm && typeof this.$refs.backFillForm.clearValidate === 'function') {
        this.$refs.backFillForm.clearValidate();
      }
    },
    // 关闭校验结果模态框
    validationClose() {
      this.validationVisible = false;
      // 重置校验结果相关状态
      this.validation = {
        summary: null,
        issues: [],
        attachments: { issueLog: [], invalidExcel: null },
        canBackFill: false,
      };
    },
    // 关闭更新结果模态框
    updateClose() {
      this.updateVisible = false;
      // 重置更新结果相关状态
      this.detailsByLang = {};
      this.failedExportDataSource = [];
      this.activeFailedLangTab = '';
    },

    // ==================== 文件上传相关 ====================
    handleIdMappingUpload(info) {
      handleFileUpload(this.formModel, info, "relationFile", "relationFileList");
    },
    handleBackFillUpload(info) {
      handleFileUpload(this.formModel, info, "backFillFile", "backFillFileList");
    },
    handlededupOriginExcelUpload(info) {
      handleFileUpload(this.formModel, info, "dedupOriginExcel", "dedupOriginExcelList");
    },
    handlededupOriginExcelChooseClick(e) {
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
    removeIdMappingFile() {
      return removeFile(this.formModel, "relationFile", "relationFileList");
    },
    removeBackFillFile() {
      return removeFile(this.formModel, "backFillFile", "backFillFileList");
    },
    removededupOriginExcel() {
      return removeFile(this.formModel, "dedupOriginExcel", "dedupOriginExcelList");
    },

    // ==================== 文件验证相关 ====================
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
    validatededupOriginExcel() {
      const file = this.formModel.dedupOriginExcel;
      if (!file) {
        return Promise.reject("请选择去重后送翻前文件文件！");
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

    // ==================== 表单提交和业务逻辑 ====================
    async handleOK() {
      if (!this.$refs.backFillForm) return;

      this.$refs.backFillForm
        .validate()
        .then(async () => {
          // validate() 已经验证了所有 rules，无需再次手动验证
          this.$emit("handleOK");

          // 保存用户偏好到 localStorage
          let data = {
            backfillFields: this.formModel.backfillFields.join(","),
            emptyStringAsValue: this.formModel.emptyStringAsValue,
            failFast: this.formModel.failFast,
            enableValidate: this.formModel.enableValidate,
            checkFields: this.formModel.checkFields.join(","),
            checkSpecialChar: this.formModel.checkSpecialChar,
            checkMaxLength: this.formModel.checkMaxLength,
            translateAttrs: (this.formModel.translateAttrs || []).join(","),
          };
          localStorage.setItem("backfillFieldsPref", JSON.stringify(data));

          this.loading = true;
          // console.log("保存用户偏好", data);
          try {
            // 校验模式：校验是否通过
            if (this.formModel.enableValidate) {
              await this.handleValidation();// 若可回填，则显示"继续回填"按钮，不再自动执行更新，改为通过"继续回填"按钮人为控制
              // console.log("canBackFill", this.validation.canBackFill)
              // 等待新模态框渲染完成后再关闭loading
              this.$nextTick(() => {
                this.loading = false;
              });
            } else {
              // 不勾选校验时：直接执行批量更新（无需展示"继续回填"按钮）
              await this.handleBatchUpdate();
              // 先关闭当前弹窗
              this.handleCloseInternal();
              this.loading = false;
            }
          } catch (error) {
            // 错误处理：确保 loading 被关闭
            this.loading = false;
            throw error; // 重新抛出错误，让外层 catch 处理
          }
        })
        .catch((err) => {
          console.log("表单校验失败", err);
        });
    },
    // 处理校验响应
    async handleValidation() {
      try {
        // 按 utils/excelUtils.js 的签名直接传参，避免 FormData 结构错位
        const payloadOptions = {
          emptyStringAsValue: this.formModel.emptyStringAsValue,
          failFast: this.formModel.failFast,
          checkSpecialChar: this.formModel.checkSpecialChar,
          checkMaxLength: this.formModel.checkMaxLength,
          translateAttributes: this.formModel.translateAttrs,
        };

        const result = await entryValidate_v2(
          this.formModel.dedupOriginExcel,                  // dedupOriginExcel
          this.formModel.backFillFile,                 // dedupUpdateExcel
          this.needRelationFile ? this.formModel.relationFile : null, // mappingJson
          this.formModel.checkFields,                  // checkFields
          this.formModel.backfillFields,               // backfillFields
          payloadOptions                               // options
        );

        // 处理响应数据：支持嵌套的 data 结构（真实 API 可能返回 {code, data: {...}}）
        const data = result?.data || result;
        this.validation = {
          summary: data.summary || null,
          issues: data.issues || [],
          attachments: data.attachments || { issueLog: [], invalidExcel: null },
          canBackFill: data.canBackFill || false,
        };

        // 统一显示校验结果模态框，无论校验结果如何
        this.validationVisible = true;
        // 等待新模态框渲染完成后再关闭旧模态框
        this.$nextTick(() => {
          this.internalVisible = false;
        });
      } catch (error) {
        handleErrorNotification(error, "校验过程发生异常！");
        throw error; // 重新抛出错误，让调用方知道校验失败
      }
    },
    // 处理导入响应
    async handleBatchUpdate() {
      try {
        let notifyTask = null;

        // 构建FormData（v1 API需要FormData格式）
        const formData = new FormData();
        formData.append("file", this.formModel.backFillFile);
        if (this.needRelationFile && this.formModel.relationFile) {
          formData.append("relationFile", this.formModel.relationFile);
        }

        // 调用v1.5的批量导入API
        let result = await entryBatchImportExcel_V1_5(
          mapValueToName(this.formModel.backfillFields, this.languageOptions),// 将回填字段english转化为英文
          formData
        );

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

          // 按钮模式需要 emit importSuccess 事件（仅在成功时）
          if (this.mode === "button") {
            this.$emit("importSuccess");
          }
        } else if (result.code === 201) {
          // 新响应体：仅解析 msgBylang（无需兼容旧字段）
          const msgBylang = Array.isArray(result.msgBylang) ? result.msgBylang : [];
          const detailsByLang = {};
          let firstFailedLang = "";

          msgBylang.forEach((item) => {
            const langKey = item?.lang;
            if (!langKey) return;

            const code = item?.code ?? 201;
            const detail = item || {};
            detailsByLang[langKey] = {
              code,
              globalMessage: detail.globalMessage || "",
              failedEntryInfos: detail.failedEntryInfos || [],
              exceptionVos: detail.exceptionVOs || detail.exceptionVos || [],
            };

            if (!firstFailedLang && code === 201) {
              firstFailedLang = langKey;
            }
          });

          // 若没有任何可展示项：走通知兜底
          if (Object.keys(detailsByLang).length > 0) {
            // 写回状态：弹窗展示所有语言（成功绿/失败红）
            this.detailsByLang = detailsByLang;
            this.activeFailedLangTab = firstFailedLang || Object.keys(detailsByLang)[0];
            this.updateVisible = true;
            // 等待新模态框渲染完成后再关闭旧模态框
            this.$nextTick(() => {
              this.validationVisible = false;
            });
          } else {
            // 若没有任何可展示项：走通知兜底
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
      } catch (error) {
        handleErrorNotification(error, "更新/回填过程发生异常！");
        throw error; // 重新抛出错误，让调用方知道更新失败
      }
    },
    // 处理继续回填按钮点击
    async handleContinueBackFill() {
      this.loading = true;
      try {
        await this.handleBatchUpdate();
        // 等待新模态框渲染完成后再关闭loading
        this.$nextTick(() => {
          this.loading = false;
        });
      } catch (error) {
        // 错误处理：确保 loading 被关闭
        this.loading = false;
        throw error; // 重新抛出错误，让调用方知道更新失败
      }
    },

    // ==================== 下载相关 ====================
    // 辅助方法：从 detailsByLang 中提取指定语种和字段的数据，并验证是否为空
    extractDataByLang(lang, fieldName, emptyMessage) {
      const data = (this.detailsByLang[lang] && this.detailsByLang[lang][fieldName]) || [];
      if (data.length === 0) {
        message.warning(emptyMessage);
        return null;
      }
      return data;
    },
    // 按语种下载失败词条
    downloadFailedEntriesByLang(lang) {
      const failedInfos = this.extractDataByLang(lang, 'failedEntryInfos', `没有${lang}的失败词条数据可导出`);
      if (!failedInfos) return;

      // console.log("下载失败词条", failedInfos);

      // 设置数据源并触发导出
      this.failedExportDataSource = failedInfos;
      this.failedExportLang = lang;

      // console.log("把下载词条传递给exportButton", this.$refs.failedExportRef, this.failedExportDataSource);
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
      // 关闭后清空，避免保留上次数据
      this.failedExportDataSource = [];
    },
    // 按语种下载异常信息
    downloadExceptionInfosByLang(lang) {
      const exceptionVos = this.extractDataByLang(lang, 'exceptionVos', `没有${lang}的异常信息可下载`);
      if (!exceptionVos) return;

      downloadJsonFile(exceptionVos, `${lang}更新异常_`, false);
    },
    // 下载附件
    async downloadAttachment(downloadUrl, fileName) {
      if (!downloadUrl) {
        message.warning("下载链接不存在");
        return;
      }

      try {
        await downloadFileFromUrl({ logPath: downloadUrl }).then(res => {
          // console.log("下载链接", res)
          downloadBlobResponse(res, fileName);
        })
      } catch (error) {
        console.error("下载附件失败：", error);
        message.error("下载附件失败");
      }
    },

    // ==================== 用户偏好和UI辅助 ====================
    // 获取用户偏好
    queryBackfillFieldsPreference() {
      // 读取本地存储的用户偏好
      const storedPreferences = localStorage.getItem("backfillFieldsPref");
      // console.log("读取用户偏好", storedPreferences);
      if (storedPreferences) {
        const preferences = JSON.parse(storedPreferences);
        // 应用偏好-回填字段
        if (preferences.backfillFields != null && preferences.backfillFields != "") {
          this.formModel.backfillFields = preferences.backfillFields.split(",");
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
        // 应用偏好-校验语种（特殊字符校验与长度超限校验共用）
        if (preferences.translateAttrs != null && preferences.translateAttrs !== "") {
          this.formModel.translateAttrs = preferences.translateAttrs.split(",");
        }
      }
    },
    // 全选-回填字段
    selectAllBackfillFields() {
      this.formModel.backfillFields = this.languageOptions.map((item) => item.value);
    },
    // 全选-翻译列语种（两个规则共用）
    selectAllValidateTranslateAttrs() {
      this.formModel.translateAttrs = this.languageOptions.map((item) => item.value);
    },
  },
};
</script>

<style scoped lang="less">
.content {
  padding: 20px;
}

.validation-content,
.update-content {
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
