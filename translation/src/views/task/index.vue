<template>
  <div class="box" ref="box">
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <a-form-item label="任务名称" name="name">
            <a-input v-model:value="search.name" placeholder="请输入任务名称" size="small"></a-input>
          </a-form-item>
          <a-form-item label="执行部门" name="department">
            <!-- <a-input v-model:value="search.department" placeholder="请输入执行部门" size="small"></a-input> -->
            <a-select v-model:value="search.department" style="width: 186px" placeholder="请选择执行部门" :options='departments' size="small" allowClear>
            </a-select>
          </a-form-item>
          <a-form-item label="产品名称" name="productName">
            <a-input v-model:value="search.productName" placeholder="请输入产品名称" size="small"></a-input>
          </a-form-item>
          <a-form-item label="开发员" name="developer">
            <a-input v-model:value="search.developer" placeholder="请输入开发员" size="small"></a-input>
          </a-form-item>
          <a-form-item label="词条审核员" name="entryAuditor">
            <a-input v-model:value="search.entryAuditor" placeholder="请输入词条审核员" size="small"></a-input>
          </a-form-item>
          <a-form-item label="翻译员" name="translator">
            <a-input v-model:value="search.translator" placeholder="请输入翻译员" size="small"></a-input>
          </a-form-item>
          <a-form-item label="翻译审核员" name="translationAuditor">
            <a-input v-model:value="search.translationAuditor" placeholder="请输入翻译审核员" size="small"></a-input>
          </a-form-item>
          <a-form-item label="任务管理员" name="creator">
            <a-input v-model:value="search.creator" placeholder="请输入任务管理员(归档)" size="small"></a-input>
          </a-form-item>
          <a-form-item label="翻译语种" name="language">
            <a-select v-model:value="search.translateType" style="width: 186px" placeholder="请选择翻译语种" :options='translateTypes'
              :fieldNames="{label:'name',value:'name'}" size="small" allowClear>
            </a-select>
          </a-form-item>
          <a-form-item label="任务状态" name="state">
            <a-select v-model:value="search.state" style="width: 186px" placeholder="请选择任务状态" :options='states' size="small" allowClear>
            </a-select>
          </a-form-item>
        </a-form>
      </template>
      <template v-slot:operate>
        <a-button type="primary" size="middle" class="resetBtn" @click="reset">重置</a-button>
        <a-button type="primary" size="middle" @click="query">查询</a-button>
      </template>
    </SearchBox>
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <template v-slot:operate>
        <div ref="button" v-if="true" style="margin-bottom:8px;display:flex;gap:8px">
          <a-button type="primary" size="small" @click="deleteTaskByAdmin" danger v-if="isAdmin"><template #icon>
              <DeleteOutlined />
            </template>无限制删除</a-button>
          <a-button type="primary" size="small" @click="handleAdd"><template #icon>
              <PlusOutlined />
            </template>新增</a-button>
          <a-button type="primary" size="small" @click="deleteTask" danger><template #icon>
              <DeleteOutlined />
            </template>删除</a-button>
          <a-popover v-model:visible="copyVisible" trigger="click" placement="bottom">
            <template #content>
              <a-input v-model:value="copyNumber" addon-before="复制" addon-after="条" type="number" style="width:180px" />
              <div style="width:100%;margin-top:5px;display: flex;justify-content: center;">
                <a @click="copy">确定</a>
              </div>
            </template>
            <a-button type="primary" size="small"><template #icon>
                <CopyOutlined />
              </template>复制</a-button>
          </a-popover>
          <a-button type="primary" size="small" @click="batchSave"><template #icon>
              <SaveOutlined />
            </template>保存</a-button>
          <a-button type="primary" size="small" class="resetBtn" @click="submitTask"><template #icon>
              <SendOutlined />
            </template>下发任务</a-button>
        </div>
      </template>
      <template v-slot:data>
        <div style="width:100%;position: absolute;">
          <a-form ref="tableFormRef" :model="dataSource" :label-col="{ style: { width: '10px' } }" :wrapper-col="{ span: 0 }" :rules="rules">
            <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource"
              :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" :row-key="record => record.id" :scroll="tableHeight"
              :pagination='pagination' :loading="loading" :rowClassName="getRowClassName" ref="taskTable" @resizeColumn="handleResizeColumn"
              :customRow="customRow">
              <template #bodyCell="{ column, text,index, record }">
                <template v-if="['name'].includes(column.dataIndex)">
                  <template v-if="editableData[record.id]">
                    <a-form-item label=" " :name="[index, column.dataIndex]" :rules="rules[column.dataIndex]">
                      <a-input @click="clickInput" v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" />
                    </a-form-item>
                  </template>
                  <template v-else>
                    {{ text }}
                  </template>
                </template>
                <template v-else-if="['description'].includes(column.dataIndex)">
                  <template v-if="editableData[record.id]">
                    <a-input @click="clickInput" v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" />
                  </template>
                  <template v-else>
                    {{ text }}
                  </template>
                </template>
                <!-- <template v-if="'department' === column.dataIndex">
                  <template v-if="editableData[record.id]">
                    <a-select v-model:value="editableData[record.id][column.dataIndex]" style="width: 100%" placeholder="请选择" :options='departments'
                      @click="clickInput" @change="changeDepartment(record)">
                    </a-select>
                  </template>
                  <template v-else>
                    {{ text }}
                  </template>
                </template> -->
                <template v-else-if="'productName' === column.dataIndex">
                  <template v-if="editableData[record.id]&&!editableData[record.id].isSubmit">
                    <a-form-item label=" " :name="[index, 'productId']" :rules="rules[column.dataIndex]">
                      <!-- <a-select v-model:value="editableData[record.id]['productId']" style="width: 85%" placeholder="请选择"
                        :options='options[record.id]["products"]' :fieldNames='{label:"name",value:"id"}' @click="clickInput"
                        @change="changeProduct(record)">
                      </a-select> -->
                      <a-tree-select v-model:value="editableData[record.id]['productId']" v-model:searchValue="searchValue" show-search
                        style="width: 85%" :dropdown-style="{ maxHeight: '400px', overflow: 'auto',minWidth: '400px' }" placeholder="请选择" allow-clear
                        tree-default-expand-all :tree-data="options[record.id]['products']" tree-node-filter-prop="title"
                        :fieldNames="{children:'children', label:'title', value: 'key'}" :treeDefaultExpandAll="false" @click="clickInput"
                        @change="changeProduct(record)">
                        <!-- <template #title="{ title }">
                          <template v-for="(fragment, i) in title.toString().split(new RegExp(`(?<=${searchValue})|(?=${searchValue})`, 'i'))">
                            <span v-if="fragment.toLowerCase() === searchValue.toLowerCase()" :key="i" style="color: #08c">
                              {{ fragment }}
                            </span>
                            <template v-else>{{ fragment }}</template>
                          </template>
                        </template> -->
                      </a-tree-select>
                      <PlusCircleOutlined class="editable-cell-icon" style="color:#369FFF;margin-left:5px" @click.stop="addProduct(record)" />
                    </a-form-item>
                  </template>
                  <template v-else>
                    {{ text }}
                  </template>
                </template>
                <template v-else-if="'versionName' === column.dataIndex">
                  <template v-if="editableData[record.id]&&!editableData[record.id].isSubmit">
                    <!-- <a-form-item label=" " :name="[index, 'productId']" :rules="rules[column.dataIndex]">
                      <a-select v-model:value="editableData[record.id]['versionId']" style="width: 85%" placeholder="请选择"
                        :options='options[record.id]["versions"]' @click="clickInput">
                      </a-select>
                      <PlusCircleOutlined class="editable-cell-icon" style="color:#369FFF;margin-left:5px" @click.stop="addVersion(record)" />
                    </a-form-item> -->
                    <a-select v-model:value="editableData[record.id]['versionId']" allowClear style="width: 85%" placeholder="请选择"
                      :options='options[record.id]["versions"]' @click="clickInput">
                    </a-select>
                    <PlusCircleOutlined class="editable-cell-icon" style="color:#369FFF;margin-left:5px" @click.stop="addVersion(record)" />
                  </template>
                  <template v-else>
                    {{ text }}
                  </template>
                </template>
                <template v-else-if="['developer','entryAuditor','translator','translationAuditor','translateType'].includes(column.dataIndex)">
                  <template v-if="editableData[record.id]&&!editableData[record.id].isSubmit">
                    <a-select v-model:value="editableData[record.id][column.dataIndex]" style="width: 100%" placeholder="请选择"
                      :options='options[record.id][column.dataIndex]' @click="clickInput" allowClear>
                    </a-select>
                  </template>
                  <template v-else>
                    {{ text }}
                  </template>
                </template>
                <!-- 创建人不可编辑，在复制和创建时会填写当前用户，只有超管可以编辑 -->
                <template v-else-if="['creator'].includes(column.dataIndex)">
                  <template v-if="editableData[record.id]&&isAdmin">
                    <a-select v-model:value="editableData[record.id][column.dataIndex]" style="width: 100%" placeholder="请选择"
                      :options='options[record.id][column.dataIndex]' @click="clickInput" allowClear>
                    </a-select>
                  </template>
                  <template v-else>
                    {{ text }}
                  </template>
                </template>
                <template v-else-if="column.dataIndex === 'state'">
                  <TaskStateBadge type="sum" :taskState="text" />
                </template>
                <template v-else-if="column.dataIndex === 'operation'">
                  <div class="editable-row-operations">
                    <span v-if="editableData[record.id]">
                      <a-button type="primary" ghost size="small" @click.stop="save(record.id)">保存</a-button>
                      <!-- <a-popconfirm title="是否取消?" ok-text='是' cancel-text='否' @confirm="cancel">
                        <a-button type="primary" ghost size="small" danger>取消</a-button>
                      </a-popconfirm> -->
                      <a-button type="primary" ghost size="small" danger @click.stop="cancel(record.id)">取消</a-button>
                    </span>
                    <span v-else>
                      <!-- <a-button type="primary" ghost size="small" @click.stop="edit(record)">编辑</a-button> -->
                      <a-button type="primary" ghost size="small" @click.stop="viewProcess(record)">查看</a-button>
                    </span>
                  </div>
                </template>
              </template>
            </a-table>
          </a-form>
        </div>
      </template>
    </DataBox>
    <OperationArea ref="operationArea" :title="operationAreaTitle" :height="operationAreaHeight" v-if="showOperationArea" @close="closeOperationArea">
      <template v-slot:content>
        <TimeLine :showButton="false" :currentTask="currentTask" ref="timeLine"></TimeLine>
      </template>
    </OperationArea>
  </div>
  <ProductModal :visible="addProductVisible" :currentTask="addProductTask" @productClose="addProductClose" @productOk="addProductOk" />
  <VersionModal :visible="addVersionVisible" :currentVersion="addProductTask" @versionClose="addVersionClose" @versionOk="addVersionOk" />
