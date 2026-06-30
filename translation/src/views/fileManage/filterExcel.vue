<template>
  <div
    class="container"
    ref="box"
  >
    <SearchBox
      v-if="false"
      ref="search"
      @change="setTableHeight"
    >
      <template v-slot:form>
        <a-form
          :model="search"
          name="horizontal_login"
          layout="inline"
          autocomplete="off"
          :label-col="labelCol"
        >
          <a-row
            class="search-row"
            style="width: 100%; display: flex; gap: 8px"
          >
            <a-form-item
              v-if="checkedSearchCondition.includes('entry')"
              label="词条"
              name="entry"
            >
              <a-textarea
                v-model:value="search.entry"
                placeholder="请输入内容"
                :auto-size="{ minRows: 1 }"
              ></a-textarea>
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('state')"
              label="词条状态"
              name="state"
            >
              <EntryStateSelect
                :entryState="search.entryState"
                @update:entryState="search.entryState = $event"
                :showForbbiden="showForbbiden"
                @update:showForbbiden="showForbbiden = $event"
              />
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('tag')"
              label="tag"
              name="tag"
            >
              <a-input
                v-model:value="search.tag"
                placeholder="请输入内容"
              ></a-input>
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('entrySource')"
              label="词条来源"
              name="entrySource"
            >
              <a-input
                v-model:value="search.entrySource"
                placeholder="请输入词条来源"
              ></a-input>
              <!-- <a-select v-model:value="search.entrySource" show-search placeholder="请输入词条来源"
                :options="entrySourceOptions" allowClear @search="handleEntrySourceSearch">
              </a-select> -->
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('language')"
              label="翻译语种"
              name="language"
            >
              <a-select
                v-model:value="search.language"
                placeholder="请选择"
                :fieldNames="{ label: 'name', value: 'name' }"
                :options="translateTypes"
                allowClear
              >
              </a-select>
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('translateState')"
              label="翻译状态"
              name="translateState"
            >
              <TransStateSelect
                :translateState="search.translateState"
                @update:translateState="search.translateState = $event"
              />
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('translate')"
              label="翻译结果"
              name="translate"
            >
              <a-input
                v-model:value="search.translate"
                placeholder="请输入内容"
              ></a-input>
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('comment')"
              label="comment"
              name="comment"
            >
              <a-input
                v-model:value="search.comment"
                placeholder="请输入内容"
              ></a-input>
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('diFileName')"
              label="辞典名称"
              name="diFileName"
            >
              <a-input
                v-model:value="search.diFileName"
                placeholder="请输入辞典名称"
              ></a-input>
              <!-- <a-select v-model:value="search.diFileName" show-search placeholder="请输入辞典名称" :options="diFileNameOptions"
                allowClear @search="handleDiFileNameSearch">
              </a-select> -->
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('startTime')"
              label="开始时间"
              name="startTime"
            >
              <a-date-picker
                v-model:value="search.startTime_"
                style="width: 186px"
              />
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('endTime')"
              label="结束时间"
              name="endTime"
            >
              <a-date-picker
                v-model:value="search.endTime_"
                style="width: 186px"
              />
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('update')"
              label="修改人"
              name="update"
            >
              <a-input
                v-model:value="search.update"
                placeholder="请输入内容"
              ></a-input>
            </a-form-item>
          </a-row>
        </a-form>
      </template>
      <template v-slot:operate>
        <ResetButton
          :size="'middle'"
          :search="search"
          :currentPage="pagination.current"
          @resetData="onResetData"
        />
        <a-button
          type="primary"
          size="middle"
          @click="getSearchClick"
          >查询</a-button
        >
        <a-popover
          trigger="click"
          placement="leftTop"
          :overlayStyle="overlayStyle"
        >
          <template #content>
            <a-checkbox-group
              v-model:value="checkedSearchCondition"
              @change="changeSearchCondition"
            >
              <a-row
                v-for="item in searchConditionList"
                :key="item.value"
              >
                <a-col :span="24">
                  <a-checkbox :value="item.value">
                    {{ item.label }}
                  </a-checkbox>
                </a-col>
              </a-row>
            </a-checkbox-group>
          </template>
          <a-button
            type="primary"
            size="middle"
            ghost
            ><template #icon> <SettingOutlined /> </template>展示条件</a-button
          >
        </a-popover>
      </template>
    </SearchBox>
    <DataBox
      :title="tableTitle"
      :height="dataHeight"
      :showOperate="true"
    >
      <template v-slot:operate>
        <div
          ref="button"
          style="margin-bottom: 8px; display: flex; gap: 10px"
        >
          <a-upload
            name="file"
            accept=".csv"
            :beforeUpload="beforeUpload"
            :show-upload-list="false"
          >
            <a-button
              type="primary"
              size="middle"
              :loading="importLoading"
            >
              <template #icon>
                <UploadOutlined />
              </template>
              导入csv
            </a-button>
          </a-upload>
          <a-button
            type="primary"
            size="middle"
            @click="handleDeduplicateExport"
          >
            <template #icon>
              <ExportOutlined />
            </template>
            去重
          </a-button>
          <!-- 去重回填(异步) -->
          <a-button
            v-if="hasFileUpdatePermission()"
            type="primary"
            size="middle"
            @click="showImportBackfillModal_v3"
          >
            <template #icon>
              <ImportOutlined />
            </template>
            去重回填
          </a-button>
          <!-- 去重回填(同步) -->
          <!-- <a-button
            v-if="hasFileUpdatePermission()"
            type="primary"
            size="middle"
            @click="showImportBackfillModal_v2_5"
          >
            <template #icon>
              <ImportOutlined />
            </template>
            去重回填
          </a-button> -->
          <!-- <a-button type="primary" size="middle" @click="showImportBackfillModal">
            <template #icon>
              <ImportOutlined />
            </template>
            去重回填 v1
          </a-button>
          <a-button v-if="hasDevPermission()" type="primary" size="middle" @click="showImportBackfillModal_v1_5">
            <template #icon>
              <ImportOutlined />
            </template>
            去重回填 v1.5
          </a-button> -->
          <a-button
            v-if="batchDeleteFlag"
            type="primary"
            danger
            size="middle"
            @click="handleBatchDelete"
          >
            批量删除
          </a-button>
          <a-button
            v-if="deleteButtonsVisible"
            type="primary"
            danger
            size="middle"
            :loading="deleteLoading"
            @click="handleDeleteEntries"
          >
            删除词条
          </a-button>
          <a-button
            v-if="deleteButtonsVisible"
            type="primary"
            size="middle"
            :loading="deleteLoading"
            @click="handleCancelDelete"
          >
            取消删除
          </a-button>
          <ColumnFilter
            :model-value="checkedColumn"
            :columns="columnSettingsList"
            :overlay-style="overlayStyle"
            button-size="middle"
            col-pref-name="colPref-fileManage"
            :normal-width="150"
            :need-filter="false"
          />
        </div>
      </template>
      <template v-slot:data>
        <div style="width: 100%; position: absolute">
          <a-config-provider :locale="locale">
            <a-table
              bordered
              class="ant-table-striped"
              ref="fileManageTable"
              :columns="columns"
              :data-source="dataSource"
              :row-key="(record) => record.id"
              :scroll="tableHeight"
              :pagination="pagination"
              :loading="loading"
              :rowClassName="getRowClassName"
              @resizeColumn="handleResizeColumn"
              :row-selection="
                batchSelectFlag
                  ? {
                      selectedRowKeys: selectedRowKeys,
                      onChange: onSelectChange,
                      onSelect: onSelect,
                      onSelectAll: onSelectAll,
                      selections: [
                        {
                          key: 'selectAll',
                          text: '全部选择',
                          onSelect: selectAllEntry,
                        },
                        {
                          key: 'clearAll',
                          text: '取消选择',
                          onSelect: clearAllEntry,
                        },
                      ],
                    }
                  : null
              "
              :expandable="{
                expandedRowKeys: expandedRowKeys,
                onExpandedRowsChange: (expandedRows) => {
                  this.expandedRowKeys = expandedRows;
                },
              }"
            >
              <template #expandIcon="props">
                <span
                  v-if="
                    props.record.children != null &&
                    props.record.children.length > 0
                  "
                >
                  <div
                    v-if="props.expanded"
                    style="display: inline-block; margin-right: 10px"
                    @click="
                      (e) => {
                        props.onExpand(props.record, e);
                      }
                    "
                  >
                    <CaretDownOutlined />
                  </div>
                  <div
                    v-else
                    style="display: inline-block; margin-right: 10px"
                    @click="
                      (e) => {
                        props.onExpand(props.record, e);
                      }
                    "
                  >
                    <CaretRightOutlined />
                  </div>
                </span>
                <span
                  v-else
                  style="margin-right: 23px"
                ></span>
              </template>
            </a-table>
          </a-config-provider>
        </div>
      </template>
    </DataBox>

    <SelectCols
      v-model:visible="filterModal.visible"
      :loading="loading"
      :columns="columns"
      @confirm="handleDeduplicateConfirm"
    />
    <BackFillModal
      modalTitle="去重回填"
      :visible="importBackfillVisible"
      :translateTypes="translateTypes"
      :needRelationFile="true"
      :defaultAccept="'.csv'"
      @handleClose="handleImportBackfillClose"
      @handleOK="handleImportBackfillOK"
    />
    <!-- <BackFillModal_v2 modalTitle="去重回填 2.0" :visible="importBackfillVisible_v2" :translateTypes="translateTypes"
      :needRelationFile="true" :defaultAccept="'.csv'" :functionMode="'updateTranslation'"
      @handleClose="handleImportBackfillClose_v2" @handleOK="handleImportBackfillOK_v2" /> -->
    <BackFillModal_v3
      modalTitle="去重回填(异步)"
      :visible="importBackfillVisible_v3"
      :needRelationFile="true"
      :defaultAccept="'.csv'"
      @handleClose="handleImportBackfillClose_v3"
      @handleOK="handleImportBackfillOK_v3"
    />
    <BackFillModal_v2_5
      modalTitle="去重回填"
      :visible="importBackfillVisible_v2_5"
      :needRelationFile="true"
      :defaultAccept="'.csv'"
      @handleClose="handleImportBackfillClose_v2_5"
      @handleOK="handleImportBackfillOK_v2_5"
    />
    <BackFillModal_v1_5
      modalTitle="去重回填 v1.5"
      :visible="importBackfillVisible_v1_5"
      :needRelationFile="true"
      :defaultAccept="'.csv'"
      @handleClose="handleImportBackfillClose_v1_5"
      @handleOK="handleImportBackfillOK_v1_5"
    />
    <ExportButton
      ref="exportButtonRef"
      :dataSource="deduplicatedDataSource"
      :fieldOptions_="exportFieldOptions"
      size="middle"
      buttonTitle="导出去重数据"
      :defaultStatusCheck="false"
      fileNamePrefix="去重文件（去重后，送翻前）_"
      :hideButton="true"
      @afterClose="handleExportAfterClose"
    />
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
import BackFillModal from "@/components/Button/fileManage/backFill/modal.vue";
import BackFillModal_v2 from "@/components/Button/fileManage/backFill/modal_v2.vue";
import BackFillModal_v2_5 from "@/components/Button/fileManage/backFill/modal_v2.5.vue";
import BackFillModal_v1_5 from "@/components/Button/fileManage/backFill/modal_v1.5.vue";
import BackFillModal_v3 from "@/components/Button/fileManage/backFill/modal_v3.vue";
import ExportButton from "@/components/Button/exportButton.vue";
import {
  ExportOutlined,
  ImportOutlined,
  SettingOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
  UploadOutlined,
} from "@ant-design/icons-vue";
import { getLanguage } from "@/http/api/translate";
import commonParam, { entryParams, entryAllCols, entryPresets } from "@/constants/commonParam.js";
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
} from "@/utils/tableUtils";
import { applyTable } from "@/components/ColumnFilter";
import { getSearch } from "@/utils/requestUtils";
import { setModalAriaHidden } from "@/utils/domUtils";
import ColumnFilter from "@/components/ColumnFilter/ColumnFilter.vue";
import { defineComponent, ref } from "vue";

