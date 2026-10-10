<template>
  <div class="file-select-with-encoding">
    <!-- 文件选择区 -->
    <template v-if="showFileSelect">
      <a-input
        v-if="showPathInput"
        :value="displayPath"
        :style="pathInputStyle"
        :size="size"
        :placeholder="pathPlaceholder"
        readonly
      />
      <a-upload
        name="file"
        :beforeUpload="onBeforeUpload"
        :accept="accept"
        :showUploadList="showUploadList"
        :max-count="maxCount"
        :fileList="innerFileList"
        :disabled="disabled"
        @change="onUploadChange"
        @remove="onRemove"
      >
        <a-button type="primary" :size="size" :style="showPathInput ? { marginLeft: '8px' } : null" :loading="loading">
          <template v-if="buttonIcon" #icon>
            <component :is="buttonIcon" />
          </template>
          {{ buttonText }}
        </a-button>
      </a-upload>
    </template>

    <!-- 编码选择：锁定时始终展示；否则仅 CSV 时展示 -->
    <template v-if="showEncoding">
      <span v-if="showEncodingLabel" class="encoding-label">文件编码：</span>
      <a-select
        :value="effectiveEncoding"
        :options="encodingOptions"
        :size="size"
        :disabled="disabled || encodingLocked"
        class="encoding-select"
        @update:value="onEncodingChange"
      />
      <a-tooltip placement="top" :overlayStyle="{ maxWidth: '360px' }">
        <template #title>
          <span class="encoding-tip-text">{{ encodingTip }}</span>
        </template>
        <QuestionCircleOutlined class="encoding-tip-icon" />
      </a-tooltip>
    </template>

    <!-- 非 CSV 也可单独展示 tips（如仅提示、不选编码） -->
    <a-tooltip
      v-else-if="alwaysShowTip"
      placement="top"
      :overlayStyle="{ maxWidth: '360px' }"
    >
      <template #title>
        <span class="encoding-tip-text">{{ encodingTip }}</span>
      </template>
      <QuestionCircleOutlined class="encoding-tip-icon" :style="showFileSelect ? { marginLeft: '8px' } : null" />
    </a-tooltip>
  </div>
</template>

<script>
import { QuestionCircleOutlined } from "@ant-design/icons-vue";
import { notification } from "ant-design-vue";
import {
  DEFAULT_ENCODING,
  ENCODING_OPTIONS,
  ENCODING_TIP,
  CSV_ONLY_ACCEPT,
  assertAcceptExtension,
  shouldShowEncoding,
} from "./constants";

