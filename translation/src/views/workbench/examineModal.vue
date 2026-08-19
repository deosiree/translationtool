<template>
  <CustomModal :visible="visible" :modalTitle="modalTitle" :modalWidth="modalWidth" :fullFlag="true" :okLoading="saveLoading" okText="保存"
    @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose" @setTableHeight="setTableHeight">
    <div class="content">
      <WorkbenchTaskInfo :task="task">
        <template #extra>
          <RulesDropdown :options="rulesOptions" @update:options="rulesOptions"></RulesDropdown>
        </template>
      </WorkbenchTaskInfo>
      <WorkbenchFormBar>
        词条：
        <a-textarea v-model:value="keyWords" style="width:300px" size="small" placeholder='请输入词条搜索' :auto-size="{ minRows: 1 }" />
        <span style="margin-left:10px">词条状态：</span>
        <EntryStateSelect :entryState="entryState" @update:entryState="entryState = $event" :size="'small'" :style="'width: 300px'"
          :filter="new Set(['0'])" />
        <a-button type="primary" size="small" style="margin-left:8px" @click="getTaskEntry">查询</a-button>
        <!-- <a-button type="primary" size="small" style="margin-left:8px" @click="selectAll">{{selectAllName}}</a-button> -->
        <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="pass">通过</a-button>
        <a-button type="primary" size="small" style="margin-left:8px" class="rejectBtn" @click="reject">驳回</a-button>
        <a-button type="primary" size="small" danger style="margin-left:8px" @click="deleteTaskEntry">删除</a-button>
        <!-- <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="aggregation">聚合</a-button>
                <a-button type="primary" size="small" style="margin-left:8px" class="yellowBtn" @click="cancelAggregation">取消聚合</a-button> -->
        <WorkbenchActionGroup inline-offset>
          <WorkbenchColumnActions
            v-model="checkedColumn"
            :columns="columnSettingsList"
            :overlay-style="overlayStyle"
            col-pref-name="colPref-examineModal"
            :normal-width="100"
            :need-filter="false"
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
        </WorkbenchActionGroup>
      </WorkbenchFormBar>
      <div class="select">
        <WorkbenchLanguageFilter
          v-model="filterLanguage"
          @change="filterLanguageChange"
        />
      </div>
      <a-table bordered class="ant-table-striped table-cell-overflow" :columns="columns" :data-source="dataSource" :row-selection="{ selectedRowKeys: selectedRowKeys, 
                onChange: onSelectChange,
                checkStrictly: false,
                selections:[
                    {key:'selectAll',text:'全部选择',onSelect:selectAllEntry},
                    {key:'clearAll',text:'取消选择',onSelect:clearAllEntry}
                ]
            }" :row-key="record => record.id" :scroll="tableHeight" :pagination='pagination' :loading="loading" :rowClassName="getRowClassName"
        :customRow="doubleClick" :expandIconColumnIndex="2" ref="workTable" @resizeColumn="handleResizeColumn" @change="handleTableChange">
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
          <template v-else-if="column.dataIndex === 'diFileName'">
            <template v-if="editableData[record.id]">
              <InputIME
                :value="editableData[record.id][column.dataIndex]"
                @update:value="(val) => onCellInput(val, record, column)"
              />
            </template>
            <template v-else>
              <CellOverflowTooltip :content="formatCellText(text)" />
            </template>
          </template>
          <template v-else-if="column.dataIndex === 'tag'">
            <template v-if="editableData[record.id]">
              <InputIME
                :value="editableData[record.id][column.dataIndex]"
                @update:value="(val) => onCellInput(val, record, column)"
              />
              <a-tooltip placement="top">
                <template #title>
                  <span>多个Tag按分号分割！</span>
                </template>
                <InfoCircleOutlined style="margin-left:3px" />
              </a-tooltip>
            </template>
            <template v-else>
              <CellOverflowTooltip :content="formatTagText(text)">
                <span>
                  <a-tag v-for="(tag,index) in companyCut(text)" :key="index" color="cyan" class="tag-content">
                    {{tag}}
                  </a-tag>
                </span>
              </CellOverflowTooltip>
            </template>
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
              @pressEnter="handleSearch(selectedKeys, confirm, column.dataIndex)" />
            <a-button type="primary" size="small" style="width: 90px; margin-right: 8px"
              @click="handleSearch(selectedKeys, confirm, column.dataIndex)">
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
    <template v-slot:leftBottomBtn>
      <a-button type="primary" size="small" style="margin-left:8px;float:left" class="resetBtn" @click="aggregation">聚合</a-button>
      <a-button type="primary" size="small" style="margin-left:8px;float:left" class="yellowBtn" @click="cancelAggregation">取消聚合</a-button>
    </template>
  </CustomModal>
  <CustomModal :visible="rejectReasonVisible" modalTitle="驳回原因" @handleClose="rejectReasonClose" @handleOK="rejectReasonOK"
    @afterClose="rejectReasonAfterClose">
    <div style="width:100%;height:100%">
      <a-form ref="exportForm" name="custom-validation" :model="rejectReason">
        <a-form-item label="驳回原因" name="reason">
          <a-textarea v-model:value="rejectReason.reason" placeholder="请输入驳回原因" allow-clear />
        </a-form-item>
      </a-form>
    </div>
  </CustomModal>
