<template>
  <a-row
    v-if="showFileType || showEncoding"
    class="locked-csv-import-meta"
    :gutter="gutter"
    type="flex"
    align="middle"
  >
    <a-col v-if="showFileType" :span="showEncoding ? fileTypeSpan : 24">
      <a-form-item
        v-if="asFormItems"
        :label="fileTypeLabel"
        :name="fileTypeName"
        :rules="fileTypeRules"
      >
        <a-select
          :value="LOCKED_IMPORT_TYPE"
          :options="importTypeOptions"
          :size="size"
          disabled
          :style="selectStyle"
        />
      </a-form-item>
      <div v-else class="locked-meta-inline">
        <span v-if="showInlineLabels" class="locked-meta-label">{{ fileTypeLabel }}：</span>
        <a-select
          :value="LOCKED_IMPORT_TYPE"
          :options="importTypeOptions"
          :size="size"
          disabled
          :style="selectStyle"
        />
      </div>
    </a-col>
    <a-col v-if="showEncoding" :span="showFileType ? encodingSpan : 24">
      <a-form-item v-if="asFormItems" :label="encodingLabel" :name="encodingName">
        <FileSelectWithEncoding
          :encoding="DEFAULT_ENCODING"
          :file-type="LOCKED_IMPORT_TYPE"
          :accept="accept"
          :show-file-select="false"
          :show-encoding-label="false"
          :encoding-locked="true"
          :size="size"
        />
      </a-form-item>
      <FileSelectWithEncoding
        v-else
        :encoding="DEFAULT_ENCODING"
        :file-type="LOCKED_IMPORT_TYPE"
        :accept="accept"
        :show-file-select="false"
        :show-encoding-label="showInlineLabels"
        :encoding-locked="true"
        :size="size"
      />
    </a-col>
  </a-row>
</template>

<script>
import FileSelectWithEncoding from "@/components/FileSelectWithEncoding/index.vue";
import {
  DEFAULT_ENCODING,
  ENCODING_OPTIONS,
  CSV_ONLY_ACCEPT,
  LOCKED_IMPORT_TYPE,
} from "@/utils/csvEncodingConstants";

/**
 * 灰禁展示「文件类型=csv」+「文件编码=UTF-8」
 * ENCODING_OPTIONS 仍含 GBK，但下拉 disabled 永不启用。
 */
export default {
  name: "LockedCsvImportMeta",
  components: {
    FileSelectWithEncoding,
  },
  props: {
    /** 是否展示文件类型列 */
    showFileType: {
      type: Boolean,
      default: true,
    },
    /** 是否展示编码列 */
    showEncoding: {
      type: Boolean,
      default: true,
    },
    /** 以 a-form-item 包裹（回填弹窗） */
    asFormItems: {
      type: Boolean,
      default: true,
    },
    /** 非 form 模式下是否显示「文件类型：」等行内 label */
    showInlineLabels: {
      type: Boolean,
      default: true,
    },
    accept: {
      type: String,
      default: CSV_ONLY_ACCEPT,
    },
    size: {
      type: String,
      default: "middle",
    },
    gutter: {
      type: Number,
      default: 16,
    },
    fileTypeSpan: {
      type: Number,
      default: 12,
    },
    encodingSpan: {
      type: Number,
      default: 12,
    },
    fileTypeLabel: {
      type: String,
      default: "文件类型",
    },
    encodingLabel: {
      type: String,
      default: "文件编码",
    },
    fileTypeName: {
      type: String,
      default: "importType",
    },
    encodingName: {
      type: String,
      default: "encoding",
    },
    selectStyle: {
      type: [String, Object],
      default: () => ({ width: "160px" }),
    },
  },
  data() {
    return {
      DEFAULT_ENCODING,
      LOCKED_IMPORT_TYPE,
      encodingOptions: ENCODING_OPTIONS,
      importTypeOptions: [
        { label: "csv", value: "csv", accept: CSV_ONLY_ACCEPT },
        { label: "excel", value: "excel", accept: ".xls,.xlsx" },
      ],
      fileTypeRules: [{ required: true, message: "请选择!" }],
    };
  },
};
</script>

<style scoped>
.locked-csv-import-meta {
  width: 100%;
}
.locked-meta-inline {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.locked-meta-label {
  white-space: nowrap;
  flex-shrink: 0;
}
</style>
