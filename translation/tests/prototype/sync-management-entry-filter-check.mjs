import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const htmlPath = new URL('../../prototype/syncManagement/sync-management-v2.html', import.meta.url);
const html = readFileSync(fileURLToPath(htmlPath), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)?.[1];
assert.ok(script, 'prototype inline script exists');
assert.doesNotThrow(() => new Function(script), 'prototype inline script parses');

/** 从页面内联脚本中提取一个顶层函数。
 * @param {string} name 函数名称。
 * @returns {string} 函数源码。
 */
function extractFunction(name) {
  const expression = new RegExp('    function ' + name + '\\([^\\n]*\\) \\{[\\s\\S]*?\\n    \\}');
  const match = script.match(expression);
  assert.ok(match, name + ' exists in the prototype');
  return match[0];
}

/** 在隔离作用域中装载页面函数。
 * @param {string} name 函数名称。
 * @param {Object<string, any>} dependencies 函数运行时依赖。
 * @returns {Function} 可独立调用的页面函数。
 */
function loadFunction(name, dependencies = {}) {
  const scope = Object.assign(Object.create(globalThis), dependencies);
  return new Function('scope', 'with (scope) { return (' + extractFunction(name) + '); }')(scope);
}

/** 创建一份独立筛选条件。
 * @returns {Object<string, any>} 空白筛选条件和默认匹配模式。
 */
function createFilters() {
  return {
    term: '',
    difference: 'all',
    languages: [],
    status: '',
    translation: '',
    modes: {
      term: { caseSensitive: false, exactMatch: false, regex: false },
      translation: { caseSensitive: false, exactMatch: false, regex: false }
    }
  };
}

/** 转义筛选控件渲染测试中的 HTML 文本。
 * @param {string} value 原始文本。
 * @returns {string} HTML 转义文本。
 */
