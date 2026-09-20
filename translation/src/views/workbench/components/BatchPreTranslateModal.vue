<template>
  <Modal
    :modalVisible="visible"
    modalTitle="批量预翻译"
    modalWidth="80%"
    :fullFlag="false"
    :okLoading="submitting"
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

      <!-- 配置区域（含归档补充项） -->
      <div class="section config-section">
        <div class="section-title">执行配置</div>
        <a-form ref="execForm" :model="model" class="exec-form">
          <a-row v-if="model.stages.archive" :gutter="16" align="top">
            <a-col :span="8">
              <a-form-item
                label="IP"
                name="archiveIp"
                :rules="[{ required: true, message: '请选择IP!' }]"
              >
                <a-select
                  v-model:value="model.archiveIp"
                  :options="ipOptions"
                  placeholder="请选择IP"
                  allowClear
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="归档方式"
                name="archiveMode"
                :rules="[{ required: true, message: '请选择归档方式!' }]"
              >
                <a-select
                  v-model:value="model.archiveMode"
                  :options="archiveModes"
                  placeholder="请选择"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16" align="top">
            <a-col :span="8">
              <a-form-item
                label="翻译方式"
                name="translatePriority"
                :rules="[{ required: true, message: '请选择翻译方式!' }]"
              >
                <a-select
                  v-model:value="model.translatePriority"
                  :options="priorityOptions"
                  style="width: 100%"
                  placeholder="请选择"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="并发数"
                name="concurrency"
                :rules="[
                  { required: true, message: '请输入并发数!' },
                  { type: 'number', min: 1, max: 100, message: '并发数须在 1~100!' }
                ]"
              >
                <a-tooltip title="全局同时执行的工作单元数（1~100）；含主任务阶段与拆分子块；每个任务内阶段仍按顺序串行">
                  <a-input-number
                    v-model:value="model.concurrency"
                    :min="1"
                    :max="100"
                    :style="{ width: '100%' }"
                  />
                </a-tooltip>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="重试次数"
                name="maxRetries"
                :rules="[
                  { required: true, message: '请输入重试次数!' },
                  { type: 'number', min: 1, max: 100, message: '重试次数须在 1~100!' }
                ]"
              >
                <a-input-number
                  v-model:value="model.maxRetries"
                  :min="1"
                  :max="100"
                  :style="{ width: '100%' }"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <div class="split-enhance-block">
            <div class="split-pill-row">
              <a-tooltip title="某阶段词条超过拆分上限时，拆成多个子任务块并发执行，降低单次接口压力">
                <button
                  type="button"
                  class="split-pill"
                  :class="{ active: model.enhancedSplit }"
                  @click="toggleEnhancedSplit"
                >
                  {{ model.enhancedSplit ? '已启用 · 拆分增强' : '拆分增强' }}
                </button>
              </a-tooltip>
              <span v-if="!model.enhancedSplit" class="split-pill-hint">
                大数据量任务建议开启，降低单次接口压力
              </span>
            </div>
            <a-row v-if="model.enhancedSplit" :gutter="16" align="top" class="split-limit-row">
              <a-col :span="8">
                <a-form-item
                  label="拆分上限（条）"
                  name="splitLimit"
                  :rules="[
                    { required: true, message: '请输入拆分上限!' },
                    { type: 'number', min: 20, max: 2000, message: '拆分上限须在 20~2000!' }
                  ]"
                >
                  <a-input-number
                    v-model:value="model.splitLimit"
                    :min="20"
                    :max="2000"
                    :style="{ width: '100%' }"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
        </a-form>

        <!-- 校验规则 -->
        <div class="config-item rules-item">
          <label>校验规则</label>
          <RulesDropdown :options="rulesOptions" @update:options="rulesOptions = $event" />
        </div>
      </div>
    </div>
  </Modal>
</template>

<script>
import Modal from '@/components/modal/index.vue'
import RulesDropdown from '@/components/Dropdown/rulesDropdown.vue'
import { message } from 'ant-design-vue'
import commonParam from '@/constants/commonParam.js'
import { TRANSLATE_PRIORITY_OPTIONS } from '@/constants/translatePriority'
import { ARCHIVE_MODE, ARCHIVE_MODES } from '@/constants/batchPreTranslateSteps'
import { setModalAriaHidden } from '@/utils/domUtils'
import { useBatchPreTranslate } from '@/composables/workbench/useBatchPreTranslate'
import { getI18nAdress } from '@/http/api/workbench'

