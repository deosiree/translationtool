<template>
  <span :class="['stage-icon', statusClass]" :title="title">
    {{ icon }}
    <span v-if="status === 'failed' && retry > 0" class="retry-badge">×{{ retry }}</span>
  </span>
</template>

<script>
export default {
  name: 'StageIcon',
  props: {
    status: { type: String, default: 'pending' },
    retry: { type: Number, default: 0 }
  },
  computed: {
    statusConfig() {
      const configs = {
        pending: { icon: '⏳', class: 'pending', title: '待执行' },
        running: { icon: '⟳', class: 'running', title: '执行中' },
        success: { icon: '✓', class: 'success', title: '成功' },
        failed: { icon: '✗', class: 'failed', title: '失败' },
        skipped: { icon: '⊘', class: 'skipped', title: '已跳过' }
      }
      return configs[this.status] || configs.pending
    },
    icon() { return this.statusConfig.icon },
    statusClass() { return this.statusConfig.class },
    title() { return this.statusConfig.title }
  }
}
</script>

<style scoped lang="less">
.stage-icon {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  padding: 2px 6px;
  border-radius: 3px;

  &.pending { color: #999; }
  &.running { color: #1890ff; animation: spin 1s linear infinite; }
  &.success { color: #52c41a; }
  &.failed { color: #ff4d4f; }
  &.skipped { color: #d9d9d9; }

  .retry-badge {
    font-size: 10px;
    background: #fff1f0;
    color: #ff4d4f;
    padding: 0 3px;
    border-radius: 2px;
    margin-left: 4px;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>