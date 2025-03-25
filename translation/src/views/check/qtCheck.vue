<template>
  <div class="box" ref="box">
    <!-- 搜索框组件 -->
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <!-- <a-form-item label="词条" name="entry">
            <a-input v-model:value="search.entry" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item>
          <a-form-item label="所属类" name="category">
            <a-input v-model:value="search.category" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item>
          <a-form-item label="Tag" name="tag">
            <a-input v-model:value="search.tag" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item> -->
          <a-form-item label="删除状态" name="isDelete">
            <a-select v-model:value="search.isDelete" style="width: 186px" placeholder="请选择删除状态" :options='isDeletes' size="small" @click="clickInput"
              allowClear>
            </a-select>
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
                  <!-- <a-button type="primary" size="small"
                    @click="showDetail(record.relationData)">详情({{showDetailNum(record.relationData)}})</a-button> -->
                  <a-button type="primary" ghost size="small" @click.stop="viewRelation(record)">详情({{record.relationCount}})</a-button>
                </template>
              </template>
            </a-table>
          </a-form>
        </div>
      </template>
    </DataBox>
  </div>
  <QtCheckRelation ref="qtCheckRelation" :visible="relationVisible" :dataSource="relationData" @relationClose="relationClose"></QtCheckRelation>
</template>
<script>
import "@/assets/style/common.less";
import { message, Modal } from "ant-design-vue";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import QtCheckRelation from "@/views/check/qtCheckRelation.vue";
import commen from "@/views/entry/common.js";
import { cloneDeep, flatMap } from "lodash-es";
import { getTsProblems, getEntryByTsVo } from "@/http/api/check";
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
    QtCheckRelation,
  },
  data() {
    return {
      labelCol: { style: { width: "84px" } },
      tableTitle: "术语列表",
      dataHeight: 400,
      tableHeight: { x: "100%", y: 0 },
      loading: false,
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
        isDelete: 0,
        importType: "TS",
      },
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
        // {
        //   title: "所属类",
        //   dataIndex: "category",
        //   align: "center",
        //   width: 100,
        //   resizable: true,
        //   index: 3,
        // },
        {
          title: "来源",
          dataIndex: "comment",
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
          title: "翻译",
          dataIndex: "translate",
          align: "center",
          width: 200,
          resizable: true,
          index: 5,
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
      isDeletes: [
        { label: "已删除", value: 1 },
        { label: "待删除", value: 0 },
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
      relationData: [],
      relationVisible: false, // 详情弹窗
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
      getTsProblems(null, this.search)
        .then((res) => {
          // console.log("校验成功！！", res);
          this.dataSource = res.data.list;
          this.pagination.total = res.data.totalNum;
          for (let item of this.dataSource) {
            // console.log("item", item);
            getEntryByTsVo([item]).then((res) => {
              console.log("详情：",res);
              item["relationCount"] = res.data.list[0].entryInfoEntities.length;
              item["reslations"] = res.data.list[0].entryInfoEntities;
              // console.log("item", item);
            });
          }
        })
        .catch((err) => {
          message.info(err.message);
          message.error(err);
        })
        .finally(() => {
          this.loading = false;
        });
    },

    // 查看详情
    viewRelation(record) {
      this.relationData = record.reslations;
      console.log(this.relationData);
      this.relationVisible = true;
    },
    // 关闭详情
    relationClose() {
      this.relationVisible = false;
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