<template>
  <Modal :modalWidth="modalWidth" :visible="visible" :modalTitle="modalTitle" @handleClose="handleClose" @handleOK="handleOK"
    @afterClose="afterClose">
    <div class="content">
      <div class="table">
        <a-table class="ant-table-striped" :columns="columns" :data-source="currentData" :scroll="{x:'100%' , y: '280px'}"
          :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" :row-key="record => record.id" ref="secondClassifyTable"
          bordered>
        </a-table>
      </div>
    </div>
  </Modal>
</template>
<script>
import Modal from "@/components/modal/index.vue";
import { addEntryClassfy, updateEntryClassfy } from "@/http/api/entryManage";
import { addProduct, updateProduct } from "@/http/api/product";
import { message } from "ant-design-vue";
import { v4 as uuidv4 } from "uuid";
export default {
  components: {
    Modal,
  },
  emits: ["classifyClose"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
    },
    currentClass: {},
  },
  data() {
    return {
      labelCol: { style: { width: "80px" } },
      modalWidth: "400px",
      classify: {
        title: "",
        maxByte: "",
        foreignMaxByte: "",
      },
      columns: [
        {
          title: "词条来源",
          dataIndex: "entrySource",
          align: "center",
          width: 100,
        },
        { title: "词条", dataIndex: "entry", align: "center", width: 400 },
      ],
    };
  },

  created() {},
  mounted() {
    this.classify = this.currentClass;
  },
  watch: {
    currentClass(newval, oldval) {
      this.classify = newval;
    },
  },
  methods: {
    handleClose() {
      this.$emit("classifyClose");
    },
    handleOK() {
      // 具体逻辑还未写：
      // 1.弹窗，并发送http请求（/entryInfo/checkNewEntryByClassfy 查询分类中新增的词条
      // 2.得到所有新词条，默认全选，可勾选不想要的，
      // 3.点击确认就发送http请求，更新到对应产品中（/workbench/insertEntry/{taskID} 新增词条
      this.$emit("classifyClose");
    },
    afterClose() {
      this.classify.title = "";
      this.classify.maxByte = "";
      this.$refs.formRef.clearValidate();
    },
  },
};
</script>
<style scoped>
:deep(.ant-form-item-label) {
  width: 85px;
}
.content {
  width: 100%;
  height: 100%;
  padding: 10px;
  background-color: #f3f3f3;
}
</style>