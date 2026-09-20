<template>
  <!-- 批量编辑：独立于已选词条；预翻译结果写回同表 -->
  <CustomModal
    modalTitle="批量编辑"
    modalWidth="90%"
    :modalVisible="visible"
    :fullFlag="true"
    :showCancel="true"
    cancelText="取消"
    okText="保存"
    :okLoading="loading"
    @handleClose="onCancel"
    @handleOK="onSave"
    @afterClose="onAfterClose"
    @setTableHeight="setTableHeight"
  >
    <div style="width:100%;height:515px">
      <div class="table">
        <div class="toolbar">
          <a-input-search
            v-model:value="keyword"
            allow-clear
            placeholder="关键字搜索"
            style="width: 280px"
          />
          <div class="toolbar-right">
            <ColumnFilter
              v-model="checkedColumn"
              :columns="columnSettingsList"
              col-pref-name="colPref-batchEditModal"
              :normal-width="200"
              :need-filter="false"
              @change="syncColumnsFromPref"
            />
            <RulesDropdown :options="rulesOptions" @update:options="rulesOptions = $event" />
          </div>
        </div>
        <a-config-provider :locale="locale">
          <a-table
            class="ant-table-striped table-cell-overflow"
            :columns="columns"
            :data-source="filteredDataSource"
            :scroll="tableHeight"
            :pagination="pagination"
            :loading="busy"
            :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
            :customRow="customRow"
            @resizeColumn="handleResizeColumn"
            bordered
          >
            <template #bodyCell="{ column, record, text }">
              <template v-if="editableTextColumns.includes(column.dataIndex)">
                <TableCellTextArea
                  v-if="editableData[record.id]"
                  :value="editableData[record.id][column.dataIndex] ?? ''"
                  :error-message="cellErrors[record.id]?.[column.dataIndex]"
                  @update:value="(val) => onCellInput(val, record, column)"
                  @blur="() => onCellBlur(record, column)"
                />
                <CellOverflowTooltip v-else :content="text" />
              </template>
              <template v-else-if="column.dataIndex === 'operation'">
                <div class="editable-row-operations">
                  <template v-if="editableData[record.id]">
                    <CheckOutlined
                      class="row-confirm-icon"
                      style="color: #369fff; margin-left: 8px; cursor: pointer"
                      @click="rowConfirm(record)"
                      title="确认"
                    />
                    <CloseOutlined
                      class="row-discard-icon"
                      style="color: red; margin-left: 8px; cursor: pointer"
                      @click="rowDiscard(record)"
                      title="取消"
                    />
                  </template>
                  <template v-else>
                    <EditOutlined
                      class="row-edit-icon"
                      style="color:#369FFF;font-size:16px;cursor:pointer"
                      @click.stop="startEditRow(record)"
                      @dblclick.stop
                      title="编辑"
                    />
                    <DeleteOutlined
                      class="row-delete-icon"
                      style="color:#369FFF;font-size:16px;margin-left:8px;cursor:pointer"
                      @click.stop="removeFromEdit(record)"
                      @dblclick.stop
                      title="不编辑此词条"
                    />
                  </template>
                </div>
              </template>
              <template v-else-if="column.dataIndex && column.dataIndex !== 'index'">
                <CellOverflowTooltip :content="formatCellText(text)" />
              </template>
            </template>
          </a-table>
        </a-config-provider>
      </div>
    </div>
    <template v-slot:leftBottomBtn>
      <a-button type="primary" :disabled="busy || !dataSource.length" @click="openPreTranslateForm">
        预翻译
      </a-button>
    </template>
  </CustomModal>
  <PreTranslateForm
    :visible="preTranslateFormVisible"
    :loading="busy"
    @handleClose="preTranslateFormVisible = false"
    @submit="onPreTranslateSubmit"
  />
</template>

<script>
import CustomModal from "@/components/modal/index.vue";
import TableCellTextArea from "@/components/table/TableCellTextArea.vue";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip.vue";
import RulesDropdown from "@/components/Dropdown/rulesDropdown.vue";
import ColumnFilter from "@/components/ColumnFilter/ColumnFilter.vue";
import PreTranslateForm from "./PreTranslateForm.vue";
import { formatCellText } from "@/components/table/cellText";
import {
  CheckOutlined,
  CloseOutlined,
  EditOutlined,
  DeleteOutlined,
} from "@ant-design/icons-vue";
import zh_CN from "ant-design-vue/es/locale/zh_CN";
import { message } from "ant-design-vue";
import { cloneDeep } from "lodash-es";
import commonParam, { entryAllCols, entryPresets } from "@/constants/commonParam.js";
import {
  applyTable,
  syncColumnsFromPref as applyTableColumnsFromPref,
} from "@/components/ColumnFilter";
import { setModalAriaHidden } from "@/utils/domUtils";
import { handleResizeColumn } from "@/utils/tableUtils";
import { withLoading, isLoading } from "@/composables/useLoading";
import {
  applyCell,
  getMethods,
  onEditableCellInput,
  openSetEdit,
  cancelEdit,
  revalidateEditingRows,
} from "@/utils/validationUtils.js";
import { usePreTranslateEdit } from "@/views/entry/composables/usePreTranslateEdit.js";
import { confirmRow } from "@/views/entry/composables/useSaveFlow.js";

