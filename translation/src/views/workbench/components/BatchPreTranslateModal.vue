<template>
  <Modal
    :visible="visible"
    modalTitle="批量预翻译"
    :modalWidth="900"
    :fullFlag="false"
    :okLoading="executing"
    @handleClose="handleClose"
    @handleOK="handleExecute"
    @afterClose="afterClose"
  >
    <div class="batch-pre-translate-modal">
      <!-- 任务列表预览 -->
      <div class="section task-list-section">
        <div class="section-title">已选任务 ({{ tasks.length }})</div>
        <div class="task-chips" v-if="tasks.length > 0">
          <a-tag
            v-for="task in tasks"
            :key="task.id"
            :color="task.color || 'blue'"
            closable
            @close="removeTask(task)"
          >
            {{ task.name }}
          </a-tag>
        </div>
        <div v-else class="empty-hint">请先在工作台勾选任务</div>
      </div>

      <!-- 阶段选择 -->
      <div class="section stage-section">
        <div class="section-title">执行阶段</div>
        <div class="stage-row">
          <label
            v-for="stage in stageConfigs"
            :key="stage.key"
            class="stage-label"
            :class="{
              disabled: !isToggleable(stage.key),
              'blocked-by-dep': stage.key === 'translateExamine' && !model.stages.preTranslate
            }"
          >
            <input
              type="checkbox"
              v-model="model.stages[stage.key]"
              :disabled="!isToggleable(stage.key) || (stage.key === 'translateExamine' && !model.stages.preTranslate)"
              @change="onStageChange(stage.key)"
            >
            <span :class="{ 'stage-required': stage.required }">{{ stage.label }}</span>
          </label>
        </div>
        <div v-if="!isContinuous(model.stages)" class="stage-error">
          阶段选择必须连续，不能有空洞（如：勾选词条审核和翻译审核但不勾选翻译）
        </div>
      </div>

      <!-- 配置区域 -->
      <div class="section config-section">
        <div class="section-title">执行配置</div>
        <a-row :gutter="16" align="middle">
          <a-col :span="8">
            <div class="config-item">
              <label>翻译方式</label>
              <a-select v-model:value="model.translatePriority" :options="priorityOptions" allowClear style="width: 100%" placeholder="请选择">
              </a-select>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="config-item">
              <label>并发数</label>
              <a-input-number v-model:value="model.concurrency" :min="1" :max="5" :style="{ width: '100%' }" />
            </div>
          </a-col>
          <a-col :span="8">
            <div class="config-item">
              <label>重试次数</label>
              <a-input-number v-model:value="model.maxRetries" :min="1" :max="100" :style="{ width: '100%' }" />
            </div>
          </a-col>
        </a-row>

        <!-- 校验规则 -->
        <div class="config-item rules-item">
          <label>校验规则</label>
          <RulesDropdown :options="rulesOptions" @update:options="rulesOptions = $event" />
        </div>
      </div>

      <!-- 执行进度 -->
      <div class="section progress-section" v-if="executing || progresses.length > 0">
        <div class="section-title">
          执行进度
          <a-button v-if="executing" type="primary" size="small" @click="abortExecution" style="margin-left: 12px;">
            停止执行
          </a-button>
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
              <tr v-for="p in progresses" :key="p.taskId" :class="{ 'current-task': p.currentStage }">
                <td>{{ p.taskName }}</td>
                <td><StageIcon :status="p.stages.entryExamine" :retry="p.retryCount" /></td>
                <td><StageIcon :status="p.stages.preTranslate" :retry="p.retryCount" /></td>
                <td><StageIcon :status="p.stages.translateExamine" :retry="p.retryCount" /></td>
                <td>
                  <span v-if="p.error" class="error-text">{{ p.error }}</span>
                  <span v-else-if="p.currentStage" class="running-text">执行中: {{ stageLabelMap[p.currentStage] }}</span>
                  <span v-else-if="isTaskComplete(p)" class="success-text">全部完成</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <template #footer>
      <div style="display: flex; justify-content: flex-end; gap: 8px; width: 100%;">
        <a-button @click="handleClose" :disabled="executing" v-if="!executing">取消</a-button>
        <a-button type="primary" @click="handleExecute" :loading="executing" :disabled="!canExecute || executing">
          {{ executing ? '执行中...' : '开始执行' }}
        </a-button>
      </div>
    </template>
  </Modal>
