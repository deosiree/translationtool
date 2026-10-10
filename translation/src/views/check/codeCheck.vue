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
            <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="tableHeight" :pagination='pagination'
              :loading="loading" :rowClassName="getRowClassName" ref="codeCheckTable" @resizeColumn="handleResizeColumn">
              <!-- 表格单元格模板 -->
              <template #bodyCell="{ column, record, text }">
                <!-- 模块名称列 -->
                <template v-if="column.dataIndex === 'name'">
                  <span>{{ record.name }}</span>
                </template>
                <!-- dic文件列 -->
                <template v-if="column.dataIndex === 'dicFile'">
                  <span>{{ record.dicFile }}</span>
                </template>
                <!-- 资源文件列 -->
                <template v-if="column.dataIndex === 'sourceFile'">
                  <span>{{ text }}</span>
                </template>
                <!-- 日志列 -->
                <template v-if="column.dataIndex === 'value'">
                  <!-- <span>{{ record.value }}</span> -->
                  <div v-for="(item, index) in record.methods" :key="index" style="display: flex; gap: 100px;">
                    <span class="value">{{ item }}</span>
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
import { message } from "ant-design-vue";
import { clickInput } from "@/utils/domUtils";
import {
  setTableHeight,
  handleResizeColumn,
  getRowClassName,
} from "@/utils/tableUtils";
import { pageChange } from "@/utils/selectionUtils"; // 引入工具函数
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
      tableTitle: "校验日志",
      dataHeight: 400,
      // tableHeight: { x: "100%", y: 0 },
      tableHeight: { x: "max-content", y: 0 },
      loading: false,
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 60,
          index: 0.1,
          customRender: (text, record, index, column) => {
            return (
              text.index +
              1 +
              this.pagination.pageSize * (this.pagination.current - 1)
            );
          },
        },
        {
          title: "文件名",
          dataIndex: "name",
          align: "center",
          width: 100,
          fixed: "right",
        },
        {
          title: "日志",
          dataIndex: "value",
          align: "center",
          width: 400,
          fixed: "right",
        },
      ],
      columns_nom: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 60,
          index: 0.1,
          customRender: (text, record, index, column) => {
            return (
              text.index +
              1 +
              this.pagination.pageSize * (this.pagination.current - 1)
            );
          },
        },
        {
          title: "文件名",
          dataIndex: "name",
          align: "center",
          width: 100,
          fixed: "right",
        },
        {
          title: "日志",
          dataIndex: "value",
          align: "center",
          width: 400,
          fixed: "right",
        },
      ],
      columns_i18n_tr: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 60,
          index: 0.1,
          customRender: (text, record, index, column) => {
            return (
              text.index +
              1 +
              this.pagination.pageSize * (this.pagination.current - 1)
            );
          },
        },
        {
          title: "dic文件",
          dataIndex: "dicFile",
          align: "center",
          width: 100,
        },
        {
          title: "资源文件",
          dataIndex: "sourceFile",
          align: "center",
          width: 100,
        },
        {
          title: "日志",
          dataIndex: "value",
          align: "center",
          width: 400,
          fixed: "right",
        },
      ],
      dataSource: [], // 表格数据
      selectedRowIndex: null, // 表格选中项
      i18nURLs: [
        {
          label: "http://10.17.196.115:18001",
          value: "http://10.17.196.115:18001",
        },
      ],
      moduleNames: [
        // 模块名{ label: "1", value: "0" },
      ],
      questionTypes: [
        // 问题类型{ label: "1", value: "0" },
      ],
      requestId: null, // 存储校验按钮的http请求
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
  },
  unmounted() {
    //注销window.onresize事件
    window.onresize = null;
  },
  methods: {
    // 初始化
    init() {
      this.setTableHeight();
      // this.getOpitons();
    },
    oni18nChange() {
      // 可选：清空搜索表单中的模块名称和问题类型
      this.search.moduleName = null;
      this.search.questionType = null;
      // if (this.search.i18n != null) this.getOpitons();// 不需要，让问题类型下拉框点击了再自动获取，否则重复
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
          console.error("获取模块名失败：", error);
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
          console.error("获取问题类型失败：", error);
        });
    },
    // 校验按钮点击事件
    check() {
      this.dataSource = []; // 清空数据
      this.pageChangeSearch = this.search;
      this.searchCheckInfo();
    },
    // 获取校验信息
    searchCheckInfo() {
      if (this.search.i18n == null) {
        message.error("请选择i18n路径！");
        return;
      }
      if (this.search.questionType == null) {
        message.error("请选择问题类型！");
        return;
      }
      if (this.search.questionType == "i18n_tr")
        this.columns = this.columns_i18n_tr;
      else this.columns = this.columns_nom;
      let params = {
        fonction: this.search.fonction,
        i18n: this.search.i18n,
        questionType: this.search.questionType,
        // requestId: `${Date.now().toString(16)}-${Math.random()
        //   .toString(16)
        //   .substr(2, 10)}`, // 使用 Date.now() 和随机数生成简单的时间戳 UUID
        requestId: `${this.search.questionType}-${Date.now().toString(16)}`,
      };
      let data = { checkURL: this.search.checkURL };
      let lastRequestId = this.requestId; // 获取上一次的请求对象requestId
      // console.log("1.params_data", params, data);

      this.loading = true;

      this.requestId = params.requestId; // 保存当前请求对象的requestId
      // console.log("2.保存请求对象的requestId", this.requestId);
      searchCheckInfo(params, data, lastRequestId)
        .then((res) => {
          if (res.data.code != 205) {
            // message.success(
            //   `校验成功！总共校验了${res.data.data.totalNum}个文件`
            // );
            let tempData = [];
            Object.values(res.data.data.list).forEach((item) => {
              // console.log("item", item);
              item.value.forEach((value) => {
                tempData.push({
                  name: item.file,
                  dicFile: item.dicFile,
                  sourceFile: item.sourceFile,
                  value: value,
                });
              });
            });
            // console.log("校验成功", tempData);
            this.dataSource = tempData;
            this.pagination.current = 1;
            this.pagination.total = this.dataSource.length;
          } else {
            // console.log("校验失败", res.data.message);
            message.error(res.data.message);
          }
        })
        .catch((err) => {
          if (err.name == "AbortError") {
            // console.log(`10.请求已取消!questionType:${this.search.questionType}`);
            message.error("请求已取消");
          } else message.error("请求出错", err.message);
        })
        .finally(() => {
          this.requestId = null; // 清空请求对象
          this.loading = false;
        });
    },
    // 阻止事件冒泡，防止事件传播到父元素
    clickInput(event) {
      clickInput(this, event);
    },
    // 动态设置表格高度
    setTableHeight() {
      setTableHeight(this);
    },
    // 表格列可伸缩
    handleResizeColumn: (w, col) => {
      return handleResizeColumn(w, col);
    },
    // 设置表格每一行的class
    getRowClassName(record, index) {
      return getRowClassName(record, index, this.selectedRowIndex);
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
.value {
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