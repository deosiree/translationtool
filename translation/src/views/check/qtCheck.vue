<template>
  <div class="box" ref="box">
    <!-- 搜索框组件 -->
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <a-form-item label="词条" name="entry">
            <a-input v-model:value="search.entry" style="width: 186px" placeholder="请输入内容" size="small"></a-input>
          </a-form-item>
          <a-form-item label="所属类" name="category">
            <a-input v-model:value="search.category" style="width: 186px" placeholder="请输入内容" size="small"></a-input>
          </a-form-item>
          <a-form-item label="Tag" name="tag">
            <a-input v-model:value="search.tag" style="width: 186px" placeholder="请输入内容" size="small"></a-input>
          </a-form-item>
        </a-form>
      </template>
      <!-- 操作按钮模板 -->
      <template v-slot:operate>
        <a-button type="primary" size="middle" class="checkBtn" @click="check">校验</a-button>
      </template>
    </SearchBox>
    <!-- 数据展示框组件 -->
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <!-- 数据展示模板 -->
      <template v-slot:data>
        <div style="width:100%;position: absolute;">
          <a-form ref="tableFormRef" :model="dataSource" :label-col="{ style: { width: '10px' } }" :wrapper-col="{ span: 0 }" :rules="rules">
            <!-- 表格组件 -->
            <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource"
              :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }" :row-key="record => record.id" :pagination='pagination'
              :loading="loading" :rowClassName="getRowClassName" ref="taskTable" @resizeColumn="handleResizeColumn">
              <!-- 表格单元格模板 -->
              <template #bodyCell="{ column, record }">
                <!-- 词条列 -->
                <template v-if="column.dataIndex === 'entry'">
                  <span>{{ record.entry }}</span>
                </template>
                <!-- 所属类列 -->
                <template v-if="column.dataIndex === 'category'">
                  <span>{{ record.category }}</span>
                </template>
                <!--Tag列 -->
                <template v-if="column.dataIndex === 'tag'">
                  <span class="tag">{{ record.tag }}</span>
                </template>
                <!-- 操作列 -->
                <template v-if="column.dataIndex === 'operation'">
                  <a-button type="primary" size="small" @click="showDetail(record.id)">详情({{showDetailNum(record.id)}})</a-button>
                </template>
              </template>
            </a-table>
          </a-form>

        </div>
      </template>
    </DataBox>
  </div>
  <!-- 详情弹框 -->
  <a-modal :visible="detailModalVisible" title="关联信息" @cancel="handleCancel" @ok="handleOk">
    <a-form ref="tableDetailFormRef" :model="detailDataSource" :label-col="{ style: { width: '10px' } }" :wrapper-col="{ span: 0 }" :rules="rules">
      <a-table bordered class="ant-table-striped" :columns="detailColumns" :data-source="detailDataSource"
        :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }" :row-key="record => record.id" :pagination='pagination'
        :loading="loading" :rowClassName="getRowClassName" ref="taskTable" @resizeColumn="handleResizeColumn">
        <!-- 表格单元格模板 -->
        <template #bodyCell="{ column, record }">
          <!--tsFile列 -->
          <template v-if="column.dataIndex === 'tsFile'">
            <span>{{ record.tsFile }}</span>
          </template>
          <!--词条列 -->
          <template v-if="column.dataIndex === 'entry'">
            <span>{{ record.entry }}</span>
          </template>
          <!--翻译列 -->
          <template v-if="column.dataIndex === 'translate'">
            <span>{{ record.translate }}</span>
          </template>
        </template>
      </a-table>
    </a-form>
  </a-modal>
