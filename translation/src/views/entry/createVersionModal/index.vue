<template>
  <CustomModal :modalWidth="modalWidth" modalTitle="批量选择" :visible="visible" :fullFlag="true"
    :showCancel="preTranslateActive" :cancelText="preTranslateActive ? '取消' : ''"
    :okText="preTranslateActive ? '保存' : '创建产品版本'"
    @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose" @setTableHeight="setTableHeight">
    <div style="width:100%;height:515px">
      <div class="table">
        <div>已选词条：</div>
        <a-config-provider :locale="locale">
          <a-table class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="tableHeight"
            :pagination="pagination" :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
            ref="historyTable" bordered>
            <template #bodyCell="{ column, record, text }">
              <template v-if="column.dataIndex === 'entryState'">
                <EntryStateBadge :entryState="text" />
              </template>
              <template v-if="langTranslateStateList.includes(column.dataIndex)">
                <TransStateBadge :translateState="text" />
              </template>
              <!-- 语种列（预翻译编辑态）：TableCellTextArea 红字编辑 / 浏览态 -->
              <template v-if="editableTextColumns.includes(column.dataIndex)">
                <TableCellTextArea v-if="editableData[record.id]" :value="editableData[record.id][column.dataIndex] ?? ''"
                  :error-message="cellErrors[record.id]?.[column.dataIndex]"
                  @update:value="(val) => onCellInput(val, record, column)" />
                <CellOverflowTooltip v-else :content="text" />
              </template>
              <template v-if="column.dataIndex === 'operation' && !preTranslateActive">
                <div class="editable-row-operations">
                  <DeleteOutlined style="color:#369FFF;font-size:16px" @click="remove(record)" title="取消选择" />
                </div>
              </template>
              <template v-if="column.dataIndex === 'operation' && preTranslateActive">
                <div class="editable-row-operations">
                  <CheckOutlined v-if="editableData[record.id]" style="color: #369fff; margin-left: 8px"
                    @click="rowConfirm(record)" title="保存" />
                  <CloseOutlined v-if="editableData[record.id]" style="color: red; margin-left: 8px"
                    @click="rowDiscard(record)" title="取消" />
                </div>
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
            <template #customFilterDropdown="{ setSelectedKeys, selectedKeys, confirm, clearFilters, column }">
              <div style="padding: 8px">
                <a-input ref="searchInput" :placeholder="`搜索 ${column.title}`" :value="selectedKeys[0]"
                  style="width: 188px; margin-bottom: 8px; display: block"
                  @change="e => setSelectedKeys(e.target.value ? [e.target.value] : [])"
                  @pressEnter="handleSearch(selectedKeys, confirm, column.dataIndex)" />
                <a-button type="primary" size="small" style="width: 90px; margin-right: 8px"
                  @click="handleSearch(selectedKeys, confirm, column.dataIndex)">
                  <template #icon>
                    <SearchOutlined />
                  </template>搜索</a-button>
                <a-button size="small" style="width: 90px" @click="handleReset(clearFilters)">重置</a-button>
              </div>
            </template>
            <template #customFilterIcon="{ filtered }">
              <SearchOutlined :style="{ color: filtered ? '#108ee9' : undefined }" />
            </template>
          </a-table>
        </a-config-provider>
      </div>
    </div>
    <!-- 正常模式：操作按钮排；预翻译编辑模式：隐藏（底部只剩 取消/保存） -->
    <template v-slot:leftBottomBtn v-if="!preTranslateActive">
      <a-button @click="cancelCreate">关闭</a-button>
      <a-button type="primary" v-if="$currentDepartment && $currentDepartment.ops.has('needIP')"
        @click="writeBackVisible = true">回写</a-button>
      <a-button type="primary" danger @click="deleteEntrys" v-if="$currentDepartment && $currentDepartment.ops.has('needDelete')">删除</a-button>
      <a-button type="primary" danger @click="forrbiddenEntrys"
        v-if="$currentDepartment && $currentDepartment.ops.has('needForbidden') && $store.state.admin">禁用</a-button>
      <ExportButton :dataSource="dataSource" :fieldOptions_="fieldOptions" size="middle" buttonTitle="导出" />
      <a-button type="primary" @click="examine"
        v-if="$currentDepartment && $currentDepartment.ops.has('needExamine')">提交词条审核</a-button>
      <a-button type="primary" @click="preTranslateFormVisible = true">预翻译</a-button>
    </template>
  </CustomModal>
  <!-- 二级操作弹窗（已按职责拆分） -->
  <CreateVersionForm :visible="createVersionFormVisible" :dataSource="dataSource" :product="product"
    @handleClose="createVersionFormVisible = false" @finished="onOperateFinished" />
  <ExamineTaskForm :visible="examineFormVisible" :dataSource="dataSource" :product="product"
    @handleClose="examineFormVisible = false" @finished="onOperateFinished" />
  <WriteBackForm :visible="writeBackVisible" :dataSource="dataSource" @handleClose="writeBackVisible = false" />
  <PreTranslateForm :visible="preTranslateFormVisible" :loading="preTranslateLoading"
    @handleClose="preTranslateFormVisible = false" @submit="onPreTranslateSubmit" />
