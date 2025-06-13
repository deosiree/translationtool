<template>
  <CustomModal :visible="visible" :modalTitle="modalTitle" :modalWidth="modalWidth" :fullFlag="true" :okLoading="saveLoading" okText="保存"
    @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose" @setTableHeight="setTableHeight">
    <div class="content">
      <div class="taskInfo">
        <div class="taskItem">任务名称：{{task.name}}</div>
        <div class="taskItem">产品名称：{{task.productName}}</div>
        <div class="taskItem">上级分类名称：{{task.classifyName}}</div>
        <div class="taskItem">翻译语种：{{task.translateType}}</div>
      </div>
      <div class="form">
        词条：
        <a-textarea v-model:value="keyWords" style="width:300px" size="small" placeholder='请输入词条搜索' :auto-size="{ minRows: 1 }" />
        <span style="margin-left:10px">词条状态：</span>
        <a-select v-model:value="entryState" size="small" style="width: 300px" allowClear>
          <a-select-option value="1">待审核</a-select-option>
          <a-select-option value="2">审核不通过</a-select-option>
          <a-select-option value="3">审核通过</a-select-option>
        </a-select>
        <a-button type="primary" size="small" style="margin-left:8px" @click="getTaskEntry">查询</a-button>
        <!-- <a-button type="primary" size="small" style="margin-left:8px" @click="selectAll">{{selectAllName}}</a-button> -->
        <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="pass">通过</a-button>
        <a-button type="primary" size="small" style="margin-left:8px" class="rejectBtn" @click="reject">驳回</a-button>
        <a-button type="primary" size="small" danger style="margin-left:8px" @click="deleteTaskEntry">删除</a-button>
        <!-- <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="aggregation">聚合</a-button>
                <a-button type="primary" size="small" style="margin-left:8px" class="yellowBtn" @click="cancelAggregation">取消聚合</a-button> -->
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
      <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :row-selection="{ selectedRowKeys: selectedRowKeys, 
                onChange: onSelectChange,
                checkStrictly: false,
                selections:[
                    {key:'selectAll',text:'全部选择',onSelect:selectAllEntry},
                    {key:'clearAll',text:'取消选择',onSelect:clearAllEntry}
                ]
            }" :row-key="record => record.id" :scroll="tableHeight" :pagination='pagination' :loading="loading" :rowClassName="getRowClassName"
        :customRow="doubleClick" :expandIconColumnIndex="2" ref="workTable" @resizeColumn="handleResizeColumn" @change="handleTableChange">
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
                    <a-input v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" @pressEnter="edit(record)" />
                  </a-form-item>
                </a-form>
              </template>
              <template v-else>
                {{ text }}
              </template>
            </div>
          </template>
          <template
            v-if="['chineseInterpretation','englishInterpretation','spanishInterpretation','frenchInterpretation','russianInterpretation' ,'auditSuggess','diFileName','comment'].includes(column.dataIndex)">
            <div>
              <template v-if="editableData[record.id]">
                <a-input v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" @pressEnter="edit(record)" />
              </template>
              <template v-else>
                {{ text }}
              </template>
            </div>
          </template>
          <template v-if="column.dataIndex === 'tag'">
            <div>
              <template v-if="editableData[record.id]">
                <a-input v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0;width:90%" @pressEnter="edit(record)" />
                <a-tooltip placement="top">
                  <template #title>
                    <span>多个Tag按分号分割！</span>
                  </template>
                  <InfoCircleOutlined style="margin-left:3px" />
                </a-tooltip>
              </template>
              <template v-else>
                <!-- {{ text }} -->
                <span>
                  <a-tag v-for="(tag,index) in companyCut(text)" :key="index" color="cyan" class="tag-content">
                    {{tag}}
                  </a-tag>
                </span>
              </template>
            </div>
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
          <template v-if="column.dataIndex === 'isExist'">
            <template v-if="record.isExist === 0">
              <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
            </template>
            <template v-if="record.isExist === 1">
              <a-badge color="#FBB31F" /><span style="color:#FBB31F">已存在</span>
            </template>
          </template>
          <template v-if="column.dataIndex === 'entryState'">
            <template v-if="record.entryState === 0">
              <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
            </template>
            <template v-if="record.entryState === 1">
              <a-badge color="#FBB31F" /><span style="color:#FBB31F">待审核</span>
            </template>
            <template v-if="record.entryState === 2">
              <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
            </template>
            <template v-if="record.entryState === 3">
              <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
            </template>
          </template>
          <template
            v-if="['englishTranslateState','frenchTranslateState','russianTranslateState','spanishTranslateState','translateState'].includes(column.dataIndex)">
            <template v-if="record[column.dataIndex] === '0' || record[column.dataIndex] === null">
              <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">未翻译</span>
            </template>
            <template v-if="record[column.dataIndex] === '1'">
              <a-badge color="#FBB31F" /><span style="color:#FBB31F">未审核</span>
            </template>
            <template v-if="record[column.dataIndex] === '2'">
              <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
            </template>
            <template v-if="record[column.dataIndex] === '3'">
              <a-badge color="#36BF7D" /><span style="color:#36BF7D">审核通过</span>
            </template>
          </template>
          <template v-else-if="column.dataIndex === 'editOperation'">
            <div class="editable-row-operations">
              <span v-if="editableData[record.id]">
                <a-tooltip placement="top">
                  <template #title>
                    <span>保存</span>
                  </template>
                  <CheckOutlined style="color:#369FFF;margin-left:8px" @click="edit(record)" />
                </a-tooltip>
                <a-tooltip placement="top">
                  <template #title>
                    <span>取消</span>
                  </template>
                  <CloseOutlined style="color:red;margin-left:8px" @click="cancel(record)" />
                </a-tooltip>
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
              @pressEnter="handleSearch(selectedKeys, confirm, column.dataIndex)" />
            <a-button type="primary" size="small" style="width: 90px; margin-right: 8px"
              @click="handleSearch(selectedKeys, confirm, column.dataIndex)">
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
    <template v-slot:leftBottomBtn>
      <a-button type="primary" size="small" style="margin-left:8px;float:left" class="resetBtn" @click="aggregation">聚合</a-button>
      <a-button type="primary" size="small" style="margin-left:8px;float:left" class="yellowBtn" @click="cancelAggregation">取消聚合</a-button>
    </template>
  </CustomModal>
  <CustomModal :visible="rejectReasonVisible" modalTitle="驳回原因" @handleClose="rejectReasonClose" @handleOK="rejectReasonOK"
    @afterClose="rejectReasonAfterClose">
    <div style="width:100%;height:100%">
      <a-form ref="exportForm" name="custom-validation" :model="rejectReason">
        <a-form-item label="驳回原因" name="reason">
          <a-textarea v-model:value="rejectReason.reason" placeholder="请输入驳回原因" allow-clear />
        </a-form-item>
      </a-form>
    </div>
  </CustomModal>
