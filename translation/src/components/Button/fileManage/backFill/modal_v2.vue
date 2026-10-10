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
              :fieldNames="{ label: 'label', value: 'label' }" placeholder="请选择更新字段" allowClear
              style="flex: 1; margin-right: 8px;" />
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

          <!-- 校验选项共用：任一规则勾选时展示"翻译列语种" -->
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

  <!-- 校验结果模态框（v1.5同款） -->
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

  <!-- 失败信息模态框（仅用于导入/更新失败，不再包含校验相关内容） -->
  <CustomModal :visible="failedInfoVisible" modalTitle="失败信息" :showOk="false" @handleClose="failedInfoClose">
    <div class="content">
      <!-- 第一行：显示 globalMessage -->
      <div v-if="globalMessage" style="margin-bottom: 16px; color: #ff4d4f; font-weight: 500;">
        <a-alert :description="globalMessage" type="info" show-icon />
      </div>

      <!-- 导入模式：部分更新失败的词条下载 -->
      <div v-if="failedEntryInfos && failedEntryInfos.length > 0" style="margin-bottom: 12px;">
        <span style="margin-right: 8px;">部分更新失败的词条：</span>
        <a-button type="link" @click="downloadFailedEntries">下载</a-button>
      </div>

      <!-- 导入模式：全部异常信息下载 -->
      <div v-if="exceptionVos && exceptionVos.length > 0">
        <span style="margin-right: 8px;">全部异常信息：</span>
        <a-button type="link" @click="downloadExceptionInfos">下载</a-button>
      </div>
    </div>
  </CustomModal>

  <!-- 更新结果模态框 -->
  <CustomModal :visible="updateVisible && hasUpdateResult" modalTitle="更新结果" :showOk="false" @handleClose="updateClose">
    <div class="update-content">
      <!-- 多个语种时显示tab页（翻译更新接口） -->
      <template v-if="showUpdateTabs && detailsByLang && Object.keys(detailsByLang).length > 0">
        <a-tabs v-model:activeKey="activeFailedLangTab" type="card" class="failure-tabs">
          <a-tab-pane v-for="(detail, langKey) in detailsByLang" :key="langKey">
            <template #tab>
              <span style="display:inline-flex;align-items:center;gap:6px;">
                <span :style="{ color: (detail.code || 201) === 200 ? '#52c41a' : '#ff4d4f', fontWeight: 500 }">
                  {{ langKey }}
                </span>
              </span>
            </template>
            <!-- 单个语种的内容 -->
            <div>
              <!-- 更新结果的globalMessage -->
              <div v-if="detail.globalMessage"
                :style="{ marginBottom: '16px', color: (detail.code || 201) === 200 ? '#52c41a' : '#ff4d4f', fontWeight: 500 }">
                <a-alert :description="detail.globalMessage" :type="(detail.code || 201) === 200 ? 'success' : 'error'"
                  show-icon />
              </div>
              <!-- 更新结果的错误信息下载 -->
              <a-card class="failure-lang-card"
                v-if="(detail.code || 201) === 201 && (detail.exceptionVos && detail.exceptionVos.length > 0)"
                :bordered="true">
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
                  <div class="failure-item">
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
            </div>
          </a-tab-pane>
        </a-tabs>
      </template>

      <!-- 单个结果时不显示tab页，直接显示内容（通用更新接口） -->
      <template v-else-if="!showUpdateTabs && detailsByLang && Object.keys(detailsByLang).length > 0">
        <div v-for="(detail, langKey) in detailsByLang" :key="langKey">
          <div v-if="detail.globalMessage"
            :style="{ marginBottom: '16px', color: (detail.code || 201) === 200 ? '#52c41a' : '#ff4d4f', fontWeight: 500 }">
            <a-alert :description="detail.globalMessage" :type="(detail.code || 201) === 200 ? 'success' : 'error'"
              show-icon />
          </div>
          <a-card class="failure-lang-card"
            v-if="(detail.code || 201) === 201 && (detail.exceptionVos && detail.exceptionVos?.length > 0)"
            :bordered="true">
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
              <div class="failure-item">
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
        </div>
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
import { entryValidate_v2, updateEntryInfosUnified } from "@/utils/excelUtils";
import { downloadJsonFile, downloadBlobResponse } from "@/utils/fileUtils";
import { downloadFileFromUrl } from "@/http/api/download";
import commonParam, { entryParams } from "@/constants/commonParam.js";
import { setModalAriaHidden, stopDomEvent } from "@/utils/domUtils";
export default {
  name: "BackFillModal_v2",
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
        translateAttrs: [],       // 翻译列语种
      },
      loading: false,
      idMappingAccept: ".json",
      importTypes: [
        { label: "csv", value: "csv", accept: ".csv" },
        { label: "excel", value: "excel", accept: ".xls,.xlsx" },
      ],
      // 校验结果相关状态（v1.5同款结构）
      validationVisible: false, // 控制校验结果模态框显示
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
      // 更新结果相关状态
      updateVisible: false, // 控制更新结果模态框显示
      detailsByLang: {}, // 失败信息相关状态（按语种聚合）{ "英文": { code, globalMessage, failedEntryInfos, exceptionVos } }
      activeFailedLangTab: '', // 当前激活的失败语种标签
      failedExportDataSource: [], // 用于导出失败词条的数据源
      failedExportLang: "", // 用于导出失败词条的文件名前缀
      // 失败信息相关状态
      failedEntryInfos: [], // 可重试失败词条数组
      exceptionVos: [], // 异常信息数组
      globalMessage: "", // 总体错误提示信息
      failedInfoVisible: false, // 控制失败信息模态框显示
      // 更新/回填字段、校验字段
      fieldOptions: (entryParams && Array.isArray(entryParams.updateableFields)) ? entryParams.updateableFields : [],
      // 回填字段(后续拓展后同校验字段),翻译列语种（语种名称，来自公共常量,name/value）
      languageOptions: (commonParam && Array.isArray(commonParam.languageList)) ? commonParam.languageList : [],
      showUpdateTabs: false, // 是否显示tab页（多个语种时显示，单个结果时不显示）
      hasUpdateResult: false, // 是否有更新结果
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
              // 校验模式：调用handleValidation，显示校验结果模态框
              await this.handleValidation();
              // 等待新模态框渲染完成后再关闭loading
              this.$nextTick(() => {
                this.loading = false;
              });
              return; // 校验模式不继续执行后续逻辑
            } else {
              // updateTranslation 模式：使用统一更新函数
              // 注意：更新接口不需要去重前Excel，后端只需要去重后Excel和映射文件就能回填
              // 使用updateEntryInfosUnified自动判断调用翻译更新接口还是通用更新接口
              // 将backfillFields（label格式）转换为value格式
              const backfillFields = this.formModel.backfillFields || [];
              if (!Array.isArray(backfillFields) || backfillFields.length === 0) {
                throw new Error("请至少选择一个更新字段！");
              }

              const selectedFieldValues = backfillFields.map(label => {
                const field = this.fieldOptions.find(f => f.label === label);
                return field ? field.value : label;
              });

              result = await updateEntryInfosUnified(
                this.formModel.backFillFile,
                selectedFieldValues, // 转换为value格式的更新字段列表
                null, // formData由函数内部构建
                {
                  mappingJson: this.needRelationFile ? this.formModel.relationFile : null
                }
              );
              // 保存用户偏好
              this.updateBackfillFieldsPreference();

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

            // 导入模式(更新)：尝试处理错误响应
            console.log("errorData导入模式(更新)：", errorData);
            this.handleImportResponse(errorData);
            this.handleCloseInternal();
          } finally {
            this.loading = false;
          }
        })
        .catch((err) => {
          console.log("表单校验失败", err);
        });
    },

    // 处理校验（v1.5同款逻辑，字段名适配v2）
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
          this.formModel.originExcel,                  // v2字段：originExcel（去重前Excel）
          this.formModel.backFillFile,                 // v2字段：backFillFile（去重后Excel）
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
        console.error("校验过程发生异常：", error);
        notification.error({
          message: "校验过程发生异常！",
          description: error?.response?.data?.message || error?.message || "未知错误",
          duration: 0,
        });
        throw error; // 重新抛出错误，让调用方知道校验失败
      }
    },

    // 处理继续回填按钮点击（v1.5同款）
    async handleContinueBackFill() {
      this.loading = true;
      try {
        // 将backfillFields（label格式）转换为value格式
        const backfillFields = this.formModel.backfillFields || [];
        if (!Array.isArray(backfillFields) || backfillFields.length === 0) {
          throw new Error("请至少选择一个更新字段！");
        }

        const selectedFieldValues = backfillFields.map(label => {
          const field = this.fieldOptions.find(f => f.label === label);
          return field ? field.value : label;
        });

        const result = await updateEntryInfosUnified(
          this.formModel.backFillFile,
          selectedFieldValues, // 转换为value格式的更新字段列表
          null, // formData由函数内部构建
          {
            mappingJson: this.needRelationFile ? this.formModel.relationFile : null
          }
        );

        // 保存用户偏好
        this.updateBackfillFieldsPreference();

        // 处理导入结果
        this.handleImportResponse(result);

        // 按钮模式需要 emit importSuccess 事件
        if (this.mode === "button") {
          this.$emit("importSuccess");
        }

        // 关闭校验结果模态框
        this.validationClose();

        // 等待新模态框渲染完成后再关闭loading
        this.$nextTick(() => {
          this.loading = false;
        });
      } catch (error) {
        console.error("继续回填过程发生异常：", error);
        notification.error({
          message: "继续回填过程发生异常！",
          description: error?.response?.data?.message || error?.message || "未知错误",
          duration: 0,
        });
        // 错误处理：确保 loading 被关闭
        this.loading = false;
        throw error; // 重新抛出错误，让调用方知道更新失败
      }
    },

    // 处理校验响应（旧方法，保留用于兼容）
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

      // 判断是翻译更新接口的返回（有 success 和 failed）还是通用更新接口的返回（只有 code 和 data）
      const hasSuccessField = result.success !== undefined;
      const hasFailedField = result.failed !== undefined;

      if (hasSuccessField && hasFailedField) {
        // 翻译更新接口的返回结构（entryBatchImportExcel）
        if (result.code === 200) {
          // 完全成功：构建按语种分组的结果
          const successLangs = result.success || [];
          const detailsByLang = {};

          // 将成功语种转换为语种名称
          successLangs.forEach(langValue => {
            const lang = this.languageOptions.find(l => l.value === langValue);
            const langName = lang ? lang.name : langValue;
            detailsByLang[langName] = {
              code: 200,
              globalMessage: `${langName} 更新成功！`,
              failedEntryInfos: [],
              exceptionVos: [],
            };
          });

          if (Object.keys(detailsByLang).length > 0) {
            // 显示更新结果模态框（多个语种，显示tab页）
            this.detailsByLang = detailsByLang;
            this.activeFailedLangTab = Object.keys(detailsByLang)[0];
            this.showUpdateTabs = true; // 多个语种，显示tab页
            this.updateVisible = true;
            this.hasUpdateResult = true;
          } else {
            // 没有成功语种，仅展示成功通知
            this.hasUpdateResult = false;
            const desc = "导入成功！";
            notifyTask = () => {
              notification.success({
                message: "导入成功！",
                description: desc,
                duration: 0,
              });
            };
          }
        } else if (result.code === 201) {
          // code 201 时，需要按语种分组展示
          const successLangs = result.success || [];
          const failedMap = result.failed || new Map();
          const allFailedEntryInfos = result.failedEntryInfos || [];
          const allExceptionVos = result.exceptionVos || [];
          const globalMessage = result.globalMessage || "";
          this.hasUpdateResult = true;

          const detailsByLang = {};
          let firstFailedLang = "";

          // 处理成功语种
          successLangs.forEach(langValue => {
            const lang = this.languageOptions.find(l => l.value === langValue);
            const langName = lang ? lang.name : langValue;
            detailsByLang[langName] = {
              code: 200,
              globalMessage: `${langName} 更新成功！`,
              failedEntryInfos: [],
              exceptionVos: [],
            };
          });

          // 处理失败语种：从 failed Map 中提取
          failedMap.forEach((langValues, errMsg) => {
            langValues.forEach(langValue => {
              const lang = this.languageOptions.find(l => l.value === langValue);
              const langName = lang ? lang.name : langValue;

              // 如果该语种还没有在 detailsByLang 中，则添加
              if (!detailsByLang[langName]) {
                detailsByLang[langName] = {
                  code: 201,
                  globalMessage: errMsg || globalMessage || `${langName} 更新存在失败或异常信息`,
                  failedEntryInfos: [],
                  exceptionVos: [],
                };

                if (!firstFailedLang) {
                  firstFailedLang = langName;
                }
              } else {
                // 如果已存在，更新 globalMessage（合并错误信息）
                if (detailsByLang[langName].code === 200) {
                  detailsByLang[langName].code = 201;
                  detailsByLang[langName].globalMessage = errMsg || globalMessage || `${langName} 更新存在失败或异常信息`;
                }
              }
            });
          });

          // 将全局的失败信息分配到各个语种（这里简化处理，将所有失败信息都显示在每个失败语种下）
          // 注意：entryBatchImportExcel 返回的 failedEntryInfos 和 exceptionVos 是所有语种合并的
          // 无法精确分配到每个语种，所以这里简化处理
          Object.keys(detailsByLang).forEach(langName => {
            if (detailsByLang[langName].code === 201) {
              // 对于失败的语种，分配失败信息
              detailsByLang[langName].failedEntryInfos = allFailedEntryInfos;
              detailsByLang[langName].exceptionVos = allExceptionVos;
            }
          });

          if (Object.keys(detailsByLang).length > 0) {
            // 显示更新结果模态框（多个语种，显示tab页）
            this.detailsByLang = detailsByLang;
            this.activeFailedLangTab = firstFailedLang || Object.keys(detailsByLang)[0];
            this.showUpdateTabs = true; // 多个语种，显示tab页
            this.updateVisible = true;
          } else {
            // 没有可展示的语种详情：用通知给出总体说明
            const desc = globalMessage || "导入存在失败或异常信息";
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
          this.hasUpdateResult = true;
          const desc = result.globalMessage || "导入失败";
          notifyTask = () => {
            notification.error({
              message: "导入失败",
              description: desc,
              duration: 0,
            });
          };
        }
      } else {
        // 通用更新接口的返回结构（updateEntryInfosByFile）
        const failedEntryInfos = result.failedEntryInfos || result.failedEntryInfos || [];
        const exceptionVos = result.exceptionVos || result.exceptionVOs || [];
        const globalMessage = result.globalMessage || "";
        const code = exceptionVos.length > 0 ? 201 : 200;

        // 通用更新接口：统一显示在更新结果模态框中（单个结果，不显示tab页）
        const detailsByLang = {
          "更新结果": {
            code: code,
            globalMessage: globalMessage || (code === 200 ? "更新成功！" : "更新存在失败或异常信息"),
            failedEntryInfos: failedEntryInfos,
            exceptionVos: exceptionVos,
          }
        };
        console.log("detailsByLang", detailsByLang);

        // 显示在更新结果模态框中
        this.detailsByLang = detailsByLang;
        this.activeFailedLangTab = "更新结果";
        this.showUpdateTabs = false; // 单个结果，不显示tab页
        this.updateVisible = true;
        // 若更新成功，没有更新结果，则不显示模态框
        this.hasUpdateResult = failedEntryInfos.length > 0 || exceptionVos.length > 0;
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
      // 重置校验结果相关状态（v1.5同款结构）
      this.validationVisible = false;
      this.validation = {
        summary: null,
        issues: [],
        attachments: { issueLog: [], invalidExcel: null },
        canBackFill: false,
      };
      // 重置更新结果相关状态
      this.updateVisible = false;
      this.detailsByLang = {};
      this.activeFailedLangTab = '';
      this.failedExportLang = "";
      this.showUpdateTabs = false;
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
    // 关闭失败信息模态框
    failedInfoClose() {
      this.failedInfoVisible = false;
      // 重置失败信息相关状态
      this.failedEntryInfos = [];
      this.exceptionVos = [];
      this.globalMessage = "";
      this.failedExportDataSource = [];
    },
    // 关闭更新结果模态框
    updateClose() {
      this.updateVisible = false;
      // 重置更新结果相关状态
      this.detailsByLang = {};
      this.failedExportDataSource = [];
      this.activeFailedLangTab = '';
      this.failedExportLang = "";
      this.showUpdateTabs = false;
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
        notification.error({
          message: "下载失败",
          description: "没有可导出的失败词条数据",
        });
        return;
      }

      // 通过 ref 调用隐藏的 ExportButton 的 showExportModal 方法
      if (this.$refs.failedExportRef) {
        this.$refs.failedExportRef.showExportModal();
      } else {
        notification.error({
          message: "下载失败",
          description: "导出功能初始化失败",
        });
      }
    },
    // 下载全部异常信息
    downloadExceptionInfos() {
      if (this.exceptionVos.length === 0) {
        notification.error({
          message: "下载失败",
          description: "没有异常信息可下载",
        });
        return;
      }

      downloadJsonFile(this.exceptionVos, "entry_import_exceptions", false);
    },
    // ==================== 按语种下载相关 ====================
    // 辅助方法：从 detailsByLang 中提取指定语种和字段的数据，并验证是否为空
    extractDataByLang(lang, fieldName, emptyMessage) {
      const data = (this.detailsByLang[lang] && this.detailsByLang[lang][fieldName]) || [];
      if (data.length === 0) {
        notification.error({
          message: "下载失败",
          description: emptyMessage,
        });
        return null;
      }
      return data;
    },
    // 按语种下载失败词条
    downloadFailedEntriesByLang(lang) {
      const failedInfos = this.extractDataByLang(lang, 'failedEntryInfos', `没有${lang}的失败词条数据可导出`);
      if (!failedInfos) return;

      // 设置数据源并触发导出
      this.failedExportDataSource = failedInfos;
      this.failedExportLang = lang;

      if (this.$refs.failedExportRef) {
        // 等待 prop 更新到子组件后再打开弹窗，否则子组件可能读到旧的空数组
        this.$nextTick(() => {
          this.$refs.failedExportRef.showExportModal();
        });
      } else {
        notification.error({
          message: "下载失败",
          description: "导出功能初始化失败",
        });
      }
    },
    handleFailedExportAfterClose() {
      // 关闭后清空，避免保留上次数据
      this.failedExportDataSource = [];
      this.failedExportLang = "";
    },
    // 按语种下载异常信息
    downloadExceptionInfosByLang(lang) {
      const exceptionVos = this.extractDataByLang(lang, 'exceptionVos', `没有${lang}的异常信息可下载`);
      if (!exceptionVos) return;

      const prefix = commonParam.langNameList.includes(lang) ? `${lang}更新异常_` : "更新异常_";
      downloadJsonFile(exceptionVos, prefix, false);
    },

    // 下载校验问题JSON
    downloadValidationIssues() {
      if (this.validationIssues.length === 0) {
        notification.error({
          message: "下载失败",
          description: "没有校验问题可下载",
        });
        return;
      }

      downloadJsonFile(this.validationIssues, "backfill_validation_issues", false);
    },

    // 下载附件
    async downloadAttachment(downloadUrl, fileName) {
      if (!downloadUrl) {
        notification.error({
          message: "下载失败",
          description: "下载链接不存在",
        });
        return;
      }

      try {
        const response = await downloadFileFromUrl(downloadUrl);
        downloadBlobResponse(response, fileName);
      } catch (error) {
        console.error("下载附件失败：", error);
        notification.error({
          message: "下载失败",
          description: "下载附件失败",
        });
      }
    },
    // 获取用户偏好（v1.5同款localStorage方式）
    queryBackfillFieldsPreference() {
      // 读取本地存储的用户偏好
      const storedPreferences = localStorage.getItem("backfillFieldsPref");
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
    // 保存用户偏好（v1.5同款localStorage方式）
    updateBackfillFieldsPreference() {
      let data = {
        backfillFields: (this.formModel.backfillFields || []).join(","),
        emptyStringAsValue: this.formModel.emptyStringAsValue,
        failFast: this.formModel.failFast,
        enableValidate: this.formModel.enableValidate,
        checkFields: (this.formModel.checkFields || []).join(","),
        checkSpecialChar: this.formModel.checkSpecialChar,
        checkMaxLength: this.formModel.checkMaxLength,
        translateAttrs: (this.formModel.translateAttrs || []).join(","),
      };
      localStorage.setItem("backfillFieldsPref", JSON.stringify(data));
    },
    // 全选更新字段方法
    selectAllBackfillFields() {
      this.formModel.backfillFields = this.fieldOptions.map((item) => item.label);
    },
    // 全选-翻译列语种（校验选项使用）
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