</template>
<script>
import { message, Modal } from "ant-design-vue";
import locale from "ant-design-vue/es/date-picker/locale/zh_CN";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import OperationArea from "@/components/operationArea/index.vue";
import TimeLine from "@/components/timeLine/index.vue";
import ProductModal from "@/views/task/productModal.vue";
import VersionModal from "@/views/task/versionModal.vue";
import commen from "@/views/entry/common.js";
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
import { getRoleUserByDepartment } from "@/http/api/user";
import { getLanguage } from "@/http/api/translate";
import {
  searchCheckInfo,
} from "@/http/api/check";
import { getClassTree } from "@/http/api/entryManage";
import { defineComponent, ref, createVNode } from "vue";
export default {
  components: {
    SearchBox,
    DataBox,
    OperationArea,
    TimeLine,
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
      locale: locale,
      labelCol: { style: { width: "84px" } },
      search: {
        entry: "",
        abbr: "",
        partOfSpeech: "",
        translateType: null,
        classfy2: null,
        entryState: null,
        tag: "",
        entrySource: "",
        language: null,
        translateState: null,
        translate: "",
      },
      tableTitle: "术语列表",
      dataHeight: 400,
      tableHeight: { x: "100%", y: 0 },
      loading: false,
      columns: [
        {
          title: "序号",
          dataIndex: "id",
          align: "center",
          width: 10,
          resizable: true,
          index: 1,
        },
        {
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 160,
          resizable: true,
          index: 2,
        },
        {
          title: "所属类",
          dataIndex: "category",
          align: "center",
          width: 130,
          index: 3,
        },
        {
          title: "Tag",
          dataIndex: "tag",
          align: "center",
          width: 150,
          resizable: true,
          index: 4,
        },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          width: 150,
          fixed: "right",
          index: 100,
        },
      ],
      // dataSource: [],// 表格数据
      dataSource: [
        {
          id: 1,
          entry: "词条",
          category: "Offline",
          tag: "Tag",
          operation: "详情",
        },
        {
          id: 2,
          entry: "词条2",
          category: "Offline",
          tag: "Tag",
          operation: "详情",
        },
        {
          id: 3,
          entry: "词条3",
          category: "Offline",
          tag: "Tag",
          operation: "详情",
        },
        // ... 其他示例数据
      ],
      // 详情数据
      detailColumns: [
        {
          id: 1,
          title: "序号",
          dataIndex: "id",
          align: "center",
        },
        {
          id: 2,

          title: "ts文件",
          dataIndex: "tsFile",
          align: "center",
        },
        {
          id: 3,
          title: "词条",
          dataIndex: "entry",
          align: "center",
        },
        {
          id: 4,
          title: "翻译",
          dataIndex: "translate",
          align: "center",
        },
      ],
      detailDataSource: [
        {
          linkID: 1,
          tsFile: "tsFile1",
          entry: "中文",
          translate: "Chinese",
        },
        {
          linkID: 2,
          tsFile: "tsFile2",
          entry: "英文",
          translate: "English",
        },
        {
          linkID: 2,
          tsFile: "tsFile3",
          entry: "学习",
          translate: "Study",
        },
        {
          linkID: 3,
          tsFile: "tsFile3",
          entry: "学习",
          translate: "Study",
        },
        // ... 其他示例数据
      ],
      detailModalVisible: false, // 详情弹窗
      editableData: {}, // 可编辑数据
      selectedRowKeys: [], // 表格选中项
      selectedRows: [], // 表格选中项
      selectedRowIndex: null, // 表格选中项
      currentTask: {}, // 当前任务
      options: {}, // 任务详情
      timer: null, // 定时器
      departments: [], // 部门
      copyVisible: false, // 复制任务弹窗
      copyNumber: 1, // 复制数量
      translateTypes: [], // 翻译语言
      addProductVisible: false, //   添加产品弹窗
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
        showTotal: total => `共 ${total} 条`,
        onChange: this.pageChange
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
      this.setTableHeight();
      this.searchCheckInfo();
      this.getLanguage();
    },
    // 查看详情
    showDetail(id) {
      this.detailModalVisible = true;
      // 获取详情数据
      this.detailDataSource = this.getDetailData(id);
    },
    // 获取详情数据
    getDetailData(id) {
      // 假设从后端获取数据
      if (id == 1)
        return [
          {
            id: 1,
            tsFile: "ts1.json",
            entry: "词条1",
            translate: "翻译1",
          },
        ];
      if (id == 2)
        return [
          {
            id: 1,
            tsFile: "qqqn",
            entry: "词条1",
            translate: "翻译1",
          },
          {
            id: 2,
            tsFile: "tsxxx",
            entry: "sfse词条1",
            translate: "sefesf翻译1",
          },
          {
            id: 3,
            tsFile: "555",
            entry: "11111词条1",
            translate: "111翻译1",
          },
        ];
      if (id == 3)
        return [
          {
            id: 1,
            tsFile: "ts1.json",
            entry: "词条1",
            translate: "翻译1",
          },
          {
            id: 2,
            tsFile: "ts1esfe.json",
            entry: "sfse词条1",
            translate: "sefesf翻译1",
          },
          {
            id: 3,
            tsFile: "txxxxs1.json",
            entry: "11111词条1",
            translate: "111翻译1",
          },
        ];
    },
    // 获取详情数量
    showDetailNum(id) {
      // this.detailModalVisible = true;
      // 获取详情数据
      return this.getDetailCount(id);
    },
    getDetailCount(id) {
      // 假设从后端获取词条的数量
      if (id === 1) return 1;
      if (id == 2) return 2;
      if (id == 3) return 3;
    },
    // 关闭详情弹框
    handleCancel() {
      this.detailModalVisible = false;
    },
    // 确认详情弹框
    handleOk() {
      this.detailModalVisible = false;
    },
    // 动态设置表格高度
    setTableHeight() {
      this.$nextTick(() => {
        // 设置列表父元素高度
        let box = this.$refs.box.offsetHeight;
        let searchHeight = this.$refs.search.$el.offsetHeight;
        try {
          let operationAreaHeight = this.$refs.operationArea.$el.offsetHeight;
          this.dataHeight = box - searchHeight - operationAreaHeight;
        } catch (error) {
          this.dataHeight = box - searchHeight;
        }

        // 设置表格高度
        let buttonHeight = 0;
        try {
          buttonHeight = this.$refs.button.offsetHeight + 8;
        } catch (error) {}
        this.tableHeight.y = this.dataHeight - buttonHeight - 150;

        // console.log(this.tableHeight.y)
      });
    },
    // 获取翻译语言
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
      });
    },
    // 校验按钮点击事件
    check() {
      this.pageChangeSearch = this.search;
      this.searchCheckInfo();
    },
    // 获取校验信息
    searchCheckInfo() {
      this.searchCheckByCondition(this.search);
    },
    searchCheckByCondition(data) {
      this.loading = true;
      let params = {
        pageIndex: this.pagination.current,
        pageSize: this.pagination.pageSize,
      };
      searchCheckInfo(data, params)
        .then((res) => {
          // this.dataSource = res.data.list
          this.loading = false;
          this.pagination.total = res.data.totalNum;
        })
        .catch((err) => {
          this.loading = false;
        });
    },
    clickInput(event) {
      event.stopPropagation();
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
          this.options[record.id].developers = developer;
        }
        if (data.ENTRY_AUDITOR) {
          let auditor = [];
          data.ENTRY_AUDITOR.forEach((item) => {
            let op = {
              label: item.userName,
              value: item.userName,
            };
            auditor.push(op);
          });
          auditor.push({ label: "无", value: "" });
          this.options[record.id].entryAuditors = auditor;
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
          this.options[record.id].translators = translateor;
        }
        if (data.TRANSLATE_AUDITOR) {
          let translateAuditor = [];
          data.TRANSLATE_AUDITOR.forEach((item) => {
            let op = {
              label: item.userName,
              value: item.userName,
            };
            translateAuditor.push(op);
          });
          translateAuditor.push({ label: "无", value: "" });
          this.options[record.id].translatorAuditors = translateAuditor;
        }
      });
      // console.log(this.options)
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
    },
    addVersionOk(record) {
      this.addVersionVisible = false;
      this.getOptions(record);
    },
    addVersionClose() {
      this.addVersionVisible = false;
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
.tag {
  font-size: 12px;
  padding: 4px 8px;
  background-color: #eefffb;
  border: 1px solid #beede5;
  border-radius: 4px;
  color: #77b3c9;
}

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