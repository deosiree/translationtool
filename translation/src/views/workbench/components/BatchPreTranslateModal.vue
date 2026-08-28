<template>
  <Modal
    :visible="visible"
    modalTitle="批量预翻译"
    :modalWidth="'900px'"
    :fullFlag="false"
    @handleClose="handleClose"
    @handleOK="handleExecute"
    @afterClose="afterClose"
  >
    <div class="batch-pre-translate-modal">
      <!-- 任务列表预览 -->
      <div class="section task-list-section">
        <div class="section-title">已选任务 ({{ filteredTasks.length }})</div>
        <div class="task-chips" v-if="filteredTasks.length > 0">
          <a-tag
            v-for="task in filteredTasks"
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
            :class="{ disabled: !isToggleable(stage.key) }"
          >
            <input
              type="checkbox"
              v-model="model.stages[stage.key]"
              :disabled="!isToggleable(stage.key)"
              @change="onStageChange(stage.key)"
            >
            <span>{{ stage.label }}</span>
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
    </div>

    <template #footer>
      <div class="modal-footer" style="display: flex; justify-content: flex-end; gap: 8px; width: 100%;">
        <a-button @click="handleClose">取消</a-button>
        <a-button type="primary" @click="handleExecute" :disabled="!canExecute" :loading="submitting">
          开始执行
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
import { setModalAriaHidden } from '@/utils/domUtils'
import { execute } from '@/composables/workbench/useBatchPreTranslate'

export default {
  components: { Modal, RulesDropdown },
  emits: ['update:visible', 'close', 'update:tasks'],
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
        { key: 'entryExamine', label: '词条审核' },
        { key: 'preTranslate', label: '翻译(预翻译)' },
        { key: 'translateExamine', label: '翻译审核' }
      ],
      submitting: false
    }
  },
  computed: {
    filteredTasks() {
      return (this.tasks || []).filter(t => t && t.id && !t.isBranch)
    },
    canExecute() {
      return this.filteredTasks.length > 0 && this.isContinuous(this.model.stages)
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
      const keys = this.stageConfigs.map(s => s.key)
      const selected = keys.filter(k => stages[k])
      if (selected.length === 0) return keys
      const firstIdx = keys.indexOf(selected[0])
      const lastIdx = keys.indexOf(selected[selected.length - 1])
      const toggleable = [keys[firstIdx], keys[lastIdx]]
      if (firstIdx > 0) toggleable.push(keys[firstIdx - 1])
      if (lastIdx < keys.length - 1) toggleable.push(keys[lastIdx + 1])
      return [...new Set(toggleable)]
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

      this.submitting = true

      const config = {
        tasks: this.filteredTasks,
        stages: { ...this.model.stages },
        translatePriority: this.model.translatePriority,
        concurrency: this.model.concurrency,
        maxRetries: this.model.maxRetries,
        rules: this.rulesOptions
      }

      try {
        // 启动全局进度遮罩
        this.$store.dispatch('batchProgress/start', { config, tasks: this.filteredTasks })

        // 关闭配置模态框
        this.$emit('update:visible', false)
        this.$emit('close')

        // 后台执行
        await execute(config, this.$store)

        message.success('批量预翻译执行完成')
        this.$emit('complete')
      } catch (err) {
        message.error('执行失败: ' + err.message)
        // 执行失败也要关闭遮罩
        this.$store.dispatch('batchProgress/complete')
      } finally {
        this.submitting = false
      }
    },

    handleClose() {
      if (this.$store.getters['batchProgress/isRunning']) return
      this.$emit('update:visible', false)
      this.$emit('close')
    },

    afterClose() {
      this.resetState()
    },

    resetState() {
      this.submitting = false
      this.model.stages = {
        entryExamine: true,
        preTranslate: true,
        translateExamine: true
      }
      this.model.translatePriority = 'shuyuku'
      this.model.concurrency = 1
      this.model.maxRetries = 3
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

      &:hover:not(.disabled) {
        background: #f0f5ff;
      }

      input {
        width: 16px;
        height: 16px;
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

  .modal-footer {
    margin-top: 16px;
  }
}
</style>