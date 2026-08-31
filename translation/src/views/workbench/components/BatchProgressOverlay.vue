<template>
  <div v-if="visible" ref="overlay" class="batch-progress-overlay">
    <div class="overlay-backdrop" @click="preventClick" />
    <div class="progress-panel">
      <div class="panel-header">
        <h3>批量预翻译执行进度</h3>
        <a-badge :status="phase === 'running' ? 'processing' : 'success'" />
      </div>

      <div class="progress-list" ref="progressList">
        <div
          v-for="p in progresses"
          :key="p.taskId"
          class="progress-item"
          :class="{ current: p.currentStep }"
        >
          <div class="task-info">
            <span class="task-name">{{ p.taskName }}</span>
            <span v-if="p.currentStep" class="running-badge">执行中: {{ currentStepLabel(p) }}</span>
          </div>
          <div class="stages">
            <div v-for="stage in stageOrder" :key="stage" class="stage-block">
              <div class="stage-header">
                <span class="stage-name">{{ stageNameMap[stage] }}</span>
                <StageIcon :status="p.stages[stage]" :retry="p.retryCount" />
              </div>
              <div v-if="p.stageMessages?.[stage]" class="stage-message">{{ p.stageMessages[stage] }}</div>
              <div class="steps">
                <div v-for="step in stageStepsMap[stage]" :key="step.key" class="step-item">
                  <span class="step-label">{{ step.label }}{{ formatStepCount(p, stage, step.key) }}</span>
                  <StageIcon :status="p.steps?.[stage]?.[step.key]" :retry="p.retryCount" />
                </div>
              </div>
            </div>
          </div>
          <div class="status">
            <span v-if="p.error" class="error">{{ p.error }}</span>
            <span v-else-if="isTaskDone(p) && hasWarning(p)" class="warning">部分完成</span>
            <span v-else-if="isTaskDone(p)" class="success">全部完成</span>
          </div>
        </div>
      </div>

      <div v-if="phase === 'completed'" class="panel-footer">
        <span class="summary">
          执行完成：成功({{ completionSummary.success }})
          <template v-if="completionSummary.warningCount > 0">
            ；部分完成/存在告警({{ completionSummary.warningCount }})：{{ completionSummary.warningNames.join('、') }}
          </template>
          <template v-if="completionSummary.failedCount > 0">
            ；失败({{ completionSummary.failedCount }})：{{ completionSummary.failedNames.join('、') }}
          </template>
        </span>
        <a-button type="primary" size="small" @click="handleClose">关闭</a-button>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState, mapGetters } from 'vuex'
import StageIcon from './StageIcon.vue'
import {
  STAGE_ORDER,
  STAGE_NAME_MAP,
  STAGE_STEPS,
  getStageLabel,
  getStepLabel
} from '@/constants/batchPreTranslateSteps'

