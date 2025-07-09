<template>
  <a-button type="primary" @click="showExportModal" :size="size">导出</a-button>

  <CustomModal :okLoading="exportLoading" modalTitle="导出" modalWidth="500px" :modalVisible="exportVisible" @handleClose="operateClose"
    @handleOK="operateOk" @afterClose="afterOperateClose">
    <a-form ref="formRef" :model="exportModal">
      <a-form-item label="文件类型" name="exportType" :rules="[{ required: true, message: '请选择文件类型!' }]">
        <a-select v-model="exportModal.fileType" @change="handleFileTypeChange">
          <a-select-option value="excel">Excel</a-select-option>
          <a-select-option value="xml">XML</a-select-option>
          <a-select-option value="csv">CSV</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="导出字段" name="field" :rules="[{ required: true, message: '请选择导出字段!' }]" v-if="showFieldSelection">
        <a-select mode="multiple" v-model="exportClass.field" :options="fieldOptions" :fieldNames="{ label: 'label', value: 'label' }"
          placeholder="请选择" allowClear />
      </a-form-item>
    </a-form>
  </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import { entryExportByCondition } from "@/http/api/download";

export default {
  components: {
    CustomModal,
  },
  props: {
    dataSource: {
      type: Array,
      required: true,
    },
    fieldOptions: {
      type: Array,
      required: true,
    },
    size: {
      type: String,
      default: "small",
    },
  },
  data() {
    return {
      exportVisible: false,
      exportLoading: false,
      exportModal: {
        fileType: "",
        field: [],
      },
      showFieldSelection: true,
      exportLoading: false,
      exportClass: {
        field: ["abbr", "词条"],
      },
      // fieldOptions: tableParam.exportFields,
    };
  },
  watch: {
    showFieldSelection(newVal) {
      if (newVal) {
        this.rules.field = [{ required: true, message: "请选择导出字段!" }];
      } else {
        this.rules.field = [];
      }
    },
  },
  methods: {
    showExportModal() {
      this.exportVisible = true;
      this.exportModal.fileType = "";
      this.exportModal.field = [];
      this.showFieldSelection = true;
    },
    handleFileTypeChange(value) {
      if (value === "xml") {
        this.showFieldSelection = false;
        this.exportModal.field = [
          "词条",
          "英文翻译",
          "俄文翻译",
          "西文翻译",
          "法文翻译",
        ];
      } else {
        this.showFieldSelection = true;
        this.exportModal.field = [];
      }
    },
    async handleExport() {
      try {
        this.exportLoading = true;
        await this.$refs.formRef.validate();
        const { fileType, field } = this.exportModal;
        let data = {};
        let params = {};

        if (fileType === "xml") {
          // 对词条去重
          const uniqueEntries = [];
          const entrySet = new Set();
          this.dataSource.forEach((item) => {
            if (!entrySet.has(item.entry)) {
              entrySet.add(item.entry);
              uniqueEntries.push(item);
            }
          });
          // 手动构建 XML 字符串
          let xml = `<?xml version="1.0" encoding="UTF-8"?>\n<DICT local_language="0">\n`;
          uniqueEntries.forEach((item) => {
            let abbr = item.abbr != null ? item.abbr : "";
            let cn_desc = item.entry != null ? item.entry : "";
            let en_desc = item.english != null ? item.english : "";
            let local_desc = item.entry != null ? item.entry : "";
            let es_desc = item.spanish != null ? item.spanish : "";
            let ru_desc = item.russian != null ? item.russian : "";

            xml += `\t<ITEM abbr="${abbr}" cn_desc="${cn_desc}" en_desc="${en_desc}" local_desc="${en_desc}" es_desc="${es_desc}" ru_desc="${ru_desc}" />\n`;
          });
          xml += `</DICT>`;

          // 导出 XML 文件
          const blob = new Blob([xml], { type: "application/xml" });
          const url = URL.createObjectURL(blob);
          const link = document.createElement("a");
          link.href = url;
          link.download = "sysdict.xml";

          link.click();
          URL.revokeObjectURL(url);
        } else {
          let fields = ["id"].concat(field);
          data = {
            columnNames: fields,
            entryInfoEntities: this.dataSource,
            excelName: "词条导出",
          };
          const res = await entryExportByCondition(data, params);
          let fileName = res.headers["content-disposition"]
            .split(";")[1]
            .split("filename=")[1];
          if (fileType === "csv") {
            fileName = fileName.split(".")[0] + ".csv";
          }
          let contentType =
            fileType === "csv"
              ? "text/csv;charset=utf-8"
              : res.headers["content-type"];
          const blob = new Blob([res.data], { type: contentType });
          const a = document.createElement("a");
          a.download = decodeURI(fileName);
          a.href = window.URL.createObjectURL(blob);
          a.click();
          a.remove();
          window.URL.revokeObjectURL(a.href);
        }
        this.exportVisible = false;
      } catch (err) {
        message.error(`导出失败: ${err.message || err}`);
      } finally {
        this.exportLoading = false;
      }
    },
  },
};
</script>