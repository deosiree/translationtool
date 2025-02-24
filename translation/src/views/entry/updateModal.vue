<template>
  <Modal :modalWidth="modalWidth" :visible="visible" :modalTitle="modalTitle" :row-key="record => record.id" @handleClose="handleClose"
    @handleOK="handleOK" @afterClose="afterClose">
    <div class="content">
      <div class="table">
        <a-table class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="{x:'100%' , y: '280px'}"
          :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
          :row-selection=" { selectedRowKeys: selectedRowKeys, onChange: onSelectChange,onSelect:onSelect,onSelectAll:onSelectAll}"
          :row-key="record => record.id" ref="updateTable" bordered>
          <!-- 表格单元格模板 -->
          <template #bodyCell="{ column, record }">
            <!-- 词性来源列 -->
            <template v-if="column.dataIndex === 'entriesList'">
              <div v-for="(item, index) in record.entriesList" :key="index" style="display: flex; gap: 100px;">
                <span class="entries">{{ item }}</span>
              </div>
            </template>
          </template>
        </a-table>
      </div>
    </div>
  </Modal>
</template>
<script>
import Modal from "@/components/modal/index.vue";
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
    dataSource: {
      type: Array,
    },
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
          dataIndex: "entriesList",
          align: "center",
          width: 400,
        },
      ],
      selectedRows: [],
      selectedRowKeys: [],
    };
  },
  mounted() {
  },
  watch: {
    dataSource: {
      immediate: true, // 在组件初始化时就会立即执行一次 handler 函数，确保在初始数据加载时也能设置默认全选。
      handler(newDataSource) {
        this.selectedRowKeys = newDataSource.map((record) => record.id); // 设置默认全选
      },
    },
  },
  methods: {
    handleClose() {
      this.$emit("classifyClose");
    },
    handleOK() {
      // 点击确认就发送http请求，更新到对应产品中（/workbench/insertEntry/{taskID} 新增词条
      let params = {};
      let data = {
        taskID: this.task.id
      };
      insertEntry(params, data).then((res) => {
        console.log("insertEntry", res);
        message.success("更新成功！");
        this.$emit("classifyClose");
        this.dataSource = [];
      });
      this.$emit("classifyClose");
    },
    afterClose() {
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
.entries {
  font-size: 12px;
  padding: 4px 8px;
  background-color: #eefffb;
  border: 1px solid #beede5;
  border-radius: 4px;
  color: #77b3c9;
  margin-bottom: 2px;
}
.content {
  width: 100%;
  height: 100%;
  padding: 10px;
  background-color: #f3f3f3;
}
</style>