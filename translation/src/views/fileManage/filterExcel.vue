<template>
  <div class="container" ref="box">
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <a-row class="search-row" style="width:100%;display:flex;gap:8px">
            <a-form-item v-if="checkedSearchCondition.includes('entry')" label="词条" name="entry">
              <a-textarea v-model:value="search.entry" placeholder="请输入内容" :auto-size="{ minRows: 1 }"></a-textarea>
            </a-form-item>
            <a-form-item v-if="checkedSearchCondition.includes('state')" label="词条状态" name="state">
              <EntryStateSelect :entryState="search.entryState" @update:entryState="search.entryState = $event"
                :showForbbiden="showForbbiden" @update:showForbbiden="showForbbiden = $event" />
            </a-form-item>
            <a-form-item v-if="checkedSearchCondition.includes('tag')" label="tag" name="tag">
              <a-input v-model:value="search.tag" placeholder="请输入内容"></a-input>
            </a-form-item>
            <a-form-item v-if="checkedSearchCondition.includes('entrySource')" label="词条来源" name="entrySource">
              <a-input v-model:value="search.entrySource" placeholder="请输入词条来源"></a-input>
              <!-- <a-select v-model:value="search.entrySource" show-search placeholder="请输入词条来源"
                :options="entrySourceOptions" allowClear @search="handleEntrySourceSearch">
              </a-select> -->
            </a-form-item>
            <a-form-item v-if="checkedSearchCondition.includes('language')" label="翻译语种" name="language">
              <a-select v-model:value="search.language" placeholder="请选择" :fieldNames="{ label: 'name', value: 'name' }"
                :options="translateTypes" allowClear>
              </a-select>
            </a-form-item>
            <a-form-item v-if="checkedSearchCondition.includes('translateState')" label="翻译状态" name="translateState">
              <TransStateSelect :translateState="search.translateState"
                @update:translateState="search.translateState = $event" />
            </a-form-item>
            <a-form-item v-if="checkedSearchCondition.includes('translate')" label="翻译结果" name="translate">
              <a-input v-model:value="search.translate" placeholder="请输入内容"></a-input>
            </a-form-item>
            <a-form-item v-if="checkedSearchCondition.includes('comment')" label="comment" name="comment">
              <a-input v-model:value="search.comment" placeholder="请输入内容"></a-input>
            </a-form-item>
            <a-form-item v-if="checkedSearchCondition.includes('diFileName')" label="辞典名称" name="diFileName">
              <a-input v-model:value="search.diFileName" placeholder="请输入辞典名称"></a-input>
              <!-- <a-select v-model:value="search.diFileName" show-search placeholder="请输入辞典名称" :options="diFileNameOptions"
                allowClear @search="handleDiFileNameSearch">
              </a-select> -->
            </a-form-item>
            <a-form-item v-if="checkedSearchCondition.includes('startTime')" label="开始时间" name="startTime">
              <a-date-picker v-model:value="search.startTime_" style="width: 186px" />
            </a-form-item>
            <a-form-item v-if="checkedSearchCondition.includes('endTime')" label="结束时间" name="endTime">
              <a-date-picker v-model:value="search.endTime_" style="width: 186px" />
            </a-form-item>
            <a-form-item v-if="checkedSearchCondition.includes('update')" label="修改人" name="update">
              <a-input v-model:value="search.update" placeholder="请输入内容"></a-input>
            </a-form-item>
          </a-row>
        </a-form>
      </template>
      <template v-slot:operate>
        <ResetButton :size="'middle'" :search="search" :currentPage="pagination.current" @resetData="onResetData" />
        <a-button type="primary" size="middle" @click="getSearchClick">查询</a-button>
        <a-popover trigger="click" placement="leftTop" :overlayStyle="overlayStyle">
          <template #content>
            <a-checkbox-group v-model:value="checkedSearchCondition" @change="changeSearchCondition">
              <a-row v-for="item in searchConditionList" :key="item.value">
                <a-col :span="24">
                  <a-checkbox :value="item.value">
                    {{ item.label }}
                  </a-checkbox>
                </a-col>
              </a-row>
            </a-checkbox-group>
          </template>
          <a-button type="primary" size="middle" ghost><template #icon>
              <SettingOutlined />
            </template>展示条件</a-button>
        </a-popover>
      </template>
    </SearchBox>
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <template v-slot:operate>
        <div ref="button" style="margin-bottom: 8px; display: flex; gap: 10px">
          <a-upload name="file" accept=".csv" :beforeUpload="beforeUpload" :show-upload-list="false">
            <a-button type="primary" size="middle">
              <template #icon>
                <UploadOutlined />
              </template>
              导入csv
            </a-button>
          </a-upload>
          <a-button type="primary" size="middle" @click="handleDeduplicateExport">
            <template #icon>
              <ExportOutlined />
            </template>
            去重
          </a-button>
          <a-button type="primary" size="middle" @click="showImportBackfillModal">
            <template #icon>
              <ImportOutlined />
            </template>
            导入回填
          </a-button>
          <a-button v-if="batchDeleteFlag" type="primary" danger size="middle" @click="handleBatchDelete">
            批量删除
          </a-button>
          <a-button v-if="deleteButtonsVisible" type="primary" danger size="middle" :loading="deleteLoading"
            @click="handleDeleteEntries">
            删除词条
          </a-button>
          <a-button v-if="deleteButtonsVisible" type="primary" size="middle" :loading="deleteLoading"
            @click="handleCancelDelete">
            取消删除
          </a-button>
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
            <a-button type="primary" size="middle">
              <template #icon>
                <SettingOutlined />
              </template>
              展示列
            </a-button>
          </a-popover>
        </div>
      </template>
      <template v-slot:data>
        <div style="width: 100%; position: absolute">
          <a-config-provider :locale="locale">
            <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource"
              :row-key="(record) => record.id" :scroll="tableHeight" :pagination="pagination" :loading="loading"
              :rowClassName="getRowClassName" ref="fileManageTable" @resizeColumn="handleResizeColumn" :row-selection="batchSelectFlag
                ? {
                  selectedRowKeys: selectedRowKeys,
                  onChange: onSelectChange,
                  onSelect: onSelect,
                  onSelectAll: onSelectAll,
                  selections: [
                    { key: 'selectAll', text: '全部选择', onSelect: selectAllEntry },
                    { key: 'clearAll', text: '取消选择', onSelect: clearAllEntry }
                  ],
                } : null" :expandable="{
                  expandedRowKeys: expandedRowKeys,
                  onExpandedRowsChange: (expandedRows) => {
                    this.expandedRowKeys = expandedRows;
                  },
                }">
              <template #expandIcon="props">
                <span v-if="props.record.children != null && props.record.children.length > 0">
                  <div v-if="props.expanded" style="display: inline-block; margin-right: 10px"
                    @click="(e) => { props.onExpand(props.record, e); }">
                    <CaretDownOutlined />
                  </div>
                  <div v-else style="display: inline-block; margin-right: 10px"
                    @click="(e) => { props.onExpand(props.record, e); }">
                    <CaretRightOutlined />
                  </div>
                </span>
                <span v-else style="margin-right:23px"></span>
              </template>
            </a-table>
          </a-config-provider>
        </div>
      </template>
    </DataBox>

    <SelectCols v-model:visible="filterModal.visible" :loading="loading" :columns="columns"
      @confirm="handleDeduplicateConfirm" />
    <a-modal v-model:open="importBackfillVisible" title="导入回填" @ok="handleImportBackfill"
      @cancel="handleImportBackfillCancel">
      <a-form :model="importBackfillForm" layout="vertical">
        <a-form-item label="带上翻译后的去重.csv" name="deduplicatedCsv" :rules="[
          { required: true, message: '请选择带上翻译后的去重.csv文件' },
        ]">
          <a-upload :before-upload="(file) => beforeUploadBackfill(file, 'deduplicatedCsv')
            " :show-upload-list="false" accept=".csv">
            <a-button>
              <template #icon>
                <UploadOutlined />
              </template>
              选择文件
            </a-button>
          </a-upload>
          <div v-if="importBackfillForm.deduplicatedCsv" style="margin-top: 8px">
            已选择: {{ importBackfillForm.deduplicatedCsv.name }}
          </div>
        </a-form-item>
        <a-form-item label="回填.json" name="backfillJson" :rules="[{ required: true, message: '请选择回填.json文件' }]">
          <a-upload :before-upload="(file) => beforeUploadBackfill(file, 'backfillJson')
            " :show-upload-list="false" accept=".json">
            <a-button>
              <template #icon>
                <UploadOutlined />
              </template>
              选择文件
            </a-button>
          </a-upload>
          <div v-if="importBackfillForm.backfillJson" style="margin-top: 8px">
            已选择: {{ importBackfillForm.backfillJson.name }}
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script>
import { message, notification } from "ant-design-vue";
import zhCN from "ant-design-vue/es/locale/zh_CN";
import CustomModal from "@/components/modal/index.vue";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import EntryStateSelect from "@/components/select/entryStateSelect.vue";
import TransStateSelect from "@/components/select/transStateSelect.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import ResetButton from "@/components/Button/resetButton.vue";
import SelectCols from "./selectCols.vue";
import {
  UploadOutlined,
  ExportOutlined,
  ImportOutlined,
  SettingOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
} from "@ant-design/icons-vue";
import { getLanguage } from "@/http/api/translate";
import commonParam, { entryParams } from "@/utils/commonParam.js";
import { entryReadExcel, exportDeduplicatedData } from "@/http/api/entryManage";
import {
  onSelectChange,
  onSelect,
  onSelectAll,
  pageChange,
  clickInput,
  setTableHeight,
  handleResizeColumn,
  getRowClassName,
  getColPref,
  changeColumn,
  getSearch,
  setModalAriaHidden,
} from "@/utils/commonUtils";
import { defineComponent, ref } from "vue";

