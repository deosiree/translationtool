<template>
  <div class="box" ref="box">
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <a-form-item label="词条" name="entry">
            <a-input v-model:value="search.entry" placeholder="请输入词条"></a-input>
          </a-form-item>
          <a-form-item label="翻译" name="translate">
            <a-input v-model:value="search.translate" placeholder="请输入翻译"></a-input>
          </a-form-item>
          <a-form-item label="翻译类型" name="translateType">
            <a-select v-model:value="search.translateType" style="width: 186px" placeholder="请选择翻译类型" :options='translateTypes'
              :fieldNames="{label:'name',value:'name'}" allowClear>
            </a-select>
          </a-form-item>
          <a-form-item label="翻译状态" name="translateState">
            <a-select v-model:value="search.translateState" style="width: 186px" placeholder="请选择翻译状态" :options='translateStates' allowClear>
            </a-select>
          </a-form-item>
          <a-form-item label="可见范围" name="visualRange">
            <a-select v-model:value="search.visualRange" style="width: 186px" placeholder="请选择可见范围" :options='visualRanges' allowClear>
            </a-select>
          </a-form-item>
        </a-form>
      </template>
      <template v-slot:operate>
        <a-button type="primary" size="middle" class="yellowBtn" @click="showNotused">空挂术语</a-button>
        <a-button type="primary" size="middle" class="resetBtn" @click="reset">重置</a-button>
        <a-button type="primary" size="middle" @click="getSykEntry">查询</a-button>
      </template>
    </SearchBox>
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <template v-slot:data>
        <div style="width:100%;position: absolute;">
          <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :row-key="record => record.id"
            :scroll="tableHeight" :pagination='pagination' :loading="loading" :rowClassName="getRowClassName" ref="glossaryTable"
            @resizeColumn="handleResizeColumn" :customRow="customRow">
            <template #bodyCell="{ column, record,text}">
              <template v-if="column.dataIndex === 'translate'">
                <div>
                  <template v-if="editableData[record.id]">
                    <a-input v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" @click="clickInput" />
                  </template>
                  <template v-else>
                    {{ text }}
                  </template>
                </div>
              </template>
              <template v-if="column.dataIndex === 'translateState'">
                <template v-if="record[column.dataIndex] === '0'">
                  <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">未翻译</span>
                </template>
                <template v-if="record[column.dataIndex] === '1'">
                  <a-badge color="#FBB31F" /><span style="color:#FBB31F">待审核</span>
                </template>
                <template v-if="record[column.dataIndex] === '2'">
                  <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
                </template>
                <template v-if="record[column.dataIndex] === '3'">
                  <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
                </template>
              </template>
              <template v-if="column.dataIndex === 'operation'">
                <div class="editable-row-operations">
                  <span v-if="editableData[record.id]">
                    <a-button type="primary" ghost size="small" @click.stop="save(record.id)">保存</a-button>
                    <a-button type="primary" ghost size="small" danger @click.stop="cancel(record.id)">取消</a-button>
                  </span>
                  <span v-else>
                    <a-button type="primary" ghost size="small" @click.stop="viewRelation(record)">详情({{record.relationCount}})</a-button>
                  </span>
                </div>
              </template>
            </template>
          </a-table>
        </div>
      </template>
    </DataBox>
  </div>
  <RelationModal :visible="relationVisible" :currentData="relationData" @relationClose="relationClose"></RelationModal>
  <NotUsedModal ref="notUsedModal" :modalTitle="notUsedTitle" :visible="notUsedVisible" @notUsedClose="notUsedClose" style="width:90%;" />