</template>
<script>
import "@/assets/style/common.less";
import CustomModal from "@/components/modal/index.vue";
import RulesDropdown from "@/components/Dropdown/rulesDropdown.vue";
import EntryStateSelect from "@/components/select/entryStateSelect.vue";
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
import {
  CheckOutlined,
  CloseOutlined,
  ExclamationCircleOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
  SettingOutlined,
  SearchOutlined,
  InfoCircleOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import commonParam, { workbenchParams } from "@/constants/commonParam.js";
import { applyTable, syncColumnsFromPref as applyTableColumnsFromPref } from "@/components/ColumnFilter";
import { filterWbColsForCtx } from "@/components/ColumnFilter/columnBuilder.js";
import { wbAllCols, wbPresets } from "@/constants/commonParam.js";
import {
  WorkbenchFormBar,
  WorkbenchActionGroup,
  WorkbenchTaskInfo,
  WorkbenchColumnActions,
  WorkbenchLanguageFilter,
} from "@/components/Workbench";
import { filterLanguageChange as applyLanguageFilter } from "@/composables/workbench/useLanguageFilter";
import {
  handleResizeColumn,
  getRowClassName,
} from "@/utils/tableUtils";
import {
  selectAllEntry as selectAllEntryUtil,
  clearAllEntry as clearAllEntryUtil,
  onSelectChange as onSelectChangeUtil,
} from "@/utils/selectionUtils";
import { interpretation2value } from "@/utils/translationUtils";
import { setModalAriaHidden } from "@/utils/domUtils";
import { filter_arr, filter_arr_keys } from "@/utils/dataStructureUtils";
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
import { defineComponent, ref, createVNode } from "vue";
export default {
  components: {
    CheckOutlined,
    CloseOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    SettingOutlined,
    SearchOutlined,
    InfoCircleOutlined,
    CustomModal,
    RulesDropdown,
    EntryStateSelect,
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
    WorkbenchLanguageFilter,
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
      modalWidth: "70%",
      task: {},
      keyWords: "",
      // tableHeight: { x: "100%", y: "415px" },
      tableHeight: { x: "max-content", y: "415px" },
      loading: false,
      columns: [],
      dataSource: [],
      allData: [],
      selectedRowKeys: [],
      selectedRows: [],
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
      filteredData: [],
      selectAllName: "全选",
      saveLoading: false,
      rejectReasonVisible: false,
      rejectReason: {
        reason: "",
      },
      filterLanguage: null,
      filterSource: [],
    };
  },
  created() {},
  mounted() {  },
  computed: {
    editableTextAreaColumns() {
      const dedicatedInputCols = ["diFileName", "tag"];
      return [
        ...(this.editList_needValidate || []),
        ...(this.editList || []),
      ].filter((col) => !dedicatedInputCols.includes(col));
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
    // 获取待审核词条
    getTaskEntry() {
      let params = {
        taskID: this.task.id,
        entryState: this.entryState,
        entry: this.keyWords,
      };
      this.loading = true;
      getEntryInfoList(params, [])
        .then((res) => {
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
            // console.log("打印词条", item);
          });
          this.allData = this.dataSource;
          this.loading = false;
          // this.select()
        })
        .catch((err) => {
          this.loading = false;
          message.error("1", err.message);
          // console.log("err1", err);
        });
    },
    async handleOK() {
      this.saveLoading = true;
      this.loading = true;

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
          if (this.editableData[key][this.task.transMap.value] != null) {
            this.editableData[key][this.task.transMap.state] = "1";
          }
          this.dataSource[index] = cloneDeep(this.editableData[key]);
        }
      }
      this.editableData = {};

      let params = {
        taskID: this.task.id,
      };
      let updateArr = [];
      let okArr = [];
      let arrCount = {
        updateNum: 0,
        updateChildNum: 0,
        okNum: 0,
        okChildNum: 0,
      };
      const promises = [];
      let messageTextParts = [];

      this.dataSource.forEach((item) => {
        if (item.auditState === 1 || item.auditState === 0) {
          updateArr.push(item);
          arrCount.updateNum++;
          if (item.children && item.children.length > 0) {
            arrCount.updateChildNum += item.children.length;
          }

          if (item.auditState === 1) {
            item.entryState = 3;
            okArr.push(item);
            arrCount.okNum++;
            if (item.children && item.children.length > 0) {
              arrCount.okChildNum += item.children.length;
            }
          } else if (item.auditState === 0) {
            item.entryState = 2;
          }
        }
      });

      if (arrCount.updateNum > 0) {
        const updatePromise = updateEntryList(params, updateArr)
          .then((res) => {
            const successCount = arrCount.updateNum - res.data.totalNum; // 成功保存的数量
            const okCount = filter_arr(okArr, res.data.list).length; // 保存，是通过的数量
            const rejectCount = successCount - okCount; // 保存，是驳回的数量
            const failCount = res.data.totalNum;
            if (successCount > 0) {
              let text = `保存成功${successCount}条`;
              if (okCount > 0 || rejectCount > 0) {
                let textChild = [];
                if (okCount > 0) {
                  let textChild1 = `通过${okCount}条`;
                  if (arrCount.okChildNum > 0) {
                    textChild1 += `（聚合${arrCount.okChildNum}条）`;
                  }
                  textChild.push(textChild1);
                }
                if (rejectCount > 0) {
                  let textChild2 = `驳回${rejectCount}条`;
                  let rjtCount = arrCount.updateChildNum - arrCount.okChildNum;
                  if (rjtCount > 0) {
                    textChild2 += `（聚合${rjtCount}条）`;
                  }
                  textChild.push(textChild2);
                }
                text += `———${textChild.join("，")}`;
              }
              messageTextParts.push(text);
            }
            if (failCount > 0) {
              messageTextParts.push(`保存失败${failCount}条`);
            }
            // if (arrCount.updateChildNum > 0) {
            //   messageTextParts.push(
            //     `其中聚合的数据${arrCount.updateChildNum}条`
            //   );
            // }
            // console.log("arrCount", arrCount, updateArr.length, updateArr);

            // 从 updateArr 中移除保存失败的数据
            const successfulUpdateArr = filter_arr(updateArr, res.data.list);
            // 从 this.dataSource 中移除保存成功的数据
            this.dataSource = filter_arr(this.dataSource, successfulUpdateArr);
            // 从this.selectedRows 中移除保存成功的数据
            this.selectedRows = filter_arr(
              this.selectedRows,
              successfulUpdateArr
            );
            this.selectedRowKeys = filter_arr_keys(
              this.selectedRowKeys,
              successfulUpdateArr
            );

            this.getTaskEntry();
          })
          .catch((err) => {
            message.error("保存失败！", err.message);
          });
        promises.push(updatePromise);
      }

      Promise.all(promises)
        .then(() => {
          if (messageTextParts.length > 0) {
            message.success("数据已保存！" + messageTextParts.join("，"));
          }
          this.allData = this.dataSource;
          // 清空选中
          this.selectedRows = [];
          this.selectedRowKeys = [];
          this.selectedRowIndex = null;
          if (this.dataSource.length == 0) {
            // 如果没有待处理的数据就自动关闭弹窗
            this.handleClose();
          }
        })
        .finally(() => {
          this.saveLoading = false;
          this.loading = false;
        });
    },
    handleClose() {
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.selectedRowIndex = null;
      this.$emit("handleClose");
    },
    getRowClassName(record, index) {
      return getRowClassName(record, index, this.selectedRowIndex);
    },
    handleResizeColumn,
    // 模糊查询
    select() {
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
    },
    onSelectChange(selectedRowKeys, selectedRows) {
      onSelectChangeUtil(this, selectedRowKeys, selectedRows);
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
    // 通过按钮点击事件
    pass() {
      this.selectedRows.forEach((item) => {
        item.auditState = 1;

        if (item.children && item.children.length > 0) {
          item.children.forEach((child) => {
            child.auditState = item.auditState;
          });
        }
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
    // 校验输入数据的长度
    vilidFildLength(record, language) {
      // console.log("校验语种：",language)
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
            // console.log("maxLength = record.maxLength;", record);
          } else {
            return Promise.resolve();
          }
        } else {
          maxLength = this.classifyLimit[record.classfy1][type];
          // console.log("maxLength = this.classifyLimit[record.classfy1][type];",this.classifyLimit,record.classfy1,type);
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
        let length = byteLength(value);
        if (length > maxLength) {
          return Promise.reject("允许最大字符数为" + maxLength + "！");
        }
        return Promise.resolve();
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
      this.editableData = {};
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.keyWords = "";
      this.pagination.current = 1;
      this.pagination.pageSize = 20;
      this.selectAllName = "全选";

      this.clearFilters();
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
    // 列筛选
    handleSearch(selectedKeys, confirm, dataIndex) {
      confirm();
      this.state.searchText = selectedKeys[0];
      this.state.searchedColumn = dataIndex;
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
    // 语种切换
    filterLanguageChange() {
      applyLanguageFilter(this);
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
          // 若校验不通过，会调用 addEdit 方法将该词条设为编辑状态，并对其表单进行校验
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
                // message.error("3", err.message);
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
    // 表格change事件
    handleTableChange(pagination, filters) {
      this.filters = filters;
      for (let key in filters) {
        this.columns.forEach((col) => {
          if (col.dataIndex === key) {
            col.filteredValue = filters[key];
          }
        });
      }
      // 获取筛选后的数据
      let isExistData = this.dataSource.filter((item) => {
        return filters.isExist && filters.isExist.includes(item.isExist);
      });
      let sourceData = this.dataSource.filter((item) => {
        return (
          filters.entrySource && item.entrySource.includes(filters.entrySource)
        );
      });
      this.filteredData = this.intersection(isExistData, sourceData);
    },
    // 两个数组取并集
    intersection(nums1, nums2) {
      if (nums1.length === 0) {
        return nums2;
      }
      if (nums2.length === 0) {
        return nums1;
      }
      let a = new Set(nums1);
      let b = new Set(nums2);
      let arr = Array.from(new Set([...b].filter((x) => a.has(x))));
      return arr;
    },
    // 清空表格筛选条件
    clearFilters() {
      if (this.filters) {
        for (let key in this.filters) {
          this.columns.forEach((col) => {
            if (col.dataIndex === key) {
              col.filteredValue = null;
            }
          });
        }
      }
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
        item.auditSuggess = this.rejectReason.reason;
        if (item.children && item.children.length > 0) {
          item.children.forEach((child) => {
            child.auditState = item.auditState;
            child.auditSuggess = this.rejectReason.reason;
          });
        }
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