</template>

<script>
import Modal from '@/components/modal/index.vue'
import RulesDropdown from '@/components/Dropdown/rulesDropdown.vue'
import { message } from 'ant-design-vue'
import commonParam from '@/constants/commonParam.js'
import { useBatchPreTranslate } from '@/composables/workbench/useBatchPreTranslate'
import { setModalAriaHidden } from '@/utils/domUtils'

const StageIcon = {
  props: ['status', 'retry'],
  setup(props) {
    const icons = {
      pending: { icon: '⏳', class: 'pending', title: '待执行' },
      running: { icon: '⟳', class: 'running', title: '执行中' },
      success: { icon: '✓', class: 'success', title: '成功' },
      failed: { icon: '✗', class: 'failed', title: '失败' },
      skipped: { icon: '⊘', class: 'skipped', title: '已跳过' }
    }
    return () => {
      const s = icons[props.status] || icons.pending
      return (
        <span class={`stage-icon ${s.class}`} title={s.title}>
          {s.icon}
          {props.status === 'failed' && props.retry > 0 && <span class="retry-badge">×{props.retry}</span>}
        </span>
      )
    }
  }
}

export default {
  components: { Modal, RulesDropdown, StageIcon },
  emits: ['update:visible', 'close', 'complete'],
  props: {
    visible: { type: Boolean, default: false },
    tasks: { type: Array, default: () => [] }
  },
  data() {
    return {
      model: {
        stages: {
          entryExamine: true,
          preTranslate: true,
          translateExamine: true
        },
        translatePriority: 'shuyuku',
        concurrency: 1,
        maxRetries: 3
      },
      rulesOptions: commonParam.rulesOptions,
      priorityOptions: [
        { label: '术语库', value: 'shuyuku' },
        { label: 'DeepL翻译', value: 'deepl' },
        { label: '有道翻译', value: 'youdao' },
        { label: '百度翻译', value: 'baidu' },
        { label: 'Google翻译', value: 'google' },
        { label: '本地模型', value: 'module' },
        { label: '综合优先级', value: 'synthesis' }
      ],
      stageConfigs: [
        { key: 'entryExamine', label: '词条审核', required: false },
        { key: 'preTranslate', label: '翻译(预翻译)', required: true },
        { key: 'translateExamine', label: '翻译审核', required: false }
      ],
      stageLabelMap: {
        entryExamine: '词条审核',
        preTranslate: '翻译(预翻译)',
        translateExamine: '翻译审核'
      },
      executing: false,
      progresses: []
    }
  },
  computed: {
    canExecute() {
      return this.tasks.length > 0 && this.isContinuous(this.model.stages)
    }
  },
  watch: {
    visible(val) {
      if (val) {
        setModalAriaHidden(this, document)
        this.resetState()
      }
    }
  },
  methods: {
    isContinuous(stages) {
      const selected = this.stageConfigs.filter(s => stages[s.key]).map(s => s.key)
      if (selected.length <= 1) return true
      const idx = this.stageConfigs.map(s => selected.includes(s.key))
      const first = idx.indexOf(true)
      const last = idx.lastIndexOf(true)
      return idx.slice(first, last + 1).every(v => v)
    },

    getToggleableStages(stages) {
      const selected = this.stageConfigs.filter(s => stages[s.key]).map(s => s.key)
      if (selected.length === 0) return this.stageConfigs.map(s => s.key)
      return [selected[0], selected[selected.length - 1]]
    },

    isToggleable(key) {
      const toggleable = this.getToggleableStages(this.model.stages)
      return toggleable.includes(key)
    },

    onStageChange(key) {
      if (!this.isContinuous(this.model.stages)) {
        this.fixContinuity(key)
      }
    },

    fixContinuity(changedKey) {
      const stages = this.model.stages
      const selected = this.stageConfigs.filter(s => stages[s.key]).map(s => s.key)

      if (selected.length === 0) return

      const first = selected[0]
      const last = selected[selected.length - 1]
      const keys = this.stageConfigs.map(s => s.key)
      const firstIdx = keys.indexOf(first)
      const lastIdx = keys.indexOf(last)

      for (let i = firstIdx; i <= lastIdx; i++) {
        stages[keys[i]] = true
      }
    },

    removeTask(task) {
      this.$emit('update:tasks', this.tasks.filter(t => t.id !== task.id))
    },

    async handleExecute() {
      if (!this.canExecute) return

      this.executing = true

      const config = {
        tasks: this.tasks,
        stages: { ...this.model.stages },
        translatePriority: this.model.translatePriority,
        concurrency: this.model.concurrency,
        maxRetries: this.model.maxRetries,
        rules: this.rulesOptions
      }

      try {
        const { execute } = useBatchPreTranslate()
        this.progresses = await execute(config, (prog) => {
          this.progresses = [...prog]
        })
        message.success('批量预翻译执行完成')
        this.$emit('complete', this.progresses)
      } catch (err) {
        message.error('执行失败: ' + err.message)
      } finally {
        this.executing = false
      }
    },

    abortExecution() {
      const { abort } = useBatchPreTranslate()
      abort()
      this.executing = false
      message.warning('已停止执行')
    },

    handleClose() {
      if (this.executing) return
      this.$emit('update:visible', false)
      this.$emit('close')
    },

    afterClose() {
      this.resetState()
    },

    resetState() {
      this.executing = false
      this.progresses = []
      this.model.stages = {
        entryExamine: true,
        preTranslate: true,
        translateExamine: true
      }
      this.model.translatePriority = 'shuyuku'
      this.model.concurrency = 1
      this.model.maxRetries = 3
    },

    isTaskComplete(p) {
      const enabled = Object.keys(p.stages).filter(k => p.stages[k] !== 'pending' && p.stages[k] !== 'skipped')
      return enabled.length > 0 && enabled.every(k => p.stages[k] === 'success' || p.stages[k] === 'skipped')
    }
  }
}
</script>

