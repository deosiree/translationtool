import { getEntryInfoList, updateEntryList, preTranslate } from '@/http/api/workbench'
import { setInfo } from '@/http/api/i18Server'
import { updateTaskInfo } from '@/http/api/task'
import { classifyArr, getMethods, clearCellErrorsForRecords } from '@/utils/validationUtils'
import commonParam from '@/constants/commonParam'
import { getTranslatePriorityLabel } from '@/constants/translatePriority'
import { getCurrentFormattedTime } from '@/utils/dateUtils'
import {
  STAGE_ORDER,
  STAGE_NAME_MAP,
  STAGE_QUERY_PARAMS,
  ARCHIVE_MODE,
  getStageSteps
} from '@/constants/batchPreTranslateSteps'
import { canRunStage, getSkipReason } from '@/utils/batchStagePermission'
import { chunkArray, aggregateToParent, buildSubTaskId } from '@/utils/batchSplitAggregate'
import { createGlobalScheduler } from '@/utils/batchGlobalScheduler'

/**
 * 批量预翻译执行编排器。
 *
 * 将「词条审核 / 翻译 / 翻译审核」三个阶段统一建模为 4 个可观察子步骤：
 *   query(查询词条) → selectAll(词条全选) → 动作(批量通过/预翻译) → save(保存)。
 *
 * 启用「拆分增强」时，某阶段 query 条数超过 splitLimit 则拆成多个子块，
 * 主任务与子块共用全局 concurrency 槽位并发执行。
 */

/** 开发阶段每个子步骤之间的延时毫秒数；生产环境为 0。 */
const DEFAULT_STEP_DELAY_MS = process.env.NODE_ENV === 'production' ? 0 : 1000

const DEFAULT_SPLIT_LIMIT = 1000
const MIN_SPLIT_LIMIT = 20
const MAX_SPLIT_LIMIT = 2000

