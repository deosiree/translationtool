<template>
  <!-- <a-spin :spinning="visible"> -->
  <Modal ref="updateBox" :modalWidth="modalWidth" :visible="visible" :updateClassfyID="updateClassfyID" :modalTitle="modalTitle"
    @handleClose="handleClose" @handleOK="handleOK">
    <div class="content" v-if="!taskVisible">
      <div class="table">
        <a-form ref="i18nURL" name="custom-validation">
          <a-form-item label="IP" name="ip">
            <a-select v-model:value="i18nURL" :options="ipOptions" placeholder="请选择IP" @change="handleUpdate" allowClear></a-select>
          </a-form-item>
        </a-form>
        <a-table class="ant-table-striped" :columns="columns" :dataSource="dataSource" :scroll="{x:'100%' , y: '280px'}"
          :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
          :row-selection=" { selectedRowKeys: selectedRowKeys, onChange: onSelectChange,onSelect:onSelect,onSelectAll:onSelectAll}"
          :row-key="record => record.sourceFile" ref="updateTable" bordered :pagination='pagination' :loading="loading">
        </a-table>
      </div>
    </div>
    <div class="content" v-if="taskVisible">
      <a-table class="ant-table-striped" :columns="taskColumns" :dataSource="taskSource" :scroll="{x:'100%' , y: '280px'}"
        :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" :row-key="record => record.sourceFile" ref="updateTaskTable"
        bordered :pagination='pagination'>
      </a-table>
    </div>
  </Modal>
  <!-- </a-spin> -->
</template>
<script>
import Modal from "@/components/modal/index.vue";
import { message,notification } from "ant-design-vue";
import { getI18nAdress } from "@/http/api/workbench";
import {
  updateEntryByClassfy,
  getEntrysourceListByClassfy,
} from "@/http/api/entryManage";
import { v4 as uuidv4 } from "uuid";
import { setModalAriaHidden } from "@/utils/commonUtils";
export default {
  components: {
    Modal,
  },
  emits: ["updateClose"],
  props: {
    // 传递来的数据放这儿，不能再在data中定义了
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
    },
    updateClassfyID: {
      type: String,
    },
  },
  data() {
    return {
      labelCol: { style: { width: "80px" } },
      modalWidth: "400px",
      i18nURL: null,
      ipOptions: [], // ip下拉选项
      columns: [
        {
          title: "词条来源",
          dataIndex: "sourceFile",
          align: "center",
          width: 400,
        },
        {
          title: "来源类型",
          dataIndex: "sourceType",
          align: "center",
          width: 100,
        },
      ],
      updateEntries: {
        // 发给我的原始数据
        type: Array,
      },
      dataSource: [], // 展示的数据
      selectedRowKeys: [],
      selectedRows: [],
      taskVisible: false,
      taskColumns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: "10%",
        },
        ,
        {
          title: "更新的任务",
          dataIndex: "task",
          align: "center",
          width: "90%",
        },
      ],
      taskSource: [],
      loading: false,
      pagination: {
        showSizeChanger: true,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
    };
  },
  mounted() {
    this.getIPs();
  },
  methods: {
    init() {
      this.updateEntries = {};
      this.dataSource = [];
      this.i18nURL = null;
      this.taskSource = [];
      this.taskVisible = false;
    },
    // 获取i18服务器ip
    getIPs() {
      this.ipOptions = [];
      getI18nAdress().then((res) => {
        res.data.list.forEach((item) => {
          let ip = {
            label: item.ip,
            value: item.ip,
          };
          this.ipOptions.push(ip);
        });
      });
    },
    // 更新窗口（i18nURL改变时触发）
    handleUpdate() {
      this.loading = true; //开始加载
      // console.log("classfyID", this.updateClassfyID);// 传自父组件的treeKey
      // console.log("i18nURL", this.i18nURL);// 传自本组件的mounted生命周期函数
      // 弹窗，并发送http请求(查询来源中新增的词条
      getEntrysourceListByClassfy({
        // classfyID: "690aae89-47f0-4578-8e6a-aefb35884403",
        // i18nUrl: "http://10.17.43.20:18099/",
        classfyID: this.updateClassfyID,
        i18nUrl: this.i18nURL,
      })
        .then((res) => {
          if(res.message){
            message.error(`请求失败: ${res.message}`, 3);
          }
          // 都放到.then内，可以确保执行顺序
          // console.log("查询来源中新增的词条", res.data.list);
          // 设置默认全选
          this.updateEntries = Object.values(res.data.list);
          // console.log("newDataSource", this.updateEntries);
          this.dataSource = [];
          this.selectedRows = [];
          this.selectedRowKeys = [];
          if (this.updateEntries.length != 0) {
            this.updateEntries.forEach((item) => {
              const EntryVO = Object.values(item.sourceFileAndEntryVO);
              const sourceType = item.type;
              if (EntryVO.length != 0) {
                EntryVO.forEach((item) => {
                  this.selectedRowKeys.push(item.sourceFile);
                  this.selectedRows.push({
                    sourceFile: item.sourceFile,
                    sourceType: sourceType,
                    // sourceFileAndEntryVO: item, // 返回的值，不用于展示，用于提交
                  });
                });
              }
            });
            // console.log("selectedRowKeys", this.selectedRowKeys);
            // console.log("selectedRows", this.selectedRows);
          }
          this.dataSource = this.selectedRows;
        })
        .catch((error) => {
          if (error.status == 200) {
            message.error(`请求失败: ${error.data.operationObject}`);
          } else {
            message.error(`请求失败，状态码: ${error.data.operationObject}`);
          }
        })
        .finally(() => {
          this.loading = false;
        });
    },
    handleClose() {
      this.init();
      this.$emit("updateClose");
    },
    handleOK() {
      if (!this.taskVisible) {
        // 点击确认就发送http请求，更新词条
        const data = [];
        this.updateEntries.forEach((item) => {
          // console.log("item", item);
          const res = {
            type: "",
            sourceFileAndEntryVO: [],
          };
          let resFlag = false; // 标记是否已经添加过
          res.type = item.type;
          // console.log("item.sourceFileAndEntryVO", Object.values(item.sourceFileAndEntryVO)[0].sourceFile);
          Object.values(item.sourceFileAndEntryVO).forEach((file) => {
            if (this.selectedRowKeys.includes(file.sourceFile)) {
              res.sourceFileAndEntryVO.push(file);
              resFlag = true;
            }
          });
          if (resFlag) data.push(res);
        });
        // console.log("data", data);
        updateEntryByClassfy(data).then((res) => {
          // console.log("updateEntryByClassfy", res);
          notification.success({
            message: "更新成功！",
            duration: 0,
          });
          this.dataSource = [];
          // this.$emit("updateClose");// 关闭弹窗
          this.taskSource = Object.values(res.data.list).map((item, index) => ({
            index: index + 1,
            task: item,
          }));
          // console.log("taskSource", this.taskSource);
          this.taskVisible = true; // 先弹出其他弹窗
          setModalAriaHidden(this, document);
        }).catch((error) => {
          if (error.status == 200) {
            message.error(`请求失败: ${error.data.operationObject}`);
          } else {
            console.log(`请求失败: ${error}`);
          }
        });
      } else {
        this.init();
        this.$emit("updateClose");
      }
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
          return item !== record;
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
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
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