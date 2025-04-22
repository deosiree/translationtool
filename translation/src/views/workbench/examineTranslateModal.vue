<template>
  <Modal :visible="visible" :modalTitle="modalTitle" :modalWidth="modalWidth" :fullFlag="true" okText="保存" :okLoading="saveLoading"
    @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose" @setTableHeight="setTableHeight">
    <div class="content">

      <div class="taskInfo">
        <div class="taskItem">任务名称：{{task.name}}</div>
        <div class="taskItem">产品名称：{{task.productName}}</div>
        <div class="taskItem">上级分类名称：{{task.classifyName}}</div>
        <div class="taskItem">翻译语种：{{task.translateType}}</div>
        <!-- <span style="float:right;font-size:12px">
                    <a-tooltip placement="left">
                        <template #title>
                            <table>
                                <tr><td style="width:100px">上一个</td><td>Ctrl + ↑</td></tr>
                                <tr><td style="width:100px">下一个</td><td>Ctrl + ↓</td></tr>
                                <tr><td style="width:100px">上一个未审核</td><td>Ctrl + Shift + ↑</td></tr>
                                <tr><td style="width:100px">下一个未审核</td><td>Ctrl + Shift + ↓</td></tr>
                                <tr><td style="width:100px">编辑 </td><td>Ctrl + e</td></tr>
                                <tr><td style="width:100px">保存 </td><td>Ctrl + Enter</td></tr>
                                <tr><td style="width:100px">通过 </td><td>Ctrl + p</td></tr>
                                <tr><td style="width:100px">驳回 </td><td>Ctrl + r</td></tr>
                            </table>
                        </template>
                        快捷键
                        <QuestionCircleOutlined />
                    </a-tooltip>
                </span> -->
      </div>
      <div class="form">
        词条：
        <a-input v-model:value="keyWords" style="width:300px" size="small" placeholder='请输入词条搜索' />
        <span style="margin-left:10px">翻译状态：</span>
        <a-select v-model:value="translateState" allowClear size="small" style="width: 300px" placeholder='请选择'>
          <a-select-option value="1">待审核</a-select-option>
          <a-select-option value="2">审核不通过</a-select-option>
          <a-select-option value="3">审核通过</a-select-option>
        </a-select>
        <a-button type="primary" size="small" style="margin-left:8px" @click="getTaskEntry">查询</a-button>
        <!-- <a-button type="primary" size="small" style="margin-left:8px" @click="selectAll">{{selectAllName}}</a-button> -->
        <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="pass">通过</a-button>
        <a-button type="primary" size="small" style="margin-left:8px" class="rejectBtn" @click="reject">驳回</a-button>
        <a-popover trigger="click" placement="leftTop" :overlayStyle="overlayStyle">
          <template #content>
            <a-checkbox-group v-model:value="checkedColumn" @change="changeColumn">
              <a-row v-for="item in checkboxList" :key="item.value">
                <a-col :span="24">
                  <a-checkbox :value="item.value">
                    {{ item.label }}
                  </a-checkbox>
                </a-col>
              </a-row>
            </a-checkbox-group>
          </template>
          <a-button type="primary" size="small" style="margin-left:auto"><template #icon>
              <SettingOutlined />
            </template>展示列</a-button>
        </a-popover>
      </div>
      <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :row-selection="{ 
                selectedRowKeys: selectedRowKeys, 
                onChange: onSelectChange,
                selections:[
                    {key:'selectAll',text:'全部选择',onSelect:selectAllEntry},
                    {key:'clearAll',text:'取消选择',onSelect:clearAllEntry}
                ]
            }" :row-key="record => record.id" :scroll="tableHeight" :pagination='pagination' :loading="loading" :rowClassName="getRowClassName"
        :customRow="doubleClick" ref="tableContainer" @resizeColumn="handleResizeColumn">
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.dataIndex === 'entry'">
            <span v-text="text?text.replace(/\n/g, '\\n'):text"></span>
          </template>
          <template v-if="['english','russian','spanish','french'].includes(column.dataIndex)">
            <div>
              <template v-if="editableData[record.id]">
                <a-form :model="editableData[record.id]" :rules="rules[record.id]" :ref="'form'+record.id.replaceAll('-','')+column.dataIndex"
                  autocomplete="off">
                  <a-form-item :name="column.dataIndex">
                    <a-input v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" @pressEnter="inputPressEnter(record)" />
                  </a-form-item>
                </a-form>
              </template>
              <template v-else>
                {{ text }}
              </template>
            </div>
          </template>
          <template v-if="['englishAuditSuggest','russianAuditSuggest','spanishAuditSuggest','frenchAuditSuggest'].includes(column.dataIndex)">
            <div>
              <template v-if="editableData[record.id]">
                <a-input v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" @pressEnter="inputPressEnter(record)" />
              </template>
              <template v-else>
                {{ text }}
              </template>
            </div>
          </template>
          <template v-if="column.dataIndex === 'tag'">
            <div>
              <span>
                <a-tag v-for="(tag,index) in companyCut(text)" :key="index" color="cyan" class="tag-content">
                  {{tag}}
                </a-tag>
              </span>
            </div>
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
          <template v-else-if="column.dataIndex === 'operation'">
            <div class="editable-row-operations">
              <span>
                <a-checkable-tag :checked="record.auditState === 1" :class="record.auditState === 1 ? 'passTagChecked' : 'passTag' "
                  @change="passTagChange(record)">通过</a-checkable-tag>
                <a-checkable-tag :checked="record.auditState === 0" :class="record.auditState === 0 ? 'rejectTagChecked' : 'rejectTag'"
                  @change="rejectTagChange(record)">驳回</a-checkable-tag>
              </span>
            </div>
          </template>
        </template>
        <template #expandIcon="props">
          <span v-if="props.record.children != null && props.record.children.length > 0">
            <div v-if="props.expanded" style="display: inline-block; margin-right: 10px" @click="(e) => {props.onExpand(props.record, e);}">
              <CaretDownOutlined />
            </div>
            <div v-else style="display: inline-block; margin-right: 10px" @click="(e) => {props.onExpand(props.record, e);}">
              <CaretRightOutlined />
            </div>
          </span>
          <span v-else style="margin-right:23px"></span>
        </template>
        <!-- 设置筛选菜单 -->
        <template #customFilterDropdown="{ setSelectedKeys, selectedKeys, confirm, clearFilters, column }">
          <div style="padding: 8px">
            <a-input ref="searchInput" :placeholder="`搜索 ${column.title}`" :value="selectedKeys[0]"
              style="width: 188px; margin-bottom: 8px; display: block" @change="e => setSelectedKeys(e.target.value ? [e.target.value] : [])"
              @pressEnter="handleSearch(selectedKeys, confirm, column.dataIndex,clearFilters)" />
            <a-button type="primary" size="small" style="width: 90px; margin-right: 8px"
              @click="handleSearch(selectedKeys, confirm, column.dataIndex,clearFilters)">
              <template #icon>
                <SearchOutlined />
              </template>搜索</a-button>
            <a-button size="small" style="width: 90px" @click="handleReset(clearFilters)">重置</a-button>
          </div>
        </template>
        <!-- 设置筛选图标 -->
        <template #customFilterIcon="{ filtered }">
          <SearchOutlined :style="{ color: filtered ? '#108ee9' : undefined }" />
        </template>
      </a-table>
    </div>
  </Modal>
  <Modal :visible="rejectReasonVisible" modalTitle="驳回原因" @handleClose="rejectReasonClose" @handleOK="rejectReasonOK"
    @afterClose="rejectReasonAfterClose">
    <div style="width:100%;height:100%">
      <a-form ref="exportForm" name="custom-validation" :model="rejectReason">
        <a-form-item label="驳回原因" name="reason">
          <a-textarea v-model:value="rejectReason.reason" placeholder="请输入驳回原因" allow-clear />
        </a-form-item>
      </a-form>
    </div>
  </Modal>
