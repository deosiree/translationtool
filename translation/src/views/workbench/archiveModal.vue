<template>
  <PipeShell
    :visible="visible"
    :modalTitle="modalTitle"
    :okLoading="loading"
    :showCancel="false"
    okText="归档并结束任务"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
    @setTableHeight="setTableHeight"
  >
    <div class="content">
      <!-- 工具栏壳：任务信息 + 查询区 + 内联展示列 -->
      <PipelinePanel
        :task="task"
        :table-host="this"
        :columns="columns"
        :data-source="dataSource"
          :loading="loading || queryLoading"
        :pagination="pagination"
        :scroll="tableHeight"
        :row-selection="archiveRowSelection"
        :column-actions="archiveColumnActions"
        :custom-row="customRow"
        @update:model-value="checkedColumn = $event"
        @columns-change="syncColumnsFromPref"
        ref="archiveTable"
      >
        词条：
        <a-input
          v-model:value="keyWords"
          style="width: 300px"
          size="small"
          placeholder="请输入词条搜索"
        />
        <span style="margin-left: 10px">词条状态：</span>
        <EntryStateSelect
          :entryState="entryState"
          @update:entryState="entryState = $event"
          :size="'small'"
          :style="'width: 300px'"
        />
        <span style="margin-left: 10px">翻译状态：</span>
        <TransStateSelect
          :translateState="translateState"
          @update:translateState="translateState = $event"
          :size="'small'"
          :style="'width: 300px'"
        />
        <a-button
          type="primary"
          size="small"
          style="margin-left: 8px"
          @click="getTaskEntry"
          >查询</a-button
        >
        <a-button
          v-if="canShowDelete"
          type="primary"
          size="small"
          danger
          style="margin-left: 8px"
          @click="deleteTaskEntry"
          >删除</a-button
        >
      </PipelinePanel>
    </div>
    <template #leftBottomBtn>
      <a-button @click="handleClose">取消</a-button>
      <a-button
        v-if="$currentDepartment && $currentDepartment.ops.has('needIP')"
        type="primary"
        ghost
        @click="placeOnFile"
        >归档</a-button
      >
      <a-button
        type="primary"
        ghost
        @click="placeOnFile2"
        >结束任务</a-button
      >
    </template>
  </PipeShell>
  <CustomModal
    :visible="ipSelectModal"
    :content-loading="writeBackLoading"
    :ok-disabled="writeBackLoading"
    modalTitle="回写服务器"
    @handleClose="ipSelectClose"
    @handleOK="ipSelectOK"
    @afterClose="ipSelectAfterClose"
  >
    <div style="width: 100%; height: 100%">
      <a-form
        ref="ipModal"
        name="custom-validation"
        :model="ipModal"
      >
        <p v-if="optionFlag === 0" style="margin: 0 0 12px; color: #666">
          将所选词条译文回写到指定 IP 服务器，不结束当前任务。
        </p>
        <p v-else style="margin: 0 0 12px; color: #666">
          词条审核和目标语种翻译审核通过后，将译文回写到指定 IP 服务器并结束当前任务。
        </p>
        <a-form-item
          label="IP"
          name="ip"
          :rules="[{ required: true, message: '请选择IP!' }]"
        >
          <a-select
            v-model:value="ipModal.ip"
            :options="ipOptions"
            placeholder="请选择IP"
            allowClear
          ></a-select>
        </a-form-item>
      </a-form>
    </div>
  </CustomModal>