</template>
<script>
import "@/assets/style/common.less";
import CustomModal from "@/components/modal/index.vue";
import { cloneDeep, iteratee } from "lodash-es";
import {
  getEntryTempByTaskID,
  updateEntryTemp,
  getEntryInfoList,
  updateEntryList,
  deleteEntryInfoByTaskID,
} from "@/http/api/workbench";
import {
  CheckOutlined,
  CloseOutlined,
  ExclamationCircleOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
  SettingOutlined,
  SearchOutlined,
  InfoCircleOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import workbenchCommon from "@/views/workbench/common.js";
import commonParam from "@/utils/commonParam.js";
import common from "../entry/common";
import { setModalAriaHidden } from "@/utils/commonUtils";
import { defineComponent, ref, createVNode } from "vue";
export default {
  components: {
    CheckOutlined,
    CloseOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    SettingOutlined,
    SearchOutlined,
    InfoCircleOutlined,
    CustomModal,
  },
  emits: ["handleClose", "handleOK"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
      default: "词条导入",
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
      tableHeight: { x: "100%", y: "415px" },
      loading: false,
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          width: 50,
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
          title: "存在状态",
          dataIndex: "isExist",
          align: "center",
          width: 100,
          resizable: true,
          fixed: "left",
          index: 1,
          filteredValue: null,
          filters: [
            { text: "已存在", value: 1 },
            { text: "新建", value: 0 },
          ],
          onFilter: (value, record) => record.isExist === value,
        },
        {
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 200,
          resizable: true,
          fixed: "left",
          index: 2,
        },
        {
          title: "翻译",
          dataIndex: "translate",
          align: "center",
          width: 200,
          resizable: true,
          index: 5,
        },
        {
          title: "tag",
          dataIndex: "tag",
          align: "center",
          width: 100,
          resizable: true,
          index: 6,
        },
        {
          title: "comment",
          dataIndex: "comment",
          align: "center",
          width: 100,
          resizable: true,
          index: 7,
        },
        {
          title: "abbr",
          dataIndex: "abbr",
          align: "center",
          width: 100,
          resizable: true,
          index: 14,
        },
        {
          title: "词条状态",
          dataIndex: "entryState",
          align: "center",
          width: 100,
          resizable: true,
          fixed: "right",
          index: 99,
        },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          width: 130,
          resizable: true,
          fixed: "right",
          index: 100,
        },
      ],
      dataSource: [],
      allData: [],
      selectedRowKeys: [],
      selectedRows: [],
      editableData: {},
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
      rules: {},
      entryState: "1",
      selectedRowIndex: null,
      timer: null,
      overlayStyle: workbenchCommon.overlayStyle,
      checkedColumn: workbenchCommon.checkedColumn,
      // checkboxList: workbenchCommon.checkboxList,
      // 移除固定列对应的配置项
      checkboxList: commonParam.checkboxList.filter(
        (item) =>
          ![
            "isExist",
            "translateState",
            "entryState",
            "entry",
            "translate",
          ].includes(item.value)
      ),
      state: {
        searchText: "",
        searchedColumn: "",
      },
      filters: null,
      filteredData: [],
      selectAllName: "全选",
      saveLoading: false,
      rejectReasonVisible: false,
      rejectReason: {
        reason: "",
      },
    };
  },

  created() {},
  mounted() {
    this.task = this.currentTask;
    this.$nextTick(() => {
      // 读取本地存储的用户偏好
      const storedPreferences = localStorage.getItem("colPref-examineModal");
      if (storedPreferences) {
        const preferences = JSON.parse(storedPreferences);
        this.checkedColumn = preferences.displayColumn.split(",");
        this.changeColumn(this.checkedColumn);
      }
    });
  },
  watch: {
    currentTask(newval, oldval) {
      this.task = newval;
      this.task.transMap = commonParam.languageMap[this.task.translateType];
      // console.log("this.task",this.task);
      this.setTranslateColumn();
    },
  },
  methods: {
    // 设置翻译列展示的语言
    setTranslateColumn() {
      this.columns.forEach((item) => {
        if (item.title === "翻译") {
          item.dataIndex =
            workbenchCommon.languageMap[this.task.translateType].code;
        }
      });
    },
    // 获取待审核词条
    getTaskEntry() {
      let params = {
        taskID: this.task.id,
        entryState: this.entryState,
        entry: this.keyWords,
      };
      this.loading = true;
      getEntryInfoList(params, [])
        .then((res) => {
          this.dataSource = res.data.list;
          // 排序  将已存在的词条放到前面
          this.dataSource.sort(function (a, b) {
            return b.isExist - a.isExist;
          });
          this.dataSource.forEach((item) => {
            item.auditState = -1;

            // 配置最大字符长度(此处对应的是翻译的最大字符长度，所以不用maxLength这个属性)
            item.foreignMaxByte =
              this.classifyLimit[item.classfy1]?.["foreignMaxByte"];
            // console.log("打印词条", item);
          });
          // this.allData = this.dataSource
          this.loading = false;
          // this.select()
        })
        .catch((err) => {
          this.loading = false;
          message.error("1", err.message);
          // console.log("err1", err);
        });
    },
    handleOK() {
      this.saveLoading = true;
      for (let key in this.editableData) {
        let entry = this.dataSource.find((item) => item.id === key);
        entry.auditSuggess = this.editableData[key].auditSuggess;
        entry[this.task.transMap.value] =
          this.editableData[key][this.task.transMap.value];
        commonParam.languageList.forEach((item) => {
          if (this.editableData[key][item.interpretation]) {
            entry[item.interpretation] =
              this.editableData[key][item.interpretation];
          }
        }); // 遍历存储外语释义

        entry.chineseInterpretation =
          this.editableData[key].chineseInterpretation;
        entry.tag = this.editableData[key].tag;
        entry.diFileName = this.editableData[key].diFileName;
        entry.comment = this.editableData[key].comment;

        if (entry[this.task.transMap.value] != null) {
          // 翻译存在  则状态为待审核状态
          entry[this.task.transMap.state] = "1";
        }
      }
      this.editableData = {};
      let params = {
        taskID: this.task.id,
      };
      let updateArr = [];
      let okArr = [];
      this.dataSource.forEach((item) => {
        if (item.auditState === 1) {
          // 词条审核通过
          item.entryState = 3;
          updateArr.push(item);
          okArr.push(item);
        } else if (item.auditState === 0) {
          // 词条审核不通过
          item.entryState = 2;
          updateArr.push(item);
        }
      });
      // 校验审核通过的词条
      let num = this.verifyTranslationLength(okArr);
      // let num = 0;
      if (num > 0) {
        // 存在超长
        message.warn("存在超长数据，请检查！");
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
            // console.log("剩余待处理数据的数量：", this.dataSource.length-updateArr.length);
            if (this.dataSource.length == updateArr.length) {
              // 如果没有待处理的数据就自动关闭弹窗
              this.handleClose();
            }
          });
      } else {
        this.saveLoading = false;
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
      if (record.children && record.children.length > 0) {
        record.children.forEach((child) => {
          child.auditState = record.auditState;
        });
      }
    },
    // 驳回标签点击事件
    rejectTagChange(record) {
      if (record.auditState === 0) {
        record.auditState = -1;
      } else {
        record.auditState = 0;
      }
      if (record.children && record.children.length > 0) {
        record.children.forEach((child) => {
          child.auditState = record.auditState;
        });
      }
    },
    // 通过按钮点击事件
    pass() {
      this.selectedRows.forEach((item) => {
        item.auditState = 1;

        if (item.children && item.children.length > 0) {
          item.children.forEach((child) => {
            child.auditState = item.auditState;
          });
        }
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
    },
    //双击表格行 可编辑
    doubleClick(record, index) {
      return {
        // onClick: (event) => {
        //     let _this = this
        //     clearTimeout(this.timer)

        //     this.timer = setTimeout(function () {
        //         _this.selectedRowIndex = record.id
        //     }, 500);
        // },
        onDblclick: (event) => {
          // clearTimeout(this.timer)
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
          this.rules[record.id][this.task.transMap.value] = [
            {
              validator: this.vilidFildLength(record, this.task.transMap.value),
            },
          ];
          this.showEditOperation(); // 显示编辑操作列
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
          maxLength === null ||
          maxLength === "" ||
          maxLength === undefined ||
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
    // 编辑，也是编辑框的回车事件
    edit(record) {
      for (const [key, value] of Object.entries(this.editableData[record.id])) {
        if (record.hasOwnProperty(key)) {
          record[key] = value;
        }
      }

      // 生成表单引用的键名
      const formRefKey = `form${record.id.replaceAll("-", "")}${
        this.task.transMap.value
      }`;
      const formRef = this.$refs[formRefKey];

      // 检查表单引用是否存在
      if (formRef) {
        // 长度校验
        formRef
          .validate()
          .then(() => {
            if (record[this.task.transMap.value] != null) {
              // 翻译存在  则状态为待审核状态
              record[this.task.transMap.state] = "1";
            }
          })
          .catch((err) => {
            message.error("2", err.message);
            // console.log("表单验证失败", formRefKey, formRef.validate());
          });
      } else {
        message.error("未找到对应的表单验证器");
      }
      delete this.editableData[record.id];
      this.hideEditOperation();
    },
    // 取消编辑
    cancel(record) {
      delete this.editableData[record.id];
      this.hideEditOperation();
    },
    // 显示编辑操作列
    showEditOperation() {
      if (this.columns.at(-1).dataIndex === "editOperation") {
        // 如果编辑操作列已经存在，则不再添加
        return;
      }
      const editOperationColumn = {
        title: "编辑操作",
        dataIndex: "editOperation",
        align: "center",
        width: 100,
        resizable: true,
        fixed: "right",
        index: 101, // 确保该列在最右侧，可根据实际情况调整
      };
      this.columns.push(editOperationColumn);
    },
    // 隐藏编辑操作列
    hideEditOperation() {
      if (Object.keys(this.editableData).length === 0) {
        this.columns = this.columns.filter((item) => {
          return item.dataIndex != "editOperation";
        });
      }
    },
    // 聚合
    aggregation() {
      if (this.selectedRows.length < 2) {
        message.warn("请选择两条以上词条聚合！");
      }
      let children = [];
      for (let i = 1; i < this.selectedRows.length; i++) {
        let child = this.selectedRows[i];
        if (child.children && child.children.length > 0) {
          child.children.forEach((item) => {
            children.push(item);
          });
        }
        child.children = [];
        children.push(child);
      }
      children.forEach((item) => {
        item.parentID = this.selectedRows[0].id;

        this.dataSource = this.dataSource.filter((data) => data.id != item.id);
      });
      if (this.selectedRows[0].children) {
        this.selectedRows[0].children =
          this.selectedRows[0].children.concat(children);
      } else {
        this.selectedRows[0].children = children;
      }

      // this.allData = this.dataSource
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    // 取消聚合
    cancelAggregation() {
      // console.log(this.selectedRows)
      this.selectedRows.forEach((item) => {
        if ((item.parentID === "" || item.parentID === null) && item.children) {
          let index = this.dataSource.findIndex(
            (entry) => entry.id === item.id
          );
          for (let i = 0; i < item.children.length; i++) {
            let child = item.children[i];
            child.parentID = "";
            this.dataSource.splice(index + i + 1, 0, child);
          }
          item.children = [];
        } else {
          let parent = this.dataSource.find(
            (data) => data.id === item.parentID
          );
          parent.children = parent.children.filter(
            (child) => child.id != item.id
          );
          let index = this.dataSource.findIndex(
            (data) => data.id === item.parentID
          );
          item.parentID = "";
          this.dataSource.splice(index + 1, 0, item);
        }
      });
      // this.allData = this.dataSource
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    afterClose() {
      this.editableData = {};
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.keyWords = "";
      this.pagination.current = 1;
      this.pagination.pageSize = 20;
      this.selectAllName = "全选";

      this.clearFilters();
    },
    // 删除词条
    deleteTaskEntry() {
      if (this.selectedRows.length === 0) {
        return;
      }
      Modal.confirm({
        title: "是否确定删除?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          let deleteIds = [];
          this.selectedRows.forEach((item) => {
            deleteIds.push(item.id);
            if (item.children && item.children.length > 0) {
              item.children.forEach((child) => {
                deleteIds.push(child.id);
              });
            }
          });
          deleteEntryInfoByTaskID({ taskID: this.task.id }, deleteIds)
            .then((res) => {
              message.success("删除成功！");
              this.getTaskEntry();
            })
            .catch((err) => {
              message.error("删除失败！", err.message);
            });
        },
        onCancel: () => {},
      });
    },
    // 展示列切换
    changeColumn(checkedValue) {
      this.checkedColumn = checkedValue;

      this.checkboxList.forEach((value) => {
        // 查找当前勾选列表中是否存在该列
        let checkedIndex = this.checkedColumn.findIndex(
          (item) => item === value.value
        );
        // 查找当前表格列中是否存在该列
        let nowColumnIndex = this.columns.findIndex(
          (item) => item.dataIndex === value.value
        );
        // 若勾选状态和列存在状态一致，则跳过
        if (
          (nowColumnIndex !== -1 && checkedIndex !== -1) ||
          (nowColumnIndex === -1 && checkedIndex === -1)
        ) {
          return;
        }
        // 若勾选了但列不存在，则添加列
        if (nowColumnIndex === -1 && checkedIndex !== -1) {
          let newCol = {
            title: value.label,
            dataIndex: value.value,
            align: "center",
            width: 100,
            ellipsis: true,
            resizable: true,
            index: value.index,
          };
          if (
            ["isExist", "translateState", "entry"].includes(newCol.dataIndex)
          ) {
            newCol.fixed = "left";
          }
          if (["auditSuggess", "entryState"].includes(newCol.dataIndex)) {
            newCol.fixed = "right";
          }
          if (newCol.dataIndex === "entrySource") {
            // 添加词条来源可筛选
            newCol.customFilterDropdown = true;
            newCol.filteredValue = null;
            newCol.onFilter = (value, record) =>
              record.entrySource
                .toString()
                .toLowerCase()
                .includes(value.toLowerCase());
          }
          this.columns.splice(-1, 0, newCol);
        }
        // 若未勾选但列存在，则移除列
        if (nowColumnIndex !== -1 && checkedIndex === -1) {
          this.columns.splice(nowColumnIndex, 1);
        }
      });

      this.columns.sort((a, b) => a.index - b.index);

      // 记录
      let data = {
        displayColumn: checkedValue.join(","),
      };
      // this.recordPartiality(data);
      localStorage.setItem("colPref-examineModal", JSON.stringify(data)); // localStorage存储用户偏好
    },
    // 列筛选
    handleSearch(selectedKeys, confirm, dataIndex) {
      confirm();
      this.state.searchText = selectedKeys[0];
      this.state.searchedColumn = dataIndex;
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
          ? this.editableData[record.id][this.task.transMap.value]
          : record[this.task.transMap.value];
        if (common.byteLength(text) > maxLength) {
          // 若校验不通过，会调用 addEdit 方法将该词条设为编辑状态，并对其表单进行校验
          flag++;
          this.addEdit(record).then((res) => {
            eval(
              "this.$refs.form" +
                record.id.replaceAll("-", "") +
                this.task.transMap.value
            )
              .validate()
              .then(() => {})
              .catch((err) => {
                // message.error("3", err.message);
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
      this.rules[record.id][this.task.transMap.value] = [
        { validator: this.vilidFildLength(record, this.task.transMap.value) },
      ];
      return Promise.resolve();
    },
    // 表格change事件
    handleTableChange(pagination, filters) {
      this.filters = filters;
      for (let key in filters) {
        this.columns.forEach((col) => {
          if (col.dataIndex === key) {
            col.filteredValue = filters[key];
          }
        });
      }
      // 获取筛选后的数据
      let isExistData = this.dataSource.filter((item) => {
        return filters.isExist && filters.isExist.includes(item.isExist);
      });
      let sourceData = this.dataSource.filter((item) => {
        return (
          filters.entrySource && item.entrySource.includes(filters.entrySource)
        );
      });
      this.filteredData = this.intersection(isExistData, sourceData);
    },
    // 两个数组取并集
    intersection(nums1, nums2) {
      if (nums1.length === 0) {
        return nums2;
      }
      if (nums2.length === 0) {
        return nums1;
      }
      let a = new Set(nums1);
      let b = new Set(nums2);
      let arr = Array.from(new Set([...b].filter((x) => a.has(x))));
      return arr;
    },
    // 清空表格筛选条件
    clearFilters() {
      if (this.filters) {
        for (let key in this.filters) {
          this.columns.forEach((col) => {
            if (col.dataIndex === key) {
              col.filteredValue = null;
            }
          });
        }
      }
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
        item.auditSuggess = this.rejectReason.reason;
        if (item.children && item.children.length > 0) {
          item.children.forEach((child) => {
            child.auditState = item.auditState;
            child.auditSuggess = this.rejectReason.reason;
          });
        }
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