</template>
<script>
import "@/assets/style/common.less";
import Modal from "@/components/modal/index.vue";
import { cloneDeep, iteratee } from "lodash-es";
import {
  getEntryTempByTaskID,
  updateEntryTemp,
  getEntryInfoList,
  updateEntryList,
} from "@/http/api/workbench";
import { message } from "ant-design-vue";
import workbenchCommon from "@/views/workbench/common.js";
import languageParam from "@/utils/languageParam.js";
import common from "../entry/common";
import { setModalAriaHidden } from "@/utils/commonUtils";
import { computed, defineComponent, ref } from "vue";
import {
  FileSearchOutlined,
  QuestionCircleOutlined,
  SearchOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
  SettingOutlined,
} from "@ant-design/icons-vue";
import key from "keymaster";
export default {
  components: {
    Modal,
    QuestionCircleOutlined,
    SearchOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    SettingOutlined,
  },
  emits: ["handleClose", "handleOK"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
      default: "翻译审核",
    },
    currentTask: {
      type: Object,
    },
    classifyLimit: {
      type: Object,
    },
  },

  data() {
    return {
      modalWidth: "70%",
      task: {},
      keyWords: "",
      tableHeight: { x: "100%", y: 415 },
      loading: false,
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          width: 70,
          customRender: (text, record, index, column) => {
            return (
              text.index +
              1 +
              this.pagination.pageSize * (this.pagination.current - 1)
            );
          },
          fixed: "left",
          index: 0,
        },
        {
          title: "审核状态",
          dataIndex: "state",
          align: "center",
          width: 100,
          fixed: "left",
        },
        {
          title: "Abbr",
          dataIndex: "abbr",
          align: "center",
          width: 150,
          resizable: true,
          index: 2,
        },
        {
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 200,
          resizable: true,
        },
        {
          title: "翻译",
          dataIndex: "translate",
          align: "center",
          width: 200,
          resizable: true,
        },
        // {title: '来源',dataIndex: 'source',align:'center',width:100,resizable: true,ellipsis:true},
        { title: "tag", dataIndex: "tag", align: "center", width: 150 },
        {
          title: "审核意见",
          dataIndex: "auditSuggess",
          align: "center",
          width: 200,
          resizable: true,
        },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          width: 100,
          ellipsis: true,
          fixed: "right",
        },
      ],
      dataSource: [],
      allData: [],
      pagination: {
        pageSizeOptions: ["20", "50", "100"],
        showSizeChanger: true,
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
      selectedRowKeys: [],
      selectedRows: [],
      editableData: {},
      rules: {},
      translateState: null,
      saveLoading: false,
      selectedRowIndex: null,
      timer: null,
      state: {
        searchText: "",
        searchedColumn: "",
      },
      clearFilters: null,
      selectAllName: "全选",
      rejectReasonVisible: false,
      rejectReason: {
        reason: "",
      },
      overlayStyle: workbenchCommon.overlayStyle,
      checkedColumn: ["abbr", "tag"],
      checkboxList: workbenchCommon.checkboxList,
      languageParam:null,
    };
  },

  created() {},
  mounted() {
    this.task = this.currentTask;
    this.languageParam = languageParam.languageList.find((it) => it.name === this.task.translateType);
    // workbenchCommon.languageMap[this.task.translateType].code
  },
  watch: {
    currentTask(newval, oldval) {
      this.task = newval;
      this.languageParam = languageParam.languageList.find((it) => it.name === this.task.translateType);
      this.setTranslateColumn();
    },
  },
  methods: {
    // 设置翻译列展示的语言
    setTranslateColumn() {
      this.columns.forEach((item) => {
        if (item.title === "翻译") {
          item.dataIndex = this.languageParam.value;
        }
        if (item.title === "翻译状态") {
          item.dataIndex = this.languageParam.state;
        }
        if (item.title === "审核意见") {
          item.dataIndex = this.languageParam.auditSuggest;
        }
      });
    },
    // 获取待审核词条
    getTaskEntry() {
      let params = {
        taskID: this.task.id,
        entryState: "3",
        entry: this.keyWords,
      };
      this.loading = true;
      let data =
        this.translateState === null || this.translateState === undefined
          ? ["1"]
          : [this.translateState];

      let auditSuggest = this.languageParam.auditSuggest;
      getEntryInfoList(params, data)
        .then((res) => {
          this.dataSource = res.data.list;

          this.dataSource.forEach((item) => {
            item.auditState = -1;
            item[this.languageParam.auditSuggest] = ""; // 对应语言的审核意见清空
          });
          // console.log("所有审核状态的状态都变成了-1，即审核不通过", this.dataSource);
          // this.allData = this.dataSource
          this.loading = false;
          // this.select()
        })
        .catch((err) => {
          this.loading = false;
          message.error(err.message);
        });

      // 初始化快捷键
      this.initShortcutKeys();
    },
    handleOK() {
      this.loading = true;
      this.saveLoading = true;
      for (let key in this.editableData) {
        let entry = this.dataSource.find((item) => item.id === key);
        entry[this.languageParam.auditSuggest] =
          this.editableData[key][this.languageParam.auditSuggest];
        entry[this.languageParam.value] = this.editableData[key][this.languageParam.value];
      }
      this.editableData = {};

      let params = {
        taskID: this.task.id,
      };
      let updateArr = [];
      let okArr = [];
      this.dataSource.forEach((item) => {
        item.parentID = "";
        if (item.auditState === 0) {
          // 审核不通过
          item[this.languageParam.state] = "2";
          updateArr.push(item);
        } else if (item.auditState === 1) {
          // 审核通过
          item[this.languageParam.state] = "3";
          updateArr.push(item);
          okArr.push(item);
        }
      });
      // 校验审核通过的词条
      let num = this.verifyTranslationLength(okArr);
      if (num > 0) {
        message.warn("存在超长翻译，请检查！");
        this.saveLoading = false;
        return;
      }
      if (updateArr.length > 0) {
        updateEntryList(params, updateArr)
          .then((res) => {
            message.success("已保存！");
            this.getTaskEntry();
          })
          .catch((err) => {
            message.error("保存失败！", err.message);
          })
          .finally(() => {
            this.saveLoading = false;
            this.loading = false;
            // console.log("剩余待处理数据的数量：", this.dataSource.length-updateArr.length);
            if (this.dataSource.length == updateArr.length) {
              // 如果没有待处理的数据就自动关闭弹窗
              this.handleClose();
            }
          });
      } else {
        this.saveLoading = false;
        this.loading = false;
      }
    },
    handleClose() {
      this.$emit("handleClose");
    },
    getRowClassName(record, index) {
      let className = null;
      if (index % 2 === 1) {
        className = "table-striped";
        if (this.selectedRowIndex === record.id) {
          className = className + " highlighted-row";
        }
      } else {
        if (this.selectedRowIndex === record.id) {
          className = "highlighted-row";
        }
      }
      return className;
    },
    handleResizeColumn: (w, col) => {
      col.width = w;
    },
    // 模糊查询
    select() {
      this.dataSource = this.allData.filter((item) =>
        item.entry.includes(this.keyWords)
      );
    },
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
    },
    // 通过标签点击事件
    passTagChange(record) {
      if (record.auditState === 1) {
        // 取消选择
        record.auditState = -1;
      } else {
        record.auditState = 1;
      }
    },
    // 驳回标签点击事件
    rejectTagChange(record) {
      if (record.auditState === 0) {
        record.auditState = -1;
      } else {
        record.auditState = 0;
      }
    },
    // 通过按钮点击事件
    pass() {
      this.selectedRows.forEach((item) => {
        item.auditState = 1;
      });
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.selectAllName = "全选";
    },
    // 驳回按钮点击事件
    reject() {
      if (this.selectedRows.length > 0) {
        this.rejectReasonVisible = true;
        setModalAriaHidden(this, document);
      }

      // this.selectedRows.forEach(item => {
      //     item.auditState = 0
      // })
      // this.selectedRowKeys = []
      // this.selectedRows = []
      // this.selectAllName = "全选"
    },
    //双击表格行 可编辑
    doubleClick(record, index) {
      return {
        onClick: (event) => {
          this.selectedRowIndex = record.id;
        },
        onDblclick: (event) => {
          if (this.editableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }
          this.editableData[record.id] = cloneDeep(
            this.dataSource.filter((item) => record.id === item.id)[0]
          );
          // 设置校验规则
          this.rules[record.id] = {
            entry: [
              { validator: this.vilidFildLength(record, "chinese") },
              { required: true, message: "请输入!" },
            ],
          };
          this.rules[record.id][this.languageParam.value] = [
            { validator: this.vilidFildLength(record, this.languageParam.value) },
          ];
        },
      };
    },
    // 校验输入数据的长度
    vilidFildLength(record, language) {
      return (rule, value) => {
        let type = "";
        if (language === "chinese") {
          type = "maxByte";
        } else {
          type = "foreignMaxByte";
        }
        let maxLength = null;
        if (
          this.classifyLimit[record.classfy1] === undefined ||
          this.classifyLimit[record.classfy1] === null
        ) {
          if (record.maxLength != null && record.maxLength != "") {
            maxLength = record.maxLength;
          } else {
            return Promise.resolve();
          }
        } else {
          maxLength = this.classifyLimit[record.classfy1][type];
        }
        if (
          maxLength === undefined ||
          maxLength === "" ||
          maxLength === null ||
          maxLength === 0
        ) {
          return Promise.resolve();
        }
        // 获取输入数据的长度
        let length = common.byteLength(value);
        if (length > maxLength) {
          return Promise.reject("允许最大字符数为" + maxLength + "！");
        }
        return Promise.resolve();
      };
    },
    // 说明 输入框 回车事件
    inputPressEnter(record) {
      // 长度校验
      let list = [
        eval(
          "this.$refs.form" + record.id.replaceAll("-", "") + this.languageParam.value
        ).validate(),
      ];
      Promise.all(list)
        .then(() => {
          record[this.languageParam.auditSuggest] =
            this.editableData[record.id][this.languageParam.auditSuggest];
          record[this.languageParam.value] = this.editableData[record.id][this.languageParam.value];
          delete this.editableData[record.id];
        })
        .catch((err) => {
          message.error(err.message);
        });
    },
    afterClose() {
      this.editableData = {};
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.keyWords = "";
      this.pagination.current = 1;
      this.pagination.pageSize = 20;
      this.selectAllName = "全选";
      // 解绑快捷键
      key.unbind(
        "ctrl+down,ctrl+up,ctrl+shift+down,ctrl+shift+up,ctrl+e,ctrl+enter,ctrl+p,ctrl+r"
      );
      // 清除表格筛选
      if (this.clearFilters) {
        this.clearFilters({ confirm: true });
        this.state.searchText = "";
      }
    },
    // 初始化快捷键
    initShortcutKeys() {
      let _this = this;
      // 绑定快捷键
      key("ctrl+down", function () {
        _this.nextEntry();
        return false;
      });
      key("ctrl+up", function () {
        _this.prevEntry();
        return false;
      });
      key("ctrl+shift+down", function () {
        _this.nextNotChecked();
        return false;
      });
      key("ctrl+shift+up", function () {
        _this.prevNotChecked();
        return false;
      });
      key("ctrl+e", function () {
        _this.editSelectRow();
        return false;
      });
      key("ctrl+enter", function () {
        _this.enterEditEntry();
        return false;
      });
      key("ctrl+p", function () {
        _this.translatePass();
        return false;
      });
      key("ctrl+r", function () {
        _this.translateReject();
        return false;
      });
    },
    // 下一个词条 快捷键
    nextEntry() {
      let index = this.dataSource.findIndex(
        (item) => item.id === this.selectedRowIndex
      );
      if (index === this.dataSource.length - 1) {
        return;
      }
      if (index === this.pagination.current * this.pagination.pageSize - 1) {
        // 翻页
        this.pageChange(this.pagination.current + 1, this.pagination.pageSize);
      }
      index++;
      this.selectedRowIndex = this.dataSource[index].id;
      this.scrollTableToRow(index);
    },
    // 上一个词条
    prevEntry() {
      if (this.selectedRowIndex === null) {
        return;
      }
      let index = this.dataSource.findIndex(
        (item) => item.id === this.selectedRowIndex
      );
      if (index === 0) {
        return;
      }
      if (index === (this.pagination.current - 1) * this.pagination.pageSize) {
        // 翻页
        this.pageChange(this.pagination.current - 1, this.pagination.pageSize);
      }
      index--;
      this.selectedRowIndex = this.dataSource[index].id;
      this.scrollTableToRow(index);
    },
    // 下一个未审核
    nextNotChecked() {
      let index = this.dataSource.findIndex(
        (item) => item.id === this.selectedRowIndex
      );
      if (index === this.dataSource.length - 1) {
        return;
      }
      let notTransIndex = index;
      index++;
      for (index; index < this.dataSource.length; index++) {
        if (this.dataSource[index].auditState === -1) {
          notTransIndex = index;
          break;
        }
      }
      let recordPage = Math.floor(notTransIndex / this.pagination.pageSize) + 1;
      if (recordPage != this.pagination.current) {
        // 翻页
        this.pageChange(recordPage, this.pagination.pageSize);
      }
      this.selectedRowIndex = this.dataSource[notTransIndex].id;
      this.scrollTableToRow(notTransIndex);
    },
    // 上一个未审核
    prevNotChecked() {
      let index = this.dataSource.findIndex(
        (item) => item.id === this.selectedRowIndex
      );
      if (index === 0) {
        return;
      }
      let preNotTransIndex = index;
      index--;
      for (index; index >= 0; index--) {
        if (this.dataSource[index].auditState === -1) {
          preNotTransIndex = index;
          break;
        }
      }
      let recordPage =
        Math.floor(preNotTransIndex / this.pagination.pageSize) + 1;
      if (recordPage != this.pagination.current) {
        // 翻页
        this.pageChange(recordPage, this.pagination.pageSize);
      }
      this.selectedRowIndex = this.dataSource[preNotTransIndex].id;
      this.scrollTableToRow(preNotTransIndex);
    },
    // 编辑选中行
    editSelectRow() {
      if (this.selectedRowIndex === null) {
        return;
      }
      if (this.editableData.hasOwnProperty(this.selectedRowIndex)) {
        // 编辑数据中包含该数据
        // eval("this.$refs.ref"+ this.selectedRowIndex.replaceAll('-','')).focus()
      } else {
        // 编辑数据中不包含该数据
        this.editableData[this.selectedRowIndex] = this.dataSource.find(
          (item) => item.id === this.selectedRowIndex
        );
        // this.$nextTick(() => {
        //     let input = eval("this.$refs.ref"+ this.selectedRowIndex.replaceAll('-',''))
        //     input.focus()
        // })
      }
    },
    // 确定编辑
    enterEditEntry() {
      let entry = this.dataSource.find(
        (item) => item.id === this.selectedRowIndex
      );
      entry = this.editableData[this.selectedRowIndex];
      delete this.editableData[this.selectedRowIndex];
    },
    // 审核通过快捷键
    translatePass() {
      if (this.selectedRowIndex === null) {
        return;
      }
      this.dataSource.find(
        (item) => item.id === this.selectedRowIndex
      ).auditState = 1;
    },
    // 审核拒绝快捷键
    translateReject() {
      if (this.selectedRowIndex === null) {
        return;
      }
      this.dataSource.find(
        (item) => item.id === this.selectedRowIndex
      ).auditState = 0;
    },
    // 滚动表格
    scrollTableToRow(rowIndex) {
      this.$nextTick(() => {
        const table = this.$refs.tableContainer; // 获取表格容器元素
        if (table && rowIndex >= 0) {
          // 根据索引查找目标行元素
          let flag =
            rowIndex - (this.pagination.current - 1) * this.pagination.pageSize;
          const targetElement = table.$el.querySelectorAll("tr")[flag];
          let container =
            this.$refs.tableContainer.$el.querySelector(".ant-table-body");
          if (targetElement) {
            container.scrollTop = flag * targetElement.offsetHeight - 370; // 当前行 * 行高 - 表格展示高度
          }
        }
      });
    },
    // 列筛选
    handleSearch(selectedKeys, confirm, dataIndex, clearFilters) {
      confirm();
      this.state.searchText = selectedKeys[0];
      this.state.searchedColumn = dataIndex;
      this.clearFilters = clearFilters;
    },
    handleReset(clearFilters) {
      clearFilters({ confirm: true });
      this.state.searchText = "";
    },
    // 动态设置表格高度
    setTableHeight(height, type) {
      if (type === "full") {
        this.tableHeight.y = height - 230;
      } else if (type === "reduce") {
        this.tableHeight.y = 415;
      }
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;

      // 翻页时校验已审核数据的长度
      let data = this.dataSource.slice((page - 1) * pageSize, page * pageSize);
      let arr = [];
      data.forEach((item) => {
        if (item.auditState >= 0) {
          arr.push(item);
        }
      });
      this.verifyTranslationLength(arr);
    },
    // 全选
    selectAll() {
      if (this.selectedRowKeys.length === this.dataSource.length) {
        // 已全选
        this.selectedRowKeys = [];
        this.selectedRows = [];
        this.selectAllName = "全选";
      } else {
        this.selectedRowKeys = [];
        this.selectedRows = [];
        this.dataSource.forEach((item) => {
          this.selectedRows.push(item);
          this.selectedRowKeys.push(item.id);
        });
        this.selectAllName = "取消全选";
      }
    },
    // 校验翻译长度
    verifyTranslationLength(array) {
      let flag = 0;
      array.forEach((record) => {
        let maxLength = null;
        if (record.classfy1 === null || record.classfy1 === "") {
          if (record.maxLength != null && record.maxLength != "") {
            maxLength = record.maxLength;
          } else {
            return;
          }
        } else {
          maxLength = this.classifyLimit[record.classfy1]
            ? this.classifyLimit[record.classfy1]["foreignMaxByte"]
            : null;
        }
        if (
          maxLength === null ||
          maxLength === "" ||
          maxLength === undefined ||
          maxLength === 0
        ) {
          return;
        }
        // 是否编辑中
        let text = this.editableData.hasOwnProperty(record.id)
          ? this.editableData[record.id][this.languageParam.value]
          : record[this.languageParam.value];
        if (common.byteLength(text) > maxLength) {
          flag++;
          this.addEdit(record).then((res) => {
            eval(
              "this.$refs.form" + record.id.replaceAll("-", "") + this.languageParam.value
            )
              .validate()
              .then(() => {})
              .catch((err) => {
                message.error(err.message);
              });
          });
        }
      });
      return flag;
    },
    addEdit(record) {
      this.editableData[record.id] = this.editableData.hasOwnProperty(record.id)
        ? this.editableData[record.id]
        : cloneDeep(record);
      // 设置校验规则
      this.rules[record.id] = {
        entry: [
          { validator: this.vilidFildLength(record, "chinese") },
          { required: true, message: "请输入!" },
        ],
      };
      this.rules[record.id][this.languageParam.value] = [
        { validator: this.vilidFildLength(record, this.languageParam.value) },
      ];
      return Promise.resolve();
    },
    selectAllEntry() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
      let dataToSelect;
      if (this.filters && (this.filters.isExist || this.filters.entrySource)) {
        // 确保 filteredData 是最新的筛选结果
        dataToSelect = this.dataSource.filter((item) => {
          const isExistMatch =
            !this.filters.isExist ||
            this.filters.isExist.includes(item.isExist);
          const entrySourceMatch =
            !this.filters.entrySource ||
            item.entrySource.includes(this.filters.entrySource);
          return isExistMatch && entrySourceMatch;
        });
      } else {
        dataToSelect = this.dataSource;
      }
      dataToSelect.forEach((item) => {
        this.selectedRowKeys.push(item.id);
        this.selectedRows.push(item);
      });
    },
    clearAllEntry() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    // 切割字符串
    companyCut(message) {
      let res = [];
      if (message === null || message === "") {
        return res;
      }
      const regex = /[;；]/;
      res = message.split(regex);
      res = res.filter((item) => item != "");
      return res;
    },
    // 编辑原因确定
    rejectReasonOK() {
      this.selectedRows.forEach((item) => {
        item.auditState = 0;
        item[this.languageParam.auditSuggest] = this.rejectReason.reason;
      });
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.rejectReasonVisible = false;
    },
    rejectReasonClose() {
      this.rejectReasonVisible = false;
    },
    rejectReasonAfterClose() {
      this.rejectReason.reason = "";
    },
    changeColumn(checkedValue) {
      this.checkedColumn = checkedValue;
      this.checkboxList.forEach((value) => {
        let checkedIndex = this.checkedColumn.findIndex(
          (item) => item === value.value
        );
        let nowColumnIndex = this.columns.findIndex(
          (item) => item.dataIndex === value.value
        );
        if (
          (nowColumnIndex !== -1 && checkedIndex !== -1) ||
          (nowColumnIndex === -1 && checkedIndex === -1)
        ) {
          return;
        }
        if (nowColumnIndex === -1 && checkedIndex !== -1) {
          let newCol = {
            title: value.label,
            dataIndex: value.value,
            align: "center",
            width: 200,
            resizable: true,
            index: value.index,
          };
          if (newCol.dataIndex === "abbr") {
            newCol.fixed = "left";
          }
          this.columns.splice(-1, 0, newCol);
        }
        if (nowColumnIndex !== -1 && checkedIndex === -1) {
          this.columns.splice(nowColumnIndex, 1);
        }
      });
      this.columns.sort(function (a, b) {
        return a.index - b.index;
      });
    },
  },
};
</script>
<style scoped lang="less">
.ant-divider {
  margin: 15px 0;
}
.content {
  width: 100%;
  height: 100%;
  padding: 10px;
  background-color: #f3f3f3;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 16px;
  align-self: stretch;

  .taskInfo {
    display: flex;
    padding: 4px 0px;
    align-items: center;
    gap: 32px;
    align-self: stretch;

    .taskItem {
      display: flex;
      align-items: center;
      flex: 1 0 0;
    }
  }
  .form {
    display: flex;
    align-items: center;
    align-self: stretch;
    width: 100%;
  }
  .rejectBtn {
    background: #fbb31f;
    border-color: #fbb31f;
  }
  .rejectBtn:hover {
    background: #fbb31f;
    border-color: #fbb31f;
  }
  .rejectBtn:focus {
    background: #fbb31f;
    border-color: #fbb31f;
  }
  .passTag {
    border: 1px solid #36bf7d;
    color: #36bf7d;
  }
  .passTagChecked {
    background-color: #36bf7d;
    color: white;
  }
  .rejectTag {
    border: 1px solid #fbb31f;
    color: #fbb31f;
  }
  .rejectTagChecked {
    background-color: #fbb31f;
    color: white;
  }
}
.ant-table-cell .ant-form-item {
  margin-bottom: 0%;
}
:deep(.ant-pagination) {
  margin: 8px 0;
}
</style>