export default {
  components: { Modal, RulesDropdown },
  emits: ['update:visible', 'close', 'update:tasks', 'complete'],
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
          translateExamine: true,
          archive: false
        },
        translatePriority: 'shuyuku',
        concurrency: 1,
        maxRetries: 3,
        enhancedSplit: false,
        splitLimit: 1000,
        archiveIp: null,
        archiveMode: ARCHIVE_MODE.WRITE
      },
      rulesOptions: commonParam.rulesOptions.map((item) => ({ ...item })),
      priorityOptions: TRANSLATE_PRIORITY_OPTIONS,
      archiveModes: ARCHIVE_MODES,
      ipOptions: [],
      stageConfigs: [
        { key: 'entryExamine', label: '词条审核' },
        { key: 'preTranslate', label: '翻译' },
        { key: 'translateExamine', label: '翻译审核' },
        { key: 'archive', label: '归档' }
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
    },
    'model.stages.archive'(val) {
      if (val) {
        this.loadIps()
      } else {
        this.model.archiveIp = null
        this.$nextTick(() => this.$refs.execForm?.clearValidate?.(['archiveIp', 'archiveMode']))
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

    toggleEnhancedSplit() {
      this.model.enhancedSplit = !this.model.enhancedSplit
    },

    loadIps() {
      this.ipOptions = []
      getI18nAdress().then((res) => {
        const list = res?.data?.list || []
        this.ipOptions = list.map(item => ({
          label: item.ip,
          value: item.ip
        }))
      }).catch((err) => {
        message.error('获取IP失败: ' + (err?.message || err))
      })
    },

    async handleExecute() {
      if (!this.canExecute) {
        if (this.filteredTasks.length === 0) {
          message.warning('请先选择任务')
        } else if (!this.isContinuous(this.model.stages)) {
          message.warning('阶段选择必须连续，不能有空洞')
        }
        return
      }
      try {
        await this.$refs.execForm.validate()
      } catch {
        return
      }

      this.submitting = true

      const config = {
        tasks: this.filteredTasks,
        stages: { ...this.model.stages },
        translatePriority: this.model.translatePriority,
        concurrency: this.model.concurrency,
        maxRetries: this.model.maxRetries,
        enhancedSplit: this.model.enhancedSplit,
        splitLimit: this.model.splitLimit,
        rules: this.rulesOptions,
        archiveIp: this.model.archiveIp,
        archiveMode: this.model.archiveMode
      }

      try {
        // 启动全局进度遮罩
        this.$store.dispatch('batchProgress/start', { config, tasks: this.filteredTasks })

        // 关闭配置模态框
        this.$emit('update:visible', false)
        this.$emit('close')

        // 后台执行
        const { execute } = useBatchPreTranslate()
        await execute(config, this.$store)

        // 完成汇总与「关闭」按钮展示在进度面板中，由用户手动关闭
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
        translateExamine: true,
        archive: false
      }
      this.model.translatePriority = 'shuyuku'
      this.model.concurrency = 1
      this.model.maxRetries = 3
      this.model.enhancedSplit = false
      this.model.splitLimit = 1000
      this.model.archiveIp = null
      this.model.archiveMode = ARCHIVE_MODE.WRITE
      this.ipOptions = []
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
    .exec-form {
      :deep(.ant-form-item) {
        margin-bottom: 12px;
      }
    }

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

    .split-enhance-block {
      margin-top: 4px;
    }

    .split-pill-row {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 10px;
      margin-bottom: 4px;
    }

    .split-pill {
      height: 32px;
      padding: 0 14px;
      border-radius: 16px;
      border: 1px solid #d9d9d9;
      background: #fff;
      color: #666;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.2s;
      line-height: 30px;

      &:hover {
        border-color: #1677ff;
        color: #1677ff;
      }

      &.active {
        border-color: #1677ff;
        background: #e6f4ff;
        color: #1677ff;
        font-weight: 500;
      }
    }

    .split-pill-hint {
      font-size: 12px;
      color: #999;
      line-height: 32px;
    }

    .split-limit-row {
      margin-top: 8px;
    }
  }

  .modal-footer {
    margin-top: 16px;
  }
}
</style>