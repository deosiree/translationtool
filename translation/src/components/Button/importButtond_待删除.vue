<template>
  <a-button type="primary" @click="importEntry" :size="size">{{ buttonTitle }}</a-button>

  <CustomModal :visible="importVisible" :okLoading="importLoading" :modalTitle="buttonTitle" @handleClose="importClose"
    @handleOK="importOK" @afterClose="importAfterClose">
    <div class="content">
      <a-form ref="importForm" :model="importModal">
        <a-form-item label="文件类型" name="importType" :rules="[{ required: true, message: '请选择!' }]">
          <a-select v-model:value="importModal.importType" placeholder="请选择文件类型" :options='importTypes' allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="语种" name="language" :rules="[{ required: true, message: '请选择!' }]">
          <a-select mode="multiple" v-model:value="importModal.language" placeholder="请选择语种" :options='translateTypes'
            :fieldNames="{ label: 'name', value: 'name' }" allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="文件" name="file" :rules="[{ required: true, validator: this.checkFile() }]">
          <a-upload name="file" :beforeUpload="beforeUpload" :accept="accept" :max-count="1" :fileList="fileList"
            @change="handleChange" @remove="removeFile"
            :disabled="!importModal.importType || (!this.importModal.language || this.importModal.language.length == 0)">
            <a-button type="primary" size="small" @click="getAccept">选择</a-button>
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
        <!-- {{ globalMessage }} -->
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
import { cloneDeep } from "lodash-es";
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import ExportButton from "@/components/Button/exportButton.vue";
import { entryImportExcle } from "@/http/api/entryManage";
import { entryBatchImportExcel } from "@/utils/excelUtils";
import { setModalAriaHidden } from "@/utils/domUtils";
import commonParam, { entryParams } from "@/constants/commonParam.js";
import { downloadJsonFile } from "@/utils/fileUtils";
export default {
  components: {
    CustomModal, // 注册 CustomModal 组件
    ExportButton,
  },
  emits: ["importSuccess"],
  props: {
    translateTypes: {
      type: Array,
      default: () => [],
    },
    size: {
      type: String,
      default: "small",
    },
    buttonTitle: {
      type: String,
      default: "导入", // 设置默认值为 "导入"
    },
  },
  data() {
    return {
      importVisible: false,
      importLoading: false,
      importModal: {
        importFile: null,
        language: [],
        importType: null,
      },
      importTypes: [
        { label: "csv", value: "csv", accept: ".csv" },
        { label: "excel", value: "excel", accept: ".xls,.xlsx" },
      ],
      accept: null,
      fileList: [],
      exportDataSource: [], // 存储更新失败的词条（旧逻辑，保留兼容）
      fieldOptions: entryParams.exportFields,
      // 新增状态：失败信息相关
      failedEntryInfos: [], // 可重试失败词条数组
      exceptionVos: [], // 异常信息数组
      globalMessage: "", // 总体错误提示信息
      failedInfoVisible: false, // 控制失败信息模态框显示
      failedExportDataSource: [], // 用于导出失败词条的数据源
    };
  },
  watch: {
    "importModal.importType": {
      handler(newValue, oldValue) {
        if (newValue !== oldValue) {
          this.accept = null;
          this.removeFile(); // 切换文件类型后清空文件
        }
      },
      immediate: false,
    },
  },
  methods: {
    // 导入词条
    importEntry() {
      this.importVisible = true;
      setModalAriaHidden(this, document);
    },
    // 关闭导入模态框
    importClose() {
      this.importVisible = false;
    },
    // 确认导入
    importOK() {
      if (!this.$refs.importForm) return;
      this.$refs.importForm
        .validate()
        .then(async () => {
          this.importLoading = true;
          const formData = new FormData();
          formData.append("file", this.importModal.importFile);

          try {
            // 调用批量导入函数
            const result = await entryBatchImportExcel(this.importModal.language, formData);

            this.$emit("importSuccess");

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

              // 关闭导入弹窗，打开失败信息模态框
              this.importVisible = false;
              this.failedInfoVisible = true;
            } else if (result.code === 200) {
              // 完全成功
              this.importVisible = false;
            } else {
              // 其他情况
              this.importVisible = false;
            }
          } catch (error) {
            console.error("导入过程发生异常：", error);
            message.error("导入失败：" + (error.message || "未知错误"));
          }
        })
        .catch((err) => {
          // console.log("未选择文件", err, this.importModal.language);
          message.error("未选择文件", err);
        })
        .finally(() => {
          this.importLoading = false;
        });
    },
    importOK_callback() {
      if (!this.$refs.importForm) return;
      this.$refs.importForm
        .validate()
        .then(async () => {
          this.importLoading = true;
          let exportDataMap = new Map(
            this.exportDataSource.map((item) => [item.id, item])
          ); // 不同语种触发更新失败时，需要记录上一次的失败词条，以便后续补充
          const formData = new FormData();
          formData.append("file", this.importModal.importFile);
          // console.log("formData", formData);
          // console.log("file", this.importModal.importFile);
          // console.log("transType", this.importModal.language);
          // console.log("importType", this.importModal.importType);
          // // 使用 entries() 方法查看 FormData 的属性
          // for (const [key, value] of formData.entries()) {
          //   console.log(`${key}: ${value}`);
          // }

          // 每种翻译语种的导入
          const msg = { success: [], failed: new Map() };
          for (const lang of this.importModal.language) {
            console.log("请求：", lang)
            const params = {
              transType: lang,
              // importType: this.importModal.importType,// 后端不需要这个参数
            };

            try {
              const res = await entryImportExcle(params, formData);
              if (res.type != "ERROR") {
                msg.success.push(lang);
              } else {
                if (!msg.failed.has(res.message)) {
                  msg.failed.set(res.message, []);
                }
                msg.failed.get(res.message).push(lang);
                res.data.list.forEach((item) => {
                  if (!exportDataMap.has(item.id)) {
                    exportDataMap.set(item.id, item);
                  }
                });
              }
            } catch (err) {
              console.log("导入失败原因", err);
              let errRes = "";
              if (typeof err === "string") {
                errRes = err;
              } else {
                errRes = "请使用在词条管理中导出的文件进行导入";
              }
              if (!msg.failed.has(errRes)) {
                msg.failed.set(errRes, []);
              }
              msg.failed.get(errRes).push(lang);
            } finally {
              this.exportDataSource = Array.from(exportDataMap.values());
            }
          }


          this.$emit("importSuccess");
          if (msg.success.length > 0) {
            message.success(msg.success.join("，") + "导入成功！", 3);
            // console.log(msg.success.join(","), "!");
          }
          if (msg.failed.size > 0) {
            function formatMapToString(mapObj) {
              const result = [];
              mapObj.forEach((valueArray, key) => {
                const valueStr = valueArray.join(",");
                result.push(`${key}：${valueStr}`);
              });
              return result.join("；");
            }
            const failedStr = "导入失败！" + formatMapToString(msg.failed);
            // console.log(failedStr,msg.failed);
            message.error(failedStr, 3);
          } else {
            this.importVisible = false;
          }
        })
        .catch((err) => {
          // console.log("未选择文件", err, this.importModal.language);
          message.error("未选择文件", err);
        })
        .finally(() => {
          this.importLoading = false;
        });
    },
    // 导入模态框关闭后回调
    importAfterClose() {
      this.importModal = {
        importFile: null,
        language: [],
        importType: null,
      };
      this.fileList = [];
      // 重置失败信息相关状态
      this.failedEntryInfos = [];
      this.exceptionVos = [];
      this.globalMessage = "";
      this.failedInfoVisible = false;
      this.failedExportDataSource = [];
      this.exportDataSource = [];
      if (this.$refs.importForm) {
        this.$refs.importForm.clearValidate();
      }
    },
    // 导入词条(在文件开始上传之前阻止文件上传操作)
    beforeUpload(file, fileList) {
      // console.log("before");
      return false;
    },
    // 文件变化处理
    handleChange(info) {
      this.fileList = info.fileList; // max-count=1,一个文件一个文件地上传
      if (info.fileList.length === 0) {
        this.importModal.importFile = null;
      } else {
        this.importModal.importFile = info.file;
      }
    },
    // 移除文件
    removeFile(file) {
      this.importModal.importFile = null;
      this.fileList = [];
      return true;
    },
    // 获得导入文件类型
    getAccept() {
      if (!this.importModal.importType) {
        message.error("请选择文件类型！");
        return;
      }
      if (!this.importModal.language || this.importModal.language.length == 0) {
        message.error("请选择语种！");
        return;
      }
      for (let key in this.importTypes) {
        if (this.importModal.importType === this.importTypes[key].value) {
          this.accept = this.importTypes[key].accept;
          break;
        }
      }
    },
    // 校验上传文件是否为空
    checkFile() {
      return (rule, value) => {
        if (!this.importModal.importFile) {
          return Promise.reject("请选择文件！");
        }
        return Promise.resolve();
      };
    },
    // 关闭导出失败模态框
    exportClose() {
      this.importClose();
      this.exportDataSource = []; //清空更新失败的词条记录
      // console.log("更新失败的词条被清空后", this.exportDataSource);
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