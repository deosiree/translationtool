<template>
  <div class="glossary-page">
    <a-tabs v-model:activeKey="mainTab" class="glossary-tabs" @change="onMainTabChange">
      <a-tab-pane key="syk" tab="术语词典">
  <div class="box" ref="box">
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form
          :model="search"
          name="horizontal_login"
          layout="inline"
          autocomplete="off"
          :label-col="labelCol"
        >
          <a-form-item label="词条" name="entry">
            <a-input
              v-model:value="search.entry"
              placeholder="请输入词条"
            ></a-input>
          </a-form-item>
          <a-form-item :label="GLOSSARY_LABEL.translate" name="translate">
            <a-input
              v-model:value="search.translate"
              :placeholder="GLOSSARY_PLACEHOLDER.translate"
            ></a-input>
          </a-form-item>
          <!-- <a-form-item label="翻译过滤" name="filter_translate">
            <a-input v-model:value="search.filter_translate" placeholder="请输入翻译"></a-input>
          </a-form-item> -->
          <a-form-item :label="GLOSSARY_LABEL.translateType" name="type">
            <a-select
              v-model:value="search.type"
              style="width: 186px"
              :placeholder="GLOSSARY_PLACEHOLDER.translateType"
              :options="translateTypes"
              :fieldNames="{ label: 'name', value: 'name' }"
              allowClear
            >
            </a-select>
          </a-form-item>
          <a-form-item :label="GLOSSARY_LABEL.translateState" name="translateState">
            <TransStateSelect
              :translateState="search.translateState"
              @update:translateState="search.translateState = $event"
              :style="'width: 186px'"
              :placeholder="GLOSSARY_PLACEHOLDER.translateState"
            />
          </a-form-item>
          <a-form-item :label="GLOSSARY_LABEL.visualRange" name="visualRange">
            <a-select
              v-model:value="search.visualRange"
              style="width: 186px"
              :placeholder="GLOSSARY_PLACEHOLDER.visualRange"
              :options="visualRanges"
              allowClear
            >
            </a-select>
          </a-form-item>
          <a-form-item label="校验类型" name="searchType">
            <a-select
              v-model:value="search.searchType"
              style="width: 186px"
              placeholder="请选择校验类型"
              :options="searchTypes"
              allowClear
            >
            </a-select>
          </a-form-item>
        </a-form>
      </template>
      <template v-slot:operate>
        <GlossarySearchOperate
          :search="search"
          :currentPage="pagination.current"
          @reset="onResetData"
          @search="getSearchClick"
        />
      </template>
    </SearchBox>
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <template v-slot:operate>
        <div
          ref="button"
          v-if="true"
          style="margin-bottom: 8px; display: flex; gap: 10px"
        >
          <BatchSelectButton
            :size="'middle'"
            :columns="columns"
            :dataSource="dataSource"
            :getSearch="getSearch"
            selected-button-text="已选术语"
            :selectAllFn="selectAllGlossaryEntries"
            v-model:search="search"
            v-model:lastSearch="lastSearch"
            v-model:loading="loading"
            v-model:selectEntry="selectEntry"
            v-model:selectedRows="selectedRows"
            v-model:selectedRowKeys="selectedRowKeys"
            v-model:batchSelectFlag="batchSelectFlag"
            v-model:batchSelectVisible="batchSelectVisible"
            @split="onSelectedSplit"
          />
          <ColumnFilter
            v-model="checkedColumn"
            :columns="columnSettingsList"
            :overlay-style="overlayStyle"
            button-size="middle"
            col-pref-name="colPref-glossary"
            :normal-width="150"
            :need-filter="false"
            @change="syncColumnsFromPref"
          />
        </div>
      </template>
      <template v-slot:data>
        <div style="width: 100%; position: absolute">
          <a-table
            bordered
            class="ant-table-striped"
            :columns="columns"
            :data-source="dataSource"
            :row-key="(record) => record.id"
            :scroll="tableHeight"
            :pagination="pagination"
            :loading="loading"
            :rowClassName="getRowClassName"
            ref="glossaryTable"
            @resizeColumn="handleResizeColumn"
            :row-selection="
              batchSelectFlag
                ? {
                    selectedRowKeys: selectedRowKeys,
                    onChange: onSelectChange,
                    onSelect: onSelect,
                    onSelectAll: onSelectAll,
                  }
                : null
            "
          >
            <template #bodyCell="{ column, record, text }">
              <template v-if="column.dataIndex === 'translate'">
                <div>
                  <template v-if="editableData[record.id]">
                    <!-- <a-input v-model:value="editableData[record.id][column.dataIndex]" @pressEnter="editOK(record)" style="margin: -5px 0"
                        @click="clickInput" :ref="el => inputRefs[record.id] = el" /> -->
                    <Input
                      :value="editableData[record.id]?.[column.dataIndex] ?? ''"
                      @update:value="
                        (val) => handleTranslateChange(val, record, column)
                      "
                      @pressEnter="editOK(record)"
                    />
                  </template>
                  <template v-else>
                    <span @dblclick="dbclickEdited(record.id)">{{ text }}</span>
                  </template>
                </div>
              </template>
              <template v-if="column.dataIndex === 'translateState'">
                <TransStateBadge :translateState="text" />
              </template>
              <template v-if="column.dataIndex === 'operation'">
                <OperationCellOverflow
                  v-if="editableData[record.id]"
                  :inline-visible-count="2"
                >
                  <OpItem label="保存" @click.stop="save(record.id)" />
                  <OpItem
                    label="取消"
                    type="danger"
                    @click.stop="cancel(record.id)"
                  />
                </OperationCellOverflow>
                <OperationCellOverflow v-else :inline-visible-count="2">
                  <OpItem
                    :label="`详情(${record.relationCount})`"
                    @click.stop="viewRelation(record)"
                  />
                </OperationCellOverflow>
              </template>
            </template>
          </a-table>
        </div>
      </template>
    </DataBox>
  </div>
      </a-tab-pane>
      <a-tab-pane key="termWord" tab="术语字典">
        <div class="box term-word-box">
          <TermWordDictionary
            v-if="mainTab === 'termWord'"
            ref="termWordDict"
            :bootstrap-status="termWordBootstrapStatus"
          />
        </div>
      </a-tab-pane>
    </a-tabs>
  <RelationModal
    :visible="relationVisible"
    :currentData="relationData"
    @relationClose="relationClose"
  ></RelationModal>
  <SplitExportModal
    :visible="splitExportVisible"
    :sourceItems="selectEntry"
    @close="splitExportVisible = false"
    @exported="onSplitExported"
    @imported="onSplitImported"
  />
  </div>
