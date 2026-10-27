<template>
  <div
    ref="box"
    class="box"
  >
    <a-row
      type="flex"
      class="workbench-row"
    >
      <a-col
        v-show="!leftPanelCollapsed"
        flex="280px"
        class="cardBox"
      >
        <a-card
          hoverable
          :class="
            activeCard === 1 ? 'handleCard activeHandleCard' : 'handleCard'
          "
          @click="clickCard(1)"
        >
          <div class="title">待办事项</div>
          <div class="logo"><img src="../../assets/workIcon/handle.png" /></div>
          <div class="data">
            <span>{{ toDoNum }}</span
            ><span>条</span>
            <!-- <a-button type="primary" ghost size="small" @click="clickCard(1)">查看</a-button> -->
          </div>
        </a-card>
        <a-card
          hoverable
          :class="
            activeCard === 2
              ? 'processedCard activeProcessedCard'
              : 'processedCard'
          "
          @click="clickCard(2)"
        >
          <div class="title">已办事项</div>
          <div class="logo">
            <img src="../../assets/workIcon/processed.png" />
          </div>
          <div class="data">
            <span>{{ finishNum }}</span
            ><span>条</span>
            <!-- <a-button type="primary" ghost size="small" @click="clickCard(2)">查看</a-button> -->
          </div>
        </a-card>
        <!-- <a-card hoverable :class="activeCard === 3 ? 'exportCard activeExportCard' : 'exportCard'">
          <div class="title">可导出词条</div>
          <div class="logo"><img src="../../assets/workIcon/export.png" /></div>
          <div class="data">
            <span>20</span><span>条</span>
            <a-button type="primary" ghost size="small" @click="clickCard(3)">查看</a-button>
          </div>
        </a-card> -->
      </a-col>
      <a-col
        flex="16px"
        class="left-panel-rail"
      >
        <button
          type="button"
          class="left-panel-toggle"
          :title="leftPanelCollapsed ? '展开待办/已办' : '收起待办/已办'"
          @click.stop="toggleLeftPanel"
        >
          <DoubleRightOutlined v-if="leftPanelCollapsed" />
          <DoubleLeftOutlined v-else />
        </button>
      </a-col>
      <a-col
        flex="auto"
        class="workbench-main"
      >
        <div class="dataBox">
          <SearchBox
            ref="search"
            @change="setTableHeight"
          >
            <template v-slot:form>
              <a-form
                class="workbench-search-form"
                :model="search"
                name="horizontal_login"
                layout="inline"
                autocomplete="off"
                :label-col="labelCol"
              >
                <a-form-item
                  label="任务名称"
                  name="name"
                >
                  <a-input
                    v-model:value="search.name"
                    placeholder="请输入任务名称"
                  ></a-input>
                </a-form-item>
                <a-form-item
                  label="产品名称"
                  name="productName"
                >
                  <a-input
                    v-model:value="search.productName"
                    placeholder="请输入产品名称"
                  ></a-input>
                </a-form-item>
                <a-form-item
                  label="翻译语种"
                  name="translateType"
                >
                  <a-select
                    v-model:value="search.translateType"
                    placeholder="请选择翻译语种"
                    :fieldNames="{ label: 'name', value: 'name' }"
                    :options="translateTypes"
                    allowClear
                  >
                  </a-select>
                </a-form-item>
                <a-form-item
                  label="执行部门"
                  name="department"
                >
                  <a-input
                    v-model:value="search.department"
                    placeholder="请输入执行部门"
                  ></a-input>
                </a-form-item>
                <a-form-item
                  label="创建人"
                  name="creator"
                >
                  <a-input
                    v-model:value="search.creator"
                    placeholder="请输入创建人"
                  ></a-input>
                </a-form-item>
                <!-- <a-form-item label="词条审核员" name="auditor">
                  <a-input v-model:value="search.auditor" placeholder="请输入词条审核员" </a-input>
                </a-form-item> -->
              </a-form>
            </template>
            <template v-slot:operate>
              <a-button
                type="primary"
                size="middle"
                class="resetBtn"
                @click="reset"
                >重置</a-button
              >
              <a-button
                type="primary"
                size="middle"
                @click="query"
                >查询</a-button
              >
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
                v-if="true"
                class="workbench-operate-btns"
              >
                <SelectionToolbarButton
                  v-if="
                    $currentDepartment &&
                    $currentDepartment.ops.has('needBranch')
                  "
                  text="取消选择"
                  preset="default"
                  :disabled="selectEntry.length === 0"
                  @click="clearAllEntry"
                />
                <a-button
                  v-if="
                    $currentDepartment &&
                    $currentDepartment.ops.has('needBranch')
                  "
                  type="primary"
                  size="small"
                  @click="
                    isTreeOr2D == 'tree'
                      ? (isTreeOr2D = '2D')
                      : (isTreeOr2D = 'tree')
                  "
                >
                  {{ isTreeOr2D == "tree" ? "平铺" : "层级" }}展示</a-button
                >
                <a-button
                  type="primary"
                  size="small"
                  @click="SelectTranslateType"
                  :disabled="selectedRows.length === 0"
                  >更改翻译语种</a-button
                >
                <a-button
                  type="primary"
                  size="small"
                  @click="openBatchPreTranslate"
                  :disabled="selectedRows.length === 0"
                  >批量预翻译</a-button
                >
                <a-modal
                  style="width: 320px"
                  class="choiceLang"
                  centered
                  title="选择语种"
                  :visible="translateTypeVisible"
                  @ok="confirmTranslateType"
                  @cancel="cancelTranslateType"
                >
                  <a-select
                    v-model:value="selectedLanguage"
                    style="width: 100%"
                    placeholder="请选择内容"
                    :options="translateTypes"
                    :fieldNames="{ label: 'name', value: 'name' }"
                    allowClear
                  >
                  </a-select>
                  <template #footer>
                    <div style="text-align: center">
                      <a-button @click="cancelTranslateType">取消</a-button>
                      <a-button
                        type="primary"
                        @click="confirmTranslateType"
                        >确定</a-button
                      >
                    </div>
                  </template>
                </a-modal>
              </div>
            </template>
            <template v-slot:data>
              <div class="workbench-main-table">
                <TableViewport
                  bordered
                  adaptive-columns
                  class="ant-table-striped"
                  :columns="columns"
                  :adaptive-column-keys="['name', 'productName']"
                  :data-source="dataSource"
                  :row-key="(record) => record.id"
                  :scroll="tableHeight"
                  :pagination="pagination"
                  :loading="tableLoading"
                  :rowClassName="getRowClassName"
                  childrenColumnName="child"
                  ref="workTable"
                  :row-selection="{
                    columnWidth: 48,
                    selectedRowKeys: tableSelectedRowKeys,
                    onSelect: onSelect,
                    onSelectAll: onSelectAll,
                  }"
                  :customRow="customRow"
                >
                  <template #bodyCell="{ column, record, text }">
                    <template
                      v-if="
                        column.dataIndex === 'index' &&
                        isTreeOr2D == 'tree' &&
                        record.isBranch
                      "
                    >
                      <span style="color: blue">
                        {{ text }}
                      </span>
                    </template>
                    <template v-if="column.dataIndex === 'state'">
                      <TaskStateBadge
                        type="normal"
                        :taskState="text"
                      />
                    </template>
                    <template v-if="column.dataIndex === 'name'">
                      <!-- <span style="position: relative; display: inline-block;">
                        {{ text }}
                        <a-badge color="#ff0000" v-if="record.isHighlighted" style="position: absolute;top: -9px;right: -9px;z-index: 1;" />
                      </span> -->
                      <div style="display: flex; gap: 5px">
                        <span>{{ text }}</span>
                        <a-badge
                          :count="record.num__total"
                          :overflow-count="99"
                          v-if="record.num__total > 0"
                        />
                      </div>
                    </template>
                  </template>
                  <template #expandIcon="props">
                    <span
                      v-if="
                        props.record.child != null &&
                        props.record.child.length > 0
                      "
                    >
                      <div
                        v-if="props.expanded"
                        style="display: inline-block; margin-right: 10px"
                        @click="
                          (e) => {
                            handleBranchExpand(
                              props.record,
                              props.expanded,
                              e,
                              props.onExpand,
                              props
                            );
                          }
                        "
                      >
                        <CaretDownOutlined />
                        <!-- 展开状态的向下三角形图标 -->
                      </div>
                      <div
                        v-else
                        style="display: inline-block; margin-right: 10px"
                        @click="
                          (e) => {
                            handleBranchExpand(
                              props.record,
                              props.expanded,
                              e,
                              props.onExpand,
                              props
                            );
                          }
                        "
                      >
                        <CaretRightOutlined />
                        <!-- 收起状态的向右三角形图标 -->
                      </div>
                    </span>
                    <span
                      v-else
                      style="margin-right: 23px"
                    ></span>
                    <!-- 无子记录时的占位 -->
                  </template>
                </TableViewport>
              </div>
            </template>
          </DataBox>
          <OperationArea
            ref="operationArea"
            :title="operationAreaTitle"
            :height="operationAreaHeight"
            v-if="showOperationArea"
            @close="closeOperationArea"
          >
            <template v-slot:content>
              <TimeLine
                ref="timeLineRef"
                :currentTask="currentTask"
                :showButton="timeLineBtn"
                @importEntry="importEntry"
                @examineEntry="examineEntry"
                @translateEntry="translateEntry"
                @examineTranslate="examineTranslate"
                @refresh="getTask"
                @archiveEntry="archiveEntry"
              >
              </TimeLine>
            </template>
          </OperationArea>
        </div>
      </a-col>
    </a-row>
  </div>
  <ImportModal
    ref="import"
    :visible="importVisible"
    :currentTask="currentTask"
    :classifyLimit="classifyLimit"
    @handleClose="importClose"
    @afterSave="refreshTaskBadges"
  />
  <ExamineModal
    ref="examine"
    :visible="examineVisible"
    :currentTask="currentTask"
    :classifyLimit="classifyLimit"
    :modalTitle="examineTitle"
    @handleClose="examineClose"
    @afterSave="refreshTaskBadges"
  />
  <TranslateModal
    ref="translate"
    :visible="translateVisible"
    :currentTask="currentTask"
    :classifyLimit="classifyLimit"
    @handleClose="translateClose"
    @afterSave="refreshTaskBadges"
  />
  <ExamineTranslateModal
    ref="examineTranslate"
    :visible="examineTranslateVisible"
    :currentTask="currentTask"
    :classifyLimit="classifyLimit"
    @handleClose="examineTranslateClose"
    @afterSave="refreshTaskBadges"
  />
  <ArchiveModal
    ref="archiveModalRef"
    :visible="archiveVisible"
    :currentTask="currentTask"
    @handleClose="archiveClose"
    @refresh="refreshTask"
  />
  <BatchPreTranslateModal
    :visible="batchPreTranslateVisible"
    :tasks="selectedRows"
    @update:tasks="syncBatchTasks"
    @handleClose="batchPreTranslateVisible = false"
    @close="batchPreTranslateVisible = false"
    @complete="onBatchPreTranslateComplete"
  />
  <BatchProgressOverlay />
