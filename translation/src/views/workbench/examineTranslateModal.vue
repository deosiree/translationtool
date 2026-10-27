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

      <!-- 工具栏壳：校验规则 + 查询/批量审核 + 内联展示列 -->
      <PipelinePanel
        :task="task"
        :table-host="this"
        :columns="columns"
        :data-source="dataSource"
        :loading="loading || queryLoading"
        :row-selection="examineTranslateRowSelection"
        :pagination="pagination"
        :scroll="tableHeight"
        :custom-row="doubleClick"
        :column-actions="examineTranslateColumnActions"
        :edit="examineTranslateEditCols"
        :editable-data="editableData"
        :cell-errors="cellErrors"
        @update:model-value="checkedColumn = $event"
        @columns-change="syncColumnsFromPref"
        @cell-input="onCellInput"
        @save-edit="edit"
        @cancel-edit="cancel"
        @audit-pass="passTagChange"
        @audit-reject="rejectTagChange"
        ref="tableContainer"
      >
        <template #taskExtra>
          <RulesDropdown :options="rulesOptions" @update:options="rulesOptions"></RulesDropdown>
        </template>
        词条：
        <a-input v-model:value="keyWords" style="width:300px" size="small" placeholder='请输入词条搜索' />
        <span style="margin-left:10px">翻译状态：</span>
        <TransStateSelect :translateState="translateState" @update:translateState="translateState = $event" :size="'small'" :style="'width: 300px'"
          :filter="new Set(['0'])" />
        <a-button type="primary" size="small" style="margin-left:8px" @click="getTaskEntry">查询</a-button>
        <AuditButtons @pass="pass" @reject="reject" />
        <a-button v-if="canShowDelete" type="primary" size="small" danger style="margin-left:8px" @click="deleteTaskEntry">删除</a-button>
      </PipelinePanel>
    </div>
  </PipeShell>
