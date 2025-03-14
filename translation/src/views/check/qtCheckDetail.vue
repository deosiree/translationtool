<template>
  <!-- 详情弹框(?修改a-modal的大小，参考之前工作台的) -->
  <a-modal :visible="visible" title="关联信息2" @cancel="handleClose" @ok="handleOK" style="width:70%">
    <div class="table">
      <a-form ref="tableDetailFormRef" :model="dataSource" :wrapper-col="{ span: 0 }">
        <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="tableHeight"
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
    </div>

  </a-modal>
</template>
<script>
import { message, Modal } from "ant-design-vue";
import locale from "ant-design-vue/es/date-picker/locale/zh_CN";
import { cloneDeep, flatMap } from "lodash-es";
import { defineComponent, ref, createVNode } from "vue";
export default {
  emits: ["detailClose"],
  props: {
    // 传递来的数据放这儿，不能再在data中定义了
    visible: {
      type: Boolean,
      default: false,
    },
    dataSource: {
      type: Array,
    },
  },
  data() {
    return {
      locale: locale,
      labelCol: { style: { width: "84px" } },
      loading: false,
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 50,
          index:0.1,
          customRender: (text, record, index, column) => {
            return text.index + 1 + this.pagination.pageSize*(this.pagination.current-1);
          },
        },
        {
          id: 2,
          title: "ts文件",
          dataIndex: "tsFile",
          align: "center",
          width: 100,

        },
        {
          id: 3,
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 300,

        },
        {
          id: 4,
          title: "翻译",
          dataIndex: "translate",
          align: "center",
          width: 300,
        },
      ],
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
  watch: {
    dataSource: {
      immediate: true, // 在组件初始化时就会立即执行一次 handler 函数，确保在初始数据加载时也能设置默认全选。
      handler(newDataSource) {
        // 设置默认全选
        this.selectedRowKeys = newDataSource.map(
          (record) => record.id
        );
        this.selectedRows = newDataSource.map((record) => record);
      },
    },
  },
  methods: {
    handleClose() {
      this.$emit("detailClose");
    },
    handleOK() {
      // // 点击确认就发送http请求，更新到对应产品中（/workbench/insertEntry/{taskID} 新增词条
      // let params = {};
      // let data = {
      //   taskID: this.task.id,
      // };
      // insertEntry(params, data).then((res) => {
      //   console.log("insertEntry", res);
      //   message.success("更新成功！");
      //   this.$emit("classifyClose");
      //   this.dataSource = [];
      // });
      this.$emit("detailClose");
    },
    // 表格复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
    },
    // 表格复选框点击事件
    onSelect(record, selected) {
      // record是被点击的行数据，selected是是否被选中
      if (selected) {
        this.selectedRows.push(record);
      } else {
        this.selectedRows = this.selectedRows.filter((item) => {
          return item.entrySource !== record.entrySource;
        });
      }
    },
    // 表格全选/反选框点击事件
    onSelectAll(selected, changeRows) {
      if (selected) {
        this.selectedRows = this.selectedRows.concat(changeRows);
      } else {
        changeRows.forEach((item) => {
          this.selectedRows = this.selectedRows.filter((entry) => {
            return entry !== item;
          });
        });
      }
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
  width: 700px;
  height: 500px;
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