</template>
<script>
import { message, Modal } from "ant-design-vue";
import locale from "ant-design-vue/es/date-picker/locale/zh_CN";
import SearchBox from "@/components/search/searchBox.vue";
import SearchForm from "@/components/search/searchForm.vue";
import DataBox from "@/components/dataBox/index.vue";
import OperationArea from "@/components/operationArea/index.vue";
import TimeLine from "@/components/timeLine/index.vue";
import TaskStateBadge from "@/components/stateBadge/taskStateBadge.vue";
import ImportModal from "@/views/workbench/importModal.vue";
import ExamineModal from "@/views/workbench/examineModal.vue";
import TranslateModal from "@/views/workbench/translateModal.vue";
import ExamineTranslateModal from "@/views/workbench/examineTranslateModal.vue";
import ArchiveModal from "@/views/workbench/archiveModal.vue";
import TableViewport from "@/components/table/TableViewport/TableViewport.vue";
import SelectionToolbarButton from "@/components/selectToolBar/SelectionToolbarButton.vue";
import BatchPreTranslateModal from "@/views/workbench/components/BatchPreTranslateModal.vue";
import BatchProgressOverlay from "@/views/workbench/components/BatchProgressOverlay.vue";
import {
  SendOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
  DoubleLeftOutlined,
  DoubleRightOutlined,
} from "@ant-design/icons-vue";
import { getToDoTaskInfo, getFinishTaskInfo , updateTaskInfo , getTaskPending } from "@/http/api/task";
import { getClassfy } from "@/http/api/entryManage";