<style scoped lang="less">
.batch-pre-translate-modal {
  padding: 8px 0;
  max-height: 70vh;
  overflow-y: auto;

  .section {
    margin-bottom: 20px;
    padding: 0 4px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .section-title {
    font-weight: 600;
    font-size: 14px;
    color: #333;
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  /* 任务列表 */
  .task-list-section {
    .task-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      max-height: 120px;
      overflow-y: auto;
      padding: 4px;
      background: #fafafa;
      border: 1px solid #e8e8e8;
      border-radius: 4px;
    }
    .empty-hint {
      color: #999;
      font-size: 13px;
      padding: 8px;
    }
  }

  /* 阶段选择 */
  .stage-section {
    .stage-row {
      display: flex;
      gap: 24px;
      align-items: center;
    }
    .stage-label {
      display: flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      font-size: 13px;
      color: #333;
      padding: 6px 10px;
      border-radius: 4px;
      transition: background 0.2s;

      &.disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      &.blocked-by-dep {
        opacity: 0.5;
        cursor: not-allowed;
      }

      &:hover:not(.disabled) {
        background: #f0f5ff;
      }

      input {
        width: 16px;
        height: 16px;
      }

      .stage-required {
        color: #1890ff;
        font-weight: 500;
      }
    }
    .stage-error {
      margin-top: 8px;
      color: #ff4d4f;
      font-size: 12px;
    }
  }

  /* 配置区域 */
  .config-section {
    .config-item {
      margin-bottom: 12px;

      label {
        display: block;
        font-size: 13px;
        color: #666;
        margin-bottom: 4px;
      }

      &.rules-item {
        margin-top: 8px;
      }
    }
  }

  /* 进度表格 */
  .progress-section {
    .progress-table-wrap {
      max-height: 300px;
      overflow-y: auto;
      border: 1px solid #e8e8e8;
      border-radius: 4px;
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

      tr.current-task {
        background: #fffbe6;
      }

      tr:last-child td {
        border-bottom: none;
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

  .error-text { color: #ff4d4f; font-size: 12px; }
  .running-text { color: #1890ff; font-size: 12px; }
  .success-text { color: #52c41a; font-size: 12px; }
}
</style>
</script>
</style>