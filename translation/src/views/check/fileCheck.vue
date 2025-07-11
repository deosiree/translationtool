<template>
  <div class="box" ref="box">
    <!-- 搜索框组件 -->
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <a-form-item label="i18n" name="i18n">
            <a-select v-model:value="search.i18n" style="width: 186px" placeholder="请选择i18n" :options='i18nURLs' size="small" @click="clickInput"
              @change="oni18nChange" allowClear>
            </a-select>
          </a-form-item>
          <!-- <a-form-item label="模块名称" name="moduleName">
            <a-select v-model:value="search.moduleName" style="width: 186px" placeholder="请选择模块名称" :options='moduleNames' size="small"
              @click="getModuleNames" allowClear>
            </a-select>
          </a-form-item> -->
          <a-form-item label="问题类型" name="questionType">
            <a-select v-model:value="search.questionType" style="width: 186px" placeholder="请选择问题类型" :options='questionTypes' size="small"
              @click="getQuestionTypes" allowClear>
            </a-select>
          </a-form-item>
          <a-form-item label="校验路径" name="checkURL">
            <a-input v-model:value="search.checkURL" style="width: 286px" placeholder="校验目录下的所有文件，请输入" size="small"></a-input>
          </a-form-item>
        </a-form>
      </template>
      <!-- 操作按钮模板 -->
      <template v-slot:operate>
        <a-button type="primary" size="middle" class="resetBtn" @click="check">白名单</a-button>
        <a-button type="primary" size="middle" class="checkBtn" @click="check">校验</a-button>
      </template>
    </SearchBox>
    <!-- 数据展示框组件 -->
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <!-- 数据展示模板 -->
      <template v-slot:data>
        <div style="width:100%;position: absolute;">
          <a-form ref="tableFormRef" :model="dataSource" :label-col="{ style: { width: '10px' } }" :wrapper-col="{ span: 0 }">
            <!-- 表格组件 -->
            <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="tableHeight"
              :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }" :row-key="record => record.id" :pagination='pagination'
              :loading="loading" :rowClassName="getRowClassName" ref="qtCheckTable" @resizeColumn="handleResizeColumn">
              <!-- 表格单元格模板 -->
              <template #bodyCell="{ column, record }">
                <!-- 模块名称列 -->
                <template v-if="column.dataIndex === 'name'">
                  <span>{{ record.name }}</span>
                </template>
                <!-- 日志列 -->
                <template v-if="column.dataIndex === 'log'">
                  <span>{{ record.log }}</span>
                </template>
              </template>
            </a-table>
          </a-form>
        </div>
      </template>
    </DataBox>
  </div>
