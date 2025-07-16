<template>
  <Modal :visible="visible" :modalTitle="modalTitle" :modalWidth="modalWidth" okText="保存" :okLoading="saveLoading" :fullFlag="true"
    @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose" @setTableHeight="setTableHeight">
    <div class="content">
      <div class="table">
        <div class="taskInfo">
          <div class="taskItem">任务名称：{{task.name}}</div>
          <div class="taskItem">产品名称：{{task.productName}}</div>
          <div class="taskItem">上级分类名称：{{task.classifyName}}</div>
          <div class="taskItem">翻译语种：{{task.translateType}}</div>
          <div class="taskItem"><a-checkbox v-model:checked="shouldCheckSykEntry">先校验再保存</a-checkbox></div>
        </div>
        <div class="form">
          <a-row :gutter="8" justify="space-between">
            <a-col :span="12">
              <a-row :gutter="8" justify="start" class="search-row">
                <a-col :span="12">
                  <div class="inline-left-align">
                    <span>词条：</span>
                    <a-input v-model:value="search.keyWords" size="small" placeholder='请输入词条搜索' />
                  </div>
                </a-col>
                <a-col :span="12">
                  <div class="inline-left-align">
                    <span>翻译状态：</span>
                    <a-select v-model:value="search.translateState" style="width: 150px" placeholder="请选择" :options='translateStates' size="small"
                      @click="clickInput" allowClear>
                    </a-select>
                  </div>
                </a-col>
                <a-col :span="12">
                  <div class="inline-left-align">
                    <span>部门所属：</span>
                    <a-select v-model:value="search.department" style="width: 186px" placeholder="请选择" :options='departments' size="small"
                      @click="clickInput" allowClear>
                    </a-select>
                  </div>
                </a-col>
              </a-row>
            </a-col>
            <a-col :span="12">
              <a-row :gutter="8" justify="start" class="search-row">
                <a-col>
                  <a-button type="primary" size="small" @click="getSearch">查询</a-button>
                </a-col>
                <a-col>
                  <a-button type="primary" size="small" class="resetBtn" @click="preTranslation">预翻译</a-button>
                </a-col>
                <a-col>
                  <a-button type="primary" size="small" @click="exportExcel">导出Excel</a-button>
                </a-col>
                <a-col>
                  <a-upload name="file" :beforeUpload="beforeUpload" :accept="accept" :showUploadList="false" @change="handleChange">
                    <a-button type="primary" size="small">翻译导入</a-button>
                  </a-upload>
                </a-col>
                <a-col>
                  <a-dropdown>
                    <template #overlay>
                      <a-menu @click="capitalizeWordsClick">
                        <a-menu-item key="upper">
                          首字母大写
                        </a-menu-item>
                        <a-menu-item key="lower">
                          首字母小写
                        </a-menu-item>
                        <a-menu-item key="replace">
                          查找替换
                        </a-menu-item>
                      </a-menu>
                    </template>
                    <a-button size="small" type="primary">
                      翻译调整
                      <DownOutlined />
                    </a-button>
                  </a-dropdown>
                </a-col>
                <a-col>
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
                    <a-button type="primary" size="small"><template #icon>
                        <SettingOutlined />
                      </template>展示列</a-button>
                  </a-popover>
                </a-col>
              </a-row>
            </a-col>
          </a-row>
        </div>
        <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :row-key="record => record.id" :scroll="tableHeight"
          :pagination='pagination' :loading="loading" :rowClassName="getRowClassName" childrenColumnName="child" ref="tableContainer"
          @resizeColumn="handleResizeColumn" :row-selection="{selectedRowKeys: selectedRowKeys, 
                    onChange: onSelectChange,
                    selections:[
                        {key:'selectAll',text:'全部选择',onSelect:selectAllEntry},
                        {key:'clearAll',text:'取消选择',onSelect:clearAllEntry}
                    ]
                }" :customRow="customRow">
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
                      <a-input :ref="'ref'+record.id.replaceAll('-','')" v-model:value="editableData[record.id][column.dataIndex]"
                        style="margin: -5px 0" @pressEnter="inputPressEnter(record)" @click="clickInput" @change="changeInput(record)" />
                    </a-form-item>
                  </a-form>
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
      <div class="suggest">
        <div style="height:30px">
          <span style="float:right;font-size:12px">
            <a-tooltip placement="left">
              <template #title>
                <table>
                  <tr>
                    <td style="width:100px">上一个</td>
                    <td>Ctrl + ↑</td>
                  </tr>
                  <tr>
                    <td style="width:100px">下一个</td>
                    <td>Ctrl + ↓</td>
                  </tr>
                  <tr>
                    <td style="width:100px">上一个未翻译</td>
                    <td>Ctrl + Shift + ↑</td>
                  </tr>
                  <tr>
                    <td style="width:100px">下一个未翻译</td>
                    <td>Ctrl + Shift + ↓</td>
                  </tr>
                  <tr>
                    <td style="width:100px">编辑 </td>
                    <td>Ctrl + e</td>
                  </tr>
                  <tr>
                    <td style="width:100px">保存 </td>
                    <td>Ctrl + Enter</td>
                  </tr>
                </table>
              </template>
              快捷键
              <QuestionCircleOutlined />
            </a-tooltip>
          </span>
        </div>
        <div style="margin-bottom: 6px;">词条释义：</div>
        <div class="suggentContent">
          <div>
            <span class="title">中文释义：</span>
            <span>{{chineseInterpretation}}</span>
          </div>
          <div>
            <span class="title">英文释义：</span>
            <span>{{englishInterpretation}}</span>
          </div>
        </div>
        <div style="margin-bottom: 6px;">翻译建议：</div>
        <a-spin :spinning="spinning" tip="翻译中....">
          <div class="suggentContent">
            <span class="title">本地翻译：</span>
            <template v-for="(item,index) in suggest.local" :key="index">
              <div class="suggentItem" @click="suggestClick(item.title,item.id)">
                <div class="tran">
                  <img src="../../assets/icon/local.png" style="width:24px;height:24px;margin-right:8px" />
                  <span>{{item.title}}</span>
                </div>
                <div class="tips">
                  {{item.tips}}
                  <span v-if='index < 9'>
                    Ctrl+{{index + 1}}
                  </span>
                </div>
              </div>
            </template>
            <span class="title">外网翻译：</span>
            <template v-for="(item,index) in suggest.web" :key="index">
              <div class="suggentItem" @click="suggestClick(item.title,item.id)">
                <div class="tran">
                  <img :src="require('../../assets/icon/'+item.type+'.png')" style="width:24px;height:24px;margin-right:8px" />
                  <span>{{item.title}}</span>
                </div>
                <div class="tips">
                  {{item.tips}}
                  <span v-if="index + this.suggest.local.length < 9">
                    Ctrl+{{index + this.suggest.local.length + 1}}
                  </span>
                </div>
              </div>
            </template>
          </div>
        </a-spin>
      </div>
    </div>
  </Modal>
  <Modal :visible="selectVisible" modalTitle="预翻译" :okLoading="preTranslateOkLoading" @handleClose="selectHandleClose" @handleOK="selectHandleOK"
    @afterClose="selectAfterClose">
    <div style="width:100%;height:100%">
      <a-form ref="formRef" name="custom-validation" autocomplete='off' :label-col="labelCol" :model="preTran">
        <a-form-item label="优先级" name="priority" :rules="[{ required: true, message: '请选择优先级!' }]">
          <a-select v-model:value="preTran.priority" placeholder="请选择" allowClear>
            <a-select-option value="shuyuku">术语库</a-select-option>
            <a-select-option value="deepl">DeepL翻译</a-select-option>
            <a-select-option value="youdao">有道翻译</a-select-option>
            <a-select-option value="baidu">百度翻译</a-select-option>
            <a-select-option value="google">Google翻译</a-select-option>
            <a-select-option value="module">本地模型</a-select-option>
            <a-select-option value="synthesis">
              综合优先级
              <a-tooltip placement="top">
                <template #title>
                  <span>使用所有的翻译引擎进行翻译，取出现次数最多的翻译为当前词条的翻译！</span>
                </template>
                <info-circle-outlined style="float:right;color:#FBB31F;margin-top:5px" />
              </a-tooltip>
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </div>
  </Modal>
  <Modal :visible="exportVisible" modalTitle="导出" @handleClose="exportClose" @handleOK="exportOK" @afterClose="exportAfterClose">
    <div style="width:100%;height:100%">
      <a-form ref="exportForm" name="custom-validation" :model="exportModal">
        <a-form-item label="导出字段" name="field" :rules="[{ required: true, message: '请选择导出字段!' }]">
          <a-select mode="multiple" v-model:value="exportModal.field" :options="fieldOptions" :fieldNames="{label:'label',value:'label'}"
            placeholder="请选择" allowClear></a-select>
        </a-form-item>
      </a-form>
    </div>
  </Modal>
  <Modal :visible="replaceVisible" modalTitle="查找替换" @handleClose="replaceClose" @handleOK="replaceOK" @afterClose="replaceAfterClose">
    <div style="width:100%;height:100%">
      <a-form ref="replaceForm" name="custom-validation" :model="replaceModal">
        <a-form-item label="原文本" name="sourceStr" :rules="[{ required: true, message: '请输入原文本!' }]">
          <a-input v-model:value="replaceModal.sourceStr" placeholder='请输入原文本' />
        </a-form-item>
        <a-form-item label="替换为" name="replaceStr" :rules="[{ required: true, message: '请输入替换文本!' }]">
          <a-input v-model:value="replaceModal.replaceStr" placeholder='请输入替换文本' />
        </a-form-item>
      </a-form>
    </div>
  </Modal>
