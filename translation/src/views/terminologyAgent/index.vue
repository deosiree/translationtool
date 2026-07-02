<template>
  <div class="terminology-audit commonBox box" ref="box">
    <SearchBox ref="search" defaultTitleName="术语学习" :operate="true" @change="setTableHeight">
      <template #form>
        <a-form
          :model="search"
          name="term_audit_search"
          layout="inline"
          autocomplete="off"
          :label-col="labelCol"
        >
          <a-form-item label="词条" name="sourceText">
            <a-input
              v-model:value="search.sourceText"
              placeholder="请输入词条"
              allow-clear
              style="width: 186px"
            />
          </a-form-item>
          <a-form-item label="目标语种" name="targetLang">
            <a-select
              v-model:value="search.targetLang"
              style="width: 186px"
              placeholder="请选择目标语种"
              :options="translateTypes"
              :field-names="{ label: 'name', value: 'name' }"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="任务名称" name="taskName">
            <a-input
              v-model:value="search.taskName"
              placeholder="请输入任务名称"
              allow-clear
              style="width: 186px"
            />
          </a-form-item>
          <a-form-item label="产品名称" name="productName">
            <a-input
              v-model:value="search.productName"
              placeholder="请输入产品名称"
              allow-clear
              style="width: 186px"
            />
          </a-form-item>
          <a-form-item label="部门所属" name="department">
            <a-select
              v-model:value="search.department"
              style="width: 186px"
              placeholder="请选择部门所属"
              :options="visualRanges"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="置信度" name="confidenceMin" class="term-audit-confidence">
            <PercentRangeInput
              v-model:min="search.confidenceMin"
              v-model:max="search.confidenceMax"
            />
          </a-form-item>
          <a-form-item label="检索方式" name="retrievalMethod">
            <a-select
              v-model:value="search.retrievalMethod"
              style="width: 186px"
              placeholder="请选择检索方式"
              :options="retrievalMethodOptions"
              allow-clear
            />
          </a-form-item>
        </a-form>
      </template>
      <template #operate>
        <ResetButton
          :size="'middle'"
          :search="search"
          :currentPage="pagination.current"
          @resetData="onResetSearch"
        />
        <a-button type="primary" size="middle" @click="handleSearch">
          查询
        </a-button>
        <a-button size="middle" @click="fetchPendingAudits">
          刷新
        </a-button>
      </template>
    </SearchBox>

    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <template #operate>
        <div
          ref="button"
          style="margin-bottom: 8px; display: flex; gap: 10px; flex-wrap: wrap"
        >
          <BatchSelectButton
            :size="'middle'"
            :dataSource="dataSource"
            :getSearch="fetchPendingAudits"
            v-model:search="search"
            v-model:lastSearch="lastSearch"
            v-model:loading="loading"
            v-model:selectEntry="selectEntry"
            v-model:selectedRows="selectedRows"
            v-model:selectedRowKeys="selectedRowKeys"
            v-model:batchSelectFlag="batchSelectFlag"
            v-model:batchSelectVisible="batchSelectVisible"
            :selectAllFn="selectAllAudits"
            :hideModal="true"
            selectedButtonText="已选术语"
          />
          <ColumnFilter
            v-model="checkedColumn"
            :columns="columnSettingsList"
            :overlay-style="overlayStyle"
            button-size="middle"
            col-pref-name="colPref-termAudit"
            :normal-width="150"
            :need-filter="false"
            @change="syncColumnsFromPref"
          />
        </div>
      </template>

      <template #data>
        <div style="width: 100%; position: absolute">
          <a-table
            bordered
            class="ant-table-striped"
            :dataSource="dataSource"
            :columns="columns"
            :loading="loading"
            :pagination="false"
            :scroll="tableHeight"
            rowKey="id"
            ref="auditTable"
            @resizeColumn="handleResizeColumn"
            :row-selection="
              batchSelectFlag
                ? {
                    selectedRowKeys: selectedRowKeys,
                    onChange: onSelectChange,
                    onSelect: onSelect,
                    onSelectAll: onSelectAll,
                  }
                : null
            "
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'source_text'">
                <span v-text="formatEntryText(record.source_text)"></span>
              </template>

              <template v-if="column.dataIndex === 'suggested_translation'">
                <a-tag color="blue">{{
                  record.suggested_translation || "未生成"
                }}</a-tag>
              </template>

              <template v-if="column.dataIndex === 'confidence'">
                <a-tag :color="confidenceColor(record.confidence)">
                  {{ formatConfidence(record.confidence) }}
                </a-tag>
              </template>

              <template v-if="column.dataIndex === 'similar_terms'">
                <template
                  v-if="record.similar_terms && record.similar_terms.length"
                >
                  <a-popover trigger="click" placement="left">
                    <template #content>
                      <a-table
                        :columns="similarTermColumns"
                        :data-source="record.similar_terms"
                        :pagination="false"
                        size="small"
                        row-key="entry"
                        style="min-width: 380px"
                      >
                        <template #bodyCell="{ column: col, record: term }">
                          <template v-if="col.key === 'retrieval_source'">
                            {{ formatRetrievalSource(term.retrieval_source) }}
                          </template>
                        </template>
                      </a-table>
                    </template>
                    <a-button type="link" size="small">
                      {{ record.similar_terms.length }} 条
                    </a-button>
                  </a-popover>
                </template>
                <span v-else>-</span>
              </template>

              <template v-if="column.dataIndex === 'retrieval_method'">
                <a-tag
                  v-if="record._mock || record._local"
                  color="orange"
                  style="margin-right: 4px"
                >
                  本地 Mock
                </a-tag>
                {{ formatRetrievalMethod(record.retrieval_method) }}
              </template>

              <template v-if="column.dataIndex === 'llm_reasoning'">
                <a-tooltip :title="record.llm_reasoning">
                  <span class="reasoning-text">
                    {{ truncateText(record.llm_reasoning, 40) }}
                  </span>
                </a-tooltip>
              </template>

              <template v-if="column.dataIndex === 'created_at'">
                {{ formatDateTime(record.created_at) }}
              </template>

              <template v-if="column.dataIndex === 'is_new_term'">
                <a-tag :color="record.is_new_term ? 'green' : 'default'">
                  {{ record.is_new_term ? "是" : "否" }}
                </a-tag>
              </template>

              <template v-if="column.dataIndex === 'action'">
                <a-button
                  type="primary"
                  size="small"
                  style="margin-right: 8px"
                  :disabled="record.processing"
                  @click="handleReview(record, 'approved')"
                >
                  确认
                </a-button>
                <a-button
                  danger
                  size="small"
                  :disabled="record.processing"
                  @click="handleReview(record, 'rejected')"
                >
                  拒绝
                </a-button>
              </template>
            </template>

            <template #emptyText>
              <a-empty
                description="暂无待审核词条（工作台 Agent 预翻译低于阈值时会进入此队列）"
              />
            </template>
          </a-table>
        </div>
      </template>
    </DataBox>

    <div class="terminology-audit__pagination">
      <Pagination
        ref="pagination"
        :total="pagination.total"
        @pageChange="handlePageChange"
      />
    </div>

    <TermAuditSelectedModal
      :visible="batchSelectVisible"
      :dataSource="selectEntry"
      :selectedRows="selectedRows"
      :selectedRowKeys="selectedRowKeys"
      @update:dataSource="selectEntry = $event"
      @update:selectedRows="selectedRows = $event"
      @update:selectedRowKeys="selectedRowKeys = $event"
      @close="batchSelectVisible = false"
      @cancelSelect="cancelBatchSelect"
      @refresh="fetchPendingAudits"
    />
  </div>
