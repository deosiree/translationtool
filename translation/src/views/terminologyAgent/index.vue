<template>
  <div class="terminology-audit">
    <a-card title="术语学习 - Agent 预翻译待审核" :bordered="false">
      <template #extra>
        <a-button type="primary" size="small" @click="fetchPendingAudits"
          >刷新</a-button
        >
      </template>

      <div class="terminology-audit__body">
        <a-table
          :dataSource="audits"
          :columns="columns"
          :loading="loading"
          :pagination="false"
          :scroll="{ x: 1600 }"
          rowKey="id"
        >
        <template #bodyCell="{ column, record }">
          <!-- 源术语列 -->
          <template v-if="column.key === 'source_text'">
            <span v-text="formatEntryText(record.source_text)"></span>
          </template>

          <!-- LLM 建议列 -->
          <template v-if="column.key === 'suggested_translation'">
            <a-tag color="blue">{{
              record.suggested_translation || "未生成"
            }}</a-tag>
          </template>
          <!-- 置信度 -->
          <template v-if="column.key === 'confidence'">
            <a-tag :color="confidenceColor(record.confidence)">
              {{ formatConfidence(record.confidence) }}
            </a-tag>
          </template>

          <!-- 参考术语 -->
          <template v-if="column.key === 'similar_terms'">
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
                    <template #bodyCell="{ column, record: term }">
                      <template v-if="column.key === 'retrieval_source'">
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
          <!-- 检索方式 -->
          <template v-if="column.key === 'retrieval_method'">
            <a-tag
              v-if="record._mock || record._local"
              color="orange"
              style="margin-right: 4px"
            >
              本地 Mock
            </a-tag>
            {{ formatRetrievalMethod(record.retrieval_method) }}
          </template>
          <!-- LLM 解释列 -->
          <template v-if="column.key === 'llm_reasoning'">
            <a-tooltip :title="record.llm_reasoning">
              <span class="reasoning-text">
                {{ truncateText(record.llm_reasoning, 40) }}
              </span>
            </a-tooltip>
          </template>

          <template v-if="column.key === 'created_at'">
            {{ formatDateTime(record.created_at) }}
          </template>

          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
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
      <div class="terminology-audit__pagination">
        <Pagination
          ref="pagination"
          :total="pagination.total"
          @pageChange="handlePageChange"
        />
      </div>
    </a-card>
  </div>
</template>

<script>
/**
 * 术语学习 — Agent 预翻译待审核页。
 *
 * Phase 3a：参考术语 Popover 增加「来源」列（RAG / Grep / RAG+Grep）。
 */
import { listPendingAudits, reviewTerm } from "@/http/api/terminologyAgent";
import {
  mergePendingAudits,
  removeLocalPendingAudit,
  formatRetrievalMethod,
  formatRetrievalSource,
  formatConfidence,
  formatEntryText,
} from "@/utils/agentPendingAudits";
import { message } from "ant-design-vue";
import Pagination from "@/components/page/pagination.vue";

export default {
  name: "TerminologyAudit",
  components: {
    Pagination,
  },
  data() {
    return {
      loading: false,
      audits: [],
      pagination: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      columns: [
        {
          title: "词条",
          dataIndex: "source_text",
          key: "source_text",
          width: 180,
          ellipsis: true,
        },
        {
          title: "建议翻译",
          dataIndex: "suggested_translation",
          key: "suggested_translation",
          width: 160,
        },
        {
          title: "目标语种",
          dataIndex: "target_lang",
          key: "target_lang",
          width: 90,
        },
        {
          title: "任务名称",
          dataIndex: "task_name",
          key: "task_name",
          width: 140,
          ellipsis: true,
        },
        {
          title: "产品名称",
          dataIndex: "product_name",
          key: "product_name",
          width: 120,
          ellipsis: true,
        },
        {
          title: "部门所属",
          dataIndex: "department",
          key: "department",
          width: 100,
        },
        {
          title: "置信度",
          dataIndex: "confidence",
          key: "confidence",
          width: 80,
        },
        { title: "参考术语", key: "similar_terms", width: 100 },
        {
          title: "检索方式",
          dataIndex: "retrieval_method",
          key: "retrieval_method",
          width: 90,
        },
        {
          title: "Agent 说明",
          dataIndex: "llm_reasoning",
          key: "llm_reasoning",
          width: 160,
          ellipsis: true,
        },
        {
          title: "提交时间",
          dataIndex: "created_at",
          key: "created_at",
          width: 160,
        },
        { title: "操作", key: "action", width: 140, fixed: "right" },
      ],
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
    this.fetchPendingAudits();
  },
  methods: {
    formatRetrievalMethod,
    formatRetrievalSource,
    formatConfidence,
    formatEntryText,
    confidenceColor(confidence) {
      if (confidence == null) return "default";
      return Number(confidence) >= 0.8 ? "green" : "orange";
    },
    /**
     * 截断文本
     * @param {string} text
     * @param {number} maxLen
     * @returns {string}
     */
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
    /**
     * 加载待审核列表：优先 API，与 localStorage / mock 合并去重
     * @returns {Promise<void>}
     */
    async fetchPendingAudits() {
      this.loading = true;
      try {
        let apiItems = [];
        let serverTotal = 0;
        try {
          const res = await listPendingAudits({
            page: this.pagination.current,
            pageSize: this.pagination.pageSize,
          });
          const data = res.data || {};
          apiItems = data.list || [];
          serverTotal = data.total ?? 0;
        } catch (err) {
          console.warn(
            "[TerminologyAudit] API unavailable, using local/mock data",
            err,
          );
        }

        const localOnlyCount = mergePendingAudits({ apiItems: [] }).length;
        const displayItems =
          this.pagination.current === 1
            ? mergePendingAudits({ apiItems })
            : apiItems;

        this.pagination.total = serverTotal + localOnlyCount;
        this.audits = displayItems.map((item) => ({
          ...item,
          processing: false,
        }));
      } catch (err) {
        message.error("获取待审核列表失败");
        console.error(err);
      } finally {
        this.loading = false;
      }
    },
    handlePageChange(current, pageSize) {
      this.pagination.current = current;
      this.pagination.pageSize = pageSize;
      this.fetchPendingAudits();
    },
    /**
     * 确认或拒绝单条待审核记录；本地/mock 走 localStorage，远端走 reviewTerm API
     * @param {import('@/http/api/terminologyAgent').AuditRecord & { processing?: boolean, _local?: boolean, _mock?: boolean }} record
     * @param {"approved"|"rejected"} action
     * @returns {Promise<void>}
     */
    async handleReview(record, action) {
      const audit = this.audits.find((a) => a.id === record.id);
      if (!audit) return;
      audit.processing = true;
      try {
        if (audit._local || audit._mock) {
          removeLocalPendingAudit(audit.id);
          if (action === "approved") {
            message.success("已确认，术语将合并至术语库");
          } else {
            message.success("已拒绝");
          }
          this.audits = this.audits.filter((a) => a.id !== audit.id);
          this.pagination.total = Math.max(0, this.pagination.total - 1);
        } else {
          await reviewTerm(audit.id, action);
          if (action === "approved") {
            message.success("已确认，术语将合并至术语库");
          } else {
            message.success("已拒绝");
          }
          if (this.audits.length === 1 && this.pagination.current > 1) {
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
  },
};
</script>

<style scoped>
.terminology-audit {
  padding: 16px;
}
.terminology-audit__body {
  margin-bottom: 8px;
}
.terminology-audit__pagination {
  position: relative;
  min-height: 48px;
}
.reasoning-text {
  color: #888;
  cursor: help;
}
</style>
