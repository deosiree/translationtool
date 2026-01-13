<template>
  <a-button v-if="!hideButton" type="primary" @click="showExportModal" :size="size">{{ buttonTitle }}</a-button>

  <CustomModal :modalTitle="buttonTitle" width="60%" :visible="exportVisible" :showCancel="false" :showOk="false"
    @handleClose="handleClose" :afterClose="afterClose">
    <div class="content">
      <a-form ref="exportForm" :model="exportModal">
        <a-form-item label="文件类型" name="exportType" :rules="[{ required: true, message: '请选择!' }]">
          <a-select v-model:value="exportModal.exportType" placeholder="请选择文件类型" :options='exportTypes'
            @change="exportTypeChange" allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="导出字段" name="field" v-if="exportModal.exportType !== 'xml'"
          :rules="[{ required: true, message: '请选择!' }]">
          <div style="display: flex; justify-content: space-between;">
            <a-select mode="multiple" v-model:value="exportModal.field" :options="fieldOptions"
              :fieldNames="{ label: 'label', value: 'label' }" placeholder="请选择导出字段"
              :disabled="exportModal.exportType === 'xml'" allowClear style="flex: 1; margin-right: 8px;" />
            <a-button type="link" size="small" @click="selectAllFields" style="
              font-size: smaller;margin-top:0">全选</a-button>
          </div>
        </a-form-item>
        <a-form-item label="文件名称" name="xml_name" v-if="exportModal.exportType === 'xml'"
          :rules="[{ required: true, message: '请输入文件名!' }]">
          <a-input v-model:value="exportModal.xml_name" placeholder="请输入文件名"></a-input>
        </a-form-item>
        <a-form-item label="指定local语种" name="local_desc" v-if="exportModal.exportType === 'xml'"
          :rules="[{ required: true, message: '请选择!' }]">
          <a-select v-model:value="exportModal.local_desc" placeholder="请选择语种" :options='localDescOptions' allowClear>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-checkbox v-model:checked="statusCheck">只能导出已审核</a-checkbox>
        </a-form-item>
      </a-form>
    </div>
    <template #leftBottomBtn>
      <a-button key="back" @click="handleClose">取消</a-button>
      <!-- <a-button type="primary" @click="handleOK" :loading="exportLoading2">指定路径</a-button> -->
      <a-button type="primary" @click="handleOK(false)" :loading="exportLoading">确定</a-button>
    </template>
  </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import { create } from "xmlbuilder2";