</template>
<script>
import { message, Modal } from "ant-design-vue";
import locale from "ant-design-vue/es/date-picker/locale/zh_CN";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import ResetButton from "@/components/Button/resetButton.vue";
import BatchSelectButton from "@/components/Button/batchSelectButton.vue";
import TransStateSelect from "@/components/select/transStateSelect.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import RelationModal from "@/views/glossary/relationModal.vue";
import TermWordDictionary from "@/views/glossary/TermWordDictionary.vue";
import GlossarySearchOperate from "@/views/glossary/shared/GlossarySearchOperate.vue";
import SplitExportModal from "@/views/glossary/shared/SplitExportModal.vue";
import {
  GLOSSARY_LABEL,
  GLOSSARY_PLACEHOLDER,
} from "@/views/glossary/shared/glossaryQueryLabels.js";
import Input from "@/components/cellEditor/input_IME.vue";

const GLOSSARY_MAIN_TAB_KEY = "glossaryMainTab";
const GLOSSARY_MAIN_TABS = new Set(["syk", "termWord"]);

function readStoredMainTab() {
  try {
    const v = localStorage.getItem(GLOSSARY_MAIN_TAB_KEY);
    return GLOSSARY_MAIN_TABS.has(v) ? v : "syk";
  } catch {
    return "syk";
  }
}
import { updateUserPartiality } from "@/http/api/userPartiality";
import { cloneDeep, flatMap } from "lodash-es";
import {
  PlusOutlined,
  DeleteOutlined,
  CopyOutlined,
  SaveOutlined,
  SendOutlined,
  PlusCircleOutlined,
  ExclamationCircleOutlined,
  SettingOutlined,
} from "@ant-design/icons-vue";
import { getLanguage } from "@/http/api/translate";
import {
  getSykEntry,
  checkSameEntry,
  getSykNotUsed,
  checkSykEntry,
  updateSykEntry,
  getSykEntryRelation,
  getSameEntryRelation,
} from "@/http/api/glossary";
import {
  setTableHeight,
  handleResizeColumn,
  getRowClassName,
} from "@/utils/tableUtils";
import { applyTable, syncColumnsFromPref as applyTableColumnsFromPref } from "@/components/ColumnFilter";
import {
  onSelectChange,
  onSelect,
  onSelectAll,
  pageChange,
  applyBatchSelectAll,
} from "@/utils/selectionUtils";
import { fetchAllByPaging } from "@/utils/fetchAllByPaging";
import { clickInput, setModalAriaHidden } from "@/utils/domUtils";
import { getSearch } from "@/utils/requestUtils";
import commonParam, {
  glossaryParams,
  glossaryAllCols,
  glossaryPresets,
} from "@/constants/commonParam.js";
import ColumnFilter from "@/components/ColumnFilter/ColumnFilter.vue";
import {
  OpItem,
  OperationCellOverflow,
} from "@/components/OperationColumn";
import { defineComponent, ref, createVNode, nextTick } from "vue";
export default {
  components: {
    SearchBox,
    DataBox,
    ResetButton,
    BatchSelectButton,
    TransStateSelect,
    TransStateBadge,
    RelationModal,
    TermWordDictionary,
    GlossarySearchOperate,
    SplitExportModal,
    Input,
    ColumnFilter,
    OpItem,
    OperationCellOverflow,
    PlusOutlined,
    DeleteOutlined,
    CopyOutlined,
    SaveOutlined,
    SendOutlined,
    PlusCircleOutlined,
    SettingOutlined,
  },
  data() {
    return {
      GLOSSARY_LABEL,
      GLOSSARY_PLACEHOLDER,
      mainTab: readStoredMainTab(),
      /** 拆分导入后挂载术语字典时的初始翻译状态（如 '1' 待审核） */
      termWordBootstrapStatus: null,
      locale: locale,
      labelCol: { style: { width: "84px" } },
      search: {
        entry: "",
        translate: null,
        translateState: null,
        type: null,
        state: null,
        visualRange: null,
        searchType: null,
        filter_translate: null, // 翻译过滤字段
      },
      lastSearch: {}, // 存储上一次的查询条件
      visualRanges: Object.values(commonParam.departmentMap).map((dept) => ({
        label: dept.label,
        value: dept.label,
      })),
      searchTypes: [
        { label: "格式校验", value: "checkSykEntry" },
        { label: "空挂术语", value: "getSykNotUsed" },
        { label: "查重自检", value: "checkSameEntry" },
        { label: "条件查询", value: "getSykEntry" },
      ],
      tableTitle: "术语列表",
      dataHeight: 400,
      // tableHeight: { x: "100%", y: 0 },
      tableHeight: { x: "max-content", y: 0 },
      loading: false,
      columns: [],
      dataSource: [],
      selectedRowKeys: [],
      selectedRows: [],
      selectEntry: [], // 已存词条，很重要，用于批量选择
      editableData: {},
      translateTypes: [],
      pagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
      relationVisible: false,
      relationData: [],
      overlayStyle: glossaryParams.overlayStyle,
      columnSettingsList: [],
      checkedColumn: [],
      batchSelectFlag: false, // 批量选择的显示（全选/反选）
      isGetSykEntry: true,
      isCheckSameEntry: false,
      batchSelectVisible: false,
      splitExportVisible: false,
      apiFunctions: {
        getSykEntry: this.getSykEntry,
        getSykNotUsed: this.getSykNotUsed,
        checkSameEntry: this.checkSameEntry,
        checkSykEntry: this.checkSykEntry,
      },
      requestId: null, // 存储校验按钮的http请求
      inputRefs: {}, // 初始化一个对象用于存储输入框的 ref
    };
  },
  mounted() {
    let _this = this;
    this.$nextTick(() => {
      // console.log("localStorage", localStorage);// 本地存储
      // console.log("store data",this.$store.state.user.department);// Vuex存储数据
      this.search.visualRange = this.$store.state.user.department; // 设置可见范围

      this.init();
      // 读取本地存储的用户偏好
      applyTable(this, {
        allCols: glossaryAllCols,
        preset: glossaryPresets.glossary,
        ctx: { pagination: this.pagination },
        colPrefName: "colPref-glossary",
        normalWidth: 150,
        needFilter: false,
      });
      /** 控制table的高度 */
      window.onresize = function () {
        _this.setTableHeight();
      };
    });
  },
  unmounted() {
    //注销window.onresize事件
    window.onresize = null;
  },
  watch: {
    "search.searchType": {
      immediate: true,
      handler(newVal) {
        if (newVal && newVal !== "getSykEntry") {
          this.isGetSykEntry = false;
        } else {
          this.isGetSykEntry = true;
        }
        if (newVal && newVal !== "checkSameEntry") {
          this.isCheckSameEntry = false;
        } else {
          this.isCheckSameEntry = true;
        }
      },
    },
  },
  methods: {
    onMainTabChange(key) {
      const next = GLOSSARY_MAIN_TABS.has(key) ? key : "syk";
      this.mainTab = next;
      try {
        localStorage.setItem(GLOSSARY_MAIN_TAB_KEY, next);
      } catch {
        /* ignore quota / private mode */
      }
      if (next !== "termWord") {
        this.termWordBootstrapStatus = null;
      }
      if (next === "syk") {
        this.$nextTick(() => {
          if (typeof this.setTableHeight === "function") this.setTableHeight();
        });
      }
    },
    /**
     * 打开拆分导出弹窗（需已选术语）
     * @returns {void}
     */
    openSplitExport() {
      if (!this.selectEntry?.length) {
        message.warning("请先批量选择术语");
        return;
      }
      this.splitExportVisible = true;
    },
    /**
     * 已选术语弹窗内点「拆分」
     * @returns {void}
     */
    onSelectedSplit() {
      this.batchSelectVisible = false;
      this.openSplitExport();
    },
    /**
     * 拆分导出完成：提示切到术语字典导入
     * @returns {void}
     */
    onSplitExported() {
      message.info("也可切换到「术语字典」Tab 使用「更多操作 → 导入」入库");
    },
    /**
     * 拆分一键导入完成：切到术语字典、以待审核挂载并刷新（数据已落库 status=1）
     * @param {{ created?: number, skipped?: number }} [data]
     * @returns {void}
     */
    onSplitImported(data) {
      // 先设 bootstrap，再切 Tab，避免 mounted 先按默认「已审核」拉取再被覆盖
      this.termWordBootstrapStatus = "1";
      const alreadyOnDict = this.mainTab === "termWord";
      this.mainTab = "termWord";
      try {
        localStorage.setItem(GLOSSARY_MAIN_TAB_KEY, "termWord");
      } catch {
        /* ignore */
      }
      this.$nextTick(() => {
        const dict = this.$refs.termWordDict;
        if (alreadyOnDict && dict?.search) {
          dict.search.status = "1";
          dict.fetchList?.();
        }
      });
      message.success(
        `导入完成：新增 ${data?.created ?? 0}，跳过 ${data?.skipped ?? 0}（已写入库，筛「待审核」可见）`,
      );
    },
    syncColumnsFromPref() {
      applyTableColumnsFromPref(this);
    },
    // 初始化
    init() {
      this.setTableHeight();
      this.getLanguage();
      this.getSearchClick(); // 需要增加取消请求，再在初始化中调用，否则切换查询条件会发生覆盖
    },
    // 获取翻译语种
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        if (!res || !res.data || !Array.isArray(res.data.list)) {
          this.translateTypes = [];
          return;
        }
        this.translateTypes = res.data.list;
      });
    },
    // 查询按钮点击事件
    getSearchClick() {
      this.batchSelectFlag = false;
      this.pagination.current = 1;
      this.getSearch();
    },
    /**
     * 当前查询类型对应的列表 HTTP 函数（不经过会清空已选的 getSearch）。
     * @returns {(params: object, data: object) => Promise} 
     */
    resolveGlossaryListApi() {
      const option = this.search.searchType || "getSykEntry";
      const map = {
        getSykEntry,
        getSykNotUsed,
        checkSameEntry,
        checkSykEntry,
      };
      return map[option] || getSykEntry;
    },
    /**
     * 「选择全部」：按当前筛选跨页拉全量术语并写入已选（不改 UI pagination）。
     * @returns {Promise<void>}
     */
    async selectAllGlossaryEntries() {
      this.loading = true;
      try {
        const api = this.resolveGlossaryListApi();
        const searchBody = { ...this.search };
        const rows = await fetchAllByPaging(async (page, pageSize) => {
          const res = await api(
            {
              pageIndex: page,
              pageSize,
              requestId: `selectAll-${Date.now().toString(16)}`,
            },
            searchBody
          );
          if (!res?.data) {
            return { list: [], total: 0 };
          }
          // checkSykEntry 返回整表数组；其它接口为 { list, totalNum }
          if (Array.isArray(res.data)) {
            return { list: res.data, total: res.data.length };
          }
          return {
            list: Array.isArray(res.data.list) ? res.data.list : [],
            total: res.data.totalNum ?? res.data.total ?? 0,
          };
        });
        applyBatchSelectAll(this, rows);
      } catch (e) {
        message.error(e?.message || "选择全部失败");
      } finally {
        this.loading = false;
      }
    },
    // 查询事件
    getSearch() {
      this.dataSource = [];
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.selectEntry = [];
      // 接口方法集合
      const apiFunctions = {
        getSykEntry: this.getSykEntry,
        getSykNotUsed: this.getSykNotUsed,
        checkSameEntry: this.checkSameEntry,
        checkSykEntry: this.checkSykEntry,
      };
      // 选项赋值
      let option = "";
      if (!this.search.searchType) {
        option = "getSykEntry"; // 默认选项
      } else {
        option = this.search.searchType;
      }
      // 对齐 search / lastSearch（含 searchType），避免批量选择误判条件变化
      this.search.searchType = option;
      this.lastSearch = { ...this.search };

      // 入参+请求体
      // this.search.type = this.search.translateType;
      let params = {
        params: {
          pageIndex: this.pagination.current,
          pageSize: this.pagination.pageSize,
          requestId: `${option}-${Date.now().toString(16)}`,
          // translateType: this.search.type,
        },
        data: this.search,
        lastRequestId: this.requestId, // 获取上一次的请求对象requestId
      };
      this.requestId = params.params.requestId; // 保存当前请求对象的requestId
      // console.log(`上次id:${params.lastRequestId};当前id:${this.requestId}`);

      // 调用getSearch方法
      getSearch(this, params, option, apiFunctions);
    },
    // 条件查询
    getSykEntry(params, data, lastRequestId) {
      return getSykEntry(params, data, lastRequestId).then((res) => {
        if (!res) {
          return;
        }
        if (!res.data || !Array.isArray(res.data.list)) {
          this.dataSource = [];
          this.pagination.total = 0;
          return;
        }
        this.dataSource = res.data.list;
        this.pagination.total = res.data.totalNum || 0;
        for (let item of this.dataSource) {
          // if (!item.type) item.type = "英文"; // 后端BUG，type字段为空
          getSykEntryRelation([item]).then((res) => {
            if (!res || !res.data || !Array.isArray(res.data.list)) {
              item["relationCount"] = 0;
              item["reslations"] = [];
              return;
            }
            item["relationCount"] = res.data.list.length;
            item["reslations"] = res.data.list;
          });
        }
      });
    },
    // 查重自检查询
    checkSameEntry(params, data, lastRequestId) {
      // console.log("查重自检", params, data, lastRequestId);
      return checkSameEntry(params, data, lastRequestId).then((res) => {
        if (!res) {
          return;
        }
        if (!res.data || !Array.isArray(res.data.list)) {
          this.dataSource = [];
          this.pagination.total = 0;
          return;
        }
        this.dataSource = res.data.list;
        this.pagination.total = res.data.totalNum || 0;
        for (let item of this.dataSource) {
          // if (!item.type) item.type = "英文"; // 后端BUG，type字段为空
          getSameEntryRelation(item).then((res) => {
            if (!res || !res.data || !Array.isArray(res.data.list)) {
              item["relationCount"] = 0;
              item["reslations"] = [];
              return;
            }
            item["relationCount"] = res.data.list.length;
            item["reslations"] = res.data.list;
          });
        }
      });
    },
    // 空挂术语查询
    getSykNotUsed(params, data, lastRequestId) {
      return getSykNotUsed(params, data, lastRequestId).then((res) => {
        if (!res) {
          return;
        }
        if (!res.data || !Array.isArray(res.data.list)) {
          this.dataSource = [];
          this.pagination.total = 0;
          return;
        }
        this.dataSource = res.data.list;
        this.pagination.total = res.data.totalNum || 0;
        for (let item of this.dataSource) {
          // if (!item.type) item.type = "英文"; // 后端BUG，type字段为空
          // 空挂术语的详情都是0
          item["relationCount"] = 0;
          item["reslations"] = [];
        }
      });
    },
    // 格式校验查询
    checkSykEntry(params, data, lastRequestId) {
      return checkSykEntry(params, data, lastRequestId).then((res) => {
        if (!res) {
          return;
        }
        this.dataSource = res.data;
        this.pagination.total = this.dataSource.length;
        for (let item of this.dataSource) {
          // if (!item.type) item.type = "英文"; // 后端BUG，type字段为空
          if (item.notUsedByEntryInfo) {
            item["relationCount"] = 0;
            item["reslations"] = [];
          } else {
            getSykEntryRelation([item]).then((res) => {
              if (!res || !res.data || !Array.isArray(res.data.list)) {
                item["relationCount"] = 0;
                item["reslations"] = [];
                return;
              }
              item["relationCount"] = res.data.list.length;
              item["reslations"] = res.data.list;
            });
          }
        }
      });
    },
    // 保存
    save(id) {
      this.loading = true;
      updateSykEntry([this.editableData[id]])
        .then((res) => {
          message.success("编辑成功！");
          delete this.editableData[id];
          this.getSearch();
        })
        .catch((err) => {
          message.error("编辑失败！", err.message);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    // 取消
    cancel(id) {
      delete this.editableData[id];
    },
    // 回车编辑框
    editOK(record) {
      this.save(record.id);
      // record.translate = this.editableData[record.id].translate;
      // delete this.editableData[record.id];
    },
    handleTranslateChange(value, record, column) {
      // 行已经不在编辑态了，直接忽略，避免给 undefined 赋值
      if (!this.editableData[record.id]) return;

      // 只更新当前列，比如 translate
      this.editableData[record.id][column.dataIndex] = value;
    },
    // 查看详情
    viewRelation(record) {
      this.relationData = record.reslations;
      // console.log(this.relationData);
      this.relationVisible = true;
      setModalAriaHidden(this, document);
    },
    // 关闭详情
    relationClose() {
      this.relationVisible = false;
    },
    // 聚焦单元格
    focusCell(recordId) {
      // console.log("聚焦单元格", recordId);
      nextTick(() => {
        const inputElement = this.inputRefs[recordId];
        if (inputElement) {
          const input = inputElement.input;
          // console.log("input", input);
          if (input) {
            input.focus();
            const length = input.value.length;
            // console.log("input.value", input.value);
            // 设置光标位置到文本末尾
            input.setSelectionRange(length, length);
          }
        }
      });
    },
    // 双击未打开编辑状态的单元格
    dbclickEdited(recordId) {
      this.editableData[recordId] = cloneDeep(
        this.dataSource.filter((item) => recordId === item.id)[0]
      );
      // console.log("editableData", this.editableData[recordId]);

      this.focusCell(recordId); // 调用点击编辑方法
    },
    // 添加表格行点击事件
    customRow(record, index) {
      return {
        onDblclick: (event) => {
          if (this.editableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }
          this.editableData[record.id] = cloneDeep(
            this.dataSource.filter((item) => record.id === item.id)[0]
          );
        },
      };
    },

    // 重置
    onResetData(newSearch, newPage) {
      this.search = newSearch;
      this.pagination.current = newPage;
      this.getSearch();
    },
    // 阻止事件冒泡，防止事件传播到父元素
    clickInput(event) {
      clickInput(this, event);
    },
    // 动态设置表格高度
    setTableHeight() {
      setTableHeight(this, 8, 150, 30); // 调用工具函数
    },
    // 表格列可伸缩
    handleResizeColumn(w, col) {
      return handleResizeColumn(w, col); // 调用工具函数
    },
    // 设置表格每一行的 class
    getRowClassName(record, index) {
      return getRowClassName(record, index, this.selectedRowIndex); // 调用工具函数
    },
    // 复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      onSelectChange(this, selectedRowKeys, selectedRows);
    },
    // 复选框点击事件
    onSelect(record, selected) {
      onSelect(this, record, selected, this.batchSelectFlag);
    },
    // 复选框当前页全选/反选框点击事件
    onSelectAll(selected, selectedRows, changeRows) {
      onSelectAll(
        this,
        selected,
        selectedRows,
        changeRows,
        this.batchSelectFlag
      );
    },
    // 分页切换
    pageChange(page, pageSize) {
      // console.log("pageChange", page, pageSize, );
      if (this.isGetSykEntry || this.isCheckSameEntry)
        // 条件查询||查重自检时，根据分页信息回调查询函数
        // if (!this.search.searchType || this.search.searchType == "getSykEntry")
        pageChange(this, page, pageSize, this.getSearch);
      // 需要回调查询接口，否则一次查询出所有数据，对前端压力太大了，所以每次分页查询只查询当前页的数据
      else pageChange(this, page, pageSize); // 不能回调查询接口，否则若使用了全选功能的话，切换到下一页全选又没了
    },
  },
};
</script>
<style lang="less">
@import url("@/assets/style/common.less");
</style>
<style scoped lang="less">
.glossary-page {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.glossary-tabs {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  :deep(.ant-tabs-nav) {
    padding: 8px 16px 0;
    margin-bottom: 0;
    flex-shrink: 0;
  }
  :deep(.ant-tabs-content-holder) {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }
  :deep(.ant-tabs-content),
  :deep(.ant-tabs-tabpane) {
    height: 100%;
    min-height: 0;
  }
  :deep(.ant-tabs-content) {
    display: flex;
    flex-direction: column;
  }
  :deep(.ant-tabs-tabpane-active) {
    display: flex !important;
    flex-direction: column;
    min-height: 0;
  }
}
.box {
  width: 100%;
  height: 100%;
  padding: 16px;
  // border: 1px solid red;
  /* 换行后每个表单项的间距 */
  & :deep(.search .form .ant-row) {
    margin-bottom: 8px !important;
  }
}
.term-word-box {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
}
</style>
<style lang="less">
.editable-cell {
  position: relative;
  .editable-cell-input-wrapper,
  .editable-cell-text-wrapper {
    padding-right: 24px;
  }

  .editable-cell-text-wrapper {
    padding: 5px 24px 5px 5px;
  }

  .editable-cell-icon,
  .editable-cell-icon-check {
    position: absolute;
    right: 0;
    width: 20px;
    cursor: pointer;
  }

  .editable-cell-icon {
    margin-top: 4px;
    display: none;
  }

  .editable-cell-icon-check {
    line-height: 28px;
  }

  .editable-cell-icon:hover,
  .editable-cell-icon-check:hover {
    color: #108ee9;
  }

  .editable-add-btn {
    margin-bottom: 8px;
  }
}
.editable-cell:hover .editable-cell-icon {
  display: inline-block;
}
.ant-table-cell {
  .ant-form-item {
    margin-bottom: 0px;
  }
}
</style>