</template>

<script>
/**
 * 术语学习 — Agent 预翻译待审核页。
 *
 * 布局对齐词条管理/术语库：SearchBox + DataBox + ColumnFilter + 批量选择。
 */
import {
  listPendingAudits,
  reviewTerm,
} from "@/http/api/terminologyAgent";
import {
  mergePendingAudits,
  removeLocalPendingAudit,
  formatRetrievalMethod,
  formatRetrievalSource,
  formatConfidence,
  formatEntryText,
  buildAuditListParams,
  extractAuditFilters,
  createDefaultAuditSearch,
  getRetrievalMethodOptions,
} from "@/utils/agentPendingAudits";
import { getLanguage } from "@/http/api/translate";
import ResetButton from "@/components/Button/resetButton.vue";
import commonParam from "@/constants/commonParam.js";
import { message } from "ant-design-vue";
import Pagination from "@/components/page/pagination.vue";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import BatchSelectButton from "@/components/Button/batchSelectButton.vue";
import ColumnFilter from "@/components/ColumnFilter/ColumnFilter.vue";
import TermAuditSelectedModal from "@/components/terminologyAgent/TermAuditSelectedModal.vue";
import PercentRangeInput from "@/components/terminologyAgent/PercentRangeInput.vue";
import {
  applyTable,
  syncColumnsFromPref as applyTableColumnsFromPref,
} from "@/components/ColumnFilter";
import {
  onSelectChange,
  onSelect,
  onSelectAll,
  pageChange,
} from "@/utils/selectionUtils";
import {
  setTableHeight,
  handleResizeColumn,
} from "@/utils/tableUtils";
import {
  termAuditAllCols,
  termAuditPresets,
  termAuditParams,
} from "@/constants/commonParam.js";

