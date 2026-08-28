<template>
  <div v-if="visible" class="batch-progress-overlay">
    <div class="overlay-backdrop" />
    <div class="progress-panel">
      <div class="panel-header">
        <h3>批量预翻译执行进度</h3>
        <a-badge :status="phase === 'running' ? 'processing' : 'success'" />
      </div>

      <div class="progress-list">
        <div
          v-for="p in progresses"
          :key="p.taskId"
          class="progress-item"
          :class="{ current: p.currentStage }"
        >
          <div class="task-info">
            <span class="task-name">{{ p.taskName }}</span>
            <span v-if="p.currentStage" class="running-badge">执行中: {{ stageLabelMap[p.currentStage] }}</span>
          </div>
          <div class="stages">
            <StageIcon :status="p.stages.entryExamine" :retry="p.retryCount" />
            <StageIcon :status="p.stages.preTranslate" :retry="p.retryCount" />
            <StageIcon :status="p.stages.translateExamine" :retry="p.retryCount" />
          </div>
          <div class="status">
            <span v-if="p.error" class="error">{{ p.error }}</span>
            <span v-else-if="isTaskDone(p)" class="success">全部完成</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState, mapGetters } from 'vuex'
import StageIcon from './StageIcon.vue'
import { notification } from 'ant-design-vue'

export default {
  name: 'BatchProgressOverlay',
  components: { StageIcon },
  computed: {
    ...mapState('batchProgress', ['phase', 'progresses']),
    ...mapGetters('batchProgress', ['visible']),
    stageLabelMap() {
      return {
        entryExamine: '词条审核',
        preTranslate: '翻译(预翻译)',
        translateExamine: '翻译审核'
      }
    }
  },
  watch: {
    phase(newVal) {
      if (newVal === 'completed') {
        this.handleCompleted()
      }
    }
  },
  methods: {
    isTaskDone(p) {
      const enabled = Object.keys(p.stages).filter(k => p.stages[k] !== 'pending' && p.stages[k] !== 'skipped')
      return enabled.length > 0 && enabled.every(k => p.stages[k] === 'success' || p.stages[k] === 'skipped')
    },
    handleCompleted() {
      const progresses = this.$store.state.batchProgress.progresses
      const success = progresses.filter(p =>
        Object.values(p.stages).every(s => s === 'success' || s === 'skipped')
      ).length
      const failed = progresses.filter(p =>
        Object.values(p.stages).includes('failed')
      )
      const failedNames = failed.map(p => p.taskName).join('、')

      let msg = `批量预翻译执行完成：成功(${success})`
      if (failed.length > 0) {
        msg += `；失败(${failed.length}): ${failedNames}`
      }

      // 先重置状态关闭遮罩，再发送通知
      this.$store.dispatch('batchProgress/reset')
      notification.success({
        message: '批量预翻译执行完成',
        description: msg,
        duration: 5
      })
    }
  }
}
</script>

<style scoped lang="less">
.batch-progress-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: flex-end;
  padding: 20px;
  pointer-events: none;
  box-sizing: border-box;

  .overlay-backdrop {
    position: absolute;
    inset: 0;
  }

  .progress-panel {
    pointer-events: auto;
    width: 100%;
    max-width: 520px;
    background: white;
    border-radius: 8px;
    border: 1px solid #e8e8e8;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    overflow: hidden;
    margin-top: 60px;

    .panel-header {
      padding: 12px 16px;
      border-bottom: 1px solid #f0f0f0;
      background: #fafafa;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-radius: 8px 8px 0 0;

      h3 {
        margin: 0;
        font-size: 14px;
        font-weight: 600;
        color: #333;
      }
    }

    .progress-list {
      max-height: 400px;
      overflow-y: auto;
      padding: 8px 12px;

      .progress-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 8px 4px;
        border-bottom: 1px solid #f5f5f5;

        &:last-child {
          border-bottom: none;
        }

        &.current {
          background: #fffbe6;
        }

        .task-info {
          flex: 1;
          min-width: 0;

          .task-name {
            display: block;
            font-size: 13px;
            color: #333;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }

          .running-badge {
            font-size: 11px;
            color: #1890ff;
            margin-top: 2px;
            display: block;
          }
        }

        .stages {
          display: flex;
          gap: 8px;
          flex-shrink: 0;
        }

        .status {
          flex-shrink: 0;
          width: 80px;
          text-align: right;
          font-size: 12px;

          &.error { color: #ff4d4f; }
          &.success { color: #52c41a; }
        }
      }
    }
  }
}

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

.error { color: #ff4d4f; font-size: 12px; }
.running { color: #1890ff; font-size: 12px; }
.success { color: #52c41a; font-size: 12px; }
</style>