</template>
<script>
import { message, Modal } from "ant-design-vue";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import { getClassTree } from "@/http/api/entryManage";
import {
  mockSearchCheckInfo,
  getModuleNames,
  getQuestionTypes,
} from "@/http/api/check";
import {
  clickInput,
  setTableHeight,
  handleResizeColumn,
  getRowClassName,
  pageChange,
} from "@/utils/commonUtils"; // 引入工具函数
export default {
  components: {
    SearchBox,
    DataBox,
  },
  data() {
    return {
      search: {
        // (v2)文件名+函数名;（v1）模块名/文件名+行号/问题类型/详情/日志
        fonction: "code",
        i18n: null, // 必须
        moduleName: null, // 必须
        questionType: null, // 必须
        checkURL: null, // 非必须
        projectName: "",
        col: "",
        details: "",
        logs: "",
      },
      columns: [
        // 模块名/文件名+词条/问题类型/详情/日志
        // { title: '任务状态', dataIndex: 'state', align: 'center', width: 100, fixed: 'right' },
        {
          title: "模块名/文件名",
          dataIndex: "name",
          align: "center",
          width: 150,
          // fixed: "right",
          resizable: true,
          index: 1,
        },
        {
          title: "词条/问题类型/详情/日志",
          dataIndex: "value",
          align: "center",
          width: 400,
          // fixed: "right",
          resizable: true,
          index: 2,
        },
      ],
      dataSource: [], // 表格数据
      i18nURLs: [
        {
          label: "http://10.17.14.115:18001",
          value: "http://10.17.14.115:18001",
        },
      ],
      moduleNames: [],
      questionTypes: [],
      translateTypes: [], // 翻译语言
      labelCol: { style: { width: "84px" } },
      tableTitle: "校验日志",
      dataHeight: 400,
      // tableHeight: { x: "100%", y: 0 },
      tableHeight: { x: "max-content", y: 0 },
      loading: false,
      selectedRowKeys: [], // 表格选中项
      selectedRows: [], // 表格选中项
      selectedRowIndex: null, // 表格选中项
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
    // this.getOpitons();
    this.searchCheckInfo(); // mock
  },
  unmounted() {
    //注销window.onresize事件
    window.onresize = null;
  },
  methods: {
    // 初始化
    init() {
      this.setTableHeight();
      // this.searchCheckInfo();
      // this.getOpitons();
    },
    // 获取下拉框信息
    getOpitons() {
      this.getModuleNames();
      this.getQuestionTypes();
    },
    // 获取模块名
    getModuleNames() {
      if (this.search.i18n == null) {
        // 不选就是默认全搜索
        message.error("请选择校验目录路径！");
        return;
      }
      getModuleNames({ url: this.search.i18n })
        .then((res) => {
          this.moduleNames = [];
          res.data.list.forEach((element) => {
            this.moduleNames.push({
              label: element,
              value: element,
            });
          });
          // console.log("getModuleNames", this.moduleNames);
        })
        .catch(({ error }) => {
          message.error("获取模块名失败：", error);
        });
    },
    // 获取问题类型
    getQuestionTypes() {
      if (this.search.i18n == null) {
        // 不选就是默认全搜索
        message.error("请选择i18n路径！");
        return;
      }
      getQuestionTypes({ url: this.search.i18n })
        .then((res) => {
          this.questionTypes = [];
          res.data.list.forEach((element) => {
            this.questionTypes.push({
              label: element,
              value: element,
            });
          });
          // console.log("getQuestionTypes", this.questionTypes);
        })
        .catch(({ error }) => {
          message.error("获取问题类型失败：", error);
        });
    },
    oni18nChange() {
      // 可选：清空搜索表单中的模块名称和问题类型
      this.search.moduleName = null;
      this.search.questionType = null;
      // if (this.search.i18n != null) this.getOpitons();// 不需要，让问题类型下拉框点击了再自动获取，否则重复
    },
    // 校验按钮点击事件
    check() {
      this.dataSource = []; // 清空数据
      this.pageChangeSearch = this.search;
      this.searchCheckInfo();
    },
    // 获取校验信息
    searchCheckInfo() {
      // if (this.search.i18n == null) {
      //   message.error("请选择i18n路径！");
      //   return;
      // }
      // if (this.search.questionType == null) {
      //   message.error("请选择问题类型！");
      //   return;
      // }
      this.loading = true;
      let params = {
        moduleName: this.search.moduleName,
        questionType: this.search.questionType,
        checkURL: this.search.checkURL,
      };
      let path = "file";
      mockSearchCheckInfo(params, path)
        .then((res) => {
          let tempData = [];
          Object.values(res.data.list).forEach((item) => {
            // console.log("item", item);
            const file = item.name;
            item.methods.forEach((method) => {
              // console.log(method);
              tempData.push({
                name: file,
                value: method,
              });
            });
          });
          // console.log("校验成功", tempData);
          this.dataSource = tempData;
          this.pagination.total = this.dataSource.totalNum;
        })
        .catch((err) => {
          message.error("校验目录路径错误！",err.message);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    // 表格复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
    },
    // 阻止事件冒泡，防止事件传播到父元素
    clickInput(event) {
      clickInput(this, event);
    },
    // 动态设置表格高度
    setTableHeight() {
      setTableHeight(this); // 调用工具函数
    },
    // 表格列可伸缩
    handleResizeColumn(w, col) {
      return handleResizeColumn(w, col); // 调用工具函数
    },
    // 设置表格每一行的 class
    getRowClassName(record, index) {
      return getRowClassName(record, index, this.selectedRowIndex); // 调用工具函数
    },
    // 分页切换
    pageChange(page, pageSize) {
      // 传入 searchCheckInfo 作为查询接口的回调函数
      pageChange(this, page, pageSize, this.searchCheckInfo);
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