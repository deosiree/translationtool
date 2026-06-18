<template>
  <div class="terminology-audit">
    <a-card title="术语学习 - 待人工确认" :bordered="false">
      <template #extra>
        <a-button type="primary" size="small" @click="fetchPendingAudits">刷新</a-button>
      </template>

      <a-table
        :dataSource="audits"
        :columns="columns"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <!-- 源术语列 -->
          <template v-if="column.key === 'source_text'">
            <strong>{{ record.source_text }}</strong>
          </template>

          <!-- LLM 建议列 -->
          <template v-if="column.key === 'suggested_translation'">
            <a-tag color="blue">{{ record.suggested_translation || '未生成' }}</a-tag>
          </template>

          <!-- LLM 解释列 -->
          <template v-if="column.key === 'llm_reasoning'">
            <a-tooltip :title="record.llm_reasoning">
              <span class="reasoning-text">{{ record.llm_reasoning ? record.llm_reasoning.substring(0, 60) + '...' : '-' }}</span>
            </a-tooltip>
          </template>

          <!-- 上下文列 -->
          <template v-if="column.key === 'context'">
            <span>{{ record.context || '-' }}</span>
          </template>

          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-button
              type="primary"
              size="small"
              style="margin-right: 8px"
              :disabled="record.processing"
              @click="handleReview(record.id, 'approved')"
            >
              确认
            </a-button>
            <a-button
              danger
              size="small"
              :disabled="record.processing"
              @click="handleReview(record.id, 'rejected')"
            >
              拒绝
            </a-button>
          </template>
        </template>

        <template #emptyText>
          <a-empty description="暂无待确认的术语" />
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script>
import { listPendingAudits, reviewTerm } from '@/http/api/terminologyAgent';
import { message } from 'ant-design-vue';

export default {
  name: 'TerminologyAudit',
  data() {
    return {
      loading: false,
      audits: [],
      columns: [
        { title: '源术语', dataIndex: 'source_text', key: 'source_text', width: 150 },
        { title: '建议翻译', dataIndex: 'suggested_translation', key: 'suggested_translation', width: 150 },
        { title: 'LLM 解释', dataIndex: 'llm_reasoning', key: 'llm_reasoning', width: 200, ellipsis: true },
        { title: '上下文', dataIndex: 'context', key: 'context', width: 200, ellipsis: true },
        { title: '提交时间', dataIndex: 'created_at', key: 'created_at', width: 180 },
        { title: '操作', key: 'action', width: 140, fixed: 'right' },
      ],
    };
  },
  mounted() {
    this.fetchPendingAudits();
  },
  methods: {
    async fetchPendingAudits() {
      this.loading = true;
      try {
        const res = await listPendingAudits();
        const data = res.data || {};
        this.audits = (data.items || []).map(item => ({ ...item, processing: false }));
      } catch (err) {
        message.error('获取待确认列表失败');
        console.error(err);
      } finally {
        this.loading = false;
      }
    },
    async handleReview(auditId, action) {
      const audit = this.audits.find(a => a.id === auditId);
      if (!audit) return;
      audit.processing = true;
      try {
        await reviewTerm(auditId, action);
        message.success(action === 'approved' ? '已确认' : '已拒绝');
        this.audits = this.audits.filter(a => a.id !== auditId);
      } catch (err) {
        message.error('操作失败');
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
.reasoning-text {
  color: #888;
  cursor: help;
}
</style>
