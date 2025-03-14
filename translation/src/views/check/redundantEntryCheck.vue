<template>
  <div class="box" ref="box">
    <!-- 搜索框组件 -->
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <a-form-item label="词条" name="entry">
            <a-input v-model:value="search.entry" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item>
          <a-form-item label="词条状态" name="state">
            <a-select v-model:value="search.entryState" style="width: 186px" placeholder="请选择" size="small" :options='entryStates' @click="clickInput"
              allowClear></a-select>
          </a-form-item>
          <a-form-item label="Tag" name="tag">
            <a-input v-model:value="search.tag" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item>
          <a-form-item label="二级分类" name="classfy2">
            <a-select v-model:value="search.classfy2" style="width: 186px" placeholder="请选择" size="small" :fieldNames="{label:'name',value:'name'}"
              :options='classify2Option' @click="clickInput" allowClear></a-select>
          </a-form-item>
          <a-form-item label="词条来源" name="entrySource">
            <a-input v-model:value="search.entrySource" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item>
          <a-form-item label="翻译语言" name="translateType">
            <a-select v-model:value="search.translateType" style="width: 186px" placeholder="请选择" size="small"
              :fieldNames="{label:'name',value:'name'}" :options='translateTypes' @click="clickInput" allowClear></a-select>
          </a-form-item>
          <a-form-item label="翻译状态" name="translateState">
            <a-select v-model:value="search.translateState" style="width: 186px" placeholder="请选择" :options='translateStates' size="small"
              @click="clickInput" allowClear></a-select>
          </a-form-item>
          <a-form-item label="翻译结果" name="translate">
            <a-input v-model:value="search.translate" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
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
      <template v-slot:label>
        产品版本： <a-select v-model:value="currentVersion" allowClear style="width: 150px" placeholder="请选择版本" :options='productVersions'
          :fieldNames="{label:'name',value:'id'}" size="small" @click="clickInput">
        </a-select>
      </template>
      <template v-slot:operate>
        <div ref="button" v-if="true" style="margin-bottom:8px;display:flex;gap:10px">
          <!-- <a-button type="primary" danger size="small" @click="selectDelEntry" v-if="!selectDelEntryFlag">删除</a-button> -->
          <!-- <a-button type="primary" size="small" @click="selectAllEntry" v-if="!selectAllEntryFlag">全选</a-button> -->
        </div>
      </template>
      <!-- 数据展示模板 -->
      <template v-slot:data>
        <div style="width:100%;position: absolute;">
          <a-form ref="tableFormRef" :model="dataSource" :label-col="{ style: { width: '10px' } }" :wrapper-col="{ span: 0 }">
            <!-- 表格组件 -->
            <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="tableHeight"
              :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }" :row-key="record => record.id" :pagination='pagination'
              :loading="loading" :rowClassName="getRowClassName" ref="redundantEntryCheckTable" @resizeColumn="handleResizeColumn">
              <!-- 表格单元格模板 -->
              <template #bodyCell="{ column, record }">
                <!-- 词条状态列 -->
                <template v-if="column.dataIndex === 'entryState'">
                  <stateBadge :entry-state="record.entryState" />
                </template>
                <!-- 词条列 -->
                <template v-if="column.dataIndex === 'entry'">
                  <span>{{ record.entry }}</span>
                </template>
                <!-- 词条版本列 -->
                <template v-if="column.dataIndex === 'entryVersion'">
                  <span>{{ record.entryVersion }}</span>
                </template>
                <!-- 词性来源列 -->
                <template v-if="column.dataIndex === 'entrySource'">
                  <span>{{ record.entrySource }}</span>
                </template>
                <!--Tag列 -->
                <template v-if="column.dataIndex === 'tag'">
                  <span>
                    <a-tag color="cyan" class="tag-content">
                      <span>{{ record.tag }}</span>
                    </a-tag>
                  </span>
                </template>
                <!--翻译列 -->
                <template v-if="column.dataIndex === 'english'">
                  <span>{{ record.english }}</span>
                </template>
                <!-- 翻译状态列 -->
                <template v-if="column.dataIndex === 'translateState'">
                  <stateBadge :entry-state="record.translateState" />
                </template>
                <!-- 辞典名称列 -->
                <template v-if="column.dataIndex === 'dicName'">
                  <span>{{ record.dicName }}</span>
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
import "@/assets/style/common.less";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import stateBadge from "@/components/stateBadge/index.vue";
import { getLanguage } from "@/http/api/translate";
import { getSecondClassify } from "@/http/api/secondClassify";
import { mockSearchCheckInfo } from "@/http/api/check";
import { message, Modal } from "ant-design-vue";
import {
  clickInput,
  setTableHeight,
  handleResizeColumn,
  getRowClassName,
  pageChange,
} from "@/utils/tableUtils"; // 引入工具函数
export default {
  components: {
    SearchBox,
    DataBox,
    stateBadge,
  },
  data() {
    return {
      search: {
        entry: "", //词条
        entryState: null, //词条状态
        tag: "", //Tag
        classfy2: null, //二级分类
        entrySource: "", //词性来源
        translateType: null, //翻译语言
        translateState: null, //翻译状态
        translate: "", //翻译结果
      },
      columns: [
        {
          title: "词条状态",
          dataIndex: "entryState",
          align: "center",
          width: 130,
          resizable: true,
          index: 0.1,
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
          title: "词条版本",
          dataIndex: "entryVersion",
          align: "center",
          width: 130,
          resizable: true,
          index: 6,
        },
        {
          title: "词条来源",
          dataIndex: "entrySource",
          align: "center",
          width: 150,
          resizable: true,
          index: 1,
        },
        {
          title: "Tag",
          dataIndex: "tag",
          align: "center",
          width: 150,
          resizable: true,
          index: 1,
        },
        {
          title: "翻译",
          dataIndex: "translate",
          align: "center",
          width: 180,
          resizable: true,
          index: 10,
        }, // 只是英文的翻译吗？为何不是对应语言的翻译{{translateType}}
        {
          title: "翻译状态",
          dataIndex: "translateState",
          align: "center",
          width: 180,
          resizable: true,
          index: 16,
        },
        {
          title: "辞典名称",
          dataIndex: "dicName",
          align: "center",
          width: 180,
          resizable: true,
          index: 19,
        },
      ],
      dataSource: [], // 表格数据
      entryStates: [
        { label: "新建", value: "0" },
        { label: "审核中", value: "1" },
        { label: "审核不通过", value: "2" },
        { label: "已审核", value: "3" },
      ], // 词条状态
      classify2Option: [], // 二级分类
      translateTypes: [], // 翻译语言
      translateStates: [
        { label: "未翻译", value: "0" },
        { label: "待审核", value: "1" },
        { label: "审核不通过", value: "2" },
        { label: "已审核", value: "3" },
      ], // 翻译状态
      currentVersion: null, // 当前产品版本
      productVersions: [], // 产品版本
      labelCol: { style: { width: "84px" } },
      tableTitle: "校验日志",
      dataHeight: 400,
      tableHeight: { x: "100%", y: 0 },
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
    console.log("冗余词条校验页面挂载了");
    let _this = this;
    this.$nextTick(() => {
      this.init();
      /** 控制table的高度 */
      window.onresize = function () {
        _this.setTableHeight();
      };
    });
    this.getOpitons();
    this.searchCheckInfo(); // mock
  },
  unmounted() {
    //注销window.onresize事件
    window.onresize = null;
    console.log("冗余词条校验页面卸载了");
  },
  methods: {
    // 初始化
    init() {
      this.setTableHeight();
      // this.getOpitons();
    },
    // 获取下拉框信息
    getOpitons() {
      this.getLanguage();
      this.getSecondClassify();
      this.getProductVersions();
    },
    // 获取翻译语言
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
      });
    },
    // 获取二级分类
    getSecondClassify() {
      let data = {};
      getSecondClassify(data).then((res) => {
        this.classify2Option = res.data.list;
      });
    },
    // 获取产品版本(?为啥是产品版本，要求的入参也对不上)
    getProductVersions() {
      let data = {};
      getSecondClassify(data).then((res) => {
        this.productVersions = res.data.list;
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
      this.loading = true;
      let params = this.search;
      let path = "redundantEntry";
      mockSearchCheckInfo(params, path)
        .then((res) => {
          this.dataSource = res.data.list;
          console.log("冗余信息", this.dataSource);
          this.pagination.total = this.dataSource.length;
        })
        .catch((err) => {
          message.info(err.message);
          message.error(err);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    // 表格复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      console.log("选择事件xxx", selectedRowKeys, selectedRows);
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
      console.log("选择事件", this.selectedRowKeys, this.selectedRows);
    },
    // 表格全选/反选框点击事件
    onSelectAll(selected) {
      // 全部选择（不是当前页全部选择）
      if (selected) {
        this.selectedRows = [...this.dataSource];
        this.selectedRowKeys = this.dataSource.map((item) => item.id);
      } else {
        this.selectedRows = [];
        this.selectedRowKeys = [];
      }
      console.log("全选事件", this.selectedRowKeys, this.selectedRows);
    },
    // 阻止事件冒泡，防止事件传播到父元素
    clickInput(event) {
      clickInput(this, event);
    },
    // 动态设置表格高度
    setTableHeight() {
      setTableHeight(this,24); // 调用工具函数
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