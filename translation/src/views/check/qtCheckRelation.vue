<template>
  <!-- 详情弹框(?修改a-modal的大小，参考之前工作台的) -->
  <a-modal :visible="visible" title="同词条，不同 翻译/词条来源" @cancel="handleClose" @ok="handleOK" style="width:70%">
    <div class="data">
      <div>
        词条：
        <span class="value">{{dataSource.entry}}</span>
      </div>
      <div>
        tag：
        <a-tag color="cyan" class="tag-content tag-offset">
          <span>{{dataSource.tag}}</span>
        </a-tag>
      </div>
      <div>
        翻译语言：
        <span class="value">{{dataSource.tsProblemsType}}</span>
      </div>
    </div>
    <div class="table">
      <a-form ref="tableRelationFormRef" :model="dataSource.list" :wrapper-col="{ span: 0 }">
        <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource.list" :scroll="{x:'60vw' , y: '50vh'}"
          :row-key="record => record.id" :pagination='pagination' :loading="loading" :rowClassName="getRowClassName" ref="taskTable"
          @resizeColumn="handleResizeColumn">
          <!-- 表格单元格模板 -->
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'translate'">
              <span>{{ record[this.translate] }}</span>
            </template>
            <template v-if="column.dataIndex === 'translateState'">
              <template v-if="record[this.translateState] === '0'">
                <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">未翻译</span>
              </template>
              <template v-if="record[this.translateState] === '1'">
                <a-badge color="#FBB31F" /><span style="color:#FBB31F">待审核</span>
              </template>
              <template v-if="record[this.translateState] === '2'">
                <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
              </template>
              <template v-if="record[this.translateState] === '3'">
                <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
              </template>
            </template>
            <template v-if="['publicState', 'entryState'].includes(column.dataIndex)">
              <template v-if="[0, '0'].includes(record[this.publicState])">
                <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
              </template>
              <template v-if="record[this.publicState] === 1">
                <a-badge color="#FBB31F" /><span style="color:#FBB31F">审核中</span>
              </template>
              <template v-if="record[this.publicState] === 2">
                <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
              </template>
              <template v-if="record[this.publicState] === 3">
                <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
              </template>
            </template>
            <template v-if="column.dataIndex === 'operation'">
              <a-button type="primary" danger size="small" @click.stop="handleDelete(record)">删除</a-button>
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
import { deleteEntryInfo } from "@/http/api/entryManage";
import languageParam from "@/utils/languageParam.js";
export default {
  emits: ["relationClose"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    dataSource: {
      type: Object,
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
          resizable: true,
          fixed: "left",
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
          title: "翻译",
          dataIndex: "translate",
          align: "center",
          fixed: "left",
          width: 100,
          resizable: true,
          index: 1,
        },
        {
          title: "翻译状态",
          dataIndex: "translateState",
          align: "center",
          width: 100,
          resizable: true,
          index: 2,
        },
        // {
        //   title: "公开状态",
        //   dataIndex: "publicState",
        //   align: "center",
        //   width: 100,
        //   resizable: true,
        //   index: 3,
        // },
        {
          title: "词条来源",
          dataIndex: "entrySource",
          align: "center",
          width: 200,
          resizable: true,
          index: 4,
        },
        {
          title: "词条状态",
          dataIndex: "entryState",
          align: "center",
          width: 100,
          resizable: true,
          index: 5,
        },
        {
          title: "修改者",
          dataIndex: "update",
          align: "center",
          width: 100,
          resizable: true,
          index: 6,
        },
        {
          title: "修改时间",
          dataIndex: "updateTime",
          align: "center",
          width: 200,
          resizable: true,
          index: 7,
        },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          fixed: "right",
          width: 100,
          resizable: true,
          index: 8,
        },
      ],
      translate: "",
      translateState: "",
      publicState: "",
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
        // console.log("新数据", newDataSource);
        if (newDataSource.tsProblemsType) {
          languageParam.languageList.forEach((item) => {
            if (newDataSource.tsProblemsType === item.name) {
              this.translate = item.value;
              this.translateState = item.state;
              this.publicState = item.publicState;
            }
          });
        }
      },
    },
  },
  methods: {
    handleClose() {
      this.pagination.current = 1;
      this.$emit("relationClose");
    },
    handleOK() {
      this.pagination.current = 1;
      this.$emit("relationClose");
    },
    // 针对单一词条的删除按钮
    handleDelete(record) {
      this.loading = true;
      deleteEntryInfo([record.id],{tableName:"t_entry_info"})
        .then((res) => {
          if (res.code === 200) {
            message.success("删除成功");
            this.dataSource.list = this.dataSource.list.filter(
              (item) => item.id !== record.id
            );
            this.pagination.current = 1;
            this.pagination.total--;
          } else {
            message.error(res.message);
          }
        })
        .finally(() => {
          this.loading = false;
        });
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
.data {
  // padding: 12px 12px 36px 12px;
  // border: 1px solid #ccc;
  padding: 0 36px 12px 36px;
  margin-bottom: 12px;
  font-size: 1.2em; // 字体设置大一号
  display: flex;
  justify-content: space-between;
}
.tag-offset {
  position: relative;
  top: 5px;
}
.value {
  font-size: 0.9em;
  color: #999; // 颜色设为淡灰色
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