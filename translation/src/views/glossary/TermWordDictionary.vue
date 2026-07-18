<template>
  <div class="term-word-dict">
    <SearchBox>
      <template v-slot:form>
        <a-form
          layout="inline"
          :model="search"
          autocomplete="off"
          :label-col="labelCol"
        >
          <a-form-item :label="GLOSSARY_LABEL.word">
            <a-input
              v-model:value="search.word"
              :placeholder="GLOSSARY_PLACEHOLDER.word"
              allow-clear
              @pressEnter="onSearch"
            />
          </a-form-item>
          <a-form-item :label="GLOSSARY_LABEL.translate">
            <a-input
              v-model:value="search.translate"
              :placeholder="GLOSSARY_PLACEHOLDER.translate"
              allow-clear
              @pressEnter="onSearch"
            />
          </a-form-item>
          <a-form-item :label="GLOSSARY_LABEL.translateType">
            <a-select
              v-model:value="search.targetLang"
              :placeholder="GLOSSARY_PLACEHOLDER.translateType"
              :options="translateTypes"
              :fieldNames="{ label: 'name', value: 'name' }"
              allow-clear
            />
          </a-form-item>
          <a-form-item :label="GLOSSARY_LABEL.translateState">
            <TransStateSelect
              :translateState="search.status"
              @update:translateState="search.status = $event"
              :placeholder="GLOSSARY_PLACEHOLDER.translateState"
            />
          </a-form-item>
          <a-form-item :label="GLOSSARY_LABEL.visualRange">
            <a-select
              v-model:value="search.department"
              :placeholder="GLOSSARY_PLACEHOLDER.visualRange"
              :options="departmentOptions"
              allow-clear
            />
          </a-form-item>
          <a-form-item :label="GLOSSARY_LABEL.hasAbbr">
            <a-select
              v-model:value="search.hasAbbr"
              :placeholder="GLOSSARY_PLACEHOLDER.yesNo"
              :options="yesNoOptions"
              allow-clear
            />
          </a-form-item>
          <a-form-item :label="GLOSSARY_LABEL.useLlm">
            <a-select
              v-model:value="search.useLlm"
              :placeholder="GLOSSARY_PLACEHOLDER.yesNo"
              :options="yesNoOptions"
              allow-clear
            />
          </a-form-item>
          <a-form-item :label="GLOSSARY_LABEL.wordRegex">
            <a-select
              v-model:value="search.wordRegex"
              :placeholder="GLOSSARY_PLACEHOLDER.wordRegex"
              :options="wordRegexOptions"
              show-search
              allow-clear
              @search="onWordRegexSearch"
            />
          </a-form-item>
          <a-form-item :label="GLOSSARY_LABEL.translateRegex">
            <a-select
              v-model:value="search.translateRegex"
              :placeholder="GLOSSARY_PLACEHOLDER.translateRegex"
              :options="translateRegexOptions"
              show-search
              allow-clear
              @search="onTranslateRegexSearch"
            />
          </a-form-item>
        </a-form>
      </template>
      <template v-slot:operate>
        <GlossarySearchOperate
          :search="search"
          :currentPage="pagination.current"
          @reset="onResetData"
          @search="onSearch"
        />
      </template>
    </SearchBox>

    <div class="list-shell">
      <div class="list-shell__head">
        <div class="list-shell__title"><span>术语字典：</span></div>
        <div class="list-shell__toolbar">
          <div class="toolbar">
            <BatchSelectButton
              size="middle"
              selectedButtonText="已选词片"
              :columns="columns"
              :dataSource="dataSource"
              :getSearch="fetchList"
              :hideModal="true"
              :selectAllFn="selectAllTermWords"
              v-model:search="search"
              v-model:lastSearch="lastSearch"
              v-model:loading="loading"
              v-model:selectEntry="selectEntry"
              v-model:selectedRows="selectedRows"
              v-model:selectedRowKeys="selectedRowKeys"
              v-model:batchSelectFlag="batchSelectFlag"
              v-model:batchSelectVisible="batchSelectVisible"
            />
            <a-button type="primary" @click="openCreate">
              <template #icon><PlusOutlined /></template>
              新增
            </a-button>
            <a-dropdown>
              <template #overlay>
                <a-menu class="term-word-more-menu">
                  <a-menu-item key="tpl">
                    <a-button
                      type="primary"
                      ghost
                      block
                      size="small"
                      @click="onDownloadTemplate"
                    >
                      下载模板
                    </a-button>
                  </a-menu-item>
                  <a-menu-item key="notes">
                    <a-tooltip
                      placement="left"
                      title="读取《注意事项清单》的「通用术语」sheet，生成标准导入 Excel；不会直接写库，需再点「导入」"
                    >
                      <a-upload
                        :show-upload-list="false"
                        :before-upload="onAdaptNotesFile"
                        accept=".xlsx,.xls"
                        class="more-upload"
                      >
                        <a-button type="primary" ghost block size="small" :loading="adapting">
                          从注意事项生成模板
                        </a-button>
                      </a-upload>
                    </a-tooltip>
                  </a-menu-item>
                  <a-menu-item key="import">
                    <a-upload
                      :show-upload-list="false"
                      :before-upload="onImportFile"
                      accept=".xlsx,.xls"
                      class="more-upload"
                    >
                      <a-button type="primary" ghost block size="small" :loading="importing">
                        导入
                      </a-button>
                    </a-upload>
                  </a-menu-item>
                </a-menu>
              </template>
              <a-button type="primary" ghost>
                更多操作
                <DownOutlined />
              </a-button>
            </a-dropdown>
            <ColumnFilter
              v-model="checkedColumn"
              :columns="columnSettingsList"
              :overlay-style="overlayStyle"
              button-size="middle"
              col-pref-name="colPref-termWord-v2"
              :normal-width="150"
              :need-filter="false"
              @change="syncColumnsFromPref"
            />
          </div>
        </div>
      </div>
      <div ref="tableWrapperRef" class="list-shell__body table-wrapper">
        <a-table
          bordered
          class="ant-table-striped"
          size="middle"
          row-key="id"
          :columns="columns"
          :data-source="dataSource"
          :loading="loading"
          :pagination="false"
          :scroll="{ y: tableScrollY }"
          :customRow="customRow"
          :row-selection="
            batchSelectFlag
              ? {
                  selectedRowKeys,
                  onChange: handleSelectChange,
                  onSelect: handleSelect,
                  onSelectAll: handleSelectAll,
                }
              : null
          "
        >
          <template #headerCell="{ column }">
            <template v-if="isField(column, 'use_llm')">
              <span :title="USE_LLM_TIP">走LLM <span class="tip-mark">ⓘ</span></span>
            </template>
            <template v-else>{{ column.title }}</template>
          </template>
          <template #bodyCell="{ column, record, text }">
            <template v-if="isEditing(record)">
              <template v-if="isField(column, 'word')">
                <a-input
                  v-model:value="editableData[record.id].word"
                  @click.stop
                  @pressEnter="saveRow(record)"
                />
              </template>
              <template v-else-if="isField(column, 'translate')">
                <a-input
                  v-model:value="editableData[record.id].translate"
                  @click.stop
                  @pressEnter="saveRow(record)"
                />
              </template>
              <template v-else-if="isField(column, 'target_lang')">
                <a-select
                  v-model:value="editableData[record.id].target_lang"
                  style="width: 100%"
                  :options="translateTypes"
                  :fieldNames="{ label: 'name', value: 'name' }"
                  allow-clear
                  @click.stop
                />
              </template>
              <template v-else-if="isField(column, 'department')">
                <a-select
                  v-model:value="editableData[record.id].department"
                  style="width: 100%"
                  :options="departmentOptions"
                  allow-clear
                  @click.stop
                />
              </template>
              <template v-else-if="isField(column, 'comment')">
                <a-input
                  v-model:value="editableData[record.id].comment"
                  @click.stop
                  @pressEnter="saveRow(record)"
                />
              </template>
              <template v-else-if="isField(column, 'category')">
                <a-input
                  v-model:value="editableData[record.id].category"
                  @click.stop
                  @pressEnter="saveRow(record)"
                />
              </template>
              <template v-else-if="isField(column, 'abbr')">
                <a-input
                  v-model:value="editableData[record.id].abbr"
                  @click.stop
                  @pressEnter="saveRow(record)"
                />
              </template>
              <template v-else-if="isField(column, 'use_llm')">
                <a-switch
                  v-model:checked="editableData[record.id].use_llm"
                  checked-children="是"
                  un-checked-children="否"
                  @click.stop
                />
              </template>
              <template v-else-if="isField(column, 'usage_notes')">
                <a-textarea
                  v-model:value="editableData[record.id].usage_notes"
                  :rows="3"
                  @click.stop
                />
              </template>
              <template v-else-if="isField(column, 'status')">
                <TransStateSelect
                  :translateState="editableData[record.id].status"
                  @update:translateState="editableData[record.id].status = $event"
                  :style="'width: 100%'"
                  :placeholder="GLOSSARY_PLACEHOLDER.translateState"
                />
              </template>
              <template v-else-if="isField(column, 'operation')">
                <OperationCellOverflow :inline-visible-count="2">
                  <OpItem label="保存" @click.stop="saveRow(record)" />
                  <OpItem label="取消" @click.stop="cancelEdit(record)" />
                </OperationCellOverflow>
              </template>
              <template v-else>{{ text }}</template>
            </template>
            <template v-else>
              <template v-if="isField(column, 'status')">
                <TransStateBadge :translateState="record.status" />
              </template>
              <template v-else-if="isField(column, 'use_llm')">
                <span :title="USE_LLM_TIP">{{ record.use_llm ? "是" : "否" }}</span>
              </template>
              <template v-else-if="isField(column, 'usage_notes')">
                <SpanByTipsFill
                  :content="record.usage_notes || ''"
                  :max-width="column.width"
                  theme="dark"
                  copyable
                  always-tip
                />
              </template>
              <template v-else-if="isField(column, 'operation')">
                <OperationCellOverflow :inline-visible-count="2">
                  <OpItem label="编辑" @click.stop="startEdit(record)" />
                  <OpItem
                    v-if="String(record.status) === '1'"
                    label="通过"
                    type="success"
                    @click.stop="onReviewOne(record, 'approved')"
                  />
                  <OpItem
                    v-if="String(record.status) === '1'"
                    label="驳回"
                    type="warning"
                    @click.stop="onReviewOne(record, 'rejected')"
                  />
                  <OpItem
                    label="删除"
                    type="danger"
                    @click.stop="onDeleteOne(record)"
                  />
                </OperationCellOverflow>
              </template>
            </template>
          </template>
        </a-table>
      </div>
      <div class="list-shell__pagination">
        <a-pagination
          v-model:current="pagination.current"
          v-model:pageSize="pagination.pageSize"
          :total="pagination.total"
          show-size-changer
          :show-total="(t) => `共 ${t} 条`"
          @change="onPageChange"
          @showSizeChange="onPageChange"
        />
      </div>
    </div>

    <TermWordSelectedModal
      :visible="batchSelectVisible"
      :dataSource="selectEntry"
      :selectedRows="selectedRows"
      :selectedRowKeys="selectedRowKeys"
      @update:dataSource="selectEntry = $event"
      @update:selectedRows="selectedRows = $event"
      @update:selectedRowKeys="selectedRowKeys = $event"
      @close="batchSelectVisible = false"
      @cancelSelect="cancelBatchSelect"
      @refresh="fetchList"
    />

    <a-modal
      v-model:visible="modalVisible"
      title="新增词片"
      :confirmLoading="modalSaving"
      destroyOnClose
      @ok="onCreateOk"
      @cancel="modalVisible = false"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item :label="GLOSSARY_LABEL.word" required>
          <a-input
            v-model:value="form.word"
            :placeholder="GLOSSARY_PLACEHOLDER.word"
            @pressEnter="onCreateOk"
          />
        </a-form-item>
        <a-form-item :label="GLOSSARY_LABEL.translate" required>
          <a-input
            v-model:value="form.translate"
            :placeholder="GLOSSARY_PLACEHOLDER.translate"
            @pressEnter="onCreateOk"
          />
        </a-form-item>
        <a-form-item :label="GLOSSARY_LABEL.translateType" required>
          <a-select
            v-model:value="form.target_lang"
            :placeholder="GLOSSARY_PLACEHOLDER.translateType"
            :options="translateTypes"
            :fieldNames="{ label: 'name', value: 'name' }"
            allow-clear
          />
        </a-form-item>
        <a-form-item :label="GLOSSARY_LABEL.visualRange">
          <a-select
            v-model:value="form.department"
            :placeholder="GLOSSARY_PLACEHOLDER.visualRange"
            :options="departmentOptions"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="comment">
          <a-input
            v-model:value="form.comment"
            placeholder="消歧 comment（可选）"
            @pressEnter="onCreateOk"
          />
        </a-form-item>
        <a-form-item :label="GLOSSARY_LABEL.domain">
          <a-input v-model:value="form.category" :placeholder="GLOSSARY_PLACEHOLDER.domain" />
        </a-form-item>
        <a-form-item :label="GLOSSARY_LABEL.abbr">
          <a-input v-model:value="form.abbr" :placeholder="GLOSSARY_PLACEHOLDER.abbr" />
        </a-form-item>
        <a-form-item :label="GLOSSARY_LABEL.useLlm">
          <a-switch
            v-model:checked="form.use_llm"
            checked-children="是"
            un-checked-children="否"
          />
          <div class="form-tip">{{ USE_LLM_TIP }}</div>
        </a-form-item>
        <a-form-item :label="GLOSSARY_LABEL.notes">
          <a-textarea
            v-model:value="form.usage_notes"
            :rows="5"
            :placeholder="GLOSSARY_PLACEHOLDER.notes"
          />
        </a-form-item>
        <a-form-item :label="GLOSSARY_LABEL.translateState" required>
          <TransStateSelect
            :translateState="form.status"
            @update:translateState="form.status = $event"
            :style="'width: 100%'"
            :placeholder="GLOSSARY_PLACEHOLDER.translateState"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { ref } from "vue";
