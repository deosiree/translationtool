<template>
  <div class="commonBox">
    <SearchBox ref="search" :operate="false">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <a-form-item label="任务列表" name="task">
            <a-select v-model:value="search.task" style="width: 186px" placeholder="请选择任务" :options='tasks' size="small" @click="clickInput"
              :fieldNames="{label:'productName',value:'productName'}">
            </a-select>
          </a-form-item>
        </a-form>
      </template>
    </SearchBox>
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <template v-slot:data>
        <div style="width:100%;position: absolute;">
          <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource"
            :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" :row-key="record => record.id" :scroll="tableHeight"
            :pagination='false' :loading="loading" :rowClassName="getRowClassName" ref="taskTable" @resizeColumn="handleResizeColumn"
            :customRow="customRow">
            <template #bodyCell="{ column, text, record }">
              <template v-if="['translate', 'unique', 'remark'].includes(column.dataIndex)">
                <div>
                  <template v-if="editableData[record.id]">
                    <a-input v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" />
                  </template>
                  <template v-else>
                    {{ text }}
                  </template>
                </div>
              </template>
            </template>
          </a-table>
        </div>
      </template>
    </DataBox>
  </div>
</template>
<script>
import { cloneDeep } from "lodash-es";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import {
  getPublicEntry,
  getCheckNewEntryByClassfy,
  updatePublicEntry,
  deletePublicEntry,
} from "@/http/api/entryManage";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
export default {
  components: {
    SearchBox,
    DataBox,
  },
  emits: [],
  props: {
    boxHeight: 0,
    currentCommon: {},
  },
  data() {
    return {
      user: {},
      admin: false,
      box: 0,
      common: {},
      search: {
        task: null, // 任务名称
      },
      labelCol: { style: { width: "84px" } },
      tableTitle: "任务列表",
      dataHeight: 200,
      tableHeight: { x: "100%", y: 0 },
      loading: false,
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 50,
          customRender: (text, record, index, column) => {
            return text.index + 1;
          },
          fixed: "left",
        },
        {
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 150,
          fixed: "left",
          resizable: true,
        },
        {
          title: "翻译",
          dataIndex: "translate",
          align: "center",
          width: 150,
          resizable: true,
        },
        {
          title: "翻译语言",
          dataIndex: "type",
          align: "center",
          width: 180,
          resizable: true,
        },
        {
          title: "唯一属性",
          dataIndex: "unique",
          align: "center",
          width: 180,
          resizable: true,
        },
        {
          title: "备注",
          dataIndex: "remark",
          align: "center",
          width: 150,
          resizable: true,
        },
      ],
      dataSource: [],
      tasks: [], // 任务列表
      editableData: {},
      selectedRowIndex: null,
      translateTypes: [],
      selectedRowKeys: [],
    };
  },

  created() {},
  mounted() {
    this.box = this.boxHeight;
    this.common = this.currentCommon;

    this.user = this.$store.state.user;
    this.admin = this.$store.state.admin;
    // console.log(this.admin)
    this.getCheckNewEntryByClassfy();
    this.setTableHeight();
    this.init();
  },
  watch: {
    boxHeight(newval, oldval) {
      this.box = newval;
      this.setTableHeight();
    },
    currentCommon(newval, oldval) {
      this.common = newval;
      this.init();
    },
  },
  methods: {
    init() {
      // console.log(this.common)
      this.getCommonEntry();
    },
    getCheckNewEntryByClassfy() {
      getCheckNewEntryByClassfy()
        .then((res) => {
          this.tasks = res.data.list;
        })
        .catch(({ data }) => {
          console.error("获取新词条失败：", data);
        });
    },
    // 阻止事件冒泡，防止事件传播到父元素
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
        let searchHeight = this.$refs.search.$el.offsetHeight;
        this.dataHeight = this.box - searchHeight - 32;

        // 设置表格高度
        let buttonHeight = 0;
        try {
          buttonHeight = this.$refs.button.offsetHeight + 8;
        } catch (error) {}
        this.tableHeight.y = this.dataHeight - buttonHeight - 110;
      });
    },
    // 查询公共库数据
    getCommonEntry() {
      let params = {
        pageIndex: -1,
        pageSize: -1,
      };
      let data = {
        visualRange: this.common.department,
        entry: this.search.entry,
        translate: this.search.translate,
        type: this.search.type,
      };
      getPublicEntry(data, params).then((res) => {
        this.dataSource = res.data.list;
      });
    },
    // 添加表格行点击事件
    customRow(record, index) {
      return {
        onClick: (event) => {
          // this.selectedRowIndex = record.id
        },
        onDblclick: (event) => {
          if (this.admin) {
            // 管理员 可修改
            this.editableData[record.id] = cloneDeep(
              this.dataSource.filter((item) => record.id === item.id)[0]
            );
            if (
              this.columns.findIndex(
                (item) => item.dataIndex === "operation"
              ) === -1
            ) {
              let operation = {
                title: "操作",
                dataIndex: "operation",
                align: "center",
                width: 80,
              };
              this.columns.push(operation);
            }
          }
        },
      };
    },
    // 表格列可伸缩
    handleResizeColumn: (w, col) => {
      col.width = w;
    },
    // 表格复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
    },
    reset() {
      this.search = {
        entry: "",
        translate: "",
        type: null,
      };
      this.getCommonEntry();
    },
  },
};
</script>
<style scoped lang="less">
.commonBox {
  width: 100%;
  height: 100%;
  // border: 1px solid red;
}
</style>