export default {
  components: {
    SearchBox,
    DataBox,
    ResetButton,
    UploadOutlined,
    ExportOutlined,
    ImportOutlined,
    SettingOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    EntryStateSelect,
    TransStateSelect,
    EntryStateBadge,
    TransStateBadge,
    SelectCols,
  },
  props: {
    boxHeight: 0,
  },
  data() {
    // 从本地缓存读取展示列偏好
    const cachedDisplayColumn = localStorage.getItem("colPref-fileManage");
    const cachedSearchCondition = localStorage.getItem(
      "searchCondition-fileManage"
    );
    return {
      locale: zhCN,
      user: {},
      currentDepartment: {
        label: "部门名称",
        value: "name",
        ops: new Set(),
      }, // 当前用户所在部门的相关信息
      labelCol: { style: { width: "84px" } },
      search: {
        entry: "",
        abbr: "",
        partOfSpeech: "",
        translateType: null,
        classfy1: [],
        classfy2: [],
        entryState_: [0, 1, 2, 3], // 如果查询条件为空即为全选，则使用这个词条状态来进行查询
        entryState: null, // 查询条件中的词条状态
        tag: "",
        entrySource: null,
        language: null,
        translateState: null,
        translate: "",
        filter_translate: "",
        comment: "",
        startTime_: null, // 时间戳格式
        endTime_: null, // 时间戳格式
        startTime: null,
        endTime: null,
        diFileName: null,
        update: null,
        searchType: null,
        i18nURL: null,
        hasRedundantRls: false, // 是否有冗余词条校验结果,有的话显示按钮“重新查询”
      },
      lastSearch: {},
      tableTitle: "词条列表",
      dataHeight: 200,
      tableHeight: { x: "max-content", y: 0 },
      loading: false,
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 50,
          customRender: (text, record, index, column) => {
            return (
              text.index +
              1 +
              this.pagination.pageSize * (this.pagination.current - 1)
            );
          },
          fixed: "left",
          index: 1,
        },
        {
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 160,
          resizable: true,
          fixed: "left",
          index: 2,
          // // 添加筛选功能(但是查询做了分页，只能获得当前页的数据)
          // customFilterDropdown: true, // 使用自定义筛选下拉框
          // filteredValue: null, // 初始状态下没有筛选条件
          // onFilter: (filterValue, record) => {
          //   // 精确匹配，不忽略大小写
          //   return record.entry.toString() === filterValue;
          // },
        },
        {
          title: "comment",
          dataIndex: "comment",
          align: "center",
          width: 130,
          resizable: true,
          index: 4,
        },
        {
          title: "英文翻译",
          dataIndex: "english",
          align: "center",
          width: 180,
          resizable: true,
          index: 12,
        },
        {
          title: "俄文翻译",
          dataIndex: "russian",
          align: "center",
          width: 180,
          resizable: true,
          index: 15,
        },
        {
          title: "西文翻译",
          dataIndex: "spanish",
          align: "center",
          width: 180,
          resizable: true,
          index: 18,
        },
        {
          title: "法文翻译",
          dataIndex: "french",
          align: "center",
          width: 180,
          resizable: true,
          index: 21,
        },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          width: 40,
          fixed: "right",
          index: 100,
        },
      ],
      overlayStyle: entryParams.overlayStyle, // 展示列样式
      checkboxList: entryParams.checkboxList, // 展示列可选的值
      checkedColumn: cachedDisplayColumn
        ? JSON.parse(cachedDisplayColumn).displayColumn.split(",")
        : [], // 展示列已选的值
      dataSource: [],
      selectedRowKeys: [],
      selectedRows: [],
      selectEntry: new Map(), // 已选任务（用map来遍历更快）
      pagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
      batchSelectFlag: false,
      batchDeleteFlag: true,
      deleteButtonsVisible: false,
      deleteLoading: false,
      importBackfillVisible: false,
      importBackfillForm: {
        deduplicatedCsv: null,
        backfillJson: null,
      },
      filterModal: {
        visible: false,
        duplicateCols: [],
      },
      expandedRowKeys: [],// 存储当前所有展开行的key值。当某一行展开时，它的key会被添加到这个数组中；折叠时则会被移除。
      searchConditionList: entryParams.searchConditionList,// 展示的查询条件框
      checkedSearchCondition: cachedSearchCondition
        ? JSON.parse(cachedSearchCondition).displayColumn.split(",")
        : entryParams.checkedSearchCondition, // (可选)显示的查询条件框
      translateTypes: [],// 翻译语言下拉框
      // entrySourceOptions: [], // 词条来源下拉框
      // diFileNameOptions: [], // 辞典名称下拉框
      // entrySourceOptions_copy: [], // 词条来源下拉框
      // diFileNameOptions_copy: [], // 辞典名称下拉框
      showForbbiden: false, // 显示/隐藏禁用
    };
  },
  watch: {
    boxHeight(newval, oldval) {
      this.box = newval;
      this.setTableHeight();
    },
    "search.startTime_": function (newValue) {
      if (newValue) {
        this.search.startTime =
          newValue.$y +
          "-" +
          (newValue.$M + 1).toString().padStart(2, "0") +
          "-" +
          newValue.$D.toString().padStart(2, "0"); // 格式化日期为 YYYY-MM-DD 格式
        if (this.search.endTime_) {
          if (this.search.startTime_ > this.search.endTime_) {
            message.error("开始时间不能大于结束时间！");
            this.search.startTime = null;
            this.search.startTime_ = null;
          }
        }
      } else {
        this.search.startTime = null;
      }
    },
    "search.endTime_": function (newValue) {
      if (newValue) {
        this.search.endTime =
          newValue.$y +
          "-" +
          (newValue.$M + 1).toString().padStart(2, "0") +
          "-" +
          (newValue.$D + 1).toString().padStart(2, "0"); // 结束日期+1，以达到当天的24:00的效果）
        if (this.search.startTime_) {
          if (this.search.startTime_ > this.search.endTime_) {
            message.error("结束时间不能小于开始时间！");
            this.search.endTime = null;
            this.search.endTime_ = null;
          }
        }
      } else {
        this.search.endTime = null;
      }
    },
  },
  mounted() {
    let _this = this;
    this.$nextTick(() => {
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
      this.search.entryState = this.currentDepartment.ops.has("entryState3")
        ? "3"
        : null;
      this.admin = this.$store.state.admin;
      //保证初次传的值给到
      this.box = this.boxHeight;
      this.setTableHeight();
      this.getLanguage();

      this.init();
      getColPref("colPref-fileManage", 150, this);
      window.onresize = function () {
        _this.setTableHeight();
      };
    });
  },
  unmounted() {
    window.onresize = null;
  },
  methods: {
    init() {
      this.setTableHeight();
      this.getSearchClick();
    },
    changeColumn(checkedValue) {
      changeColumn(
        "colPref-fileManage",
        150,
        checkedValue,
        this,
        false,
        this.checkboxList
      );
    },
    getSearchClick() {
      this.batchSelectFlag = false;
      this.pagination.current = 1;
      this.getSearch();
    },
    getSearch() {
      this.dataSource = [];
      this.selectedRows = [];
      this.selectedRowKeys = [];
      const currentSearch = { ...this.search };
      this.lastSearch = currentSearch;
      console.log("查询条件:", currentSearch);
      this.loading = true;
      setTimeout(() => {
        this.dataSource = [];
        this.pagination.total = 0;
        this.loading = false;
      }, 500);
    },
    onResetData() {
      this.search = {
        entry: "",
        abbr: "",
        partOfSpeech: "",
        translateType: null,
        classfy1: [],
        classfy2: [],
        entryState_: [0, 1, 2, 3], // 如果查询条件为空即为全选，则使用这个词条状态来进行查询
        entryState: this.currentDepartment.ops.has("entryState3") ? "3" : null, // 查询条件中的词条状态
        tag: "",
        entrySource: null,
        language: null,
        translateState: null,
        translate: "",
        filter_translate: "",
        comment: "",
        startTime_: null, // 时间戳格式
        endTime_: null, // 时间戳格式
        startTime: null,
        endTime: null,
        diFileName: null,
        update: null,
        i18nURL: null,
        hasRedundantRls: false,
      };
      this.getSearchClick();
    },
    beforeUpload(file) {
      const isCsv = file.name.endsWith(".csv");
      if (!isCsv) {
        message.error("只能上传 CSV 文件!");
        return false;
      }
      const reader = new FileReader();
      reader.onload = async (e) => {
        const content = e.target.result;
        try {
          const encoding = this.detectEncoding(content);
          // if (encoding !== "UTF-8") {
          //   message.warning(
          //     "文件编码不是 UTF-8，请另存为 UTF-8 编码的 CSV 文件"
          //   );
          // }
          console.log("导入 CSV 文件:", file.name, "编码:", encoding, "内容", content);

          const formData = new FormData();
          formData.append("file", file);

          const res = await entryReadExcel({}, formData);
          if (res.type === "SUCCESS") {
            const success = this.adjustColumnsBasedOnCSV(res.data.list);
            if (success) {
              this.dataSource = res.data.list;
              this.pagination.total = res.data.list.length;
              message.success("文件读取成功");
            }
          } else {
            message.error("文件读取失败: " + res.message);
          }
        } catch (error) {
          console.error("文件解析失败", error);
          message.error("文件解析失败");
        }
      };
      reader.readAsText(file);
      return false;// 在文件开始上传之前阻止文件上传操作
    },
    detectEncoding(content) {
      const bom = content.charCodeAt(0);
      if (bom === 0xfeff) {
        return "UTF-8";
      }
      try {
        decodeURIComponent(escape(content));
        return "UTF-8";
      } catch (e) {
        return "GBK";
      }
    },
    adjustColumnsBasedOnCSV(dataList) {
      if (!dataList || dataList.length === 0) {
        this.showImportError("CSV 文件为空或没有有效数据，请检查文件后重新上传");
        return false;
      }

      const csvColumns = Object.keys(dataList[0]);

      const unmatchedColumns = this.getUnmatchedColumns(csvColumns);
      if (unmatchedColumns.length > 0) {
        const errorMsg = `CSV 文件包含不支持的列：${unmatchedColumns.join(', ')}，请修改 CSV 文件后重新上传`;
        this.showImportError(errorMsg);
        return false;
      }

      const sortedIntersection = this.calculateSortedIntersection(csvColumns);

      changeColumn(
        "colPref-fileManage",
        200,
        sortedIntersection,
        this,
        false,
        commonParam.checkboxList
      );

      this.checkedColumn = sortedIntersection;

      return true;
    },
    getUnmatchedColumns(csvColumns) {
      const excludedColumns = ['id', 'index', 'entry', 'operation'];
      const filteredColumns = csvColumns.filter(col => !excludedColumns.includes(col));
      
      const supportedColumns = entryParams.checkboxList.map(item => item.value);
      return filteredColumns.filter(col => !supportedColumns.includes(col));
    },
    calculateSortedIntersection(csvColumns) {
      const excludedColumns = ['id', 'index', 'entry', 'operation'];
      const filteredColumns = csvColumns.filter(col => !excludedColumns.includes(col));
      
      const supportedColumns = entryParams.checkboxList.map(item => item.value);
      const intersection = filteredColumns.filter(col => supportedColumns.includes(col));

      const sortedIntersection = entryParams.checkboxList
        .filter(item => intersection.includes(item.value))
        .sort((a, b) => a.index - b.index)
        .map(item => item.value);

      const requiredColumns = ['index', 'entry', 'operation'];
      const finalColumns = [...new Set([...sortedIntersection, ...requiredColumns])];

      return finalColumns;
    },
    showImportError(description) {
      notification.error({
        message: '导入失败',
        description: description,
        duration: 0,
        key: 'csv-import-error',
      });
    },
    handleDeduplicateExport() {
      if (this.dataSource.length === 0) {
        message.error("没有数据，无法去重");
        return;
      }
      this.filterModal.visible = true;
      this.filterModal.duplicateCols = [];
      setModalAriaHidden(this, document);
    },
    handleDeduplicateConfirm(selectedColumns) {
      this.filterModal.duplicateCols = selectedColumns;
      this.executeDeduplicateExport();
    },
    async executeDeduplicateExport() {
      if (!this.filterModal.duplicateCols.length) {
        message.error("请至少选择一列用于去重");
        return;
      }

      this.loading = true;
      try {
        const params = {
          data: this.dataSource,
          params: {
            deduplicateColumns: this.filterModal.duplicateCols
          }
        };

        const res = await exportDeduplicatedData(params);
        if (res.type === "SUCCESS") {
          this.dataSource = res.data.dataSource;
          this.pagination.total = res.data.dataSource.length;
          this.exportIdMap(res.data.idMap);
          message.success("去重成功");
          this.filterModal.visible = false;
        } else {
          message.error(res.message || "去重失败");
        }
      } catch (error) {
        console.error("去重失败", error);
        message.error("去重失败: " + error.message);
      } finally {
        this.loading = false;
      }
    },
    exportIdMap(idMap) {
      const jsonString = JSON.stringify(idMap, null, 2);
      const blob = new Blob([jsonString], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = '去重映射.json';
      link.click();
      URL.revokeObjectURL(url);
    },
    showImportBackfillModal() {
      this.importBackfillVisible = true;
      this.importBackfillForm = {
        deduplicatedCsv: null,
        backfillJson: null,
      };
    },
    beforeUploadBackfill(file, type) {
      if (type === "deduplicatedCsv") {
        const isCsv = file.name.endsWith(".csv");
        if (!isCsv) {
          message.error("只能上传 CSV 文件!");
          return false;
        }
        this.importBackfillForm.deduplicatedCsv = file;
      } else if (type === "backfillJson") {
        const isJson = file.name.endsWith(".json");
        if (!isJson) {
          message.error("只能上传 JSON 文件!");
          return false;
        }
        this.importBackfillForm.backfillJson = file;
      }
      return false;
    },
    handleImportBackfill() {
      if (
        !this.importBackfillForm.deduplicatedCsv ||
        !this.importBackfillForm.backfillJson
      ) {
        message.error("请选择两个文件");
        return;
      }
      console.log("导入回填 - 后端实现");
      console.log(
        "带上翻译后的去重.csv:",
        this.importBackfillForm.deduplicatedCsv.name
      );
      console.log("回填.json:", this.importBackfillForm.backfillJson.name);
      message.success("回填成功");
      this.importBackfillVisible = false;
    },
    handleImportBackfillCancel() {
      this.importBackfillVisible = false;
    },
    // 获取翻译语种
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
      });
    },
    // // 处理词条来源的搜索输入
    // handleEntrySourceSearch(value) {
    //   const option = {
    //     label: value,
    //     value: value,
    //   };
    //   this.entrySourceOptions = this.entrySourceOptions_copy.concat([option]);
    // },
    // // 处理辞典名称的搜索输入
    // handleDiFileNameSearch(value) {
    //   const option = {
    //     label: value,
    //     value: value,
    //   };
    //   this.diFileNameOptions = this.diFileNameOptions_copy.concat([option]);
    // },
    // 展示条件切换并保存用户偏好
    changeSearchCondition(checkedValue) {
      changeColumn(
        "searchCondition-fileManage",
        200,
        checkedValue,
        this,
        false,
        entryParams.searchConditionList
      );
    },
    // 展示列切换并保存用户偏好
    changeColumn(checkedValue) {
      changeColumn(
        "colPref-fileManage",
        200,
        checkedValue,
        this,
        false,
        commonParam.checkboxList
      );
    },
    // 表格复选框选择事件的回调（全选/反选不会回调这个函数）
    onSelectChange(selectedRowKeys, selectedRows) {
      // this.selectedRowKeys = selectedRowKeys;
      // this.selectedRows = selectedRows;
      // onSelect(单选/取选)、onSelectAll(全选/反选)后，更新selectedRows、selectedRowKeys
      this.selectedRows = [...this.selectEntry.values()];
      this.selectedRowKeys = [...this.selectEntry.keys()]; // selectEntryList.map((item) => item.id);
      // console.log("表格复选框选择事件", this.selectedRows);
    },
    // 表格复选框点击事件
    onSelect(record, selected) {
      if (selected) {
        this.selectEntry.set(record.id, record);
      } else {
        this.selectEntry.delete(record.id);
      }

      // console.log("表格复选框点击事件", record, selected);
    },
    // 表格全选/反选框点击事件（当前页）
    onSelectAll(selected, selectedRows, changeRows) {
      if (selected) {
        changeRows.forEach((item) => {
          this.selectEntry.set(item.id, item);
        });
      } else {
        changeRows.forEach((item) => {
          this.selectEntry.delete(item.id);
        });
      }
      // console.log(
      //   "表格全选/反选框点击事件",
      //   selected,
      //   selectedRows, // 全选->取选当前页数据时，这个是5个undefined
      //   changeRows
      // );
    },
    // 复选框全选事件
    selectAllEntry() {
      this.toDoTasks.forEach((item) => {
        this.selectedRowKeys.push(item.id);
        this.selectedRows.push(item);
        this.selectEntry.set(item.id, item);
      });
    },
    //复选框反选事件
    clearAllEntry() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
      // this.selectEntry.clear();
    },
    // 批量删除按钮点击事件
    handleBatchDelete() {
      this.batchDeleteFlag = false;
      this.deleteButtonsVisible = true;
      this.batchSelectFlag = true;
    },
    // 删除词条按钮点击事件
    handleDeleteEntries() {
      this.deleteLoading = true;
      if (this.selectedRowKeys.length === 0) {
        message.warning("请先选择要删除的词条");
        this.deleteLoading = false;
        return;
      }
      this.dataSource = this.dataSource.filter(item => !this.selectedRowKeys.includes(item.id));
      this.clearAllEntry();
      this.deleteLoading = false;
      this.batchSelectFlag = false;
      this.deleteButtonsVisible = false;
      this.batchDeleteFlag = true;
      message.success("删除成功");
    },
    // 取消删除按钮点击事件
    handleCancelDelete() {
      this.deleteLoading = true;
      this.clearAllEntry();
      this.deleteLoading = false;
      this.batchSelectFlag = false;
      this.deleteButtonsVisible = false;
      this.batchDeleteFlag = true;
    },
    // 动态设置表格高度
    setTableHeight() {
      this.$nextTick(() => {
        setTableHeight(this, 32, 126, 90, { ok: true, h: this.box });
      });
    },
    // 设置表格每一行的class
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
    // 表格列可伸缩
    handleResizeColumn: (w, col) => {
      col.width = w;
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
      this.currentPageBranch = 0;
    },
  },
};
</script>
<style lang="less" scoped>
.container {
  width: 100%;
  height: 100%;
}
</style>