import { message, Modal } from "ant-design-vue";
import { PlusOutlined, DownOutlined } from "@ant-design/icons-vue";
import { cloneDeep } from "lodash-es";
import SearchBox from "@/components/search/searchBox.vue";
import BatchSelectButton from "@/components/Button/batchSelectButton.vue";
import ColumnFilter from "@/components/ColumnFilter/ColumnFilter.vue";
import {
  applyTable,
  syncColumnsFromPref as applyTableColumnsFromPref,
  columnFilterOverlayStyle,
} from "@/components/ColumnFilter";
import {
  OpItem,
  OperationCellOverflow,
} from "@/components/OperationColumn";
import { useTableBodyHeight } from "@/composables/useTableBodyHeight";
import TransStateSelect from "@/components/select/transStateSelect.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import GlossarySearchOperate from "@/views/glossary/shared/GlossarySearchOperate.vue";
import {
  GLOSSARY_LABEL,
  GLOSSARY_PLACEHOLDER,
  USE_LLM_TIP,
} from "@/views/glossary/shared/glossaryQueryLabels.js";
import TermWordSelectedModal from "@/components/terminologyAgent/TermWordSelectedModal.vue";
import SpanByTipsFill from "@/components/SpanByTips/SpanByTipsFill/index.vue";
import {
  listTermWords,
  createTermWord,
  updateTermWord,
  deleteTermWord,
  downloadTermWordTemplate,
  importTermWords,
  adaptNotesToTermWordRows,
  exportTermWordRows,
  batchReviewTermWords,
} from "@/http/api/terminologyAgent";
import { getLanguage } from "@/http/api/translate";
import {
  onSelectChange,
  onSelect,
  onSelectAll,
  applyBatchSelectAll,
  pageChange,
} from "@/utils/selectionUtils";
import { fetchAllByPaging } from "@/utils/fetchAllByPaging";
import { downloadBlobResponse } from "@/utils/fileUtils";
import commonParam, {
  termWordAllCols,
  termWordPresets,
} from "@/constants/commonParam.js";
import { TERM_WORD_REGEX_PRESETS } from "@/constants/termWordRegexPresets.js";
import {
  appendTypedOption,
  clonePresetOptions,
} from "@/utils/searchableSelectOptions";