function escapeHtml(value) {
  return String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

const conditionNames = [...html.matchAll(/data-entry-condition="([^"]+)"/g)].map((match) => match[1]);
assert.deepEqual(conditionNames, ['language', 'status', 'translation'], '展示条件只有语种、状态和结果三项');
assert.doesNotMatch(html, /data-entry-condition="(?:translation|status):/, '旧的逐语种展示条件已移除');
assert.match(html, /\.entry-condition-list input\[type="checkbox"\]:focus:not\(:focus-visible\),\.entry-language-options input\[type="checkbox"\]:focus:not\(:focus-visible\)\s*\{\s*border-color:var\(--line\);\s*box-shadow:none;\s*\}/, '展示条件和翻译语种鼠标焦点样式一致');

const languages = ['en_US', 'ru_RU', 'es_ES', 'zh_CN'];
const languageNames = { en_US: '英文', ru_RU: '俄文', es_ES: '西文', zh_CN: '中文' };
const statusOptions = [['0', '未翻译'], ['1', '待审核'], ['2', '已审核']];
const rows = [
  { language: 'en_US', status: '1', term: '曲线标题', incoming: 'Remote Alpha', current: 'Local Alpha', syncBase: 'Base Alpha', difference: 'remote-only' },
  { language: 'ru_RU', status: '0', term: '曲线标题', incoming: 'Remote Beta', current: 'Local Beta', syncBase: 'Base Beta', difference: 'local-only' },
  { language: 'es_ES', status: '1', term: '曲线标题', incoming: 'Remote Gamma', current: 'Local Gamma', syncBase: 'Base Gamma', difference: 'both' },
  { language: 'zh_CN', status: '2', term: '曲线标题', incoming: 'Remote Delta', current: 'Local Delta', syncBase: 'Base Delta', difference: 'remote-only' }
];
const matchOptions = { caseSensitive: false, exactMatch: false, regex: false };
const entryFilterState = { applied: createFilters() };
const createEntryTextMatcher = loadFunction('createEntryTextMatcher', { defaultEntryMatchOptions: matchOptions });
const getMatchingEntryRows = loadFunction('getMatchingEntryRows', {
  entryFilterState,
  entryFilterAllStatuses: 'all',
  createEntryTextMatcher,
  getEntryDifference: (row) => ({ type: row.difference })
});

assert.deepEqual(getMatchingEntryRows(rows), rows, '空筛选保留所有差异候选行');
entryFilterState.applied = { ...createFilters(), languages: ['en_US', 'ru_RU'] };
assert.deepEqual(getMatchingEntryRows(rows).map((row) => row.language), ['en_US', 'ru_RU'], '多语种筛选按并集返回行');
entryFilterState.applied = { ...createFilters(), status: '1' };
assert.deepEqual(getMatchingEntryRows(rows).map((row) => row.language), ['en_US', 'es_ES'], '未选语种时状态筛选覆盖全部语种');
entryFilterState.applied = { ...createFilters(), languages: ['en_US', 'ru_RU'], status: '1', translation: 'Local' };
assert.deepEqual(getMatchingEntryRows(rows).map((row) => row.language), ['en_US'], '语种、状态和译文条件按交集匹配');
entryFilterState.applied = { ...createFilters(), languages: ['ru_RU'], status: '0', translation: 'Base Beta' };
assert.deepEqual(getMatchingEntryRows(rows).map((row) => row.language), ['ru_RU'], '翻译结果可匹配同步基线');
entryFilterState.applied = { ...createFilters(), difference: 'local-only', languages: ['en_US', 'ru_RU'] };
assert.deepEqual(getMatchingEntryRows(rows).map((row) => row.language), ['ru_RU'], '差异筛选与语种筛选继续叠加');
entryFilterState.applied = { ...createFilters(), languages: ['en_US'], status: 'all' };
assert.deepEqual(getMatchingEntryRows(rows).map((row) => row.language), ['en_US'], '全部状态仍受语种范围限制');

entryFilterState.applied = {
  ...createFilters(),
  translation: 'Remote (Alpha|Beta)',
  modes: { term: { ...matchOptions }, translation: { ...matchOptions, regex: true } }
};
assert.deepEqual(getMatchingEntryRows(rows).map((row) => row.language), ['en_US', 'ru_RU'], '正则模式作用于统一翻译结果条件');
entryFilterState.applied = {
  ...createFilters(),
  translation: 'Remote Alpha',
  modes: { term: { ...matchOptions }, translation: { ...matchOptions, exactMatch: true } }
};
assert.deepEqual(getMatchingEntryRows(rows).map((row) => row.language), ['en_US'], '精确匹配模式保留');
entryFilterState.applied = {
  ...createFilters(),
  translation: 'REMOTE',
  modes: { term: { ...matchOptions }, translation: { ...matchOptions, caseSensitive: true } }
};
assert.deepEqual(getMatchingEntryRows(rows), [], '区分大小写模式保留');
assert.equal(createEntryTextMatcher('[', { ...matchOptions, regex: true }), null, '无效正则返回空匹配器');

const applyState = {
  draft: {
    ...createFilters(),
    term: 'curve',
    difference: 'both',
    languages: ['en_US', 'ru_RU'],
    status: '1',
    translation: 'Remote',
    modes: {
      term: { ...matchOptions, exactMatch: true },
      translation: { ...matchOptions, regex: true }
    }
  },
  applied: { ...createFilters(), term: 'previous' }
};
const applySelection = new Set([2, 4]);
const applyNodes = new Map(Object.entries({ entryTableScroll: { scrollTop: 64 }, entryLanguagePicker: { open: true } }));
const applyRenderCalls = new Set();
const applyToasts = [];
const applyEntryFilters = loadFunction('applyEntryFilters', {
  entryFilterState: applyState,
  selectedEntryRows: applySelection,
  createEntryTextMatcher,
  showToast: applyToasts.push.bind(applyToasts),
  document: { getElementById: applyNodes.get.bind(applyNodes) },
  renderCurrentEntryFilters: applyRenderCalls.add.bind(applyRenderCalls)
});
applyEntryFilters();
assert.deepEqual(applyState.applied, applyState.draft, '应用时复制全部草稿筛选值');
assert.notEqual(applyState.applied, applyState.draft, '已应用条件与草稿对象分离');
assert.notEqual(applyState.applied.languages, applyState.draft.languages, '语种数组独立复制');
assert.notEqual(applyState.applied.modes.translation, applyState.draft.modes.translation, '匹配模式独立复制');
applyState.draft.languages.push('es_ES');
applyState.draft.modes.translation.regex = false;
assert.deepEqual(applyState.applied.languages, ['en_US', 'ru_RU'], '后续草稿编辑不改动已应用语种');
assert.equal(applyState.applied.modes.translation.regex, true, '后续草稿编辑不改动已应用匹配模式');
assert.equal(applySelection.size, 0, '应用条件清空旧的行选择');
assert.equal(applyNodes.get('entryTableScroll').scrollTop, 0, '应用条件回到列表顶部');
assert.equal(applyNodes.get('entryLanguagePicker').open, false, '应用条件关闭语种菜单');
assert.equal(applyRenderCalls.size, 1, '应用条件只安排一次列表渲染');

for (const invalidField of ['term', 'translation']) {
  const invalidDraft = createFilters();
  invalidDraft[invalidField] = '[';
  invalidDraft.modes[invalidField].regex = true;
  const previousApplied = { ...createFilters(), term: 'previous' };
  const invalidState = { draft: invalidDraft, applied: previousApplied };
  const invalidSelection = new Set([3]);
  const invalidNodes = new Map(Object.entries({ entryTableScroll: { scrollTop: 72 }, entryLanguagePicker: { open: true } }));
  const invalidToasts = [];
  const invalidRenderCalls = new Set();
  loadFunction('applyEntryFilters', {
    entryFilterState: invalidState,
    selectedEntryRows: invalidSelection,
    createEntryTextMatcher,
    showToast: invalidToasts.push.bind(invalidToasts),
    document: { getElementById: invalidNodes.get.bind(invalidNodes) },
    renderCurrentEntryFilters: invalidRenderCalls.add.bind(invalidRenderCalls)
  })();
  assert.equal(invalidState.applied, previousApplied, invalidField + ' 无效正则不覆盖已应用条件');
  assert.equal(invalidToasts.length, 1, invalidField + ' 无效正则显示错误反馈');
  assert.equal(invalidSelection.size, 1, invalidField + ' 无效正则保留行选择');
  assert.equal(invalidNodes.get('entryTableScroll').scrollTop, 72, invalidField + ' 无效正则不改变滚动位置');
  assert.equal(invalidNodes.get('entryLanguagePicker').open, true, invalidField + ' 无效正则不关闭语种菜单');
  assert.equal(invalidRenderCalls.size, 0, invalidField + ' 无效正则不触发列表渲染');
}

const hiddenConditionState = {
  visibleConditions: new Set(['language', 'status', 'translation']),
  draft: { ...createFilters(), languages: ['en_US', 'ru_RU'], status: '1', translation: 'Remote' },
  applied: { ...createFilters(), languages: ['en_US'], status: '2', translation: 'Base' }
};
const setEntryConditionVisibility = loadFunction('setEntryConditionVisibility', { entryFilterState: hiddenConditionState });
setEntryConditionVisibility('language', false);
assert.deepEqual(hiddenConditionState.draft.languages, [], '隐藏语种条件清除语种草稿');
assert.equal(hiddenConditionState.draft.status, '1', '隐藏语种条件保留状态草稿');
assert.equal(hiddenConditionState.draft.translation, 'Remote', '隐藏语种条件保留译文草稿');
setEntryConditionVisibility('status', false);
assert.equal(hiddenConditionState.draft.status, '', '隐藏状态条件清除状态草稿');
assert.equal(hiddenConditionState.draft.translation, 'Remote', '隐藏状态条件保留译文草稿');
setEntryConditionVisibility('translation', false);
assert.equal(hiddenConditionState.draft.translation, '', '隐藏译文条件清除译文草稿');
assert.equal(hiddenConditionState.visibleConditions.size, 0, '隐藏条件从展示集合移除');
assert.deepEqual(hiddenConditionState.applied.languages, ['en_US'], '隐藏条件不提前修改已应用语种');
assert.equal(hiddenConditionState.applied.status, '2', '隐藏条件不提前修改已应用状态');
assert.equal(hiddenConditionState.applied.translation, 'Base', '隐藏条件不提前修改已应用译文');
setEntryConditionVisibility('status', true);
assert.equal(hiddenConditionState.visibleConditions.has('status'), true, '重新显示条件时恢复展示状态');

const controlContainer = { innerHTML: '' };
const controlState = {
  visibleConditions: new Set(['language', 'status', 'translation']),
  draft: {
    ...createFilters(),
    languages: ['ru_RU', 'zh_CN'],
    status: '1',
    translation: 'Remote',
    modes: { term: { ...matchOptions }, translation: { ...matchOptions, caseSensitive: true } }
  }
};
const renderEntryMatchButtons = loadFunction('renderEntryMatchButtons', {
  defaultEntryMatchOptions: matchOptions,
  escapeHtml
});
loadFunction('renderEntryFilterControls', {
  document: { getElementById: () => controlContainer },
  entryFilterState: controlState,
  entryFilterLanguages: languages,
  translationLanguageNames: languageNames,
  entryTranslateStatusOptions: statusOptions,
  entryFilterAllStatuses: 'all',
  defaultEntryMatchOptions: matchOptions,
  escapeHtml,
  renderEntryMatchButtons
})();
assert.match(controlContainer.innerHTML, /data-entry-filter-language="ru_RU" checked/);
assert.match(controlContainer.innerHTML, /data-entry-filter-language="zh_CN" checked/);
assert.match(controlContainer.innerHTML, /俄文、中文/);
assert.match(controlContainer.innerHTML, /<select data-entry-filter-status aria-label="翻译状态">/);
assert.match(controlContainer.innerHTML, /data-entry-filter-text="translation"/);
assert.match(controlContainer.innerHTML, /data-entry-mode-field="translation"/, '译文高级匹配控件共用一个字段');

const resetState = {
  draft: { ...createFilters(), term: 'curve', difference: 'both', languages: ['en_US'], status: '1', translation: 'Remote' },
  applied: { ...createFilters(), term: 'curve', difference: 'both', languages: ['en_US'], status: '1', translation: 'Remote' },
  visibleConditions: new Set(['language', 'status', 'translation'])
};
const resetInputs = [{ checked: true }, { checked: true }, { checked: true }];
const resetTable = { scrollTop: 88 };
const resetRows = new Set([3]);
loadFunction('resetEntryConditionDisplay', {
  entryFilterState: resetState,
  selectedEntryRows: resetRows,
  document: {
    querySelectorAll: () => resetInputs,
    getElementById: () => resetTable
  },
  renderEntryFilterControls: () => {},
  renderCurrentEntryFilters: () => {}
})();
assert.equal(resetState.draft.term, 'curve', '展示条件重置保留词条查询');
assert.equal(resetState.draft.difference, 'both', '展示条件重置保留差异条件');
assert.deepEqual(resetState.draft.languages, []);
assert.equal(resetState.draft.status, '');
assert.equal(resetState.draft.translation, '');
assert.deepEqual(resetState.applied.languages, []);
assert.equal(resetState.applied.status, '');
assert.equal(resetState.applied.translation, '');
assert.equal(resetState.visibleConditions.size, 0);
assert.equal(resetRows.size, 0);
assert.equal(resetTable.scrollTop, 0);

const resetFilterState = {
  draft: { ...createFilters(), term: 'curve', languages: ['en_US'], status: '1', translation: 'Remote' },
  applied: { ...createFilters(), term: 'curve', languages: ['en_US'], status: '1', translation: 'Remote' },
  visibleConditions: new Set(['language', 'status', 'translation'])
};
const resetConditionInputs = [{ checked: true }, { checked: true }, { checked: true }];
const resetModeButtons = [{ setAttribute: (name, value) => { resetModeButtons[0][name] = value; } }];
const resetFilterNodes = {
  entryTermFilter: { value: 'curve' },
  entryDifferenceFilter: { value: 'both' },
  entryTableScroll: { scrollTop: 88 }
};
const resetFilterSelection = new Set([3]);
loadFunction('resetEntryFilters', {
  entryFilterState: resetFilterState,
  selectedEntryRows: resetFilterSelection,
  createEntryFilterValues: loadFunction('createEntryFilterValues', { defaultEntryMatchOptions: matchOptions }),
  document: {
    getElementById: (id) => resetFilterNodes[id],
    querySelectorAll: (selector) => selector.includes('data-entry-condition') ? resetConditionInputs : resetModeButtons
  },
  renderEntryFilterControls: () => {},
  renderCurrentEntryFilters: () => {}
})();
assert.equal(resetFilterState.draft.term, '');
assert.equal(resetFilterState.draft.difference, 'all');
assert.deepEqual(resetFilterState.draft.languages, []);
assert.equal(resetFilterState.draft.status, '');
assert.equal(resetFilterState.draft.translation, '');
assert.equal(resetFilterState.visibleConditions.size, 0);
assert.equal(resetFilterNodes.entryTermFilter.value, '');
assert.equal(resetFilterNodes.entryDifferenceFilter.value, 'all');
assert.equal(resetFilterNodes.entryTableScroll.scrollTop, 0);
assert.equal(resetFilterSelection.size, 0);

console.log('sync management entry filter checks passed');
