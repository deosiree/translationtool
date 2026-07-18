<template>
  <div class="comment-rules">
    <SearchBox>
      <template v-slot:form>
        <a-form
          layout="inline"
          :model="search"
          autocomplete="off"
          :label-col="labelCol"
        >
          <a-form-item :label="GLOSSARY_LABEL.commentKey">
            <a-input
              v-model:value="search.commentKey"
              :placeholder="GLOSSARY_PLACEHOLDER.commentKey"
              allow-clear
              @pressEnter="onSearch"
            />
          </a-form-item>
          <a-form-item :label="GLOSSARY_LABEL.preferAbbr">
            <a-select
              v-model:value="search.preferAbbr"
              :placeholder="GLOSSARY_PLACEHOLDER.yesNo"
              :options="yesNoOptions"
              allow-clear
            />
          </a-form-item>
        </a-form>
      </template>
      <template v-slot:operate>
        <GlossarySearchOperate
          :search="search"
          :currentPage="pagination.current"
          @reset="onReset"
          @search="onSearch"
        />
      </template>
    </SearchBox>

    <div class="list-shell">
      <div class="list-shell__head">
        <div class="list-shell__title"><span>Comment规则：</span></div>
        <div class="list-shell__toolbar">
          <div class="toolbar">
            <a-button type="primary" @click="openCreate">
              <template #icon><PlusOutlined /></template>
              新增
            </a-button>
            <a-upload
              :show-upload-list="false"
              :before-upload="beforeImport"
              accept=".xlsx,.xls"
            >
              <a-button :loading="importing">导入</a-button>
            </a-upload>
          </div>
        </div>
      </div>
      <div ref="tableWrapperRef" class="list-shell__body table-wrapper">
        <a-table
          size="middle"
          row-key="id"
          :columns="columns"
          :data-source="dataSource"
          :loading="loading"
          :pagination="false"
          :scroll="{ y: tableScrollY }"
          :customRow="customRow"
        >
          <template #bodyCell="{ column, text, record }">
            <template v-if="isEditing(record)">
              <template v-if="isField(column, 'comment_key')">
                <a-input
                  v-model:value="editableData[record.id].comment_key"
                  @click.stop
                  @pressEnter="saveRow(record)"
                />
              </template>
              <template v-else-if="isField(column, 'entry_source')">
                <a-input
                  v-model:value="editableData[record.id].entry_source"
                  @click.stop
                  @pressEnter="saveRow(record)"
                />
              </template>
              <template v-else-if="isField(column, 'scene')">
                <a-textarea
                  v-model:value="editableData[record.id].scene"
                  :rows="3"
                  @click.stop
                />
              </template>
              <template v-else-if="isField(column, 'rule_text')">
                <a-textarea
                  v-model:value="editableData[record.id].rule_text"
                  :rows="4"
                  @click.stop
                />
              </template>
              <template v-else-if="isField(column, 'prefer_abbr')">
                <a-switch
                  v-model:checked="editableData[record.id].prefer_abbr"
                  checked-children="是"
                  un-checked-children="否"
                  @click.stop
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
              <template v-if="isField(column, 'prefer_abbr')">
                {{ record.prefer_abbr ? "是" : "否" }}
              </template>
              <template v-else-if="isField(column, 'scene')">
                <SpanByTipsFill
                  :content="record.scene || ''"
                  :max-width="column.width"
                  theme="dark"
                  copyable
                  always-tip
                />
              </template>
              <template v-else-if="isField(column, 'rule_text')">
                <SpanByTipsFill
                  :content="record.rule_text || ''"
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
                    v-if="record.related_id"
                    label="对应"
                    @click.stop="openRelated(record)"
                  />
                  <OpItem
                    label="删除"
                    type="danger"
                    @click.stop="onDelete(record)"
                  />
                </OperationCellOverflow>
              </template>
              <template v-else>{{ text }}</template>
            </template>
          </template>
        </a-table>
      </div>
      <div class="list-shell__pagination">
        <a-pagination
          v-model:current="pagination.current"
          v-model:pageSize="pagination.pageSize"
          :total="pagination.total"
          :show-size-changer="pagination.showSizeChanger"
          :show-total="pagination.showTotal"
          @change="onPageChange"
        />
      </div>
    </div>

    <a-modal
      v-model:visible="modalVisible"
      title="新增 Comment 规则"
      :confirm-loading="modalSaving"
      destroy-on-close
      @ok="saveModal"
      @cancel="modalVisible = false"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item :label="GLOSSARY_LABEL.commentKey" required>
          <a-input
            v-model:value="form.comment_key"
            :placeholder="GLOSSARY_PLACEHOLDER.commentKey"
          />
        </a-form-item>
        <a-form-item :label="GLOSSARY_LABEL.entrySource">
          <a-input
            v-model:value="form.entry_source"
            :placeholder="GLOSSARY_PLACEHOLDER.entrySource"
          />
        </a-form-item>
        <a-form-item :label="GLOSSARY_LABEL.scene">
          <a-textarea
            v-model:value="form.scene"
            :rows="3"
            :placeholder="GLOSSARY_PLACEHOLDER.scene"
          />
        </a-form-item>
        <a-form-item :label="GLOSSARY_LABEL.ruleText">
          <a-textarea
            v-model:value="form.rule_text"
            :rows="5"
            :placeholder="GLOSSARY_PLACEHOLDER.ruleText"
          />
        </a-form-item>
        <a-form-item :label="GLOSSARY_LABEL.preferAbbr">
          <a-switch
            v-model:checked="form.prefer_abbr"
            checked-children="是"
            un-checked-children="否"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:visible="relatedVisible"
      title="对应 Comment"
      :footer="null"
      width="920px"
      destroy-on-close
      wrap-class-name="comment-related-modal"
      @cancel="closeRelated"
    >
      <a-spin :spinning="relatedLoading">
        <div v-if="relatedPairs.length" class="related-compare">
          <article
            v-for="(item, idx) in relatedPairs"
            :key="item.id || idx"
            class="related-card"
            :class="idx === 0 ? 'related-card--self' : 'related-card--peer'"
          >
            <header class="related-card__head">
              <span class="related-card__badge">{{
                idx === 0 ? "本条" : "对应条"
              }}</span>
              <span class="related-card__key">{{ item.comment_key }}</span>
            </header>
            <div class="related-card__body">
              <div class="related-card__meta">
                <div>
                  <span class="related-card__label">词条来源</span>
                  <div class="related-card__value">
                    {{ item.entry_source || "—" }}
                  </div>
                </div>
                <div>
                  <span class="related-card__label">优先缩写</span>
                  <div class="related-card__value">
                    {{ item.prefer_abbr ? "是" : "否" }}
                  </div>
                </div>
              </div>
              <div>
                <span class="related-card__label">场景</span>
                <div class="related-sbt related-sbt--scene">
                  <SpanByTipsFill
                    :content="item.scene || ''"
                    max-width="100%"
                    theme="dark"
                    copyable
                    always-tip
                  />
                </div>
              </div>
              <div>
                <span class="related-card__label">规则</span>
                <div class="related-sbt related-sbt--rule">
                  <SpanByTipsFill
                    :content="item.rule_text || ''"
                    max-width="100%"
                    theme="dark"
                    copyable
                    always-tip
                  />
                </div>
              </div>
            </div>
          </article>
        </div>
        <a-empty v-else-if="!relatedLoading" />
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
import { ref } from "vue";
import { message, Modal } from "ant-design-vue";
import { PlusOutlined } from "@ant-design/icons-vue";
import { cloneDeep } from "lodash-es";
import SearchBox from "@/components/search/searchBox.vue";
import GlossarySearchOperate from "@/views/glossary/shared/GlossarySearchOperate.vue";
import {
  OpItem,
  OperationCellOverflow,
} from "@/components/OperationColumn";
import SpanByTipsFill from "@/components/SpanByTips/SpanByTipsFill/index.vue";
import { useTableBodyHeight } from "@/composables/useTableBodyHeight";
import {
  GLOSSARY_LABEL,
  GLOSSARY_PLACEHOLDER,
} from "@/views/glossary/shared/glossaryQueryLabels.js";
import {
  listCommentRules,
  getCommentRule,
  createCommentRule,
  updateCommentRule,
  deleteCommentRule,
  importCommentRules,
} from "@/http/api/terminologyAgent";