export default {
  name: "TerminologyAudit",
  components: {
    Pagination,
    SearchBox,
    DataBox,
    BatchSelectButton,
    ColumnFilter,
    TermAuditSelectedModal,
    PercentRangeInput,
    ResetButton,
  },
  data() {
    return {
      loading: false,
      dataSource: [],
      tableTitle: "待审核术语",
      dataHeight: 400,
      tableHeight: { x: "max-content", y: 0 },
      labelCol: termAuditParams.labelCol,
      search: createDefaultAuditSearch(),
      lastSearch: createDefaultAuditSearch(),
      translateTypes: [],
      visualRanges: Object.values(commonParam.departmentMap).map((dept) => ({
        label: dept.label,
        value: dept.label,
      })),
      retrievalMethodOptions: getRetrievalMethodOptions(),
      pagination: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      columns: [],
      columnSettingsList: [],
      checkedColumn: [],
      overlayStyle: termAuditParams.overlayStyle,
      batchSelectFlag: false,
      batchSelectVisible: false,
      selectedRowKeys: [],
      selectedRows: [],
      selectEntry: [],
      similarTermColumns: [
        { title: "词条", dataIndex: "entry", key: "entry", ellipsis: true },
        {
          title: "已有翻译",
          dataIndex: "translate",
          key: "translate",
          ellipsis: true,
        },
        {
          title: "来源",
          dataIndex: "retrieval_source",
          key: "retrieval_source",
          width: 80,
        },
      ],
    };
  },
  mounted() {
    const _this = this;
    this.loadTranslateTypes();
    this.$nextTick(() => {
      applyTable(this, {
        allCols: termAuditAllCols,
        preset: termAuditPresets.termAudit,
        ctx: { pagination: this.pagination },
        colPrefName: "colPref-termAudit",
        normalWidth: 150,
        needFilter: false,
      });
      this.setTableHeight();
      window.onresize = function () {
        _this.setTableHeight();
      };
      this.fetchPendingAudits();
    });
  },
  unmounted() {
    window.onresize = null;
  },
  methods: {
    handleResizeColumn,
    setTableHeight() {
      setTableHeight(this, 8, 150, 30);
    },
    syncColumnsFromPref() {
      applyTableColumnsFromPref(this);
    },
    formatRetrievalMethod,
    formatRetrievalSource,
    formatConfidence,
    formatEntryText,
    confidenceColor(confidence) {
      if (confidence == null) return "default";
      return Number(confidence) >= 0.8 ? "green" : "orange";
    },
    truncateText(text, maxLen) {
      if (!text) return "-";
      return text.length > maxLen ? `${text.substring(0, maxLen)}...` : text;
    },
    formatDateTime(value) {
      if (!value) return "-";
      if (typeof value === "string")
        return value.replace("T", " ").slice(0, 19);
      return String(value);
    },
    onSelectChange(selectedRowKeys, selectedRows) {
      onSelectChange(this, selectedRowKeys, selectedRows);
    },
    onSelect(record, selected) {
      onSelect(this, record, selected, this.batchSelectFlag);
    },
    onSelectAll(selected, selectedRows, changeRows) {
      onSelectAll(
        this,
        selected,
        selectedRows,
        changeRows,
        this.batchSelectFlag,
      );
    },
    cancelBatchSelect() {
      this.selectEntry = [];
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.batchSelectFlag = false;
      this.batchSelectVisible = false;
    },
    loadTranslateTypes() {
      getLanguage({})
        .then((res) => {
          const list = res?.data?.list;
          this.translateTypes = Array.isArray(list) ? list : [];
        })
        .catch((err) => {
          console.warn("[TerminologyAudit] getLanguage failed", err);
          this.translateTypes = [];
        });
    },
    onResetSearch(_newSearch, newPage) {
      this.search = createDefaultAuditSearch();
      this.pagination.current = newPage;
      if (this.$refs.pagination) {
        this.$refs.pagination.current = newPage;
      }
      this.fetchPendingAudits();
    },
    handleSearch() {
      this.pagination.current = 1;
      if (this.$refs.pagination) {
        this.$refs.pagination.current = 1;
      }
      this.fetchPendingAudits();
    },
    getListQueryParams(overrides = {}) {
      return buildAuditListParams(this.search, {
        current: overrides.page ?? this.pagination.current,
        pageSize: overrides.pageSize ?? this.pagination.pageSize,
      });
    },
    async selectAllAudits() {
      if (this.pagination.total <= 0) return;
      this.loading = true;
      try {
        let apiItems = [];
        try {
          const res = await listPendingAudits(
            this.getListQueryParams({
              page: 1,
              pageSize: this.pagination.total,
            }),
          );
          apiItems = res.data?.list || [];
        } catch (err) {
          console.warn("[TerminologyAudit] selectAll API unavailable", err);
        }
        const filters = extractAuditFilters(this.search);
        const allItems = mergePendingAudits({ apiItems, filters });
        this.selectEntry = allItems;
        this.selectedRows = [...allItems];
        this.selectedRowKeys = allItems.map((item) => item.id);
      } catch (err) {
        message.error("获取全部待审核术语失败");
        console.error(err);
      } finally {
        this.loading = false;
      }
    },
    async fetchPendingAudits() {
      this.loading = true;
      this.lastSearch = { ...this.search };
      const filters = extractAuditFilters(this.search);
      try {
        let apiItems = [];
        let serverTotal = 0;
        try {
          const res = await listPendingAudits(this.getListQueryParams());
          const data = res.data || {};
          apiItems = data.list || [];
          serverTotal = data.total ?? 0;
        } catch (err) {
          const status = err?.response?.status;
          console.warn(
            "[TerminologyAudit] API unavailable, using local/mock data",
            err,
          );
          if (status === 500 || status === 502 || err?.code === "ERR_NETWORK") {
            message.warning("术语 Agent 服务未就绪，请确认 terminology-agent 已启动");
          }
        }

        const localOnlyCount = mergePendingAudits({
          apiItems: [],
          filters,
        }).length;
        const displayItems =
          this.pagination.current === 1
            ? mergePendingAudits({ apiItems, filters })
            : apiItems;

        this.pagination.total = serverTotal + localOnlyCount;
        this.dataSource = displayItems.map((item) => ({
          ...item,
          processing: false,
        }));
      } catch (err) {
        message.error("获取待审核列表失败");
        console.error(err);
      } finally {
        this.loading = false;
        this.$nextTick(() => this.setTableHeight());
      }
    },
    handlePageChange(current, pageSize) {
      pageChange(
        this,
        current,
        pageSize,
        this.fetchPendingAudits,
        "selectEntry",
      );
    },
    async handleReview(record, action) {
      const audit = this.dataSource.find((a) => a.id === record.id);
      if (!audit) return;
      audit.processing = true;
      try {
        if (audit._local || audit._mock) {
          removeLocalPendingAudit(audit.id);
          if (action === "approved") {
            message.success("已确认，术语将合并至术语库，可在翻译审核中查看");
          } else {
            message.success("已拒绝");
          }
          this.dataSource = this.dataSource.filter((a) => a.id !== audit.id);
          this.pagination.total = Math.max(0, this.pagination.total - 1);
          this.removeFromSelection(audit.id);
        } else {
          await reviewTerm(audit.id, action);
          if (action === "approved") {
            message.success("已确认，术语将合并至术语库，可在翻译审核中查看");
          } else {
            message.success("已拒绝");
          }
          this.removeFromSelection(audit.id);
          if (this.dataSource.length === 1 && this.pagination.current > 1) {
            this.pagination.current -= 1;
            if (this.$refs.pagination) {
              this.$refs.pagination.current = this.pagination.current;
            }
          }
          await this.fetchPendingAudits();
        }
      } catch (err) {
        message.error("操作失败");
        console.error(err);
      } finally {
        audit.processing = false;
      }
    },
    removeFromSelection(id) {
      this.selectedRowKeys = this.selectedRowKeys.filter((key) => key !== id);
      this.selectedRows = this.selectedRows.filter((item) => item.id !== id);
      this.selectEntry = this.selectEntry.filter((item) => item.id !== id);
    },
  },
};
</script>

<style scoped lang="less">
.terminology-audit {
  padding: 16px;

  :deep(.search) {
    margin-bottom: 12px;
  }

  :deep(.search .form .ant-row) {
    margin-bottom: 8px !important;
  }

  :deep(.term-audit-confidence .ant-form-item-control) {
    width: auto !important;
  }
}

.terminology-audit__pagination {
  position: relative;
  min-height: 48px;
  margin-top: 8px;
}

.reasoning-text {
  color: #888;
  cursor: help;
}
</style>
