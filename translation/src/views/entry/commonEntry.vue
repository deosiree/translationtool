<template>
  <div class="commonBox">
    <SearchBox ref="search" :operate="false">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <a-form-item label="词条" name="entry">
            <a-input v-model:value="search.entry" placeholder="请输入内容"></a-input>
          </a-form-item>

          <a-form-item label="翻译" name="translate">
            <a-input v-model:value="search.translate" placeholder="请输入内容"></a-input>
          </a-form-item>
          <a-form-item label="翻译语言" name="type">
            <a-select v-model:value="search.type" style="width: 186px" placeholder="请选择内容" :options='translateTypes'
              :fieldNames="{label:'name',value:'name'}" allowClear>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" size="middle" class="resetBtn" @click="reset">重置</a-button>
            <a-button type="primary" size="middle" style="margin-left:8px" @click="getCommonEntry()">查询</a-button>
          </a-form-item>
        </a-form>
      </template>
    </SearchBox>
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <template v-slot:operate>
        <div ref="button" v-if="admin" style="margin-bottom:8px;display:flex;gap:8px">
          <a-button type="primary" size="small" @click="deletePublicEntry"><template #icon>
              <DeleteOutlined />
            </template>删除</a-button>
          <a-button type="primary" size="small" @click="batchSave"><template #icon>
              <SaveOutlined />
            </template>保存</a-button>
          <!-- <a-button type="primary" size="small"><template #icon><SettingOutlined /></template>列设置</a-button> -->
        </div>
      </template>
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
              <!-- <template v-else-if="column.dataIndex === 'type'">
                                <div>
                                    <template v-if="editableData[record.id]">
                                        <a-select
                                        v-model:value="editableData[record.id][column.dataIndex]"
                                        style="width: 100%"
                                        placeholder="请选择内容"
                                        :options='translateTypes'
                                        :fieldNames="{label:'name',value:'name'}"
                                        >
                                        </a-select>
                                    </template>
                                    <template v-else>
                                        {{ text }}
                                    </template>
                                </div>
                            </template> -->
              <template v-else-if="column.dataIndex === 'operation'">
                <div class="editable-row-operations">
                  <span v-if="editableData[record.id]">
                    <a-button type="primary" ghost size="small" @click.stop="save(record)">保存</a-button>
                    <a-button type="primary" ghost size="small" danger @click.stop="cancel(record)">取消</a-button>
                    <!-- <a-tooltip placement="top">
                                            <template #title>
                                            <span>保存</span>
                                            </template>
                                            <CheckOutlined style="color:#369FFF;margin-left:8px" @click="save(record)"/>
                                        </a-tooltip>
                                        <a-tooltip placement="top">
                                            <template #title>
                                            <span>取消</span>
                                            </template>
                                            <CloseOutlined style="color:red;margin-left:8px" @click="cancel(record)"/>
                                        </a-tooltip> -->
                  </span>
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
  DeleteOutlined,
  SaveOutlined,
  SettingOutlined,
  CheckOutlined,
  CloseOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons-vue";
import {
  getPublicEntry,
  updatePublicEntry,
  deletePublicEntry,
} from "@/http/api/entryManage";
import { getLanguage } from "@/http/api/translate";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import { setTableHeight } from "@/utils/commonUtils";
export default {
  components: {
    SearchBox,
    DataBox,
    DeleteOutlined,
    SaveOutlined,
    SettingOutlined,
    CheckOutlined,
    CloseOutlined,
    ExclamationCircleOutlined,
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
        entry: "",
        translate: "",
        type: null,
      },
      labelCol: { style: { width: "84px" } },
      tableTitle: "公共词条列表",
      dataHeight: 200,
      // tableHeight: { x:'100%',y: 0 },
      tableHeight: { x: "max-content", y: 0 },
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
        // {title: '操作',dataIndex: 'operation',align:'center',width:50},
      ],
      dataSource: [],
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
      this.getLanguage();
      this.getCommonEntry();
    },
    // 获取翻译语言
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
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
    // 动态设置表格高度
    setTableHeight() {
      this.$nextTick(() => {
        setTableHeight(this, 8, 126, 32, { ok: true, h: this.box });
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
    // 保存
    save(record) {
      updatePublicEntry(this.editableData[record.id]).then((res) => {
        message.success("编辑成功！");
        this.getCommonEntry();
        delete this.editableData[record.id];

        if (JSON.stringify(this.editableData) === "{}") {
          this.columns.some((item, i) => {
            if (item.dataIndex === "operation") {
              this.columns.splice(i, 1);
              return true;
            }
          });
        }
      });
    },
    batchSave() {
      if (JSON.stringify(this.editableData) === "{}") {
        return;
      }
      Modal.confirm({
        title: "是否全部保存?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "确定",
        cancelText: "取消",
        style: { top: "30%" },
        onOk: () => {
          for (let key in this.editableData) {
            updatePublicEntry(this.editableData[key]).then((res) => {
              delete this.editableData[key];
            });
          }
          message.success("保存成功！");
        },
      });
      this.columns.some((item, i) => {
        if (item.dataIndex === "operation") {
          this.columns.splice(i, 1);
          return true;
        }
      });
    },
    // 取消
    cancel(record) {
      delete this.editableData[record.id];
      // editableData为空时  删除编辑列
      if (JSON.stringify(this.editableData) === "{}") {
        this.columns.some((item, i) => {
          if (item.dataIndex === "operation") {
            this.columns.splice(i, 1);
            return true;
          }
        });
      }
    },
    // 删除
    deletePublicEntry() {
      if (this.selectedRowKeys.length === 0) {
        return;
      }
      Modal.confirm({
        title: "是否确定删除?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "确定",
        cancelText: "取消",
        onOk: () => {
          deletePublicEntry(this.selectedRowKeys).then((res) => {
            message.success("删除成功！");
            this.getCommonEntry();
            this.selectedRowKeys = [];
          });
        },
      });
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