</template>
<script>
import { message, Modal } from "ant-design-vue";
import locale from "ant-design-vue/es/date-picker/locale/zh_CN";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import commen from "@/views/entry/common.js";
import RelationModal from "@/views/glossary/relationModal.vue";
import NotUsedModal from "@/views/glossary/notUsedModal.vue";
import { cloneDeep, flatMap } from "lodash-es";
import {
  PlusOutlined,
  DeleteOutlined,
  CopyOutlined,
  SaveOutlined,
  SendOutlined,
  PlusCircleOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons-vue";

import { getLanguage } from "@/http/api/translate";
import {
  getSykEntry,
  updateSykEntry,
  getSykEntryRelation,
} from "@/http/api/glossary";
import { defineComponent, ref, createVNode } from "vue";
export default {
  components: {
    SearchBox,
    DataBox,
    RelationModal,
    NotUsedModal,
    PlusOutlined,
    DeleteOutlined,
    CopyOutlined,
    SaveOutlined,
    SendOutlined,
    PlusCircleOutlined,
  },
  data() {
    return {
      locale: locale,
      labelCol: { style: { width: "84px" } },
      search: {
        entry: "",
        translate: null,
        translateState: null,
        type: null,
        state: null,
        visualRange: null,
      },
      translateStates: [
        { label: "未翻译", value: "0" },
        { label: "待审核", value: "1" },
        { label: "审核不通过", value: "2" },
        { label: "已审核", value: "3" },
      ],
      visualRanges: [
        { label: "通用平台部", value: "通用平台部" },
        { label: "监控系统部", value: "监控系统部" },
        { label: "装置开发部", value: "装置开发部" },
        { label: "柔性输电系统部", value: "柔性输电系统部" },
      ],
      tableTitle: "术语列表",
      dataHeight: 400,
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
          title: "翻译类型",
          dataIndex: "type",
          align: "center",
          width: 230,
          resizable: true,
        },
        {
          title: "翻译状态",
          dataIndex: "translateState",
          align: "center",
          width: 180,
          resizable: true,
        },
        {
          title: "翻译字符数",
          dataIndex: "charLength",
          align: "center",
          width: 150,
        },
        {
          title: "可见范围",
          dataIndex: "visualRange",
          align: "center",
          width: 150,
        },
        {
          title: "词条审核员",
          dataIndex: "entryAuditor",
          align: "center",
          width: 150,
        },
        {
          title: "公开状态",
          dataIndex: "publicState",
          align: "center",
          width: 150,
        },
        {
          title: "最大限制长度",
          dataIndex: "maxLength",
          align: "center",
          width: 150,
        },
        {
          title: "审核意见",
          dataIndex: "auditSuggest",
          align: "center",
          width: 230,
          ellipsis: true,
          resizable: true,
        },
        { title: "备注", dataIndex: "remark", align: "center", width: 200 },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          width: 150,
          fixed: "right",
        },
      ],
      dataSource: [],
      editableData: {},
      translateTypes: [],
      pagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
      relationVisible: false,
      relationData: [],
      notUsedVisible: false,
      notUsedTitle: "未使用的翻译",
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
      this.getLanguage();
      this.getSykEntry();
    },
    // 动态设置表格高度
    setTableHeight() {
      this.$nextTick(() => {
        // 设置列表父元素高度
        let box = this.$refs.box.offsetHeight;
        let searchHeight = this.$refs.search.$el.offsetHeight;
        try {
          let operationAreaHeight = this.$refs.operationArea.$el.offsetHeight;
          this.dataHeight = box - searchHeight - operationAreaHeight - 30;
        } catch (error) {
          this.dataHeight = box - searchHeight - 30;
        }

        // 设置表格高度
        let buttonHeight = 0;
        try {
          buttonHeight = this.$refs.button.offsetHeight + 8;
        } catch (error) {}
        this.tableHeight.y = this.dataHeight - buttonHeight - 150;
      });
    },
    // 获取翻译语言
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
      });
    },
    getSykEntry() {
      this.loading = true;
      let params = {
        pageIndex: this.pagination.current,
        pageSize: this.pagination.pageSize,
      };
      getSykEntry(params, this.search)
        .then((res) => {
          this.dataSource = res.data.list;
          this.pagination.total = res.data.totalNum;
          this.loading = false;
          this.getSykEntryRelationCount();
        })
        .catch((err) => {
          this.loading = false;
        });
    },
    save(id) {
      updateSykEntry([this.editableData[id]])
        .then((res) => {
          message.success("编辑成功！");
          delete this.editableData[id];
          this.getSykEntry();
        })
        .catch((err) => {
          message.error("编辑失败！");
        });
    },
    // 取消
    cancel(id) {
      delete this.editableData[id];
    },
    getSykEntryRelationCount() {
      for (let item of this.dataSource) {
        getSykEntryRelation([item]).then((res) => {
          item["relationCount"] = res.data.list.length;
          item["reslations"] = res.data.list;
        });
      }
    },
    viewRelation(record) {
      this.relationData = record.reslations;
      // console.log(this.relationData);
      this.relationVisible = true;
    },
    relationClose() {
      this.relationVisible = false;
    },
    // 添加表格行点击事件
    customRow(record, index) {
      return {
        onDblclick: (event) => {
          if (this.editableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }
          this.editableData[record.id] = cloneDeep(
            this.dataSource.filter((item) => record.id === item.id)[0]
          );
        },
      };
    },
    // 空挂术语展示
    showNotused() {
      this.notUsedVisible = true;
    },
    notUsedClose() {
      this.notUsedVisible = false;
    },
    // 重置
    reset() {
      this.search = {
        entry: "",
        translate: null,
        translateState: null,
        type: null,
        state: null,
        visualRange: null,
      };
      this.getSykEntry();
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
      this.getSykEntry();
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
    clickInput(event) {
      event.stopPropagation();
    },
  },
};
</script>
<style lang="less">
@import url("@/assets/style/common.less");
</style>
<style scoped lang="less">
.box {
  width: 100%;
  height: 100%;
  padding: 16px;
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