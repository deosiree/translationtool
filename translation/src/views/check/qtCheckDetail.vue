<template>
  <!-- 详情弹框(?修改a-modal的大小，参考之前工作台的) -->
  <a-modal :visible="detailModalVisible" title="关联信息" @cancel="handleCancel" @ok="handleOk">
    <a-form ref="tableDetailFormRef" :model="dataSource" :wrapper-col="{ span: 0 }">
      <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource"
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
import { cloneDeep, flatMap } from "lodash-es";
import { defineComponent, ref, createVNode } from "vue";
export default {
  data() {
    return {
      locale: locale,
      labelCol: { style: { width: "84px" } },
      loading: false,
      columns: [
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
      // dataSource: [],// 表格数据
      dataSource: [
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
    // 查看详情
    showDetail(res) {
      this.detailModalVisible = true;
      // 获取详情数据
      this.dataSource = res.data.list;
    },
    // 获取详情数量
    showDetailNum(res) {
      // this.detailModalVisible = true;
      // 获取详情数据
      if (res) return res.data.totalNum;
      return 0;
    },
    // 关闭详情弹框
    handleCancel() {
      this.detailModalVisible = false;
    },
    // 确认详情弹框
    handleOk() {
      this.detailModalVisible = false;
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
    // 表格复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
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