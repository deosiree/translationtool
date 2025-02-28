<template>
  <div class="box" ref="box">
    <!-- 搜索框组件 -->
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <a-form-item label="模块名称" name="moduleName">
            <a-select v-model:value="search.moduleName" style="width: 186px" placeholder="请选择模块名称" :options='moduleNames' size="small"
              @click="clickInput">
            </a-select>
          </a-form-item>
          <a-form-item label="问题类型" name="questionType">
            <a-select v-model:value="search.questionType" style="width: 186px" placeholder="请选择问题类型" :options='questionTypes' size="small"
              @click="clickInput">
            </a-select>
          </a-form-item>
          <a-form-item label="校验路径" name="checkURL">
            <a-input v-model:value="search.checkURL" style="width: 186px" placeholder="请输入校验目录路径" size="small"></a-input>
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
            <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="{x:'100%' , y: '280px'}"
              :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }" :row-key="record => record.name"
              :pagination='pagination' :loading="loading" :rowClassName="getRowClassName" ref="codeCheckTable" @resizeColumn="handleResizeColumn">
              <!-- 表格单元格模板 -->
              <template #bodyCell="{ column, record }">
                <!-- 模块名称列 -->
                <template v-if="column.dataIndex === 'name'">
                  <span>{{ record.name }}</span>
                </template>
                <!-- 日志列 -->
                <template v-if="column.dataIndex === 'log'">
                  <!-- <span>{{ record.log }}</span> -->
                  <div v-for="(item, index) in record.methods" :key="index" style="display: flex; gap: 100px;">
                    <span class="log">{{ item }}</span>
                  </div>
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
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import OperationArea from "@/components/operationArea/index.vue";
import {
  searchCheckInfo,
  getModuleNames,
  getQuestionTypes,
} from "@/http/api/check";
import { message } from 'ant-design-vue';
export default {
  components: {
    SearchBox,
    DataBox,
    OperationArea,
  },
  data() {
    return {
      labelCol: { style: { width: "84px" } },
      search: {
        // (v2)文件名+函数名;（v1）模块名/文件名+行号/问题类型/详情/日志
        moduleName: null, // 必须
        questionType: null, // 必须
        checkURL: null, // 非必须
        projectName: "",
        col: "",
        details: "",
        logs: "",
      },
      tableTitle: "校验日志",
      dataHeight: 400,
      tableHeight: { x: "100%", y: 0 },
      loading: false,
      columns: [
        {
          title: "文件名",
          dataIndex: "name",
          align: "center",
          width: 100,
          fixed: "right",
        },
        {
          title: "函数名",
          dataIndex: "log",
          align: "center",
          width: 200,
          fixed: "right",
        },
      ],
      dataSource: [], // 表格数据
      editableData: {}, // 可编辑数据
      selectedRowKeys: [], // 表格选中项
      selectedRows: [], // 表格选中项(暂时无用，若有创建删除时，则有用)
      selectedRowIndex: null, // 表格选中项
      currentTask: {}, // 当前任务
      options: {}, // 下拉框的选项列表
      moduleNames: [
        // 模块名{ label: "1", value: "0" },
      ],
      questionTypes: [
        // 问题类型{ label: "1", value: "0" },
      ],
      rules: {
        name: [{ required: true, message: "请输入" }],
        productName: [{ required: true, message: "请选择" }],
        versionName: [{ required: true, message: "请选择" }],
        translateType: [{ required: true, message: "请选择" }],
      },
      pagination: {
        showSizeChanger: true,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
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
    this.getOpitons();
  },
  unmounted() {
    //注销window.onresize事件
    window.onresize = null;
  },
  methods: {
    // 初始化
    init() {
      this.setTableHeight();
      this.getOpitons();
    },
    // 获取下拉框信息
    getOpitons() {
      this.$nextTick(() => {
        this.getModuleNames();
        this.getQuestionTypes();
      });
    },
    // 获取模块名
    getModuleNames() {
      getModuleNames()
        .then((res) => {
          this.moduleNames = res.data.list;
        })
        .catch(({ data }) => {
          console.error("获取模块名失败：", data);
        });
    },
    // 获取问题类型
    getQuestionTypes() {
      getQuestionTypes()
        .then((res) => {
          this.questionTypes = res.data.list;
        })
        .catch(({ data }) => {
          console.error("获取问题类型失败：", data);
        });
    },
    // 校验按钮点击事件
    check() {
      this.pageChangeSearch = this.search;
      this.searchCheckInfo();
    },
    // 获取校验信息
    searchCheckInfo() {
      this.loading = true;
      let params = this.search;
      let path = "code";
      searchCheckInfo(params,path)
        .then((res) => {
          this.dataSource = res.data.list;
          this.pagination.total = res.data.totalNum;
          this.loading = false;
        })
        .catch((err) => {
          message.error("校验目录路径错误！");
          this.loading = false;
        });
    },
    // 阻止事件冒泡，防止事件传播到父元素
    clickInput(event) {
      event.stopPropagation();
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
    // 表格列可伸缩
    handleResizeColumn: (w, col) => {
      col.width = w;
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
    // 表格复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows; // 暂时无用，若有创建删除时，则有用
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
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
.log {
  font-size: 12px;
  padding: 4px 8px;
  background-color: #eefffb;
  border: 1px solid #beede5;
  border-radius: 4px;
  color: #77b3c9;
  margin-bottom: 2px;
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