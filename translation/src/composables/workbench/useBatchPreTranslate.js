import { ref, computed } from 'vue'
import { loading, startLoading, endLoading } from '@/composables/useLoading'
import { getEntryInfoList, updateEntryList, preTranslate } from '@/http/api/workbench'
import { classifyArr, verifyArray_workbench, getMethods, clearCellErrorsForRecords, revalidateLoaded } from '@/utils/validationUtils'
import { message } from 'ant-design-vue'

const STAGES = ['entryExamine', 'preTranslate', 'translateExamine']

export function useBatchPreTranslate() {
  const executing = ref(false)
  const aborted = ref(false)
  const progresses = ref([])
  const currentTaskIndex = ref(0)

  function createInitialProgress(tasks) {
    return tasks.map(t => ({
      taskId: t.id,
      taskName: t.name,
      stages: {
        entryExamine: 'pending',
        preTranslate: 'pending',
        translateExamine: 'pending'
      },
      currentStage: null,
      error: null,
      retryCount: 0
    }))
  }

  function isStageEnabled(stages, key) {
    return stages[key] === true
  }

  function getEnabledStages(stages) {
    return STAGES.filter(k => isStageEnabled(stages, k))
  }

  function isContinuous(stages) {
    const selected = STAGES.filter(k => stages[k])
    if (selected.length <= 1) return true
    const idx = STAGES.map(k => selected.includes(k))
    const first = idx.indexOf(true)
    const last = idx.lastIndexOf(true)
    return idx.slice(first, last + 1).every(v => v)
  }

  function getToggleableStages(stages) {
    const selected = STAGES.filter(k => stages[k])
    if (selected.length === 0) return STAGES
    return [selected[0], selected[selected.length - 1]]
  }

  async function sleep(ms) {
    return new Promise(r => setTimeout(r, ms))
  }

  async function runWithRetry(fn, maxRetries, onRetry) {
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        return await fn()
      } catch (e) {
        if (attempt === maxRetries) throw e
        await onRetry(attempt, e)
        await sleep(attempt * 1000)
      }
    }
  }

  async function executeEntryExamine(task, rules, progress, maxRetries) {
    progress.stages.entryExamine = 'running'
    progress.currentStage = 'entryExamine'

    await runWithRetry(async () => {
      const params = { taskID: task.id, entryState: '1', entry: '' }
      const res = await getEntryInfoList(params, ['1', '2'])
      const entries = res.data.list

      if (entries.length === 0) return

      const transCol = task.translateType
      const methods = getMethods({ rulesOptions: rules })

      clearCellErrorsForRecords({ cellErrors: {} }, entries.map(e => e.id))
      const verifyResult = await classifyArr({ rulesOptions: rules }, entries, transCol, methods)

      if (verifyResult.errorIds.size > 0) {
        throw new Error(`校验不通过 ${verifyResult.errorIds.size} 条`)
      }

      const updateArr = entries.map(e => ({ ...e, auditState: 1, entryState: 3 }))
      await updateEntryList({ taskID: task.id }, updateArr)
    }, maxRetries, (attempt, err) => {
      progress.retryCount = attempt
      console.log(`[${task.name}] 词条审核重试 ${attempt}/${maxRetries}:`, err.message)
    })

    progress.stages.entryExamine = 'success'
  }

  async function executePreTranslate(task, priority, rules, progress, maxRetries) {
    progress.stages.preTranslate = 'running'
    progress.currentStage = 'preTranslate'

    await runWithRetry(async () => {
      const params = { taskID: task.id, entryState: '3', entry: '' }
      const res = await getEntryInfoList(params, ['0'])
      const entries = res.data.list

      if (entries.length === 0) return

      const preParams = { taskID: task.id, priority }
      await preTranslate(preParams, entries)

      const transCol = task.translateType
      const methods = getMethods({ rulesOptions: rules })

      clearCellErrorsForRecords({ cellErrors: {} }, entries.map(e => e.id))
      const verifyResult = await verifyArray_workbench({ rulesOptions: rules }, entries, transCol, methods)

      if (verifyResult.errorIds.size > 0) {
        throw new Error(`预翻译校验不通过 ${verifyResult.errorIds.size} 条`)
      }

      const updateArr = entries.map(e => ({ ...e, [transCol]: e[transCol], [transCol + 'State']: '1' }))
      await updateEntryList({ taskID: task.id }, updateArr)
    }, maxRetries, (attempt, err) => {
      progress.retryCount = attempt
      console.log(`[${task.name}] 预翻译重试 ${attempt}/${maxRetries}:`, err.message)
    })

    progress.stages.preTranslate = 'success'
  }

  async function executeTranslateExamine(task, rules, progress, maxRetries) {
    progress.stages.translateExamine = 'running'
    progress.currentStage = 'translateExamine'

    await runWithRetry(async () => {
      const params = { taskID: task.id, entryState: '3', entry: '' }
      const res = await getEntryInfoList(params, ['1'])
      const entries = res.data.list

      if (entries.length === 0) return

      const transCol = task.translateType
      const methods = getMethods({ rulesOptions: rules })

      clearCellErrorsForRecords({ cellErrors: {} }, entries.map(e => e.id))
      const verifyResult = await classifyArr({ rulesOptions: rules }, entries, transCol, methods)

      if (verifyResult.errorIds.size > 0) {
        throw new Error(`翻译审核校验不通过 ${verifyResult.errorIds.size} 条`)
      }

      const updateArr = entries.map(e => ({ ...e, auditState: 1, [transCol + 'State']: '3' }))
      await updateEntryList({ taskID: task.id }, updateArr)
    }, maxRetries, (attempt, err) => {
      progress.retryCount = attempt
      console.log(`[${task.name}] 翻译审核重试 ${attempt}/${maxRetries}:`, err.message)
    })

    progress.stages.translateExamine = 'success'
  }

  const stageExecutors = {
    entryExamine: executeEntryExamine,
    preTranslate: executePreTranslate,
    translateExamine: executeTranslateExamine
  }

  async function execute(config, onProgress) {
    if (executing.value) return
    if (!isContinuous(config.stages)) {
      throw new Error('阶段选择必须连续，不能有空洞')
    }

    executing.value = true
    aborted.value = false
    progresses.value = createInitialProgress(config.tasks)
    currentTaskIndex.value = 0
    onProgress?.(progresses.value)

    const enabledStages = getEnabledStages(config.stages)
    const maxRetries = config.maxRetries ?? 3

    try {
      for (let i = 0; i < config.tasks.length; i++) {
        if (aborted.value) break

        const task = config.tasks[i]
        currentTaskIndex.value = i
        const progress = progresses.value[i]

        for (const stageKey of enabledStages) {
          if (aborted.value) break

          progress.currentStage = stageKey
          onProgress?.(progresses.value)

          try {
            await stageExecutors[stageKey](task, config, progress, maxRetries)
            progress.stages[stageKey] = 'success'
          } catch (err) {
            progress.stages[stageKey] = 'failed'
            progress.error = err.message

            const stageIdx = enabledStages.indexOf(stageKey)
            for (let j = stageIdx + 1; j < enabledStages.length; j++) {
              progress.stages[enabledStages[j]] = 'skipped'
            }
            break
          }

          onProgress?.(progresses.value)
        }
      }
    } finally {
      executing.value = false
      onProgress?.(progresses.value)
    }

    return progresses.value
  }

  function abort() {
    aborted.value = true
  }

  function reset() {
    executing.value = false
    aborted.value = false
    progresses.value = []
    currentTaskIndex.value = 0
  }

  return {
    executing,
    aborted,
    progresses,
    currentTaskIndex,
    execute,
    abort,
    reset,
    isContinuous,
    getToggleableStages,
    STAGES
  }
}