</template>
<script>
import { message, Modal } from "ant-design-vue";
import locale from "ant-design-vue/es/date-picker/locale/zh_CN";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import OperationArea from "@/components/operationArea/index.vue";
import TimeLine from "@/components/timeLine/index.vue";
import TaskStateBadge from "@/components/stateBadge/taskStateBadge.vue";
import ProductModal from "@/views/task/productModal.vue";
import VersionModal from "@/views/task/versionModal.vue";
// import commen from "@/views/entry/common.js";
import { getCurrentFormattedTime } from "@/utils/dateUtils";
import { cloneDeep, flatMap } from "lodash-es";
import {
  PlusOutlined,
  DeleteOutlined,
  CopyOutlined,
  SaveOutlined,
  SendOutlined,
  PlusCircleOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons-vue";
import {
  searchTaskInfo,
  addTaskInfos,
  deleteTaskInfo,
  updateTaskInfo,
  taskSubmission,
  taskCreateNewLanguageTask,
} from "@/http/api/task";
import { getProduct } from "@/http/api/product";
import { getVersion } from "@/http/api/productVersion";
import { getRoleUserByDepartment, getDepartments } from "@/http/api/user";
import { getLanguage } from "@/http/api/translate";
import { getClassTree } from "@/http/api/entryManage";
import { setTableHeight } from "@/utils/tableUtils";
import { setModalAriaHidden } from "@/utils/domUtils";
import { defineComponent, ref, createVNode } from "vue";
export default {
  components: {
    SearchBox,
    DataBox,
    OperationArea,
    TimeLine,
    TaskStateBadge,
    ProductModal,
    VersionModal,
    PlusOutlined,
    DeleteOutlined,
    CopyOutlined,
    SaveOutlined,
    SendOutlined,
    PlusCircleOutlined,
  },
  data() {
    return {
      user: {
        userName: "",
        department: "",
      },
      isAdmin: false,
      locale: locale,
      labelCol: { style: { width: "84px" } },
      search: {
        name: "",
        productId: null,
        translateType: null,
        department: null,
        state: null,
        developer: "",
        entryAuditor: "",
        translator: "",
        translationAuditor: "",
        creator: "",
      },
      tableTitle: "任务列表",
      dataHeight: 400,
      // tableHeight: { x: "100%", y: 0 },
      tableHeight: { x: "max-content", y: 0 },
      loading: false,
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 50,
          customRender: (text, record, index, column) => {
            return (
              text.index +
              1 +
              this.pagination.pageSize * (this.pagination.current - 1)
            );
          },
          fixed: "left",
        },
        {
          title: "任务名称",
          dataIndex: "name",
          align: "center",
          width: 150,
          fixed: "left",
          resizable: true,
        },
        {
          title: "执行部门",
          dataIndex: "department",
          align: "center",
          width: 150,
          resizable: true,
        },
        {
          title: "产品名称",
          dataIndex: "productName",
          align: "center",
          width: 230,
          resizable: true,
        },
        {
          title: "产品版本",
          dataIndex: "versionName",
          align: "center",
          width: 180,
          resizable: true,
        },
        {
          title: "翻译语种",
          dataIndex: "translateType",
          align: "center",
          width: 150,
        },
        {
          title: "开发员",
          dataIndex: "developer",
          align: "center",
          width: 150,
        },
        {
          title: "词条审核员",
          dataIndex: "entryAuditor",
          align: "center",
          width: 150,
        },
        {
          title: "翻译员",
          dataIndex: "translator",
          align: "center",
          width: 150,
        },
        {
          title: "翻译审核员",
          dataIndex: "translationAuditor",
          align: "center",
          width: 150,
        },
        {
          title: "任务管理员",
          dataIndex: "creator",
          align: "center",
          width: 150,
        },
        {
          title: "任务描述",
          dataIndex: "description",
          align: "center",
          width: 230,
          ellipsis: true,
          resizable: true,
        },
        {
          title: "下发时间",
          dataIndex: "deliveryTime",
          align: "center",
          width: 200,
        },
        {
          title: "结束时间",
          dataIndex: "endTime",
          align: "center",
          width: 200,
        },
        {
          title: "任务状态",
          dataIndex: "state",
          align: "center",
          width: 100,
          fixed: "right",
        },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          width: 150,
          fixed: "right",
        },
      ],
      dataSource: [],
      editableData: {},
      selectedRowKeys: [],
      selectedRows: [],
      selectedRowIndex: null,
      currentTask: {},
      options: {},
      operationAreaTitle: "流程信息",
      operationAreaHeight: 190,
      showOperationArea: false,
      timer: null,
      departments: [],
      copyVisible: false,
      copyNumber: 1,
      states: [
        { label: "新建", value: "0" },
        { label: "流程中", value: "1,2,3,4,5" },
        { label: "已完成", value: "6" },
      ],
      translateTypes: [],
      addProductVisible: false,
      addProductTask: "",
      addVersionVisible: false,
      copyTaskEntry: {},
      rules: {
        name: [{ required: true, message: "请输入" }],
        productName: [{ required: true, message: "请选择" }],
        versionName: [{ required: true, message: "请选择" }],
        translateType: [{ required: true, message: "请选择" }],
      },
      searchValue: "",
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
  },
  unmounted() {
    //注销window.onresize事件
    window.onresize = null;
  },
  methods: {
    // 初始化
    init() {
      this.user = this.$store.state.user;
      this.isAdmin = this.user.roleName.includes("超级管理员");
      this.setTableHeight();
      this.searchTaskInfo();
      this.getDepartments();
      this.getLanguage();
    },
    // 动态设置表格高度
    setTableHeight() {
      this.$nextTick(() => {
        setTableHeight(this, 8, 166, 0);
      });
    },
    // 获取执行部门
    getDepartments() {
      getDepartments().then((res) => {
        this.departments = [];
        res.data.list.forEach((item) => {
          let d = {
            label: item,
            value: item,
          };
          this.departments.push(d);
        });
      });
    },
    // 获取翻译语种
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
      });
    },
    // 查询按钮点击事件
    query() {
      this.pageChangeSearch = this.search;
      this.searchTaskInfo();
    },
    // 获取任务列表
    async searchTaskInfo() {
      await this.searchTaskByCondition(this.search);
    },
    async searchTaskByCondition(data) {
      this.loading = true;
      let params = {
        pageIndex: this.pagination.current,
        pageSize: this.pagination.pageSize,
      };
      await searchTaskInfo(data, params)
        .then((res) => {
          this.dataSource = res.data.list;
          this.loading = false;
          this.pagination.total = res.data.totalNum;
        })
        .catch((err) => {
          this.loading = false;
          message.error(err.message);
        });
    },
    clickInput(event) {
      event.stopPropagation();
    },
    // 关闭流程操作区
    closeOperationArea() {
      this.showOperationArea = false;
      this.setTableHeight();
      this.selectedRowIndex = null;
    },
    // 设置表格每一行的class
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
    // 添加表格行点击事件
    customRow(record, index) {
      return {
        // onClick: (event) => {
        //     let _this = this
        //     clearTimeout(this.timer)
        //     this.timer = setTimeout(function () {
        //         _this.selectedRowIndex = record.id
        //         _this.currentTask = record
        //         _this.showOperationArea = true
        //         _this.setTableHeight()
        //     }, 300);
        // },
        onDblclick: (event) => {
          // clearTimeout(this.timer)
          // this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
          if (this.editableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }
          // 0-待分配 1-待开发员确认 2-待词条审核员确认 3-待翻译员确认 4-待翻译审核员确认 5-已完成 6-已归档
          if (record.state === "6") {
            message.info("当前任务已归档，不可编辑！");
          } else {
            if (record.state != "0") {
              console.log("当前任务已下发，不可编辑使用人员！");
            }
            this.edit(record);
          }
        },
      };
    },
    //新增
    handleAdd() {
      this.pagination.current = 1; // 重置分页
      const newData = {
        id: `new${this.dataSource.length + 1}`,
        name: "",
        state: "0",
        department: this.user.department,
        creator: this.user.userName, // 任务管理员-创建人-归档
        developer: this.user.userName, // 开发员
        entryAuditor: this.user.userName, // 词条审核员
        translator: this.user.userName, // 翻译员
        translationAuditor: this.user.userName, // 翻译审核员
        translateType: "英文",
        versionId: null,
        versionName: null,
        isSubmit: false,
      };
      this.options[newData.id] = {
        products: [],
        version: [],
        developer: [],
      };
      // this.dataSource.push(newData); //在数组末尾插入元素
      this.dataSource.unshift(newData); // 在数组开头插入元素
      this.editableData[newData.id] = newData;
      this.getOptions(newData);
      // 滚动表格
      this.$nextTick(() => {
        let container =
          this.$refs.taskTable.$el.querySelector(".ant-table-body");
        // container.scrollTop = container.scrollHeight  //表格滚动到最底部
        container.scrollTop = 0; //表格滚动到顶部
      });
    },
    // 查看流程
    edit(record) {
      // 获取选择菜单数据
      this.getOptions(record);
      this.editableData[record.id] = cloneDeep(
        this.dataSource.filter((item) => record.id === item.id)[0]
      );
      if (record.state === "0") {
        this.editableData[record.id].isSubmit = false;
      } else {
        this.editableData[record.id].isSubmit = true;
      }
    },
    // 保存
    save(id) {
      // 校验必填字段
      this.$refs.tableFormRef
        .validate()
        .then(() => {
          // 保存词条
          this.saveEntry(id);
        })
        .catch((err) => {
          message.error(err.message);
        });
    },
    saveEntry(id) {
      // console.log(this.editableData[id])
      let falg = this.checkTask(id);
      if (!falg) {
        return;
      }
      if (id.startsWith("new")) {
        // 新增
        let data = [this.editableData[id]];
        addTaskInfos(data).then((res) => {
          message.success("新增成功！");
          this.searchTaskInfo();
          delete this.editableData[id];
        });
      } else if (id.startsWith("copy")) {
        if (this.copyTaskEntry[id] === 1) {
          // 使用原任务已导入的任务
          this.loading = true;
          let first = id.indexOf("_");
          let end = id.lastIndexOf("_");
          let copyTaskId = id.substring(first + 1, end);
          let params = {
            taskID: copyTaskId,
          };
          this.editableData[id].upgrade = 1;
          this.editableData[id].state = "0";
          let currentDate = this.getCurrentDate();
          this.editableData[id].createTime = currentDate;
          this.editableData[id].endTime = null;
          this.editableData[id].entryAutiorStartTime = null;
          this.editableData[id].translationAuditorStartTime = null;
          this.editableData[id].translateStartTime = null;
          this.editableData[id].deliveryTime = null;
          taskCreateNewLanguageTask(params, this.editableData[id])
            .then((res) => {
              message.success("已保存！");
              delete this.copyTaskEntry[id];
              delete this.editableData[id];
              this.searchTaskInfo();
            })
            .catch((err) => {
              message.error("复制失败！", err.message);
              this.loading = false;
            });
        } else {
          // 新增
          let data = [this.editableData[id]];
          addTaskInfos(data).then((res) => {
            message.success("新增成功！");
            this.searchTaskInfo();
            delete this.editableData[id];
          });
        }
      } else {
        // 编辑
        updateTaskInfo(this.editableData[id]).then((res) => {
          message.success("编辑成功！");
          this.searchTaskInfo();
          delete this.editableData[id];
        });
      }
    },
    checkTask(id) {
      //1、开发员和词条审核员必须成对出现
      //2、翻译员和翻译审核员必须成对出现
      //3、(开发员、词条审核员) 和 (翻译员、翻译审核员) 必须出现一对
      let newTask = this.editableData[id];
      if (
        !this.isEmptyString(newTask.developer) &&
        this.isEmptyString(newTask.entryAuditor)
      ) {
        message.info("请选择词条审核员！");
        return false;
      }
      if (
        !this.isEmptyString(newTask.entryAuditor) &&
        this.isEmptyString(newTask.developer)
      ) {
        message.info("请选择开发员！");
        return false;
      }
      if (
        !this.isEmptyString(newTask.translator) &&
        this.isEmptyString(newTask.translationAuditor)
      ) {
        message.info("请选择翻译审核员！");
        return false;
      }
      if (
        !this.isEmptyString(newTask.translationAuditor) &&
        this.isEmptyString(newTask.translator)
      ) {
        message.info("请选择翻译员！");
        return false;
      }
      if (
        this.isEmptyString(newTask.translationAuditor) &&
        this.isEmptyString(newTask.translator) &&
        this.isEmptyString(newTask.developer) &&
        this.isEmptyString(newTask.entryAuditor)
      ) {
        message.info("请选择操作人员！");
        return false;
      }
      return true;
    },
    isEmptyString(value) {
      return value === null || value === "" || value === undefined;
    },

    // 批量保存
    async batchSave() {
      // 校验必填字段
      await this.$refs.tableFormRef.validate();
      // 保存
      await this.batchSaveEntry();
    },
    async batchSaveEntry() {
      let add = [];
      let edit = [];
      let copy = [];
      for (let key in this.editableData) {
        // console.log(this.editableData[key])
        let id = this.editableData[key].id;
        if (id.startsWith("new")) {
          add.push(this.editableData[key]);
        } else if (id.startsWith("copy")) {
          if (this.copyTaskEntry[id] === 1) {
            copy.push(this.editableData[key]);
          } else {
            add.push(this.editableData[key]);
          }
        } else {
          edit.push(this.editableData[key]);
        }
      }
      if (add.length === 0 && edit.length === 0 && copy.length === 0) {
        return;
      }
      Modal.confirm({
        title: "是否全部保存?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "确定",
        cancelText: "取消",
        style: { top: "30%" },
        onOk: async () => {
          this.loading = true;
          const promises = [];
          // 新增接口
          if (add.length > 0) {
            add.forEach((item) => {
              promises.push(
                addTaskInfos(item).then((res) => {
                  delete this.editableData[item.id];
                })
              );
            });
          }
          // 修改接口
          if (edit.length > 0) {
            edit.forEach((item) => {
              promises.push(
                updateTaskInfo(item).then((res) => {
                  delete this.editableData[item.id];
                })
              );
            });
          }
          if (copy.length > 0) {
            const currentDate = await this.getCurrentDate();
            copy.forEach((item) => {
              promises.push(
                (async () => {
                  let first = item.id.indexOf("_");
                  let end = item.id.lastIndexOf("_");
                  let copyTaskId = item.id.substring(first + 1, end);
                  let params = {
                    taskID: copyTaskId,
                  };

                  item.upgrade = 1;
                  item.state = "0";
                  item.createTime = currentDate;
                  item.endTime = null;
                  item.entryAutiorStartTime = null;
                  item.translationAuditorStartTime = null;
                  item.translateStartTime = null;
                  item.deliveryTime = null;

                  await taskCreateNewLanguageTask(params, item);
                  delete this.copyTaskEntry[item.id];
                  delete this.editableData[item.id];
                })()
              );
            });
          }

          try {
            // 等待所有异步操作完成
            await Promise.all(promises);
            // 重新加载数据
            await this.searchTaskInfo();
            this.editableData = {};
            message.success("保存成功！");
          } catch (error) {
            message.error("保存失败：" + error.message);
          } finally {
            this.loading = false;
          }
        },
      });
    },
    // 取消
    cancel(id) {
      delete this.editableData[id];
      delete this.copyTaskEntry[id];
      if (id.startsWith("new") || id.startsWith("copy")) {
        //从dataSource中删除
        this.dataSource.some((item, i) => {
          if (item.id === id) {
            this.dataSource.splice(i, 1);
            return true;
          }
        });
      }
    },
    // 删除任务
    deleteTask() {
      if (this.selectedRowKeys.length === 0) {
        message.info("请选择需要删除的任务！");
        return;
      }
      let flag = false;
      this.selectedRows.forEach((item) => {
        if (item.state != "0") {
          flag = true;
        }
      });
      if (flag) {
        message.error("已下发或已归档的任务不可删除！");
        return;
      }
      Modal.confirm({
        title: "是否确定删除?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "确定",
        cancelText: "取消",
        style: { top: "30%" },
        onOk: () => {
          deleteTaskInfo(this.selectedRowKeys).then((res) => {
            message.success("删除成功！");
            this.searchTaskInfo();
            this.selectedRowKeys = [];
            this.selectedRows = [];
          });
        },
      });
    },
    // 无限制删除-仅限超管
    deleteTaskByAdmin() {
      if (this.selectedRowKeys.length === 0) {
        message.info("请选择需要删除的任务！");
        return;
      }
      Modal.confirm({
        title: "是否确定删除?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "确定",
        cancelText: "取消",
        style: { top: "30%" },
        onOk: () => {
          deleteTaskInfo(this.selectedRowKeys).then((res) => {
            message.success("删除成功！");
            this.searchTaskInfo();
            this.selectedRowKeys = [];
            this.selectedRows = [];
          });
        },
      });
    },
    // 任务下发
    submitTask() {
      if (this.selectedRows.length === 0) {
        message.info("请选择需要下发的任务！");
        return;
      }
      Modal.confirm({
        title: "是否确定下发?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "确定",
        cancelText: "取消",
        style: { top: "30%" },
        onOk: () => {
          let ids = [];
          this.selectedRows.forEach((item) => {
            if (item.state === "0") {
              // 新建状态
              ids.push(item.id);
            } else {
            }
          });
          if (ids.length === 0) {
            return;
          }
          taskSubmission(ids).then((res) => {
            this.searchTaskInfo();
            message.success("下发成功！");
            this.selectedRowKeys = [];
            this.selectedRows = [];
          });
        },
      });
    },
    dealData(param) {
      return param.map((item) => ({
        ...item,
        disabled: item.type != "product" ? true : false,
        children: item.children ? this.dealData(item.children) : [],
      }));
    },
    // 获取可编辑行下拉菜单的选项
    getOptions(record) {
      let products = [];
      let versions = [];
      let op = {
        products: products,
        versions: versions,
      };
      this.options[record.id] = op;
      // console.log(this.options[record.id])
      // 获取部门产品列表
      // let product = {
      //     // department: record.department
      //     department: this.user.department
      // }
      // getProduct(product).then((res) => {

      //     this.options[record.id].products = res.data.list
      // })
      let product = {
        department: "",
        className: record.department,
      };
      getClassTree(product).then((res) => {
        this.options[record.id].products = res.data.list;
        // console.log(this.options[record.id].products)
        this.options[record.id].products = this.dealData(
          this.options[record.id].products
        );
      });
      // 获取产品版本列表
      if (record.productId != null) {
        let version = {
          productId: record.productId,
        };
        getVersion(version).then((res) => {
          res.data.list.forEach((item) => {
            let v = {
              label: item.name,
              value: item.id,
            };
            this.options[record.id].versions.push(v);
          });
        });
      }
      // 获取部门下的 开发员、词条审核员、翻译员、翻译审核员
      let params = {
        department: record.department,
      };
      getRoleUserByDepartment(params).then((res) => {
        let data = res.data;
        if (data.DEVELOPER) {
          let developer = [];
          data.DEVELOPER.forEach((item) => {
            let op = {
              label: item.userName,
              value: item.userName,
            };
            developer.push(op);
          });
          developer.push({ label: "无", value: "" });
          this.options[record.id].developer = developer;
          this.options[record.id].creator = developer; // 任务管理员-创建人-归档，没有data.CREATOR，所以这边我借用了开发者的options
        }
        if (data.ENTRY_AUDITOR) {
          let entryAuditor = [];
          data.ENTRY_AUDITOR.forEach((item) => {
            let op = {
              label: item.userName,
              value: item.userName,
            };
            entryAuditor.push(op);
          });
          entryAuditor.push({ label: "无", value: "" });
          this.options[record.id].entryAuditor = entryAuditor;
        }
        if (data.TRANSLATOR) {
          let translateor = [];
          data.TRANSLATOR.forEach((item) => {
            let op = {
              label: item.userName,
              value: item.userName,
            };
            translateor.push(op);
          });
          translateor.push({ label: "无", value: "" });
          this.options[record.id].translator = translateor;
        }
        if (data.TRANSLATE_AUDITOR) {
          let translationAuditor = [];
          data.TRANSLATE_AUDITOR.forEach((item) => {
            let op = {
              label: item.userName,
              value: item.userName,
            };
            translationAuditor.push(op);
          });
          translationAuditor.push({ label: "无", value: "" });
          this.options[record.id].translationAuditor = translationAuditor;
        }
      });
      // console.log(this.options)
    },
    // 部门选择触发事件
    changeDepartment(record) {
      // console.log(this.editableData[record.id])
      // 部门选择时  将产品和版本清空
      this.editableData[record.id].productId = null;
      this.editableData[record.id].productName = null;
      this.editableData[record.id].versionId = null;
      this.editableData[record.id].versionName = null;
      this.getOptions(this.editableData[record.id]);
    },
    // 产品选择触发事件
    changeProduct(record) {
      // 将版本清空
      this.editableData[record.id].versionId = null;
      this.editableData[record.id].versionName = null;
      this.getOptions(this.editableData[record.id]);
    },
    // 复制
    copy() {
      this.copyVisible = false;
      if (this.copyNumber < 1) {
        this.copyNumber = 1;
        return;
      }
      if (this.selectedRows.length != 1) {
        message.info("请选择一条需要复制的任务！");
        return;
      }
      let task = this.selectedRows[0];
      let copyIds = [];
      // 查询当前任务执行部门下的产品
      for (let i = 1; i <= this.copyNumber; i++) {
        let copyTask = cloneDeep(task);
        let id = "copy_" + copyTask.id + "_" + (this.dataSource.length + 1);
        copyTask.id = id;
        copyTask.state = "0";
        copyTask.name = copyTask.name + "(复制)";
        copyTask.deliveryTime = null;
        copyTask.createTime = getCurrentFormattedTime();
        copyTask.endTime = null;
        copyTask.creator = this.user.userName; // 任务管理员-创建人-归档
        copyTask.isSubmit = false;

        this.dataSource.unshift(copyTask);
        this.options[id] = {
          products: [],
          version: [],
          developer: [],
        };
        this.editableData[id] = copyTask;
        this.getOptions(this.editableData[id]);
        copyIds.push(id);
      }

      let _this = this;
      // 使用原任务已导入的词条-后端查询复杂度nm，会缓冲池爆掉，暂时封掉该方法
      // Modal.confirm({
      //   title: "是否使用原任务已导入的词条?",
      //   icon: createVNode(ExclamationCircleOutlined),
      //   content: "",
      //   okText: "是",
      //   cancelText: "否",
      //   style: { top: "30%" },
      //   onOk() {
      //     // console.log('OK');
      //     copyIds.forEach((item) => {
      //       _this.copyTaskEntry[item] = 1;
      //     });
      //   },
      //   onCancel() {
      //     // console.log('Cancel');
      //     copyIds.forEach((item) => {
      //       _this.copyTaskEntry[item] = 0;
      //     });
      //   },
      // });
      copyIds.forEach((item) => {
        _this.copyTaskEntry[item] = 0;
      });

      this.copyNumber = 1;
      this.selectedRowKeys = [];
      this.selectedRows = [];
      // 滚动到最底部
      this.$nextTick(() => {
        let container =
          this.$refs.taskTable.$el.querySelector(".ant-table-body");
        container.scrollTop = 0;
      });
    },
    // 表格列可伸缩
    handleResizeColumn: (w, col) => {
      col.width = w;
    },
    // 表格复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
    },
    // 添加产品
    addProduct(record) {
      // message.info("添加产品！")
      this.addProductTask = this.editableData[record.id];
      this.addProductTask.allProducts = this.options[record.id].products;
      this.addProductVisible = true;
      setModalAriaHidden(this, document);
    },
    addProductOk(record) {
      this.addProductVisible = false;
      this.getOptions(record);
    },
    addProductClose() {
      this.addProductVisible = false;
    },
    // 添加版本
    addVersion(record) {
      let productId = this.editableData[record.id].productId;
      if (productId === null || productId === "" || productId === undefined) {
        message.info("请先选择产品！");
        return;
      }
      this.addProductTask = this.editableData[record.id];
      this.addProductTask.allVersions = this.options[record.id].versions;
      this.addVersionVisible = true;
      setModalAriaHidden(this, document);
    },
    addVersionOk(record) {
      this.addVersionVisible = false;
      this.getOptions(record);
    },
    addVersionClose() {
      this.addVersionVisible = false;
    },
    // 重置
    reset() {
      this.search = {
        name: "",
        productId: null,
        translateType: null,
        department: null,
        state: null,
        developer: "",
        entryAuditor: "",
        translator: "",
        translationAuditor: "",
        creator: "",
      };
      this.pageChangeSearch = this.search;
      this.searchTaskInfo();
    },
    // 获取当前时间
    getCurrentDate() {
      // 创建一个新的Date对象
      var currentTime = new Date();

      // 格式化为指定的日期字符串
      var formattedTime = `${currentTime.getFullYear()}-${(
        currentTime.getMonth() + 1
      )
        .toString()
        .padStart(2, "0")}-${currentTime
        .getDate()
        .toString()
        .padStart(2, "0")} ${currentTime
        .getHours()
        .toString()
        .padStart(2, "0")}:${currentTime
        .getMinutes()
        .toString()
        .padStart(2, "0")}:${currentTime
        .getSeconds()
        .toString()
        .padStart(2, "0")}`;

      return formattedTime;
    },
    // 查看任务流程
    viewProcess(record) {
      this.selectedRowIndex = record.id;
      this.currentTask = record;
      this.showOperationArea = true;
      this.setTableHeight();
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;

      this.searchTaskByCondition(this.pageChangeSearch);
    },
  },
};
</script>
<style scoped lang="less">
.box {
  width: 100%;
  height: 100%;
  // border: 1px solid red;
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