/**
 * @returns {{
 *   word: string,
 *   translate: string,
 *   targetLang: undefined,
 *   department: undefined,
 *   status: undefined,
 *   hasAbbr: undefined,
 *   useLlm: undefined,
 *   wordRegex: undefined,
 *   translateRegex: undefined,
 * }}
 */
function emptySearch() {
  return {
    word: "",
    translate: "",
    targetLang: undefined,
    department: undefined,
    status: undefined,
    hasAbbr: undefined,
    useLlm: undefined,
    wordRegex: undefined,
    translateRegex: undefined,
  };
}

const TERM_WORD_STATUS_KEY = "termWordSearchStatus";

/**
 * @returns {Object}
 */
function emptyForm() {
  return {
    word: "",
    translate: "",
    target_lang: undefined,
    department: undefined,
    comment: "",
    category: "",
    abbr: "",
    use_llm: false,
    usage_notes: "",
    status: "1",
  };
}

export default {
  name: "TermWordDictionary",
  components: {
    SearchBox,
    BatchSelectButton,
    ColumnFilter,
    OpItem,
    OperationCellOverflow,
    TransStateSelect,
    TransStateBadge,
    GlossarySearchOperate,
    TermWordSelectedModal,
    SpanByTipsFill,
    PlusOutlined,
    DownOutlined,
  },
  props: {
    /** 父级导入后指定初始翻译状态，优先于默认「已审核」 */
    bootstrapStatus: { type: String, default: null },
  },
  setup() {
    const tableWrapperRef = ref(null);
    const { tableScrollY, syncHeight } = useTableBodyHeight(tableWrapperRef, {
      subtractHeader: true,
      minHeight: 120,
      headerFallback: 39,
    });
    return {
      tableWrapperRef,
      tableScrollY,
      syncTableHeight: syncHeight,
    };
  },
  data() {
    // 默认不选状态；仅父级导入引导时带 bootstrapStatus（如待审核）
    const boot =
      this.bootstrapStatus === "0" ||
      this.bootstrapStatus === "1" ||
      this.bootstrapStatus === "2" ||
      this.bootstrapStatus === "3"
        ? this.bootstrapStatus
        : null;
    const search = emptySearch();
    if (boot) search.status = boot;
    return {
      GLOSSARY_LABEL,
      GLOSSARY_PLACEHOLDER,
      USE_LLM_TIP,
      labelCol: { style: { width: "84px" } },
      overlayStyle: columnFilterOverlayStyle,
      yesNoOptions: [
        { label: "是", value: true },
        { label: "否", value: false },
      ],
      wordRegexOptions: clonePresetOptions(TERM_WORD_REGEX_PRESETS),
      wordRegexOptionsBase: clonePresetOptions(TERM_WORD_REGEX_PRESETS),
      translateRegexOptions: clonePresetOptions(TERM_WORD_REGEX_PRESETS),
      translateRegexOptionsBase: clonePresetOptions(TERM_WORD_REGEX_PRESETS),
      search,
      lastSearch: {},
      /** 忽略过期 list 响应，避免与 bootstrap 切筛竞态 */
      _fetchSeq: 0,
      departmentOptions: Object.values(commonParam.departmentMap || {}).map((d) => ({
        label: d.label,
        value: d.label,
      })),
      translateTypes: [],
      columns: [],
      columnSettingsList: [],
      checkedColumn: [],
      dataSource: [],
      loading: false,
      importing: false,
      adapting: false,
      pagination: {
        current: 1,
        pageSize: 20,
        total: 0,
        showSizeChanger: true,
        showTotal: (t) => `共 ${t} 条`,
      },
      batchSelectFlag: false,
      batchSelectVisible: false,
      selectEntry: [],
      selectedRows: [],
      selectedRowKeys: [],
      editableData: {},
      savingRowId: null,
      modalVisible: false,
      modalSaving: false,
      form: emptyForm(),
    };
  },
  mounted() {
    applyTable(this, {
      allCols: termWordAllCols,
      preset: termWordPresets.termWord,
      ctx: { pagination: this.pagination },
      colPrefName: "colPref-termWord-v2",
      normalWidth: 150,
      needFilter: false,
    });
    this.loadTranslateTypes();
    this.fetchList();
  },
  methods: {
    /**
     * 判断列是否匹配字段名
     * @param {{ dataIndex?: string, colValue?: string }} column
     * @param {string} name
     * @returns {boolean}
     */
    isField(column, name) {
      return column.dataIndex === name || column.colValue === name;
    },
    /**
     * 当前筛选 → list API params
     * @returns {Object}
     */
    buildListParams() {
      return {
        word: this.search.word || undefined,
        translate: this.search.translate || undefined,
        targetLang: this.search.targetLang || undefined,
        department: this.search.department || undefined,
        status: this.search.status || undefined,
        hasAbbr:
          this.search.hasAbbr === true || this.search.hasAbbr === false
            ? this.search.hasAbbr
            : undefined,
        useLlm:
          this.search.useLlm === true || this.search.useLlm === false
            ? this.search.useLlm
            : undefined,
        wordRegex: this.search.wordRegex || undefined,
        translateRegex: this.search.translateRegex || undefined,
      };
    },
    onWordRegexSearch(value) {
      this.wordRegexOptions = appendTypedOption(
        this.wordRegexOptionsBase,
        value
      );
    },
    onTranslateRegexSearch(value) {
      this.translateRegexOptions = appendTypedOption(
        this.translateRegexOptionsBase,
        value
      );
    },
    /**
     * @param {{ id?: string }} record
     * @returns {boolean}
     */
    isEditing(record) {
      return !!this.editableData[record.id];
    },
    customRow(record) {
      return {
        onDblclick: (e) => {
          if (
            e?.target?.closest?.(
              ".ant-btn, .ant-select, input, textarea, .operation-column-op-item, .operation-buttons"
            )
          ) {
            return;
          }
          this.startEdit(record);
        },
      };
    },
    startEdit(record) {
      if (!record?.id) return;
      // 同时只编辑一行
      this.editableData = {
        [record.id]: cloneDeep({
          word: record.word || "",
          translate: record.translate || "",
          target_lang: record.target_lang || undefined,
          department: record.department || undefined,
          comment: record.comment || "",
          category: record.category || "",
          abbr: record.abbr || "",
          use_llm: !!record.use_llm,
          usage_notes: record.usage_notes || "",
          status: record.status || "1",
        }),
      };
    },
    cancelEdit(record) {
      if (!record?.id) return;
      const next = { ...this.editableData };
      delete next[record.id];
      this.editableData = next;
    },
    async saveRow(record) {
      const draft = this.editableData[record.id];
      if (!draft || this.savingRowId === record.id) return;
      const word = (draft.word || "").trim();
      const translate = (draft.translate || "").trim();
      const target_lang = (draft.target_lang || "").trim();
      if (!word || !translate || !target_lang) {
        message.warning("请填写词片、翻译、翻译类型");
        return;
      }
      if (!draft.status) {
        message.warning("请选择翻译状态");
        return;
      }
      this.savingRowId = record.id;
      try {
        await updateTermWord(record.id, {
          word,
          translate,
          target_lang,
          department: draft.department || null,
          comment: (draft.comment || "").trim(),
          category: (draft.category || "").trim() || null,
          abbr: (draft.abbr || "").trim() || null,
          use_llm: !!draft.use_llm,
          usage_notes: (draft.usage_notes || "").trim() || null,
          status: draft.status,
        });
        message.success("保存成功");
        this.cancelEdit(record);
        await this.fetchList();
      } catch (e) {
        message.error(e?.message || "保存失败");
      } finally {
        this.savingRowId = null;
      }
    },
    syncColumnsFromPref() {
      applyTableColumnsFromPref(this);
    },
    loadTranslateTypes() {
      getLanguage({})
        .then((res) => {
          this.translateTypes = res?.data?.list || [];
        })
        .catch(() => {
          this.translateTypes = [];
        });
    },
    onSearch() {
      this.pagination.current = 1;
      this.fetchList();
    },
    onResetData() {
      this.search = emptySearch();
      this.pagination.current = 1;
      this.fetchList();
    },
    /**
     * 外置分页变更
     * @param {number} page
     * @param {number} pageSize
     */
    onPageChange(page, pageSize) {
      pageChange(this, page, pageSize, this.fetchList);
    },
    handleSelectChange(selectedRowKeys, selectedRows) {
      onSelectChange(this, selectedRowKeys, selectedRows);
    },
    handleSelect(record, selected) {
      onSelect(this, record, selected, this.batchSelectFlag);
    },
    handleSelectAll(selected, selectedRows, changeRows) {
      onSelectAll(
        this,
        selected,
        selectedRows,
        changeRows,
        this.batchSelectFlag
      );
    },
    clearSelection() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.selectEntry = [];
    },
    cancelBatchSelect() {
      this.clearSelection();
      this.batchSelectFlag = false;
      this.batchSelectVisible = false;
    },
    /**
     * 「选择全部」：按当前筛选跨页拉全量词片并写入已选。
     * @returns {Promise<void>}
     */
    async selectAllTermWords() {
      this.loading = true;
      try {
        const rows = await fetchAllByPaging((page, pageSize) =>
          listTermWords({
            page,
            pageSize,
            ...this.buildListParams(),
          }).then((res) => {
            const data = res?.data ?? res;
            return {
              list: data?.list || [],
              total: data?.total ?? 0,
            };
          })
        );
        applyBatchSelectAll(this, rows);
      } catch (e) {
        message.error(e?.message || "选择全部失败");
      } finally {
        this.loading = false;
      }
    },
    async fetchList() {
      const seq = ++this._fetchSeq;
      this.loading = true;
      try {
        const res = await listTermWords({
          page: this.pagination.current,
          pageSize: this.pagination.pageSize,
          ...this.buildListParams(),
        });
        if (seq !== this._fetchSeq) return;
        const data = res?.data ?? res;
        this.dataSource = data?.list || [];
        this.pagination.total = data?.total ?? 0;
        this.lastSearch = { ...this.search };
        this.editableData = {};
        try {
          if (this.search.status) {
            sessionStorage.setItem(TERM_WORD_STATUS_KEY, this.search.status);
          } else {
            sessionStorage.removeItem(TERM_WORD_STATUS_KEY);
          }
        } catch {
          /* ignore */
        }
      } catch (e) {
        if (seq !== this._fetchSeq) return;
        this.dataSource = [];
        this.pagination.total = 0;
        message.error(e?.message || "加载术语字典失败（请确认 Agent 已启动）");
      } finally {
        if (seq === this._fetchSeq) this.loading = false;
        this.$nextTick(() => this.syncTableHeight?.());
      }
    },
    openCreate() {
      this.form = emptyForm();
      this.form.department = this.$store?.state?.user?.department || undefined;
      this.modalVisible = true;
    },
    async onCreateOk() {
      const word = (this.form.word || "").trim();
      const translate = (this.form.translate || "").trim();
      const target_lang = (this.form.target_lang || "").trim();
      if (!word || !translate || !target_lang) {
        message.warning("请填写词片、翻译、翻译类型");
        return;
      }
      if (!this.form.status) {
        message.warning("请选择翻译状态");
        return;
      }
      this.modalSaving = true;
      try {
        await createTermWord({
          word,
          translate,
          target_lang,
          department: this.form.department || null,
          comment: (this.form.comment || "").trim(),
          category: (this.form.category || "").trim() || null,
          abbr: (this.form.abbr || "").trim() || null,
          use_llm: !!this.form.use_llm,
          usage_notes: (this.form.usage_notes || "").trim() || null,
          status: this.form.status,
        });
        message.success("新增成功");
        this.modalVisible = false;
        await this.fetchList();
      } catch (e) {
        message.error(e?.message || "保存失败");
      } finally {
        this.modalSaving = false;
      }
    },
    /**
     * 下载导入模板（默认带样例）
     * @returns {Promise<void>}
     */
    async onDownloadTemplate() {
      try {
        const resp = await downloadTermWordTemplate(true);
        const saved = await downloadBlobResponse(
          resp,
          "term_word_import_sample.xlsx",
        );
        if (saved) message.success("模板已保存");
      } catch (e) {
        message.error(e?.message || "下载模板失败");
      }
    },
    /**
     * 导入标准 Excel
     * @param {File} file
     * @returns {boolean}
     */
    onImportFile(file) {
      this.runImport(file, false);
      return false;
    },
    /**
     * 注意事项清单适配下载
     * @param {File} file
     * @returns {boolean}
     */
    onAdaptNotesFile(file) {
      this.runAdaptNotes(file);
      return false;
    },
    /**
     * @param {File} file
     * @param {boolean} forcePending
     * @returns {Promise<void>}
     */
    async runImport(file, forcePending) {
      this.importing = true;
      try {
        const res = await importTermWords(file, {
          forcePendingWhenTranslated: forcePending,
        });
        const data = res?.data ?? res;
        message.success(
          `导入完成：新增 ${data?.created ?? 0}，跳过 ${data?.skipped ?? 0}`,
        );
        if (data?.parseErrors?.length) {
          message.warning(data.parseErrors.slice(0, 3).join("；"));
        }
        await this.fetchList();
      } catch (e) {
        message.error(e?.message || "导入失败");
      } finally {
        this.importing = false;
      }
    },
    /**
     * @param {File} file
     * @returns {Promise<void>}
     */
    async runAdaptNotes(file) {
      this.adapting = true;
      try {
        const res = await adaptNotesToTermWordRows(file, "英文");
        const list = res?.data?.list || [];
        if (!list.length) {
          message.warning("未解析到可导入词片（需有中文+英文）");
          return;
        }
        const resp = await exportTermWordRows(list, true);
        const saved = await downloadBlobResponse(
          resp,
          "term_word_from_notes.xlsx",
        );
        if (saved) {
          message.success(
            `已生成 ${list.length} 行标准模板（未写库），可再点「更多操作 → 导入」入库`,
          );
        }
      } catch (e) {
        message.error(e?.message || "适配失败");
      } finally {
        this.adapting = false;
      }
    },
    /**
     * 单行审阅：通过→已审核(3) / 驳回→审核不通过(2)
     * @param {{ id: string, word?: string, status?: string }} record
     * @param {"approved"|"rejected"} action
     * @returns {void}
     */
    onReviewOne(record, action) {
      if (String(record?.status) !== "1") {
        message.warning("仅待审核词片可审阅");
        return;
      }
      const approve = action === "approved";
      Modal.confirm({
        title: approve ? "确认通过该词片？" : "确认驳回该词片？",
        content: record.word,
        okType: approve ? "primary" : "danger",
        onOk: async () => {
          const res = await batchReviewTermWords([record.id], action);
          const data = res?.data ?? res;
          if ((data?.updated ?? 0) < 1) {
            message.warning("未更新（可能已非待审核）");
          } else {
            message.success(approve ? "已通过" : "已驳回");
          }
          await this.fetchList();
        },
      });
    },
    /**
     * @param {{ id: string, word?: string }} record
     * @returns {void}
     */
    onDeleteOne(record) {
      Modal.confirm({
        title: "确认删除该词片？",
        content: record.word,
        okType: "danger",
        onOk: async () => {
          await deleteTermWord(record.id);
          message.success("已删除");
          this.clearSelection();
          await this.fetchList();
        },
      });
    },
  },
};
</script>

<style scoped>
.term-word-dict {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}
.term-word-dict > :first-child {
  flex-shrink: 0;
}
.list-shell {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #dcdcdc;
  border-top: none;
  background: #fff;
}
.list-shell__head {
  flex-shrink: 0;
}
.list-shell__title {
  display: flex;
  height: 32px;
  padding: 8px 16px;
  align-items: center;
  background: #f3f3f3;
}
.list-shell__title span {
  color: rgba(0, 0, 0, 0.9);
  font-family: Microsoft YaHei, sans-serif;
  font-size: 14px;
  font-weight: 700;
  line-height: 22px;
}
.list-shell__toolbar {
  padding: 16px 16px 0;
  display: flex;
  justify-content: flex-end;
}
.list-shell__body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 8px 16px 0;
}
.list-shell__pagination {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px 16px;
}
.toolbar {
  margin-bottom: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}
.tip-mark {
  opacity: 0.55;
  font-size: 12px;
}
.form-tip {
  margin-top: 6px;
  color: #5b6b7c;
  font-size: 12px;
  line-height: 1.4;
}
.more-upload {
  display: block;
  width: 100%;
}
.more-upload :deep(.ant-upload) {
  display: block;
  width: 100%;
}
</style>
