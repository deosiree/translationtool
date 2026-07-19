<template>
  <CustomModal
    :modalWidth="'70%'"
    modalTitle="拆分结果 · 导入字典"
    :visible="visible"
    :showOk="false"
    :showCancel="false"
    @handleClose="handleClose"
    @handleOK="handleClose"
  >
    <div class="split-export">
      <p class="hint">
        已过滤无意义词片（如「与」）；默认批量 LLM 按整句语境补词片译法（走 LLM
        列仍为否，表示日后不必读注意事项）。导入后翻译状态为「待审核」。可双击行编辑词片/翻译，保存后再导入。
      </p>
      <div class="toolbar">
        <a-button size="small" @click="selectAll">全选</a-button>
        <a-button size="small" @click="clearSelect">取消勾选</a-button>
        <span class="count">已选 {{ selectedRowKeys.length }} / {{ dataSource.length }}</span>
      </div>
      <a-table
        size="middle"
        bordered
        row-key="rowKey"
        :columns="columns"
        :data-source="dataSource"
        :pagination="false"
        :scroll="{ y: '50vh', x: 900 }"
        :customRow="customRow"
        :row-selection="{
          selectedRowKeys,
          onChange: onSelectChange,
        }"
        :loading="loading"
      >
        <template #bodyCell="{ column, record, text }">
          <template v-if="isEditing(record)">
            <template v-if="column.dataIndex === 'word'">
              <a-input
                v-model:value="editableData[record.rowKey].word"
                @click.stop
                @pressEnter="saveRow(record)"
              />
            </template>
            <template v-else-if="column.dataIndex === 'translate'">
              <a-input
                v-model:value="editableData[record.rowKey].translate"
                @click.stop
                @pressEnter="saveRow(record)"
              />
            </template>
            <template v-else-if="column.dataIndex === 'operation'">
              <OperationCellOverflow :inline-visible-count="2">
                <OpItem label="保存" @click.stop="saveRow(record)" />
                <OpItem label="取消" @click.stop="cancelEdit(record)" />
              </OperationCellOverflow>
            </template>
            <template v-else-if="column.dataIndex === 'use_llm'">
              {{ record.use_llm ? "是" : "否" }}
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              <TransStateBadge translateState="1" />
            </template>
            <template v-else>{{ text }}</template>
          </template>
          <template v-else>
            <template v-if="column.dataIndex === 'use_llm'">
              {{ record.use_llm ? "是" : "否" }}
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              <TransStateBadge translateState="1" />
            </template>
            <template v-else-if="column.dataIndex === 'operation'">
              <OperationCellOverflow :inline-visible-count="1">
                <OpItem label="编辑" @click.stop="startEdit(record)" />
              </OperationCellOverflow>
            </template>
          </template>
        </template>
      </a-table>
    </div>
    <template v-slot:leftBottomBtn>
      <a-button @click="handleClose">关闭</a-button>
      <a-button
        type="primary"
        ghost
        :loading="exporting"
        :disabled="!selectedRowKeys.length"
        @click="onExport"
      >
        导出
      </a-button>
      <a-button
        type="primary"
        :loading="importing"
        :disabled="!selectedRowKeys.length"
        @click="onImport"
      >
        导入字典
      </a-button>
    </template>
  </CustomModal>
</template>

<script>
import CustomModal from "@/components/modal/index.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import {
  OpItem,
  OperationCellOverflow,
} from "@/components/OperationColumn";
import { message } from "ant-design-vue";
import {
  splitTermWordPreview,
  exportTermWordRows,
  importTermWordRows,
} from "@/http/api/terminologyAgent";
import { downloadBlobResponse } from "@/utils/fileUtils";

/**
 * 术语词典拆分结果弹窗：预览词片，可本地编辑后一键导入字典或导出 Excel。
 */