// import builder from "xmlbuilder"; // 注意：这是 xmlbuilder 的常见导入方式
import { ref } from "vue";
import CustomModal from "@/components/modal/index.vue";
import {
  entryExportByCondition,
  checkBeforeExportEntry,
} from "@/http/api/download";
import {
  queryUserPartiality,
  updateUserPartiality,
} from "@/http/api/userPartiality";
import commonParam, { entryParams } from "@/utils/commonParam.js";
import {
  setModalAriaHidden,
  getCurrentStringTime,
} from "@/utils/commonUtils.js";
export default {
  components: {
    CustomModal,
  },
  emits: ["afterClose"],
  props: {
    dataSource: {
      type: Array,
      required: true,
    },
    fieldOptions_: {
      type: Array,
      required: true,
    },
    size: {
      type: String,
      default: "small",
    },
    buttonTitle: {
      type: String,
      default: "导出",
    },
    defaultStatusCheck: {
      type: Boolean,
      default: true,
    }, // 默认只能导出已审核词条
    fileNamePrefix: {
      type: String,
      default: "",
    }, // 可选：导出文件名前缀，例如“去重_”
    hideButton: {
      type: Boolean,
      default: false,
    }, // 是否隐藏按钮（仅显示模态框）
  },
  data() {
    return {
      exportVisible: false,
      exportLoading: false,
      exportLoading2: false,
      exportModal: {
        exportType: null,
        field: [],
        local_desc: null,
        xml_name: "sysdict",
      },
      exportTypes: [
        { label: "excel", value: "excel" },
        { label: "csv", value: "csv" },
        { label: "xml", value: "xml" },
      ],
      localDescOptions: commonParam.languageList.map((item) => ({
        label: item.name,
        value: item.value,
      })),
      fileHandle: null, // 新增，用于保存文件句柄
      user: null, // 当前用户的相关信息
      currentDepartment: {
        label: "部门名称",
        importTypes: [],
        value: "name",
        xml_temp: false,
        ops: new Set(),
      }, // 当前用户所在部门的相关信息
      fieldOptions: [], // 初始值，会在mounted中从fieldOptions_ prop更新
      statusCheck: this.defaultStatusCheck, // 使用prop值作为初始值( props 不应该被组件内部直接修改)
    };
  },
  mounted() {
    this.$nextTick(() => {
      // 排除导出字段中不能包含的列名（excel只能更新翻译，其他列改不了，而且不能传id列）
      // "修改人"(目前更新接口不能自动修改修改人，只能在打开编辑后保存，来记录修改人与修改时间)
      const filterColNames = [
        "英文翻译id",
        "俄文翻译id",
        "西文翻译id",
        "法文翻译id",
        "中文翻译id",
      ];
      // 使用传入的 fieldOptions_ prop，如果不存在则使用 entryParams.exportFields 作为后备(就是导出全部属性)
      const sourceOptions = this.fieldOptions_ && Array.isArray(this.fieldOptions_) && this.fieldOptions_.length > 0
        ? this.fieldOptions_
        : (entryParams && entryParams.exportFields ? entryParams.exportFields : []);
      this.fieldOptions = sourceOptions.filter(
        (item) => !filterColNames.includes(item.label)
      );
      // 获取当前用户信息
      this.user = this.$store.state.user;
      // 获取当前用户所在部门的相关信息
      if (
        Object.keys(commonParam.departmentMap).includes(this.user.department)
      ) {
        this.currentDepartment =
          commonParam.departmentMap[this.user.department];
      } else {
        this.currentDepartment = commonParam.departmentMap["default"];
      }
    });
  },
  methods: {
    showExportModal() {
      this.exportVisible = true;
      setModalAriaHidden(this, document);
      // console.log("local", this.localDescOptions);
      // console.log("获取用户偏好前：导出字段", this.exportModal.field);
      // 获取用户偏好
      queryUserPartiality().then((res) => {
        if (res.data.list && res.data.list.length > 0) {
          // console.log("获取用户偏好！！！", res.data.list);
          let exportColumn = res.data.list[0].exportColumn;
          if (exportColumn != null && exportColumn != "") {
            this.exportModal.field = exportColumn.split(",");
          }
        }
      });
      // console.log("获取用户偏好后：导出字段", this.exportModal.field);
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
        // console.log("锁定导出字段", this.exportModal.field);
      }
    },
    // 全选导出字段方法
    selectAllFields() {
      this.exportModal.field = this.fieldOptions.map((item) => item.label);
    },
    // 导出-确认
    async handleOK(choosePath = true) {
      if (choosePath) this.exportLoading2 = true;
      else this.exportLoading = true;
      try {
        if (!this.exportModal.exportType) {
          message.error("请选择导出的文件类型！");
          return;
        }
        if (
          this.exportModal.exportType != "xml" &&
          (!this.exportModal.field || this.exportModal.field.length === 0)
        ) {
          message.error("请选择导出字段！");
          return;
        }
        if (
          this.exportModal.exportType === "xml" &&
          !this.exportModal.local_desc
        ) {
          message.error("请选择指定local语种！");
          return;
        }

        await this.$refs.exportForm.validate();
        const { exportType, field } = this.exportModal;
        let data = {};
        let params = {
          exportType: exportType,
        };

        // let blob;// 二进制大对象，不可变的、原始数据的类文件对象
        let suggestedName; // 默认导出的文件名
        let types;

        if (choosePath && "showSaveFilePicker" in window) {
          // 提前获取文件句柄
          const prefix = this.fileNamePrefix || "";
          if (exportType === "xml") {
            suggestedName = prefix + this.exportModal.xml_name + ".xml";
            types = [
              {
                description: "XML 文件",
                accept: {
                  "application/xml": [".xml"],
                },
              },
            ];
          } else if (exportType === "excel" || exportType === "csv") {
            // 这里先设置一个临时建议名，后续获取真实文件名后再更新
            const time = getCurrentStringTime();
            // console.log("当前时间", time);
            suggestedName = `${prefix}词条导出_${time}${exportType === "excel" ? ".xlsx" : ".csv"
              }`;
            types = [
              {
                description: exportType === "excel" ? "Excel 文件" : "CSV 文件",
                accept: {
                  [exportType === "excel"
                    ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    : "text/csv"]: [exportType === "excel" ? ".xlsx" : ".csv"],
                },
              },
            ];
          }
          this.fileHandle = await window.showSaveFilePicker({
            suggestedName,
            types,
          });
        }

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

          // 自动转义并生成xml文件
          const itemDataList = [];
          uniqueEntries.forEach((item) => {
            // console.log(
            //   "法国",
            //   item,
            //   this.exportModal.local_desc,
            //   item[this.exportModal.local_desc]
            // );
            const itemData = {
              abbr: item.entry != null ? item.entry : "",
              cn_desc: item.chinese != null ? item.chinese : "",
              en_desc: item.english != null ? item.english : "",
              es_desc: item.spanish != null ? item.spanish : "",
              ru_desc: item.russian != null ? item.russian : "",
              local_desc:
                item[this.exportModal.local_desc] != null
                  ? item[this.exportModal.local_desc]
                  : "",
            };

            // 装置部给领导看的临时版本
            if (
              this.currentDepartment.hasOwnProperty("xml_temp") &&
              this.currentDepartment.xml_temp
            ) {
              let abbr_tmp = itemData.abbr;
              itemData.abbr = itemData.cn_desc;
              itemData.cn_desc = abbr_tmp;
            }

            itemDataList.push(itemData);
          });

          // 创建 XML 文档（指定版本和编码）
          // 基于xmlbuilder2包（更规范）
          const xml = create({ version: "1.0", encoding: "UTF-8" }).ele(
            "DICT",
            { local_language: "0" }
          );
          // 遍历数据，为每个 item 生成 <ITEM> 子元素
          itemDataList.forEach((item) => {
            // 添加 <ITEM> 子元素，并设置其属性（自动转义特殊字符）
            xml.ele("ITEM", {
              abbr: item.abbr, // 属性名合法（字母开头）
              cn_desc: item.cn_desc,
              en_desc: item.en_desc,
              local_desc: item.local_desc,
              es_desc: item.es_desc,
              ru_desc: item.ru_desc,
            });
          });
          const xmlString = xml.end({ prettyPrint: true, indent: "  " });
          // // 基于xmlbuilder包（与后端统一，但>也未转义）
          // const xml = builder
          //   .create("DICT", { version: "1.0", encoding: "UTF-8" })
          //   .att("local_language", "0");

          // // 遍历数据，生成 ITEM 节点
          // itemDataList.forEach((item) => {
          //   xml
          //     .ele("ITEM")
          //     .att("abbr", item.abbr)
          //     .att("cn_desc", item.cn_desc)
          //     .att("en_desc", item.en_desc)
          //     .att("local_desc", item.local_desc)
          //     .att("es_desc", item.es_desc)
          //     .att("ru_desc", item.ru_desc)
          //     .up(); // 返回 DICT
          // });

          // // 生成格式化 XML 字符串
          // const xmlString = xml.end({ pretty: true, indent: "  " });
          // console.log("生成的 XML 内容：\n", xmlString); // 关键：输出检查

          // 创建 Blob 对象（类型为 XML）
          const blob = new Blob([xmlString], { type: "application/xml" });
          // 生成临时 URL
          const url = URL.createObjectURL(blob);
          // 创建隐藏的 <a> 标签触发下载
          const a = document.createElement("a");
          a.href = url;
          a.download = this.exportModal.xml_name + ".xml"; // 文件名
          a.style.display = "none";
          document.body.appendChild(a);
          a.click();
          // 释放资源
          document.body.removeChild(a);
          URL.revokeObjectURL(url);

          // // 手动构建 XML 字符串
          // let xml = `<?xml version="1.0" encoding="UTF-8"?>\n<DICT local_language="0">\n`;
          // uniqueEntries.forEach((item) => {
          //   // 赋值
          //   let abbr = item.entry != null ? item.entry : ""; // 装置部需求
          //   let cn_desc = item.chinese != null ? item.chinese : ""; // 装置部需求
          //   let en_desc = item.english != null ? item.english : "";
          //   let es_desc = item.spanish != null ? item.spanish : "";
          //   let ru_desc = item.russian != null ? item.russian : "";
          //   let local_desc =
          //     item[this.exportModal.local_desc] != null
          //       ? item[this.exportModal.local_desc]
          //       : "";

          //   // 转义
          //   const escapeHtml = (str) => {
          //     return str.replace(
          //       /[&<>"']/g,
          //       (char) => commonParam.escapeMap[char] || char
          //     );
          //   };
          //   abbr = escapeHtml(abbr);
          //   cn_desc = escapeHtml(cn_desc);
          //   en_desc = escapeHtml(en_desc);
          //   es_desc = escapeHtml(es_desc);
          //   ru_desc = escapeHtml(ru_desc);
          //   local_desc = escapeHtml(local_desc);

          //   // 装置部给领导看的临时版本
          //   if (
          //     this.currentDepartment.hasOwnProperty("xml_temp") &&
          //     this.currentDepartment.xml_temp
          //   ) {
          //     let abbr_tmp = abbr;
          //     abbr = cn_desc;
          //     cn_desc = abbr_tmp;
          //   }

          //   xml += `\t<ITEM abbr="${abbr}" cn_desc="${cn_desc}" en_desc="${en_desc}" local_desc="${local_desc}" es_desc="${es_desc}" ru_desc="${ru_desc}" />\n`;

          //   // console.log("当前xml内容:", xml, this.currentDepartment);
          // });
          // xml += `</DICT>`;

          // // 导出文件
          // const blob = new Blob([xml], { type: "application/xml" });
          // if (choosePath && this.fileHandle) {
          //   const writable = await this.fileHandle.createWritable();
          //   await writable.write(blob);
          //   await writable.close();
          // } else {
          //   await this.handleFileSave(
          //     blob,
          //     this.exportModal.xml_name + ".xml",
          //     [
          //       {
          //         description: "XML 文件",
          //         accept: {
          //           "application/xml": [".xml"],
          //         },
          //       },
          //     ],
          //     choosePath
          //   );
          // }
        } else if (exportType === "excel" || exportType === "csv") {
          // 导出接口
          let fields = ["id"].concat(field);
          data = {
            columnNames: fields,
            entryInfoEntities: this.dataSource,
            excelName: "词条导出",
          };
          let accept_ = [];
          // 只能导出已审核，需要检查是否有审核未结束的词条
          if (this.statusCheck) {
            accept_ = await checkBeforeExportEntry(data, params);
            if (accept_.data.list.length != 0) {
              message.warning("导出失败：存在审核未结束的词条");
            }
          }
          if (!this.statusCheck || accept_.data.list.length == 0) { // 如果不是只能导出已审核，或者没有审核未结束的词条，则导出全部词条
            const res = await entryExportByCondition(data, params);
            let fileName = res.headers["content-disposition"]
              .split(";")[1]
              .split("filename=")[1];

            let contentType = res.headers["content-type"];
            // 去除字符编码信息
            contentType = contentType.split(";")[0];

            // 导出文件
            const blob = new Blob([res.data], { type: contentType });
            if (choosePath && this.fileHandle) {
              // 更新建议文件名
              const writable = await this.fileHandle.createWritable();
              await writable.write(blob);
              await writable.close();
            } else {
              const prefix = this.fileNamePrefix || "";
              const decodedName = decodeURI(fileName);
              const finalName = prefix + decodedName;
              await this.handleFileSave(
                blob,
                finalName,
                [
                  {
                    description:
                      exportType === "excel" ? "Excel 文件" : "CSV 文件",
                    accept: {
                      [contentType]: [
                        exportType === "excel" ? ".xlsx" : ".csv",
                      ],
                    },
                  },
                ],
                choosePath
              );
            }
          }
        }
        this.exportVisible = false;
        // 记录偏好
        this.exportFieldChange(this.exportModal.field);
      } catch (error) {
        if (!(error.name === "AbortError")) {
          console.log("导出失败原因", error);
          message.error(`导出失败: ${error.message || error}`);
        }
      } finally {
        if (choosePath) this.exportLoading2 = false;
        else this.exportLoading = false;
        this.fileHandle = null; // 重置文件句柄
      }
    },
    // 保存文件
    async handleFileSave(blob, suggestedName, types, choosePath = true) {
      try {
        if ("showSaveFilePicker" in window && choosePath) {
          const fileHandle = await window.showSaveFilePicker({
            suggestedName,
            types,
          });
          const writable = await fileHandle.createWritable();
          await writable.write(blob);
          await writable.close();
        } else {
          // 旧浏览器兼容处理
          const url = URL.createObjectURL(blob);
          const link = document.createElement("a");
          link.href = url;
          link.download = suggestedName;
          link.click();
          URL.revokeObjectURL(url);
        }
      } catch (error) {
        if (error.name === "AbortError") {
          console.log("用户取消了文件保存操作", error);
          return;
        }
        message.error("文件保存失败:", error);
        throw error;
      }
    },
    // 记录用户偏好
    exportFieldChange(value) {
      let data = {
        exportColumn: value.join(","),
      };
      updateUserPartiality(data).then((res) => { });
    },
    // 关闭导出模态框
    handleClose() {
      this.exportVisible = false;
    },
    // 关闭模态框后
    afterClose() {
      this.exportModal = {
        exportType: null,
        field: [],
        local_desc: null,
        xml_name: "sysdict",
      };
      this.$emit("afterClose");
    },
  },
};
</script>