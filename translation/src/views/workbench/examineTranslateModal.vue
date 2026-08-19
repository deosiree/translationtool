<template>
  <Modal :visible="visible" :modalTitle="modalTitle" :modalWidth="modalWidth" :fullFlag="true" okText="保存" :okLoading="saveLoading"
    @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose" @setTableHeight="setTableHeight">
    <div class="content">

      <WorkbenchTaskInfo :task="task">
        <template #extra>
          <RulesDropdown :options="rulesOptions" @update:options="rulesOptions"></RulesDropdown>
        </template>
      </WorkbenchTaskInfo>
      <WorkbenchFormBar>
        词条：
        <a-input v-model:value="keyWords" style="width:300px" size="small" placeholder='请输入词条搜索' />
        <span style="margin-left:10px">翻译状态：</span>
        <TransStateSelect :translateState="translateState" @update:translateState="translateState = $event" :size="'small'" :style="'width: 300px'"
          :filter="new Set(['0'])" />
        <a-button type="primary" size="small" style="margin-left:8px" @click="getTaskEntry">查询</a-button>
        <!-- <a-button type="primary" size="small" style="margin-left:8px" @click="selectAll">{{selectAllName}}</a-button> -->
        <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="pass">通过</a-button>
        <a-button type="primary" size="small" style="margin-left:8px" class="rejectBtn" @click="reject">驳回</a-button>
        <a-button v-if="canShowDelete" type="primary" size="small" danger style="margin-left:8px" @click="deleteTaskEntry">删除</a-button>
        <WorkbenchActionGroup inline-offset>
          <WorkbenchColumnActions
            v-model="checkedColumn"
            :columns="columnSettingsList"
            :overlay-style="overlayStyle"
            col-pref-name="colPref-examineTranslateModal"
            :normal-width="100"
            :need-filter="false"
            @change="syncColumnsFromPref"
          />
        </WorkbenchActionGroup>
      </WorkbenchFormBar>
      <a-table bordered class="ant-table-striped table-cell-overflow" :columns="columns" :data-source="dataSource" :row-selection="{ 
                selectedRowKeys: selectedRowKeys, 
                onChange: onSelectChange,
                selections:[
                    {key:'selectAll',text:'全部选择',onSelect:selectAllEntry},
                    {key:'clearAll',text:'取消选择',onSelect:clearAllEntry}
                ]
            }" :row-key="record => record.id" :scroll="tableHeight" :pagination='pagination' :loading="loading" :rowClassName="getRowClassName"
        :customRow="doubleClick" ref="tableContainer" @resizeColumn="handleResizeColumn">
        <template #headerCell="{ title, column }">
          <CellOverflowTooltip v-if="column.colValue" :content="title">
            {{ title }}
          </CellOverflowTooltip>
        </template>
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.dataIndex === 'entry'">
            <CellOverflowTooltip :content="formatEntryText(text)">
              {{ formatEntryText(text) }}
            </CellOverflowTooltip>
          </template>
          <template v-else-if="editableTextAreaColumns.includes(column.dataIndex)">
            <template v-if="editableData[record.id]">
              <TableCellTextArea
                :value="editableData[record.id][column.dataIndex] ?? ''"
                @update:value="(val) => onCellInput(val, record, column)"
                :error-message="cellErrors[record.id]?.[column.dataIndex]"
              />
            </template>
            <template v-else>
              <CellOverflowTooltip :content="formatCellText(text)" />
            </template>
          </template>
          <template v-else-if="column.dataIndex === 'tag'">
            <CellOverflowTooltip :content="formatTagText(text)">
              <span>
                <a-tag v-for="(tag,index) in companyCut(text)" :key="index" color="cyan" class="tag-content">
                  {{tag}}
                </a-tag>
              </span>
            </CellOverflowTooltip>
          </template>
          <template v-else-if="column.dataIndex === 'isExist'">
            <CellOverflowTooltip :content="isExistLabel(text)">
              <IsExistBadge :isExist="text" />
            </CellOverflowTooltip>
          </template>
          <template v-else-if="column.dataIndex === 'entryState'">
            <CellOverflowTooltip :content="entryStateLabel(text)">
              <EntryStateBadge :entryState="text" />
            </CellOverflowTooltip>
          </template>
          <template v-else-if="translateStateList.includes(column.dataIndex)">
            <CellOverflowTooltip :content="translateStateLabel(text)">
              <TransStateBadge :translateState="text" />
            </CellOverflowTooltip>
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
          <template v-else-if="column.dataIndex && column.dataIndex !== 'index'">
            <CellOverflowTooltip :content="formatCellText(text)" />
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
  </Modal>
  <Modal :visible="rejectReasonVisible" modalTitle="驳回原因" @handleClose="rejectReasonClose" @handleOK="rejectReasonOK"
    @afterClose="rejectReasonAfterClose">
    <div style="width:100%;height:100%">
      <a-form ref="exportForm" name="custom-validation" :model="rejectReason">
        <a-form-item label="驳回原因" name="reason">
          <a-textarea v-model:value="rejectReason.reason" placeholder="请输入驳回原因" allow-clear />
        </a-form-item>
      </a-form>
    </div>
  </Modal>