export default {
  name: "SplitExportModal",
  components: { CustomModal, TransStateBadge, OpItem, OperationCellOverflow },
  emits: ["close", "exported", "imported"],
  props: {
    visible: { type: Boolean, default: false },
    /** @type {{ entry: string, translate?: string, type?: string, target_lang?: string, visualRange?: string, department?: string, comment?: string }[]} */
    sourceItems: { type: Array, default: () => [] },
    /**
     * 预加载的拆分结果行（跳过 API 调用直接使用）
     * 格式：[{ word, translate?, target_lang, department? }]
     */
    preloadedRows: { type: Array, default: () => [] },
  },
  data() {
    return {
      loading: false,
      exporting: false,
      importing: false,
      dataSource: [],
      selectedRowKeys: [],
      /** @type {Record<string, { word: string, translate: string }>} */
      editableData: {},
      columns: [
        { title: "词片", dataIndex: "word", width: 140, ellipsis: true },
        { title: "翻译", dataIndex: "translate", width: 160, ellipsis: true },
        { title: "翻译类型", dataIndex: "target_lang", width: 100 },
        { title: "走LLM", dataIndex: "use_llm", width: 80 },
        { title: "翻译状态", dataIndex: "status", width: 100 },
        { title: "来源词条", dataIndex: "source_entry", width: 160, ellipsis: true },
        {
          title: "操作",
          dataIndex: "operation",
          width: 120,
          fixed: "right",
        },
      ],
    };
  },
  watch: {
    visible(val) {
      if (val) {
        if (this.preloadedRows && this.preloadedRows.length) {
          this.usePreloadedRows();
        } else {
          this.loadPreview();
        }
      } else {
        this.dataSource = [];
        this.selectedRowKeys = [];
        this.editableData = {};
      }
    },
  },
  methods: {
    /**
     * 关闭弹窗
     * @returns {void}
     */
    handleClose() {
      this.editableData = {};
      this.$emit("close");
    },
    /**
     * 勾选变化
     * @param {string[]} keys
     * @returns {void}
     */
    onSelectChange(keys) {
      this.selectedRowKeys = keys;
    },
    /**
     * 全选当前候选
     * @returns {void}
     */
    selectAll() {
      this.selectedRowKeys = this.dataSource.map((r) => r.rowKey);
    },
    /**
     * 清空勾选
     * @returns {void}
     */
    clearSelect() {
      this.selectedRowKeys = [];
    },
    /**
     * @param {{ rowKey?: string }} record
     * @returns {boolean}
     */
    isEditing(record) {
      return !!this.editableData[record?.rowKey];
    },
    /**
     * 是否存在未保存的行内编辑
     * @returns {boolean}
     */
    hasUnsavedEdit() {
      return Object.keys(this.editableData).length > 0;
    },
    /**
     * 双击行进入编辑（忽略按钮/输入控件）
     * @param {{ rowKey?: string }} record
     * @returns {{ onDblclick: (e: MouseEvent) => void }}
     */
    customRow(record) {
      return {
        onDblclick: (e) => {
          if (
            e?.target?.closest?.(
              ".ant-btn, .ant-select, input, textarea, .operation-column-op-item, .operation-buttons",
            )
          ) {
            return;
          }
          this.startEdit(record);
        },
      };
    },
    /**
     * 进入行编辑（同时只编辑一行）
     * @param {{ rowKey?: string, word?: string, translate?: string }} record
     * @returns {void}
     */
    startEdit(record) {
      if (!record?.rowKey) return;
      this.editableData = {
        [record.rowKey]: {
          word: record.word || "",
          translate: record.translate || "",
        },
      };
    },
    /**
     * 取消行编辑
     * @param {{ rowKey?: string }} record
     * @returns {void}
     */
    cancelEdit(record) {
      if (!record?.rowKey) return;
      const next = { ...this.editableData };
      delete next[record.rowKey];
      this.editableData = next;
    },
    /**
     * 将草稿写回本地 dataSource（不调后端）
     * @param {{ rowKey?: string }} record
     * @returns {void}
     */
    saveRow(record) {
      const draft = this.editableData[record?.rowKey];
      if (!draft) return;
      const word = (draft.word || "").trim();
      const translate = (draft.translate || "").trim();
      if (!word || !translate) {
        message.warning("请填写词片和翻译");
        return;
      }
      const row = this.dataSource.find((r) => r.rowKey === record.rowKey);
      if (!row) {
        this.cancelEdit(record);
        return;
      }
      row.word = word;
      row.translate = translate;
      this.cancelEdit(record);
      message.success("已保存");
    },
    /**
     * 调用 split-preview 加载候选词片
     * @returns {Promise<void>}
     */
    async loadPreview() {
      this.editableData = {};
      const items = (this.sourceItems || [])
        .map((row) => ({
          entry: row.entry || row.word || "",
          translate: row.translate || "",
          targetLang: row.type || row.target_lang || row.targetLang || "",
          department: row.visualRange || row.department || null,
          comment: row.comment || "",
        }))
        .filter((it) => it.entry && it.targetLang);
      if (!items.length) {
        message.warning("已选术语缺少词条或翻译类型，无法拆分");
        this.dataSource = [];
        return;
      }
      this.loading = true;
      try {
        const res = await splitTermWordPreview(items, { fillWithLlm: true });
        const list = res?.data?.list || [];
        this.dataSource = list.map((r, i) => ({
          ...r,
          rowKey: `${r.word || ""}_${r.target_lang || ""}_${i}`,
        }));
        this.selectedRowKeys = this.dataSource.map((r) => r.rowKey);
        if (!this.dataSource.length) {
          message.info("拆分后无带翻译的词片（无意义词已过滤）");
        }
      } catch (err) {
        message.error(err?.message || "拆分预览失败");
      } finally {
        this.loading = false;
      }
    },
    /**
     * 使用预加载的拆分结果行（跳过 API 调用）
     */
    usePreloadedRows() {
      this.editableData = {};
      const rows = this.preloadedRows || [];
      this.dataSource = rows.map((r, i) => ({
        word: r.word || "",
        translate: r.translate || "",
        target_lang: r.target_lang || "",
        department: r.department || null,
        comment: r.comment || "",
        use_llm: false,
        status: "1",
        rowKey: `${r.word || ""}_${r.target_lang || ""}_${i}`,
      }));
      this.selectedRowKeys = this.dataSource.map((r) => r.rowKey);
      this.loading = false;
    },
    /**
     * 勾选行 → 标准字典字段 payload
     * @returns {Object[]}
     */
    selectedPayload() {
      const rows = this.dataSource.filter((r) =>
        this.selectedRowKeys.includes(r.rowKey),
      );
      return rows.map(
        ({
          word,
          translate,
          target_lang,
          department,
          comment,
          category,
          abbr,
          use_llm,
          usage_notes,
        }) => ({
          word,
          translate,
          target_lang,
          department,
          comment: comment || "",
          category,
          abbr,
          use_llm: !!use_llm,
          usage_notes,
          status: "1",
        }),
      );
    },
    /**
     * 导出勾选词片为标准字典 Excel（翻译状态待审核）
     * @returns {Promise<void>}
     */
    async onExport() {
      if (this.hasUnsavedEdit()) {
        message.warning("请先保存或取消当前编辑");
        return;
      }
      const payload = this.selectedPayload();
      if (!payload.length) {
        message.warning("请勾选要导出的词片");
        return;
      }
      this.exporting = true;
      try {
        const resp = await exportTermWordRows(payload, true);
        const saved = await downloadBlobResponse(
          resp,
          "split_export_词典切分.xlsx",
        );
        if (saved) {
          message.success(`已导出 ${payload.length} 条词片`);
          this.$emit("exported", payload);
        }
      } catch (err) {
        message.error(err?.message || "导出失败");
      } finally {
        this.exporting = false;
      }
    },
    /**
     * 一键导入勾选词片到术语字典（待审核）；成功文案由父级统一 toast
     * @returns {Promise<void>}
     */
    async onImport() {
      if (this.hasUnsavedEdit()) {
        message.warning("请先保存或取消当前编辑");
        return;
      }
      const payload = this.selectedPayload();
      if (!payload.length) {
        message.warning("请勾选要导入的词片");
        return;
      }
      this.importing = true;
      try {
        const res = await importTermWordRows(payload, true);
        const data = res?.data ?? res;
        if (data?.skipDetails?.length) {
          message.warning(data.skipDetails.slice(0, 3).join("；"));
        }
        this.$emit("imported", data);
        this.handleClose();
      } catch (err) {
        message.error(err?.message || "导入失败");
      } finally {
        this.importing = false;
      }
    },
  },
};
</script>

<style scoped>
.split-export .hint {
  margin: 0 0 10px;
  color: #5b6b7c;
  font-size: 13px;
}
.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.count {
  color: #5b6b7c;
  font-size: 12px;
}
</style>