export function useBatchPreTranslate() {
  function isStageEnabled(stages, key) {
    return stages[key] === true
  }

  function getEnabledStages(stages) {
    return STAGE_ORDER.filter(k => isStageEnabled(stages, k))
  }

  function isContinuous(stages) {
    const selected = STAGE_ORDER.filter(k => stages[k])
    if (selected.length <= 1) return true
    const idx = STAGE_ORDER.map(k => selected.includes(k))
    const first = idx.indexOf(true)
    const last = idx.lastIndexOf(true)
    return idx.slice(first, last + 1).every(v => v)
  }

  function getToggleableStages(stages) {
    const selected = STAGE_ORDER.filter(k => stages[k])
    if (selected.length === 0) return STAGE_ORDER
    return [selected[0], selected[selected.length - 1]]
  }

  function getSplitLimit(config) {
    const raw = config.splitLimit ?? DEFAULT_SPLIT_LIMIT
    return Math.min(Math.max(raw, MIN_SPLIT_LIMIT), MAX_SPLIT_LIMIT)
  }

  function shouldSplit(config, entryCount) {
    return config.enhancedSplit === true && entryCount > getSplitLimit(config)
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

  function createValidationVm(config) {
    return {
      rulesOptions: config.rules || [],
      editableData: {},
      classifyLimit: {}
    }
  }

  function resolveTransMap(task) {
    const transMap = commonParam.languageMap[task.translateType]
    if (!transMap) {
      throw new Error('未知翻译语种: ' + task.translateType)
    }
    return transMap
  }

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

  function getProgressById(store, taskId) {
    return store.state.batchProgress.progresses.find(p => p.taskId === taskId)
  }

  function getSubProgresses(store, parentTaskId, stageKey) {
    return store.state.batchProgress.progresses.filter(
      p => p.parentTaskId === parentTaskId && p.splitMeta?.stageKey === stageKey
    )
  }

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

  function resetStageSteps(progress, stageKey, store) {
    for (const step of getStageSteps(stageKey)) {
      progress.steps[stageKey][step.key] = 'pending'
    }
    progress.currentStep = null
    store.dispatch('batchProgress/updateProgress', progress)
  }

  function skipStepsAfter(progress, stageKey, stepKey, store) {
    const steps = getStageSteps(stageKey)
    const stepIdx = steps.findIndex(step => step.key === stepKey)
    for (let i = stepIdx + 1; i < steps.length; i++) {
      progress.steps[stageKey][steps[i].key] = 'skipped'
    }
    store.dispatch('batchProgress/updateProgress', progress)
  }

  function skipRemainingSteps(progress, stageKey, store) {
    skipStepsAfter(progress, stageKey, 'query', store)
  }

  function skipStageForPermission(progress, stageKey, reason, store) {
    progress.stages[stageKey] = 'skipped'
    progress.stageCounts[stageKey] = { current: 0, total: 0 }
    for (const step of getStageSteps(stageKey)) {
      progress.steps[stageKey][step.key] = 'skipped'
    }
    progress.stageMessages = progress.stageMessages || {}
    progress.stageMessages[stageKey] = reason
    progress.currentStage = null
    progress.currentStep = null
    store.dispatch('batchProgress/updateProgress', progress)
  }

  /**
   * 执行 query 子步骤（主任务专用，仅一次）。
   * archive 全量列表不传 entryState（与手工归档弹窗一致）。
   */
  async function runQueryStep(task, progress, stageKey, store, delayMs) {
    let entries = []
    await advanceStep(progress, stageKey, 'query', store, delayMs, async () => {
      const queryParams = STAGE_QUERY_PARAMS[stageKey]
      if (!queryParams) {
        throw new Error(`${STAGE_NAME_MAP[stageKey] || stageKey}查询参数未配置`)
      }
      const { entryState, transStates, fullList } = queryParams
      const params = fullList
        ? { taskID: task.id, entry: '' }
        : { taskID: task.id, entryState, entry: '' }
      const res = await getEntryInfoList(params, transStates || [])
      if (!res || !Array.isArray(res?.data?.list)) {
        const stageName = STAGE_NAME_MAP[stageKey] || stageKey
        throw new Error(`${stageName}查询返回异常`)
      }
      entries = res.data.list
      return entries.length
    })
    return entries
  }

  /**
   * 词条是否全部可归档结束：entryState==3 且目标语种 state==3。
   * @param {Array} entries
   * @param {string} stateKey languageMap[translateType].state
   * @returns {boolean}
   */
  function entriesDone(entries, stateKey) {
    if (!entries || entries.length === 0) return true
    return entries.every(item => Number(item.entryState) === 3 && Number(item[stateKey]) === 3)
  }

  /**
   * 归档阶段主体（永不拆分）：selectAll → writeBack →（可选）endTask。
   * query 已在 runOneStage 完成。
   */
  async function runArchiveChunk(task, config, progress, entries, store, delayMs) {
    progress.stages.archive = 'running'
    progress.stageCounts.archive = { current: 0, total: entries.length }
    store.dispatch('batchProgress/updateProgress', progress)

    await advanceStep(progress, 'archive', 'selectAll', store, delayMs, async () => entries.length)

    const mode = config.archiveMode || ARCHIVE_MODE.WRITE
    if (mode === ARCHIVE_MODE.WRITE_END) {
      const transMap = resolveTransMap(task)
      if (!entriesDone(entries, transMap.state)) {
        const reason = '存在未处理完的词条，已跳过归档'
        progress.stages.archive = 'skipped'
        progress.stageCounts.archive = { current: 0, total: entries.length }
        progress.steps.archive.writeBack = 'skipped'
        progress.steps.archive.endTask = 'skipped'
        progress.stageMessages = progress.stageMessages || {}
        progress.stageMessages.archive = reason
        progress.currentStep = null
        store.dispatch('batchProgress/updateProgress', progress)
        return { status: 'skipped' }
      }
    }

    await advanceStep(progress, 'archive', 'writeBack', store, delayMs, async () => {
      if (entries.length === 0) return 0
      const params = {
        taskID: task.id,
        translateType: task.translateType,
        isTag: 0,
        isComment: 0,
        i18nUrl: config.archiveIp
      }
      const res = await setInfo(params, entries)
      if (!res || res.code !== 200) {
        throw new Error('归档回写失败')
      }
      return entries.length
    })

    if (mode === ARCHIVE_MODE.WRITE_END) {
      await advanceStep(progress, 'archive', 'endTask', store, delayMs, async () => {
        task.state = '6'
        task.endTime = getCurrentFormattedTime()
        await updateTaskInfo(task)
        return 1
      })
    } else {
      progress.steps.archive.endTask = 'skipped'
      progress.stepCounts.archive.endTask = 0
    }

    progress.stageCounts.archive = { current: entries.length, total: entries.length }
    progress.currentStep = null
    store.dispatch('batchProgress/updateProgress', progress)
    return { status: 'success' }
  }

  async function runEntryExamineChunk(task, config, chunkProgress, entries, store, delayMs) {
    let updateArr = []
    chunkProgress.stages.entryExamine = 'running'
    store.dispatch('batchProgress/updateProgress', chunkProgress)

    await advanceStep(chunkProgress, 'entryExamine', 'selectAll', store, delayMs, async () => entries.length)
    await advanceStep(chunkProgress, 'entryExamine', 'batchApprove', store, delayMs, async () => {
      await validateEntries(config, task, entries, '校验不通过')
      updateArr = entries.map(e => ({ ...e, auditState: 1, entryState: 3 }))
      return updateArr.length
    })
    await advanceStep(chunkProgress, 'entryExamine', 'save', store, delayMs, async () => {
      if (updateArr.length > 0) {
        const updRes = await updateEntryList({ taskID: task.id }, updateArr)
        if (!updRes || updRes.code !== 200) {
          throw new Error('词条审核保存失败')
        }
      }
      return updateArr.length
    })

    chunkProgress.stageCounts.entryExamine = { current: updateArr.length, total: entries.length }
    chunkProgress.stages.entryExamine = 'success'
    chunkProgress.currentStep = null
    store.dispatch('batchProgress/updateProgress', chunkProgress)
    return { processed: updateArr.length, warnings: [], savedEntries: updateArr }
  }

  async function runPreTranslateChunk(task, config, chunkProgress, entries, store, delayMs) {
    let updateArr = []
    const warningReasons = []
    chunkProgress.stages.preTranslate = 'running'
    store.dispatch('batchProgress/updateProgress', chunkProgress)

    await advanceStep(chunkProgress, 'preTranslate', 'selectAll', store, delayMs, async () => entries.length)

    await advanceStep(chunkProgress, 'preTranslate', 'preTranslate', store, delayMs, async () => {
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
      if (!hasList) warningReasons.push('翻译接口返回数据格式异常')
      if (preRes.code === 203) warningReasons.push('翻译接口返回部分结果')

      if (translatedEntries.length === 0) {
        warningReasons.push('翻译接口返回空数组')
        skipStepsAfter(chunkProgress, 'preTranslate', 'preTranslate', store)
        return 0
      }

      if (translatedEntries.length !== entries.length) {
        warningReasons.push(
          '翻译接口返回数量不一致，请检查 API（返回 ' + translatedEntries.length + ' 条，查询到 ' + entries.length + ' 条）'
        )
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
        skipStepsAfter(chunkProgress, 'preTranslate', 'preTranslate', store)
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
        skipStepsAfter(chunkProgress, 'preTranslate', 'preTranslate', store)
      }
      return updateArr.length
    })

    if (updateArr.length > 0) {
      await advanceStep(chunkProgress, 'preTranslate', 'save', store, delayMs, async () => {
        const updRes = await updateEntryList({ taskID: task.id }, updateArr)
        if (!updRes || updRes.code !== 200) {
          throw new Error('预翻译保存失败')
        }
        return updateArr.length
      })
    } else {
      chunkProgress.steps.preTranslate.save = 'skipped'
      store.dispatch('batchProgress/updateProgress', chunkProgress)
    }

    chunkProgress.stageCounts.preTranslate = { current: updateArr.length, total: entries.length }
    if (warningReasons.length > 0) {
      const message = '翻译阶段告警：' + [...new Set(warningReasons)].join('；')
      chunkProgress.warning = message
      chunkProgress.stageMessages = chunkProgress.stageMessages || {}
      chunkProgress.stageMessages.preTranslate = message
      chunkProgress.stages.preTranslate = 'warning'
    } else {
      chunkProgress.stages.preTranslate = 'success'
    }
    chunkProgress.currentStep = null
    store.dispatch('batchProgress/updateProgress', chunkProgress)
    return { processed: updateArr.length, warnings: warningReasons, savedEntries: updateArr }
  }

  async function runTranslateExamineChunk(task, config, chunkProgress, entries, store, delayMs) {
    let updateArr = []
    chunkProgress.stages.translateExamine = 'running'
    store.dispatch('batchProgress/updateProgress', chunkProgress)

    await advanceStep(chunkProgress, 'translateExamine', 'selectAll', store, delayMs, async () => entries.length)
    await advanceStep(chunkProgress, 'translateExamine', 'batchApprove', store, delayMs, async () => {
      const transMap = await validateEntries(config, task, entries, '翻译审核校验不通过')
      updateArr = entries.map(e => ({ ...e, auditState: 1, [transMap.state]: '3' }))
      return updateArr.length
    })
    await advanceStep(chunkProgress, 'translateExamine', 'save', store, delayMs, async () => {
      if (updateArr.length > 0) {
        const updRes = await updateEntryList({ taskID: task.id }, updateArr)
        if (!updRes || updRes.code !== 200) {
          throw new Error('翻译审核保存失败')
        }
      }
      return updateArr.length
    })

    chunkProgress.stageCounts.translateExamine = { current: updateArr.length, total: entries.length }
    chunkProgress.stages.translateExamine = 'success'
    chunkProgress.currentStep = null
    store.dispatch('batchProgress/updateProgress', chunkProgress)
    return { processed: updateArr.length, warnings: [], savedEntries: updateArr }
  }

  const chunkRunners = {
    entryExamine: runEntryExamineChunk,
    preTranslate: runPreTranslateChunk,
    translateExamine: runTranslateExamineChunk
  }

  /**
   * 从触发阶段起，取本次启用配置中向后的贯通阶段列表。
   * @param {string} stageKey
   * @param {Object<string, boolean>} stages
   * @returns {string[]}
   */
  function getContinuumStages(stageKey, stages) {
    // 归档永不进入拆分贯通（全量回写一次完成）
    const enabled = getEnabledStages(stages).filter(k => k !== 'archive')
    const idx = enabled.indexOf(stageKey)
    if (idx === -1) return [stageKey]
    return enabled.slice(idx)
  }

  /**
   * 二次进入某阶段前重置该阶段进度，避免上一波贯通聚合残留。
   * @param {Object} progress
   * @param {string} stageKey
   * @param {Object} store
   */
  function resetStageForReentry(progress, stageKey, store) {
    for (const step of getStageSteps(stageKey)) {
      progress.steps[stageKey][step.key] = 'pending'
      progress.stepCounts[stageKey][step.key] = 0
    }
    progress.stageCounts[stageKey] = { current: 0, total: 0 }
    progress.stages[stageKey] = 'running'
    if (progress.stageMessages) delete progress.stageMessages[stageKey]
    progress.currentStage = stageKey
    progress.currentStep = null
    store.dispatch('batchProgress/updateProgress', progress)
  }

  /**
   * 将贯通阶段内子块进度全部聚合到主任务。
   * @param {Object} parent
   * @param {Object[]} subs
   * @param {string[]} continuum
   * @param {Object<string, number>} totalsByStage stageKey → query/切片总条数
   * @returns {{ hasFailed: boolean, hasWarning: boolean }}
   */
  function aggregateContinuumToParent(parent, subs, continuum, totalsByStage) {
    let hasFailed = false
    let hasWarning = false
    for (const sk of continuum) {
      const total = totalsByStage[sk] ?? totalsByStage[continuum[0]] ?? 0
      const agg = aggregateToParent(parent, subs, sk, total)
      if (agg.hasFailed) hasFailed = true
      if (agg.hasWarning) hasWarning = true
    }
    return { hasFailed, hasWarning }
  }

  /**
   * 子块内串行跑贯通阶段：下游用上游 savedEntries，不重新全表 query。
   */
  async function runChunkContinuum(task, config, chunkProgress, chunkEntries, continuum, maxRetries, store, delayMs) {
    let pipelineEntries = chunkEntries
    let triggerStatus = 'success'

    for (let i = 0; i < continuum.length; i++) {
      const sk = continuum[i]
      if (!canRunStage(store.state.user, task, sk)) {
        skipStageForPermission(chunkProgress, sk, getSkipReason(store.state.user, task, sk), store)
        continue
      }

      if (!pipelineEntries || pipelineEntries.length === 0) {
        chunkProgress.stages[sk] = 'skipped'
        chunkProgress.stageCounts[sk] = { current: 0, total: 0 }
        for (const step of getStageSteps(sk)) {
          if (step.key === 'query') continue
          chunkProgress.steps[sk][step.key] = 'skipped'
        }
        store.dispatch('batchProgress/updateProgress', chunkProgress)
        continue
      }

      const runner = chunkRunners[sk]
      try {
        const result = await runWithRetry(
          () => runner(task, config, chunkProgress, pipelineEntries, store, delayMs),
          maxRetries,
          (attempt, err) => {
            chunkProgress.retryCount = attempt
            resetStageSteps(chunkProgress, sk, store)
            console.log(
              `[${chunkProgress.taskName}] ${STAGE_NAME_MAP[sk]}重试 ${attempt}/${maxRetries}:`,
              err.message
            )
          }
        )
        pipelineEntries = result.savedEntries || []
        if (i === 0) {
          triggerStatus = result.warnings?.length > 0 || chunkProgress.stages[sk] === 'warning'
            ? 'warning'
            : 'success'
        }
      } catch (err) {
        chunkProgress.stages[sk] = 'failed'
        chunkProgress.error = `${STAGE_NAME_MAP[sk]}子任务失败`
        store.dispatch('batchProgress/updateProgress', chunkProgress)
        // 后续贯通阶段跳过
        for (let j = i + 1; j < continuum.length; j++) {
          const nextSk = continuum[j]
          chunkProgress.stages[nextSk] = 'skipped'
          for (const step of getStageSteps(nextSk)) {
            if (chunkProgress.steps[nextSk][step.key] === 'pending') {
              chunkProgress.steps[nextSk][step.key] = 'skipped'
            }
          }
        }
        store.dispatch('batchProgress/updateProgress', chunkProgress)
        if (i === 0) throw err
        break
      }
    }

    return { status: triggerStatus }
  }

  /**
   * 不拆分时，在主任务 progress 上串行执行 query 之后的子步骤。
   */
  async function runStageBodyWithoutSplit(task, config, progress, stageKey, entries, store, delayMs) {
    if (stageKey === 'entryExamine') {
      let updateArr = []
      progress.stageCounts.entryExamine = { current: 0, total: entries.length }
      store.dispatch('batchProgress/updateProgress', progress)

      await advanceStep(progress, 'entryExamine', 'selectAll', store, delayMs, async () => entries.length)
      await advanceStep(progress, 'entryExamine', 'batchApprove', store, delayMs, async () => {
        await validateEntries(config, task, entries, '校验不通过')
        updateArr = entries.map(e => ({ ...e, auditState: 1, entryState: 3 }))
        return updateArr.length
      })
      await advanceStep(progress, 'entryExamine', 'save', store, delayMs, async () => {
        if (updateArr.length > 0) {
          const updRes = await updateEntryList({ taskID: task.id }, updateArr)
          if (!updRes || updRes.code !== 200) throw new Error('词条审核保存失败')
        }
        return updateArr.length
      })
      progress.stageCounts.entryExamine = { current: updateArr.length, total: entries.length }
      store.dispatch('batchProgress/updateProgress', progress)
      return { status: 'success' }
    }

    if (stageKey === 'preTranslate') {
      let updateArr = []
      const warningReasons = []
      progress.stageCounts.preTranslate = { current: 0, total: entries.length }
      store.dispatch('batchProgress/updateProgress', progress)

      await advanceStep(progress, 'preTranslate', 'selectAll', store, delayMs, async () => entries.length)
      await advanceStep(progress, 'preTranslate', 'preTranslate', store, delayMs, async () => {
        const transMap = resolveTransMap(task)
        const transCol = transMap.value
        const priorityName = getTranslatePriorityLabel(config.translatePriority)
        const preParams = { taskID: task.id, priority: config.translatePriority }
        const preRes = await preTranslate(preParams, entries)
        if (!preRes || ![200, 203].includes(preRes.code)) throw new Error('预翻译接口返回异常')

        const hasList = Array.isArray(preRes?.data?.list)
        const translatedEntries = hasList ? preRes.data.list : []
        if (!hasList) warningReasons.push('翻译接口返回数据格式异常')
        if (preRes.code === 203) warningReasons.push('翻译接口返回部分结果')

        if (translatedEntries.length === 0) {
          warningReasons.push('翻译接口返回空数组')
          skipStepsAfter(progress, 'preTranslate', 'preTranslate', store)
          return 0
        }
        if (translatedEntries.length !== entries.length) {
          warningReasons.push(
            '翻译接口返回数量不一致，请检查 API（返回 ' + translatedEntries.length + ' 条，查询到 ' + entries.length + ' 条）'
          )
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
        if (updateArr.length === 0) skipStepsAfter(progress, 'preTranslate', 'preTranslate', store)
        return updateArr.length
      })

      if (updateArr.length > 0) {
        await advanceStep(progress, 'preTranslate', 'save', store, delayMs, async () => {
          const updRes = await updateEntryList({ taskID: task.id }, updateArr)
          if (!updRes || updRes.code !== 200) throw new Error('预翻译保存失败')
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
    }

    if (stageKey === 'translateExamine') {
      let updateArr = []
      progress.stageCounts.translateExamine = { current: 0, total: entries.length }
      store.dispatch('batchProgress/updateProgress', progress)

      await advanceStep(progress, 'translateExamine', 'selectAll', store, delayMs, async () => entries.length)
      await advanceStep(progress, 'translateExamine', 'batchApprove', store, delayMs, async () => {
        const transMap = await validateEntries(config, task, entries, '翻译审核校验不通过')
        updateArr = entries.map(e => ({ ...e, auditState: 1, [transMap.state]: '3' }))
        return updateArr.length
      })
      await advanceStep(progress, 'translateExamine', 'save', store, delayMs, async () => {
        if (updateArr.length > 0) {
          const updRes = await updateEntryList({ taskID: task.id }, updateArr)
          if (!updRes || updRes.code !== 200) throw new Error('翻译审核保存失败')
        }
        return updateArr.length
      })
      progress.stageCounts.translateExamine = { current: updateArr.length, total: entries.length }
      store.dispatch('batchProgress/updateProgress', progress)
      return { status: 'success' }
    }

    if (stageKey === 'archive') {
      return runArchiveChunk(task, config, progress, entries, store, delayMs)
    }

    return { status: 'success' }
  }

  /**
   * 拆分路径：创建子块，每块贯通后续启用阶段，经全局调度器并发执行。
   * 主任务不跳过后续阶段（由 pipeline 独立再 query）。
   */
  async function runStageWithSplit(task, config, progress, stageKey, entries, maxRetries, store, delayMs, scheduler) {
    const splitLimit = getSplitLimit(config)
    const chunks = chunkArray(entries, splitLimit)
    const chunkTotal = chunks.length
    const continuum = getContinuumStages(stageKey, config.stages)

    // 主任务：触发阶段 selectAll 先标成功；贯通后续阶段先置 running 以便 Overlay 聚合
    progress.stageCounts[stageKey] = { current: 0, total: entries.length }
    progress.steps[stageKey].selectAll = 'success'
    progress.stepCounts[stageKey].selectAll = entries.length
    progress.isSplitParent = true
    for (const sk of continuum) {
      if (sk === stageKey) continue
      progress.stages[sk] = 'running'
      progress.stageCounts[sk] = { current: 0, total: entries.length }
      progress.steps[sk].query = 'skipped'
      progress.stepCounts[sk].query = 0
    }
    store.dispatch('batchProgress/updateProgress', progress)

    await store.dispatch('batchProgress/addSubTasks', {
      parentTaskId: task.id,
      stageKey,
      chunkTotal,
      stageConfig: config.stages,
      continuumStages: continuum
    })

    // 贯通阶段总量：触发阶段用 entries.length；下游用各块 saved 聚合时动态更新，初始用 entries.length 作上限展示
    const totalsByStage = {}
    for (const sk of continuum) {
      totalsByStage[sk] = entries.length
    }

    let aggregateChain = Promise.resolve()
    const scheduleParentAggregate = () => {
      aggregateChain = aggregateChain.then(() => {
        const p = getProgressById(store, task.id)
        const subs = getSubProgresses(store, task.id, stageKey)
        // 下游 total 用各子块 stageCounts.total 之和（更贴近本波实际）
        for (const sk of continuum) {
          if (sk === stageKey) {
            totalsByStage[sk] = entries.length
          } else {
            const sumTotal = subs.reduce((acc, sp) => acc + (sp.stageCounts?.[sk]?.total || 0), 0)
            totalsByStage[sk] = sumTotal > 0 ? sumTotal : entries.length
          }
        }
        aggregateContinuumToParent(p, subs, continuum, totalsByStage)
        p.isSplitParent = true
        p.currentStage = stageKey
        p.currentStep = null
        store.dispatch('batchProgress/updateProgress', p)
      })
      return aggregateChain
    }

    const chunkPromises = chunks.map((chunkEntries, idx) => {
      const chunkIndex = idx + 1
      const subTaskId = buildSubTaskId(task.id, stageKey, chunkIndex)
      return scheduler.schedule(async () => {
        const chunkProgress = getProgressById(store, subTaskId)
        try {
          await runChunkContinuum(
            task, config, chunkProgress, chunkEntries, continuum, maxRetries, store, delayMs
          )
        } catch (err) {
          chunkProgress.stages[stageKey] = 'failed'
          chunkProgress.error = `${STAGE_NAME_MAP[stageKey]}子任务失败`
          store.dispatch('batchProgress/updateProgress', chunkProgress)
          throw err
        } finally {
          await scheduleParentAggregate()
        }
      })
    })

    const settled = await Promise.allSettled(chunkPromises)
    await aggregateChain

    const parent = getProgressById(store, task.id)
    const subs = getSubProgresses(store, task.id, stageKey)
    for (const sk of continuum) {
      if (sk === stageKey) {
        totalsByStage[sk] = entries.length
      } else {
        const sumTotal = subs.reduce((acc, sp) => acc + (sp.stageCounts?.[sk]?.total || 0), 0)
        totalsByStage[sk] = sumTotal > 0 ? sumTotal : 0
      }
    }
    const agg = aggregateContinuumToParent(parent, subs, continuum, totalsByStage)
    parent.isSplitParent = true
    parent.currentStage = stageKey
    parent.currentStep = null
    store.dispatch('batchProgress/updateProgress', parent)

    await store.dispatch('batchProgress/removeSubTasks', { parentTaskId: task.id })

    const finalParent = getProgressById(store, task.id)
    finalParent.isSplitParent = false
    finalParent.currentStep = null

    // 仅判定触发阶段是否失败（贯通下游的失败不阻塞主任务进入下一阶段的独立 query）
    const triggerFailed = agg.hasFailed || settled.some(r => r.status === 'rejected')
      || subs.some(sp => sp.stages?.[stageKey] === 'failed')

    if (triggerFailed) {
      finalParent.stages[stageKey] = 'failed'
      store.dispatch('batchProgress/updateProgress', finalParent)
      throw new Error(`${STAGE_NAME_MAP[stageKey]}阶段失败，并达到重复上限`)
    }

    if (agg.hasWarning || finalParent.stages[stageKey] === 'warning') {
      finalParent.stages[stageKey] = 'warning'
      store.dispatch('batchProgress/updateProgress', finalParent)
      return { status: 'warning' }
    }

    finalParent.stages[stageKey] = 'success'
    store.dispatch('batchProgress/updateProgress', finalParent)
    return { status: 'success' }
  }

  async function runOneStage(task, stageKey, config, maxRetries, store, delayMs, scheduler) {
    let progress = getProgressById(store, task.id)

    if (!canRunStage(store.state.user, task, stageKey)) {
      skipStageForPermission(progress, stageKey, getSkipReason(store.state.user, task, stageKey), store)
      return { status: 'skipped' }
    }

    // 二次进入（例如贯通已聚合过翻译审核）时先重置该阶段，再独立 query
    resetStageForReentry(progress, stageKey, store)
    progress = getProgressById(store, task.id)

    try {
      const result = await runWithRetry(async () => {
        progress = getProgressById(store, task.id)
        let entries = []
        await scheduler.schedule(async () => {
          entries = await runQueryStep(task, progress, stageKey, store, delayMs)
        })

        // 归档：空列表仍走 writeEnd 结束任务；其它阶段空列表直接成功
        if (entries.length === 0 && stageKey !== 'archive') {
          progress.stageCounts[stageKey] = { current: 0, total: 0 }
          skipRemainingSteps(progress, stageKey, store)
          progress.stages[stageKey] = 'success'
          store.dispatch('batchProgress/updateProgress', progress)
          return { status: 'success' }
        }

        // 归档永不拆分
        if (stageKey !== 'archive' && shouldSplit(config, entries.length)) {
          return await runStageWithSplit(task, config, progress, stageKey, entries, maxRetries, store, delayMs, scheduler)
        }

        let stageResult
        await scheduler.schedule(async () => {
          progress = getProgressById(store, task.id)
          stageResult = await runStageBodyWithoutSplit(task, config, progress, stageKey, entries, store, delayMs)
        })
        progress = getProgressById(store, task.id)
        if (stageResult.status === 'skipped') {
          progress.stages[stageKey] = 'skipped'
        } else if (stageResult.status === 'warning') {
          progress.stages[stageKey] = 'warning'
        } else {
          progress.stages[stageKey] = 'success'
        }
        store.dispatch('batchProgress/updateProgress', progress)
        return stageResult
      }, maxRetries, (attempt, err) => {
        progress = getProgressById(store, task.id)
        progress.retryCount = attempt
        resetStageSteps(progress, stageKey, store)
        console.log(`[${task.name}] ${STAGE_NAME_MAP[stageKey]}重试 ${attempt}/${maxRetries}:`, err.message)
      })

      return result
    } catch (err) {
      progress = getProgressById(store, task.id)
      progress.stages[stageKey] = 'failed'
      const stageName = STAGE_NAME_MAP[stageKey] || '当前'
      progress.error = `${stageName}阶段失败，并达到重复上限`
      progress.currentStage = null
      progress.currentStep = null
      progress.isSplitParent = false
      store.dispatch('batchProgress/updateProgress', progress)
      return { status: 'failed', stageKey }
    }
  }

  async function runTaskPipeline(task, enabledStages, config, maxRetries, store, delayMs, scheduler) {
    for (const stageKey of enabledStages) {
      const result = await runOneStage(task, stageKey, config, maxRetries, store, delayMs, scheduler)

      if (result?.status === 'failed') {
        if (stageKey === 'preTranslate' && enabledStages.includes('translateExamine')) {
          continue
        }
        const progress = getProgressById(store, task.id)
        const stageIdx = enabledStages.indexOf(stageKey)
        for (let j = stageIdx + 1; j < enabledStages.length; j++) {
          progress.stages[enabledStages[j]] = 'skipped'
          for (const step of getStageSteps(enabledStages[j])) {
            progress.steps[enabledStages[j]][step.key] = 'skipped'
          }
        }
        store.dispatch('batchProgress/updateProgress', progress)
        break
      }
    }

    const progress = getProgressById(store, task.id)
    progress.currentStage = null
    progress.currentStep = null
    progress.isSplitParent = false
    store.dispatch('batchProgress/updateProgress', progress)
  }

  /**
   * 批量执行入口：全局 concurrency 槽位调度主任务阶段单元与拆分子块。
   */
  async function execute(config, store) {
    if (!isContinuous(config.stages)) {
      throw new Error('阶段选择必须连续，不能有空洞')
    }

    const enabledStages = getEnabledStages(config.stages)
    const maxRetries = config.maxRetries ?? 3
    const delayMs = config.stepDelayMs ?? DEFAULT_STEP_DELAY_MS
    const concurrency = Math.min(Math.max(config.concurrency ?? 1, 1), 100)
    const scheduler = createGlobalScheduler(concurrency)

    try {
      const pipelines = config.tasks.map(task =>
        runTaskPipeline(task, enabledStages, config, maxRetries, store, delayMs, scheduler)
      )
      await Promise.all(pipelines)
    } finally {
      store.dispatch('batchProgress/complete')
    }
  }

  return {
    execute,
    isContinuous,
    getToggleableStages,
    STAGES: STAGE_ORDER,
    getSplitLimit,
    shouldSplit,
    entriesDone
  }
}
