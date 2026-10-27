<template>
  <PipeShell
    :visible="visible"
    :modalTitle="modalTitle"
    :okLoading="loading"
    okText="保存"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
    @setTableHeight="setTableHeight"
  >
    <div class="content">
      <!-- 工具栏壳：校验规则 + 查询/批量审核 + 展示列/CoverButton + 过滤语种 -->
      <PipelinePanel
        :task="task"
        :table-host="this"
        :columns="columns"
        :data-source="dataSource"
        :loading="loading || queryLoading"
        :row-selection="examineRowSelection"
        :pagination="pagination"
        :scroll="tableHeight"
        :custom-row="doubleClick"
        :edit="examineEditCols"
        :editable-data="editableData"
        :cell-errors="cellErrors"
        @columns-change="syncColumnsFromPref"
        @cell-input="onCellInput"
        @save-edit="edit"
        @cancel-edit="cancel"
        @audit-pass="passTagChange"
        @audit-reject="rejectTagChange"
        ref="workTable"
      >
        <template #taskExtra>
          <RulesDropdown :options="rulesOptions" @update:options="rulesOptions"></RulesDropdown>
        </template>
        词条：
        <a-textarea v-model:value="keyWords" style="width:300px" size="small" placeholder='请输入词条搜索' :auto-size="{ minRows: 1 }" />
        <span style="margin-left:10px">词条状态：</span>
        <EntryStateSelect :entryState="entryState" @update:entryState="entryState = $event" :size="'small'" :style="'width: 300px'"
          :filter="new Set(['0'])" />
        <a-button type="primary" size="small" style="margin-left:8px" @click="getTaskEntry">查询</a-button>
        <AuditButtons @pass="pass" @reject="reject" />
        <a-button type="primary" size="small" danger style="margin-left:8px" @click="deleteTaskEntry">删除</a-button>
        <template #columnActions>
          <!-- 展示列 + 释义覆盖（工具栏按钮，非表头放大镜） -->
          <ColumnActions
            v-model="checkedColumn"
            :columns="columnSettingsList"
            :overlay-style="overlayStyle"
            col-pref-name="colPref-examineModal"
            :normal-width="100"
            show-cover-button
            :cover-button-props="{
              translate: task.translateType,
              dataSource,
              oldEditableData: editableData,
            }"
            @update:old-editable-data="editableData = $event"
            @show-edit-operation="showEditOperation"
            @change="syncColumnsFromPref"
          />
        </template>
        <template #subToolbar>
          <!-- 过滤语种（工具栏下方第二行） -->
          <div class="select">
            <LanguageFilter
              v-model="filterLanguage"
              @change="filterLanguageChange"
            />
          </div>
        </template>
      </PipelinePanel>
    </div>
    <template #leftBottomBtn>
      <a-button type="primary" size="small" style="margin-left:8px;float:left" class="resetBtn" @click="aggregation">聚合</a-button>
      <a-button type="primary" size="small" style="margin-left:8px;float:left" class="yellowBtn" @click="cancelAggregation">取消聚合</a-button>
    </template>
  </PipeShell>
</template>
<script>
import "@/assets/style/common.less";
import PipeShell from "@/views/workbench/components/PipeShell.vue";
import RulesDropdown from "@/components/Dropdown/rulesDropdown.vue";
import EntryStateSelect from "@/components/select/entryStateSelect.vue";
import IsExistBadge from "@/components/stateBadge/isExistBadge.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import InputIME from "@/components/cellEditor/input_IME.vue";
import TableCellTextArea from "@/components/table/TableCellTextArea/TableCellTextArea.vue";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip/CellOverflowTooltip.vue";
import { formatEntryText, formatCellText, formatMaxLengthText } from "@/components/table/cellText";
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
  SettingOutlined,
  InfoCircleOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import commonParam, { workbenchParams } from "@/constants/commonParam.js";
