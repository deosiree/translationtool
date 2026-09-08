<template>
  <CustomModal :modalWidth="modalWidth" modalTitle="批量选择" :visible="visible" :fullFlag="true"
    :showCancel="isEditMode" :cancelText="isEditMode ? '取消' : ''"
    :okText="isEditMode ? '保存' : '创建产品版本'"
    @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose" @setTableHeight="setTableHeight">
    <div style="width:100%;height:515px">
      <div class="table">
        <div class="selected-entry-toolbar">
          <div>已选词条：</div>
          <RulesDropdown :options="rulesOptions" @update:options="rulesOptions = $event" />
        </div>
        <a-config-provider :locale="locale">
          <a-table class="ant-table-striped table-cell-overflow" :columns="columns" :data-source="dataSource" :scroll="tableHeight"
            :pagination="pagination" :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
            :customRow="customRow" ref="historyTable" bordered>
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
              <!-- 语种列（行编辑态）：TableCellTextArea 红字编辑 / 浏览态 -->
              <template v-else-if="editableTextColumns.includes(column.dataIndex)">
                <TableCellTextArea v-if="editableData[record.id]" :value="editableData[record.id][column.dataIndex] ?? ''"
                  :error-message="cellErrors[record.id]?.[column.dataIndex]"
                  @update:value="(val) => onCellInput(val, record, column)" />
                <CellOverflowTooltip v-else :content="text" />
              </template>
              <template v-else-if="column.dataIndex === 'operation'">
                <div class="editable-row-operations">
                  <template v-if="editableData[record.id]">
                    <CheckOutlined class="row-confirm-icon" style="color: #369fff; margin-left: 8px"
                      @click="rowConfirm(record)" title="保存" />
                    <CloseOutlined class="row-discard-icon" style="color: red; margin-left: 8px"
                      @click="rowDiscard(record)" title="取消" />
                  </template>
                  <template v-else>
                    <EditOutlined class="row-edit-icon" style="color:#369FFF;font-size:16px;cursor:pointer"
                      @click.stop="startEditRow(record)" @dblclick.stop title="编辑" />
                    <DeleteOutlined class="row-delete-icon"
                      style="color:#369FFF;font-size:16px;margin-left:8px;cursor:pointer" @click.stop="remove(record)"
                      @dblclick.stop title="取消选择" />
                  </template>
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
    <!-- 正常模式：操作按钮排；编辑模式：隐藏（底部只剩 取消/保存） -->
    <template v-slot:leftBottomBtn v-if="!isEditMode">
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
import { formatCellText } from "@/components/table/cellText";
import RulesDropdown from "@/components/Dropdown/rulesDropdown.vue";
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
  EditOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import { createVNode } from "vue";