</template>
<script>
import "@/assets/style/common.less";
import PipeShell from "@/views/workbench/components/PipeShell.vue";
import TransStateSelect from "@/components/select/transStateSelect.vue";
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
import { message, Modal as AntModal } from "ant-design-vue";
import commonParam, { workbenchParams } from "@/constants/commonParam.js";
import { applyTable, syncColumnsFromPref as applyTableColumnsFromPref } from "@/components/ColumnFilter";
import { filterWbColsForCtx } from "@/components/ColumnFilter/columnBuilder.js";
import { wbAllCols, wbPresets } from "@/constants/commonParam.js";
import {
  PipelinePanel,
  AuditTags,
  AuditButtons,
} from "@/views/workbench/components";
import {
  getRowClassName,
  handleReset as tableHandleReset,
} from "@/utils/tableUtils";
import { defaultPagination, pageChange as wbPageChange } from "@/views/workbench/composables/page";
import {
  handleFilterSearch,
  resetOnClose,
} from "@/views/workbench/composables/filterClear";
import { editTextCols } from "@/views/workbench/utils/editCols";
import {
  applyTableSelectionById,
  clearTableSelection,
  clearTableRowsContext,
  resetTableQuerySession,
  getCurrentSelectionRows,
  refreshTableSelectionById,
  updateTableSelectionById,
} from "@/utils/selectionUtils";
import { createQueryVersionGuard } from "@/utils/queryVersionGuard";
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
import RulesDropdown from "@/components/Dropdown/rulesDropdown.vue";
import { canDeleteAsEntryAuditor } from "@/utils/entryAuditorAuth";
import { computed, defineComponent, ref, createVNode } from "vue";
import {
  CheckOutlined,
  CloseOutlined,
  FileSearchOutlined,
  QuestionCircleOutlined,
  SettingOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons-vue";
import key from "keymaster";
export default {
  // 全局单一 loading 状态（hook 导出，响应式）：表格遮罩/保存按钮统一绑定
  setup() {
    return { loading };
  },
  components: {
    CheckOutlined,
    CloseOutlined,
    PipeShell,
    QuestionCircleOutlined,
    SettingOutlined,
    ExclamationCircleOutlined,
    TransStateSelect,
    IsExistBadge,
    EntryStateBadge,
    TransStateBadge,
    InputIME,
    TableCellTextArea,
    CellOverflowTooltip,
    PipelinePanel,
    RulesDropdown,
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
      // modal width 由 PipeShell 默认 90%
      task: {},
      keyWords: "",
      tableHeight: { x: "max-content", y: 415 },
      // tableHeight: { x: "100%", y: 415 },
      columns: [],
      dataSource: [],
      allData: [],
      pagination: defaultPagination(this.pageChange),
      selectedRowKeys: [],
      selectedRows: [],
      queryGuard: createQueryVersionGuard(),
      queryScope: "",
      queryLoading: false,
      editableData: {},
      rules: {},
      cellErrors: {},
      translateState: null,
      selectedRowIndex: null,
      timer: null,
      state: {
        searchText: "",
        searchedColumn: "",
      },
      antClearFilter: null,
      rulesOptions: commonParam.rulesOptions,
      overlayStyle: workbenchParams.overlayStyle, // 展示列样式
      columnSettingsList: [],
      checkboxList: [], // 展示列可选的值
      checkedColumn: [], // 展示列已选的值
      editList_needValidate: null, // 可编辑的列名集合(需要验证长度)
      editList: null, // 可编辑的列名集合
      translateStateList: [
        ...commonParam.langTranslateStateList,
        "translateState",
      ],
    };
  },

  created() {},
  mounted() {},
  computed: {
    examineTranslateEditCols() {
      return [...this.editableTextAreaColumns, "maxLength"];
    },
    editableTextAreaColumns() {
      return editTextCols(this, { withEditList: true });
    },
    canShowDelete() {
      return canDeleteAsEntryAuditor(this.$store.state.user, this.task);
    },
    examineTranslateColumnActions() {
      return {
        modelValue: this.checkedColumn,
        columns: this.columnSettingsList,
        overlayStyle: this.overlayStyle,
        colPrefName: "colPref-examineTranslateModal",
        normalWidth: 100,
      };
    },
    examineTranslateRowSelection() {
      return {
        columnWidth: 48,
        selectedRowKeys: this.selectedRowKeys,
        onSelect: this.onSelect,
        onSelectAll: this.onSelectAll,
      };
    },
  },
  watch: {
    /**
     * 切换翻译审核任务时清理旧选择并使旧列表请求失效。
     * @param {Object} newTask - 当前任务。
     * @param {Object} oldTask - 上一个任务。
     * @returns {void} 无返回值。
     */
    currentTask(newval, oldval) {
      this.task = newval;
      this.task.transMap = commonParam.languageMap[this.task.translateType];
      if (newval?.id !== oldval?.id) {
        // 任务切换时同步丢弃旧选择与请求，避免审核意见写入错误任务。
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
       * 响应翻译审核弹窗开关并使关闭前的列表请求失效。
       * @param {boolean} newValue - 弹窗当前是否打开。
       * @returns {Promise<void>} 可见状态处理完成后结束。
       */
      async handler(newVal) {
        // console.log("打开工作台-翻译审核", newVal);
        if (newVal) {
          this.$nextTick(() => {
            // 1.设置翻译列展示的语种
            // 设置翻译列可编辑&可校验
            this.editList_needValidate = [this.task.transMap.value];
            // 设置对应的翻译释义列可编辑
            this.editList = [
              this.task.transMap.interpretation,
              this.task.transMap.auditSuggest,
            ];
            applyTable(this, {
              allCols: wbAllCols,
              preset: wbPresets.examineTranslateModal,
              ctx: {
                task: this.task,
                transMap: this.task.transMap,
                pagination: this.pagination,
              },
              colPrefName: "colPref-examineTranslateModal",
              normalWidth: 100,
              needFilter: true,
              filterCols: filterWbColsForCtx,
            });
          });
        } else {
          // 弹窗关闭后不再接收旧请求，防止下次打开看到过期审核列表。
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
    // 删除词条（词条审核员 + 本任务指派人）
    deleteTaskEntry() {
      if (this.selectedRows.length === 0) {
        return;
      }
      AntModal.confirm({
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
              message.success(text);
              this.getTaskEntry();
            })
            .catch((err) => {
              message.error("删除失败！", err.message);
            });
        },
        onCancel: () => {},
      });
    },
    /**
     * 查询当前任务待审核翻译，只提交最新任务与筛选条件的响应。
     * @returns {Promise<void>} 查询和校验完成后结束。
     */
    async getTaskEntry() {
      let params = {
        taskID: this.task.id,
        entryState: "3",
        entry: this.keyWords,
      };
      let data =
        this.translateState === null || this.translateState === undefined
          ? ["1"]
          : [this.translateState];
      const scope = JSON.stringify({ params, data });
      if (scope !== this.queryScope) {
        // 当前筛选范围发生变化时仅清空列表和编辑上下文，保留跨查询的已选词条参与审核。
        this.queryScope = scope;
        clearTableRowsContext(this);
      }
      // 版本令牌确保快速筛选时只有最后一次请求能更新页面。
      const token = this.queryGuard.next();
      const taskId = this.task.id;
      this.queryLoading = true;

      try {
        const res = await getEntryInfoList(params, data);
        // 任务或筛选已变化时忽略迟到响应，保护当前审核上下文。
        if (!this.queryGuard.isCurrent(token) || this.task.id !== taskId) return;
        // 更新成功：刷新所有任务的小红点
        this.$emit("afterSave", this.currentTask);

        this.dataSource = res.data.list;

        this.dataSource.forEach((item) => {
          item.auditState = -1;
          item[this.task.transMap.auditSuggest] = ""; // 对应语种的审核意见清空
        });
        this.allData = this.dataSource;
        // 列表刷新后按 ID 对齐仍有效的勾选项，避免底部批量操作指向旧对象。
        refreshTableSelectionById(this, this.dataSource);
        await revalidateLoaded(
          this,
          this.task.transMap.value,
          () => this.queryGuard.isCurrent(token) && this.task.id === taskId,
        );
      } catch (err) {
        if (this.queryGuard.isCurrent(token)) message.error("1", err.message);
      } finally {
        // 只允许当前请求结束加载态，避免并发请求造成遮罩闪烁。
        if (this.queryGuard.isCurrent(token)) this.queryLoading = false;
      }
    },
    // 单元格输入更新：集中处理 editableData 写入，防止 IME 组合期间给 undefined 赋值
    onCellInput(value, record, column) {
      onEditableCellInput(this, record.id, column.dataIndex, value);
    },
    formatEntryText,
    formatCellText,
    formatMaxLengthText,
    /**
     * 校验并保存翻译审核结果，只把接口确认成功的行合并回表格。
     * @returns {Promise<void>} 保存与列表刷新完成后结束。
     */
    async handleOK() {
      // 保存/校验期间触发全局遮罩（引用计数成对，防连点由遮罩拦截）
      startLoading();

      const transCol = this.task.transMap.value;
      const methods = getMethods(this);

      // 1. 底部保存：按当前勾选复检所有待流转词条
      // 若某行仍在编辑态，classifyArr 优先读 editableData 中的最新值
      const updateArrCandidate = this.dataSource.filter(
        (item) => item.auditState === 1 || item.auditState === 0
      );
      if (updateArrCandidate.length > 0) {
        // 校验失败时保留原列表和编辑态，并将用户定位到红字单元格。
        clearCellErrorsForRecords(this, updateArrCandidate.map((r) => r.id));
        const verifyResult = await classifyArr(this, updateArrCandidate, transCol, methods);
        if (verifyResult.errorIds.size > 0) {
          await openFailRows(this, updateArrCandidate, verifyResult, transCol);
          message.warn(`校验不通过 ${verifyResult.errorIds.size} 条，请检查翻译列红字`);
          endLoading();
          return;
        }
      }

      let params = {
        taskID: this.task.id,
      };
      const updateArr = [];
      // 把审核结论转换为目标翻译状态；只提交用户明确操作过的行。
      this.dataSource.forEach((item) => {
        if (item.auditState === 0) {
          const submitted = cloneDeep(this.editableData[item.id] || item);
          submitted.parentID = "";
          submitted[this.task.transMap.state] = "2";
          updateArr.push(submitted);
        } else if (item.auditState === 1) {
          const submitted = cloneDeep(this.editableData[item.id] || item);
          submitted.parentID = "";
          submitted[this.task.transMap.state] = "3";
          updateArr.push(submitted);
        }
      });

      if (updateArr.length === 0) {
        endLoading();
        return;
      }

      try {
        const res = await updateEntryList(params, updateArr);
        // 失败 ID 与成功行分开映射，部分失败不会覆盖用户仍需处理的内容。
        const failedIds = new Set((res.data?.list || []).map((item) => item.id));
        const successfulRows = updateArr.filter((item) => !failedIds.has(item.id));
        const successfulById = new Map(successfulRows.map((item) => [item.id, item]));

        // 仅将已确认成功的快照写回列表和选择，随后清除对应编辑缓存。
        this.dataSource = this.dataSource.map(
          (item) => successfulById.get(item.id) || item
        );
        this.allData = this.allData.map(
          (item) => successfulById.get(item.id) || item
        );
        this.selectedRows = this.selectedRows.map(
          (item) => successfulById.get(item.id) || item
        );
        successfulRows.forEach((item) => delete this.editableData[item.id]);

        if (failedIds.size > 0) {
          message.error(`保存失败 ${failedIds.size} 条`);
        } else {
          message.success("已保存！");
        }
        // 保存后重新查询待审核队列，使成功流转的行从当前页面消失。
        await this.getTaskEntry();
        if (this.dataSource.length === 0) this.handleClose();
      } catch (err) {
        message.error("保存失败！", err.message);
      } finally {
        endLoading();
      }
    },
    /**
     * 关闭翻译审核弹窗并丢弃当前列表与选择请求。
     * @returns {void} 无返回值。
     */
    handleClose() {
      this.queryGuard.invalidate();
      this.selectedRows = [];
      this.selectedRowKeys = [];
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
      this.dataSource = this.allData.filter((item) =>
        item.entry.includes(this.keyWords)
      );
      this.pagination.current = 1;
      this.pagination.total = this.dataSource.length;
    },
    /**
     * 同步翻译审核表格的单行操作，同时保留完整结果范围外的选择。
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
    // 通过标签点击事件
    passTagChange(record) {
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
    },
    // 驳回标签点击事件
    rejectTagChange(record) {
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
    },
    /**
     * 批量设置选中行审核状态并清空多选。
     * @param {0|1} state 0 驳回，1 通过
     */
    applyAudit(state) {
      this.selectedRows.forEach((item) => {
        item.auditState = state;
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
        onClick: (event) => {
          this.selectedRowIndex = record.id;
        },
        onDblclick: async (event) => {
          if (this.editableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }
          await openSetEdit(record, [this.task.transMap.value], this);
          this.showEditOperation(); // 显示编辑操作列；双击不跑 applyCell
        },
      };
    },
    // 行内 ✓ / 编辑框回车：公共 saveEdit；本页回写非空字段并置翻译状态为待审核
    async edit(record) {
      const transCol = this.task.transMap.value;
      await saveEdit(this, record, {
        transCol,
        commit: (rec, row) => {
          for (const [key, value] of Object.entries(row)) {
            if (rec.hasOwnProperty(key) && value != null && value !== "") {
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
    // 删除操作列
    hideEditOperation() {
      hideEditOp(this);
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
    /**
     * 将当前编辑行滚动到共享视口可见区域。
     * @param {number} rowIndex 当前数据行索引。
     * @returns {void} 等待 Vue 更新后完成滚动。
     */
    scrollTableToRow(rowIndex) {
      this.$nextTick(() => {
        const table = this.$refs.tableContainer; // 获取表格容器元素
        if (table && rowIndex >= 0) {
          // 根据索引查找目标行元素
          let flag =
            rowIndex - (this.pagination.current - 1) * this.pagination.pageSize;
          const targetElement = table.$el.querySelectorAll("tr")[flag];
          const container =
            this.$refs.tableContainer.$el.querySelector(".ant-table-content");
          if (targetElement && container) {
            const contentHeight = container.clientHeight;
            container.scrollTop = flag * targetElement.offsetHeight - contentHeight; // 当前行 * 行高 - 表格展示高度
          }
        }
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
