<template>
  <a-button type="primary" @click="showExportModal" :size="size">导出</a-button>

  <CustomModal :okLoading="exportLoading" modalTitle="导出" width="500px" :visible="exportVisible" @handleClose="handleClose" @handleOK="handleOK">
    <div class="content">
      <a-form ref="exportForm" :model="exportModal">
        <a-form-item label="文件类型" name="exportType" :rules="[{ required: true, message: '请选择!' }]">
          <a-select v-model:value="exportModal.exportType" placeholder="请选择文件类型" :options='exportTypes' @change="exportTypeChange" allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="导出字段" name="field" v-if="exportModal.exportType !== 'xml'" :rules="[{ required: true, message: '请选择!' }]">
          <a-select mode="multiple" v-model:value="exportModal.field" :options="fieldOptions" :fieldNames="{ label: 'label', value: 'label' }"
            placeholder="请选择导出字段" :disabled="exportModal.exportType === 'xml'" allowClear />
        </a-form-item>
        <a-form-item label="指定local语言" name="local_desc" v-if="exportModal.exportType === 'xml'" :rules="[{ required: true, message: '请选择!' }]">
          <a-select v-model:value="exportModal.local_desc" placeholder="请选择语言" :options='localDescOptions' allowClear>
          </a-select>
        </a-form-item>
      </a-form>
    </div>
  </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import { entryExportByCondition } from "@/http/api/download";
import {
  queryUserPartiality,
  updateUserPartiality,
} from "@/http/api/userPartiality";
import { workbenchParams } from "@/utils/commonParam.js";
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
        exportType: null,
        field: [],
        local_desc: null,
      },
      exportTypes: [
        { label: "excel", value: "excel" },
        { label: "csv", value: "csv" },
        { label: "xml", value: "xml" },
      ],
      localDescOptions: workbenchParams.languageList.map((item) => ({
        label: item.language,
        value: item.code,
      })),
      // fieldOptions: tableParam.exportFields,
    };
  },
  methods: {
    showExportModal() {
      this.exportVisible = true;
      console.log("local", this.localDescOptions);
      console.log("获取用户偏好前：导出字段", this.exportModal.field);
      // 获取用户偏好
      queryUserPartiality().then((res) => {
        if (res.data.list && res.data.list.length > 0) {
          console.log("获取用户偏好！！！", res.data.list);
          let exportColumn = res.data.list[0].exportColumn;
          if (exportColumn != null && exportColumn != "") {
            this.exportModal.field = exportColumn.split(",");
          }
        }
      });
      console.log("获取用户偏好后：导出字段", this.exportModal.field);
    },
    exportTypeChange(value) {
      if (value === "xml") {
        // 找到“词条”与“翻译”相关字段
        const translationRegex = /翻译$/; // 匹配以“翻译”结尾的字段
        const translationFields = this.fieldOptions
          .filter((item) => translationRegex.test(item.label))
          .map((item) => item.label);
        // 添加上“词条”字段
        this.exportModal.field = ["词条", ...translationFields];
        console.log("锁定导出字段", this.exportModal.field);
      }
    },
    async handleOK() {
      if (!this.exportModal.exportType) {
        message.error("请选择导出的文件类型！");
        return;
      }
      if (this.exportModal.exportType != "xml" && (!this.exportModal.field || this.exportModal.field.length === 0)) {
        message.error("请选择导出字段！");
        return;
      }
      if(this.exportModal.exportType === "xml" && !this.exportModal.local_desc) {
        message.error("请选择指定local语言！");
        return;
      }

      try {
        this.exportLoading = true;
        await this.$refs.exportForm.validate();
        const { exportType, field } = this.exportModal;
        let data = {};
        let params = {};

        if (exportType === "xml") {
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
            let abbr = item.entry != null ? item.entry : ""; // 装置部需求
            let cn_desc = item.chinese != null ? item.chinese : ""; // 装置部需求
            let en_desc = item.english != null ? item.english : "";
            let local_desc =
              item[this.exportModal.local_desc] != null
                ? item[this.exportModal.local_desc]
                : "";
            console.log(
              "当前选中的local:",
              this.exportModal.local_desc,
              item,
              item[this.exportModal.local_desc]
            );
            let es_desc = item.spanish != null ? item.spanish : "";
            let ru_desc = item.russian != null ? item.russian : "";

            xml += `\t<ITEM abbr="${abbr}" cn_desc="${cn_desc}" en_desc="${en_desc}" local_desc="${local_desc}" es_desc="${es_desc}" ru_desc="${ru_desc}" />\n`;
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
        } else if (exportType === "excel" || exportType === "csv") {
          this.$refs.exportForm.validate().then(() => {
            // 导出接口
            let fields = ["id"].concat(field);
            data = {
              columnNames: fields,
              entryInfoEntities: this.dataSource,
              excelName: "词条导出",
            };
            entryExportByCondition(data, params).then((res) => {
              let fileName = res.headers["content-disposition"]
                .split(";")[1]
                .split("filename=")[1];
              // csv与excel不同的地方
              if (exportType === "csv") {
                fileName = fileName.split(".")[0] + ".csv";
              }
              let contentType =
                exportType === "csv"
                  ? "text/csv;charset=utf-8"
                  : res.headers["content-type"];

              const blob = new Blob([res.data], { type: contentType });
              const a = document.createElement("a");
              a.download = decodeURI(fileName);
              a.href = window.URL.createObjectURL(blob);
              a.click();
              a.remove();
              window.URL.revokeObjectURL(a.href);
            });
          });
        }
      } catch (err) {
        message.error(`导出失败: ${err.message || err}`);
      } finally {
        this.$emit("operateClose");
        // // 表示让父组件执行关闭，使得父组件的父组件执行相关代码
        // this.$emit("createClose");
        // this.$emit("cancelCreate");
        this.exportVisible = false;
        this.exportLoading = false;
      }
      // 记录偏好
      this.exportFieldChange(this.exportModal.field);
    },
    // 记录用户偏好
    exportFieldChange(value) {
      let data = {
        exportColumn: value.join(","),
      };
      updateUserPartiality(data).then((res) => {});
    },
    handleClose() {
      this.exportVisible = false;
    },
  },
};
</script>