function emptySearch() {
  return { commentKey: "", preferAbbr: undefined };
}

function emptyForm() {
  return {
    comment_key: "",
    entry_source: "",
    scene: "",
    rule_text: "",
    prefer_abbr: false,
  };
}

export default {
  name: "CommentRules",
  components: {
    SearchBox,
    GlossarySearchOperate,
    OpItem,
    OperationCellOverflow,
    SpanByTipsFill,
    PlusOutlined,
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
    return {
      GLOSSARY_LABEL,
      GLOSSARY_PLACEHOLDER,
      labelCol: { style: { width: "84px" } },
      yesNoOptions: [
        { label: "是", value: true },
        { label: "否", value: false },
      ],
      search: emptySearch(),
      columns: [
        { title: "comment", dataIndex: "comment_key", width: 140, ellipsis: true },
        { title: "词条来源", dataIndex: "entry_source", width: 120, ellipsis: true },
        { title: "场景", dataIndex: "scene", width: 200 },
        { title: "规则", dataIndex: "rule_text", width: 280 },
        { title: "优先缩写", dataIndex: "prefer_abbr", width: 88 },
        { title: "操作", dataIndex: "operation", width: 100, fixed: "right" },
      ],
      dataSource: [],
      loading: false,
      importing: false,
      editableData: {},
      savingRowId: null,
      pagination: {
        current: 1,
        pageSize: 20,
        total: 0,
        showSizeChanger: true,
        showTotal: (t) => `共 ${t} 条`,
      },
      modalVisible: false,
      modalSaving: false,
      form: emptyForm(),
      relatedVisible: false,
      relatedLoading: false,
      relatedPairs: [],
    };
  },
  mounted() {
    this.fetchList();
  },
  methods: {
    isField(column, name) {
      return column.dataIndex === name || column.colValue === name;
    },
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
      this.editableData = {
        [record.id]: cloneDeep({
          comment_key: record.comment_key || "",
          entry_source: record.entry_source || "",
          scene: record.scene || "",
          rule_text: record.rule_text || "",
          prefer_abbr: !!record.prefer_abbr,
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
      const key = (draft.comment_key || "").trim();
      if (!key) {
        message.warning("请填写 comment");
        return;
      }
      this.savingRowId = record.id;
      try {
        await updateCommentRule(record.id, {
          commentKey: key,
          entrySource: (draft.entry_source || "").trim() || null,
          scene: draft.scene || null,
          ruleText: draft.rule_text || null,
          preferAbbr: !!draft.prefer_abbr,
        });
        message.success("已保存");
        this.cancelEdit(record);
        await this.fetchList();
      } catch (e) {
        message.error(e?.message || "保存失败");
      } finally {
        this.savingRowId = null;
      }
    },
    onSearch() {
      this.pagination.current = 1;
      this.fetchList();
    },
    onReset() {
      this.search = emptySearch();
      this.pagination.current = 1;
      this.fetchList();
    },
    onPageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
      this.fetchList();
    },
    async fetchList() {
      this.loading = true;
      try {
        const res = await listCommentRules({
          page: this.pagination.current,
          pageSize: this.pagination.pageSize,
          commentKey: this.search.commentKey || undefined,
          preferAbbr:
            this.search.preferAbbr === true || this.search.preferAbbr === false
              ? this.search.preferAbbr
              : undefined,
        });
        const data = res?.data ?? res;
        this.dataSource = data?.list || [];
        this.pagination.total = data?.total ?? 0;
        this.$nextTick(() => this.syncTableHeight?.());
      } catch (e) {
        message.error(e?.message || "加载失败");
      } finally {
        this.loading = false;
      }
    },
    openCreate() {
      this.form = emptyForm();
      this.modalVisible = true;
    },
    async openRelated(record) {
      if (!record?.related_id) return;
      this.relatedVisible = true;
      this.relatedLoading = true;
      this.relatedPairs = [];
      try {
        const res = await getCommentRule(record.related_id);
        const related = res?.data ?? res;
        this.relatedPairs = [
          {
            id: record.id,
            comment_key: record.comment_key,
            entry_source: record.entry_source,
            scene: record.scene,
            rule_text: record.rule_text,
            prefer_abbr: record.prefer_abbr,
          },
          related,
        ].filter(Boolean);
      } catch (e) {
        message.error(e?.message || "加载对应规则失败");
        this.relatedVisible = false;
      } finally {
        this.relatedLoading = false;
      }
    },
    closeRelated() {
      this.relatedVisible = false;
      this.relatedPairs = [];
    },
    async saveModal() {
      const key = (this.form.comment_key || "").trim();
      if (!key) {
        message.warning("请填写 comment");
        return;
      }
      this.modalSaving = true;
      try {
        await createCommentRule({
          commentKey: key,
          entrySource: (this.form.entry_source || "").trim() || null,
          scene: this.form.scene || null,
          ruleText: this.form.rule_text || null,
          preferAbbr: !!this.form.prefer_abbr,
        });
        message.success("已保存");
        this.modalVisible = false;
        await this.fetchList();
      } catch (e) {
        message.error(e?.message || "保存失败");
      } finally {
        this.modalSaving = false;
      }
    },
    onDelete(record) {
      Modal.confirm({
        title: "确认删除该规则？",
        content: record.comment_key,
        onOk: async () => {
          await deleteCommentRule(record.id);
          message.success("已删除");
          this.cancelEdit(record);
          await this.fetchList();
        },
      });
    },
    beforeImport(file) {
      Modal.confirm({
        title: "导入 Comment 规则",
        content: "按 comment+场景 upsert；默认不覆盖已有「优先缩写」。是否继续？",
        onOk: async () => {
          this.importing = true;
          try {
            const res = await importCommentRules(file, false);
            const data = res?.data ?? res;
            message.success(
              `导入完成：新增 ${data?.created ?? 0}，更新 ${data?.updated ?? 0}`
            );
            await this.fetchList();
          } catch (e) {
            message.error(e?.message || "导入失败");
          } finally {
            this.importing = false;
          }
        },
      });
      return false;
    },
  },
};
</script>

<style scoped>
.comment-rules {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}
.comment-rules > :first-child {
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
.related-compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-items: stretch;
}
.related-card {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8f9fb;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin: 0;
  padding: 0;
}
.related-card--self .related-card__head {
  border-top: 3px solid #1677ff;
}
.related-card--peer .related-card__head {
  border-top: 3px solid #13c2c2;
}
.related-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}
.related-card__badge {
  font-size: 12px;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 999px;
  line-height: 20px;
}
.related-card--self .related-card__badge {
  background: #e6f4ff;
  color: #1677ff;
}
.related-card--peer .related-card__badge {
  background: #e6fffb;
  color: #08979c;
}
.related-card__key {
  font-size: 15px;
  font-weight: 700;
  color: #1677ff;
}
.related-card__body {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}
.related-card__meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.related-card__label {
  display: block;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 4px;
  line-height: 1.4;
}
.related-card__value {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.88);
  line-height: 1.5;
}
.related-sbt {
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #fff;
  padding: 8px 10px;
  min-width: 0;
}
.related-sbt :deep(.span-by-tips-fill-host) {
  max-width: 100% !important;
  width: 100%;
}
.related-sbt :deep(.span-by-tips-fill) {
  white-space: pre-wrap;
  word-break: break-word;
  text-overflow: unset;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  line-height: 1.55;
  font-size: 13px;
}
.related-sbt--scene :deep(.span-by-tips-fill) {
  -webkit-line-clamp: 4;
  min-height: 72px;
}
.related-sbt--rule :deep(.span-by-tips-fill) {
  -webkit-line-clamp: 10;
  min-height: 140px;
}
</style>

<style>
/* 模态 wrap 非 scoped，保证窄屏可纵向堆叠 */
@media (max-width: 768px) {
  .comment-related-modal .related-compare {
    grid-template-columns: 1fr;
  }
}
</style>