import { getEntryInfoList } from "@/http/api/workbench";
import { getProduct } from "@/http/api/product";

import { fetchLanguages } from "@/composables/useLanguageCache";
import { setTableHeight } from "@/utils/tableUtils";
import { setModalAriaHidden } from "@/utils/domUtils";
import {
  collectAllPages,
  flattenRowsById,
  refreshSelectedEntriesById,
} from "@/utils/selectionUtils";
import { createQueryVersionGuard } from "@/utils/queryVersionGuard";
import { useSelectedEntries } from "@/components/selectToolBar/composables/useSelectedEntries";
import { useSelectionOperations } from "@/components/selectToolBar/composables/useSelectionOperations";
import commonParam from "@/constants/commonParam";
import {
  loading,
  startLoading,
  endLoading,
  withLoading,
} from "@/composables/useLoading";
export default {
  setup() {
    return { loading };
  },
  components: {
    SearchBox,
    SearchForm,
    DataBox,
    OperationArea,
    TimeLine,
    TaskStateBadge,
    ImportModal,
    ExamineModal,
    TranslateModal,
    ExamineTranslateModal,
    ArchiveModal,
    TableViewport,
    SelectionToolbarButton,
    BatchPreTranslateModal,
    BatchProgressOverlay,
    SendOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    DoubleLeftOutlined,
    DoubleRightOutlined,
  },
  data() {
    return {
      name: "workbench",
      leftPanelCollapsed: false,
      // 汉化包
      locale: locale,
      toDoNum: 0,
      finishNum: 0,
      labelCol: { style: { width: "84px" } },
      lastSearch: {},
      search: {
        name: "",
        productName: "",
        translateType: null,
        department: "",
        auditor: "",
        creator: "",
      },
      tableHeight: { x: "100%", y: 0 },
      currentPageBranch: 0, // 当前页的分支数量
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 56,
          customRender: (record) => {
            if (this.isTreeOr2D != "tree") {
              const currentIndex =
                record.index +
                1 +
                this.pagination.pageSize * (this.pagination.current - 1);
              return currentIndex;
            }
          },
          fixed: "left",
          index: 0.1,
        },
        {
          title: "任务名称",
          dataIndex: "name",
          align: "left",
          width: 152,
          minWidth: 50,
          adaptive: true,
          fixed: "left",
          resizable: true,
          index: 1,
        },
        // {title: '任务状态',dataIndex: 'state',align:'center',width:80,fixed: 'left',resizable: true,index:1},
        {
          title: "产品名称",
          dataIndex: "productName",
          align: "left",
          width: 136,
          minWidth: 50,
          adaptive: true,
          resizable: true,
          index: 2,
        },
        {
          title: "产品版本",
          dataIndex: "versionName",
          align: "center",
          width: 112,
          minWidth: 50,
          resizable: true,
          index: 3,
        },
        // {title: '执行部门',dataIndex: 'department',align:'center',width:100,ellipsis: true,resizable: true,index: 3},
        {
          title: "翻译语种",
          dataIndex: "translateType",
          align: "center",
          width: 88,
          minWidth: 50,
          ellipsis: true,
          resizable: true,
          index: 4,
        },
        {
          title: "描述",
          dataIndex: "description",
          align: "left",
          width: 128,
          minWidth: 50,
          ellipsis: true,
          resizable: true,
          index: 5,
        },
        {
          title: "创建人",
          dataIndex: "creator",
          align: "center",
          width: 72,
          minWidth: 50,
          ellipsis: true,
          resizable: true,
          index: 6,
        },
        {
          title: "下发时间",
          dataIndex: "deliveryTime",
          align: "center",
          width: 104,
          minWidth: 50,
          ellipsis: true,
          resizable: true,
          index: 7,
        },
        // {title: '操作',dataIndex: 'operation',align:'center',width:50,fixed: 'right',},
      ],
      dataSource: [],
      selectedRows: [],
      selectedRowKeys: [],
      selectedRowIndex: null,
      selectEntry: [], // 已选任务
      queryGuard: createQueryVersionGuard(),
      taskTotalGuard: createQueryVersionGuard(),
      selectionOperations: useSelectionOperations(),
      selectedEntryOperations: useSelectedEntries(),
      selectionScope: "",
      activeQueryParams: {},
      queryLoading: false,
      selectedLanguage: null, // 新增：用于存储用户选择的语种
      translateTypes: [], // 新增：下拉框的语种选项
      translateTypeVisible: false, // 新增：控制语种选择弹窗的显示与隐藏
      currentTask: {},
      timeLineBtn: true,
      activeCard: 1,
      dataHeight: 490,
      tableTitle: "待办事项列表",
      operationAreaTitle: "流程操作区",
      operationAreaHeight: 190,
      showOperationArea: false,
      importVisible: false,
      examineVisible: false,
      examineTitle: "",
      translateVisible: false,
      examineTranslateVisible: false,
      archiveVisible: false,
      classifyLimit: {},
      lastClassifyProductId: null, // getClassfy 去重：上次已加载分类的产品ID
      isTreeOr2D: null, // 树展示（层级）/平铺展示
      pagination: {
        showSizeChanger: true,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
      pageChangeSearch: {},
      expandSource: [], // 记录展开了的分支信息
      batchPreTranslateVisible: false, // 批量预翻译弹窗
    };
  },
  computed: {
    /**
     * 合并列表查询与页面操作遮罩状态。
     * @returns {boolean} 当前主表是否显示 loading。
     */
    tableLoading() {
      return this.queryLoading || this.loading;
    },
    /**
     * 返回当前数据页的已提交选择和待处理全量操作投影。
     * @returns {Array<string|number>} 当前可见行的选中键。
     */
    tableSelectedRowKeys() {
      // 读取守卫版本，使异步全量操作开始/结束时立即刷新投影。
      this.selectionOperations.revision;
      const selected = new Set(this.selectedRowKeys);
      const visibleRows = flattenRowsById(this.dataSource, "child");
      visibleRows.forEach((row) => {
        const isSelected = this.selectionOperations.project(
          row.id,
          this.selectionScope,
          selected.has(row.id)
        );
        if (isSelected) selected.add(row.id);
        else selected.delete(row.id);
      });
      return Array.from(selected);
    },
  },
  mounted() {
    let _this = this;
    this.$nextTick(() => {
      this.user = this.$store.state.user;
      if (
        !this.$currentDepartment ||
        !this.$currentDepartment.ops.has("needBranch")
      ) {
        this.isTreeOr2D = "2D";
      } else {
        // 增加分支列
        this.columns.splice(1, 0, {
          title: "分支",
          dataIndex: "codeBranch",
          align: "left",
          width: 108,
          minWidth: 50,
          fixed: "left",
          resizable: true,
          index: 0.5,
        });

        // 读取本地存储的用户偏好
        // localStorage.removeItem("display-workbenchIndex");
        const storedDisplay = localStorage.getItem("display-workbenchIndex");
        if (storedDisplay) {
          // console.log("读取到用户偏好:", storedDisplay, localStorage);
          this.isTreeOr2D = JSON.parse(storedDisplay);
        } else {
          // console.log(
          //   "读不到用户偏好，使用默认值:",
          //   this.isTreeOr2D,
          //   localStorage
          // );
          this.isTreeOr2D = "2D";
        }
      }

      // 首次查询
      if (this.isTreeOr2D == "tree") {
        if (this.pagination.current != 1 || this.pagination.pageSize != 100) {
          this.pageChange(1, 100);
        }
      } else {
        // 否则不是层级展示
        if (this.pagination.current != 1 || this.pagination.pageSize != 20) {
          this.pageChange(1, 20);
        } else {
          this.getTaskByCondition(this.pageChangeSearch);
        }
      }

      this.init();

      /** 控制table的高度 */
      window.onresize = function () {
        _this.setTableHeight();
      };
    });
    this.getLanguage();
  },
  /**
   * 组件卸载时使当前列表、数量和全量选择请求失效。
   * @returns {void} 无返回值。
   */
  unmounted() {
    this.queryGuard.invalidate();
    this.taskTotalGuard.invalidate();
    this.selectionOperations.invalidate();
    //注销window.onresize事件
    window.onresize = null;
  },
  watch: {
    isTreeOr2D: {
      handler(newVal, oldVal) {
        if (
          this.$currentDepartment &&
          this.$currentDepartment.ops.has("needBranch") &&
          newVal != null &&
          oldVal != null && // 初始化（null→值）不触发：mounted 已做首次查询，避免重复
          newVal !== oldVal
        ) {
          localStorage.setItem(
            "display-workbenchIndex",
            JSON.stringify(newVal)
          ); // 存储用户偏好
          if (newVal == "tree") {
            if (
              this.pagination.current != 1 ||
              this.pagination.pageSize != 100
            ) {
              this.pageChange(1, 100);
            }
          } else {
            if (
              this.pagination.current != 1 ||
              this.pagination.pageSize != 20
            ) {
              this.pageChange(1, 20);
            } else {
              this.query(); // 分页中自带查询，但是不修改分页信息时，刷新页面后需要手动查询一次
            }
          }
        }
      },
      immediate: true,
    },
  },
  methods: {
    // 处理分支展开/折叠事件
    async handleBranchExpand(record, isExpanded, event, onExpand, props) {
      // 阻止事件冒泡，避免触发其他点击事件
      if (event) {
        event.stopPropagation();
      }

      // 设置加载状态（try/finally 保证计数不泄漏）
      startLoading();
      try {
        // 调用正确的onExpand函数（通过props传递进来的）
        if (typeof onExpand === "function") {
          onExpand(record, event);
        }

        // 记录展开状态的props（用于查询时重新计算状态）
        if (isExpanded) {
          // 折叠时，从记录中移除
          this.expandSource = this.expandSource.filter(
            (item) => item.id !== record.id
          );
        } else {
          // 展开时，添加到记录中
          this.expandSource.push({
            id: record.id,
            record: record,
            isExpanded: true,
          });
        }

        // 在展开时计算该分支下任务的未完成状态
        await this.getBranchPending(); // 只获得展开分支的任务执行状态
      } finally {
        // 清除加载状态
        endLoading();
      }
    },
    /**
     * 刷新展开分支计数，可在提交新列表前对局部快照执行。
     * @param {Array<Object>} sourceRows - 当前或待提交的任务列表。
     * @param {number} token - 可选的列表查询版本。
     * @returns {Promise<void>} 计数刷新完成后结束。
     */
    async getBranchPending(sourceRows = this.dataSource, token) {
      for (const expanded of this.expandSource) {
        const branch = sourceRows.find((item) => item.id === expanded.id);
        if (!branch?.child?.length) continue;
        const { tasks, totalNum } = await this.getTaskPending(branch.child);
        if (token != null && !this.queryGuard.isCurrent(token)) return;
        branch.child = tasks;
        branch.num__total = totalNum;
        if (sourceRows === this.dataSource) expanded.record = branch;
      }
    },
    // 获取所有分支的任务执行状态
    async getAllBranchPending() {
      for (let i = 0; i < this.dataSource.length; i++) {
        if (this.dataSource[i].child && this.dataSource[i].child.length > 0) {
          const { tasks, totalNum } = await this.getTaskPending(
            this.dataSource[i].child
          );
          this.dataSource[i].child = tasks;
          this.dataSource[i].num__total = totalNum;
        }
      }
      // console.log("获取所有分支的任务执行状态", this.dataSource);
    },
    // 获取词条数量
    async getTaskPending(tasks) {
      const taskIds = tasks.map((item) => item.id);
      let totalTaskPendingNum = 0;

      await getTaskPending(taskIds).then((res) => {
        for (let i = 0; i < res.data.length; i++) {
          const item = res.data[i];
          const taskIndex = tasks.findIndex((t) => t.id == item.taskID);
          if (taskIndex !== -1) {
            tasks[taskIndex].num__total = item.totalCounts;
            tasks[taskIndex].num_entryExamine = item.entryExamineCounts;
            tasks[taskIndex].num_import = item.importNum;
            tasks[taskIndex].num_translate = item.translateCounts;
            tasks[taskIndex].num_translateExamine = item.translateExamineCounts;
            if (tasks[taskIndex].num__total > 0) {
              tasks[taskIndex].isHighlighted = true;
              totalTaskPendingNum += tasks[taskIndex].num__total;
            } else {
              tasks[taskIndex].isHighlighted = false;
            }
          }
        }
      });
      return { tasks: tasks, totalNum: totalTaskPendingNum };
    },
    // 获取翻译语种（模块级缓存，与悬浮工具箱等共享同一请求）
    getLanguage() {
      fetchLanguages()
        .then((res) => {
          this.translateTypes = res.data.list;
        })
        .catch((err) => {
          console.error("获取语种列表失败:", err);
        });
    },
    // 更改翻译语种
    SelectTranslateType() {
      if (this.selectedRows.length === 0) {
        message.warning("请至少选择一条任务");
        return;
      }
      this.translateTypeVisible = true; // 显示语种选择弹窗
      setModalAriaHidden(this, document);
      this.selectedLanguage = null; // 重置选择的语种
    },
    // 点击 confirm确认 按钮后会发生下面的操作（弹窗）
    /**
     * 仅提交副本，并在成功后刷新对应已选任务，避免失败项缓存被提前改写。
     * @returns {Promise<void>} 批量改语种及成功任务同步完成后结束。
     */
    async confirmTranslateType() {
      if (!this.selectedLanguage) {
        // 未选择语种
        message.warning("请选择一种语种");
        return;
      }
      const update1Tasks = []; // 仅包含接口提交副本
      const msg = { update1: [], update2: [], updateSuc: [], updateErr: [] };
      // 区分需要更改的和已经更改了的
      for (const task of this.selectedRows) {
        if (task.isBranch) continue;
        if (task.translateType != this.selectedLanguage) {
          update1Tasks.push({ ...task, translateType: this.selectedLanguage });
        } else {
          msg.update2.push(task.name);
        }
      }
      const promises = update1Tasks.map((task) => {
        return updateTaskInfo(task)
          .then((result) => {
            msg.update1.push(task.name);
            return { task, success: true, result };
          })
          .catch((error) => {
            msg.updateErr.push(task.name);
            return { task, success: false, error };
          });
      }); // 单条更新任务，批量循环调用
      let results = [];
      try {
        // 每个任务独立结算，个别失败不会阻止其他已选任务在界面上更新语种。
        results = await Promise.allSettled(promises);
      } catch (error) {
        console.error("处理所有任务时发生错误", error);
      }
      if (msg.update1.length > 0)
        msg.updateSuc.push(`成功更改：${msg.update1.join("、")}`);
      if (msg.update2.length > 0)
        msg.updateSuc.push(`无需更改：${msg.update2.join("、")}`);
      if (msg.updateSuc.length > 0) {
        const msg_str = `成功${
          msg.update1.length + msg.update2.length
        }条！${msg.updateSuc.join("；")}`;
        message.success(msg_str);
      }
      if (msg.updateErr.length > 0) {
        const msg_str = `更新${
          msg.updateErr.length
        }条任务失败：${msg.updateErr.join("、")}。`;
        message.error(msg_str);
      }

      const okTasks = results
        .filter((r) => r.status === "fulfilled" && r.value && r.value.success)
        .map((r) => r.value.task);
      if (okTasks.length > 0) {
        // 仅将接口确认成功的任务合并回列表，并刷新对应的待办统计。
        this.mergePendingTasks(okTasks);
        const { tasks: updatedTasks } = await this.getTaskPending(okTasks);
        this.mergePendingTasks(updatedTasks);
      }

      this.showOperationArea = false;
      this.translateTypeVisible = false;
    },
    // 点击 cancel取消 按钮后会发生下面的操作（弹窗）
    cancelTranslateType() {
      this.translateTypeVisible = false; // 关闭弹窗
    },
    /**
     * 从全局已选任务数组同步表格行对象和稳定 ID。
     * @returns {void} 无返回值。
     */
    syncSelection() {
      this.selectEntry = this.selectedEntryOperations.merge([], this.selectEntry);
      this.selectedRows = [...this.selectEntry];
      this.selectedRowKeys = this.selectEntry.map((task) => task.id);
    },
    /**
     * 用当前查询返回的新对象替换已选清单中的同 ID 行。
     * @param {Array<Object>} updatedTasks - 查询或保存成功返回的最新任务。
     * @returns {void} 无返回值。
     */
    replaceSelectedRecords(updatedTasks) {
      if (!updatedTasks || updatedTasks.length === 0) {
        this.syncSelection();
        return;
      }
      const selectedIds = new Set(this.selectEntry.map((task) => task.id));
      this.selectEntry = this.selectedEntryOperations.merge(
        this.selectEntry,
        updatedTasks.filter((task) => selectedIds.has(task.id))
      );
      this.syncSelection();
    },
    // 表格复选框点击事件
    /**
     * 更新单行或分支及其子任务选择，并覆盖待处理全量操作的投影。
     * @param {Object} record - 当前点击行。
     * @param {boolean} selected - 行的目标选择状态。
     * @returns {void} 无返回值。
     */
    onSelect(record, selected) {
      const rows = record.isBranch
        ? flattenRowsById([record], "child")
        : [record];
      rows.forEach((row) =>
        this.selectionOperations.record(row.id, selected, this.selectionScope)
      );
      this.selectEntry = this.selectedEntryOperations.apply(
        this.selectEntry,
        rows,
        selected
      );
      this.syncSelection();
    },
    /**
     * 按当前查询范围逐页收集全部任务并提交全选或取勾。
     * @param {boolean} selected - 是否选择当前查询范围。
     * @returns {Promise<void>} 全量选择请求完成后结束。
     */
    async onSelectAll(selected) {
      // 固定操作开始时的查询范围；用户切换卡片或重新筛选后，该异步操作自动失效。
      const scope = this.selectionScope;
      const query = { ...this.activeQueryParams };
      const api = this.activeCard === 1 ? getToDoTaskInfo : getFinishTaskInfo;
      const operation = this.selectionOperations.begin(scope, selected);
      try {
        // 分页收集完整结果集，而非误把当前可见页当成“全部”。
        const rows = await collectAllPages(
          (pageIndex, pageSize) => api({ pageIndex, pageSize }, query),
          100
        );
        // 拉取期间范围若改变，不更新复选框，避免旧结果套用到新筛选。
        if (scope !== this.selectionScope) return;
        const completeRows = this.isTreeOr2D === "tree"
          ? flattenRowsById(this.buildTreeData(rows), "child")
          : rows;
        const selectedRows = this.selectionOperations.resolve(
          operation,
          completeRows,
          this.selectEntry,
          this.selectionScope
        );
        // 将完整结果选择投影回当前表格，统一刷新已选数量和后续批量操作目标。
        this.selectEntry = selectedRows;
        this.syncSelection();
      } catch (error) {
        // 失败时撤销本次全量操作，但保留此前稳定的选择，避免用户重新勾选。
        const isCurrent = this.selectionOperations.isCurrent(
          operation,
          this.selectionScope
        );
        this.selectionOperations.reject(operation);
        this.syncSelection();
        if (isCurrent) {
          message.error("全量选择失败：" + (error?.message || error));
        }
      }
    },
    /**
     * 清空已选任务并使未完成的全量选择失效。
     * @returns {void} 无返回值。
     */
    clearAllEntry() {
      this.selectionOperations.invalidate();
      this.selectEntry = this.selectedEntryOperations.clear();
      this.syncSelection();
    },
    getRowClassName(record, index) {
      let className = null;
      if (record.isBranch) {
        className = "branch-row";
      } else {
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
      }
      return className;
    },
    customRow(record, index) {
      return {
        onClick: (event) => {
          if (record.isBranch) return;
          this.selectedRowIndex = record.id;
          this.showOperationArea = true;
          this.setTableHeight();
          this.currentTask = record;
          this.getClassfy(record);
          // console.log("点击流水线", record,event);
        },
      };
    },
    getClassfy(task) {
      if (!task.productId || task.productId === "") {
        this.classifyLimit = {};
        this.lastClassifyProductId = null;
        return;
      }
      // 同一任务重复点击不重发（classifyLimit 只随 productId 变化）
      if (this.lastClassifyProductId === task.productId) return;
      let params = {
        parentId: task.productId,
        type: "module",
      };
      getClassfy(params)
        .then((res) => {
          this.classifyLimit = {};
          this.lastClassifyProductId = task.productId;
          res.data.list.forEach((element) => {
            this.classifyLimit[element.title] = element;
          });
        })
        .catch((err) => {
          message.error(err.message);
        });
    },
    clickCard(index) {
      if (index !== this.activeCard) this.pagination.current = 1; // 查询条件变化，分页重置
      this.activeCard = index;
      if (index === 1) {
        this.tableTitle = "待办事项列表";
        this.timeLineBtn = true;
        // this.columns.some((item,i) => {
        //     if(item.dataIndex === 'operation'){
        //         this.columns.splice(i,1)
        //         return true
        //     }
        // })
      } else if (index === 2) {
        this.tableTitle = "已办事项列表";
        this.timeLineBtn = false;
        // if(this.columns.findIndex(item => item.dataIndex === 'operation') === -1){
        //     let operate = {title: '操作',dataIndex: 'operation',align:'center',width:50,fixed: 'right'}
        //     this.columns.push(operate)
        // }
      }
      this.reset();
    },
    init() {
      this.setTableHeight();
      this.getTaskTotal();
      // getLanguage 已在 mounted 调用（语种走模块级缓存，勿重复请求）
    },
    setTableHeight() {
      this.$nextTick(() => {
        setTableHeight(this, 8, 174, 32);
      });
    },
    toggleLeftPanel() {
      this.leftPanelCollapsed = !this.leftPanelCollapsed;
      this.$nextTick(() => {
        this.setTableHeight();
      });
    },
    /**
     * 使用单条分页请求刷新待办和已办总数。
     * @returns {void} 无返回值。
     */
    getTaskTotal() {
      const token = this.taskTotalGuard.next();
      const params = { pageIndex: 1, pageSize: 1 };
      Promise.all([getToDoTaskInfo(params, {}), getFinishTaskInfo(params, {})])
        .then(([todoResponse, finishResponse]) => {
          if (!this.taskTotalGuard.isCurrent(token)) return;
          this.toDoNum = todoResponse.data?.totalNum || 0;
          this.finishNum = finishResponse.data?.totalNum || 0;
        })
        .catch((error) => {
          if (this.taskTotalGuard.isCurrent(token)) {
            message.error("任务数量获取失败：" + (error?.message || error));
          }
        });
    },
    /**
     * 提交当前筛选条件并清空旧查询范围的选择。
     * @returns {void} 无返回值。
     */
    query() {
      this.clearAllEntry(); //清空已选
      this.pagination.current = 1;
      this.pageChangeSearch = { ...this.search };
      return this.getTask();
    },
    // 获取任务
    getTask() {
      return this.getTaskByCondition(this.search);
    },
    /**
     * 按筛选和分页条件查询任务，只允许最新请求更新表格状态。
     * @param {Object} data - 当前工作台查询条件。
     * @returns {Promise<void>} 列表及其待办数量加载完成后结束。
     */
    async getTaskByCondition(data) {
      this.checkSearchChange();
      const query = { ...(data || {}) };
      const scope = JSON.stringify({ activeCard: this.activeCard, query });
      if (scope !== this.selectionScope) {
        // 查询范围变化使在途全选失去语义，先终止该操作并保留当前稳定选择。
        this.selectionOperations.invalidate();
        this.selectionScope = scope;
        this.syncSelection();
      }
      this.activeQueryParams = query;
      // 列表请求遵循“最新一次优先”，防止慢请求回写旧卡片或旧筛选。
      const token = this.queryGuard.next();
      this.queryLoading = true;
      const params = {
        pageIndex: this.pagination.current,
        pageSize: this.pagination.pageSize,
      };
      const api = this.activeCard === 1 ? getToDoTaskInfo : getFinishTaskInfo;
      try {
        const res = await api(params, query);
        // 用户已经发起了新查询时，旧响应不可覆盖当前表格。
        if (!this.queryGuard.isCurrent(token)) return;
        const taskList = res.data?.list || [];
        let rows;
        if (this.isTreeOr2D == "tree") {
          // 树形模式先分组，再补齐每个分支下任务的待办数据。
          rows = this.buildTreeData(taskList);
          await this.getBranchPending(rows, token);
        } else {
          const { tasks } = await this.getTaskPending(taskList);
          rows = tasks;
        }
        if (!this.queryGuard.isCurrent(token)) return;
        // 替换列表对象后按 ID 重新关联已选任务，避免批量操作持有旧对象。
        this.dataSource = rows || [];
        this.pagination.total = res.data?.totalNum || 0;
        this.replaceSelectedRecords(flattenRowsById(this.dataSource, "child"));
      } catch (err) {
        if (this.queryGuard.isCurrent(token)) {
          message.error("数据获取失败！", err.message);
        }
      } finally {
        // 旧请求结束不能提前隐藏新请求的表格加载态。
        if (this.queryGuard.isCurrent(token)) this.queryLoading = false;
      }
    },
    // 把 getTaskPending 结果写回当前表格（平铺行 / 层级子任务与分支合计）
    mergePendingTasks(updatedTasks) {
      if (!updatedTasks || updatedTasks.length === 0) return;
      if (this.isTreeOr2D == "tree") {
        const byId = new Map(updatedTasks.map((t) => [t.id, t]));
        for (let b = 0; b < this.dataSource.length; b++) {
          const branch = this.dataSource[b];
          if (!branch.child || branch.child.length === 0) continue;
          let touched = false;
          for (let i = 0; i < branch.child.length; i++) {
            const updated = byId.get(branch.child[i].id);
            if (updated) {
              branch.child[i] = updated;
              touched = true;
            }
          }
          if (touched) {
            branch.num__total = branch.child.reduce(
              (sum, t) => sum + (t.num__total || 0),
              0
            );
          }
        }
      } else {
        for (const updatedTask of updatedTasks) {
          const taskIndex = this.dataSource.findIndex(
            (t) => t.id === updatedTask.id
          );
          if (taskIndex !== -1) {
            this.dataSource[taskIndex] = updatedTask;
          }
        }
      }
      this.replaceSelectedRecords(updatedTasks);
    },
    // 刷新当前任务红点（任务行 badge + 流水线各阶段 badge）。
    // getTaskPending 原地写 task.num_* 字段：dataSource 行 / currentTask / TimeLine.task 同引用，一次写入三方同步。
    async refreshTaskBadges(task) {
      if (!task || !task.id || String(task.id).startsWith("branch_")) return;
      try {
        await withLoading(async () => {
          await this.getTaskPending([task]);
          if (this.isTreeOr2D == "tree") {
            // 层级展示：重算所属分支的合计角标（全量重算，不依赖旧值）
            const branch = this.dataSource.find(
              (b) => b.child && b.child.some((t) => t.id === task.id)
            );
            if (branch && branch.child.length > 0) {
              branch.num__total = branch.child.reduce(
                (sum, t) => sum + (t.num__total || 0),
                0
              );
            }
          }
          this.replaceSelectedRecords([task]);
        });
      } catch (err) {
        message.error("红点刷新失败：" + (err?.message || err));
      }
    },
    // 构建树结构数据
    buildTreeData(taskList) {
      const branchMap = new Map();

      // 按codeBranch分组
      taskList.forEach((task, index) => {
        task.index = index;
        task.isHighlighted = undefined; // 初始化为undefined
        const branchKey = task.codeBranch || "无分支";

        if (!branchMap.has(branchKey)) {
          branchMap.set(branchKey, []);
        }
        branchMap.get(branchKey).push(task);
      });

      // 构建树结构
      const treeData = [];
      branchMap.forEach((tasks, branchName) => {
        // 创建分支节点
        const branchNode = {
          id: `branch_${branchName}`,
          codeBranch: branchName,
          isBranch: true,
          isHighlighted: undefined, // 分支节点也初始化为undefined
          productName: "",
          versionName: "",
          translateType: "",
          description: `共${tasks.length}个任务`,
          creator: "",
          deliveryTime: "",
          child: tasks, // 子节点为该分支下的所有任务
        };
        treeData.push(branchNode);
      });

      // 按分支名称排序，将'无分支'放在最后
      treeData.sort((a, b) => {
        if (a.codeBranch === "无分支") return 1;
        if (b.codeBranch === "无分支") return -1;
        return a.codeBranch.localeCompare(b.codeBranch);
      });
      for (let i = 0; i < treeData.length; i++) {
        treeData[i].index = i + 1;
        for (let j = 0; j < treeData[i].child.length; j++) {
          treeData[i].child[j].index = j + 1;
        }
      }
      return treeData;
    },
    checkSearchChange() {
      // 检测查询条件是否发生变化
      let isChanged = false;
      for (let key in this.search) {
        if (this.search[key] !== this.lastSearch[key]) {
          isChanged = true;
          break;
        }
      }
      if (isChanged) {
        this.pagination.current = 1; // 查询条件变化，分页重置
        this.lastSearch = { ...this.search }; // 保存查询条件
      }
    },
    closeOperationArea() {
      this.showOperationArea = false;
      this.setTableHeight();
      this.selectedRowIndex = null;
    },
    importEntry() {
      this.importVisible = true;
      setModalAriaHidden(this, document);
      this.$refs.import.initTaskEntry();
    },
    importClose() {
      this.importVisible = false;
      // 刷新流水线"导入"角标 + 任务行红点
      this.$refs.timeLineRef.initEntryCount();
      this.refreshTaskBadges(this.currentTask);
    },
    // 词条审核
    examineEntry() {
      this.examineVisible = true;
      setModalAriaHidden(this, document);
      this.examineTitle = "词条审核";
      this.$refs.examine.getTaskEntry();
    },
    examineClose() {
      this.examineVisible = false;
      // 刷新流水线"导入"角标 + 任务行红点
      this.$refs.timeLineRef.initEntryCount();
      this.refreshTaskBadges(this.currentTask);
    },
    // 词条翻译
    translateEntry() {
      this.translateVisible = true;
      setModalAriaHidden(this, document);
      // this.$refs.translate.getTranslateEntry()
      this.$refs.translate.initTranslateEntry();
    },
    translateClose() {
      this.translateVisible = false;
      // 刷新流水线"导入"角标 + 任务行红点
      this.$refs.timeLineRef.initEntryCount();
      this.refreshTaskBadges(this.currentTask);
    },
    // 翻译审核
    examineTranslate() {
      this.examineTranslateVisible = true;
      setModalAriaHidden(this, document);
      this.$refs.examineTranslate.getTaskEntry();
    },
    examineTranslateClose() {
      this.examineTranslateVisible = false;
      // 刷新流水线"导入"角标 + 任务行红点
      this.$refs.timeLineRef.initEntryCount();
      this.refreshTaskBadges(this.currentTask);
    },
    // 归档
    archiveEntry() {
      this.archiveVisible = true;
      setModalAriaHidden(this, document);
      this.$refs.archiveModalRef.getTaskEntry();
    },
    archiveClose() {
      this.archiveVisible = false;
      // this.getTask()
    },
    refreshTask() {
      this.getTask();
      this.archiveVisible = false;
      this.showOperationArea = false;
      this.setTableHeight();
      this.getTaskTotal();
    },
    // 批量预翻译
    openBatchPreTranslate() {
      if (this.selectedRows.length === 0) {
        message.warning("请先选择任务");
        return;
      }
      this.batchPreTranslateVisible = true;
      setModalAriaHidden(this, document);
    },
    async onBatchPreTranslateComplete() {
      const id = this.currentTask?.id;
      this.batchPreTranslateVisible = false;
      await this.getTask();
      this.getTaskTotal();
      this.rebindTask(id);
      this.$refs.timeLineRef?.initEntryCount?.();
    },
    /** 按 id 从当前列表重绑打开中的流水线任务（getTask 换新对象后保持红点同源） */
    rebindTask(id) {
      if (!id) return;
      const flat =
        this.isTreeOr2D === "tree"
          ? (this.dataSource || []).flatMap((b) => b.child || [])
          : this.dataSource || [];
      const hit = flat.find((t) => t.id === id);
      if (hit) this.currentTask = hit;
    },
    // 同步批量预翻译任务列表（用于模态框删除任务后同步父组件）
    syncBatchTasks(newTasks) {
      this.selectedRows = newTasks;
      this.selectedRowKeys = newTasks.map((t) => t.id);
      this.selectEntry = this.selectedEntryOperations.merge([], newTasks);
      this.syncSelection();
    },
    // 重置
    reset() {
      this.search = {
        name: "",
        productName: "",
        translateType: "",
        department: "",
        auditor: "",
        creator: "",
      };
      this.clearAllEntry(); //清空已选
      this.pageChangeSearch = { ...this.search };
      this.pagination.current = 1;
      return this.getTask();
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
      this.currentPageBranch = 0;
      this.getTaskByCondition(this.pageChangeSearch);
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
}
.ant-row {
  height: 100%;
}
.workbench-row {
  height: 100%;
}
.workbench-main {
  min-width: 0;
}
.workbench-operate-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 8px;
  justify-content: flex-end;
  min-width: 0;

  :deep(.ant-btn) {
    flex-shrink: 0;
  }
}
.left-panel-rail {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  border: 1px solid #e8eaf2;
}
.left-panel-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  color: #647aff;
  cursor: pointer;

  &:hover {
    color: #4b5fd9;
    background: #f7f8fc;
  }
}
.cardBox {
  display: flex;
  padding: 0;
  flex-direction: column;
  align-items: flex-start;
  gap: 32px;
  overflow: auto;
  height: 100%;

  .title {
    /* 三级文字/加粗 */
    font-family: Microsoft YaHei;
    font-size: 16px;
    font-style: normal;
    font-weight: 700;
    line-height: 24px; /* 150% */
  }
  .logo {
    width: 100%;
    height: 66px;
    margin-top: 16px;

    display: flex;
    padding: 1px;
    justify-content: center;
    align-items: flex-start;
    gap: 8px;
    align-self: stretch;
  }
  .data {
    width: 100%;
    height: 44px;
    margin-top: 16px;
    text-align: center;
    position: relative;

    span {
      color: #000;
      /* 一级文字/加粗 */
      font-family: Microsoft YaHei;
      font-style: normal;
    }

    .ant-btn {
      position: absolute;
      right: 0;
      background-color: white;
      top: 50%;
      transform: translateY(-50%);
    }
  }
  .data span:first-child {
    font-size: 36px;
    font-weight: 700;
    line-height: 44px;
  }
  .data span:last-child {
    font-size: 14px;
    font-weight: 400;
    line-height: 22px;
  }
}
.dataBox {
  padding-left: 0px;
  position: absolute;
  height: 100%;
  width: 100%;
  /* 换行后每个表单项的间距 */
  & :deep(.search .form .ant-row) {
    margin-bottom: 8px !important;
  }
}
:deep(.search .form .workbench-search-form) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  width: 100%;
}
:deep(.search .form .workbench-search-form .ant-form-item) {
  flex: 1 1 270px;
  min-width: 0;
  margin: 0;
}
:deep(.search .form .workbench-search-form .ant-form-item-label) {
  flex: 0 0 84px;
}
:deep(.search .form .workbench-search-form .ant-form-item-control) {
  flex: 1 1 0;
  width: auto;
  min-width: 0;
  max-width: 186px;
}
.workbench-main-table {
  position: absolute;
  width: 100%;
  min-width: 0;
}
.ant-card {
  width: 100%;
  height: 200px;
  border-radius: 8px;
  padding: 16px;
}
:deep(.ant-card-body) {
  padding: 0px;
}
.handleCard {
  background: #f1f3ff;

  .title {
    color: #647aff;
  }
}
.activeHandleCard {
  box-shadow:
    1px 6px 12px 0px rgba(100, 122, 255, 0.2),
    -1px 0px 8px 0px rgba(100, 122, 255, 0.2);
}
.handleCard:hover {
  box-shadow:
    1px 6px 12px 0px rgba(100, 122, 255, 0.2),
    -1px 0px 8px 0px rgba(100, 122, 255, 0.2);
}
.processedCard {
  background: #f0fffc;

  .title {
    color: #36bf7d;
  }
}
.activeProcessedCard {
  box-shadow:
    1px 6px 12px 0px rgba(54, 191, 125, 0.2),
    -1px 0px 8px 0px rgba(54, 191, 125, 0.2);
}
.processedCard:hover {
  box-shadow:
    1px 6px 12px 0px rgba(54, 191, 125, 0.2),
    -1px 0px 8px 0px rgba(54, 191, 125, 0.2);
}
.exportCard {
  background: #fffbf0;

  .title {
    color: #f1bd2e;
  }
}
.activeExportCard {
  box-shadow:
    1px 6px 12px 0px rgba(241, 189, 46, 0.2),
    -1px 0px 8px 0px rgba(241, 189, 46, 0.2);
}
.exportCard:hover {
  box-shadow:
    1px 6px 12px 0px rgba(241, 189, 46, 0.2),
    -1px 0px 8px 0px rgba(241, 189, 46, 0.2);
}
.red-text {
  color: red;
}
:deep(.branch-row) {
  color: blue !important;
  font-weight: bold;
}
</style>
