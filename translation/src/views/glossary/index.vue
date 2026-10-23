<template>
  <div class="box" ref="box">
    <div class="glossary-search">
      <a-spin
        :spinning="isCollectingSelection"
        tip="正在全量加载查询结果，完成后可修改查询条件"
        wrapper-class-name="glossary-search-mask"
        role="status"
        aria-live="polite"
      >
        <div
          class="glossary-search-content"
          :inert="isCollectingSelection ? '' : null"
          :aria-disabled="isCollectingSelection"
        >
          <SearchBox ref="search">
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
                  :disabled="isCollectingSelection"
                ></a-input>
              </a-form-item>
              <a-form-item label="翻译" name="translate">
                <a-input
                  v-model:value="search.translate"
                  placeholder="请输入翻译"
                  :disabled="isCollectingSelection"
                ></a-input>
              </a-form-item>
              <!-- <a-form-item label="翻译过滤" name="filter_translate">
                <a-input v-model:value="search.filter_translate" placeholder="请输入翻译"></a-input>
              </a-form-item> -->
              <a-form-item label="翻译类型" name="type">
                <a-select
                  v-model:value="search.type"
                  style="width: 186px"
                  placeholder="请选择翻译类型"
                  :options="translateTypes"
                  :fieldNames="{ label: 'name', value: 'name' }"
                  :disabled="isCollectingSelection"
                  allowClear
                >
                </a-select>
              </a-form-item>
              <a-form-item label="翻译状态" name="translateState">
                <TransStateSelect
                  :translateState="search.translateState"
                  @update:translateState="search.translateState = $event"
                  :style="'width: 186px'"
                  :placeholder="'请选择翻译状态'"
                  :disabled="isCollectingSelection"
                />
              </a-form-item>
              <a-form-item label="可见范围" name="visualRange">
                <a-select
                  v-model:value="search.visualRange"
                  style="width: 186px"
                  placeholder="请选择可见范围"
                  :options="visualRanges"
                  :disabled="isCollectingSelection"
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
                  :disabled="isCollectingSelection"
                  allowClear
                >
                </a-select>
              </a-form-item>
            </a-form>
          </template>
          <template v-slot:operate>
            <ResetButton
              :size="'middle'"
              :search="search"
              :currentPage="pagination.current"
              :disabled="isCollectingSelection"
              @resetData="onResetData"
            />
            <a-button
              type="primary"
              size="middle"
              :disabled="isCollectingSelection"
              @click="getSearchClick"
            >查询</a-button>
          </template>
          </SearchBox>
        </div>
      </a-spin>
    </div>
    <DataBox
      class="glossary-data-box"
      :title="tableTitle"
      :showOperate="true"
    >
      <template v-slot:operate>
        <SelectionToolbar ref="button">
          <SelectionToolbarButton
            text="已选词条"
            preset="selected"
            :badge-count="selectionToolbarState.badgeCount"
            :loading="selectionToolbarState.loading"
            :disabled="selectionToolbarState.loading"
            @click="viewSelectedEntries"
          />
          <SelectionToolbarButton
            text="取消选择"
            :disabled="selectionToolbarState.clearDisabled"
            @click="clearSelectionState"
          />
          <ColumnFilter
            v-model="checkedColumn"
            :columns="columnSettingsList"
            :overlay-style="overlayStyle"
            button-size="small"
            button-type="text"
            col-pref-name="colPref-glossary"
            :normal-width="150"
            :need-filter="false"
            @change="syncColumnsFromPref"
          />
          <BatchSelectModal
            :visible="batchSelectVisible"
            :data-source="selectEntry"
            :selected-row-keys="selectedRowKeys"
            :selected-rows="selectedRows"
            @update:data-source="handleSelectedEntriesChange"
            @update:selected-row-keys="selectedRowKeys = $event"
            @update:selected-rows="handleSelectedEntriesChange"
            @batch-select-close="batchSelectClose"
            @batch-select-cancel="clearSelectionState"
            @refresh="getSearch"
          />
        </SelectionToolbar>
      </template>
      <template v-slot:data>
        <TableViewport
          class="ant-table-striped table-cell-overflow"
          :bordered="true"
          adaptive-columns
          table-layout="fixed"
          :height="'100%'"
          :scroll="tableHeight"
          :columns="columns"
          :data-source="dataSource"
          :row-key="(record) => record.id"
          :pagination="pagination"
          :loading="loading"
          :rowClassName="getRowClassName"
          ref="glossaryTable"
          @resizeColumn="handleResizeColumn"
          :row-selection="{
            columnWidth: 48,
            selectedRowKeys: tableSelectedRowKeys,
            onSelect: onSelect,
            onSelectAll: onSelectAll,
          }"
        >
            <template #headerCell="{ title, column }">
              <TableHeaderHint
                v-if="['tag', 'comment'].includes(column.dataIndex)"
                :title="title"
                :column="column"
              />
              <CellOverflowTooltip v-else-if="column.colValue" :content="title">
                {{ title }}
              </CellOverflowTooltip>
            </template>
            <template #bodyCell="{ column, record, text }">
              <template v-if="column.dataIndex === 'entry'">
                <TableCellText :text="formatCellText(text)" />
              </template>
              <template v-else-if="column.dataIndex === 'translate'">
                <TableCellText
                  :text="formatCellText(text)"
                  :value="editableData[record.id]?.[column.dataIndex] ?? ''"
                  :editing="!!editableData[record.id]"
                  @dblclick="dbclickEdited(record.id)"
                  @update:value="
                    (val) => handleTranslateChange(val, record, column)
                  "
                />
              </template>
              <template v-else-if="column.dataIndex === 'translateState'">
                <CellOverflowTooltip :content="translateStateLabel(text)">
                  <TransStateBadge :translateState="text" />
                </CellOverflowTooltip>
              </template>
              <template v-else-if="column.dataIndex === 'operation'">
                <div class="editable-row-operations">
                  <span v-if="editableData[record.id]">
                    <a-button
                      type="primary"
                      ghost
                      size="small"
                      @click.stop="save(record.id)"
                      >保存</a-button
                    >
                    <a-button
                      type="primary"
                      ghost
                      size="small"
                      danger
                      @click.stop="cancel(record.id)"
                      >取消</a-button
                    >
                  </span>
                  <span v-else>
                    <a-button
                      type="primary"
                      ghost
                      size="small"
                      @click.stop="viewRelation(record)"
                      >详情({{ record.relationCount }})</a-button
                    >
                  </span>
                </div>
              </template>
              <template v-else-if="column.dataIndex && column.dataIndex !== 'index'">
                <CellOverflowTooltip :content="formatCellText(text)" />
              </template>
            </template>
        </TableViewport>
      </template>
    </DataBox>
  </div>
  <RelationModal
    :visible="relationVisible"
    :currentData="relationData"
    @relationClose="relationClose"
  ></RelationModal>