import { deleteEntryInfoByID } from "@/http/api/workbench.js";
import { forbiddenEntryInfo, updateEntryInfo } from "@/http/api/entryManage.js";
import { updateUserPartiality } from "@/http/api/userPartiality";
import commonParam, { entryParams, entryAllCols, entryPresets } from "@/constants/commonParam.js";
import { pageChange as pageChangeUtil } from "@/utils/selectionUtils";
import {
  cancelEdit,
  getMethods,
  onEditableCellInput,
  openSetEdit,
} from "@/utils/validationUtils.js";
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
    EditOutlined,
    ExportButton,
    EntryStateBadge,
    TransStateBadge,
    TableCellTextArea,
    CellOverflowTooltip,
    CreateVersionForm,
    ExamineTaskForm,
    WriteBackForm,
    PreTranslateForm,
    RulesDropdown,
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
      modalWidth: "90%",
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
      rulesOptions: commonParam.rulesOptions.map((item) => ({ ...item })),

      // ===== 行编辑态（复用工作台 validationUtils 机制） =====
      editableData: {},
      rules: {},
      cellErrors: {},
      preTranslateActive: false, // 编辑模式：底部切换为 取消/保存
      preTranslateSnapshot: null, // 取消恢复依据
      preTranslateLoading: false,
      manualEditActive: false, // 非预翻译行编辑会话
      manualEditSnapshot: null,
      // 保存对比快照时需要检查的列（entry 各语种）
      fieldsNeedSave: ["entry", ...commonParam.langValList],
    };
  },

  created() {
    this.product = this.currentProduct;
  },
  computed: {
    // 可编辑语种列（固定为全部语种列）
    editableTextColumns() {
      return commonParam.langValList;
    },
    // 任意编辑会话中，底部仅保留取消/保存
    isEditMode() {
      return this.preTranslateActive || this.manualEditActive;
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
            normalWidth: 200,
            needFilter: false,
            lockCellSize: true,
          });
        }
      },
      immediate: false, // 不立即执行
    },
    rulesOptions: {
      deep: true,
      handler() {
        this.cellErrors = {};
      },
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
      if (this.manualEditSnapshot) {
        this.manualEditSnapshot = this.manualEditSnapshot.filter(
          (item) => item.id != record.id
        );
      }
    },
    handleClose() {
      if (this.isEditMode) {
        Modal.confirm({
          title: this.preTranslateActive ? "是否取消预翻译?" : "是否取消编辑?",
          icon: createVNode(ExclamationCircleOutlined),
          content: this.preTranslateActive
            ? "取消后，所有词条的预翻译结果将被丢弃"
            : "取消后，本次未保存的修改将被丢弃",
          okText: "是",
          cancelText: "否",
          style: { top: "30%" },
          onOk: () => {
            if (this.preTranslateActive) this.preTranslateCancel();
            else this.cancelManualEdits();
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
      if (this.manualEditActive) {
        await this.saveManualEdits();
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
      } else if (this.manualEditActive) {
        this.cancelManualEdits();
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
          await execute(this, {
            ...config,
            verifyMethods: getMethods(this),
          });
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
        const remainingIds = new Set(result.remaining.map((record) => record.id));
        this.$emit(
          "update:selectedRows",
          this.selectedRows.filter((record) => remainingIds.has(record.id))
        );
        this.$emit(
          "update:selectedRowKeys",
          this.selectedRowKeys.filter((id) => remainingIds.has(id))
        );
        this.$emit("update:dataSource", result.remaining);
        this.$emit("refresh");
      }
      // savedRecords 为空且 remaining 为空数组外的情况（无改动）：无操作
    },

    // 编辑单元格输入（onEditableCellInput 写 editableData + 清红字）
    onCellInput(value, record, column) {
      onEditableCellInput(this, record.id, column.dataIndex, value);
    },

    /** 普通单元格文本格式化（供 CellOverflowTooltip 使用） */
    formatCellText,

    /**
     * 浏览态进入行编辑：生成独立编辑副本并配置语种校验规则。
     * @param {Object} record - 当前行数据
     * @returns {Promise<boolean>} 是否成功进入编辑态
     */
    async startEditRow(record) {
      if (this.editableData[record.id]) return false;
      if (!this.preTranslateActive && !this.manualEditActive) {
        this.manualEditSnapshot = cloneDeep(this.dataSource);
        this.manualEditActive = true;
      }
      await openSetEdit(record, this.editableTextColumns, this);
      return true;
    },

    /**
     * 判断双击目标是否属于交互控件。
     * @param {EventTarget} target - 双击事件目标
     * @returns {boolean} 是否命中交互控件
     */
    isInteractiveRowTarget(target) {
      if (!target || typeof target.closest !== "function") return false;
      return !!target.closest(
        "button, input, textarea, select, a, .ant-btn, .ant-select, .ant-checkbox-wrapper, .ant-input, .editable-row-operations"
      );
    },

    /**
     * 双击行进入编辑态；交互控件和已编辑行不重复初始化。
     * @param {Object} record - 当前行数据
     * @param {MouseEvent} event - 双击事件
     * @returns {Promise<void>}
     */
    async handleRowDblclick(record, event) {
      if (this.editableData[record.id]) return;
      if (this.isInteractiveRowTarget(event?.target)) return;
      await this.startEditRow(record);
    },

    /**
     * 为表格行绑定双击编辑事件。
     * @param {Object} record - 当前行数据
     * @returns {Object} Ant Design Vue 行事件配置
     */
    customRow(record) {
      return {
        onDblclick: (event) => this.handleRowDblclick(record, event),
      };
    },

    /**
     * 结束普通编辑会话并清理临时状态。
     * @returns {void}
     */
    finishManualEdit() {
      this.manualEditActive = false;
      this.manualEditSnapshot = null;
      this.editableData = {};
      this.rules = {};
      this.cellErrors = {};
    },

    /**
     * 取消普通编辑：恢复进入编辑前的 dataSource 快照。
     * @returns {void}
     */
    cancelManualEdits() {
      const restored = this.manualEditSnapshot
        ? cloneDeep(this.manualEditSnapshot)
        : this.dataSource;
      this.$emit("update:dataSource", restored);
      this.finishManualEdit();
    },

    /**
     * 保存普通编辑：校验并提交编辑行，逐条落库，但保留已选词条。
     * @returns {Promise<void>}
     */
    async saveManualEdits() {
      const result = await withLoading(async () => {
        let allPassed = true;
        for (const record of this.dataSource) {
          if (!this.editableData[record.id]) continue;
          const ok = await confirmRow(this, record);
          if (!ok) allPassed = false;
        }
        if (!allPassed) {
          message.warning("存在未通过校验的译文，已标红，请修正后再保存。");
          return { allPassed: false, failedCount: 0 };
        }

        const beforeById = new Map(
          (this.manualEditSnapshot || []).map((record) => [record.id, record])
        );
        const changedRecords = this.dataSource.filter((record) => {
          const before = beforeById.get(record.id);
          if (!before) return true;
          return this.fieldsNeedSave.some(
            (col) => (record[col] ?? "") !== (before[col] ?? "")
          );
        });

        if (changedRecords.length === 0) {
          message.info("没有需要保存的改动。");
          return { allPassed: true, failedCount: 0 };
        }

        const saveResults = await Promise.allSettled(
          changedRecords.map((record) =>
            updateEntryInfo(record, { notes: "编辑词条" }).then(() => record)
          )
        );
        const savedRecords = saveResults
          .filter((item) => item.status === "fulfilled")
          .map((item) => item.value);
        const failedCount = saveResults.length - savedRecords.length;

        if (savedRecords.length > 0) {
          const savedIds = new Set(savedRecords.map((record) => record.id));
          this.manualEditSnapshot = (this.manualEditSnapshot || []).map((item) => {
            if (!savedIds.has(item.id)) return item;
            const current = this.dataSource.find((record) => record.id === item.id);
            return current ? cloneDeep(current) : item;
          });
          message.success(`已保存 ${savedRecords.length} 条词条。`);
        }
        if (failedCount > 0) {
          message.error(`有 ${failedCount} 条词条保存失败，已保留在列表中。`);
        }
        return { allPassed: true, failedCount };
      });

      if (result?.allPassed && result.failedCount === 0) {
        this.finishManualEdit();
      }
    },

    // 行内 ✓：校验并提交该行（不落库，等底部保存）
    async rowConfirm(record) {
      await confirmRow(this, record);
    },
    // 行内 ×：丢弃该行编辑内容
    rowDiscard(record) {
      if (this.preTranslateActive) {
        discardRow(this, record);
        return;
      }
      const before = this.manualEditSnapshot?.find((item) => item.id === record.id);
      const editRow = this.editableData?.[record.id] || {};
      if (before) {
        const current = this.dataSource.find((item) => item.id === record.id);
        if (current) {
          for (const col of Object.keys(editRow)) {
            if (col === "id" || col === "children") continue;
            current[col] = before[col] ?? "";
          }
        }
      }
      cancelEdit(this, record.id);
      if (this.cellErrors[record.id]) delete this.cellErrors[record.id];
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

.selected-entry-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