const { execute, cancelAll, save, discardRow } = usePreTranslateEdit();

/** 关键字匹配时跳过的非标量 / 内部字段 */
const KEYWORD_SKIP_KEYS = new Set(["children"]);

/**
 * 判断记录字段值是否包含关键字（大小写不敏感）。
 * @param {*} value
 * @param {string} kwLower
 * @returns {boolean}
 */
function valueMatchesKeyword(value, kwLower) {
  if (value == null || value === "") return false;
  if (typeof value === "object") return false;
  return String(value).toLowerCase().includes(kwLower);
}

/**
 * 记录（含可选编辑草稿）是否命中关键字。
 * @param {Object} record
 * @param {Object|undefined} editRow
 * @param {string} kwLower
 * @returns {boolean}
 */
function recordMatchesKeyword(record, editRow, kwLower) {
  for (const key of Object.keys(record || {})) {
    if (KEYWORD_SKIP_KEYS.has(key)) continue;
    if (valueMatchesKeyword(record[key], kwLower)) return true;
  }
  if (editRow) {
    for (const key of Object.keys(editRow)) {
      if (KEYWORD_SKIP_KEYS.has(key) || key === "id") continue;
      if (valueMatchesKeyword(editRow[key], kwLower)) return true;
    }
  }
  return false;
}

