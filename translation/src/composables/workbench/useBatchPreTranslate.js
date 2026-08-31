import { getEntryInfoList, updateEntryList, preTranslate } from '@/http/api/workbench'
import { classifyArr, getMethods, clearCellErrorsForRecords } from '@/utils/validationUtils'
import commonParam from '@/constants/commonParam'
import { getTranslatePriorityLabel } from '@/constants/translatePriority'
import {
  STAGE_ORDER,
  STAGE_NAME_MAP,
  STAGE_QUERY_PARAMS,
  getStageSteps
} from '@/constants/batchPreTranslateSteps'

/**
 * 批量预翻译执行编排器。
 *
 * 将「词条审核 / 翻译 / 翻译审核」三个阶段统一建模为 4 个可观察子步骤：
 *   query(查询词条) → selectAll(词条全选) → 动作(批量通过/预翻译) → save(保存)。
 * 每个子步骤之间在开发环境插入约 1 秒延时，使进度遮罩的蓝色「执行中」文字可逐级推进。
 *
 * 接口映射（详见 {@link STAGE_STEPS}）：
 *   - query        → getEntryInfoList（后端）
 *   - selectAll    → 前端本地全选，无后端调用
 *   - batchApprove → 前端本地批量通过，无后端调用
 *   - preTranslate → preTranslate（后端）
 *   - save         → updateEntryList（后端）
 */

/** 开发阶段每个子步骤之间的延时毫秒数；生产环境为 0。 */
const DEFAULT_STEP_DELAY_MS = process.env.NODE_ENV === 'production' ? 0 : 1000

