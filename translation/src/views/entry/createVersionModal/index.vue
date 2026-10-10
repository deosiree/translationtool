<template>
  <CustomModal :modalWidth="modalWidth" modalTitle="批量选择" :visible="visible" :fullFlag="true"
    :showCancel="false"
    okText="创建产品版本" :okLoading="loading"
    @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose" @setTableHeight="setTableHeight">
    <div style="width:100%;height:515px">
      <div class="table">
        <div class="selected-entry-toolbar">已选词条：</div>
        <a-config-provider :locale="locale">
          <a-table class="ant-table-striped table-cell-overflow" :columns="columns" :data-source="dataSource" :scroll="tableHeight"
            :pagination="pagination" :loading="loading"
            :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
            ref="historyTable" bordered>
            <template #headerCell="{ title, column }">
              <CellOverflowTooltip v-if="column.colValue" :content="title">
                {{ title }}
              </CellOverflowTooltip>
            </template>
            <template #bodyCell="{ column, record, text }">
              <template v-if="column.dataIndex === 'entryState'">
                <CellOverflowTooltip :content="text">
                  <EntryStateBadge :entryState="text" />
                </CellOverflowTooltip>
              </template>
              <template v-else-if="langTranslateStateList.includes(column.dataIndex)">
                <CellOverflowTooltip :content="text">
                  <TransStateBadge :translateState="text" />
                </CellOverflowTooltip>
              </template>
              <template v-else-if="column.dataIndex === 'operation'">
                <div class="editable-row-operations">
                  <DeleteOutlined class="row-delete-icon"
                    style="color:#369FFF;font-size:16px;cursor:pointer" @click.stop="remove(record)"
                    title="取消选择" />
                </div>
              </template>
              <template v-else-if="column.dataIndex && column.dataIndex !== 'index'">
                <CellOverflowTooltip :content="formatCellText(text)" />
              </template>
            </template>
            <template #expandIcon="props">
              <span v-if="props.record.children != null && props.record.children.length > 0">
                <div v-if="props.expanded" style="display: inline-block; margin-right: 10px"
                  @click="(e) => { props.onExpand(props.record, e); }">
                  <CaretDownOutlined />
                </div>
                <div v-else style="display: inline-block; margin-right: 10px"
                  @click="(e) => { props.onExpand(props.record, e); }">
                  <CaretRightOutlined />
                </div>
              </span>
              <span v-else style="margin-right:23px"></span>
            </template>
          </a-table>
        </a-config-provider>
      </div>
    </div>
    <template v-slot:leftBottomBtn>
      <a-button @click="cancelCreate">关闭</a-button>
      <a-button type="primary" v-if="$currentDepartment && $currentDepartment.ops.has('needIP')"
        @click="writeBackVisible = true">回写</a-button>
      <a-button type="primary" danger @click="deleteEntrys" v-if="$currentDepartment && $currentDepartment.ops.has('needDelete')">删除</a-button>
      <a-button type="primary" danger @click="forrbiddenEntrys"
        v-if="$currentDepartment && $currentDepartment.ops.has('needForbidden') && $store.state.admin">禁用</a-button>
      <ExportButton :dataSource="dataSource" :fieldOptions_="fieldOptions" size="middle" buttonTitle="导出" />
      <a-button type="primary" @click="examine"
        v-if="$currentDepartment && $currentDepartment.ops.has('needExamine')">提交词条审核</a-button>
      <a-button type="primary" @click="openBatchEdit">编辑</a-button>
    </template>
  </CustomModal>
  <!-- 二级操作弹窗（已按职责拆分） -->
  <CreateVersionForm :visible="createVersionFormVisible" :dataSource="dataSource" :product="product"
    @handleClose="createVersionFormVisible = false" @finished="onOperateFinished" />
  <ExamineTaskForm :visible="examineFormVisible" :dataSource="dataSource" :product="product"
    @handleClose="examineFormVisible = false" @finished="onOperateFinished" />
  <WriteBackForm :visible="writeBackVisible" :dataSource="dataSource" @handleClose="writeBackVisible = false" />
  <BatchEditModal
    v-model:visible="batchEditOpen"
    :entries="dataSource"
    :classify-limit="classifyLimit"
    :rules-options-prop="rulesOptions"
    @saved="onBatchEditSaved"
    @refresh="$emit('refresh')"
  />