export default {
  components: {
    SearchBox,
    DataBox,
    ResetButton,
    ExportOutlined,
    ImportOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    UploadOutlined,
    EntryStateSelect,
    TransStateSelect,
    EntryStateBadge,
    TransStateBadge,
    SelectCols,
    BackFillModal,
    BackFillModal_v2_5,
    BackFillModal_v3,
    BackFillModal_v2,
    BackFillModal_v1_5,
    ExportButton,
    ColumnFilter,
  },
  props: {
    boxHeight: 0,
  },
  data() {
    const cachedSearchCondition = localStorage.getItem(
      "searchCondition-fileManage"
    );
    return {
      admin: false,
      locale: zhCN,
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
      columns: [],
      overlayStyle: entryParams.overlayStyle,
      columnSettingsList: [],
      checkedColumn: [],
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
      importLoading: false,
      importBackfillVisible: false,
      importBackfillVisible_v2_5: false,
      importBackfillVisible_v2: false,
      importBackfillVisible_v1_5: false,
      importBackfillVisible_v3: false,
      deduplicatedDataSource: [], // 存储去重后的数据，用于导出
      filterModal: {
        visible: false,
        duplicateCols: [],
      },
      // 导出去重数据时可选的字段
      exportFieldOptions: entryParams.exportFields,
      expandedRowKeys: [], // 存储当前所有展开行的key值。当某一行展开时，它的key会被添加到这个数组中；折叠时则会被移除。
      searchConditionList: entryParams.searchConditionList, // 展示的查询条件框
      checkedSearchCondition: cachedSearchCondition
        ? JSON.parse(cachedSearchCondition).displayColumn.split(",")
        : entryParams.checkedSearchCondition, // (可选)显示的查询条件框
      translateTypes: [], // 获取翻译语种
      // entrySourceOptions: [], // 词条来源下拉框
      // diFileNameOptions: [], // 辞典名称下拉框
      // entrySourceOptions_copy: [], // 词条来源下拉框
      // diFileNameOptions_copy: [], // 辞典名称下拉框
      showForbbiden: false, // 显示/隐藏禁用
      excludedCols: ["id", "index", "entry", "operation"], // 无须比较的列, 'index', 'entry', 'operation'
      requiredCols: ["index", "entry", "operation"], // 必须添加的列
    };
  },
  watch: {
    boxHeight(newval) {
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
      // 获取当前用户所在部门的相关信息
      if (this.$currentDepartment) {
        this.search.entryState = this.$currentDepartment.ops?.has("entryState3")
          ? "3"
          : null;
      }
      this.admin = this.$store.state.admin;
      //保证初次传的值给到
      this.box = this.boxHeight;
      this.getLanguage();

      this.init();
      window.onresize = function () {
        _this.setTableHeight();
      };
    });
  },
  unmounted() {
    window.onresize = null;
  },
  methods: {
    hasFileUpdatePermission() {
      return (
        (this.admin && this.$currentDepartment?.ops?.has("fileUpdate")) || false
      );
    },
    hasDevPermission() {
      return this.$currentDepartment?.ops?.has("dev") || false;
    },
    init() {
      this.reset();
      this.dataSource = [];
      this.setTableHeight();
      // this.getSearchClick();
      applyTable(this, {
        allCols: entryAllCols,
        preset: entryPresets.filterExcel,
        ctx: { pagination: this.pagination },
        colPrefName: "colPref-fileManage",
        normalWidth: 150,
        needFilter: false,
      });
    },
    // 获取翻译语种
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
      });
    },
    resetSelected() {
      this.clearAllEntry();
      this.pagination.current = 1;
      this.batchSelectFlag = false;
      this.batchDeleteFlag = true;
      this.deleteButtonsVisible = false;
    },
    // ===================去重回填模态框================================
    // 打开去重回填模态框
    showImportBackfillModal() {
      this.importBackfillVisible = true;
      setModalAriaHidden(this, document);
    },
    handleImportBackfillClose() {
      this.importBackfillVisible = false;
    },
    handleImportBackfillOK() {
      this.init();
      this.handleImportBackfillClose();
    },
    // ===================去重回填模态框 (v3版本)================================
    // 打开去重回填模态框 (v3版本)
    showImportBackfillModal_v3() {
      this.importBackfillVisible_v3 = true;
      setModalAriaHidden(this, document);
    },
    handleImportBackfillClose_v3() {
      this.importBackfillVisible_v3 = false;
    },
    handleImportBackfillOK_v3() {
      this.init();
      this.handleImportBackfillClose_v3();
    },
    // ===================去重回填模态框 (v2.5版本)================================
    // 打开去重回填模态框 (v2.5版本)
    showImportBackfillModal_v2_5() {
      this.importBackfillVisible_v2_5 = true;
      setModalAriaHidden(this, document);
    },
    handleImportBackfillClose_v2_5() {
      this.importBackfillVisible_v2_5 = false;
    },
    handleImportBackfillOK_v2_5() {
      this.init();
      this.handleImportBackfillClose_v2_5();
    },
    // ===================去重回填模态框 (v2版本)================================
    // 打开去重回填模态框 (v2版本)
    showImportBackfillModal_v2() {
      this.importBackfillVisible_v2 = true;
      setModalAriaHidden(this, document);
    },
    handleImportBackfillClose_v2() {
      this.importBackfillVisible_v2 = false;
    },
    handleImportBackfillOK_v2() {
      this.init();
      this.handleImportBackfillClose_v2();
    },
    // ===================去重回填模态框 (v1.5版本)================================
    // 打开去重回填模态框 (v1.5版本)
    showImportBackfillModal_v1_5() {
      this.importBackfillVisible_v1_5 = true;
      setModalAriaHidden(this, document);
    },
    handleImportBackfillClose_v1_5() {
      this.importBackfillVisible_v1_5 = false;
    },
    handleImportBackfillOK_v1_5() {
      this.init();
      this.handleImportBackfillClose_v1_5();
    },
    // ===================导入csv文件================================
    // 上传前校验文件格式
    beforeUpload(file) {
      const isCsv = file.name.endsWith(".csv");
      if (!isCsv) {
        message.error("只能上传 CSV 文件!");
        return false;
      }
      this.importLoading = true;
      this.loading = true;
      const reader = new FileReader();
      reader.onload = async (e) => {
        const content = e.target.result;
        try {
          const formData = new FormData();
          formData.append("file", file);

          const res = await entryReadExcel(formData);
          this.dataSource = res.data;
          this.pagination.total = this.dataSource.length;
          message.success("文件读取成功");
        } catch (error) {
          console.error("文件解析失败", error);
          message.error("文件解析失败");
        } finally {
          this.importLoading = false;
          this.loading = false;
        }
      };
      reader.onerror = () => {
        message.error("文件读取失败");
        this.importLoading = false;
        this.loading = false;
      };
      reader.readAsText(file);
      return false; // 在文件开始上传之前阻止文件上传操作
    },
    // ==============去重按钮点击事件======================
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
        const data = this.dataSource;
        const params = {
          attributes: this.filterModal.duplicateCols,
        };

        const res = await exportDeduplicatedData(params, data);
        console.log("去重数据1", res);

        // 更新表格数据为去重后的结果
        this.dataSource = res.data.notReplicatedEntryInfos;
        this.pagination.total = res.data.notReplicatedEntryInfos.length;
        // 保存去重后的数据用于导出
        this.deduplicatedDataSource = res.data.notReplicatedEntryInfos;
        // 先导出JSON映射文件
        this.exportIdMap(res.data.idRelationMap);
        // 通过导出按钮组件打开导出模态框
        if (this.$refs.exportButtonRef) {
          this.$refs.exportButtonRef.showExportModal();
        }
        message.success("去重成功");
        this.filterModal.visible = false;
      } catch (error) {
        console.error("去重失败", error);
        notification.error({
          message: "去重失败！",
          description: error.message || "去重失败",
          duration: 0,
        });
      } finally {
        this.loading = false;
      }
    },
    exportIdMap(idMap) {
      const jsonString = JSON.stringify(idMap, null, 2);
      const blob = new Blob([jsonString], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "去重映射.json";
      link.click();
      URL.revokeObjectURL(url);
    },
    // 导出模态框关闭后，清空去重数据
    handleExportAfterClose() {
      this.deduplicatedDataSource = [];
    },
    // ===================批量删除按钮点击事件======================
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
      this.dataSource = this.dataSource.filter(
        (item) => !this.selectedRowKeys.includes(item.id)
      );
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
    // ===================表格======================
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
      this.selectedRows = [...this.selectEntry.values()];
      this.selectedRowKeys = [...this.selectEntry.keys()];
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
      this.selectedRows = [...this.selectEntry.values()];
      this.selectedRowKeys = [...this.selectEntry.keys()];
    },
    // 复选框全选事件
    selectAllEntry() {
      if (!this.dataSource || this.dataSource.length === 0) {
        message.warning("没有数据可全选");
        return;
      }
      this.dataSource.forEach((item) => {
        if (!this.selectedRowKeys.includes(item.id)) {
          this.selectedRowKeys.push(item.id);
          this.selectedRows.push(item);
          this.selectEntry.set(item.id, item);
        }
      });
    },
    //复选框反选事件
    clearAllEntry() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.selectEntry.clear();
    },
    // 动态设置表格高度
    setTableHeight() {
      this.$nextTick(() => {
        setTableHeight(this, 32, 166, 90, { ok: true, h: this.box });
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
    // ===查询box已隐藏，方法均未使用=======================================
    getSearchClick() {
      this.resetSelected();
      this.getSearch();
    },
    getSearch() {
      this.dataSource = [];
      this.clearAllEntry();
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
        entryState: this.$currentDepartment.ops.has("entryState3") ? "3" : null, // 查询条件中的词条状态
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
    reset() {
      this.resetSearch();
      this.resetSelected();
    },
    resetSearch() {
      // console.log("查询相关的封起来了，暂时没有要用的地方");
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
    // 展示条件切换并保存用户偏好(查询条件处的，已经封起来了)
    changeSearchCondition(checkedValue) {
      // changeColumn(
      //   "searchCondition-fileManage",
      //   200,
      //   checkedValue,
      //   this,
      //   false,
      //   entryParams.searchConditionList
      // );
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
