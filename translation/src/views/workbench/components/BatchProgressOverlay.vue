<template>
  <div v-if="visible" class="batch-progress-overlay">
    <div class="overlay-panel">
      <div class="overlay-header">
        <h3>批量预翻译执行进度</h3>
        <a-badge :status="phase === 'running' ? 'processing' : 'success'" />
      </div>

      <div class="progress-table-wrap">
        <table class="progress-table">
          <thead>
            <tr>
              <th style="width: 200px;">任务名称</th>
              <th style="width: 120px;">词条审核</th>
              <th style="width: 120px;">翻译(预翻译)</th>
              <th style="width: 120px;">翻译审核</th>
              <th>状态 / 错误</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in progresses" :key="p.taskId" :class="{ current: p.currentStage }">
              <td>{{ p.taskName }}</td>
              <td><StageIcon :status="p.stages.entryExamine" :retry="p.retryCount" /></td>
              <td><StageIcon :status="p.stages.preTranslate" :retry="p.retryCount" /></td>
              <td><StageIcon :status="p.stages.translateExamine" :retry="p.retryCount" /></td>
              <td>
                <span v-if="p.error" class="error">{{ p.error }}</span>
                <span v-else-if="p.currentStage" class="running">执行中: {{ stageLabelMap[p.currentStage] }}</span>
                <span v-else-if="isTaskDone(p)" class="success">全部完成</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="phase === 'completed'" class="overlay-footer">
        <a-button type="primary" @click="close">关闭</a-button>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState, mapGetters } from 'vuex'
import StageIcon from './StageIcon.vue'

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
  methods: {
    isTaskDone(p) {
      const enabled = Object.keys(p.stages).filter(k => p.stages[k] !== 'pending' && p.stages[k] !== 'skipped')
      return enabled.length > 0 && enabled.every(k => p.stages[k] === 'success' || p.stages[k] === 'skipped')
    },
    close() {
      this.$store.dispatch('batchProgress/reset')
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
  background: rgba(255, 255, 255, 0.95);
  display: flex;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
  overflow: auto;

  .overlay-panel {
    width: 100%;
    max-width: 1000px;
    background: white;
    border-radius: 8px;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
    overflow: hidden;
    min-height: 300px;
  }

  .overlay-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;
    background: #fafafa;
    border-radius: 8px 8px 0 0;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
  }

  .progress-table-wrap {
    max-height: 400px;
    overflow-y: auto;
    padding: 0 20px;
  }

  .progress-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;

    th {
      background: #fafafa;
      padding: 10px 8px;
      text-align: left;
      font-weight: 600;
      color: #333;
      border-bottom: 1px solid #e8e8e8;
      position: sticky;
      top: 0;
      z-index: 1;
    }

    td {
      padding: 10px 8px;
      border-bottom: 1px solid #f0f0f0;
      vertical-align: middle;
    }

    tr.current {
      background: #fffbe6;
    }

    tr:last-child td {
      border-bottom: none;
    }
  }

  .overlay-footer {
    padding: 16px 20px;
    border-top: 1px solid #f0f0f0;
    background: #fafafa;
    display: flex;
    justify-content: flex-end;
    border-radius: 0 0 8px 8px;
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