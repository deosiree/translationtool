<template>
  <!-- <a-spin :spinning="visible"> -->
  <Modal ref="notUsedBox" :modalWidth="modalWidth" :visible="visible" :modalTitle="modalTitle" @handleClose="handleClose" @handleOK="handleOK">
    <div class="content">
      <div class="table">
        <a-table class="ant-table-striped" :columns="columns" :dataSource="dataSource" :scroll="{x:'100%' , y: '280px'}"
          :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" :row-selection="{selectedRowKeys: selectedRowKeys, 
                    onChange: onSelectChange,
                    selections:[
                        {key:'selectAll',text:'全部选择',onSelect:selectAllEntry},
                        {key:'clearAll',text:'取消选择',onSelect:clearAllEntry}
                    ]
                }" :row-key="record => record.id" ref="notUsedTable" bordered :pagination='pagination' :loading="loading">
          <template #bodyCell="{ column, text }">
            <template v-if="['deleteState','publicState'].includes(column.dataIndex)">
              <template v-if="text === 0 || text === '0'">
                <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
              </template>
              <template v-if="text === 1 || text === '1'">
                <a-badge color="#FBB31F" /><span style="color:#FBB31F">审核中</span>
              </template>
              <template v-if="text === 2 || text === '2'">
                <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
              </template>
              <template v-if="text === 3 || text === '3'">
                <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
              </template>
            </template>
            <template v-if="column.dataIndex === 'translateState'">
              <template v-if="record[column.dataIndex]==undefined||record[column.dataIndex]==null">
                <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">无翻译状态</span>
              </template>
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
          </template>
        </a-table>
      </div>
    </div>
    <template #leftBottomBtn>
      <a-button key="delete" type="danger" @click="deleteSykEntry">删除</a-button>
    </template>
  </Modal>
  <!-- </a-spin> -->
</template>
<script>
import Modal from "@/components/modal/index.vue";
import { message } from "ant-design-vue";
import { getSykNotUsed, deleteSykEntry } from "@/http/api/glossary";
import { v4 as uuidv4 } from "uuid";
import {onSelectChange, selectAllEntry, clearAllEntry, pageChange} from "@/utils/tableUtils";
export default {
  components: {
    Modal,
  },
  emits: ["notUsedClose"],
  props: {
    // 传递来的数据放这儿，不能再在data中定义了
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
    },
  },
  data() {
    return {
      labelCol: { style: { width: "80px" } },
      modalWidth: "400px",
      loading: false,
      columns: [
        {
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 100,
        },
        {
          title: "翻译",
          dataIndex: "translate",
          align: "center",
          width: 100,
        },
        {
          title: "翻译类型",
          dataIndex: "type",
          align: "center",
          width: 100,
        },
        // {
        //   title: "翻译状态",
        //   dataIndex: "translateState",
        //   align: "center",
        //   width: 100,
        // },// 有的翻译状态不正常，不知道咋处理，感觉也不是很有必要展示的，注掉了
        {
          title: "上次使用时间",
          dataIndex: "lastUseTime",
          align: "center",
          width: 200,
        },
        {
          title: "翻译字符数",
          dataIndex: "charLength",
          align: "center",
          width: 100,
        },
        {
          title: "可见范围",
          dataIndex: "visualRange",
          align: "center",
          width: 100,
        },
        {
          title: "删除状态",
          dataIndex: "deleteState",
          align: "center",
          width: 100,
        },
        {
          title: "最大限制长度",
          dataIndex: "maxLength",
          align: "center",
          width: 150,
        },
        {
          title: "公开状态",
          dataIndex: "publicState",
          align: "center",
          width: 100,
        },
        {
          title: "审核意见",
          dataIndex: "auditSuggest",
          align: "center",
          width: 100,
        },
      ],
      notUsedEntries: {
        // 发给我的原始数据
        type: Array,
      },
      dataSource: [], // 展示的数据
      selectedRowKeys: [],
      selectedRows: [],
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
  mounted() {},
  methods: {
    // 更新窗口
    getSykNotUsed() {
      this.loading = true; //开始加载
      getSykNotUsed()
        .then((res) => {
          // 都放到.then内，可以确保执行顺序
          // 设置默认全选
          this.notUsedEntries = Object.values(res.data.list);
          // console.log("newDataSource", this.notUsedEntries);
          this.dataSource = [];
          this.selectedRows = [];
          this.selectedRowKeys = [];
          if (this.notUsedEntries.length != 0) {
            this.notUsedEntries.forEach((item) => {
              this.dataSource.push({ ...item });
              // this.selectedRows.push({ ...item });
              // this.selectedRowKeys.push(item.id);
            });
          }
          // // console.log("selectedRowKeys", this.selectedRowKeys);
          // // console.log("selectedRows", this.selectedRows);
          // this.dataSource = this.selectedRows;
        })
        .catch((error) => {
          // console.log("error", error);
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
    // 删除词条
    deleteSykEntry() {
      if (this.selectedRowKeys.length == 0) {
        message.warning("请选择要删除的词条");
        return;
      }
      this.loading = true;
      // console.log("selectedRows", this.selectedRows);
      const deletedatas = [];
      this.selectedRows.forEach((item) => {
        const data = { ...item };
        data.deleteState = 1;
        deletedatas.push(data);
      });
      // console.log("deletedatas", deletedatas);
      const deleteLen = deletedatas.length;
      // setTimeout(() => {
      //   this.loading = false;
      //   message.success(`术语删除成功！一共${deletedatas.length}条`);
      //   this.getSykNotUsed();
      // }, 3000);
      deleteSykEntry(deletedatas)
        .then((res) => {
          getSykNotUsed()
            .then((res) => {
              // 设置默认全选
              this.notUsedEntries = Object.values(res.data.list);
              this.dataSource = [];
              this.selectedRows = [];
              this.selectedRowKeys = [];
              if (this.notUsedEntries.length != 0) {
                this.notUsedEntries.forEach((item) => {
                  this.selectedRows.push({ ...item });
                  this.selectedRowKeys.push(item.id);
                });
              }
              this.dataSource = this.selectedRows;
            })
            .catch((error) => {
              if (error.status == 200) {
                message.error(`请求失败: ${error.data.operationObject}`);
              } else {
                message.error(
                  `请求失败，状态码: ${error.data.operationObject}`
                );
              }
            })
            .finally(() => {
              if (res.data.totalNum) {
                message.warn(
                  `部分术语已删除，有${res.data.totalNum}条删除失败!`
                );
                console.log("删除失败的词条", res.data.list);
              } else message.success(`术语全部删除成功！一共${deleteLen}条`);
              this.loading = false;
            });
        })
        .catch((err) => {
          message.error("术语删除失败！");
          this.loading = false;
        });
    },
    handleClose() {
      this.$emit("notUsedClose");
    },
    handleOK() {
      this.$emit("notUsedClose");
    },
    // 表格复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      onSelectChange(this, selectedRowKeys, selectedRows);
    },
    // 表格复选框全选事件
    selectAllEntry() {
      selectAllEntry(this);
    },
    // 表格复选框全不选事件
    clearAllEntry() {
      clearAllEntry(this);
    },
    // 分页切换
    pageChange(page, pageSize) {
      pageChange(this, page, pageSize);
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