</template>
<script>
import CustomModal from "@/components/modal/index.vue";
import ExportButton from "@/components/Button/exportButton.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import TableCellTextArea from "@/components/table/TableCellTextArea.vue";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip.vue";
import CreateVersionForm from "./CreateVersionForm.vue";
import ExamineTaskForm from "./ExamineTaskForm.vue";
import WriteBackForm from "./WriteBackForm.vue";
import PreTranslateForm from "./PreTranslateForm.vue";
import zh_CN from "ant-design-vue/es/locale/zh_CN";
import {
  ExclamationCircleOutlined,
  DeleteOutlined,
  SearchOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
  CheckOutlined,
  CloseOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import { createVNode } from "vue";
import { deleteEntryInfoByID } from "@/http/api/workbench.js";
import { forbiddenEntryInfo } from "@/http/api/entryManage.js";
import { updateUserPartiality } from "@/http/api/userPartiality";
import commonParam, { entryParams, entryAllCols, entryPresets } from "@/constants/commonParam.js";
import { pageChange as pageChangeUtil } from "@/utils/selectionUtils";
import { onEditableCellInput } from "@/utils/validationUtils.js";
import {
  handleSearch as handleSearchUtil,
  handleReset as handleResetUtil,
} from "@/utils/tableUtils";
import { applyTable } from "@/components/ColumnFilter";
import { setModalAriaHidden } from "@/utils/domUtils";
import { withLoading } from "@/composables/useLoading";
import { usePreTranslateEdit } from "@/composables/entry/usePreTranslateEdit.js";
import { cloneDeep } from "lodash-es";

const { execute, cancelAll, save, discardRow, confirmRow } = usePreTranslateEdit();

export default {
  components: {
    CustomModal,
    ExclamationCircleOutlined,
    DeleteOutlined,
    SearchOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    CheckOutlined,
    CloseOutlined,
    ExportButton,
    EntryStateBadge,
    TransStateBadge,
    TableCellTextArea,
    CellOverflowTooltip,
    CreateVersionForm,
    ExamineTaskForm,
    WriteBackForm,
    PreTranslateForm,
  },
  emits: [
    "createClose",
    "cancelCreate",
    "refresh",
    "update:dataSource", // 添加 update:dataSource 事件
    "update:selectedRowKeys", // 添加 update:selectedRowKeys 事件
    "update:selectedRows", // 添加 update:selectedRows 事件
    "update:selectedProducts",
  ],
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
    // 分类限制（getMaxLength 依赖；预翻译长度校验需要）
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
  },

  data() {
    return {
      locale: zh_CN,
      modalWidth: "60%",
      // tableHeight: { x: "100%", y: 395 },
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

      // ===== 二级弹窗显隐（按职责拆分后各自独立） =====
      createVersionFormVisible: false,
      examineFormVisible: false,
      writeBackVisible: false,
      preTranslateFormVisible: false,

      // ===== 预翻译编辑态（复用工作台 validationUtils 机制） =====
      editableData: {},
      rules: {},
      cellErrors: {},
      preTranslateActive: false, // 编辑模式：底部切换为 取消/保存
      preTranslateSnapshot: null, // 取消恢复依据
      preTranslateLoading: false,
      // 保存对比快照时需要检查的列（entry 各语种）
      fieldsNeedSave: ["entry", ...commonParam.langValList],
    };
  },

  created() {
    this.product = this.currentProduct;
  },
  computed: {
    // 可编辑语种列（固定为全部语种列；编辑态渲染优先于 TransStateBadge 列模板）
    editableTextColumns() {
      return this.preTranslateActive ? commonParam.langValList : [];
    },
  },
  watch: {
    currentProduct(newval, oldval) {
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
            normalWidth: 100,
            needFilter: false,
          });
        }
      },
      immediate: false, // 不立即执行
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
      if (this.preTranslateActive) {
        // 编辑模式：确认后取消预翻译（模态框不关）
        Modal.confirm({
          title: "是否取消预翻译?",
          icon: createVNode(ExclamationCircleOutlined),
          content: "取消后，所有词条的预翻译结果将被丢弃",
          okText: "是",
          cancelText: "否",
          style: { top: "30%" },
          onOk: () => {
            this.preTranslateCancel();
          },
        });
        return;
      }
      this.$emit("createClose");
    },
    // 底部主按钮：正常模式=创建版本入口；预翻译模式=保存
    async handleOK() {
      if (this.preTranslateActive) {
        await this.preTranslateSave();
        return;
      }
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
      // 防御：万一编辑模式下被外层关闭，恢复现场
      if (this.preTranslateActive) {
        cancelAll(this);
      }
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

    // ===== 预翻译 v2：编辑态编排 =====

    // 配置弹窗提交：执行预翻译并进入编辑模式
    async onPreTranslateSubmit(config) {
      if (this.dataSource.length === 0) {
        message.warn("没有已选词条，无法预翻译！");
        return;
      }
      this.preTranslateLoading = true;
      try {
        await withLoading(async () => {
          await execute(this, config);
        });
        this.preTranslateFormVisible = false;
      } catch (err) {
        message.error("预翻译失败！", err?.message);
      } finally {
        this.preTranslateLoading = false;
      }
    },

    // 编辑模式-取消：回滚快照（模态框不关）
    preTranslateCancel() {
      const restored = cancelAll(this);
      this.$emit("update:dataSource", restored);
    },

    // 编辑模式-保存：校验编辑行 → 逐条落库 → 成功行移除；列表清空则关壳
    async preTranslateSave() {
      const result = await withLoading(async () => save(this));
      if (!result.allPassed) {
        return; // 存在校验失败行：红字已标，保持打开
      }
      if (result.remaining != null && result.remainingCount === 0) {
        // 全部保存成功且移除完：清空选择并关壳、刷新主表
        this.$emit("update:dataSource", []);
        this.$emit("update:selectedRows", []);
        this.$emit("update:selectedRowKeys", []);
        this.$emit("createClose");
        this.$emit("cancelCreate");
        this.$emit("refresh");
      } else if (result.remaining != null) {
        // 部分保存成功：剩余行留在列表（编辑模式退出，可继续其他操作）
        this.$emit("update:dataSource", result.remaining);
        this.$emit("refresh");
      }
      // savedRecords 为空且 remaining 为空数组外的情况（无改动）：无操作
    },

    // 编辑单元格输入（onEditableCellInput 写 editableData + 清红字）
    onCellInput(value, record, column) {
      onEditableCellInput(this, record.id, column.dataIndex, value);
    },

    // 行内 ✓：校验并提交该行（不落库，等底部保存）
    async rowConfirm(record) {
      await confirmRow(this, record);
    },
    // 行内 ×：丢弃该行预翻译译文
    rowDiscard(record) {
      discardRow(this, record);
    },

    // 动态设置表格高度
    setTableHeight(height, type) {
      if (type === "full") {
        this.tableHeight.y = height - 150;
      } else if (type === "reduce") {
        this.tableHeight.y = 395;
      }
    },
    // 列头筛选（转 tableUtils）
    handleSearch(selectedKeys, confirm, dataIndex) {
      handleSearchUtil(selectedKeys, confirm, dataIndex, this);
    },
    handleReset(clearFilters) {
      handleResetUtil(clearFilters, this);
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

.ant-form-inline .ant-form-item-with-help {
  margin-bottom: 0px;
}
</style>