export default {
  name: "FileSelectWithEncoding",
  components: {
    QuestionCircleOutlined,
  },
  props: {
    /** 编码值，支持 v-model:encoding */
    encoding: {
      type: String,
      default: DEFAULT_ENCODING,
    },
    /** 文件路径展示（只读输入框），支持 v-model:filePath */
    filePath: {
      type: String,
      default: "",
    },
    /** 外部指定文件类型（如 BackFill 的 importType：'csv' | 'excel'） */
    fileType: {
      type: String,
      default: null,
    },
    accept: {
      type: String,
      default: CSV_ONLY_ACCEPT,
    },
    /** 是否展示文件选择（false 时仅编码+tips，供已有 upload 的场景复用） */
    showFileSelect: {
      type: Boolean,
      default: true,
    },
    /** 是否展示「文件编码：」文案（表单项已有 label 时可关闭） */
    showEncodingLabel: {
      type: Boolean,
      default: true,
    },
    /** 是否展示编码下拉（false 时隐藏，由外部强制 encoding） */
    showEncodingSelect: {
      type: Boolean,
      default: true,
    },
    /**
     * 编码灰禁：始终展示编码下拉且不可改，值锁为 UTF-8
     * （ENCODING_OPTIONS 仍含 GBK，但永不启用）
     */
    encodingLocked: {
      type: Boolean,
      default: true,
    },
    showPathInput: {
      type: Boolean,
      default: false,
    },
    pathPlaceholder: {
      type: String,
      default: "请选择文件",
    },
    pathInputStyle: {
      type: [String, Object],
      default: () => ({ width: "40%" }),
    },
    showUploadList: {
      type: Boolean,
      default: false,
    },
    maxCount: {
      type: Number,
      default: 1,
    },
    fileList: {
      type: Array,
      default: () => [],
    },
    buttonText: {
      type: String,
      default: "选择文件",
    },
    /** 可选：传入图标组件 */
    buttonIcon: {
      type: [Object, Function],
      default: null,
    },
    size: {
      type: String,
      default: "small",
    },
    loading: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    /** 非 CSV 时是否仍展示 tips 图标 */
    alwaysShowTip: {
      type: Boolean,
      default: false,
    },
    /** 选文件时是否按 accept 校验扩展名并 notification */
    validateAcceptOnSelect: {
      type: Boolean,
      default: true,
    },
    beforeUpload: {
      type: Function,
      default: null,
    },
  },
  emits: [
    "update:encoding",
    "update:filePath",
    "change",
    "remove",
    "fileChange",
  ],
  data() {
    return {
      encodingOptions: ENCODING_OPTIONS,
      encodingTip: ENCODING_TIP,
      selectedFileName: "",
    };
  },
  computed: {
    /**
     * 路径输入框展示值（优先外部 filePath）
     * @returns {string}
     */
    displayPath() {
      return this.filePath || this.selectedFileName || "";
    },
    /**
     * 锁定时强制 UTF-8
     * @returns {string}
     */
    effectiveEncoding() {
      return this.encodingLocked ? DEFAULT_ENCODING : this.encoding;
    },
    /**
     * 是否展示编码选择
     * @returns {boolean}
     */
    showEncoding() {
      if (!this.showEncodingSelect) return false;
      if (this.encodingLocked) return true;
      return shouldShowEncoding({
        accept: this.accept,
        fileName: this.selectedFileName || this.filePath,
        fileType: this.fileType,
      });
    },
    /**
     * 透传给 a-upload 的 fileList
     * @returns {Array}
     */
    innerFileList() {
      return this.fileList || [];
    },
  },
  watch: {
    /**
     * 外部清空路径时同步清空内部文件名
     * @param {string} val
     */
    filePath(val) {
      if (!val) {
        this.selectedFileName = "";
      }
    },
    /**
     * 外部 fileList 变化时同步 selectedFileName
     * @param {Array} list
     */
    fileList(list) {
      if (!list || list.length === 0) {
        this.selectedFileName = "";
      } else if (list[0]?.name) {
        this.selectedFileName = list[0].name;
      }
    },
    encodingLocked: {
      immediate: true,
      handler(locked) {
        if (locked && this.encoding !== DEFAULT_ENCODING) {
          this.$emit("update:encoding", DEFAULT_ENCODING);
        }
      },
    },
  },
  methods: {
    /**
     * Upload beforeUpload 代理：先按 accept 校验，再调外部钩子
     * @param {File} file
     * @param {File[]} fileList
     * @returns {boolean|Promise}
     */
    onBeforeUpload(file, fileList) {
      if (this.validateAcceptOnSelect) {
        const { ok, message } = assertAcceptExtension(file?.name, this.accept);
        if (!ok) {
          notification.error({
            message: "文件类型不匹配",
            description: message,
          });
          return false;
        }
      }
      if (typeof this.beforeUpload === "function") {
        return this.beforeUpload(file, fileList);
      }
      return false;
    },
    /**
     * 文件选择变更：同步路径/文件名并向上抛出
     * @param {Object} info - ant-design-vue Upload change 事件对象
     * @returns {void}
     */
    onUploadChange(info) {
      const file = info?.file?.originFileObj || info?.file;
      if (file) {
        this.selectedFileName = file.name || "";
        const path = file.path || file.name || "";
        this.$emit("update:filePath", path);
        this.$emit("fileChange", file);
      }
      this.$emit("change", info);
    },
    /**
     * 移除已选文件
     * @param {File} file
     * @returns {void}
     */
    onRemove(file) {
      this.selectedFileName = "";
      this.$emit("update:filePath", "");
      this.$emit("remove", file);
    },
    /**
     * 编码下拉变更（锁定时忽略）
     * @param {string} val - 编码值（UTF-8 / GBK）
     * @returns {void}
     */
    onEncodingChange(val) {
      if (this.encodingLocked) return;
      this.$emit("update:encoding", val);
    },
  },
};
</script>

<style scoped>
.file-select-with-encoding {
  display: inline-flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 4px;
  max-width: 100%;
}
.file-select-with-encoding :deep(.ant-upload) {
  display: inline-flex;
  flex-shrink: 0;
}
.encoding-label {
  white-space: nowrap;
  flex-shrink: 0;
  margin-left: 8px;
}
.encoding-select {
  width: 90px;
  flex-shrink: 0;
}
.encoding-tip-icon {
  color: #369fff;
  margin-left: 4px;
  cursor: pointer;
  font-size: 14px;
  flex-shrink: 0;
}
</style>

<style>
/* tooltip 挂到 body，需非 scoped 才能让 \n 换行生效 */
.encoding-tip-text {
  white-space: pre-line;
}
</style>
