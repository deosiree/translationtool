// ==================== 原型 Mock 数据生成工具 ====================

function randInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

function randPick(list) {
  return list[randInt(0, list.length - 1)]
}

// 轻量 UUID（够用来做 mock 唯一 id / realKey；避免引入依赖）
function uuidLike() {
  // 例：c1f8a2d0-7b3c-4d1a-9e2f-3a7c8b9d0e1f
  const s4 = () => Math.floor((1 + Math.random()) * 0x10000).toString(16).slice(1)
  return `${s4()}${s4()}-${s4()}-${s4()}-${s4()}-${s4()}${s4()}${s4()}`
}

// selectedRows：必须，来自父组件的 dataA（已选词条），用于复用其 id，方便在校验弹窗中对齐 dataA
export function buildMockValidationResult(selectedRows) {
  if (!Array.isArray(selectedRows) || selectedRows.length === 0) {
    throw new Error('[buildMockValidationResult] 原型模式必须传入 dataA（selectedRows），否则无法生成校验数据')
  }

  // 初始 mock：按 dataA 严格展开为若干重复组
  const now = Date.now()
  const entryPool = ['SAVE', 'CANCEL', 'OK', 'NEXT', 'PREV', 'SUBMIT', 'SEARCH', 'DELETE', 'UPLOAD', 'DOWNLOAD']
  const translationPool = ['Save', 'Store', 'Cancel', 'Abort', 'OK', 'Next', 'Previous', 'Submit', 'Search', 'Delete', 'Upload', 'Download']
  const sourcePool = ['ts', 'dictionary', 'file', 'manual', 'import']
  const dictPool = ['common.dict', 'ui.dict', 'core.dict']
  const langPool = ['英文', '法文', '西文', '俄文']
  const tagPool = ['core', 'ui', 'common', 'biz', '-']
  const commentPool = ['from ts', 'from file', 'from dictionary', 'imported', '-', 'edited']
  const tsFilePool = ['home.ts', 'common.ts', 'login.ts', 'settings.ts', '-', '']

  const groups = []

  // 严格使用 dataA 中的每一个 id，且只用一次：
  // 1. 子元素总数 === dataA.length
  // 2. 不重复使用 id，也不丢任何一个 id
  const list = selectedRows.filter((item) => item && item.id)
  let cursor = 0
  let gIndex = 1

  while (cursor < list.length) {
    const remaining = list.length - cursor
    // 每组 2~5 条，但不能超过剩余数量
    const childCount = remaining <= 5 ? remaining : randInt(2, 5)

    const entry = randPick(entryPool)
    const baseTranslation = randPick(translationPool)
    const baseSource = randPick(sourcePool)
    const baseDi = baseSource === 'dictionary' ? randPick(dictPool) : '-'
    const baseLang = randPick(langPool)

    const children = []
    const baseTime = now - 1000 * 60 * randInt(0, 30)

    for (let j = 0; j < childCount && cursor < list.length; j += 1, cursor += 1) {
      const src = list[cursor]
      const child = {
        _type: 'child',
        // id：严格使用 dataA 中的 id，一次且仅一次
        id: src.id,
        realKey: uuidLike(),
        // 尝试复用外部 entry / 语种，否则用随机值补齐
        entry: src.entry || entry,
        translateType: src.translateType || baseLang,
        translation: baseTranslation,
        entrySource: baseSource,
        diFileName: baseDi,
        tag: randPick(tagPool),
        comment: randPick(commentPool),
        tsFileName: randPick(tsFilePool),
        updatedAt: baseTime + randInt(0, 1000 * 60 * 10),
      }
      if (child.entrySource === 'dictionary') {
        child.diFileName = randPick(dictPool)
      }
      children.push(child)
    }

    // 最新在前
    children.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0))

    groups.push({
      _type: 'parent',
      groupId: `g-${gIndex}`,
      entry,
      tag: randPick(tagPool),
      comment: randPick(commentPool),
      children,
      updatedAt: now - 1000 * 60 * randInt(0, 30),
    })

    gIndex += 1
  }

  return groups
}

export function buildMockSelectedEntries(count = 24) {
  const pool = [
    'SAVE',
    'CANCEL',
    'OK',
    'NEXT',
    'PREV',
    'SUBMIT',
    'SEARCH',
    'DELETE',
    'UPLOAD',
    'DOWNLOAD',
    'IMPORT',
    'EXPORT',
    'LOGIN',
    'LOGOUT',
    'RESET',
    'CONFIRM',
    'SETTINGS',
    'PROFILE',
    'LANGUAGE',
    'THEME',
    'HELP',
    'ABOUT',
    'RETRY',
    'SKIP',
    'FINISH',
  ]
  // EntryStateBadge 期望为数字状态码：
  // 0 新建 / 1 审核中 / 2 审核不通过 / 3 已审核 / -1 禁用
  const entryStatePool = [0, 1, 3, 2, -1]
  const list = []
  for (let i = 0; i < count; i += 1) {
    list.push({
      id: `mock-entry-${i + 1}`,
      entry: pool[i % pool.length],
      entryState: entryStatePool[i % entryStatePool.length],
    })
  }
  return list
}