export default {
  name: 'BatchProgressOverlay',
  components: { StageIcon },
  computed: {
    ...mapState('batchProgress', ['phase', 'progresses']),
    ...mapGetters('batchProgress', ['visible']),
    stageOrder() {
      return STAGE_ORDER
    },
    stageNameMap() {
      return STAGE_NAME_MAP
    },
    stageStepsMap() {
      return STAGE_STEPS
    },
    /**
     * 汇总成功、告警与失败任务数量。
     * @returns {{success: number, warningCount: number, warningNames: string[], failedCount: number, failedNames: string[]}}
     */
    completionSummary() {
      const success = this.progresses.filter(p =>
        Object.values(p.stages).every(s => s === 'success' || s === 'skipped')
      ).length
      const warning = this.progresses.filter(p =>
        !Object.values(p.stages).includes('failed') && Object.values(p.stages).includes('warning')
      )
      const failed = this.progresses.filter(p =>
        Object.values(p.stages).includes('failed')
      )
      return {
        success,
        warningCount: warning.length,
        warningNames: warning.map(p => p.taskName),
        failedCount: failed.length,
        failedNames: failed.map(p => p.taskName)
      }
    }
  },
  mounted() {
    this.adjustPanelHeight()
    window.addEventListener('resize', this.adjustPanelHeight)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.adjustPanelHeight)
  },
  methods: {
    /**
     * 计算某子步骤的词条计数展示文案（如「 (3条)」）。
     * 仅在该子步骤已开始（running）或已成功（success）且有计数时展示。
     * @param {Object} p 任务进度对象
     * @param {string} stageKey 阶段 key
     * @param {string} stepKey 子步骤 key
     * @returns {string}
     */
    formatStepCount(p, stageKey, stepKey) {
      const status = p.steps?.[stageKey]?.[stepKey]
      if (status !== 'running' && status !== 'success') return ''
      const count = p.stepCounts?.[stageKey]?.[stepKey]
      if (count === undefined || count === null) return ''
      return ` (${count}条)`
    },
    /**
     * 构造蓝色「执行中」文字：阶段名 · 子步骤名，避免跨阶段同名子步骤歧义。
     * @param {Object} p 任务进度对象
     * @returns {string}
     */
    currentStepLabel(p) {
      if (!p.currentStep) return ''
      const stageName = getStageLabel(p.currentStep.stage)
      const stepLabel = getStepLabel(p.currentStep.stage, p.currentStep.step)
      return `${stageName} · ${stepLabel}`
    },
    /**
     * 判断任务是否已结束，warning 视为已结束但不等同于全量成功。
     * @param {Object} p 任务进度对象
     * @returns {boolean}
     */
    isTaskDone(p) {
      const enabled = Object.keys(p.stages).filter(k => p.stages[k] !== 'pending' && p.stages[k] !== 'skipped')
      return enabled.length > 0 && enabled.every(k => p.stages[k] === 'success' || p.stages[k] === 'warning' || p.stages[k] === 'skipped')
    },
    /**
     * 判断任务是否包含阶段级告警。
     * @param {Object} p 任务进度对象
     * @returns {boolean}
     */
    hasWarning(p) {
      return Object.values(p.stages).includes('warning')
    },
    preventClick(e) {
      e.stopPropagation()
    },
    adjustPanelHeight() {
      this.$nextTick(() => {
        const root = this.$refs.overlay
        if (!root) return
        const panel = root.querySelector('.progress-panel')
        const list = root.querySelector('.progress-list')
        if (!panel || !list) return

        const headerHeight = 56 // panel-header 高度
        const padding = 32 // padding-top + padding-bottom
        const footerHeight = this.phase === 'completed' ? 50 : 0 // 完成时底部汇总/按钮区域
        const maxHeight = window.innerHeight * 0.8
        const availableHeight = maxHeight - headerHeight - padding - footerHeight

        // 面板最大高度限制为 80vh
        panel.style.maxHeight = `${maxHeight}px`

        // 列表区域最大高度 = 80vh - header - padding - footer
        list.style.maxHeight = `${availableHeight}px`
      })
    },
    // 仅当用户点击「关闭」时才重置进度状态并关闭面板
    handleClose() {
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
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: flex-end;
  padding: 20px;
  box-sizing: border-box;
  pointer-events: none; // 遮罩层不阻塞，但 overlay-backdrop 会阻塞

  .overlay-backdrop {
    position: absolute;
    inset: 0;
    // 遮罩层阻塞点击，防止穿透到底层
  }

  .progress-panel {
    pointer-events: auto;
    width: 60%;
    min-width: 800px;
    background: white;
    border-radius: 8px;
    border: 1px solid #e8e8e8;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    overflow: hidden;
    margin-top: 60px;
    max-height: 80vh; // 面板最大高度 80vh
    display: flex;
    flex-direction: column;

    .panel-header {
      padding: 12px 16px;
      border-bottom: 1px solid #f0f0f0;
      background: #fafafa;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-radius: 8px 8px 0 0;
      flex-shrink: 0;

      h3 {
        margin: 0;
        font-size: 14px;
        font-weight: 600;
        color: #333;
      }
    }

    .progress-list {
      flex: 1;
      overflow-y: auto;
      padding: 8px 12px;
      // 高度由父级 flex 布局自动计算，内容少时自适应，超出滚动

      .progress-item {
        display: flex;
        align-items: flex-start;
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
          padding-top: 2px;

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
          gap: 20px;
          flex-shrink: 0;

          .stage-block {
            display: flex;
            flex-direction: column;
            gap: 4px;

            .stage-message {
              max-width: 220px;
              color: #d48806;
              font-size: 11px;
              line-height: 1.4;
              white-space: normal;
            }

            .stage-header {
              display: flex;
              align-items: center;
              gap: 6px;
              font-size: 12px;
              color: #333;
              font-weight: 600;
              white-space: nowrap;

              .stage-name {
                flex-shrink: 0;
              }
            }

            .steps {
              display: flex;
              flex-direction: column;
              gap: 2px;
              padding-left: 4px;

              .step-item {
                display: flex;
                align-items: center;
                gap: 6px;
                font-size: 12px;
                color: #666;
                white-space: nowrap;

                .step-label {
                  flex-shrink: 0;
                  min-width: 0;
                }
              }
            }
          }
        }

        .status {
          flex-shrink: 0;
          width: 80px;
          text-align: right;
          font-size: 12px;
          padding-top: 2px;
        }
      }
    }

    .panel-footer {
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 12px 16px;
      border-top: 1px solid #f0f0f0;
      background: #fafafa;

      .summary {
        font-size: 13px;
        color: #333;
        line-height: 1.5;
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
.warning { color: #d48806; font-size: 12px; }
.success { color: #52c41a; font-size: 12px; }
</style>