</template>
<script>
import { message, Modal } from "ant-design-vue";
import locale from "ant-design-vue/es/date-picker/locale/zh_CN";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import TableViewport from "@/components/table/TableViewport.vue";
import ResetButton from "@/components/Button/resetButton.vue";
import BatchSelectModal from "@/components/Button/batchSelectModal.vue";
import TransStateSelect from "@/components/select/transStateSelect.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import RelationModal from "@/views/glossary/relationModal.vue";
import Input from "@/components/cellEditor/input_IME.vue";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip.vue";
import TableCellText from "@/components/table/TableCellText.vue";
import TableHeaderHint from "@/components/table/TableHeaderHint.vue";
import { formatCellText } from "@/components/table/cellText";
import { updateUserPartiality } from "@/http/api/userPartiality";
import { cloneDeep, flatMap } from "lodash-es";
import { onEditableCellInput } from "@/utils/validationUtils";
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
import { cancelRequest } from "@/http/request";
import {
  handleResizeColumn,
  getRowClassName,
} from "@/utils/tableUtils";
import { applyTable, syncColumnsFromPref as applyTableColumnsFromPref } from "@/components/ColumnFilter";
import {
  pageChange,
  collectAllPages,
  refreshTableSelectionById,
} from "@/utils/selectionUtils";
import { createQueryVersionGuard } from "@/utils/queryVersionGuard";
import SelectionToolbar from "@/components/selectToolBar/SelectionToolbar.vue";
import SelectionToolbarButton from "@/components/selectToolBar/SelectionToolbarButton.vue";
import { useSelectedEntries } from "@/components/selectToolBar/composables/useSelectedEntries";
import { useSelectionOperations } from "@/components/selectToolBar/composables/useSelectionOperations";
import { useSelectionToolbarState } from "@/components/selectToolBar/composables/useSelectionToolbarState";
import { useAdaptiveColumnWidths } from "@/components/table/useAdaptiveColumnWidths";
import { clickInput, setModalAriaHidden } from "@/utils/domUtils";
import commonParam, {
  glossaryParams,
  glossaryAllCols,
  glossaryPresets,
} from "@/constants/commonParam.js";
import ColumnFilter from "@/components/ColumnFilter/ColumnFilter.vue";
import { defineComponent, ref, createVNode, nextTick, markRaw } from "vue";

