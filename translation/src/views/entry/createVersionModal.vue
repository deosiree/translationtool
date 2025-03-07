<template>
  <CustomModal :modalWidth="modalWidth" modalTitle="批量选择" :visible="visible" :showCancel="false" :fullFlag="true" cancelText="取消" okText="创建产品版本"
    @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose" @setTableHeight="setTableHeight">
    <div style="width:100%;height:515px">
      <!-- <a-form
            :model="search"
            layout="inline"
            autocomplete="off"
            ref="formRef"
            >
                <a-form-item
                label="版本名称"
                name="versionName"
                :rules="[{ required: true, message: '请输入版本名称!' }]"
                >
                    <a-input v-model:value="search.versionName" placeholder="请输入版本名称"></a-input>
                </a-form-item>
            </a-form> -->
      <div class="table">
        <div>已选词条：</div>
        <a-config-provider :locale="locale">
          <a-table class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="tableHeight" :pagination="pagination"
            :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="historyTable" bordered>
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'operation'">
                <div class="editable-row-operations">
                  <DeleteOutlined style="color:#369FFF;font-size:16px" @click="remove(record)" title="取消选择" />
                </div>
              </template>
              <template v-if="column.dataIndex === 'entryState'">
                <template v-if="record.entryState === 0">
                  <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
                </template>
                <template v-if="record.entryState === 1">
                  <a-badge color="#FBB31F" /><span style="color:#FBB31F">审核中</span>
                </template>
                <template v-if="record.entryState === 2">
                  <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
                </template>
                <template v-if="record.entryState === 3">
                  <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
                </template>
              </template>
              <template
                v-if="['englishTranslateState','russianTranslateState','spanishTranslateState','frenchTranslateState'].includes(column.dataIndex)">
                <template v-if="record[column.dataIndex] === '0'">
                  <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">未翻译</span>
                </template>
                <template v-if="record[column.dataIndex] === '1'">
                  <a-badge color="#FBB31F" /><span style="color:#FBB31F">待审核</span>
                </template>
                <template v-if="record[column.dataIndex] === '2'">
                  <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
                </template>
                <template v-if="record[column.dataIndex] === '3'">
                  <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
                </template>
              </template>
            </template>
          </a-table>
        </a-config-provider>
      </div>
    </div>
    <template v-slot:leftBottomBtn>
      <a-button @click="cancelCreate">关闭</a-button>
      <a-button type="primary" @click="writeBackFun">回写</a-button>
      <a-button type="primary" danger @click="deleteEntrys">删除</a-button>
      <a-button type="primary" @click="exportExcel">导出Excel</a-button>
      <a-button type="primary" @click="exportXml">导出XML</a-button>
      <a-button type="primary" @click="examine">提交审核/翻译</a-button>
    </template>
  </CustomModal>
  <CustomModal :modalTitle="title" :modalWidth="operateWidth" :modalVisible="operateVisible" @handleClose="operateClose" @handleOK="operateOk"
    @afterClose="afterOperateClose">
    <div style="width:100%;height:100%">

      <a-form v-if="title === '创建版本'" :model="version" autocomplete="off" ref="versionForm" :label-col="{ span: 6 }">
        <a-form-item label="产品版本名称" name="versionName" :rules="[{ required: true, message: '请输入版本名称!' }]">
          <a-input v-model:value="version.versionName" placeholder="请输入版本名称"></a-input>
        </a-form-item>
        <a-form-item label="备注" name="remarks">
          <a-textarea v-model:value="version.remarks" placeholder="请输入备注" :rows="4" />
        </a-form-item>
      </a-form>
      <a-form v-if="title === '导出'" :model="exportClass" autocomplete="off" ref="exportForm" :label-col="{ span: 4 }">
        <a-form-item label="导出字段" name="field" :rules="[{ required: true, message: '请选择导出字段!' }]">
          <a-select mode="multiple" v-model:value="exportClass.field" :options="fieldOptions" :fieldNames="{label:'label',value:'label'}"
            placeholder="请选择" allowClear></a-select>
        </a-form-item>
      </a-form>
      <div class="table" v-if="title === '选择任务'">
        <a-table class="ant-table-striped" :columns="taskColumns" :data-source="taskDataSource" :row-selection='taskRowSelection'
          :row-key="record => record.id" :scroll="{x:'100%' , y: '195px'}" :pagination="false"
          :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="taskTable" bordered>
        </a-table>
      </div>
      <!-- 添加加载动画 -->
      <a-spin :spinning="writeBackLoading">
        <a-form v-if="title === '回写'" :model="writeBack" autocomplete="off" ref="writeBack" :label-col="{ span: 4 }">
          <a-form-item label="IP" name="ip" :rules="[{ required: true, message: '请选择IP!' }]">
            <a-select v-model:value="writeBack.ip" :options="ipOptions" placeholder="请选择IP" allowClear></a-select>
          </a-form-item>
          <a-form-item label="回写语言" name="language" :rules="[{ required: true, message: '请选择导出字段!' }]">
            <a-select v-model:value="writeBack.language" placeholder="请选择" @change="languageChange" allowClear>
              <a-select-option value="英文">英文</a-select-option>
              <a-select-option value="俄文">俄文</a-select-option>
              <a-select-option value="西文">西文</a-select-option>
              <a-select-option value="法文">法文</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="回写类型" name="type">
            <a-radio-group v-model:value="writeBack.type" name="radioGroup" @change="writeBackTypeChange">
              <a-radio value="DEFAUT">默认 </a-radio>
              <a-radio value="TS">TS文件</a-radio>
              <a-radio value="DI">辞典</a-radio>
            </a-radio-group>
            <a-tooltip placement="top">
              <template #title>
                <span>默认：按词条来源回写；TS文件：写入到ts文件；辞典：写入到辞典</span>
              </template>
              <QuestionCircleOutlined style="color:#00000066;float:right;margin-top:3px" />
            </a-tooltip>
          </a-form-item>
          <a-form-item :label="writeBack.label" name="file" v-if="writeBack.type != 'DEFAUT'">
            <a-select show-search v-model:value="writeBack.file" :options="writeBack.fileOptions" placeholder="请选择" allowClear></a-select>
          </a-form-item>
          <a-form-item label=" " :colon="false">
            <a-checkbox v-model:checked="writeBack.isTag" :disabled="writeBack.tagDisabled">回写Tag</a-checkbox>
            <a-checkbox v-model:checked="writeBack.isComment" :disabled="writeBack.commentDisabled">回写来源</a-checkbox>
            <a-tooltip placement="top">
              <template #title>
                <span>词条默认复用，增加标识可以确保词条唯一性（不推荐）</span>
              </template>
              <QuestionCircleOutlined style="color:#00000066;float:right;margin-top:3px" />
            </a-tooltip>
          </a-form-item>
          <!-- <a-form-item
                label="回写Tag"
                name="isTag"
                >
                    <a-switch v-model:checked="writeBack.isTag" checked-children="是" un-checked-children="否" />
                </a-form-item>
                <a-form-item
                label="回写来源"
                name="isComment"
                >
                    <a-switch v-model:checked="writeBack.isComment" checked-children="是" un-checked-children="否" />
                </a-form-item> -->

        </a-form>
      </a-spin>
    </div>
  </CustomModal>