</template>
<script>
import CustomModal from "@/components/modal/index.vue";
import ExportButton from "@/components/Button/exportButton.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip.vue";
import { formatCellText } from "@/components/table/cellText";
import CreateVersionForm from "./CreateVersionForm.vue";
import ExamineTaskForm from "./ExamineTaskForm.vue";
import WriteBackForm from "./WriteBackForm.vue";
import BatchEditModal from "./BatchEditModal.vue";
import zh_CN from "ant-design-vue/es/locale/zh_CN";
import {
  ExclamationCircleOutlined,
  DeleteOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import { createVNode } from "vue";
import { deleteEntryInfoByID } from "@/http/api/workbench.js";
import { forbiddenEntryInfo } from "@/http/api/entryManage.js";
import { updateUserPartiality } from "@/http/api/userPartiality";
import commonParam, { entryParams, entryAllCols, entryPresets } from "@/constants/commonParam.js";
import { pageChange as pageChangeUtil } from "@/utils/selectionUtils";
import { applyTable } from "@/components/ColumnFilter";
import { setModalAriaHidden } from "@/utils/domUtils";
import { loading, isLoading } from "@/composables/useLoading";
import { cloneDeep } from "lodash-es";

export default {
  components: {
    CustomModal,
    ExclamationCircleOutlined,
    DeleteOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    ExportButton,
    EntryStateBadge,
    TransStateBadge,
    CellOverflowTooltip,
    CreateVersionForm,
    ExamineTaskForm,
    WriteBackForm,
    BatchEditModal,
  },
  emits: [
    "createClose",
    "cancelCreate",
    "refresh",
    "update:dataSource",
    "update:selectedRowKeys",
    "update:selectedRows",
    "update:selectedProducts",
  ],
  setup() {
    /**
     * 暴露全局 loading，供保存按钮 okLoading 与表格遮罩绑定。
     * @returns {{ loading: import('vue').ComputedRef<boolean> }}
     */
    return { loading };
  },
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    dataSource: {
      type: Array,
    },
    currentProduct: {
      type: Object,
    },
    classifyLimit: {
      type: Object,
      default: () => ({}),
    },
    selectedRowKeys: {
      type: Array,
      default: () => [],
    },
    selectedRows: {
      type: Array,
      default: () => [],
    },
    selectedProducts: {
      type: Object,
      default: () => ({
        products: new Map(),
        totalNum: 0,
      }),
    },
    /** 词条列表页勾选的校验规则，打开批量编辑时下传 */
    rulesOptions: {
      type: Array,
      default: null,
    },
  },

  data() {
    return {
      locale: zh_CN,
      modalWidth: "90%",
      tableHeight: { x: "max-content", y: 395 },
      columns: [],
      pagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
      fieldOptions: entryParams.exportFields,
      product: {},
      langTranslateStateList: commonParam.langTranslateStateList,

      createVersionFormVisible: false,
      examineFormVisible: false,
      writeBackVisible: false,
      batchEditOpen: false,
    };
  },

  created() {
    this.product = this.currentProduct;
  },
  watch: {
    currentProduct(newval) {
      this.product = newval;
    },
    visible: {
      async handler(newVal) {
        if (newVal) {
          applyTable(this, {
            allCols: entryAllCols,
            preset: entryPresets.createVersion,
            ctx: { pagination: this.pagination },
            colPrefName: "colPref-productEntry",
            normalWidth: 200,
            needFilter: false,
            lockCellSize: true,
          });
        }
      },
      immediate: false,
    },
  },
  methods: {
    // 移除某条已选词条（每行最右边的按钮
    remove(record) {
      const newdataSource = this.dataSource.filter((item) => {
        return item.id != record.id;
      });
      const newSelectedRowKeys = this.selectedRowKeys.filter((item) => {
        return item.id != record.id;
      });
      const newSelectedRows = this.selectedRows.filter((item) => {
        return item.id != record.id;
      });
      let newSelectedProducts = cloneDeep(this.selectedProducts);
      const pID =
        this.product.type == "module"
          ? this.product.parentId
          : this.product.key;
      // 重新更新，触发table重新渲染，已选词条列表会自动更新，不需要重新请求接口获取数据了，减少接口调用次数，提升性能
      this.$emit("update:dataSource", newdataSource); //
      this.$emit("update:selectedRowKeys", newSelectedRowKeys);
      this.$emit("update:selectedRows", newSelectedRows);
      if (newSelectedProducts.products.size > 0 && record.productID != pID) {
        newSelectedProducts.totalNum--; // 切换前的和已选词条不同步,需要手动更新
        const num = newSelectedProducts.products.get(record.productID) - 1;
        if (num != 0) newSelectedProducts.products.set(record.productID, num);
        else newSelectedProducts.products.delete(record.productID);
        this.$emit("update:selectedProducts", newSelectedProducts);
      }
    },
    handleClose() {
      this.$emit("createClose");
    },
    // 底部主按钮：创建产品版本
    async handleOK() {
      if (isLoading()) return;
      this.createVersionFormVisible = true;
      setModalAriaHidden(this, document);
    },
    // 二级表单完成（创建版本/提交审核）：关壳并清空选择
    onOperateFinished() {
      this.$emit("createClose");
      this.$emit("cancelCreate");
    },
    cancelCreate() {
      Modal.confirm({
        title: "是否确认关闭?",
        icon: createVNode(ExclamationCircleOutlined),
        content: "确认关闭后，已选择的词条将被清空",
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          this.$emit("cancelCreate");
        },
      });
    },
    afterClose() {
      this.batchEditOpen = false;
    },

    exportFieldChange(value) {
      let data = {
        exportColumn: value.join(","),
      };
      updateUserPartiality(data).then((res) => { });
    },
    // 提交词条审核（入口校验跨产品，表单已拆分到 ExamineTaskForm）
    examine() {
      const productID =
        this.product.type === "module"
          ? this.product.parentId
          : this.product.key; // 产品的ID
      const products = this.selectedProducts.products;
      if (
        products.size == 0 ||
        (products.size == 1 && products.has(productID))
      ) {
        // 只有这两种情况可以-1.切换记录中无其他产品2.切换记录中有且只有本产品
        this.examineFormVisible = true;
        setModalAriaHidden(this, document);
      } else {
        Modal.confirm({
          title: "存在非本产品的已选词条，不能选择任务。",
          icon: createVNode(ExclamationCircleOutlined),
          content: "",
          okText: "是",
          cancelText: "否",
          style: { top: "30%" },
          onOk: () => { },
          onCancel: () => { },
        });
      }
    },
    // 禁用词条
    forrbiddenEntrys() {
      Modal.confirm({
        title: "是否确定禁用?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          forbiddenEntryInfo(this.dataSource).then((res) => {
            this.$emit("createClose");
            this.$emit("cancelCreate");
            this.$emit("refresh");
          });
        },
      });
    },
    // 删除词条
    deleteEntrys() {
      Modal.confirm({
        title: "是否确定删除?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          const seen = {};
          this.dataSource.forEach((item) => {
            let key = "";
            if (item.versionID) key = `${item.productID}--${item.versionID}`;
            else key = `${item.productID}`;
            if (!seen[key]) {
              seen[key] = [];
            }
            seen[key].push(item.id);
          });

          // 存储删除成功和失败的信息
          let totalDeleted = 0;
          let failedProducts = {};
          let promises = [];

          // 遍历seen对象，并调用接口
          for (const [combinedKey, ids] of Object.entries(seen)) {
            const [productID, versionID] = combinedKey.split("--");
            let params = {};
            if (versionID == null || versionID == "")
              params = { productID: productID };
            else
              params = {
                versionID: versionID,
                productID: productID,
              };
            const promise = deleteEntryInfoByID(params, ids)
              .then(() => {
                totalDeleted += ids.length; // 累计成功条数
              })
              .catch((error) => {
                failedProducts[combinedKey] = {
                  count: ids.length,
                  reason: error.message || "未知错误",
                }; // 记录失败信息
              });
            promises.push(promise);
          }

          Promise.allSettled(promises).then(() => {
            let messageText = `总共删除了 ${totalDeleted} 条词条!`;
            if (Object.keys(failedProducts).length > 0) {
              messageText += "\n以下产品有词条删除失败：\n";
              for (const [combinedKey, info] of Object.entries(
                failedProducts
              )) {
                const [productID, versionID] = combinedKey.split("-");
                messageText += `产品ID: ${productID}, 版本ID: ${versionID}, 失败数量: ${info.count}, 原因: ${info.reason}\n`;
              }
              message.info(messageText);
            } else {
              message.success(messageText);
            }
            this.$emit("createClose");
            this.$emit("cancelCreate");
            this.$emit("refresh");
          });
        },
      });
    },

    // ===== 批量编辑 =====

    openBatchEdit() {
      if (!this.dataSource?.length) {
        message.warn("没有已选词条，无法编辑！");
        return;
      }
      this.batchEditOpen = true;
    },

    /**
     * 批量编辑保存后：合并成功行到已选（保留勾选），不剔除 keys。
     * @param {{savedRecords:Object[],remainingIds:string[],allDone:boolean}} payload
     */
    onBatchEditSaved(payload) {
      const { savedRecords = [], allDone } = payload || {};
      if (savedRecords.length > 0) {
        const byId = new Map(savedRecords.map((r) => [r.id, r]));
        const mergeList = (list) =>
          (list || []).map((row) => {
            const saved = byId.get(row.id);
            return saved ? { ...row, ...saved } : row;
          });
        this.$emit("update:dataSource", mergeList(this.dataSource));
        this.$emit("update:selectedRows", mergeList(this.selectedRows));
      }
      if (allDone) {
        this.batchEditOpen = false;
      }
    },

    /** 普通单元格文本格式化（供 CellOverflowTooltip 使用） */
    formatCellText,

    // 动态设置表格高度
    setTableHeight(height, type) {
      if (type === "full") {
        this.tableHeight.y = height - 150;
      } else if (type === "reduce") {
        this.tableHeight.y = 395;
      }
    },
    // 分页切换
    pageChange(page, pageSize) {
      pageChangeUtil(this, page, pageSize);
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

.selected-entry-toolbar {
  margin-bottom: 8px;
}

.editable-row-operations {
  display: flex;
  align-items: center;
  justify-content: center;
}

.ant-form-inline .ant-form-item-with-help {
  margin-bottom: 0px;
}
</style>
