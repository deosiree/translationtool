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
                    <div class="logo"><img src="../../assets/workIcon/export.png"/></div>
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
                  <a-input v-model:value="search.translateType" placeholder="请输入翻译语种"></a-input>
                </a-form-item>
                <a-form-item label="执行部门" name="department">
                  <a-input v-model:value="search.department" placeholder="请输入执行部门"></a-input>
                </a-form-item>
                <a-form-item label="创建人" name="creator">
                  <a-input v-model:value="search.creator" placeholder="请输入创建人"></a-input>
                </a-form-item>
                <!-- <a-form-item
                                label="词条审核员"
                                name="auditor"
                                >
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
                <a-button type="primary" size="middle" @click="SelectTranslateType">更改翻译语种</a-button>
                <a-modal style="width: 320px;" class="choiceLang" centered title="选择语言" :visible="translateTypeVisible" @ok="confirmTranslateType"
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
              <div style="width:100%;position: absolute;">
                <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource"
                  :row-selection=" { selectedRowKeys: selectedRowKeys, onChange: onSelectChange,onSelect:onSelect,onSelectAll:onSelectAll}"
                  :row-key="record => record.id" :scroll="tableHeight" :pagination='pagination' :loading="loading" :rowClassName="getRowClassName"
                  ref="workTable" @resizeColumn="handleResizeColumn" :customRow="customRow">
                  <template #bodyCell="{ column, record, text }">
                    <template v-if="column.dataIndex === 'state'">
                      <template v-if="record.state === '0'">
                        <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
                      </template>
                      <template v-else-if="record.state === '1'">
                        <a-badge color="#FBB31F" /><span style="color:#FBB31F">词条待导入</span>
                      </template>
                      <template v-else-if="record.state === '2'">
                        <a-badge color="#FBB31F" /><span style="color:#FBB31F">词条待审核</span>
                      </template>
                      <template v-else-if="record.state === '3'">
                        <a-badge color="#FBB31F" /><span style="color:#FBB31F">词条待翻译</span>
                      </template>
                      <template v-else-if="record.state === '4'">
                        <a-badge color="#FBB31F" /><span style="color:#FBB31F">翻译待审核</span>
                      </template>
                      <template v-else-if="record.state === '5'">
                        <a-badge color="#FBB31F" /><span style="color:#FBB31F">待导出</span>
                      </template>
                      <template v-else>
                        <a-badge color="#36BF7D" /><span style="color:#36BF7D">已完成</span>
                      </template>
                    </template>
                    <template v-if="column.dataIndex === 'name'">
                      <span style="position: relative; display: inline-block;">
                        {{ text }}
                        <a-badge color="#ff0000" v-if="record.isHighlighted" style="position: absolute;top: -9px;right: -9px;z-index: 1;" />
                      </span>
                    </template>
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
  />
  <TranslateModal ref="translate" :visible="translateVisible" :currentTask="currentTask" :classifyLimit="classifyLimit"
    @handleClose="translateClose" />
  />
  <ExamineTranslateModal ref="examineTranslate" :visible="examineTranslateVisible" :currentTask="currentTask" :classifyLimit="classifyLimit"
    @handleClose="examineTranslateClose" />
  />
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
import ImportModal from "@/views/workbench/importModal.vue";
import ExamineModal from "@/views/workbench/examineModal.vue";
import TranslateModal from "@/views/workbench/translateModal.vue";
import ExamineTranslateModal from "@/views/workbench/examineTranslateModal.vue";
import ArchiveModal from "@/views/workbench/archiveModal.vue";
import { SendOutlined } from "@ant-design/icons-vue";
import { getToDoTaskInfo, getFinishTaskInfo } from "@/http/api/task";
import { getClassfy } from "@/http/api/entryManage";
import { getLanguage } from "@/http/api/translate";
import { updateTaskInfo } from "@/http/api/task";
import { getEntryInfoList } from "@/http/api/workbench";
import { setModalAriaHidden } from "@/utils/commonUtils";
export default {
  components: {
    SearchBox,
    SearchForm,
    DataBox,
    OperationArea,
    TimeLine,
    ImportModal,
    ExamineModal,
    TranslateModal,
    ExamineTranslateModal,
    ArchiveModal,
    SendOutlined,
  },
  data() {
    return {
      name: "workbench",
      // 汉化包
      locale: locale,
      toDoNum: 0,
      finishNum: 0,
      labelCol: { style: { width: "84px" } },
      lastSearch: {},
      search: {
        name: "",
        productName: "",
        translateType: "",
        department: "",
        auditor: "",
        creator: "",
      },
      tableHeight: { x: "max-content", y: 0 },
      // tableHeight: { x: "100%", y: 0 },
      loading: false,
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 40,
          customRender: (text) => {
            const currentIndex =
              text.index +
              1 +
              this.pagination.pageSize * (this.pagination.current - 1);
            return currentIndex;
          },
          fixed: "left",
          index: 0.1,
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
          title: "任务描述",
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
      selectedLanguage: null, // 新增：用于存储用户选择的语言
      translateTypes: [], // 新增：下拉框的语言选项
      translateTypeVisible: false, // 新增：控制语言选择弹窗的显示与隐藏
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
      pagination: {
        showSizeChanger: true,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
      pageChangeSearch: {},
    };
  },
  mounted() {
    let _this = this;
    this.$nextTick(() => {
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
  methods: {
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
    // 获取翻译语言
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
        // console.log("翻译语言:", res.data.list);
      });
    },
    // 更改翻译语种
    SelectTranslateType() {
      if (this.selectedRows.length === 0) {
        message.warning("请至少选择一条任务");
        return;
      }
      this.translateTypeVisible = true; // 显示语言选择弹窗
      setModalAriaHidden(this, document);
      this.selectedLanguage = null; // 重置选择的语言
    },
    // 点击 confirm确认 按钮后会发生下面的操作（弹窗）
    async confirmTranslateType() {
      if (!this.selectedLanguage) {
        // 未选择语言
        message.warning("请选择一种语言");
        return;
      }
      // 单条更新任务，批量循环调用
      for (const task of this.selectedRows) {
        if (task.translateType != this.selectedLanguage) {
          task.translateType = this.selectedLanguage;
          updateTaskInfo(task).then((res) => {
            this.showOperationArea = false;
            this.init();
            message.success(
              `“${task.name}”的翻译语种已更改为${task.translateType}！`
            );
            // console.log("当前翻译语种", this.selectedLanguage,task);
          });
        } else {
          message.success(
            `“${task.name}”的翻译语种已为${task.translateType}，无需更改。`
          );
        }
      }

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
    // 表格复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
    },
    // 表格复选框点击事件
    onSelect(record, selected) {
      if (this.createVersionFlag) {
        // 创建版本时使用
        if (selected) {
          this.selectEntry.push(record);
        } else {
          this.selectEntry = this.selectEntry.filter((item) => {
            return item.id !== record.id;
          });
        }
      }
    },
    // 表格全选/反选框点击事件
    onSelectAll(selected, selectedRows, changeRows) {
      if (this.createVersionFlag) {
        if (selected) {
          this.selectEntry = this.selectEntry.concat(changeRows);
        } else {
          changeRows.forEach((item) => {
            this.selectEntry = this.selectEntry.filter((entry) => {
              return entry !== item;
            });
          });
        }
      }
    },
    getRowClassName(record, index) {
      let className = null;
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
      return className;
    },
    customRow(record, index) {
      return {
        onClick: (event) => {
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
        // 设置列表父元素高度
        let box = this.$refs.box.offsetHeight;
        let searchHeight = this.$refs.search.$el.offsetHeight;
        try {
          let operationAreaHeight = this.$refs.operationArea.$el.offsetHeight;
          this.dataHeight = box - searchHeight - operationAreaHeight - 32;
        } catch (error) {
          this.dataHeight = box - searchHeight - 32;
        }

        // 设置表格高度
        let buttonHeight = 0;
        try {
          buttonHeight = this.$refs.button.offsetHeight + 8;
        } catch (error) {}
        this.tableHeight.y = this.dataHeight - buttonHeight - 150;
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
    },
    // 获取任务
    getTask() {
      this.getTaskByCondition(this.search);
    },
    // 根据条件获取任务
    getTaskByCondition(data) {
      this.loading = true;
      this.checkSearchChange();
      let params = {
        pageIndex: this.pagination.current,
        pageSize: this.pagination.pageSize,
      };
      if (this.activeCard === 1) {
        // 待办事项
        getToDoTaskInfo(params, data)
          .then(async (res) => {
            this.dataSource = res.data.list;
            this.pagination.total = res.data.totalNum;
            for (const item of this.dataSource) {
              if (await this.getTaskPending(item.id)) {
                item.isHighlighted = true;
              } else {
                item.isHighlighted = false;
              }
            }
          })
          .catch((err) => {
            message.error("数据获取失败！", err.message);
          })
          .finally(() => {
            this.loading = false;
          });
      } else if (this.activeCard === 2) {
        // 已办事项
        getFinishTaskInfo(params, data)
          .then((res) => {
            this.dataSource = res.data.list;
            this.pagination.total = res.data.totalNum;
          })
          .catch((err) => {
            message.error("数据获取失败！", err.message);
          })
          .finally(() => {
            this.loading = false;
          });
      }
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
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;

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
</style>