</template>
<script>
import CustomModal from "@/components/modal/index.vue";
import PipeShell from "@/views/workbench/components/PipeShell.vue";
import IsExistBadge from "@/components/stateBadge/isExistBadge.vue";
import EntryStateSelect from "@/components/select/entryStateSelect.vue";
import TransStateSelect from "@/components/select/transStateSelect.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip/CellOverflowTooltip.vue";
import { formatEntryText, formatCellText } from "@/components/table/cellText";
import "@/assets/style/common.less";
import { cloneDeep, iteratee } from "lodash-es";
import {
  getEntryInfoList,
  getI18nAdress,
  deleteEntryInfoByTaskID,
} from "@/http/api/workbench";
import { updateTaskInfo } from "@/http/api/task";
import { setInfo } from "@/http/api/i18Server";
import {
  CheckOutlined,
  CloseOutlined,
  ExclamationCircleOutlined,
  SettingOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import commonParam, { workbenchParams , wbAllCols, wbPresets } from "@/constants/commonParam.js";
import {
  applyTable,
  syncColumnsFromPref as applyTableColumnsFromPref,
} from "@/components/ColumnFilter";
import { filterWbColsForCtx } from "@/components/ColumnFilter/columnBuilder.js";

import {
  handleTableChange as tableHandleTableChange,
  handleReset as tableHandleReset,
  getRowClassName,
} from "@/utils/tableUtils";
import { defaultPagination } from "@/views/workbench/composables/page";
import {
  handleFilterSearch,
  resetOnClose,
} from "@/views/workbench/composables/filterClear";
import { PipelinePanel } from "@/views/workbench/components";
import {
  applyTableSelectionById,
  invalidateSelectionWhenFiltersChange,
  clearTableRowsContext,
  resetTableQuerySession,
  getCurrentSelectionRows,
  refreshTableSelectionById,
  updateTableSelectionById,
} from "@/utils/selectionUtils";
import { createQueryVersionGuard } from "@/utils/queryVersionGuard";
import { getCurrentFormattedTime } from "@/utils/dateUtils";
import { canDeleteAsEntryAuditor } from "@/utils/entryAuditorAuth";
import {
  loading,
  startLoading,
  endLoading,
  resetLoading,
} from "@/composables/useLoading";
import { defineComponent, ref, createVNode } from "vue";
export default {
  setup() {
    return { loading };
  },
  components: {
    CheckOutlined,
    CloseOutlined,
    ExclamationCircleOutlined,
    CustomModal,
    PipeShell,
    IsExistBadge,
    EntryStateSelect,
    TransStateSelect,
    EntryStateBadge,
    TransStateBadge,
    CellOverflowTooltip,
    PipelinePanel,
  },
  emits: ["handleClose", "handleOK", "refresh"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
      default: "归档预览",
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
      pagination: defaultPagination(this.pageChange),
      entryState: null,
      translateState: null,
      selectedRowIndex: null,
      overlayStyle: workbenchParams.overlayStyle, // 展示列样式
      columnSettingsList: [],
      checkboxList: [], // 展示列可选的值
      checkedColumn: [], // 展示列已选的值
      translateStateList: [
        ...commonParam.langTranslateStateList,
        "translateState",
      ],
      state: {
        searchText: "",
        searchedColumn: "",
      },
      filters: null,
      // 历史字段 filteredData：曾由 tableUtils.handleTableChange 写入，仓库内无读取方；批量勾选走 this.filters + selectionUtils（AND）。
      antClearFilter: null,
      selectedRowKeys: [],
      selectedRows: [],
      queryGuard: createQueryVersionGuard(),
      queryScope: "",
      queryLoading: false,
      ipSelectModal: false,
      ipModal: {
        ip: null,
      },
      ipOptions: [],
      optionFlag: 0,
      writeBackLoading: false,
    };
  },
  created() {},
  mounted() {
    this.$nextTick(() => {
      // 获取当前用户信息
      this.user = this.$store.state.user;
    });
  },
  watch: {
    /**
     * 切换归档任务时使旧列表失效，并在弹窗打开时加载新任务。
     * @param {Object} newTask - 当前任务。
     * @param {Object} oldTask - 上一个任务。
     * @returns {void} 无返回值。
     */
    currentTask(newval, oldval) {
      this.task = newval;
      this.task.transMap = commonParam.languageMap[this.task.translateType];
      if (newval?.id !== oldval?.id) {
        // 弹窗实例会复用；先清空旧任务的列表/选择上下文，避免短暂显示错误任务的数据。
        resetTableQuerySession(this);
        if (this.visible) this.getTaskEntry();
      }
    },
    visible: {
      /**
       * 响应归档弹窗开关并使关闭前的列表请求失效。
       * @param {boolean} newValue - 弹窗当前是否打开。
       * @returns {Promise<void>} 可见状态处理完成后结束。
       */
      async handler(newVal) {
        // console.log("打开工作台-归档", newVal);
        if (newVal) {
          this.$nextTick(() => {
            // 1.设置翻译列展示的语种
            applyTable(this, {
              allCols: wbAllCols,
              preset: wbPresets.archiveModal,
              ctx: {
                task: this.task,
                transMap: this.task.transMap,
                pagination: this.pagination,
              },
              colPrefName: "colPref-archiveModal",
              normalWidth: 100,
              needFilter: true,
              filterCols: filterWbColsForCtx,
            });
          });
        } else {
          // 关闭后不接受迟到响应，避免下次打开时被上一轮查询重新填充。
          resetTableQuerySession(this);
        }
      },
    },
  },
  computed: {
    canShowDelete() {
      return canDeleteAsEntryAuditor(this.$store.state.user, this.task);
    },
    archiveColumnActions() {
      return {
        modelValue: this.checkedColumn,
        columns: this.columnSettingsList,
        overlayStyle: this.overlayStyle,
        colPrefName: "colPref-archiveModal",
        normalWidth: 100,
      };
    },
    archiveRowSelection() {
      return {
        columnWidth: 48,
        selectedRowKeys: this.selectedRowKeys,
        selectedRows: this.selectedRows,
        onSelect: this.onSelect,
        onSelectAll: this.onSelectAll,
      };
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
    formatEntryText,
    formatCellText,
    // 删除词条（词条审核员 + 本任务指派人）
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
     * 查询当前任务归档词条，仅允许最新任务与筛选条件更新列表。
     * @returns {Promise<void>} 查询完成后结束。
     */
    async getTaskEntry() {
      let params = {
        taskID: this.task.id,
        entry: this.keyWords,
        entryState: this.entryState,
      };
      let data = [];
      if (this.translateState) {
        data.push(this.translateState);
      }
      const scope = JSON.stringify({ params, data });
      if (scope !== this.queryScope) {
        // 查询范围变化时仅清空列表和编辑上下文，保留跨查询的已选词条参与归档。
        this.queryScope = scope;
        clearTableRowsContext(this);
      }
      // 用版本号绑定本次请求，后返回的旧响应不会覆盖用户刚切换的筛选结果。
      const token = this.queryGuard.next();
      const taskId = this.task.id;
      this.queryLoading = true;
      try {
        const res = await getEntryInfoList(params, data);
        // 请求返回后再次确认任务和版本，保护弹窗复用时的列表展示。
        if (!this.queryGuard.isCurrent(token) || this.task.id !== taskId) return;
        this.dataSource = res.data.list;
        this.allData = this.dataSource;
        // 按 ID 将仍在结果中的选择投影回新行对象，保持复选框和底部操作一致。
        refreshTableSelectionById(this, this.dataSource);
      } catch (err) {
        if (this.queryGuard.isCurrent(token)) message.error(err.message);
      } finally {
        // 只有当前请求才能收起加载态，防止旧请求结束时误关新请求的遮罩。
        if (this.queryGuard.isCurrent(token)) this.queryLoading = false;
      }
    },
    handleOK() {
      // 归档
      // 获取全部词条
      let params = {
        taskID: this.task.id,
        entry: "",
      };
      let data = [];
      startLoading();
      getEntryInfoList(params, data)
        .then((res) => {
          this.checkedEntry(res.data.list);
        })
        .catch((err) => {
          message.error(err.message);
          endLoading();
        });
    },
    checkedEntry(data) {
      if (!data) {
        endLoading();
        return;
      }
      let code = commonParam.languageMap[this.task.translateType].state;
      let flag = false;
      data.forEach((item) => {
        if (item.entryState != 3 || item[code] != 3) {
          // 含有未处理完的词条
          flag = true;
        }
      });

      if (flag) {
        endLoading();
        Modal.error({
          title: "当前任务存在未处理完的词条，不可归档！",
          style: { top: "30%" },
        });
      } else {
        this.optionFlag = 1;
        this.ipSelectModal = true;
        this.getIPs();
        // Modal.confirm({
        //     title: '是否确定归档并结束任务?',
        //     icon: createVNode(ExclamationCircleOutlined),
        //     okText: '是',
        //     cancelText: '否',
        //     style: {top:'30%'},
        //     onOk: () => {
        //         this.saveLoading = false
        //         this.task.state = '6'
        //         this.task.endTime = new Date().toLocaleString().replaceAll('/','-')
        //         updateTaskInfo(this.task).then((res) => {
        //             message.success("已归档！")
        //             this.$emit('refresh')
        //         })
        //         // 回写数据
        //         let params = {
        //             taskID: this.task.id,
        //             translateType: this.task.translateType,
        //             isTag:0,
        //             isComment:0
        //         }
        //         setInfo(params,[]).then((res) => {

        //         }).catch((err) => {

        //         })
        //     },
        //     onCancel: () => {
        //         this.saveLoading = false
        //     }
        // });
      }
    },
    handleClose() {
      this.$emit("handleClose");
    },
    getRowClassName(record, index) {
      return getRowClassName(record, index, this.selectedRowIndex);
    },
    // 添加表格行点击事件
    customRow(record, index) {
      return {
        onClick: (event) => {
          // this.selectedRowIndex = record.id
        },
      };
    },
    afterClose() {
      resetLoading();
      this.keyWords = "";
      this.pagination.current = 1;
      this.pagination.pageSize = 20;
      // 关弹窗：Ant 原生清列头筛选 + 搜索态复位
      resetOnClose(this);
    },
    // 列头自定义筛选 + 保存 Ant clearFilters
    handleSearch(selectedKeys, confirm, dataIndex, clearFilters) {
      handleFilterSearch(selectedKeys, confirm, dataIndex, clearFilters, this);
    },
    // 列筛选重置
    handleReset(clearFilters) {
      tableHandleReset(clearFilters, this);
    },
    // 同步 filters / filteredValue（供全部选择；filteredData 写入见 tableUtils）
    /**
     * 应用归档表格分页及筛选变化。
     * @param {Object} pagination - 表格分页状态。
     * @param {Object} filters - 表格列筛选值。
     * @returns {void} 无返回值。
     */
    handleTableChange(pagination, filters) {
      invalidateSelectionWhenFiltersChange(this, filters);
      tableHandleTableChange(pagination, filters, this);
    },
    // // 两个数组取并集(可封装)
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

    // 动态设置表格高度
    setTableHeight(height, type) {
      if (type === "full") {
        this.tableHeight.y = height - 230;
      } else if (type === "reduce") {
        this.tableHeight.y = 415;
      }
    },
    // 分页（archive 无当前页校验）
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
    },
    /**
     * 同步归档表格的单行操作，同时保留当前完整结果范围外的选择。
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
     * 将当前选中的词条回写到已选服务器。
     * @param {void} none 本方法不接收参数。
     * @returns {Promise<void>} 回写成功或失败处理完成后结束。
     */
    async writeBackFun() {
      if (this.selectedRows.length === 0) {
        message.info("请选择词条");
        return;
      }
      // 回写数据
      let params = {
        taskID: this.task.id,
        translateType: this.task.translateType,
        isTag: 0,
        isComment: 0,
        i18nUrl: this.ipModal.ip,
      };
      try {
        await setInfo(params, this.selectedRows);
        message.success("归档成功！");
        this.selectedRowKeys = [];
        this.selectedRows = [];
        this.ipSelectModal = false;
      } catch (err) {
        message.error("归档失败！", err.message);
      }
    },
    // 归档按钮点击事件
    placeOnFile() {
      if (this.selectedRows.length === 0) {
        message.info("请选择词条");
        return;
      }
      this.ipSelectModal = true;
      this.optionFlag = 0;
      this.getIPs();
    },
    /**
     * 打开结束当前任务的影响确认框。
     * @param {void} none 本方法不接收参数。
     * @returns {void} 确认框打开后结束。
     */
    placeOnFile2() {
      Modal.confirm({
        title: "是否结束任务？",
        content: '结束后，当前任务下的所有词条状态都将更新为"已归档"，任务将从待办事项移至已办事项。不会回写词条到服务器。',
        icon: createVNode(ExclamationCircleOutlined),
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          this.task.state = "6";
          this.task.endTime = getCurrentFormattedTime();
          updateTaskInfo(this.task).then((res) => {
            message.success("已结束任务！（词条状态更新为'已归档'）");
            this.$emit("refresh");
          });
        },
      });
    },
    ipSelectAfterClose() {
      resetLoading();
      this.ipModal.ip = null;
    },
    /**
     * 校验回写服务器并完成当前归档模式，始终覆盖完整异步操作显示 loading。
     * @param {void} none 本方法不接收参数。
     * @returns {Promise<void>} 校验和归档请求全部结束后完成。
     */
    async ipSelectOK() {
      if (this.writeBackLoading) return;
      this.writeBackLoading = true;
      try {
        try {
          await this.$refs.ipModal.validate();
        } catch {
          return;
        }
        if (this.optionFlag === 0) {
          await this.writeBackFun();
        } else if (this.optionFlag === 1) {
          endLoading();
          this.task.state = "6";
          this.task.endTime = getCurrentFormattedTime();
          const params = {
            taskID: this.task.id,
            translateType: this.task.translateType,
            isTag: 0,
            isComment: 0,
            i18nUrl: this.ipModal.ip,
          };
          const taskUpdate = updateTaskInfo(this.task)
            .then(() => {
              message.success("已归档！");
              this.$emit("refresh");
            })
            .catch((err) => {
              message.error(err.message);
            });
          const archiveWriteBack = setInfo(params, [])
            .then(() => {
              this.ipSelectModal = false;
            })
            .catch((err) => {
              message.error(err.message);
            });
          await Promise.all([taskUpdate, archiveWriteBack]);
        }
      } catch (err) {
        message.error(err.message);
      } finally {
        this.writeBackLoading = false;
      }
    },
    ipSelectClose() {
      this.ipSelectModal = false;
      endLoading();
      this.ipModal.ip = null;
    },
    // 获取i18服务器ip
    getIPs() {
      this.ipOptions = [];
      getI18nAdress().then((res) => {
        res.data.list.forEach((item) => {
          let ip = {
            label: item.ip,
            value: item.ip,
          };
          // if(item.state === '1'){
          //     this.ipModal.ip = item.ip
          // }
          this.ipOptions.push(ip);
        });
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
}
.ant-table-cell .ant-form-item {
  margin-bottom: 0%;
}
:deep(.ant-pagination) {
  margin: 8px 0;
}
</style>
