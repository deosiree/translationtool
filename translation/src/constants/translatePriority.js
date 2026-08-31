/**
 * 批量预翻译下拉框支持的翻译优先级选项。
 *
 * 下拉框展示与执行阶段告警共用这份数据，避免 value 与中文名称分散维护。
 */
export const TRANSLATE_PRIORITY_OPTIONS = [
  { label: '术语库', value: 'shuyuku' },
  { label: 'DeepL翻译', value: 'deepl' },
  { label: '有道翻译', value: 'youdao' },
  { label: '百度翻译', value: 'baidu' },
  { label: 'Google翻译', value: 'google' },
  { label: '本地模型', value: 'module' },
  { label: '综合优先级', value: 'synthesis' }
]

/**
 * 将翻译优先级 value 转换为下拉框对应的中文名称。
 * @param {string|null|undefined} value 翻译优先级 value
 * @returns {string} 优先级中文名称；未知 value 返回原始 value，空值返回默认文案
 */
export function getTranslatePriorityLabel(value) {
  return TRANSLATE_PRIORITY_OPTIONS.find(option => option.value === value)?.label || value || '当前翻译方法'
}
