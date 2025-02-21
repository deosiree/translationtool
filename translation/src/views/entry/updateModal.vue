<template>
  <Modal :modalWidth="modalWidth" :visible="visible" :modalTitle="modalTitle"
    :row-selection=" { selectedRowKeys: selectedRowKeys, onChange: onSelectChange,onSelect:onSelect,onSelectAll:onSelectAll}"
    :row-key="record => record.id" @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose" @click="getUpdateEntry">
    <div class="content">
      <div class="table">
        <a-table class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="{x:'100%' , y: '280px'}"
          :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" :row-key="record => record.id" ref="updateTable"
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
import { getUpdateEntryByClassfy } from "@/http/api/entryManage";
import { message } from "ant-design-vue";
import { v4 as uuidv4 } from "uuid";
export default {
  components: {
    Modal,
  },
  emits: ["classifyClose"],
  props: {
    // 传递来的数据放这儿，不能再在data中定义了
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
    },
    updateEntries: [],
    dataSource: [],
  },
  data() {
    return {
      labelCol: { style: { width: "80px" } },
      modalWidth: "400px",
      columns: [
        {
          title: "词条来源",
          dataIndex: "entrySource",
          align: "center",
          width: 100,
        },
        {
          title: "词条",
          dataIndex: "entries",
          align: "center",
          width: 400,
        },
      ],
      selectedRows: [],
      selectedRowKeys: [],
    };
  },
  mounted() {
    // this.$emit("updateEntries");
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
      let params = {};
      let data = {
        // id: this.entries.key,
        // name: this.entries.title,
        // parentId: this.entries.parentId,
      };
      // getUpdateEntryByClassfy(params, data).then((res) => {
      //   console.log("getUpdateEntryByClassfy", res);
      //   message.success("更新成功！");
      //   this.$emit("classifyClose");
      //   this.dataSource = res.data.list;
      // });
      this.$emit("classifyClose");
    },
    afterClose() {
      this.updateEntries.title = "";
      this.updateEntries.maxByte = "";
      this.$refs.formRef.clearValidate();
    },
    // 表格复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
    },
    // 表格复选框点击事件
    onSelect(record, selected) {
      if (this.createVersionFlag) {
        // 创建版本时使用
        if (selected) {
          this.selectEntry.push(record);
        } else {
          this.selectEntry = this.selectEntry.filter((item) => {
            return item.id !== record.id;
          });
        }
      }
    },
    // 表格全选/反选框点击事件
    onSelectAll(selected, selectedRows, changeRows) {
      if (this.createVersionFlag) {
        if (selected) {
          this.selectEntry = this.selectEntry.concat(changeRows);
        } else {
          changeRows.forEach((item) => {
            this.selectEntry = this.selectEntry.filter((entry) => {
              return entry !== item;
            });
          });
        }
      }
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