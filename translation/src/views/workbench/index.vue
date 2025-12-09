<template>
  <div ref="box" class="box">
    <a-row type="flex">
      <a-col flex="296px" class="cardBox">
        <a-card hoverable :class="activeCard === 1 ? 'handleCard activeHandleCard' : 'handleCard'" @click="clickCard(1)">
          <div class="title">待办事项</div>
          <div class="logo"><img src="../../assets/workIcon/handle.png" /></div>
          <div class="data">
            <span>{{toDoNum}}</span><span>条</span>
            <!-- <a-button type="primary" ghost size="small" @click="clickCard(1)">查看</a-button> -->
          </div>
        </a-card>
        <a-card hoverable :class="activeCard === 2 ? 'processedCard activeProcessedCard' : 'processedCard'" @click="clickCard(2)">
          <div class="title">已办事项</div>
          <div class="logo"><img src="../../assets/workIcon/processed.png" /></div>
          <div class="data">
            <span>{{finishNum}}</span><span>条</span>
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
      <a-col flex="auto">
        <div class="dataBox">
          <SearchBox ref="search" @change="setTableHeight">
            <template v-slot:form>
              <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
                <a-form-item label="任务名称" name="name">
                  <a-input v-model:value="search.name" placeholder="请输入任务名称"></a-input>
                </a-form-item>
                <a-form-item label="产品名称" name="productName">
                  <a-input v-model:value="search.productName" placeholder="请输入产品名称"></a-input>
                </a-form-item>
                <a-form-item label="翻译语种" name="translateType">
                  <a-select v-model:value="search.translateType" placeholder="请选择翻译语种" :fieldNames="{label:'name',value:'name'}"
                    :options='translateTypes' allowClear>
                  </a-select>
                </a-form-item>
                <a-form-item label="执行部门" name="department">
                  <a-input v-model:value="search.department" placeholder="请输入执行部门"></a-input>
                </a-form-item>
                <a-form-item label="创建人" name="creator">
                  <a-input v-model:value="search.creator" placeholder="请输入创建人"></a-input>
                </a-form-item>
                <!-- <a-form-item label="词条审核员" name="auditor">
                  <a-input v-model:value="search.auditor" placeholder="请输入词条审核员" </a-input>
                </a-form-item> -->
              </a-form>
            </template>
            <template v-slot:operate>
              <a-button type="primary" size="middle" class="resetBtn" @click="reset">重置</a-button>
              <a-button type="primary" size="middle" @click="query">查询</a-button>
            </template>
          </SearchBox>
          <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
            <template v-slot:operate>
              <div ref="button" v-if="true" style="margin-bottom:8px">
                <a-button type="primary" size="middle" style="margin-right:8px;" @click="isTreeOr2D=='tree'?isTreeOr2D='2D':isTreeOr2D='tree'">
                  {{isTreeOr2D=='tree'?'平铺':'层级'}}展示</a-button>
                <a-button type="primary" size="middle" @click="SelectTranslateType">更改翻译语种</a-button>
                <a-modal style="width: 320px;" class="choiceLang" centered title="选择语种" :visible="translateTypeVisible" @ok="confirmTranslateType"
                  @cancel="cancelTranslateType">
                  <a-select v-model:value="selectedLanguage" style="width: 100%;" placeholder="请选择内容" :options='translateTypes'
                    :fieldNames="{label:'name',value:'name'}" allowClear>
                  </a-select>
                  <template #footer>
                    <div style="text-align: center;">
                      <a-button @click="cancelTranslateType">取消</a-button>
                      <a-button type="primary" @click="confirmTranslateType">确定</a-button>
                    </div>
                  </template>
                </a-modal>
              </div>
            </template>
            <template v-slot:data>
              <!-- , onChange: onSelectChange -->
              <div style="width:100%;position: absolute;">
                <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :row-key="record => record.id"
                  :scroll="tableHeight" :pagination='pagination' :loading="loading" :rowClassName="getRowClassName" childrenColumnName="child"
                  ref="workTable" @resizeColumn="handleResizeColumn" :row-selection=" { selectedRowKeys: selectedRowKeys,onSelect:onSelect,onSelectAll:onSelectAll, onChange: onSelectChange,
                    selections:[
                        {key:'selectAll',text:'全部选择',onSelect:selectAllEntry},
                        {key:'clearAll',text:'取消选择',onSelect:clearAllEntry}
                    ]}" :customRow="customRow">
                  <template #bodyCell="{ column, record, text }">
                    <template v-if="column.dataIndex === 'index'&&isTreeOr2D=='tree'&&record.isBranch">
                      <span style="color:blue;">
                        {{ text }}
                      </span>
                    </template>
                    <template v-if="column.dataIndex === 'state'">
                      <TaskStateBadge type="normal" :taskState="text" />
                    </template>
                    <template v-if="column.dataIndex === 'name'">
                      <span style="position: relative; display: inline-block;">
                        {{ text }}
                        <a-badge color="#ff0000" v-if="record.isHighlighted" style="position: absolute;top: -9px;right: -9px;z-index: 1;" />
                      </span>
                    </template>
                  </template>
                  <template #expandIcon="props">
                    <span v-if="props.record.child != null && props.record.child.length > 0">
                      <div v-if="props.expanded" style="display: inline-block; margin-right: 10px"
                        @click="(e) => {handleBranchExpand(props.record, props.expanded, e, props.onExpand, props);}">
                        <CaretDownOutlined /> <!-- 展开状态的向下三角形图标 -->
                      </div>
                      <div v-else style="display: inline-block; margin-right: 10px"
                        @click="(e) => {handleBranchExpand(props.record, props.expanded, e, props.onExpand, props);}">
                        <CaretRightOutlined /> <!-- 收起状态的向右三角形图标 -->
                      </div>
                    </span>
                    <span v-else style="margin-right:23px"></span> <!-- 无子记录时的占位 -->
                  </template>
                </a-table>
              </div>
            </template>
          </DataBox>
          <OperationArea ref="operationArea" :title="operationAreaTitle" :height="operationAreaHeight" v-if="showOperationArea"
            @close="closeOperationArea">
            <template v-slot:content>
              <TimeLine ref="timeLineRef" :currentTask="currentTask" :showButton="timeLineBtn" @importEntry="importEntry" @examineEntry="examineEntry"
                @translateEntry="translateEntry" @examineTranslate="examineTranslate" @refresh="getTask" @archiveEntry="archiveEntry">

              </TimeLine>
            </template>
          </OperationArea>
        </div>
      </a-col>
    </a-row>

  </div>
  <ImportModal ref="import" :visible="importVisible" :currentTask="currentTask" :classifyLimit="classifyLimit" @handleClose="importClose" />
  <ExamineModal ref="examine" :visible="examineVisible" :currentTask="currentTask" :classifyLimit="classifyLimit" :modalTitle="examineTitle"
    @handleClose="examineClose" />
  <TranslateModal ref="translate" :visible="translateVisible" :currentTask="currentTask" :classifyLimit="classifyLimit"
    @handleClose="translateClose" />
  <ExamineTranslateModal ref="examineTranslate" :visible="examineTranslateVisible" :currentTask="currentTask" :classifyLimit="classifyLimit"
    @handleClose="examineTranslateClose" />
  <ArchiveModal ref="archiveModalRef" :visible="archiveVisible" :currentTask="currentTask" @handleClose="archiveClose" @refresh="refreshTask" />
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
import {
  SendOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
} from "@ant-design/icons-vue";
import { getToDoTaskInfo, getFinishTaskInfo } from "@/http/api/task";
import { getClassfy } from "@/http/api/entryManage";
import { getLanguage } from "@/http/api/translate";
import { updateTaskInfo } from "@/http/api/task";
import { getEntryInfoList } from "@/http/api/workbench";
import {
  setTableHeight,
  setModalAriaHidden,
  selectAllEntry,
  clearAllEntry,
  pageChange,
} from "@/utils/commonUtils";
import { getProduct } from "@/http/api/product";
export default {
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
    SendOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
  },
  data() {
    return {
      name: "workbench",
      // 汉化包
      locale: locale,
      toDoNum: 0,
      toDoTasks: [], // 待办任务列表(便于全部选择)
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
      tableHeight: { x: "max-content", y: 0 },
      // tableHeight: { x: "100%", y: 0 },
      loading: false,
      currentPageBranch: 0, // 当前页的分支数量
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 40,
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
          title: "分支",
          dataIndex: "codeBranch",
          align: "center",
          width: 100,
          fixed: "left",
          resizable: true,
          index: 0.5,
        },
        {
          title: "任务名称",
          dataIndex: "name",
          align: "center",
          width: 100,
          fixed: "left",
          resizable: true,
          index: 1,
        },
        // {title: '任务状态',dataIndex: 'state',align:'center',width:80,fixed: 'left',resizable: true,index:1},
        {
          title: "产品名称",
          dataIndex: "productName",
          align: "center",
          width: 100,
          resizable: true,
          index: 2,
        },
        {
          title: "产品版本",
          dataIndex: "versionName",
          align: "center",
          width: 100,
          resizable: true,
          index: 3,
        },
        // {title: '执行部门',dataIndex: 'department',align:'center',width:100,ellipsis: true,resizable: true,index: 3},
        {
          title: "翻译语种",
          dataIndex: "translateType",
          align: "center",
          width: 100,
          ellipsis: true,
          resizable: true,
          index: 4,
        },
        {
          title: "描述",
          dataIndex: "description",
          align: "center",
          width: 100,
          ellipsis: true,
          resizable: true,
          index: 5,
        },
        {
          title: "创建人",
          dataIndex: "creator",
          align: "center",
          width: 50,
          ellipsis: true,
          index: 6,
        },
        {
          title: "下发时间",
          dataIndex: "deliveryTime",
          align: "center",
          width: 100,
          ellipsis: true,
          index: 7,
        },
        // {title: '操作',dataIndex: 'operation',align:'center',width:50,fixed: 'right',},
      ],
      dataSource: [],
      selectedRows: [],
      selectedRowKeys: [],
      selectedRowIndex: null,
      selectEntry: new Map(), // 已选任务
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
    };
  },
  mounted() {
    let _this = this;
    this.$nextTick(() => {
      // 读取本地存储的用户偏好
      const storedDisplay = localStorage.getItem("display-workbenchIndex");
      // console.log("偏好：", storedDisplay, localStorage);
      if (storedDisplay) {
        this.isTreeOr2D = JSON.parse(storedDisplay);
        if (this.isTreeOr2D == "tree") this.pageChange(1, 100);
        else this.pageChange(1, 20);
      }
      this.init();

      /** 控制table的高度 */
      window.onresize = function () {
        _this.setTableHeight();
      };
    });
    this.getLanguage();
  },
  unmounted() {
    //注销window.onresize事件
    window.onresize = null;
  },
  watch: {
    isTreeOr2D: {
      handler(newVal, oldVal) {
        if (newVal != null && newVal !== oldVal) {
          this.query();
          localStorage.setItem(
            "display-workbenchIndex",
            JSON.stringify(newVal)
          ); // 存储用户偏好
          if (newVal == "tree") this.pageChange(1, 100);
          else this.pageChange(1, 20);
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

      // 设置加载状态
      this.loading = true;

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
      await this.getBranchPending();

      // 清除加载状态
      this.loading = false;
    },
    // 获取展开的分支的任务执行状态
    async getBranchPending() {
      for (let i = 0; i < this.expandSource.length; i++) {
        let datai = this.dataSource.findIndex(
          (item) => item.id == this.expandSource[i].id
        );
        this.expandSource[i].record = this.dataSource[datai];// 保证expandSource和dataSource一致
        let branch = this.expandSource[i].record;

        // console.log("比较：", branch, this.dataSource[datai]);
        if (branch.child && branch.child.length > 0) {
          let hasPendingTask = false;

          // 遍历该分支下的所有任务
          for (let j = 0; j < branch.child.length; j++) {
            // console.log("分类信息", branch.child[j], branch.child[j].id);
            const isHL = await this.getTaskPending(branch.child[j].id);
            branch.child[j].isHighlighted = isHL;
            // this.dataSource[datai].child[j].isHighlighted = isHL;

            // 但是切换到下一页时会有问题，封掉这个功能

            // 如果有任何一个任务有未完成词条，标记分支有未完成任务
            if (branch.child[j].isHighlighted) {
              hasPendingTask = true;
            }
          }

          // 更新分支节点的高亮状态
          branch.isHighlighted = hasPendingTask;
          // this.dataSource[datai].isHighlighted = hasPendingTask;
        }
      }
    },
    // 获取词条数量
    async getTaskPending(taskID) {
      function fetch(params, data) {
        return getEntryInfoList(params, data)
          .then((res) => {
            return res.data.list.length > 0;
          })
          .catch((err) => {
            message.error(err.message);
            return false;
          });
      }
      const entryStates = ["1", "2", "3", "3"];
      const datas = [[], [], ["0", "2"], ["1"]];
      const promises = [];

      for (let i = 0; i < entryStates.length; i++) {
        const params = {
          taskID: taskID,
          entryState: entryStates[i],
          entry: "",
        };
        const data = datas[i];
        promises.push(fetch(params, data));
      }
      try {
        const results = await Promise.all(promises);
        return results.some((result) => result);
      } catch (error) {
        return false;
      }
    },
    // 获取翻译语种
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
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
    async confirmTranslateType() {
      if (!this.selectedLanguage) {
        // 未选择语种
        message.warning("请选择一种语种");
        return;
      }
      const update1Tasks = []; //需要更改的任务列表
      const msg = { update1: [], update2: [], updateSuc: [], updateErr: [] };
      // 区分需要更改的和已经更改了的
      for (const task of this.selectedRows) {
        if (task.isBranch) continue;
        if (task.translateType != this.selectedLanguage) {
          task.translateType = this.selectedLanguage;
          update1Tasks.push(task);
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
      Promise.allSettled(promises)
        .then((results) => {
          // console.log("所有任务处理完成");
          // const successfulTasks = results.filter(
          //   (result) => result.status === "fulfilled"
          // );
          // const failedTasks = results.filter(
          //   (result) => result.status === "rejected"
          // );
          // console.log(
          //   `成功任务数: ${successfulTasks.length}, 失败任务数: ${failedTasks.length}`
          // );

          // console.log("打印信息", msg);
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
        })
        .catch((error) => {
          // 这里处理Promise.allSettled本身可能出现的错误（通常很少见）
          console.error("处理所有任务时发生错误", error);
        })
        .finally(() => {
          this.showOperationArea = false;
          this.init();
        });

      // 更新完成后刷新任务列表
      this.selectedRows = [];
      this.selectedRowKeys = [];

      this.translateTypeVisible = false; // 关闭弹窗
      // this.init();// 刷新任务列表
    },
    // 点击 cancel取消 按钮后会发生下面的操作（弹窗）
    cancelTranslateType() {
      this.translateTypeVisible = false; // 关闭弹窗
    },
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

      if (record.isBranch) {
        if (selected) {
          record.child.forEach((item) => {
            this.selectEntry.set(item.id, item);
          });
        } else {
          record.child.forEach((item) => {
            this.selectEntry.delete(item.id);
          });
        }
      }

      // console.log("表格复选框点击事件", record, selected);
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
      // console.log(
      //   "表格全选/反选框点击事件",
      //   selected,
      //   selectedRows, // 全选->取选当前页数据时，这个是5个undefined
      //   changeRows
      // );
    },
    // 复选框全选事件
    selectAllEntry() {
      this.toDoTasks.forEach((item) => {
        this.selectedRowKeys.push(item.id);
        this.selectedRows.push(item);
        this.selectEntry.set(item.id, item);
      });
    },
    //复选框反选事件
    clearAllEntry() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.selectEntry.clear();
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
        return;
      }
      let params = {
        parentId: task.productId,
        type: "module",
      };
      getClassfy(params)
        .then((res) => {
          this.classifyLimit = {};
          res.data.list.forEach((element) => {
            this.classifyLimit[element.title] = element;
          });
        })
        .catch((err) => {
          message.err(err.message);
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
      this.getTask();
      this.getTaskTotal();
      this.getLanguage();
    },
    setTableHeight() {
      this.$nextTick(() => {
        setTableHeight(this, 8, 174, 32);
      });
    },
    // 获取待办事项和已办事项数量
    getTaskTotal() {
      let params = {
        pageIndex: -1,
        pageSize: -1,
      };
      // 待办事项
      getToDoTaskInfo(params, {}).then((res) => {
        this.toDoNum = res.data.totalNum;
        this.toDoTasks = res.data.list;
        // console.log("需要查询是否完成的任务1", this.toDoTasks);
      });
      // 已办事项
      getFinishTaskInfo(params, {}).then((res) => {
        this.finishNum = res.data.totalNum;
      });
    },
    // 查询按钮点击事件
    query() {
      this.pageChangeSearch = this.search;
      this.getTask();
      this.clearAllEntry(); //清空已选
    },
    // 获取任务
    getTask() {
      this.getTaskByCondition(this.search);
    },
    // 根据条件获取任务
    async getTaskByCondition(data) {
      this.loading = true;
      this.checkSearchChange();
      let params = {
        pageIndex: this.pagination.current,
        pageSize: this.pagination.pageSize,
      };
      const api = this.activeCard === 1 ? getToDoTaskInfo : getFinishTaskInfo;

      api(params, data)
        .then(async (res) => {
          // console.log("需要查询是否完成的任务2", res.data.list);
          const taskList = res.data.list;
          this.pagination.total = res.data.totalNum;

          if (this.isTreeOr2D == "tree") {
            // 层级展示
            this.dataSource = this.buildTreeData(taskList);
            // 使用$nextTick确保DOM更新后再执行getBranchPending
            this.$nextTick(async () => {
              // console.log(
              //   "根据条件获取任务,重新创建的treeData",
              //   this.dataSource,
              //   this.expandSource
              // );
              await this.getBranchPending();
            });
          } else {
            // 平铺展示
            this.dataSource = taskList;
            // 太卡了先不用
            for (const item of this.dataSource) {
              if (await this.getTaskPending(item.id)) {
                item.isHighlighted = true;
              } else {
                item.isHighlighted = false;
              }
            }
          }
        })
        .catch((err) => {
          message.error("数据获取失败！", err.message);
        })
        .finally(() => {
          this.loading = false;
        });
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
    handleResizeColumn: (w, col) => {
      col.width = w;
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
      // 刷新词条数量
      this.$refs.timeLineRef.initEntryCount();
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
      // 刷新词条数量
      this.$refs.timeLineRef.initEntryCount();
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
      // 刷新词条数量
      this.$refs.timeLineRef.initEntryCount();
    },
    // 翻译审核
    examineTranslate() {
      this.examineTranslateVisible = true;
      setModalAriaHidden(this, document);
      this.$refs.examineTranslate.getTaskEntry();
    },
    examineTranslateClose() {
      this.examineTranslateVisible = false;
      // 刷新词条数量
      this.$refs.timeLineRef.initEntryCount();
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
      this.pageChangeSearch = this.search;
      this.getTask();
      this.clearAllEntry(); //清空已选
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
.cardBox {
  display: flex;
  padding: 0px 8px;
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
  padding-left: 16px;
  position: absolute;
  height: 100%;
  width: 100%;
  /* 换行后每个表单项的间距 */
  & :deep(.search .form .ant-row) {
    margin-bottom: 8px !important;
  }
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
  box-shadow: 1px 6px 12px 0px rgba(100, 122, 255, 0.2),
    -1px 0px 8px 0px rgba(100, 122, 255, 0.2);
}
.handleCard:hover {
  box-shadow: 1px 6px 12px 0px rgba(100, 122, 255, 0.2),
    -1px 0px 8px 0px rgba(100, 122, 255, 0.2);
}
.processedCard {
  background: #f0fffc;

  .title {
    color: #36bf7d;
  }
}
.activeProcessedCard {
  box-shadow: 1px 6px 12px 0px rgba(54, 191, 125, 0.2),
    -1px 0px 8px 0px rgba(54, 191, 125, 0.2);
}
.processedCard:hover {
  box-shadow: 1px 6px 12px 0px rgba(54, 191, 125, 0.2),
    -1px 0px 8px 0px rgba(54, 191, 125, 0.2);
}
.exportCard {
  background: #fffbf0;

  .title {
    color: #f1bd2e;
  }
}
.activeExportCard {
  box-shadow: 1px 6px 12px 0px rgba(241, 189, 46, 0.2),
    -1px 0px 8px 0px rgba(241, 189, 46, 0.2);
}
.exportCard:hover {
  box-shadow: 1px 6px 12px 0px rgba(241, 189, 46, 0.2),
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