import { applyTable, syncColumnsFromPref as applyTableColumnsFromPref } from "@/components/ColumnFilter";
import { filterWbColsForCtx } from "@/components/ColumnFilter/columnBuilder.js";
import { wbAllCols, wbPresets } from "@/constants/commonParam.js";
import {
  PipelinePanel,
  ColumnActions,
  LanguageFilter,
  AuditTags,
  AuditButtons,
} from "@/views/workbench/components";
import { filterLanguageChange as applyLanguageFilter } from "@/composables/workbench/useLanguageFilter";
import { defaultPagination, pageChange as wbPageChange } from "@/views/workbench/composables/page";
import {
  handleFilterSearch,
  resetOnClose,
} from "@/views/workbench/composables/filterClear";
import { editTextCols } from "@/views/workbench/utils/editCols";
import {
  getRowClassName,
  handleReset as tableHandleReset,
} from "@/utils/tableUtils";
import {
  applyTableSelectionById,
  clearTableSelection,
  clearTableRowsContext,
  resetTableQuerySession,
  flattenRowsById,
  getCurrentSelectionRows,
  refreshTableSelectionById,
  updateTableSelectionById,
} from "@/utils/selectionUtils";
import { createQueryVersionGuard } from "@/utils/queryVersionGuard";
import { interpretation2value } from "@/utils/translationUtils";
import { partitionSubmittedRows } from "@/utils/batchUpdateResults.js";
import {
  onEditableCellInput,
  clearCellErrorsForRecords,
  openSetEdit,
  getMethods,
  classifyArr,
  openFailRows,
  revalidateLoaded,
  saveEdit,
  cancelEdit,
  // as 别名：避免 methods 里同名递归
  showEditOperation as showEditOp,
  hideEditOperation as hideEditOp,
} from "@/utils/validationUtils";
import { loading, startLoading, endLoading, resetLoading } from "@/composables/useLoading";
import { defineComponent, ref, createVNode } from "vue";
export default {
  // 全局单一 loading 状态（hook 导出，响应式）：表格遮罩/保存按钮统一绑定
  setup() {
    return { loading };
  },
  components: {
    CheckOutlined,
    CloseOutlined,
    SettingOutlined,
    InfoCircleOutlined,
    PipeShell,
    RulesDropdown,
    EntryStateSelect,
    IsExistBadge,
    EntryStateBadge,
    TransStateBadge,
    InputIME,
    TableCellTextArea,
    CellOverflowTooltip,
    PipelinePanel,
    ColumnActions,
    LanguageFilter,
    AuditTags,
    AuditButtons,
  },
  emits: ["handleClose", "handleOK", "afterSave"],
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
      // modal width 由 PipeShell 默认 90%
      task: {},
      keyWords: "",
      // tableHeight: { x: "100%", y: "415px" },
      tableHeight: { x: "max-content", y: "415px" },
      columns: [],
      dataSource: [],
      allData: [],
      selectedRowKeys: [],
      selectedRows: [],
      editableData: {},
      pagination: defaultPagination(this.pageChange),
      rules: {},
      cellErrors: {},
      entryState: "1",
      selectedRowIndex: null,
      timer: null,
      rulesOptions: commonParam.rulesOptions,
      overlayStyle: workbenchParams.overlayStyle, // 展示列样式
      columnSettingsList: [],
      checkboxList: [], // 展示列可选的值
      checkedColumn: [], // 展示列已选的值
      editList_needValidate: null, // 可编辑且需要表单校验的list(工作台只有任务的翻译语种可编辑,并且需要进行表单校验)
      editList: null, // 可编辑的list
      translateStateList: [
        ...commonParam.langTranslateStateList,
        "translateState",
      ],
      state: {
        searchText: "",
        searchedColumn: "",
      },
      filters: null,
      // 历史字段 filteredData：曾由 handleTableChange 写入，仓库内无读取方；批量勾选走 this.filters + selectionUtils（AND）。
      antClearFilter: null,
      queryGuard: createQueryVersionGuard(),
      queryScope: "",
      queryLoading: false,
      filterLanguage: null,
      filterSource: [],
    };
  },
  created() {},
  mounted() {  },
  computed: {
    examineEditCols() {
      return [...this.editableTextAreaColumns, "diFileName", "tag", "maxLength"];
    },
    editableTextAreaColumns() {
      return editTextCols(this, { withEditList: true });
    },
    examineRowSelection() {
      return {
        columnWidth: 48,
        selectedRowKeys: this.selectedRowKeys,
        onSelect: this.onSelect,
        onSelectAll: this.onSelectAll,
        checkStrictly: false,
      };
    },
  },
  watch: {
    /**
     * 切换审核任务时使旧列表失效，并在弹窗打开时加载新任务。
     * @param {Object} newTask - 当前任务。
     * @param {Object} oldTask - 上一个任务。
     * @returns {void} 无返回值。
     */
    currentTask(newval, oldval) {
      this.task = newval;
      this.task.transMap = commonParam.languageMap[this.task.translateType];
      if (newval?.id !== oldval?.id) {
        // 任务切换时清空审核上下文，防止上一任务的勾选和请求结果混入当前列表。
        resetTableQuerySession(this);
        if (this.visible) this.getTaskEntry();
      }
    },
    rulesOptions: {
      deep: true,
      async handler() {
        const transCol = this.task?.transMap?.value;
        if (!transCol) return;
        await revalidateLoaded(this, transCol);
      },
    },
    visible: {
      /**
       * 响应审核弹窗开关并使关闭前的列表请求失效。
       * @param {boolean} newValue - 弹窗当前是否打开。
       * @returns {Promise<void>} 可见状态处理完成后结束。
       */
      async handler(newVal) {
        // console.log("打开工作台-词条审核", newVal);
        if (newVal) {
          this.$nextTick(() => {
            // 1.设置翻译列展示的语种
            // 设置翻译列可编辑&可校验
            this.editList_needValidate = [this.task.transMap.value];
            // 设置对应的翻译释义列可编辑
            this.editList = [
              this.task.transMap.interpretation,
              "auditSuggess",
              "comment",
            ];
            applyTable(this, {
              allCols: wbAllCols,
              preset: wbPresets.examineModal,
              ctx: {
                task: this.task,
                transMap: this.task.transMap,
                pagination: this.pagination,
              },
              colPrefName: "colPref-examineModal",
              normalWidth: 100,
              needFilter: true,
              filterCols: filterWbColsForCtx,
            });
          });
        } else {
          // 关闭弹窗后立即废弃在途请求，避免隐藏弹窗被迟到响应重绘。
          resetTableQuerySession(this);
        }
      },
    },
  },
  methods: {
    /**
     * 应用展示列偏好并通知共享表格重新建立列布局。
     * @returns {void} 无返回值。
     */
    syncColumnsFromPref() {
      applyTableColumnsFromPref(this);
      this.columns = [...this.columns];
    },
    // 释义覆盖翻译
    interpretation2value() {
      interpretation2value(this);
      // const verifyMethods = this.rulesOptions
      //   .filter((option) => option.checked)
      //   .map((option) => option.key);
      // interpretation2value_(this, this.task.transMap, verifyMethods);
    },
    // 单元格输入更新：集中处理 editableData 写入，防止 IME 组合期间给 undefined 赋值
    onCellInput(value, record, column) {
      onEditableCellInput(this, record.id, column.dataIndex, value);
    },
    formatEntryText,
    formatCellText,
    formatMaxLengthText,
    /**
     * 查询当前任务待审核词条，只提交最新任务与筛选条件的响应。
     * @param {{ notifyOnError?: boolean }} [options={}] - 查询失败时是否显示错误提示。
     * @returns {Promise<'success'|'error'|'stale'>} 查询结果状态。
     */
    async getTaskEntry({ notifyOnError = true } = {}) {
      let params = {
        taskID: this.task.id,
        entryState: this.entryState,
        entry: this.keyWords,
      };
      const scope = JSON.stringify({ params, taskId: this.task.id });
      if (scope !== this.queryScope) {
        // 关键词或状态变化时仅清空列表和编辑上下文，保留跨查询的已选词条参与审核。
        this.queryScope = scope;
        clearTableRowsContext(this);
      }
      // 将本次查询登记为最新版本，列表只接受最后一次用户操作的返回。
      const token = this.queryGuard.next();
      const taskId = this.task.id;
      this.queryLoading = true;
      try {
        const res = await getEntryInfoList(params, []);
        // 任务切换或再次查询期间返回的旧数据不得更新审核 UI。
        if (!this.queryGuard.isCurrent(token) || this.task.id !== taskId) return "stale";
        // 更新成功：刷新所有任务的小红点
        this.$emit("afterSave", this.currentTask);

        this.dataSource = res.data.list;
        // 排序  将已存在的词条放到前面
        this.dataSource.sort(function (a, b) {
          return b.isExist - a.isExist;
        });
        this.dataSource.forEach((item) => {
          item.auditState = -1;

          // 装置部：行上展示模块上限（词条表不落这些字段）
          item.maxByte = this.classifyLimit[item.classfy1]?.["maxByte"];
          item.foreignMaxByte =
            this.classifyLimit[item.classfy1]?.["foreignMaxByte"];
        });
        this.allData = this.dataSource;
        // 新列表对象替换后按 ID 恢复仍可见行的选择，保持批量操作目标准确。
        refreshTableSelectionById(this, this.dataSource);
        await revalidateLoaded(
          this,
          this.task.transMap.value,
          () => this.queryGuard.isCurrent(token) && this.task.id === taskId,
        );
        return this.queryGuard.isCurrent(token) && this.task.id === taskId
          ? "success"
          : "stale";
      } catch (err) {
        if (this.queryGuard.isCurrent(token) && notifyOnError) {
          message.error("查询失败！", err?.message || String(err));
        }
        return this.queryGuard.isCurrent(token) ? "error" : "stale";
      } finally {
        // 防止先发请求结束时提前收起后发请求的加载提示。
        if (this.queryGuard.isCurrent(token)) this.queryLoading = false;
      }
    },
    /**
     * 校验并提交审核状态，保存后以服务端查询结果刷新列表。
     * @returns {Promise<void>} 校验、保存及列表刷新完成。
     */
    async handleOK() {
      startLoading();
      try {
        const taskId = this.task.id;
        const saveQueryVersion = this.queryGuard.current();
        const transCol = this.task.transMap.value;
        const methods = getMethods(this);
        const candidates = this.dataSource.filter(
          (item) => item.auditState === 1 || item.auditState === 0,
        );
        const candidateIds = new Set(candidates.map((row) => String(row.id)));
        const localOnlyRows = Object.entries(this.editableData)
          .filter(([id]) => !candidateIds.has(String(id)))
          .map(([id, row]) => ({ id, row }));
        if (candidates.length > 0) {
          // 先在前端标出校验失败单元格；失败时不发请求，用户可直接在当前行修改。
          clearCellErrorsForRecords(this, candidates.map((row) => row.id));
          const verifyResult = await classifyArr(this, candidates, transCol, methods);
          if (verifyResult.errorIds.size > 0) {
            await openFailRows(this, candidates, verifyResult, transCol);
            message.warn(`校验不通过 ${verifyResult.errorIds.size} 条，请检查翻译列红字`);
            return;
          }
        }

        const updateRows = [];
        // 仅把待通过/驳回的行组装为接口快照，避免将未操作行一并写回。
        const sourceById = new Map(
          flattenRowsById(this.dataSource).map((row) => [row.id, row]),
        );
        for (const sourceRow of candidates) {
          const row = cloneDeep(sourceById.get(sourceRow.id) || sourceRow);
          if (this.editableData[row.id]) {
            Object.assign(row, cloneDeep(this.editableData[row.id]));
          }
          if (row[transCol] != null) row[this.task.transMap.state] = "1";
          row.entryState = row.auditState === 1 ? 3 : 2;
          updateRows.push(row);
        }
        if (updateRows.length === 0) {
          this.applyLocalOnlyEdits(localOnlyRows);
          return;
        }

        const response = await updateEntryList(
          { taskID: taskId },
          updateRows,
        );
        // 任务切换后仅让旧请求落库，不再更新新任务的前端状态。
        if (this.task.id !== taskId || this.queryGuard.current() !== saveQueryVersion) return;
        // 接口可能部分成功：后续 UI 更新必须按成功、失败 ID 分开处理。
        const result = partitionSubmittedRows(updateRows, response);
        const successIds = new Set(
          flattenRowsById(result.successRows).map((row) => row.id),
        );
        const okCount = result.successRows.filter((row) => row.auditState === 1).length;
        const rejectCount = result.successCount - okCount;
        const messageParts = [];
        if (result.successCount > 0) {
          const successText = [`保存成功${result.successCount}条`];
          if (okCount > 0) successText.push(`通过${okCount}条`);
          if (rejectCount > 0) successText.push(`驳回${rejectCount}条`);
          messageParts.push(successText.join("———"));
        }
        if (result.failedCount > 0) {
          messageParts.push(`保存失败${result.failedCount}条`);
        }

        if (successIds.size > 0) {
          // 刷新失败时也不能让已确认成功的行再次进入保存范围。
          this.dataSource = this.dataSource.filter((row) => !successIds.has(row.id));
          this.allData = this.dataSource;
          this.selectedRows = this.selectedRows.filter((row) => !successIds.has(row.id));
          this.selectedRowKeys = this.selectedRowKeys.filter((id) => !successIds.has(id));
        }
        // 写接口只提供成功、失败归属；清掉本次草稿后重新查询持久态。
        for (const row of updateRows) delete this.editableData[row.id];
        const refreshed = await this.getTaskEntry({ notifyOnError: false });
        if (this.task.id !== taskId || refreshed === "stale") return;
        if (refreshed === "error") {
          // 查询失败时不展示本次提交的本地快照，等待用户重新查询服务端状态。
          const submittedIds = new Set(updateRows.map((row) => row.id));
          this.dataSource = this.dataSource.filter((row) => !submittedIds.has(row.id));
          this.allData = this.dataSource;
          this.selectedRows = this.selectedRows.filter((row) => !submittedIds.has(row.id));
          this.selectedRowKeys = this.selectedRowKeys.filter((id) => !submittedIds.has(id));
          this.selectedRowIndex = null;
          message.warning(`保存结果已返回，${messageParts.join("，")}，列表刷新失败，请重新查询`);
          return;
        }
        // 非本次提交的局部编辑不应被批量审核查询覆盖。
        this.applyLocalOnlyEdits(localOnlyRows);
        // 表格重绘可能暂时只保留行键；渲染完成后用最新服务端行恢复选择对象。
        await this.$nextTick();
        refreshTableSelectionById(this, this.dataSource);
        this.selectedRowIndex = null;
        if (messageParts.length > 0) {
          const text = `数据已保存！${messageParts.join("，")}`;
          if (result.failedCount > 0) message.warning(text);
          else message.success(text);
        }

        if (this.dataSource.length === 0) {
          this.handleClose();
        }
      } catch (err) {
        message.error("保存失败！", err?.message || String(err));
      } finally {
        endLoading();
      }
    },
    handleClose() {
      this.queryGuard.invalidate();
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.selectedRowIndex = null;
      this.$emit("handleClose");
    },
    getRowClassName(record, index) {
      return getRowClassName(record, index, this.selectedRowIndex);
    },
    /**
     * 应用本地搜索条件并使旧列表请求和选择失效。
     * @returns {void} 无返回值。
     */
    select() {
      this.queryGuard.invalidate();
      clearTableSelection(this);
      // this.dataSource = this.allData.filter((item) =>
      //   item.entry.includes(this.keyWords)
      // );
      if (this.filterLanguage === null) {
        this.dataSource = this.allData.filter((item) =>
          item.entry.includes(this.keyWords)
        );
      } else {
        this.dataSource = this.filterSource.filter((item) =>
          item.entry.includes(this.keyWords)
        );
      }
      this.pagination.current = 1;
      this.pagination.total = this.dataSource.length;
    },
    /**
     * 同步审核表格的单行操作，同时保留完整结果范围外的选择。
     * @param {Object} record - 本次点击的行。
     * @param {boolean} selected - 点击后的行选择状态。
     * @returns {void} 无返回值。
     */
    onSelect(record, selected) {
      updateTableSelectionById(this, [record], selected);
    },
    /**
     * 将表头复选框作用于当前完整查询结果。
     * @param {boolean} selected - 是否选中当前结果。
     * @returns {void} 无返回值。
     */
    onSelectAll(selected) {
      applyTableSelectionById(
        this,
        getCurrentSelectionRows(this),
        selected
      );
    },
    /**
     * 将未进入审核流转的本地编辑同步回表格和已选行。
     * @param {Array<{id: string|number, row: Object}>} localOnlyRows - 仅本地保存的编辑行。
     * @returns {void} 无返回值。
     */
    applyLocalOnlyEdits(localOnlyRows) {
      if (!localOnlyRows.length) return;
      const rowsById = new Map(
        localOnlyRows.map(({ id, row }) => [String(id), row]),
      );
      const applyToRows = (rows) => rows.map((row) => {
        const edit = rowsById.get(String(row.id));
        return edit ? { ...row, ...cloneDeep(edit) } : row;
      });
      this.dataSource = applyToRows(this.dataSource);
      this.allData = this.dataSource;
      this.selectedRows = applyToRows(this.selectedRows);
      localOnlyRows.forEach(({ id }) => delete this.editableData[id]);
    },
    // 通过标签点击事件
    passTagChange(record) {
      // 判断是否为子节点，若是则阻止操作
      if (record.parentID) {
        return;
      }
      let state = 1; // 审核通过
      if (record.auditState === 1) {
        // 取消选择
        state = -1;
      }
      record.auditState = state;
      if (this.editableData[record.id]) {
        // 同样要修改编辑态的审核状态
        this.editableData[record.id].auditState = state;
      }
      if (record.children && record.children.length > 0) {
        record.children.forEach((child) => {
          child.auditState = record.auditState;
          if (this.editableData[child.id]) {
            this.editableData[child.id].auditState = record.auditState;
          }
        });
      }
    },
    // 驳回标签点击事件
    rejectTagChange(record) {
      // 判断是否为子节点，若是则阻止操作
      if (record.parentID) {
        return;
      }
      let state = 0; // 审核不通过
      if (record.auditState === 1) {
        // 取消选择
        state = -1;
      }
      record.auditState = state;
      if (this.editableData[record.id]) {
        // 同样要修改编辑态的审核状态
        this.editableData[record.id].auditState = state;
      }
      if (record.children && record.children.length > 0) {
        record.children.forEach((child) => {
          child.auditState = record.auditState;
          if (this.editableData[child.id]) {
            this.editableData[child.id].auditState = record.auditState;
          }
        });
      }
    },
    /**
     * 批量设置选中行审核状态并清空多选。
     * @param {0|1} state 0 驳回，1 通过
     */
    applyAudit(state) {
      this.selectedRows.forEach((item) => {
        item.auditState = state;
        if (item.children && item.children.length > 0) {
          item.children.forEach((child) => {
            child.auditState = item.auditState;
          });
        }
      });
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    pass() {
      this.applyAudit(1);
    },
    reject() {
      if (this.selectedRows.length === 0) return;
      this.applyAudit(0);
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
        onDblclick: async (event) => {
          // clearTimeout(this.timer)
          // 判断是否为子节点，若是则阻止操作
          if (record.parentID) {
            return;
          }
          if (this.editableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }
          await openSetEdit(record, [this.task.transMap.value], this);
          this.showEditOperation(); // 显示编辑操作列；双击不跑 applyCell
        },
      };
    },
    // 行内 ✓ / 编辑框回车：公共 saveEdit；本页回写全部字段并置翻译状态为待审核
    async edit(record) {
      const transCol = this.task.transMap.value;
      await saveEdit(this, record, {
        transCol,
        commit: (rec, row) => {
          for (const [key, value] of Object.entries(row)) {
            if (rec.hasOwnProperty(key)) {
              rec[key] = value;
            }
          }
          if (rec[transCol] != null) {
            rec[this.task.transMap.state] = "1";
          }
        },
      });
    },
    // 取消编辑
    cancel(record) {
      cancelEdit(this, record.id);
    },
    // 显示编辑操作列
    showEditOperation() {
      showEditOp(this);
    },
    // 隐藏编辑操作列
    hideEditOperation() {
      hideEditOp(this);
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

      this.allData = this.dataSource;
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
      this.allData = this.dataSource;
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    afterClose() {
      resetLoading();
      this.editableData = {};
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.keyWords = "";
      this.pagination.current = 1;
      this.pagination.pageSize = 20;

      // 关弹窗：Ant 原生清列头筛选 + 搜索态复位
      resetOnClose(this);
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
          let delCount = {
            num: 0,
            childNum: 0,
          };
          this.selectedRows.forEach((item) => {
            deleteIds.push(item.id);
            delCount.num++;
            if (item.children && item.children.length > 0) {
              delCount.childNum += item.children.length;
              item.children.forEach((child) => {
                deleteIds.push(child.id);
              });
            }
          });
          this.selectedRowKeys = [];
          this.selectedRows = [];
          deleteEntryInfoByTaskID({ taskID: this.task.id }, deleteIds)
            .then((res) => {
              let text = `删除成功${delCount.num - delCount.childNum}条`;
              if (delCount.childNum > 0) {
                text += `(聚合${delCount.childNum}条)`;
              }
              message.success(text); // 最后一列的词条状态
              this.getTaskEntry();
            })
            .catch((err) => {
              message.error("删除失败！", err.message);
            });
        },
        onCancel: () => {},
      });
    },
    // 列头自定义筛选 + 保存 Ant clearFilters
    handleSearch(selectedKeys, confirm, dataIndex, clearFilters) {
      handleFilterSearch(
        selectedKeys,
        confirm,
        dataIndex,
        clearFilters,
        this
      );
    },
    // 列筛选重置
    handleReset(clearFilters) {
      tableHandleReset(clearFilters, this);
    },
    // 动态设置表格高度
    setTableHeight(height, type) {
      if (type === "full") {
        this.tableHeight.y = height - 230;
      } else if (type === "reduce") {
        this.tableHeight.y = 415;
      }
    },
    // 分页 + 当前页校验（见 composables/page）
    pageChange(page, pageSize) {
      wbPageChange(this, page, pageSize, () => this.task.transMap.value);
    },
    // 语种切换
    filterLanguageChange() {
      applyLanguageFilter(this);
    },
    // 同步 filters / filteredValue（供全部选择）
    /**
     * 应用审核表格分页及筛选变化。
     * @param {Object} pagination - 表格分页状态。
     * @param {Object} filters - 表格列筛选值。
     * @returns {void} 无返回值。
     */
    handleTableChange(pagination, filters) {
      this.filters = filters;
      for (let key in filters) {
        this.columns.forEach((col) => {
          if (col.dataIndex === key) {
            col.filteredValue = filters[key];
          }
        });
      }
      // 以下 filteredData 计算为历史遗留（examine 交集）；均未被消费。保留便于对照；确认无回归后可删除。
      // let isExistData = this.dataSource.filter((item) => {
      //   return filters.isExist && filters.isExist.includes(item.isExist);
      // });
      // let sourceData = this.dataSource.filter((item) => {
      //   return (
      //     filters.entrySource && item.entrySource.includes(filters.entrySource)
      //   );
      // });
      // this.filteredData = this.intersection(isExistData, sourceData);
    },
    // 历史 helper：曾为 filteredData 做集合合并；现无调用方。
    // intersection(nums1, nums2) {
    //   if (nums1.length === 0) {
    //     return nums2;
    //   }
    //   if (nums2.length === 0) {
    //     return nums1;
    //   }
    //   let a = new Set(nums1);
    //   let b = new Set(nums2);
    //   let arr = Array.from(new Set([...b].filter((x) => a.has(x))));
    //   return arr;
    // },
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
}
:deep(.ant-pagination) {
  margin: 8px 0;
}
</style>
