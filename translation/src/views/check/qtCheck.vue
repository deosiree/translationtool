<template>
  <div class="box" ref="box">
    <!-- 搜索框组件 -->
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <a-form-item label="词条" name="entry">
            <a-input v-model:value="search.entry" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item>
          <a-form-item label="所属类" name="category">
            <a-input v-model:value="search.category" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item>
          <a-form-item label="Tag" name="tag">
            <a-input v-model:value="search.tag" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
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
          <a-form ref="tableFormRef" :model="dataSource" :label-col="{ style: { width: '10px' } }" :wrapper-col="{ span: 0 }">
            <!-- 表格组件 -->
            <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="tableHeight"
              :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }" :row-key="record => record.id" :pagination='pagination'
              :loading="loading" :rowClassName="getRowClassName" ref="qtCheckTable" @resizeColumn="handleResizeColumn">
              <!-- 表格单元格模板 -->
              <template #bodyCell="{ column, text, record }">
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
                  <span>
                    <a-tag color="cyan" class="tag-content">
                      <span>{{ text }}</span>
                    </a-tag>
                  </span>
                </template>
                <!-- 操作列 -->
                <template v-if="column.dataIndex === 'operation'">
                  <a-button type="primary" size="small"
                    @click="showDetail(record.detailDataSource)">详情({{showDetailNum(record.detailDataSource)}})</a-button>
                </template>
              </template>
            </a-table>
          </a-form>
        </div>
      </template>
    </DataBox>
  </div>
  <QTCheckDetail ref="qtCheckDetail" :visible="detailModalVisible" :dataSource="detailDataSource" @detailClose="detailClose"></QTCheckDetail>
</template>
<script>
import "@/assets/style/common.less";
import { message, Modal } from "ant-design-vue";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import QTCheckDetail from "@/views/check/qtCheckDetail.vue";
import commen from "@/views/entry/common.js";
import { cloneDeep, flatMap } from "lodash-es";
import { mockSearchCheckInfo } from "@/http/api/check";
import { defineComponent, ref, createVNode } from "vue";
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
    QTCheckDetail,
  },
  data() {
    return {
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
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 300,
          resizable: true,
          index: 2,
        },
        {
          title: "所属类",
          dataIndex: "category",
          align: "center",
          width: 100,
          resizable: true,
          index: 3,
        },
        {
          title: "Tag",
          dataIndex: "tag",
          align: "center",
          width: 200,
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
          entry: "词条",
          category: "Offline",
          tag: "Tag1111111111111111111111111111111111111111111111111111",
          operation: "详情",
        },
        {
          entry: "词条2",
          category: "Offline",
          tag: "Tag22222222222222222222222222222222222222222222222222222222222222",
          operation: "详情",
        },
        {
          entry: "词条3",
          category: "Offline",
          tag: "Tagvsfvsefsefsfsefesfsefiefieshfiwefwnfiwnfiwnfownefowenfowenf",
          operation: "详情",
        },
        {
          entry: "词条",
          category: "Offline",
          tag: "Tageeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
          operation: "详情",
        },
        {
          entry: "词条2",
          category: "Offline",
          tag: "Tagfffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
          operation: "详情",
        },
        {
          entry: "词条3",
          category: "Offline",
          tag: "Tag4444444444444444444444444444444444444444444444444444444444444444444",
          operation: "详情",
        },
        {
          entry: "词条",
          category: "Offline",
          tag: "Tagbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
          operation: "详情",
        },
        {
          entry: "词条2",
          category: "Offline",
          tag: "Tagkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk",
          operation: "详情",
        },
        {
          entry: "词条3",
          category: "Offline",
          tag: "Tag99999999999999999999999999999999999999999999999999999999999999999999999999999999",
          operation: "详情",
        },
        {
          entry: "词条",
          category: "Offline",
          tag: "Tag0000000000000000000000000000000000000000000000000000000000000000000000000",
          operation: "详情",
        },
        // ... 其他示例数据
      ],
      detailDataSource: [],
      detailModalVisible: false, // 详情弹窗
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
      let path = "qt";
      mockSearchCheckInfo(params, path)
        .then((res) => {
          this.dataSource = res.data.list;
          this.pagination.total = res.data.totalNum;
          this.loading = false;
        })
        .catch((err) => {
          message.info(err.message);
          message.error(err);
          this.loading = false;
        });
    },

    // 查看详情
    showDetail(res) {
      this.detailModalVisible = true;
      // 获取详情数据
      // this.detailDataSource = this.getDetailData(id);
      this.detailDataSource = res.data.list;
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
    showDetailNum(res) {
      // this.detailModalVisible = true;
      // 获取详情数据
      // return this.getDetailCount(id);
      if (res) return res.data.totalNum;
      return 0;
    },
    getDetailCount(id) {
      // 假设从后端获取词条的数量
      if (id === 1) return 1;
      if (id == 2) return 2;
      if (id == 3) return 3;
    },
    // 关闭详情弹框
    detailClose() {
      // console.log("详情数据",this.detailDataSource);
      this.detailModalVisible = false; // 关闭弹窗，具体点击确认/取消的操作都在组件中写，这里只是传递关闭弹窗的信息
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