export function useBatchPreTranslate() {
  /**
   * 判断某阶段是否被启用。
   * @param {Object<string, boolean>} stages 阶段启用配置
   * @param {string} key 阶段 key
   * @returns {boolean}
   */
  function isStageEnabled(stages, key) {
    return stages[key] === true
  }

  /**
   * 取被启用的阶段 key 列表（按 STAGE_ORDER 顺序）。
   * @param {Object<string, boolean>} stages 阶段启用配置
   * @returns {string[]}
   */
  function getEnabledStages(stages) {
    return STAGE_ORDER.filter(k => isStageEnabled(stages, k))
  }

  /**
   * 校验阶段选择是否连续（不允许中间出现空洞）。
   * @param {Object<string, boolean>} stages 阶段启用配置
   * @returns {boolean}
   */
  function isContinuous(stages) {
    const selected = STAGE_ORDER.filter(k => stages[k])
    if (selected.length <= 1) return true
    const idx = STAGE_ORDER.map(k => selected.includes(k))
    const first = idx.indexOf(true)
    const last = idx.lastIndexOf(true)
    return idx.slice(first, last + 1).every(v => v)
  }

  /**
   * 计算当前可勾选切换的阶段 key（用于弹窗复选框禁用态）。
   * @param {Object<string, boolean>} stages 阶段启用配置
   * @returns {string[]}
   */
  function getToggleableStages(stages) {
    const selected = STAGE_ORDER.filter(k => stages[k])
    if (selected.length === 0) return STAGE_ORDER
    return [selected[0], selected[selected.length - 1]]
  }

  /**
   * 延时等待。
   * @param {number} ms 毫秒
   * @returns {Promise<void>}
   */
  async function sleep(ms) {
    return new Promise(r => setTimeout(r, ms))
  }

  /**
   * 以指定最大重试次数执行 fn；每次失败后调用 onRetry 并指数退避等待。
   * @param {() => Promise<any>} fn 待执行的异步函数
   * @param {number} maxRetries 最大重试次数
   * @param {(attempt: number, err: Error) => void|Promise<void>} onRetry 重试前回调
   * @returns {Promise<any>} fn 的返回值
   */
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

  /**
   * 构造校验所需的 vm 形状。
   * @param {Object} config 执行配置（含 rules）
   * @returns {{rulesOptions: Array, editableData: Object, classifyLimit: Object}}
   */
  function createValidationVm(config) {
    return {
      rulesOptions: config.rules || [],
      editableData: {},
      classifyLimit: {}
    }
  }

  /**
   * 依据任务 translateType（中文名，如「英文」）解析翻译列与状态列字段映射。
   * @param {{translateType: string}} task 任务
   * @returns {Object} languageMap 中对应语种的映射对象
   * @throws {Error} 语种未知时抛错
   */
  function resolveTransMap(task) {
    const transMap = commonParam.languageMap[task.translateType]
    if (!transMap) {
      throw new Error('未知翻译语种: ' + task.translateType)
    }
    return transMap
  }

  /**
   * 对词条执行校验规则，校验失败抛出中文错误。
   * @param {Object} config 执行配置
   * @param {Object} task 任务
   * @param {Array<Object>} entries 待校验词条
   * @param {string} errorPrefix 校验失败错误信息前缀
   * @returns {Promise<Object>} 解析出的翻译字段映射 transMap
   * @throws {Error} 校验不通过时抛错
   */
  async function validateEntries(config, task, entries, errorPrefix) {
    const transMap = resolveTransMap(task)
    const transCol = transMap.value
    const valVm = createValidationVm(config)
    const methods = getMethods(valVm)

    clearCellErrorsForRecords({ cellErrors: {} }, entries.map(e => e.id))
    const verifyResult = await classifyArr(valVm, entries, transCol, methods)

    if (verifyResult.errorIds.size > 0) {
      throw new Error(`${errorPrefix} ${verifyResult.errorIds.size} 条`)
    }
    return transMap
  }

  /**
   * 推进一个子步骤：置 running → dispatch → sleep(delayMs) → 执行 fn → 置 success 并写计数。
   * 若 fn 抛错则置 failed 后重新抛出，交由阶段级 runWithRetry 处理。
   *
   * @param {Object} progress 任务进度对象（原地修改）
   * @param {string} stageKey 阶段 key
   * @param {string} stepKey 子步骤 key
   * @param {Object} store Vuex store
   * @param {number} delayMs 子步骤延时
   * @param {() => Promise<number>|number} fn 子步骤业务逻辑，返回该子步骤处理的词条数
   * @returns {Promise<number>} fn 返回的词条数
   */
  async function advanceStep(progress, stageKey, stepKey, store, delayMs, fn) {
    progress.currentStep = { stage: stageKey, step: stepKey }
    progress.steps[stageKey][stepKey] = 'running'
    store.dispatch('batchProgress/updateProgress', progress)

    await sleep(delayMs)

    try {
      const count = await fn()
      progress.steps[stageKey][stepKey] = 'success'
      if (typeof count === 'number') {
        progress.stepCounts[stageKey][stepKey] = count
      }
      store.dispatch('batchProgress/updateProgress', progress)
      return count
    } catch (err) {
      progress.steps[stageKey][stepKey] = 'failed'
      store.dispatch('batchProgress/updateProgress', progress)
      throw err
    }
  }

  /**
   * 重试前将某阶段的全部子步骤重置为 pending，使重试在 UI 上从头展示。
   * @param {Object} progress 任务进度对象
   * @param {string} stageKey 阶段 key
   * @param {Object} store Vuex store
   */
  function resetStageSteps(progress, stageKey, store) {
    for (const step of getStageSteps(stageKey)) {
      progress.steps[stageKey][step.key] = 'pending'
    }
    progress.currentStep = null
    store.dispatch('batchProgress/updateProgress', progress)
  }

  /**
   * 将指定子步骤之后的步骤标记为已跳过。
   * @param {Object} progress 任务进度对象
   * @param {string} stageKey 阶段 key
   * @param {string} stepKey 当前子步骤 key
   * @param {Object} store Vuex store
   * @returns {void}
   */
  function skipStepsAfter(progress, stageKey, stepKey, store) {
    const steps = getStageSteps(stageKey)
    const stepIdx = steps.findIndex(step => step.key === stepKey)
    for (let i = stepIdx + 1; i < steps.length; i++) {
      progress.steps[stageKey][steps[i].key] = 'skipped'
    }
    store.dispatch('batchProgress/updateProgress', progress)
  }

  /**
   * 将某阶段 query 之后的其余子步骤标记为 skipped（用于 0 条场景）。
   * @param {Object} progress 任务进度对象
   * @param {string} stageKey 阶段 key
   * @param {Object} store Vuex store
   * @returns {void}
   */
  function skipRemainingSteps(progress, stageKey, store) {
    skipStepsAfter(progress, stageKey, 'query', store)
  }

  /**
   * 执行「词条审核」阶段：查询词条 → 词条全选 → 批量通过 → 保存。
   *
   * 接口映射：query→getEntryInfoList(后端)、selectAll/batchApprove(前端)、save→updateEntryList(后端)。
   *
   * @param {Object} task 任务
   * @param {Object} config 执行配置
   * @param {Object} progress 任务进度对象
   * @param {number} maxRetries 最大重试次数
   * @param {Object} store Vuex store
   * @param {number} delayMs 子步骤延时
   * @returns {Promise<void>}
   */
  async function executeEntryExamine(task, config, progress, maxRetries, store, delayMs) {
    store.dispatch('batchProgress/updateProgress', progress)

    await runWithRetry(async () => {
      let entries = []
      let updateArr = []

      // 1) 查询词条（后端 getEntryInfoList，入参取 STAGE_QUERY_PARAMS 统一口径）
      await advanceStep(progress, 'entryExamine', 'query', store, delayMs, async () => {
        const { entryState, transStates } = STAGE_QUERY_PARAMS.entryExamine
        const params = { taskID: task.id, entryState, entry: '' }
        const res = await getEntryInfoList(params, transStates)
        if (!res || !Array.isArray(res?.data?.list)) {
          throw new Error('词条审核查询返回异常')
        }
        entries = res.data.list
        return entries.length
      })

      if (entries.length === 0) {
        progress.stageCounts.entryExamine = { current: 0, total: 0 }
        skipRemainingSteps(progress, 'entryExamine', store)
        return
      }

      progress.stageCounts.entryExamine = { current: 0, total: entries.length }
      store.dispatch('batchProgress/updateProgress', progress)

      // 2) 词条全选（前端）
      await advanceStep(progress, 'entryExamine', 'selectAll', store, delayMs, async () => entries.length)

      // 3) 批量通过（前端：校验 + 构造审核通过状态）
      await advanceStep(progress, 'entryExamine', 'batchApprove', store, delayMs, async () => {
        await validateEntries(config, task, entries, '校验不通过')
        updateArr = entries.map(e => ({ ...e, auditState: 1, entryState: 3 }))
        return updateArr.length
      })

      // 4) 保存（后端 updateEntryList）
      await advanceStep(progress, 'entryExamine', 'save', store, delayMs, async () => {
        if (updateArr.length > 0) {
          const updRes = await updateEntryList({ taskID: task.id }, updateArr)
          if (!updRes || updRes.code !== 200) {
            throw new Error('词条审核保存失败')
          }
        }
        return updateArr.length
      })

      progress.stageCounts.entryExamine = { current: updateArr.length, total: entries.length }
      store.dispatch('batchProgress/updateProgress', progress)
    }, maxRetries, (attempt, err) => {
      progress.retryCount = attempt
      resetStageSteps(progress, 'entryExamine', store)
      console.log(`[${task.name}] 词条审核重试 ${attempt}/${maxRetries}:`, err.message)
    })
  }

  /**
   * 执行「翻译」阶段：查询词条 → 词条全选 → 预翻译 → 保存。
   *
   * 接口映射：query→getEntryInfoList(后端)、selectAll(前端)、preTranslate→preTranslate(后端)、save→updateEntryList(后端)。
   *
   * @param {Object} task 任务
   * @param {Object} config 执行配置
   * @param {Object} progress 任务进度对象
   * @param {number} maxRetries 最大重试次数
   * @param {Object} store Vuex store
   * @param {number} delayMs 子步骤延时
   * @returns {Promise<{status: string, message?: string}>}
   */
  async function executePreTranslate(task, config, progress, maxRetries, store, delayMs) {
    store.dispatch('batchProgress/updateProgress', progress)

    const result = await runWithRetry(async () => {
      let entries = []
      let updateArr = []
      const warningReasons = []

      // 1) 查询翻译阶段自己的待处理词条，不使用上一阶段的返回数组或数量
      await advanceStep(progress, 'preTranslate', 'query', store, delayMs, async () => {
        const { entryState, transStates } = STAGE_QUERY_PARAMS.preTranslate
        const params = { taskID: task.id, entryState, entry: '' }
        const res = await getEntryInfoList(params, transStates)
        if (!res || !Array.isArray(res?.data?.list)) {
          throw new Error('预翻译查询返回异常')
        }
        entries = res.data.list
        return entries.length
      })

      if (entries.length === 0) {
        progress.stageCounts.preTranslate = { current: 0, total: 0 }
        skipRemainingSteps(progress, 'preTranslate', store)
        return { status: 'success' }
      }

      progress.stageCounts.preTranslate = { current: 0, total: entries.length }
      store.dispatch('batchProgress/updateProgress', progress)

      // 2) 词条全选（前端）
      await advanceStep(progress, 'preTranslate', 'selectAll', store, delayMs, async () => entries.length)

      // 3) 预翻译：HTTP 200 的业务结果异常转为 warning，不进入重试
      await advanceStep(progress, 'preTranslate', 'preTranslate', store, delayMs, async () => {
        const transMap = resolveTransMap(task)
        const transCol = transMap.value
        const priorityName = getTranslatePriorityLabel(config.translatePriority)

        const preParams = { taskID: task.id, priority: config.translatePriority }
        const preRes = await preTranslate(preParams, entries)
        if (!preRes || ![200, 203].includes(preRes.code)) {
          throw new Error('预翻译接口返回异常')
        }
        const hasList = Array.isArray(preRes?.data?.list)
        const translatedEntries = hasList ? preRes.data.list : []
        if (!hasList) {
          warningReasons.push('翻译接口返回数据格式异常')
        }
        if (preRes.code === 203) {
          warningReasons.push('翻译接口返回部分结果')
        }

        if (translatedEntries.length === 0) {
          warningReasons.push('翻译接口返回空数组')
          skipStepsAfter(progress, 'preTranslate', 'preTranslate', store)
          return 0
        }

        if (translatedEntries.length !== entries.length) {
          warningReasons.push('翻译接口返回数量不一致，请检查 API（返回 ' + translatedEntries.length + ' 条，查询到 ' + entries.length + ' 条）')
        }

        const entryMap = new Map(entries.map(entry => [entry.id, entry]))
        const returnedEntries = translatedEntries
          .filter(entry => entryMap.has(entry.id))
          .map(entry => ({ ...entryMap.get(entry.id), ...entry }))
        const blankEntries = returnedEntries.filter(entry => !entry[transCol] || !String(entry[transCol]).trim())
        if (blankEntries.length > 0) {
          warningReasons.push('翻译方法「' + priorityName + '」返回 ' + blankEntries.length + ' 条词条无译文')
        }

        const candidates = returnedEntries.filter(entry => entry[transCol] && String(entry[transCol]).trim())
        if (candidates.length === 0) {
          if (warningReasons.length === 0) warningReasons.push('翻译方法「' + priorityName + '」未返回可保存译文')
          skipStepsAfter(progress, 'preTranslate', 'preTranslate', store)
          return 0
        }

        const validationVm = createValidationVm(config)
        const verifyResult = await classifyArr(validationVm, candidates, transCol, getMethods(validationVm))
        if (verifyResult.errorIds.size > 0) {
          warningReasons.push('翻译结果校验不通过 ' + verifyResult.errorIds.size + ' 条')
        }

        const validIds = verifyResult.acceptIds
        updateArr = candidates
          .filter(entry => validIds.has(entry.id))
          .map(entry => ({ ...entry, [transMap.state]: '1' }))
        if (updateArr.length === 0) {
          skipStepsAfter(progress, 'preTranslate', 'preTranslate', store)
        }
        return updateArr.length
      })

      // 4) 保存有效子集；没有有效结果时不调用保存接口
      if (updateArr.length > 0) {
        await advanceStep(progress, 'preTranslate', 'save', store, delayMs, async () => {
          const updRes = await updateEntryList({ taskID: task.id }, updateArr)
          if (!updRes || updRes.code !== 200) {
            throw new Error('预翻译保存失败')
          }
          return updateArr.length
        })
      }

      progress.stageCounts.preTranslate = { current: updateArr.length, total: entries.length }
      if (warningReasons.length > 0) {
        const message = '翻译阶段告警：' + [...new Set(warningReasons)].join('；')
        progress.warning = message
        progress.stageMessages = progress.stageMessages || {}
        progress.stageMessages.preTranslate = message
        store.dispatch('batchProgress/updateProgress', progress)
        return { status: 'warning', message }
      }
      store.dispatch('batchProgress/updateProgress', progress)
      return { status: 'success' }
    }, maxRetries, (attempt, err) => {
      progress.retryCount = attempt
      resetStageSteps(progress, 'preTranslate', store)
      console.log(`[${task.name}] 预翻译重试 ${attempt}/${maxRetries}:`, err.message)
    })

    return result
  }

  /**
   * 执行「翻译审核」阶段：独立查询待审核词条 → 词条全选 → 批量通过 → 保存。
   * 不读取翻译阶段的返回数组或数量。
   *
   * @param {Object} task 任务
   * @param {Object} config 执行配置
   * @param {Object} progress 任务进度对象
   * @param {number} maxRetries 最大重试次数
   * @param {Object} store Vuex store
   * @param {number} delayMs 子步骤延时
   * @returns {Promise<void>}
   */
  async function executeTranslateExamine(task, config, progress, maxRetries, store, delayMs) {
    store.dispatch('batchProgress/updateProgress', progress)

    await runWithRetry(async () => {
      let entries = []
      let updateArr = []

      // 1) 查询词条（后端 getEntryInfoList，入参取 STAGE_QUERY_PARAMS 统一口径）
      await advanceStep(progress, 'translateExamine', 'query', store, delayMs, async () => {
        const { entryState, transStates } = STAGE_QUERY_PARAMS.translateExamine
        const params = { taskID: task.id, entryState, entry: '' }
        const res = await getEntryInfoList(params, transStates)
        if (!res || !Array.isArray(res?.data?.list)) {
          throw new Error('翻译审核查询返回异常')
        }
        entries = res.data.list
        return entries.length
      })

      if (entries.length === 0) {
        progress.stageCounts.translateExamine = { current: 0, total: 0 }
        skipRemainingSteps(progress, 'translateExamine', store)
        return
      }

      progress.stageCounts.translateExamine = { current: 0, total: entries.length }
      store.dispatch('batchProgress/updateProgress', progress)

      // 2) 词条全选（前端）
      await advanceStep(progress, 'translateExamine', 'selectAll', store, delayMs, async () => entries.length)

      // 3) 批量通过（前端：校验 + 构造审核通过状态）
      await advanceStep(progress, 'translateExamine', 'batchApprove', store, delayMs, async () => {
        const transMap = await validateEntries(config, task, entries, '翻译审核校验不通过')
        updateArr = entries.map(e => ({ ...e, auditState: 1, [transMap.state]: '3' }))
        return updateArr.length
      })

      // 4) 保存（后端 updateEntryList）
      await advanceStep(progress, 'translateExamine', 'save', store, delayMs, async () => {
        if (updateArr.length > 0) {
          const updRes = await updateEntryList({ taskID: task.id }, updateArr)
          if (!updRes || updRes.code !== 200) {
            throw new Error('翻译审核保存失败')
          }
        }
        return updateArr.length
      })

      progress.stageCounts.translateExamine = { current: updateArr.length, total: entries.length }
      store.dispatch('batchProgress/updateProgress', progress)
    }, maxRetries, (attempt, err) => {
      progress.retryCount = attempt
      resetStageSteps(progress, 'translateExamine', store)
      console.log(`[${task.name}] 翻译审核重试 ${attempt}/${maxRetries}:`, err.message)
    })
  }

  /** 阶段 key → 执行器映射。 */
  const stageExecutors = {
    entryExamine: executeEntryExamine,
    preTranslate: executePreTranslate,
    translateExamine: executeTranslateExamine
  }

  /**
   * 串行执行单个任务的全部启用阶段（供 worker 池并发调用，任务之间互不干扰）。
   * @param {Object} task 任务
   * @param {Object} progress 该任务的进度对象（原地修改，按 taskId 上报）
   * @param {string[]} enabledStages 启用阶段 key 列表
   * @param {Object} config 执行配置
   * @param {number} maxRetries 最大重试次数
   * @param {Object} store Vuex store
   * @param {number} delayMs 子步骤延时
   * @returns {Promise<void>}
   */
  async function runTask(task, progress, enabledStages, config, maxRetries, store, delayMs) {
    for (const stageKey of enabledStages) {
      progress.currentStage = stageKey
      progress.currentStep = null
      progress.stages[stageKey] = 'running'
      store.dispatch('batchProgress/updateProgress', progress)

      try {
        const result = await stageExecutors[stageKey](task, config, progress, maxRetries, store, delayMs)
        progress.stages[stageKey] = result?.status === 'warning' ? 'warning' : 'success'
        store.dispatch('batchProgress/updateProgress', progress)
      } catch (err) {
        progress.stages[stageKey] = 'failed'
        const stageName = STAGE_NAME_MAP[stageKey] || '当前'
        progress.error = `${stageName}阶段失败，并达到重复上限`
        progress.currentStage = null
        progress.currentStep = null
        store.dispatch('batchProgress/updateProgress', progress)

        // 翻译阶段失败也不能阻塞翻译审核；翻译审核会重新查询自己的待处理词条。
        if (stageKey === 'preTranslate' && enabledStages.includes('translateExamine')) {
          continue
        }

        const stageIdx = enabledStages.indexOf(stageKey)
        for (let j = stageIdx + 1; j < enabledStages.length; j++) {
          progress.stages[enabledStages[j]] = 'skipped'
          for (const step of getStageSteps(enabledStages[j])) {
            progress.steps[enabledStages[j]][step.key] = 'skipped'
          }
        }
        store.dispatch('batchProgress/updateProgress', progress)
      }
    }

    progress.currentStage = null
    progress.currentStep = null
    store.dispatch('batchProgress/updateProgress', progress)
  }

  /**
   * 批量执行入口：任务之间按 concurrency 并行（任务级 worker 池），
   * 单个任务内阶段仍按 STAGE_ORDER 顺序串行，失败即跳过该任务后续阶段。
   * @param {Object} config 执行配置（tasks / stages / concurrency / translatePriority / maxRetries / stepDelayMs 等）
   * @param {Object} store Vuex store
   * @returns {Promise<void>}
   */
  async function execute(config, store) {
    if (!isContinuous(config.stages)) {
      throw new Error('阶段选择必须连续，不能有空洞')
    }

    const enabledStages = getEnabledStages(config.stages)
    const maxRetries = config.maxRetries ?? 3
    const delayMs = config.stepDelayMs ?? DEFAULT_STEP_DELAY_MS
    const concurrency = Math.min(Math.max(config.concurrency ?? 1, 1), 100)

    try {
      let nextIndex = 0
      const claimTask = () => {
        const i = nextIndex
        nextIndex++
        return i < config.tasks.length
          ? { task: config.tasks[i], progress: store.state.batchProgress.progresses[i] }
          : null
      }
      const workerCount = Math.min(concurrency, config.tasks.length)
      const workers = Array.from({ length: workerCount }, async () => {
        for (let claimed = claimTask(); claimed; claimed = claimTask()) {
          await runTask(claimed.task, claimed.progress, enabledStages, config, maxRetries, store, delayMs)
        }
      })
      await Promise.all(workers)
    } finally {
      store.dispatch('batchProgress/complete')
    }
  }

  return {
    execute,
    isContinuous,
    getToggleableStages,
    STAGES: STAGE_ORDER
  }
}