export default {
  name: "BatchEditModal",
  components: {
    CustomModal,
    TableCellTextArea,
    CellOverflowTooltip,
    RulesDropdown,
    ColumnFilter,
    PreTranslateForm,
    CheckOutlined,
    CloseOutlined,
    EditOutlined,
    DeleteOutlined,
  },
  emits: ["update:visible", "handleClose", "saved", "refresh"],
  props: {
    visible: { type: Boolean, default: false },
    /** 已选词条快照，打开时克隆为工作表 */
    entries: { type: Array, default: () => [] },
    classifyLimit: { type: Object, default: () => ({}) },
    /** 祖先词条列表勾选的校验规则；打开时克隆为会话副本 */
    rulesOptionsProp: { type: Array, default: null },
  },
  data() {
    return {
      locale: zh_CN,
      tableHeight: { x: "max-content", y: 395 },
      columns: [],
      columnSettingsList: [],
      checkedColumn: [],
      pagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
      },
      dataSource: [],
      editableData: {},
      rules: {},
      cellErrors: {},
      preTranslateSnapshot: null,
      reviewActive: false,
      _pendingOk: null,
      fieldsNeedSave: ["entry", ...commonParam.langValList],
      rulesOptions: commonParam.rulesOptions.map((item) => ({ ...item })),
      busy: false,
      preTranslateFormVisible: false,
      keyword: "",
    };
  },
  computed: {
    editableTextColumns() {
      return commonParam.langValList;
    },
    loading() {
      return this.busy;
    },
    /**
     * 关键字过滤后的展示数据；保存/预翻译仍用完整 dataSource。
     * @returns {Object[]}
     */
    filteredDataSource() {
      const kw = (this.keyword || "").trim().toLowerCase();
      if (!kw) return this.dataSource;
      return (this.dataSource || []).filter((record) =>
        recordMatchesKeyword(record, this.editableData?.[record.id], kw)
      );
    },
  },
  watch: {
    visible: {
      immediate: true,
      handler(open) {
        if (open) {
          setModalAriaHidden(this, document);
          this.bootstrap();
        }
      },
    },
    rulesOptions: {
      deep: true,
      async handler() {
        await revalidateEditingRows(this);
      },
    },
  },
  methods: {
    formatCellText,
    handleResizeColumn,
    bootstrap() {
      this.dataSource = cloneDeep(this.entries || []);
      this.editableData = {};
      this.rules = {};
      this.cellErrors = {};
      this.preTranslateSnapshot = cloneDeep(this.dataSource);
      this.reviewActive = false;
      this._pendingOk = null;
      this.preTranslateFormVisible = false;
      this.keyword = "";
      if (Array.isArray(this.rulesOptionsProp) && this.rulesOptionsProp.length) {
        this.rulesOptions = this.rulesOptionsProp.map((item) => ({ ...item }));
      } else {
        this.rulesOptions = commonParam.rulesOptions.map((item) => ({
          ...item,
        }));
      }
      applyTable(this, {
        allCols: entryAllCols,
        preset: entryPresets.createVersion,
        ctx: { pagination: this.pagination },
        colPrefName: "colPref-batchEditModal",
        normalWidth: 200,
        needFilter: false,
        lockCellSize: true,
      });
    },
    syncColumnsFromPref() {
      applyTableColumnsFromPref(this);
    },
    openPreTranslateForm() {
      if (!this.dataSource.length) {
        message.warn("没有可编辑词条，无法预翻译！");
        return;
      }
      this.preTranslateFormVisible = true;
    },
    async onPreTranslateSubmit(config) {
      this.preTranslateFormVisible = false;
      if (!this.dataSource.length) {
        message.warn("没有可编辑词条，无法预翻译！");
        return;
      }
      this.busy = true;
      try {
        await withLoading(async () => {
          await execute(this, {
            ...config,
            verifyMethods: getMethods(this),
          });
        });
      } catch (err) {
        message.error("预翻译失败！", err?.message);
      } finally {
        this.busy = false;
      }
    },
    async onCellInput(value, record, column) {
      onEditableCellInput(this, record.id, column.dataIndex, value);
      // change 即校验，不必等失焦或行内 √ 才发现错误
      if (!this.editableData[record.id]) return;
      await applyCell(this, record.id, column.dataIndex);
    },
    async onCellBlur(record, column) {
      if (!this.editableData[record.id]) return;
      await applyCell(this, record.id, column.dataIndex);
    },
    async startEditRow(record) {
      if (this.editableData[record.id]) return;
      await openSetEdit(record, this.editableTextColumns, this);
    },
    /**
     * 判断双击目标是否属于交互控件。
     * @param {EventTarget} target
     * @returns {boolean}
     */
    isInteractiveRowTarget(target) {
      if (!target || typeof target.closest !== "function") return false;
      return !!target.closest(
        "button, input, textarea, select, a, .ant-btn, .ant-select, .ant-checkbox-wrapper, .ant-input, .editable-row-operations"
      );
    },
    /**
     * 双击行进入编辑态。
     * @param {Object} record
     * @param {MouseEvent} event
     * @returns {Promise<void>}
     */
    async handleRowDblclick(record, event) {
      if (this.busy) return;
      if (this.editableData[record.id]) return;
      if (this.isInteractiveRowTarget(event?.target)) return;
      await this.startEditRow(record);
    },
    /**
     * @param {Object} record
     * @returns {Object}
     */
    customRow(record) {
      return {
        onDblclick: (event) => this.handleRowDblclick(record, event),
      };
    },
    rowConfirm(record) {
      confirmRow(this, record);
    },
    rowDiscard(record) {
      discardRow(this, record);
    },
    /**
     * 从批量编辑工作表移除一行（不影响已选词条）。
     * @param {Object} record
     */
    removeFromEdit(record) {
      const id = record.id;
      this.dataSource = this.dataSource.filter((r) => r.id !== id);
      cancelEdit(this, id);
      if (this.cellErrors?.[id]) delete this.cellErrors[id];
      if (this.preTranslateSnapshot) {
        this.preTranslateSnapshot = this.preTranslateSnapshot.filter(
          (r) => r.id !== id
        );
      }
    },
    onCancel() {
      cancelAll(this);
      this.preTranslateFormVisible = false;
      this.$emit("update:visible", false);
      this.$emit("handleClose");
    },
    async onSave() {
      if (isLoading()) return;
      this.busy = true;
      try {
        const result = await withLoading(async () => save(this));
        if (!result.allPassed) return;
        this.$emit("saved", {
          savedRecords: result.savedRecords,
          remainingIds: (result.remaining || []).map((r) => r.id),
          allDone: result.remainingCount === 0 && result.failedCount === 0,
        });
        this.$emit("refresh");
        if (result.remainingCount === 0 && result.failedCount === 0) {
          this.$emit("update:visible", false);
          this.$emit("handleClose");
        }
      } finally {
        this.busy = false;
      }
    },
    onAfterClose() {
      cancelAll(this);
      this.dataSource = [];
      this.keyword = "";
      this.preTranslateFormVisible = false;
    },
    setTableHeight(height, type) {
      if (type === "full") this.tableHeight.y = height - 150;
      else if (type === "reduce") this.tableHeight.y = 395;
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
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.editable-row-operations {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