</template>
<script>
import "@/assets/style/common.less";
import Modal from "@/components/modal/index.vue";
import TransStateSelect from "@/components/select/transStateSelect.vue";
import IsExistBadge from "@/components/stateBadge/isExistBadge.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import InputIME from "@/components/cellEditor/input_IME.vue";
import TableCellTextArea from "@/components/table/TableCellTextArea.vue";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip.vue";
import { formatEntryText, formatCellText } from "@/components/table/cellText";
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
  WorkbenchFormBar,
  WorkbenchActionGroup,
  WorkbenchTaskInfo,
  WorkbenchColumnActions,
} from "@/components/Workbench";
import {
  handleResizeColumn,
  getRowClassName,
} from "@/utils/tableUtils";
import {
  selectAllEntry as selectAllEntryUtil,
  clearAllEntry as clearAllEntryUtil,
  onSelectChange as onSelectChangeUtil,
} from "@/utils/selectionUtils";
import { setModalAriaHidden } from "@/utils/domUtils";
import {
  byteLength,
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
import RulesDropdown from "@/components/Dropdown/rulesDropdown.vue";
import { canDeleteAsEntryAuditor } from "@/utils/entryAuditorAuth";
import { computed, defineComponent, ref, createVNode } from "vue";
import {
  CheckOutlined,
  CloseOutlined,
  FileSearchOutlined,
  QuestionCircleOutlined,
  SearchOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
  SettingOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons-vue";
import key from "keymaster";
export default {
  components: {
    CheckOutlined,
    CloseOutlined,
    Modal,
    QuestionCircleOutlined,
    SearchOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    SettingOutlined,
    ExclamationCircleOutlined,
    TransStateSelect,
    IsExistBadge,
    EntryStateBadge,
    TransStateBadge,
    InputIME,
    TableCellTextArea,
    CellOverflowTooltip,
    WorkbenchFormBar,
    WorkbenchActionGroup,
    WorkbenchTaskInfo,
    WorkbenchColumnActions,
    RulesDropdown,
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
      modalWidth: "70%",
      task: {},
      keyWords: "",
      tableHeight: { x: "max-content", y: 415 },
      // tableHeight: { x: "100%", y: 415 },
      loading: false,
      columns: [],
      dataSource: [],
      allData: [],
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
      selectedRowKeys: [],
      selectedRows: [],
      editableData: {},
      rules: {},
      cellErrors: {},
      translateState: null,
      saveLoading: false,
      selectedRowIndex: null,
      timer: null,
      state: {
        searchText: "",
        searchedColumn: "",
      },
      clearFilters: null,
      selectAllName: "全选",
      rejectReasonVisible: false,
      rejectReason: {
        reason: "",
      },
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
    editableTextAreaColumns() {
      const dedicatedInputCols = ["diFileName", "tag"];
      return [
        ...(this.editList_needValidate || []),
        ...(this.editList || []),
      ].filter((col) => !dedicatedInputCols.includes(col));
    },
    canShowDelete() {
      return canDeleteAsEntryAuditor(this.$store.state.user, this.task);
    },
  },
  watch: {
    currentTask(newval, oldval) {
      this.task = newval;
      this.task.transMap = commonParam.languageMap[this.task.translateType];
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
              needFilter: false,
              filterCols: filterWbColsForCtx,
              lockCellSize: true,
            });
          });
        }
      },
    },
  },
  methods: {
    syncColumnsFromPref() {
      applyTableColumnsFromPref(this);
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
    // 获取待审核词条
    getTaskEntry() {
      let params = {
        taskID: this.task.id,
        entryState: "3",
        entry: this.keyWords,
      };
      this.loading = true;
      let data =
        this.translateState === null || this.translateState === undefined
          ? ["1"]
          : [this.translateState];

      let auditSuggest = this.task.transMap.auditSuggest;
      getEntryInfoList(params, data)
        .then((res) => {
          // 更新成功：刷新所有任务的小红点
          this.$emit("afterSave", this.currentTask);

          this.dataSource = res.data.list;

          this.dataSource.forEach((item) => {
            item.auditState = -1;
            item[this.task.transMap.auditSuggest] = ""; // 对应语种的审核意见清空
          });
          // console.log("所有审核状态的状态都变成了-1，即审核不通过", this.dataSource);
          // this.allData = this.dataSource
          this.loading = false;
          // this.select()
        })
        .catch((err) => {
          this.loading = false;
          message.error("1", err.message);
        });

      // 初始化快捷键
      // this.initShortcutKeys();
    },
    // 单元格输入更新：集中处理 editableData 写入，防止 IME 组合期间给 undefined 赋值
    onCellInput(value, record, column) {
      onEditableCellInput(this, record.id, column.dataIndex, value);
    },
    formatEntryText,
    formatCellText,
    formatTagText(text) {
      return this.companyCut(text).join("; ");
    },
    isExistLabel(value) {
      if (value === 0) return "新建";
      if (value === 1) return "已存在";
      return "";
    },
    entryStateLabel(value) {
      const map = {
        0: "新建",
        1: "审核中",
        2: "审核不通过",
        3: "已审核",
        "-1": "禁用",
      };
      return map[value] ?? "";
    },
    translateStateLabel(value) {
      const map = {
        0: "未翻译",
        1: "待审核",
        2: "审核不通过",
        3: "已审核",
      };
      return map[value] ?? "未翻译";
    },
    async handleOK() {
      this.loading = true;
      this.saveLoading = true;

      const transCol = this.task.transMap.value;
      const methods = getMethods(this);

      // 1. 底部保存：按当前勾选复检所有待流转词条
      // 若某行仍在编辑态，classifyArr 优先读 editableData 中的最新值
      const updateArrCandidate = this.dataSource.filter(
        (item) => item.auditState === 1 || item.auditState === 0
      );
      if (updateArrCandidate.length > 0) {
        clearCellErrorsForRecords(this, updateArrCandidate.map((r) => r.id));
        const verifyResult = await classifyArr(this, updateArrCandidate, transCol, methods);
        if (verifyResult.errorIds.size > 0) {
          await openFailRows(this, updateArrCandidate, verifyResult, transCol);
          message.warn(`校验不通过 ${verifyResult.errorIds.size} 条，请检查翻译列红字`);
          this.saveLoading = false;
          this.loading = false;
          return;
        }
      }

      // 2. 校验通过后再合并 editableData -> dataSource
      for (let key in this.editableData) {
        const index = this.dataSource.findIndex((item) => item.id === key);
        if (index != -1) {
          this.dataSource[index] = cloneDeep(this.editableData[key]);
        }
      }
      this.editableData = {};

      let params = {
        taskID: this.task.id,
      };
      let updateArr = [];
      let okArr = [];
      this.dataSource.forEach((item) => {
        item.parentID = "";
        if (item.auditState === 0) {
          item[this.task.transMap.state] = "2";
          updateArr.push(item);
        } else if (item.auditState === 1) {
          item[this.task.transMap.state] = "3";
          updateArr.push(item);
          okArr.push(item);
        }
      });

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
            this.loading = false;
            // console.log("剩余待处理数据的数量：", this.dataSource.length-updateArr.length);
            if (this.dataSource.length == updateArr.length) {
              // 如果没有待处理的数据就自动关闭弹窗
              this.handleClose();
            }
          });
      } else {
        this.saveLoading = false;
        this.loading = false;
      }
    },
    handleClose() {
      this.$emit("handleClose");
    },
    getRowClassName(record, index) {
      return getRowClassName(record, index, this.selectedRowIndex);
    },
    handleResizeColumn,
    // 模糊查询
    select() {
      this.dataSource = this.allData.filter((item) =>
        item.entry.includes(this.keyWords)
      );
    },
    onSelectChange(selectedRowKeys, selectedRows) {
      onSelectChangeUtil(this, selectedRowKeys, selectedRows);
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
    // 通过按钮点击事件
    pass() {
      this.selectedRows.forEach((item) => {
        item.auditState = 1;
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

      // this.selectedRows.forEach(item => {
      //     item.auditState = 0
      // })
      // this.selectedRowKeys = []
      // this.selectedRows = []
      // this.selectAllName = "全选"
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
          maxLength === undefined ||
          maxLength === "" ||
          maxLength === null ||
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
            container.scrollTop = flag * targetElement.offsetHeight - 370; // 当前行 * 行高 - 表格展示高度
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
        // if (common.byteLength(text) > maxLength) {
        if (byteLength(text) > maxLength) {
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
                message.error(
                  "翻译审核校验未通过，请检查翻译内容",
                  err.message
                );
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
    selectAllEntry() {
      selectAllEntryUtil(this);
    },
    clearAllEntry() {
      clearAllEntryUtil(this);
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
        item[this.task.transMap.auditSuggest] = this.rejectReason.reason;
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
    afterClose() {
      this.editableData = {};
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.keyWords = "";
      this.pagination.current = 1;
      this.pagination.pageSize = 20;
      this.selectAllName = "全选";
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
:deep(.ant-pagination) {
  margin: 8px 0;
}
</style>