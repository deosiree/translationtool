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

// ==================== 完整词条 Mock 数据生成 ====================
/**
 * 生成包含所有导出字段的完整词条mock数据
 * 基于 commonParam.js 中的 entry_exportFields 定义
 * @param {Object} options - 配置选项
 * @param {number} options.count - 生成数量，默认1
 * @param {Object} options.overrides - 覆盖特定字段的值
 * @returns {Array|Object} 如果count=1返回单个对象，否则返回数组
 */
export function buildMockEntryWithAllFields(options = {}) {
  const { count = 1, overrides = {} } = options

  // 数据池定义
  const entryPool = ['SAVE', 'CANCEL', 'OK', 'NEXT', 'PREV', 'SUBMIT', 'SEARCH', 'DELETE', 'UPLOAD', 'DOWNLOAD', 'IMPORT', 'EXPORT', 'LOGIN', 'LOGOUT', 'RESET', 'CONFIRM', 'SETTINGS', 'PROFILE', 'LANGUAGE', 'THEME']
  const tagPool = ['core', 'ui', 'common', 'biz', 'system', 'user', 'admin']
  const commentPool = ['from ts', 'from file', 'from dictionary', 'imported', 'edited', 'auto-generated', 'manual entry']
  const entrySourcePool = ['ts', 'dictionary', 'file', 'manual', 'import', 'api']
  const dictPool = ['common.dict', 'ui.dict', 'core.dict', 'business.dict', 'system.dict']
  const classfy1Pool = ['系统', '界面', '业务', '通用', '用户', '管理']
  const classfy2Pool = ['按钮', '菜单', '提示', '错误', '成功', '警告', '信息']
  const productNamePool = ['产品A', '产品B', '产品C', '核心系统', '管理平台']
  const versionNamePool = ['v1.0.0', 'v1.1.0', 'v2.0.0', 'v2.1.0', 'v3.0.0']
  const partOfSpeechPool = ['名词', '动词', '形容词', '副词', '介词', '连词', '-']
  const updateUserPool = ['admin', 'user001', 'translator01', 'editor01', 'system']
  const abbrPool = ['SAV', 'CNL', 'OK', 'NXT', 'PRV', 'SMT', 'SRC', 'DEL', 'UPL', 'DWN']

  // 翻译数据池
  const chineseTranslationPool = ['保存', '取消', '确定', '下一步', '上一步', '提交', '搜索', '删除', '上传', '下载']
  const chineseInterpretationPool = ['保存操作', '取消当前操作', '确认执行', '进入下一步', '返回上一步', '提交表单', '搜索内容', '删除项目', '上传文件', '下载文件']
  const englishTranslationPool = ['Save', 'Cancel', 'OK', 'Next', 'Previous', 'Submit', 'Search', 'Delete', 'Upload', 'Download']
  const englishInterpretationPool = ['Save the current state', 'Cancel the operation', 'Confirm action', 'Go to next step', 'Go to previous step', 'Submit form', 'Search content', 'Delete item', 'Upload file', 'Download file']
  const russianTranslationPool = ['Сохранить', 'Отмена', 'ОК', 'Далее', 'Назад', 'Отправить', 'Поиск', 'Удалить', 'Загрузить', 'Скачать']
  const russianInterpretationPool = ['Сохранить текущее состояние', 'Отменить операцию', 'Подтвердить действие', 'Перейти к следующему шагу', 'Вернуться назад', 'Отправить форму', 'Поиск содержимого', 'Удалить элемент', 'Загрузить файл', 'Скачать файл']
  const spanishTranslationPool = ['Guardar', 'Cancelar', 'OK', 'Siguiente', 'Anterior', 'Enviar', 'Buscar', 'Eliminar', 'Subir', 'Descargar']
  const spanishInterpretationPool = ['Guardar el estado actual', 'Cancelar la operación', 'Confirmar acción', 'Ir al siguiente paso', 'Volver al paso anterior', 'Enviar formulario', 'Buscar contenido', 'Eliminar elemento', 'Subir archivo', 'Descargar archivo']
  const frenchTranslationPool = ['Enregistrer', 'Annuler', 'OK', 'Suivant', 'Précédent', 'Envoyer', 'Rechercher', 'Supprimer', 'Télécharger', 'Téléverser']
  const frenchInterpretationPool = ['Enregistrer l\'état actuel', 'Annuler l\'opération', 'Confirmer l\'action', 'Aller à l\'étape suivante', 'Revenir à l\'étape précédente', 'Envoyer le formulaire', 'Rechercher le contenu', 'Supprimer l\'élément', 'Téléverser le fichier', 'Télécharger le fichier']

  // 生成单个mock词条
  function generateSingleEntry(index = 0) {
    const entry = randPick(entryPool)
    const entryLength = entry.length
    const entrySource = randPick(entrySourcePool)
    const diFileName = entrySource === 'dictionary' ? randPick(dictPool) : '-'
    
    // 生成翻译数据
    const poolIndex = index % chineseTranslationPool.length
    const chinese = chineseTranslationPool[poolIndex]
    const chineseInterpretation = chineseInterpretationPool[poolIndex]
    const english = englishTranslationPool[poolIndex]
    const englishInterpretation = englishInterpretationPool[poolIndex]
    const russian = russianTranslationPool[poolIndex]
    const russianInterpretation = russianInterpretationPool[poolIndex]
    const spanish = spanishTranslationPool[poolIndex]
    const spanishInterpretation = spanishInterpretationPool[poolIndex]
    const french = frenchTranslationPool[poolIndex]
    const frenchInterpretation = frenchInterpretationPool[poolIndex]

    // 生成字符数
    const enCharLength = english.length
    const zhCharLength = chinese.length
    const ruCharLength = russian.length
    const spaCharLength = spanish.length
    const fraCharLength = french.length

    // 生成长度限制（通常比实际字符数大一些）
    const maxChineseLength = randInt(zhCharLength + 5, zhCharLength + 20)
    const foreignMaxLength = randInt(Math.max(enCharLength, ruCharLength, spaCharLength, fraCharLength) + 5, 50)
    const maxLength = Math.max(maxChineseLength, foreignMaxLength)

    // 生成时间（最近30天内）
    const now = Date.now()
    const updateTime = new Date(now - 1000 * 60 * 60 * 24 * randInt(0, 30)).toISOString().replace('T', ' ').substring(0, 19)

    // 构建完整的mock数据对象
    const mockEntry = {
      // 基础字段
      entry: entry,
      tag: randPick(tagPool),
      comment: randPick(commentPool),
      entryLength: entryLength,
      entrySource: entrySource,
      
      // 中文相关
      chineseInterpretation: chineseInterpretation,
      chinese: chinese,
      
      // 英文相关
      englishInterpretation: englishInterpretation,
      english: english,
      
      // 俄文相关
      russianInterpretation: russianInterpretation,
      russian: russian,
      
      // 西文相关
      spanishInterpretation: spanishInterpretation,
      spanish: spanish,
      
      // 法文相关
      frenchInterpretation: frenchInterpretation,
      french: french,
      
      // 分类相关
      classfy1: randPick(classfy1Pool),
      classfy2: randPick(classfy2Pool),
      classifyId: randInt(1000, 9999),
      
      // 文件相关
      diFileName: diFileName,
      
      // 备注
      remark: randPick(commentPool),
      
      // 长度限制
      maxChineseLength: maxChineseLength,
      foreignMaxLength: foreignMaxLength,
      maxLength: maxLength,
      
      // 术语字符数
      enCharLength: enCharLength,
      zhCharLength: zhCharLength,
      ruCharLength: ruCharLength,
      spaCharLength: spaCharLength,
      fraCharLength: fraCharLength,
      
      // 更新信息
      update: randPick(updateUserPool),
      updateTime: updateTime,
      
      // 词性备注
      partOfSpeech: randPick(partOfSpeechPool),
      
      // 产品版本
      productName: randPick(productNamePool),
      versionName: randPick(versionNamePool),
      
      // 缩写
      abbr: randPick(abbrPool),
    }

    // 应用覆盖值
    return { ...mockEntry, ...overrides }
  }

  // 根据count返回单个对象或数组
  if (count === 1) {
    return generateSingleEntry(0)
  }

  const result = []
  for (let i = 0; i < count; i += 1) {
    result.push(generateSingleEntry(i))
  }
  console.log("mock数据",result)
  return result
}

// ==================== 选中词条 Mock 数据生成（使用完整字段） ====================
/**
 * 生成选中词条的mock数据，包含所有导出字段
 * 基于 buildMockEntryWithAllFields，额外添加 id 和 entryState 字段
 * @param {number} count - 生成数量，默认24
 * @returns {Array} 包含完整字段的词条数组
 */
export function buildMockSelectedEntries(count = 24) {
  // EntryStateBadge 期望为数字状态码：
  // 0 新建 / 1 审核中 / 2 审核不通过 / 3 已审核 / -1 禁用
  const entryStatePool = [0, 1, 3, 2, -1]
  
  // 使用完整mock数据生成函数，生成包含所有字段的数据
  const fullMockEntries = buildMockEntryWithAllFields({ count })
  
  // 为每个词条添加 id 和 entryState 字段
  return fullMockEntries.map((entry, index) => ({
    ...entry,
    id: entry.id || `mock-entry-${index + 1}`,
    entryState: entryStatePool[index % entryStatePool.length],
  }))
}
