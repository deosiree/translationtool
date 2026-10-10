<template>
  <CustomModal :modalWidth="modalWidth" :visible="visible" :modalTitle="modalTitle" :showOk="false" cancelText="关闭" @handleClose="handleClose"
    @handleOK="handleOK" @afterClose="afterClose">
    <div style="width:100%;">
      <div class="table">
        <a-table class="ant-table-striped" :columns="columns" :data-source="currentData" :scroll="{x:'100%' , y: '280px'}"
          :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" :row-key="record => record.id" ref="secondClassifyTable"
          bordered>
          <template #bodyCell="{ column, record,text }">
            <template v-if="column.dataIndex === 'entryState'">
              <EntryStateBadge :entryState="text" />
            </template>
            <template v-if="column.dataIndex === 'translateState'">
              <TransStateBadge :translateState="text" />
            </template>
            <!--tag列 -->
            <template v-if="column.dataIndex === 'tag'">
              <span>
                <a-tag color="cyan" class="tag-content">
                  <span>{{ record.tag }}</span>
                </a-tag>
              </span>
            </template>
          </template>
        </a-table>
      </div>
    </div>
  </CustomModal>
</template>
<script>
import "@/assets/style/common.less";
import CustomModal from "@/components/modal/index.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import locale from "ant-design-vue/es/date-picker/locale/zh_CN";
import {
  CheckOutlined,
  EditOutlined,
  CloseOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import { cloneDeep, iteratee } from "lodash-es";
export default {
  components: {
    CustomModal,
    CheckOutlined,
    EditOutlined,
    CloseOutlined,
    ExclamationCircleOutlined,
    EntryStateBadge,
    TransStateBadge,
  },
  emits: ["relationClose"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
      default: "关联信息",
    },
    currentData: {
      type: Object,
    },
  },

  data() {
    return {
      locale: locale,
      modalWidth: "70%",
      dataSource: [],
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 60,
          customRender: (text, record, index, column) => {
            return text.index + 1;
          },
          fixed: "left",
        },
        { title: "词条", dataIndex: "entry", align: "center", width: 200 },
        {
          title: "词条状态",
          dataIndex: "entryState",
          align: "center",
          width: 120,
        },
        { title: "翻译", dataIndex: "translate", align: "center", width: 200 },
        {
          title: "翻译状态",
          dataIndex: "translateState",
          align: "center",
          width: 200,
        },
        {
          title: "分类名称",
          dataIndex: "classify",
          align: "center",
          width: 200,
        },
        {
          title: "产品名称",
          dataIndex: "productName",
          align: "center",
          width: 200,
        },
        { title: "Tag", dataIndex: "tag", align: "center", width: 200 },
        { title: "comment", dataIndex: "comment", align: "center", width: 200 },
        {
          title: "版本",
          dataIndex: "versionName",
          align: "center",
          width: 200,
        },
        { title: "辞典名称", dataIndex: "diName", align: "center", width: 200 },
        {
          title: "词条来源",
          dataIndex: "entrySource",
          align: "center",
          width: 200,
        },
        {
          title: "任务名称",
          dataIndex: "taskName",
          align: "center",
          width: 200,
        },
        { title: "创建人", dataIndex: "userName", align: "center", width: 200 },
        {
          title: "创建时间",
          dataIndex: "createTime",
          align: "center",
          width: 200,
        },
        { title: "Abbr", dataIndex: "abbr", align: "center", width: 200 },
      ],
      search: {
        classify1: null,
      },
      classfy1Option: [],
      product: {},
      editableData: {},
      selectedRowKeys: [],
    };
  },

  created() {},
  mounted() {
    this.dataSource = this.currentData;
    // console.log(this.dataSource);
  },
  watch: {
    currentProduct(newval, oldval) {
      this.dataSource = newval;
    },
  },
  methods: {
    handleClose() {
      this.$emit("relationClose");
    },
    handleOK() {
      this.$emit("relationClose");
    },
    // 初始化数据
    init() {},
    afterClose() {
      this.dataSource = [];
    },
  },
};
</script>
<style lang="less" scoped>
.table {
  width: 100%;
  margin-top: 5px;
  position: relative;
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
</style>