</template>
<script>
import "@/assets/style/common.less";
import Modal from "@/components/modal/index.vue";
import { cloneDeep } from "lodash-es";
import {
  getEntryTempByTaskID,
  preTranslate,
  getEntryInfoList,
  updateEntryList,
  importCommonExcle,
  capitalizeWords,
  replaceWords,
} from "@/http/api/workbench";
import { translate, workImportExcleTrans } from "@/http/api/entryManage";
import { entryExportByCondition } from "@/http/api/download";
import { importExcle } from "@/http/api/entry";
import {
  queryUserPartiality,
  updateUserPartiality,
} from "@/http/api/userPartiality";
import { checkSykEntryBeforeSave } from "@/http/api/glossary";
import {
  QuestionCircleOutlined,
  SearchOutlined,
  InfoCircleOutlined,
  DownOutlined,
  SettingOutlined,
} from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import key from "keymaster";
import {
  clickInput,
  setTableHeight,
  handleResizeColumn,
  getRowClassName,
  pageChange,
  getColPref,
  changeColumn,
  setModalAriaHidden,
  byteLength,
  encodeParams,
} from "@/utils/commonUtils"; // 引入工具函数
import commonParam, {
  entryParams,
  workbenchParams,
} from "@/utils/commonParam.js";
export default {
  components: {
    Modal,
    QuestionCircleOutlined,
    SearchOutlined,
    InfoCircleOutlined,
    DownOutlined,
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
      default: "词条翻译",
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
      modalWidth: "75%",
      task: {},
      search: {
        keyWords: "",
        translateState: null,
        department: null,
      },
      // tableHeight: { x: "100%", y: 415 },
      tableHeight: { x: "max-content", y: 415 },
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
          title: "翻译状态",
          dataIndex: "translateState", // 动态的
          align: "center",
          width: 100,
          resizable: true,
          fixed: "left",
          index: 2,
        },
        {
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 100,
          resizable: true,
          fixed: "left",
          index: 4,
          // 添加 sorter 属性实现排序功能
          sorter: (a, b) => a.entry.localeCompare(b.entry),
          sortDirections: ["ascend", "descend"],
        },
        {
          title: "翻译",
          dataIndex: "translate", // 动态的
          align: "center",
          width: 100,
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
          title: "审核意见",
          dataIndex: "this.commonParam.auditSuggest", // 动态的
          align: "center",
          width: 100,
          resizable: true,
          fixed: "right",
          index: 99,
        },
      ],
      dataSourceAll: [], // 所有数据
      dataSource: [], // 展示的数据（可能经历过过滤）
      selectedRowKeys: [],
      selectedRows: [],
      selectedRowsCount: {
        saveLen: 0,
        errorLen: 0,
        noTranslateLen: 0,
      }, // 保存词条数量统计
      selectVisible: false,
      selectTitle: "",
      selectedRowIndex: null,
      redHighlightIds: [],
      translateStates: [
        { label: "未翻译", value: "0" },
        { label: "待审核", value: "1" },
        { label: "审核不通过", value: "2" },
        // { label: "已审核", value: "3" },
      ], // 翻译状态
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
      suggest: {
        local: [],
        web: [],
      },
      spinning: false,
      timer: null,
      preTran: {
        priority: null,
      },
      labelCol: { style: { width: "80px" } },
      chineseInterpretation: "",
      englishInterpretation: "",
      rules: {},
      exportVisible: false,
      exportModal: {
        field: ["abbr", "词条"],
      },
      // fieldOptions: entryParams.exportFields,
      fieldOptions: entryParams.checkboxList,
      accept: ".xls,.xlsx",
      preTranslateOkLoading: false,
      state: {
        searchText: "",
        searchedColumn: "",
      },
      clearFilters: null,
      saveLoading: false,
      replaceVisible: false,
      replaceModal: {
        sourceStr: null,
        replaceStr: null,
      },
      commonParam: {
        value: "",
        state: "",
        auditSuggest: "",
      },
      shouldCheckSykEntry: false, // 新增勾选状态变量
      overlayStyle: workbenchParams.overlayStyle, // 展示列
      checkedColumn: workbenchParams.checkedColumn, // 展示列切换
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
      departments: commonParam.departmentList.map((item) => ({
        label: item.label,
        value: item.label,
      })),
    };
  },
  watch: {
    currentTask(newval, oldval) {
      this.task = newval;
      this.commonParam = commonParam.languageList.find(
        (it) => it.name === this.task.translateType
      );
      this.search.department = this.task.department; // 默认部门
      // workbenchParams.languageMap[this.task.translateType].code
      this.setTranslateColumn();
    },
    redHighlightIds(newval, oldval) {
      // 强制更新表格，触发 customRow 重新渲染样式
      this.$forceUpdate();
    },
  },
  methods: {
    // 设置翻译列展示的语言
    setTranslateColumn() {
      this.columns.forEach((item) => {
        if (item.title === "翻译") {
          item.dataIndex = this.commonParam.value;
        }
        if (item.title === "翻译状态") {
          item.dataIndex = this.commonParam.state;
        }
        if (item.title === "审核意见") {
          item.dataIndex = this.commonParam.auditSuggest;
        }
      });
    },
    dynamicSortFunction(a, b) {
      if (a[this.commonParam.value] === null) {
        return -1;
      }
      if (b[this.commonParam.value] === null) {
        return 1;
      }
      return a[this.commonParam.value].localeCompare(b[this.commonParam.value]);
    },
    initTranslateEntry() {
      this.getTranslateEntry();
      this.init();
    },
    init() {
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
        _this.nextNotTransEntry();
        return false;
      });
      key("ctrl+shift+up", function () {
        _this.prevNotTransEntry();
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
    },
    // 根据查询条件，过滤dataSource
    getSearch() {
      this.loading = true;
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.selectedRowIndex = null;
      this.dataSource = this.allData.filter((item) => {
        const keywordMatch =
          !this.search.keyWords || item.entry.includes(this.search.keyWords);
        const stateMatch =
          !this.search.translateState ||
          item[this.commonParam.state] === this.search.translateState;
        return keywordMatch && stateMatch;
      });
      this.pagination.current = 1;
      this.pagination.total = this.dataSource.length;
      this.loading = false;
    },
    // 获取待翻译词条
    async getTranslateEntry() {
      this.loading = true;
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.selectedRowIndex = null;
      this.allData = [];
      this.dataSource = [];
      this.setTranslateColumn(); // 设置翻译列展示的语言
      const params = {
        taskID: this.task.id,
        entryState: "3",
        entry: this.search.keyWords,
      };
      const data = ["0", "2"];
      await getEntryInfoList(params, data)
        .then((res) => {
          this.allData = res.data.list;
          if (this.allData.length > 0) {
            this.selectedRowIndex = this.allData[0].id;
            this.assistedTranslation(this.allData[0]);
          }
          this.allData.forEach((item) => {
            // 前端提供了翻译状态的展示，是否合理？
            if (!item[this.commonParam.state]) {
              item[this.commonParam.state] = "0";
            }
          });
          this.dataSource = this.allData; // 在刚取到时，无过滤，所以全量克隆
          this.pagination.current = 1;
          this.pagination.total = this.dataSource.length;
        })
        .catch((err) => {
          message.error(err.message);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    handleOK() {
      if (this.selectedRows.length === 0) {
        message.info("请选择需要保存的数据！");
        return;
      }
      this.saveLoading = true;
      this.loading = true;
      this.selectedRowsCount = {
        saveLen: 0,
        errorLen: 0,
        noTranslateLen: 0,
      };

      // 更新编辑数据
      this.saveEditEntry();
      // 更新选中词条
      this.updateNewByOld(this.selectedRows, this.dataSource);
      // 设置翻译状态，再记录不同类型的词条数量
      this.selectedRows.forEach((item) => {
        // 点击保存后前端执行对翻译状态的更新，是否合理？
        // 记录是否翻译，更新翻译状态
        if (!item[this.commonParam.value]) {
          this.selectedRowsCount.noTranslateLen++; // 未翻译数量
          item[this.commonParam.state] = "0"; // 待翻译
        } else {
          this.selectedRowsCount.saveLen++; // 已翻译数量
          if (["0", "2"].includes(item[this.commonParam.state])) {
            item[this.commonParam.state] = "1"; // 待审核(只有“1”才能通过保存)
          }
          // 如校验失败时保留在翻译页面，审核不通过的仍是审核不通过状态
        }
      });
      //// 不用，因为保存后会重新查询
      // this.updateNewByOld(this.dataSource, this.selectedRows);
      // this.updateNewByOld(this.allData, this.selectedRows);
      // console.log("修改翻译状态", this.selectedRows);
      // 校验翻译长度
      let num = this.verifyTranslationLength(this.selectedRows);
      if (num > 0) {
        message.warn(`超长翻译${num}条`);
        this.saveLoading = false;
        this.loading = false;
        return;
      }

      // 先校验再执行（若勾选
      if (this.shouldCheckSykEntry) {
        this.checkSykEntryBeforeSave().then(() => {
          this.updateEntryList();
        });
      } else {
        this.updateEntryList();
      }
    },
    // 保存编辑数据
    saveEditEntry() {
      for (let key in this.editableData) {
        let entry = this.dataSource.find((item) => item.id === key);
        entry[this.commonParam.value] =
          this.editableData[key][this.commonParam.value];
        entry[this.commonParam.transIdName] =
          this.editableData[key][this.commonParam.transIdName];
      }
      this.editableData = {};
    },
    // 先校验再保存
    checkSykEntryBeforeSave() {
      const datas = []; // 已翻译的词条
      Object.values(this.selectedRows).forEach((item) => {
        if (item[this.commonParam.value]) {
          const data = {
            id: item.id,
            entry: item.entry,
            translate: item[this.commonParam.value],
          };
          datas.push(data);
        }
      });
      return checkSykEntryBeforeSave(datas)
        .then((res) => {
          this.redHighlightIds = res.data.map((item) => item.id);
          // 过滤掉异常数据
          this.selectedRows = this.selectedRows.filter(
            (item) => !this.redHighlightIds.includes(item.id)
          );
          this.selectedRowKeys = this.selectedRowKeys.filter(
            (item) => !this.redHighlightIds.includes(item)
          );

          this.selectedRowsCount.errorLen = this.redHighlightIds.length;
          this.selectedRowsCount.saveLen -= this.selectedRowsCount.errorLen;
          if (this.selectedRowsCount.errorLen > 0) {
            message.warn(
              `校验结束，共有${this.selectedRowsCount.errorLen}条翻译异常，未保存！`,
              1
            );
          }
        })
        .catch((err) => {
          message.error("校验失败！", err);
        });
    },
    // 保存
    updateEntryList() {
      const updateParams = {
        taskID: this.task.id,
      };
      this.updateNewByOld(this.selectedRows, this.dataSource);
      updateEntryList(updateParams, this.selectedRows)
        .then(async (res) => {
          if (this.selectedRowsCount.noTranslateLen > 0) {
            message.warn(
              `未翻译${this.selectedRowsCount.noTranslateLen}条，未保存！`,
              1
            );
          }
          if (this.selectedRowsCount.saveLen > 0) {
            message.success(
              `翻译成功${this.selectedRowsCount.saveLen}条，已保存！`,
              1
            );
          }
          await this.getTranslateEntry();
          if (this.allData.length == 0) this.handleClose();
        })
        .catch((err) => {
          message.error("操作失败！", err, 1);
        })
        .finally(() => {
          this.saveLoading = false;
          this.loading = false;
        });
    },
    handleClose() {
      this.redHighlightIds = []; // 重新查询前要把异常数据清空
      this.$emit("handleClose");
    },
    getRowClassName(record, index) {
      return getRowClassName(record, index, this.selectedRowIndex);
    },
    // 添加表格行点击事件
    customRow(record, index) {
      return {
        onClick: (event) => {
          this.selectedRowIndex = record.id;
          this.assistedTranslation(record);
        },
        onDblclick: (event) => {
          if (this.editableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }
          this.edit(record);
        },
        style: {
          // 标红的那一行的<tr></tr>，文字颜色变红
          color: this.redHighlightIds.includes(record.id) ? "red" : "inherit",
        },
      };
    },
    edit(record) {
      this.editableData[record.id] = this.editableData.hasOwnProperty(record.id)
        ? this.editableData[record.id]
        : cloneDeep(this.allData.filter((item) => record.id === item.id)[0]);
      // 设置校验规则
      this.rules[record.id] = {
        entry: [
          { validator: this.vilidFildLength(record, "chinese") },
          { required: true, message: "请输入!" },
        ],
      };
      this.rules[record.id][this.commonParam.value] = [
        { validator: this.vilidFildLength(record, this.commonParam.value) },
      ];
      return Promise.resolve();
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
        // let length = common.byteLength(value);
        let length = byteLength(value);
        if (length > maxLength) {
          return Promise.reject("允许最大字符数为" + maxLength + "！");
        }
        return Promise.resolve();
      };
    },
    handleResizeColumn: (w, col) => {
      col.width = w;
    },
    // 辅助翻译
    assistedTranslation(record) {
      // 设置翻译建议中的中文释义  英文释义
      this.chineseInterpretation = record.chineseInterpretation;
      this.englishInterpretation = record.englishInterpretation;

      this.spinning = true;
      let params = {
        // name: encodeParams(record.entry),
        name: record.entry,
        type: this.task.translateType,
        department: this.search.department,
      };
      // 清空 快捷键
      this.deleteShortcutKeys();

      translate(params)
        .then((res) => {
          // console.log("辅助翻译", res.data);
          this.suggest = {
            local: [],
            web: [],
          };
          res.data.translateEntities.forEach((element) => {
            if (
              element.languageEntities.length == 0 ||
              (element.languageEntities.length == 1 &&
                element.languageEntities[0] == null)
            )
              return;
            if (element.source.includes("本地翻译")) {
              element.languageEntities.forEach((item) => {
                let suggent = {
                  title: item.value,
                  tips: element.source,
                  type: "local",
                  id: item.id,
                };
                this.suggest.local.push(suggent);
              });
            } else {
              let type = "";
              if (element.source.includes("百度")) {
                type = "baidu";
              } else if (element.source.includes("有道")) {
                type = "youdao";
              } else if (element.source.includes("Google")) {
                type = "google";
              } else if (element.source.includes("模型")) {
                type = "ai";
              } else if (element.source.includes("DeepL")) {
                type = "DeepL";
              }
              element.languageEntities.forEach((item) => {
                let suggent = {
                  title: item.value,
                  tips: element.source,
                  type: type,
                  id: item.id,
                };
                this.suggest.web.push(suggent);
              });
            }
          });
          this.spinning = false;
          this.setShortcutKeys();
        })
        .catch((err) => {
          this.suggest = {
            local: [],
            web: [],
          };
          this.spinning = false;
        });
    },
    // 设置翻译建议快捷键
    setShortcutKeys() {
      let _this = this;
      let list = this.suggest.local.concat(this.suggest.web);
      for (let i = 1; i <= list.length; i++) {
        if (i < 10) {
          key("ctrl+" + i, function () {
            _this.clickSug(i);
            return false;
          });
        }
      }
    },
    // 删除辅助翻译快捷键
    deleteShortcutKeys() {
      let list = this.suggest.local.concat(this.suggest.web);
      for (let i = 0; i <= list.length; i++) {
        if (i < 9) {
          key.unbind("ctrl+" + i + 1);
        }
      }
      key.unbind(
        "ctrl+down,ctrl+up,ctrl+shift+down,ctrl+shift+up,ctrl+e,ctrl+enter"
      ); // 解绑快捷键
    },
    // 辅助翻译快捷键点击事件：当用户按下与翻译建议对应的快捷键时，调用此方法来应用对应的翻译建议
    clickSug(i) {
      // 将本地翻译建议和外网翻译建议合并成一个列表
      let list = this.suggest.local.concat(this.suggest.web);
      // 调用 suggestClick 方法，传入对应序号的翻译建议的标题和 ID
      this.suggestClick(list[i - 1].title, list[i - 1].id);
    },
    suggestClick(title, id) {
      // 检查是否有选中的词条，如果没有则直接返回，不进行后续操作
      if (this.selectedRowIndex === null) {
        return;
      }
      // 从数据源中查找当前选中的词条记录
      let record = this.dataSource.find(
        (item) => item.id === this.selectedRowIndex
      );

      record[this.commonParam.value] = title;
      // record[transIdName] = id

      if (this.editableData[this.selectedRowIndex] != undefined) {
        // 如果处于编辑状态，将翻译建议的标题赋值给编辑数据中的翻译字段
        this.editableData[this.selectedRowIndex][this.commonParam.value] =
          title;
        // this.editableData[this.selectedRowIndex][transIdName] = id

        // 如果有子词条  则写入子词条
        if (
          this.editableData[this.selectedRowIndex].children &&
          this.editableData[this.selectedRowIndex].children.length > 0
        ) {
          // 如果有子词条，将翻译建议的标题应用到所有子词条的翻译字段上
          this.editableData[this.selectedRowIndex].children.forEach((item) => {
            item[this.commonParam.value] = title;
            // item[transIdName] = id
          });
        }
      }

      // 校验字符串长度
      this.verifyTranslationLength([record]);
    },
    // 输入框 回车事件
    async inputPressEnter(record) {
      // 长度校验
      const formRefName = `form${record.id.replaceAll("-", "")}${
        this.commonParam.value
      }`;
      const formRef = this.$refs[formRefName];
      if (formRef) {
        try {
          await formRef.validate();
          record[this.commonParam.value] =
            this.editableData[record.id][this.commonParam.value];
          record[this.commonParam.transIdName] =
            this.editableData[record.id][this.commonParam.transIdName];
          delete this.editableData[record.id];
        } catch (err) {
          console.error("输入框回车验证失败:", err);
          message.error("输入验证失败，请检查输入内容", err.message);
        }
      }
    },
    // 使用old数据来更新new数据
    updateNewByOld(New, Old) {
      // updateNewByOld(this.allData,this.dataSource) 据dataSource（过滤后的展示数据）来更新dataSourceAll（不经过滤的全量数据）
      // updateNewByOld(this.selectedRows,this.dataSource) 执行保存updateEntryList前更新一下已选数据
      const idToIndexMap = new Map();
      New.forEach((item, index) => {
        idToIndexMap.set(item.id, index);
      });

      Old.forEach((item) => {
        const index = idToIndexMap.get(item.id);
        if (index !== undefined) {
          New[index] = item;
        }
      });
    },
    // 预翻译
    preTranslation() {
      if (this.dataSource.length === 0) {
        return;
      }
      this.selectVisible = true;
      setModalAriaHidden(this, document);
      // 顺势执行@handleOK="selectHandleOK"
    },
    selectHandleOK() {
      this.$refs.formRef.validate().then(() => {
        this.preTranslateOkLoading = true;
        let params = {
          taskID: this.task.id,
          priority: this.preTran.priority,
        };
        this.loading = true;
        // 将 编辑数据对应 的 dataSource 的翻译都变成空，以便被预翻译覆盖
        this.dataSource.forEach((item) => {
          if (
            Object.keys(this.editableData).some(
              (key) => this.editableData[key].id === item.id
            )
          ) {
            item[this.commonParam.value] = "";
          }
        });
        this.editableData = []; // 取消所有编辑状态
        preTranslate(params, this.dataSource)
          .then((res) => {
            // console.log("预翻译结果：", res.data.list);
            // console.log("预翻译语种：", this.commonParam.value);
            // 更新 dataSource 中的翻译数据
            this.dataSource = res.data.list.map((item) => {
              item.translate = item[this.commonParam.value];
              // if (!item.translate) {
              //   console.log(`${item.entry}没有翻译数据`, item);
              // }
              return item;
            });
            this.updateNewByOld(this.allData, this.dataSource); // 也更新一下全量数据
            // // console.log("编辑数据有：", Object.keys(this.editableData));
            // // 修改编辑中数据值
            // for (let key in this.editableData) {
            //   this.editableData[key] = this.dataSource.find(
            //     (item) => item.id === key
            //   );
            // }
            // 校验翻译长度
            let num = this.verifyTranslationLength(this.dataSource);
            if (num > 0) {
              message.warn("存在超长翻译,请检查！");
            }
          })
          .catch((err) => {
            message.error("预翻译失败！", err.message);
          })
          .finally(() => {
            this.loading = false;
            this.selectVisible = false;
            this.preTranslateOkLoading = false;
          });
      });
    },
    selectHandleClose() {
      this.selectVisible = false;
    },
    selectAfterClose() {
      this.preTran.priority = null;
    },
    clickInput(event) {
      event.stopPropagation();
    },
    changeInput(record) {
      record.translateID = "";
    },
    afterClose() {
      this.pagination.current = 1;
      this.pagination.pageSize = 20;
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.editableData = {};
      if (this.clearFilters) {
        this.clearFilters({ confirm: true });
        this.state.searchText = "";
      }
      this.selectedRowIndex = null;
      this.search = {
        keyWords: "",
        translateState: null,
        department: this.task.department,
      };
      this.deleteShortcutKeys(); // 清空 辅助翻译快捷键
      this.suggest = {
        local: [],
        web: [],
      };
    },

    // 导出
    exportExcel() {
      if (this.dataSource.length === 0) {
        return;
      }
      this.exportVisible = true;
      setModalAriaHidden(this, document);

      this.queryPartiality();
    },
    exportClose() {
      this.exportVisible = false;
    },
    exportOK() {
      this.$refs.exportForm
        .validate()
        .then(() => {
          // 导出接口
          let fields = ["id"].concat(this.exportModal.field);
          let data = {
            columnNames: fields,
            entryInfoEntities: this.dataSource,
            excelName: this.task.name + "_",
          };
          let params = {
            taskID: this.task.id,
          };
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
            this.exportVisible = false;
          });

          // 记录偏好
          this.exportFieldChange(this.exportModal.field);
        })
        .catch((err) => {
          message.error("7", err.message);
        });
    },
    exportAfterClose() {
      this.exportModal.field = ["abbr", "词条"];
    },
    beforeUpload(file, fileList) {
      // console.log("before");
      return false;
    },
    handleChange(info) {
      let file = info.file;
      let formData = new FormData();
      formData.append("file", file);
      formData.append("taskID", this.task.id);
      this.loading = true;
      workImportExcleTrans(formData)
        .then((res) => {
          // this.dataSource = res.data.list
          this.dataSource.forEach((item1) => {
            const matchItem = res.data.list.find(
              (item2) => item2.id === item1.id
            );
            if (matchItem) {
              item1[this.commonParam.value] = matchItem[this.commonParam.value];
            }
          });
          this.dataSource.forEach((item) => {
            item.entryState = 3;
          });
          this.updateNewByOld(this.allData, this.dataSource); // 也更新一下全量数据
        })
        .catch((err) => {
          message.error("导入失败！", err.message);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    // 下一个词条 快捷键
    nextEntry() {
      if (this.selectedRowIndex === null) {
        return;
      }
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
      // this.assistedTranslation(this.dataSource[index])
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
      // this.assistedTranslation(this.dataSource[index])
    },
    // 下一个未翻译词条
    nextNotTransEntry() {
      if (this.selectedRowIndex === null) {
        return;
      }
      let index = this.dataSource.findIndex(
        (item) => item.id === this.selectedRowIndex
      );
      if (index === this.dataSource.length - 1) {
        return;
      }
      let notTransIndex = index;
      index++;
      for (index; index < this.dataSource.length; index++) {
        if (
          this.dataSource[index][this.commonParam.value] === null ||
          this.dataSource[index][this.commonParam.value] === ""
        ) {
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
      // this.assistedTranslation(this.dataSource[notTransIndex])
    },
    // 上一个未翻译词条
    prevNotTransEntry() {
      if (this.selectedRowIndex === null) {
        return;
      }
      let index = this.dataSource.findIndex(
        (item) => item.id === this.selectedRowIndex
      );
      if (index === 0) {
        return;
      }
      let preNotTransIndex = index;
      index--;
      for (index; index >= 0; index--) {
        if (
          this.dataSource[index][this.commonParam.value] === null ||
          this.dataSource[index][this.commonParam.value] === ""
        ) {
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
      // this.assistedTranslation(this.dataSource[preNotTransIndex])
    },
    // 编辑选中行
    editSelectRow() {
      if (this.selectedRowIndex === null) {
        return;
      }
      const refName = `ref${this.selectedRowIndex.replaceAll("-", "")}`;
      const inputRef = this.$refs[refName];

      if (this.editableData.hasOwnProperty(this.selectedRowIndex)) {
        // 编辑数据中包含该数据
        if (inputRef) {
          inputRef.focus();
        }
      } else {
        // 编辑数据中不包含该数据
        this.editableData[this.selectedRowIndex] = this.dataSource.find(
          (item) => item.id === this.selectedRowIndex
        );
        this.$nextTick(() => {
          const input = this.$refs[refName];
          if (input) {
            input.focus();
          }
        });
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
            // container.scrollTop = targetElement.offsetTop - container.scrollHeight / 2 + 40 // 设置滚动条位置
            container.scrollTop =
              flag * targetElement.offsetHeight - this.tableHeight.y + 50; // 当前行 * 行高 - 表格展示高度
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
        this.tableHeight.y = height - 200;
      } else if (type === "reduce") {
        this.tableHeight.y = 415;
      }
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
      // 校验该页数据翻译长度
      let checkArr = this.dataSource.slice(
        (page - 1) * pageSize,
        page * pageSize
      );
      this.verifyTranslationLength(checkArr);
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
          ? this.editableData[record.id][this.commonParam.value]
          : record[this.commonParam.value];
        // if (common.byteLength(text) > maxLength) {
        if (byteLength(text) > maxLength) {
          flag++;
          this.edit(record).then(async () => {
            const formRefName = `form${record.id.replaceAll("-", "")}${
              this.commonParam.value
            }`;
            const formRef = this.$refs[formRefName];
            if (formRef) {
              try {
                await formRef.validate();
              } catch (err) {
                console.error("校验翻译长度时表单验证失败:", err);
                message.error("翻译校验未通过，请检查翻译内容", err.message);
              }
            }
          });
        }
      });
      return flag;
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
    // 获取用户偏好
    queryPartiality() {
      queryUserPartiality().then((res) => {
        if (res.data.list && res.data.list.length > 0) {
          let exportColumn = res.data.list[0].exportColumn;
          if (exportColumn != null && exportColumn != "") {
            this.exportModal.field = exportColumn.split(",");
          }
        }
      });
    },
    // 设置偏好
    updatePartiality(data) {
      updateUserPartiality(data).then((res) => {});
    },
    exportFieldChange(value) {
      let data = {
        exportColumn: value.join(","),
      };
      this.updatePartiality(data);
    },
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
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
    // 首字母转换
    capitalizeWordsClick(value) {
      if (this.selectedRows.length === 0) {
        message.info("请选择！");
        return;
      }
      if (value.key === "upper" || value.key === "lower") {
        let params = {
          changeType: value.key,
          translateType: this.task.translateType,
        };
        capitalizeWords(params, this.selectedRows).then((res) => {
          this.dataSource = this.dataSource.map((item1) => {
            const matchedItem2 = res.data.list.find(
              (item2) => item2.id === item1.id
            );
            return matchedItem2 || item1;
          });
          this.updateNewByOld(this.allData, this.dataSource); // 也更新一下全量数据

          this.selectedRowKeys = [];
          this.selectedRows = [];
          message.success("转换成功！");
        });
      } else if (value.key === "replace") {
        this.replace();
      }
    },
    replace() {
      this.replaceVisible = true;
      setModalAriaHidden(this, document);
    },
    replaceClose() {
      this.replaceVisible = false;
    },
    replaceOK() {
      this.$refs.replaceForm
        .validate()
        .then(() => {
          let params = {
            sourceStr: this.replaceModal.sourceStr,
            replaceStr: this.replaceModal.replaceStr,
            translateType: this.task.translateType,
          };
          replaceWords(params, this.selectedRows).then((res) => {
            this.dataSource = this.dataSource.map((item1) => {
              const matchedItem2 = res.data.list.find(
                (item2) => item2.id === item1.id
              );
              return matchedItem2 || item1;
            });
            this.updateNewByOld(this.allData, this.dataSource); // 也更新一下全量数据

            message.success("替换成功！");
            this.replaceVisible = false;
            this.selectedRowKeys = [];
            this.selectedRows = [];
          });
        })
        .catch((err) => {
          message.error("11", err.message);
        });
    },
    replaceAfterClose() {
      this.replaceModal = {
        sourceStr: null,
        replaceStr: null,
      };
    },
    // 展示列切换并保存用户偏好
    changeColumn(checkedValue) {
      changeColumn("colPref-translateModal", 100, checkedValue, this);
    },
  },
  mounted() {
    let _this = this;
    this.$nextTick(() => {
      // 读取本地存储的用户偏好
      getColPref("colPref-translateModal", 100, this);
      // this.init();
      window.onresize = function () {
        _this.setTableHeight();
      };
    });
  },
  beforeUnmount() {
    this.afterClose();
  },
};
</script>
<style lang="less">
@import url("@/assets/style/common.less");
</style>
<style scoped lang="less">
.search-row .ant-col {
  margin-bottom: 4px !important; /* 使用 !important 确保覆盖 Ant Design 默认样式 */
}
.inline-left-align {
  display: flex; /* 使用 Flex 布局 */
  flex-direction: row; /* 水平排列 */
  align-items: center; /* 垂直居中对齐 */
  white-space: nowrap; /* 防止内容换行 */
  & span {
    margin-right: 8px; /* 给 span 右侧添加间距 */
  }
  & a-input {
    flex: 1; /* 让输入框自动填充剩余空间 */
  }
}
.content {
  width: 100%;
  height: 100%;
  // min-height: 400px;
  padding: 10px;
  background-color: #f3f3f3;
  display: flex;
  // align-items: center;
  gap: 16px;
  align-self: stretch;

  .table {
    width: 70%;
    height: 100%;
  }
  .suggest {
    width: 30%;
    // padding-top: 30px;
    // flex:1;
    position: relative;

    .suggentContent {
      width: 100%;
      // height: calc(100% - 30px);
      background: #fff;
      display: flex;
      padding: 10px;
      flex-direction: column;
      align-items: flex-start;
      gap: 8px;
      flex: 1 0 0;
      align-self: stretch;
      overflow: auto;
      // max-height: 360px;

      .title {
        color: var(--text-icon-font-gy-340-placeholder, rgba(0, 0, 0, 0.4));

        /* 五级文字/常规 */
        font-family: Microsoft YaHei;
        font-size: 12px;
        font-style: normal;
        font-weight: 400;
        line-height: 20px; /* 166.667% */
      }

      .suggentItem {
        width: 100%;
        .tran {
          display: flex;
          align-items: center;

          span {
            color: var(--text-icon-font-gy-190-primary, rgba(0, 0, 0, 0.9));
            /* 四级文字/常规 */
            font-family: Microsoft YaHei;
            font-size: 14px;
            font-style: normal;
            font-weight: 400;
            line-height: 22px;
          }
        }
        .tips {
          color: var(--text-icon-font-gy-340-placeholder, rgba(0, 0, 0, 0.4));
          /* 五级文字/常规 */
          font-family: Microsoft YaHei;
          font-size: 10px;
          font-style: normal;
          font-weight: 400;
          line-height: 20px; /* 166.667% */
        }
      }
      .suggentItem:hover {
        background-color: #f1f5f6;
      }
    }

    :deep(.ant-spin) {
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%);
    }
  }
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
    margin-bottom: 6px;
  }
}
.ant-table-cell .ant-form-item {
  margin-bottom: 0%;
}
:deep(.ant-pagination) {
  margin: 8px 0;
}
</style>