const SEARCH_APIS = {
  getSykEntry,
  getSykNotUsed,
  checkSameEntry,
  checkSykEntry,
};

const ADAPTIVE_COLUMN_KEYS = ["entry", "translate"];
const SELECTION_COLUMN_WIDTH = 48;

/**
 * 等待 Vue 更新和浏览器下一帧布局完成。
 * @param {Object} vm - 当前 Vue 组件实例。
 * @returns {Promise<void>} 渲染稳定后结束。
 */
function waitForRender(vm) {
  return vm.$nextTick().then(() => {
    return new Promise((resolve) => {
      let settled = false;
      const finish = () => {
        if (settled) return;
        settled = true;
        clearTimeout(timeoutId);
        resolve();
      };
      const timeoutId = setTimeout(finish, 64);
      if (typeof requestAnimationFrame === "function") {
        requestAnimationFrame(() => requestAnimationFrame(finish));
      }
    });
  });
}

export default {
  components: {
    SearchBox,
    DataBox,
    TableViewport,
    ResetButton,
    BatchSelectModal,
    SelectionToolbar,
    SelectionToolbarButton,
    TransStateSelect,
    TransStateBadge,
    RelationModal,
    Input,
    CellOverflowTooltip,
    TableCellText,
    TableHeaderHint,
    ColumnFilter,
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
      // tableHeight: { x: "100%", y: 0 },
      tableHeight: { x: "100%" },
      loading: false,
      columns: [],
      dataSource: [],
      selectedRowKeys: [],
      selectedRows: [],
      selectEntry: [], // 已存词条，很重要，用于批量选择
      queryGuard: createQueryVersionGuard(),
      selectionOperations: useSelectionOperations(),
      selectedEntryOperations: useSelectedEntries(),
      selectionScope: "",
      selectionSettlingCount: 0,
      activeSelectionRequestIds: [],
      requestSequence: 0,
      adaptiveColumnWidthState: useAdaptiveColumnWidths(),
      columnWidthObserver: null,
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
      isGetSykEntry: true,
      isCheckSameEntry: false,
      batchSelectVisible: false,
      inputRefs: {}, // 初始化一个对象用于存储输入框的 ref
    };
  },
  computed: {
    /**
     * 返回当前页与待处理全量选择意图对应的行键。
     * @returns {Array<string|number>} 当前页应显示为已选的行键。
     */
    tableSelectedRowKeys() {
      // 读取守卫版本，使异步全量操作开始/结束时立即刷新投影。
      this.selectionOperations.revision;
      const committedIds = new Set(this.selectedRowKeys);
      return this.dataSource
        .map((row) => row.id)
        .filter((id) => this.selectionOperations.project(
          id,
          this.selectionScope,
          committedIds.has(id)
        ));
    },
    /**
     * 返回工具栏当前选择数量、徽标和清空按钮状态。
     * @returns {Object} 选择工具栏状态。
     */
    selectionToolbarState() {
      this.selectionOperations.revision;
      return useSelectionToolbarState(
        this.selectEntry,
        this.selectionOperations.pendingCount > 0 || this.selectionSettlingCount > 0
      );
    },
    /**
     * 判断是否有全量选择操作仍在收集中。
     * @returns {boolean} 查询条件是否需要临时禁用。
     */
    isCollectingSelection() {
      this.selectionOperations.revision;
      return this.selectionOperations.pendingCount > 0;
    },
  },
  mounted() {
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
        lockCellSize: true,
        fluidColValues: ["entry", "translate"],
      });
      this.$nextTick(() => this.observeColumnWidth());
    });
  },
  /**
   * 路由离开术语库时立即停止全量选择分页收集。
   * @param {Object} to - 目标路由。
   * @param {Object} from - 当前路由。
   * @param {Function} next - 继续导航的回调。
   * @returns {void} 无返回值。
   */
  beforeRouteLeave(to, from, next) {
    this.stopSelectionCollection();
    next();
  },
  /**
   * 释放视口观察器并中止未完成的全量选择请求。
   * @returns {void} 无返回值。
   */
  beforeUnmount() {
    this.stopSelectionCollection();
    this.columnWidthObserver?.disconnect();
    this.columnWidthObserver = null;
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
    /**
     * 将列偏好重新投影到表格后，按当前视口重新分配自适应列宽。
     * @returns {void} 无返回值。
     */
    syncColumnsFromPref() {
      applyTableColumnsFromPref(this);
      this.$nextTick(() => this.recomputeAdaptiveColumnWidths());
    },
    /**
     * 观察表格视口宽度并在布局变化时重新计算自适应列宽。
     * @returns {void} 无返回值。
     */
    observeColumnWidth() {
      const viewport = this.$refs.glossaryTable?.$el;
      if (!viewport) return;

      if (typeof ResizeObserver === "undefined") {
        this.recomputeAdaptiveColumnWidths(viewport.clientWidth);
        return;
      }

      this.columnWidthObserver?.disconnect();
      const observer = new ResizeObserver((entries) => {
        if (this.columnWidthObserver !== observer) return;
        const width = entries[0]?.contentRect?.width ?? viewport.clientWidth;
        this.recomputeAdaptiveColumnWidths(width);
      });
      this.columnWidthObserver = markRaw(observer);
      observer.observe(viewport);
      this.recomputeAdaptiveColumnWidths(viewport.clientWidth);
    },
    /**
     * 按可见列定义、手动宽度和表格内容视口宽度更新列宽。
     * @param {number} [viewportWidth] 表格视口可用宽度。
     * @returns {void} 无返回值。
     */
    recomputeAdaptiveColumnWidths(viewportWidth) {
      const viewport = this.$refs.glossaryTable?.$el;
      const contentWidth = viewport?.querySelector?.(".ant-table-content")?.clientWidth;
      const availableWidth = contentWidth > 0
        ? contentWidth
        : viewportWidth ?? viewport?.clientWidth ?? 0;
      const settingsByValue = new Map(
        this.columnSettingsList.map((column) => [column.value, column])
      );
      const visibleColumns = this.columns.map((column) => {
        const settings = settingsByValue.get(column.colValue);
        return {
          colValue: column.colValue,
          width: settings?.width ?? column.width,
          minWidth: settings?.minWidth ?? column.minWidth,
        };
      });
      const resolvedWidths = this.adaptiveColumnWidthState.resolve(
        availableWidth,
        visibleColumns,
        ADAPTIVE_COLUMN_KEYS,
        SELECTION_COLUMN_WIDTH
      );
      const nextColumns = this.columns.map((column) => {
        const width = resolvedWidths[column.colValue];
        return width == null || width === column.width
          ? column
          : { ...column, width };
      });
      const tableWidth = nextColumns.reduce(
        (total, column) => total + (Number(column.width) || 0),
        SELECTION_COLUMN_WIDTH
      );

      if (nextColumns.some((column, index) => column !== this.columns[index])) {
        this.columns = nextColumns;
      }
      if (this.tableHeight.x !== tableWidth) {
        this.tableHeight = { x: tableWidth };
      }
    },
    // 初始化
    init() {
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
    /**
     * 提交新的术语库查询并清空旧查询范围选择。
     * @returns {Promise<Array>} 新查询结果。
     */
    getSearchClick() {
      this.pagination.current = 1;
      return this.getSearch();
    },
    /**
     * 查询当前术语库范围，并只提交最新请求的结果。
     * @returns {Promise<Array>} 当前页或本地完整结果。
     */
    async getSearch() {
      const option = this.search.searchType || "getSykEntry";
      const search = { ...this.search, searchType: option };
      const scope = JSON.stringify({ option, search });
      if (scope !== this.selectionScope) {
        this.stopSelectionCollection();
        this.selectionScope = scope;
      }
      const token = this.queryGuard.next();
      this.dataSource = [];
      // 选择模式比较原始查询条件；默认空 searchType 仍由 option 解析为 getSykEntry。
      this.lastSearch = { ...this.search };
      this.loading = true;
      try {
        const response = await this.requestSearchPage(
          option,
          this.pagination.current,
          this.pagination.pageSize,
          search
        );
        if (!this.queryGuard.isCurrent(token)) return [];
        const rows = option === "checkSykEntry"
          ? response?.data
          : response?.data?.list;
        this.dataSource = Array.isArray(rows) ? rows : [];
        this.pagination.total = option === "checkSykEntry"
          ? this.dataSource.length
          : Number(response?.data?.totalNum) || 0;
        refreshTableSelectionById(this, this.dataSource);
        this.selectEntry = this.selectedRows;
        this.syncSelectionState();
        await this.loadRelationInfo(this.dataSource, option, token);
        await waitForRender(this);
        return this.dataSource;
      } catch (error) {
        if (this.queryGuard.isCurrent(token)) {
          this.dataSource = [];
          this.pagination.total = 0;
          message.error("查询失败：" + (error?.message || error));
          await waitForRender(this);
        }
        return [];
      } finally {
        if (this.queryGuard.isCurrent(token)) this.loading = false;
      }
    },
    /**
     * 请求指定搜索类型的一页，不修改组件列表状态。
     * @param {string} option - 搜索类型。
     * @param {number} pageIndex - 页码。
     * @param {number} pageSize - 每页数量。
     * @param {Object} search - 当前查询条件快照。
     * @param {boolean} [trackSelectionRequest] - 是否登记为可批量取消的全量选择请求。
     * @returns {Promise<Object>} API 响应。
     */
    requestSearchPage(option, pageIndex, pageSize, search, trackSelectionRequest = false) {
      const api = SEARCH_APIS[option];
      if (!api) return Promise.resolve({ data: { list: [], totalNum: 0 } });
      const requestId = `glossary-${++this.requestSequence}`;
      if (trackSelectionRequest) this.activeSelectionRequestIds.push(requestId);
      const request = api({ pageIndex, pageSize, requestId }, search);
      if (!trackSelectionRequest) return request;
      return request.finally(() => {
        this.activeSelectionRequestIds = this.activeSelectionRequestIds.filter(
          (activeRequestId) => activeRequestId !== requestId
        );
      });
    },
    /**
     * 立即中止当前全量选择收集中的分页请求。
     * @returns {void} 无返回值。
     */
    cancelSelectionRequests() {
      const requestIds = this.activeSelectionRequestIds;
      this.activeSelectionRequestIds = [];
      requestIds.forEach((requestId) => cancelRequest(requestId));
    },
    /**
     * 使所有待处理的全量选择操作失效并中止当前分页请求。
     * @returns {void} 无返回值。
     */
    stopSelectionCollection() {
      this.selectionOperations.invalidate();
      this.cancelSelectionRequests();
    },
    /**
     * 为当前行加载关联信息，忽略查询已过期后的回调。
     * @param {Array<Object>} rows - 当前页行。
     * @param {string} option - 搜索类型。
     * @param {number} token - 列表查询版本。
     * @returns {Promise<Array>} 所有关联请求的 settled 结果。
     */
    loadRelationInfo(rows, option, token) {
      const requests = rows.map((row) => {
        if (option === "getSykNotUsed" || (option === "checkSykEntry" && row.notUsedByEntryInfo)) {
          row.relationCount = 0;
          row.reslations = [];
          return Promise.resolve();
        }
        const request = option === "checkSameEntry"
          ? getSameEntryRelation(row)
          : getSykEntryRelation([row]);
        return Promise.resolve(request).then((response) => {
          if (!this.queryGuard.isCurrent(token)) return;
          row.relationCount = Array.isArray(response?.data?.list)
            ? response.data.list.length
            : 0;
          row.reslations = response?.data?.list || [];
        }).catch(() => {
          if (!this.queryGuard.isCurrent(token)) return;
          row.relationCount = 0;
          row.reslations = [];
        });
      });
      return Promise.allSettled(requests);
    },
    /**
     * 同步已选行、行键和数量来源数组。
     * @returns {void} 无返回值。
     */
    syncSelectionState() {
      this.selectEntry = this.selectedEntryOperations.merge([], this.selectEntry);
      this.selectedRows = this.selectEntry;
      this.selectedRowKeys = this.selectEntry.map((row) => row.id);
    },
    /**
     * 清空已选项并使进行中的全量操作失效。
     * @returns {void} 无返回值。
     */
    clearSelectionState() {
      this.stopSelectionCollection();
      this.selectEntry = this.selectedEntryOperations.clear();
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.batchSelectVisible = false;
    },
    /**
     * 打开已选词条列表。
     * @returns {void} 无返回值。
     */
    viewSelectedEntries() {
      this.batchSelectVisible = true;
      setModalAriaHidden(this, document);
    },
    /**
     * 关闭已选词条列表。
     * @returns {void} 无返回值。
     */
    batchSelectClose() {
      this.batchSelectVisible = false;
    },
    /**
     * 同步已选弹窗返回的词条对象及行键。
     * @param {Array<Object>} rows - 弹窗变更后的已选词条。
     * @returns {void} 无返回值。
     */
    handleSelectedEntriesChange(rows) {
      this.selectEntry = this.selectedEntryOperations.merge([], rows);
      this.syncSelectionState();
    },
    /**
     * 应用单行选择并记录其操作顺序。
     * @param {Object} record - 当前行。
     * @param {boolean} selected - 最终选择状态。
     * @returns {void} 无返回值。
     */
    onSelect(record, selected) {
      this.selectionOperations.record(record.id, selected, this.selectionScope);
      this.selectEntry = this.selectedEntryOperations.apply(
        this.selectEntry,
        [record],
        selected
      );
      this.syncSelectionState();
    },
    /**
     * 异步选择或移除当前查询的全部结果页。
     * @param {boolean} selected - 是否选择当前查询结果。
     * @returns {Promise<void>} 选择请求完成后结束。
     */
    async onSelectAll(selected) {
      if (!this.selectionScope) return;
      if (this.selectionOperations.pendingCount > 0) {
        this.stopSelectionCollection();
      }
      const operation = this.selectionOperations.begin(this.selectionScope, selected);
      this.selectionSettlingCount += 1;
      const search = this.lastSearch && Object.keys(this.lastSearch).length
        ? { ...this.lastSearch }
        : { ...this.search, searchType: this.search.searchType || "getSykEntry" };
      const option = search.searchType || "getSykEntry";
      const shouldContinue = () => this.selectionOperations.isCurrent(
        operation,
        this.selectionScope
      );
      try {
        const rows = option === "checkSykEntry"
          ? this.dataSource
          : await collectAllPages(
              (pageIndex, pageSize) => this.requestSearchPage(
                option,
                pageIndex,
                pageSize,
                search,
                true
              ),
              100,
              shouldContinue
            );
        if (!shouldContinue()) return;
        this.selectEntry = this.selectionOperations.resolve(
          operation,
          rows,
          this.selectEntry,
          this.selectionScope
        );
        this.syncSelectionState();
      } catch (error) {
        const isCurrent = this.selectionOperations.isCurrent(
          operation,
          this.selectionScope
        );
        this.selectionOperations.reject(operation);
        if (isCurrent) message.error("全量选择失败：" + (error?.message || error));
      } finally {
        await waitForRender(this);
        this.selectionSettlingCount = Math.max(this.selectionSettlingCount - 1, 0);
      }
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
    formatCellText,
    translateStateLabel(value) {
      const map = {
        0: "未翻译",
        1: "待审核",
        2: "审核不通过",
        3: "已审核",
      };
      return map[value] ?? "未翻译";
    },
    // 回车编辑框
    editOK(record) {
      this.save(record.id);
      // record.translate = this.editableData[record.id].translate;
      // delete this.editableData[record.id];
    },
    handleTranslateChange(value, record, column) {
      onEditableCellInput(this, record.id, column.dataIndex, value);
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
    /**
     * 保存列拖拽宽度，并将其从后续的自动扩宽分配中固定下来。
     * @param {number} width 拖拽后的列宽。
     * @param {Object} column 被拖拽的表格列。
     * @returns {void} 无返回值。
     */
    handleResizeColumn(width, column) {
      handleResizeColumn(width, column);
      if (column?.colValue) {
        this.adaptiveColumnWidthState.setManualWidth(column.colValue, width);
      }
      this.recomputeAdaptiveColumnWidths();
    },
    // 设置表格每一行的 class
    getRowClassName(record, index) {
      return getRowClassName(record, index, this.selectedRowIndex); // 调用工具函数
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
.box {
  width: 100%;
  height: 100%;
  padding: 16px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  min-height: 0;
  // border: 1px solid red;
  /* 换行后每个表单项的间距 */
  & :deep(.search .form .ant-row) {
    margin-bottom: 8px !important;
  }
}

.box :deep(.search) {
  flex: 0 0 auto;
}

.glossary-search {
  flex: 0 0 auto;
}

.glossary-data-box {
  display: flex;
  flex: 1 1 0;
  height: 0 !important;
  min-height: 0;
  flex-direction: column;
  box-sizing: border-box;
  overflow: hidden;
}

.glossary-data-box :deep(.title) {
  flex: 0 0 32px;
}

.glossary-data-box :deep(.content) {
  display: flex;
  flex: 1 1 0;
  min-height: 0;
  flex-direction: column;
  box-sizing: border-box;
}

.glossary-data-box :deep(.operateBox) {
  flex: 0 0 auto;
}

.glossary-data-box :deep(.data) {
  display: flex;
  flex: 1 1 0;
  min-height: 0;
  overflow: hidden;
}

.glossary-data-box :deep(.data > .table-viewport) {
  flex: 1 1 0;
  height: 100%;
  min-height: 0;
}

.glossary-data-box :deep(.ant-table-selection-column) {
  box-sizing: border-box;
  width: 48px !important;
  min-width: 48px !important;
  max-width: 48px !important;
  padding-right: 4px !important;
  padding-left: 4px !important;
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
