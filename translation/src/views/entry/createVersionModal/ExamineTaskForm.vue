<template>
  <!-- 提交词条审核：词条管理-已选词条-选择任务的二级表单弹窗 -->
  <CustomModal modalTitle="选择任务" modalWidth="50%" :modalVisible="visible" @handleClose="handleClose"
    @handleOK="handleOK" @afterClose="afterClose">
    <div style="width:100%;height:100%">
      <div class="table">
        <a-table class="ant-table-striped" :columns="taskColumns" :data-source="taskDataSource"
          :row-selection='taskRowSelection' :row-key="record => record.id" :scroll="{ x: '100%', y: '195px' }"
          :pagination="false" :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
          ref="taskTable" bordered>
        </a-table>
      </div>
    </div>
  </CustomModal>
</template>
<script>
import CustomModal from "@/components/modal/index.vue";
import { message, Modal } from "ant-design-vue";
import { createVNode } from "vue";
import { ExclamationCircleOutlined } from "@ant-design/icons-vue";
import { addProductRelation } from "@/http/api/entryManage";
import { searchTaskInfo } from "@/http/api/task";
import { setModalAriaHidden } from "@/utils/domUtils";
export default {
  components: { CustomModal, ExclamationCircleOutlined },
  emits: ["handleClose", "finished"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    // 已选词条
    dataSource: {
      type: Array,
      default: () => [],
    },
    // 当前产品（词条挂到 product.key 下）
    product: {
      type: Object,
      default: () => ({}),
    },
  },
  data() {
    return {
      taskColumns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 70,
          customRender: (text, record, index, column) => {
            return text.index + 1;
          },
          fixed: "left",
        },
        {
          title: "任务名称",
          dataIndex: "name",
          align: "center",
          width: 150,
          fixed: "left",
          resizable: true,
        },
        {
          title: "产品名称",
          dataIndex: "productName",
          align: "center",
          width: 230,
          resizable: true,
        },
        {
          title: "版本名称",
          dataIndex: "versionName",
          align: "center",
          width: 180,
          resizable: true,
        },
        {
          title: "翻译语种",
          dataIndex: "translateType",
          align: "center",
          width: 150,
        },
        {
          title: "开发员",
          dataIndex: "developer",
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
          title: "翻译员",
          dataIndex: "translator",
          align: "center",
          width: 150,
        },
        {
          title: "翻译审核员",
          dataIndex: "translationAuditor",
          align: "center",
          width: 150,
        },
        {
          title: "任务描述",
          dataIndex: "description",
          align: "center",
          width: 230,
          ellipsis: true,
          resizable: true,
        },
        {
          title: "下发时间",
          dataIndex: "deliveryTime",
          align: "center",
          width: 200,
        },
      ],
      taskDataSource: [],
      selectedTaskRows: [],
    };
  },
  computed: {
    taskRowSelection() {
      return {
        type: "radio",
        onChange: (selectedRowKeys, selectedRows) => {
          this.selectedTaskRows = selectedRows;
        },
      };
    },
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        setModalAriaHidden(this, document);
        const productID =
          this.product.type === "module" ? this.product.parentId : this.product.key;
        this.getTaskList(productID);
      }
    },
  },
  methods: {
    handleClose() {
      this.$emit("handleClose");
    },
    // 获取该产品下的任务
    getTaskList(productID) {
      let params = {
        pageIndex: -1,
        pageSize: -1,
      };
      let data = {
        productId: productID,
        state: "1,2,3,4,5",
      };
      searchTaskInfo(data, params).then((res) => {
        this.taskDataSource = res.data.list;
      });
    },
    handleOK() {
      if (this.selectedTaskRows.length === 0) {
        message.warn("请选择任务！");
        return;
      }
      // 判断词条中是否含有 中文释义和英文释义都不存在的词条
      let notInterpretation = [];
      this.dataSource.forEach((item) => {
        if (
          (item.englishInterpretation === null ||
            item.englishInterpretation === "") &&
          (item.chineseInterpretation === null ||
            item.chineseInterpretation === "")
        ) {
          notInterpretation.push(item);
        }
      });
      if (notInterpretation.length > 0) {
        Modal.confirm({
          title:
            "保存数据中含有中文释义和英文释义都不存在的词条，是否继续保存?",
          icon: createVNode(ExclamationCircleOutlined),
          content: "",
          okText: "是",
          cancelText: "否",
          style: { top: "30%" },
          onOk: () => {
            this.submitExamine();
          },
          onCancel: () => { },
        });
        return;
      } else {
        this.submitExamine();
      }
    },
    // 提交词条审核
    submitExamine() {
      // 将词条提交到任务
      let data = [];
      this.dataSource.forEach((item) => {
        let info = {
          id: item.id,
          productID: this.product.key,
          taskId: this.selectedTaskRows[0].id,
          versionID: this.selectedTaskRows[0].versionId,
          enTransId: item.enTransId,
          english: item.english,
          fraTransId: item.fraTransId,
          french: item.french,
          ruTransId: item.ruTransId,
          russian: item.russian,
          spaTransId: item.spaTransId,
          spanish: item.spanish,
          entryState: item.entryState,
        };
        data.push(info);
      });
      addProductRelation(data)
        .then((res) => {
          message.success("已提交！");
          this.$emit("handleClose");
          this.$emit("finished");
        })
        .catch((err) => {
          message.error("提交失败！", err.message);
        });
    },
    afterClose() {
      this.taskDataSource = [];
      this.selectedTaskRows = [];
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
</style>
