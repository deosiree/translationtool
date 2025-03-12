<template>
  <CustomModal :modalWidth="modalWidth" :visible="visible" :modalTitle="modalTitle" :showOk="false" :fullFlag="true" cancelText="关闭"
    @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose" @setTableHeight="setTableHeight">
    <div style="width:100%;">
      <a-form :model="search" layout="inline" autocomplete="off" ref="formRef">
        <a-form-item label="辞典" name="fileName" :rules="[{ required: true, message: '请选择辞典!' }]">
          <a-select v-model:value="search.fileName" style="width: 200px" placeholder="请选择" :options='dictionaryOption' allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="词条" name="entry">
          <a-input v-model:value="search.entry" placeholder="请输入内容"></a-input>
        </a-form-item>
        <a-form-item label="Tag" name="tag">
          <a-input v-model:value="search.tag" placeholder="请输入内容"></a-input>
        </a-form-item>
        <a-form-item label="来源" name="common">
          <a-input v-model:value="search.common" placeholder="请输入内容"></a-input>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="select">查看</a-button>
          <a-button type="primary" danger @click="clearDic" style="margin-left:8px">清空</a-button>
        </a-form-item>
      </a-form>
      <div class="table">
        <a-table class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="tableHeight" :pagination='pagination'
          :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" :row-key="record => record.id" :loading="loading"
          ref="dictionaryTable" bordered>
          <template #bodyCell="{ column, text }">
            <template v-if="column.dataIndex === 'translation'">
              <pre>{{ text }}</pre>
            </template>
          </template>
          <!--Tag列 -->
          <template v-if="column.dataIndex === 'tag'">
            <span>
              <a-tag color="cyan" class="tag-content">
                <span>{{ text }}</span>
              </a-tag>
            </span>
          </template>
        </a-table>
      </div>
    </div>
  </CustomModal>
</template>
<script>
import '@/assets/style/common.less'
import CustomModal from "@/components/modal/index.vue";
import {
  CheckOutlined,
  EditOutlined,
  CloseOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import { cloneDeep, iteratee } from "lodash-es";
import { getDictionary, clearDic } from "@/http/api/i18Server";
import { getDictory } from "@/http/api/workbench";
export default {
  components: {
    CustomModal,
    CheckOutlined,
    EditOutlined,
    CloseOutlined,
    ExclamationCircleOutlined,
  },
  emits: ["dictionaryClose"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
      default: "辞典查看",
    },
    currentProduct: {
      type: Object,
    },
  },

  data() {
    return {
      modalWidth: "65%",
      tableHeight: { x: "100%", y: 380 },
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
        },
        { title: "词条", dataIndex: "source", align: "center", width: 200 },
        { title: "来源", dataIndex: "comments", align: "center", width: 200 },
        { title: "Tag", dataIndex: "tag", align: "center", width: 150 },
        { title: "翻译", dataIndex: "translation" },
      ],
      search: {
        fileName: null,
        source: "",
        tag: "",
        common: "",
      },
      dictionaryOption: [],
      product: {},
      loading: false,
      pagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
    };
  },

  created() {},
  mounted() {
    this.product = this.currentProduct;
  },
  watch: {
    currentProduct(newval, oldval) {
      this.product = newval;
    },
  },
  methods: {
    handleClose() {
      this.$emit("dictionaryClose");
    },
    handleOK() {
      this.$emit("dictionaryClose");
    },
    // 初始化数据
    init() {
      getDictionary().then((res) => {
        this.dictionaryOption = [];
        res.data.list.forEach((item) => {
          let option = {
            label: item,
            value: item,
          };
          this.dictionaryOption.push(option);
        });
      });
    },
    select() {
      this.$refs.formRef.validate().then(() => {
        this.loading = true;
        getDictory(this.search)
          .then((res) => {
            this.dataSource = res.data.list;
            this.loading = false;
          })
          .catch((err) => {
            this.loading = false;
            message.error("查询失败！");
          });
      });
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
    },
    afterClose() {
      this.dataSource = [];
      this.dictionaryOption = [];
      this.search = {
        fileName: null,
        source: "",
        tag: "",
        common: "",
      };
    },
    // 格式化json
    prettyJson(json) {
      try {
        return JSON.stringify(json, null, 2);
      } catch (e) {
        return json;
      }
    },
    // 动态设置表格高度
    setTableHeight(height, type) {
      if (type === "full") {
        this.tableHeight.y = height - 170;
      } else if (type === "reduce") {
        this.tableHeight.y = 380;
      }
    },
    // 清空辞典
    clearDic() {
      if (this.search.fileName === null || this.search.fileName === "") {
        message.info("请选择辞典！");
        return;
      }
      let fileName = this.search.fileName;
      Modal.confirm({
        title: "确定要清空当前辞典吗?",
        icon: createVNode(ExclamationCircleOutlined),
        content: "",
        okText: "确定",
        okType: "danger",
        cancelText: "取消",
        onOk() {
          console.log(fileName);
          let params = {
            dicName: fileName,
          };
          clearDic(params)
            .then((res) => {
              message.success("清空成功！");
            })
            .catch((err) => {
              message.error("清空失败！");
            });
        },
        onCancel() {},
      });
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
.ant-form-item-with-help {
  margin-bottom: 0%;
}
</style>