</template>
<script>
import CustomModal from "@/components/modal/index.vue";
import zh_CN from "ant-design-vue/es/locale/zh_CN";
import tableParam from "./tableParam.js";
import {
  MinusSquareOutlined,
  ExclamationCircleOutlined,
  DeleteOutlined,
  QuestionCircleOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import { deleteEntryInfoByID, getI18nAdress } from "@/http/api/workbench.js";
import {
  createVersionByEntry,
  addProductRelation,
  updateEntryInfo,
  writeBack,
} from "@/http/api/entryManage";
import { entryExportByCondition } from "@/http/api/download";
import { searchTaskInfo } from "@/http/api/task";
import {
  setInfo,
  getDictionary,
  getFileListByLang,
} from "@/http/api/i18Server";
import {
  queryUserPartiality,
  updateUserPartiality,
} from "@/http/api/userPartiality";
export default {
  components: {
    CustomModal,
    MinusSquareOutlined,
    ExclamationCircleOutlined,
    DeleteOutlined,
    QuestionCircleOutlined,
  },
  emits: ["createClose", "removeEntry", "cancelCreate", "refresh"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    dataSource: {
      type: Array,
    },
    currentProduct: {
      type: Object,
    },
  },

  data() {
    return {
      locale: zh_CN,
      modalWidth: "60%",
      tableHeight: { x: "100%", y: 395 },
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 60,
          customRender: (text, record, index, column) => {
            return text.index + 1;
          },
          fixed: "left",
        },
        {
          title: "词条状态",
          dataIndex: "entryState",
          align: "center",
          width: 130,
          fixed: "left",
        },
        {
          title: "Abbr",
          dataIndex: "abbr",
          align: "center",
          width: 180,
          fixed: "left",
        },
        { title: "词条", dataIndex: "entry", align: "center", width: 180 },
        {
          title: "中文释义",
          dataIndex: "chineseInterpretation",
          align: "center",
          width: 180,
        },
        {
          title: "英文释义",
          dataIndex: "englishInterpretation",
          align: "center",
          width: 180,
        },
        {
          title: "英文翻译",
          dataIndex: "english",
          align: "center",
          width: 180,
        },
        {
          title: "英文翻译状态",
          dataIndex: "englishTranslateState",
          align: "center",
          width: 180,
        },
        {
          title: "西文翻译",
          dataIndex: "spanish",
          align: "center",
          width: 180,
        },
        {
          title: "西文翻译状态",
          dataIndex: "spanishTranslateState",
          align: "center",
          width: 180,
        },
        {
          title: "俄文翻译",
          dataIndex: "russian",
          align: "center",
          width: 180,
        },
        {
          title: "俄文翻译状态",
          dataIndex: "russianTranslateState",
          align: "center",
          width: 180,
        },
        { title: "法文翻译", dataIndex: "french", align: "center", width: 180 },
        {
          title: "法文翻译状态",
          dataIndex: "frenchTranslateState",
          align: "center",
          width: 180,
        },
        {
          title: "删除",
          dataIndex: "operation",
          align: "center",
          width: 50,
          fixed: "right",
        },
      ],
      version: {
        language: null,
        versionName: "",
        remarks: "",
      },
      pagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        showTotal: (total) => `共 ${total} 条`,
      },
      title: "",
      operateVisible: false,
      operateWidth: "500px",
      exportClass: {
        field: ["abbr", "词条"],
      },
      fieldOptions: tableParam.exportFields,
      product: {},
      taskColumns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 70,
          customRender: (text, record, index, column) => {
            return text.index + 1;
          },
          fixed: "left",
        },
        {
          title: "任务名称",
          dataIndex: "name",
          align: "center",
          width: 150,
          fixed: "left",
          resizable: true,
        },
        {
          title: "产品名称",
          dataIndex: "productName",
          align: "center",
          width: 230,
          resizable: true,
        },
        {
          title: "版本名称",
          dataIndex: "versionName",
          align: "center",
          width: 180,
          resizable: true,
        },
        {
          title: "翻译语种",
          dataIndex: "translateType",
          align: "center",
          width: 150,
        },
        {
          title: "开发员",
          dataIndex: "developer",
          align: "center",
          width: 150,
        },
        {
          title: "词条审核员",
          dataIndex: "entryAuditor",
          align: "center",
          width: 150,
        },
        {
          title: "翻译员",
          dataIndex: "translator",
          align: "center",
          width: 150,
        },
        {
          title: "翻译审核员",
          dataIndex: "translationAuditor",
          align: "center",
          width: 150,
        },
        {
          title: "任务描述",
          dataIndex: "description",
          align: "center",
          width: 230,
          ellipsis: true,
          resizable: true,
        },
        {
          title: "下发时间",
          dataIndex: "deliveryTime",
          align: "center",
          width: 200,
        },
      ],
      taskDataSource: [],
      selectedTaskRows: [],
      writeBack: {
        language: null,
        type: "DEFAUT",
        label: "",
        file: null,
        isTag: null,
        isComment: null,
        fileOptions: [],
        commentDisabled: false,
        tagDisabled: false,
        ip: null,
      },
      writeBackLoading: false,
      ipOptions: [],
    };
  },

  created() {
    this.product = this.currentProduct;
  },
  mounted() {},
  computed: {
    taskRowSelection() {
      return {
        type: "radio",
        onChange: (selectedRowKeys, selectedRows) => {
          // console.log(selectedRowKeys)
          // console.log(selectedRows)
          this.selectedTaskRows = selectedRows;
        },
      };
    },
  },
  watch: {
    currentProduct(newval, oldval) {
      this.product = newval;
    },
  },
  methods: {
    handleClose() {
      this.$emit("createClose");
    },
    // 创建版本
    handleOK() {
      this.operateVisible = true;
      this.operateWidth = "500px";
      this.title = "创建版本";
    },
    remove(record) {
      this.$emit("removeEntry", record);
    },
    cancelCreate() {
      Modal.confirm({
        title: "是否确认关闭?",
        icon: createVNode(ExclamationCircleOutlined),
        content: "确认关闭后，已选择的词条将被清空",
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          this.$emit("cancelCreate");
        },
      });
    },
    afterClose() {
      this.search = {
        versionName: "",
        language: null,
      };
      // this.$refs.formRef.clearValidate()
    },
    // 导出Excel
    exportExcel() {
      this.operateVisible = true;
      this.operateWidth = "500px";
      this.title = "导出";

      // 获取用户偏好
      queryUserPartiality().then((res) => {
        if (res.data.list && res.data.list.length > 0) {
          let exportColumn = res.data.list[0].exportColumn;
          if (exportColumn != null && exportColumn != "") {
            this.exportClass.field = exportColumn.split(",");
          }
        }
      });
    },
    exportFieldChange(value) {
      let data = {
        exportColumn: value.join(","),
      };
      updateUserPartiality(data).then((res) => {});
    },
    // 提交审核/翻译
    examine() {
      this.operateVisible = true;
      this.operateWidth = "50%";
      this.title = "选择任务";
      this.getTaskList();
    },
    // 回写
    writeBackFun() {
      this.operateVisible = true;
      this.operateWidth = "500px";
      this.title = "回写";
      this.getIPs();
    },
    // 获取该产品下的任务
    getTaskList() {
      let params = {
        pageIndex: -1,
        pageSize: -1,
      };
      let data = {
        productId: this.product.key,
        state: "1,2,3,4,5",
      };
      searchTaskInfo(data, params).then((res) => {
        this.taskDataSource = res.data.list;
      });
    },
    operateClose() {
      this.operateVisible = false;
    },
    operateOk() {
      if (this.title === "创建版本") {
        this.$refs.versionForm.validate().then(() => {
          // TODO 创建版本接口
          let params = {
            productID: this.product.key,
            versionName: this.version.versionName,
            common: this.version.remarks,
          };
          createVersionByEntry(params, this.dataSource)
            .then((res) => {
              message.success("创建版本完成！");
              this.operateVisible = false;
              this.$emit("createClose");
              this.$emit("cancelCreate");
            })
            .catch((err) => {
              message.error("创建失败！");
            });
        });
      } else if (this.title === "导出") {
        this.$refs.exportForm.validate().then(() => {
          // 导出接口
          let fields = ["id"].concat(this.exportClass.field);
          let data = {
            columnNames: fields,
            entryInfoEntities: this.dataSource,
            excelName: "词条导出",
          };
          let params = {};
          entryExportByCondition(data, params).then((res) => {
            let fileName = res.headers["content-disposition"]
              .split(";")[1]
              .split("filename=")[1];
            let contentType = res.headers["content-type"];
            const blob = new Blob([res.data], { type: contentType });
            const a = document.createElement("a"); // 转换完成，创建一个a标签用于下载
            a.download = decodeURI(fileName);
            a.href = window.URL.createObjectURL(blob);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(a.href);
            this.operateVisible = false;
            this.$emit("createClose");
            this.$emit("cancelCreate");
          });
          // 记录偏好
          this.exportFieldChange(this.exportClass.field);
        });
      } else if (this.title === "选择任务") {
        //提交审核/翻译
        if (this.selectedTaskRows.length === 0) {
          message.warn("请选择任务！");
          return;
        }
        // 判断词条中是否含有 中文释义和英文释义都不存在的词条
        let notInterpretation = [];
        this.dataSource.forEach((item) => {
          if (
            (item.englishInterpretation === null ||
              item.englishInterpretation === "") &&
            (item.chineseInterpretation === null ||
              item.chineseInterpretation === "")
          ) {
            notInterpretation.push(item);
          }
        });
        if (notInterpretation.length > 0) {
          Modal.confirm({
            title:
              "保存数据中含有中文释义和英文释义都不存在的词条，是否继续保存?",
            icon: createVNode(ExclamationCircleOutlined),
            content: "",
            okText: "是",
            cancelText: "否",
            style: { top: "30%" },
            onOk: () => {
              this.submitExamine();
            },
            onCancel: () => {},
          });
        } else {
          this.submitExamine();
        }
      } else if (this.title === "回写") {
        if (this.writeBack.type != "DEFAUT" && this.writeBack.file === null) {
          message.info("请选择" + this.writeBack.label + "!");
          return;
        }
        this.$refs.writeBack
          .validate()
          .then(() => {
            let params = {
              translateType: this.writeBack.language,
              isTag: this.writeBack.isTag ? 1 : 0,
              isComment: this.writeBack.isComment ? 1 : 0,
              writeType: this.writeBack.type,
              fileName: this.writeBack.file,
              i18nUrl: this.writeBack.ip,
            };
            this.writeBackLoading = true;
            // console.log("开始loading");
            // setTimeout(() => {
            //   console.log(params);
            //   this.writeBackLoading = false;
            //   console.log("结束loading");
            //   message.success("回写成功！");
            // }, 3000);
            writeBack(params, this.dataSource)
              .then((res) => {
                message.success("回写成功！");
                this.operateVisible = false;
                this.$emit("createClose");
                this.$emit("cancelCreate");
              })
              .catch((err) => {
                message.error("回写失败！");
              })
              .finally(() => {
                this.writeBackLoading = false;
                this.loading = false;
              });
          })
          .catch((err) => {});
      }
    },
    // 提交审核/翻译
    submitExamine() {
      let params = {
        notes: "",
      };
      // 修改词条状态
      this.dataSource.forEach((item) => {
        if (item.entryState === 0) {
          item.entryState = 1;
          updateEntryInfo(item, params).then((res) => {});
        }
      });
      // 将词条提交到任务
      let data = [];
      this.dataSource.forEach((item) => {
        let info = {
          id: item.id,
          productID: this.product.key,
          taskId: this.selectedTaskRows[0].id,
          versionID: this.selectedTaskRows[0].versionId,
          enTransId: item.enTransId,
          english: item.english,
          fraTransId: item.fraTransId,
          french: item.french,
          ruTransId: item.ruTransId,
          russian: item.russian,
          spaTransId: item.spaTransId,
          spanish: item.spanish,
        };
        data.push(info);
      });
      addProductRelation(data)
        .then((res) => {
          message.success("已提交！");
          this.operateVisible = false;
          this.$emit("createClose");
          this.$emit("cancelCreate");
        })
        .catch((err) => {
          message.error("提交失败！");
        });
    },
    // 删除词条
    deleteEntrys() {
      Modal.confirm({
        title: "是否确定删除?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          // console.log("已选词条", this.dataSource);
          const seen = {};
          this.dataSource.forEach((item) => {
            const key = `${item.productID}-${item.versionID}`;
            if (!seen[key]) {
              seen[key] = [];
            }
            seen[key].push(item.id);
          });

          // 遍历seen对象，并调用接口
          for (const [combinedKey, ids] of Object.entries(seen)) {
            const [productID, versionID] = combinedKey.split("-");
            let params = {
              versionID: versionID,
              productID: productID,
            };
            deleteEntryInfoByID(params, ids).then((res) => {
              message.success(`已删除${ids.length}条词条!`);
              this.$emit("createClose");
              this.$emit("cancelCreate");
              this.$emit("refresh");
            });
          }
        },
      });
    },

    afterOperateClose() {
      this.version = { versionName: "", remarks: "" };
      this.exportClass = { field: ["abbr", "词条"] };
      this.writeBack = {
        language: null,
        type: "DEFAUT",
        label: "",
        file: null,
        isTag: null,
        isComment: null,
        fileOptions: [],
        commentDisabled: false,
        tagDisabled: false,
      };
    },
    // 动态设置表格高度
    setTableHeight(height, type) {
      if (type === "full") {
        this.tableHeight.y = height - 150;
      } else if (type === "reduce") {
        this.tableHeight.y = 395;
      }
    },
    // 回写类型切换事件
    writeBackTypeChange() {
      this.writeBack.file = null;
      this.writeBack.fileOptions = [];
      this.writeBack.isTag = false;
      this.writeBack.isComment = false;
      this.writeBack.commentDisabled = false;
      this.writeBack.tagDisabled = false;

      if (this.writeBack.type === "TS") {
        this.writeBack.label = "ts文件";
        this.writeBack.isTag = true;
        this.writeBack.isComment = false;
        this.writeBack.commentDisabled = true;
        this.writeBack.tagDisabled = true;
        if (
          this.writeBack.language === null ||
          this.writeBack.language === ""
        ) {
          message.warn("请选择回写语言！");
          return;
        }
        // 获取ts文件列表
        this.getTsFile();
      } else if (this.writeBack.type === "DI") {
        this.writeBack.label = "辞典";
        // 获取辞典文件列表
        this.getDictionary();
      }
    },
    // 获取ts文件
    getTsFile() {
      let params = {
        language: this.writeBack.language,
      };
      getFileListByLang(params).then((res) => {
        res.data.list.forEach((item) => {
          let option = {
            label: item,
            value: item,
          };
          this.writeBack.fileOptions.push(option);
        });
      });
    },
    // 获取辞典
    getDictionary() {
      getDictionary().then((res) => {
        res.data.list.forEach((item) => {
          let option = {
            label: item,
            value: item,
          };
          this.writeBack.fileOptions.push(option);
        });
      });
    },
    // 回写语言change事件
    languageChange() {
      if (this.writeBack.type === "TS") {
        this.getTsFile();
      }
    },
    // 获取i18服务器ip
    getIPs() {
      this.ipOptions = [];
      getI18nAdress().then((res) => {
        res.data.list.forEach((item) => {
          let ip = {
            label: item.ip,
            value: item.ip,
          };
          // if(item.state === '1'){
          //     this.writeBack.ip = item.ip
          // }
          this.ipOptions.push(ip);
        });
      });
    },
    // 导出xml文件，装置的格式
    exportXml() {
      // 手动构建 XML 字符串
      let xml = `<?xml version="1.0" encoding="UTF-8"?>\n<DICT local_language="0">\n`;
      this.dataSource.forEach((item) => {
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

      this.$emit("createClose");
      this.$emit("cancelCreate");
    },
  },
};
</script>
<style lang="less" scoped>
.table {
  width: 100%;
  margin-top: 5px;
  position: relative;
}
.ant-form-inline .ant-form-item-with-help {
  margin-bottom: 0px;
}
</style>