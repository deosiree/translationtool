import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { JSDOM } from 'jsdom';

const htmlPath = new URL('../../prototype/syncManagement/sync-management-v2.html', import.meta.url);
const html = readFileSync(fileURLToPath(htmlPath), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)?.[1];
assert.ok(script, 'prototype inline script exists');
assert.doesNotThrow(() => new Function(script), 'prototype inline script parses');
const defaultEntryMatchOptions = { caseSensitive: false, exactMatch: false, regex: false };

/** 从原型内联脚本中提取具名顶层函数。
 * @param {string} name 函数名称。
 * @returns {string} 函数源码。
 */
function extractFunction(name) {
  const expression = new RegExp(`    function ${name}\\([^\\n]*\\) \\{[\\s\\S]*?\\n    \\}`);
  const match = script.match(expression);
  assert.ok(match, `${name} exists in the prototype`);
  return match[0];
}

/** 在隔离的依赖作用域中装载原型函数。
 * @param {string} name 函数名称。
 * @param {Object<string, any>} dependencies 函数依赖。
 * @returns {Function} 可独立调用的函数。
 */
function loadFunction(name, dependencies = {}) {
  const scope = Object.assign(Object.create(globalThis), dependencies);
  return new Function('scope', `with (scope) { return (${extractFunction(name)}); }`)(scope);
}

/** 创建用于筛选验证的词条译文行。
 * @param {number} rowIndex 稳定行索引。
 * @param {string} language 语种代码。
 * @param {string} incoming 远端译文。
 * @param {string} current 本地译文。
 * @param {string} syncBase 同步基线译文。
 * @param {boolean} requiresDecision 是否需要用户决策。
 * @returns {Object<string, any>} 可供原型筛选器处理的行。
 */
function makeRow(rowIndex, language, incoming, current, syncBase, requiresDecision = true) {
  return {
    rowIndex,
    term: 'Create menu item',
    language,
    incoming,
    current,
    syncBase,
    status: '3',
    requiresDecision
  };
}

/** 按指定已应用条件运行真实的原型筛选函数。
 * @param {Object<string, any>[]} rows 同一词条的译文行。
 * @param {Object<string, any>} overrides 覆盖默认筛选条件。
 * @returns {Object<string, any>[]} 匹配的译文行。
 */
function filterRows(rows, overrides = {}) {
  const applied = {
    term: '',
    difference: 'all',
    languages: [],
    status: '',
    translation: '',
    modes: { term: defaultEntryMatchOptions, translation: defaultEntryMatchOptions },
    ...overrides
  };
  const getMatchingEntryRows = loadFunction('getMatchingEntryRows', {
    entryFilterState: { applied },
    entryFilterLanguages: ['en_US', 'ru_RU'],
    entryFilterAllStatuses: '__all__',
    getEntryDifference: difference,
    createEntryTextMatcher: (query, options = {}) => {
      const normalized = String(query || '').toLocaleLowerCase();
      return (value) => {
        const candidate = String(value || '').toLocaleLowerCase();
        return options.exactMatch ? candidate === normalized : candidate.includes(normalized);
      };
    }
  });
  return getMatchingEntryRows(rows);
}

const difference = loadFunction('getEntryDifference');
const remoteOnly = makeRow(1, 'en_US', 'Remote text', 'Base text', 'Base text');
const localOnly = makeRow(2, 'ru_RU', 'Base text', 'Local text', 'Base text');
const bothDifferent = makeRow(3, 'en_US', 'Remote text', 'Local text', 'Base text');
const bothEqual = makeRow(4, 'ru_RU', 'Updated text', 'Updated text', 'Base text');
const unchanged = makeRow(5, 'en_US', 'Base text', 'Base text', 'Base text');

assert.equal(difference(remoteOnly).type, 'remote-only');
assert.equal(difference(localOnly).type, 'local-only');
assert.equal(difference(bothDifferent).type, 'both');
assert.equal(difference(bothEqual).type, 'both', 'equal final translations still count when both differ from baseline');
assert.equal(difference(unchanged).type, 'none');

const rows = [remoteOnly, localOnly, bothDifferent, bothEqual, unchanged];
assert.deepEqual(filterRows(rows, { difference: 'remote-only' }), [remoteOnly]);
assert.deepEqual(filterRows(rows, { difference: 'local-only' }), [localOnly]);
assert.deepEqual(filterRows(rows, { difference: 'both' }), [bothDifferent, bothEqual]);
assert.deepEqual(filterRows(rows, { difference: 'all' }), rows);
assert.deepEqual(filterRows(rows, { difference: 'both', term: 'menu' }), [bothDifferent, bothEqual]);
assert.deepEqual(filterRows(rows, {
  difference: 'both',
  translation: 'remote'
}), [bothDifferent], 'difference and translation text conditions intersect');
assert.deepEqual(filterRows(rows, {
  difference: 'both',
  status: '2'
}), [], 'difference and status conditions intersect');
assert.deepEqual(filterRows(rows, { difference: 'both', languages: ['ru_RU'] }), [bothEqual]);
assert.deepEqual(filterRows(rows, { difference: 'both', languages: ['en_US', 'ru_RU'] }), [bothDifferent, bothEqual]);

const dom = new JSDOM(html);
const { document } = dom.window;
const options = [...document.querySelectorAll('#entryDifferenceFilter option')];
assert.deepEqual(options.map((option) => option.value), ['all', 'remote-only', 'local-only', 'both']);
assert.deepEqual(options.map((option) => option.textContent.trim()), [
  '不限差异',
  '仅有远端译文更新',
  '仅有本地译文更新',
  '远端译文和本地译文均更新'
]);
assert.match(html, /\.entry-condition-list input\[type="checkbox"\]:focus:not\(:focus-visible\)(?:,\.entry-language-options input\[type="checkbox"\]:focus:not\(:focus-visible\))?\s*\{[^}]*box-shadow:none/);
assert.match(html, /select:focus,input:focus,textarea:focus\s*\{[^}]*border-color:[^;]+;\s*box-shadow:[^;]+;/);
assert.match(html, /\.entry-condition-list input\[type="checkbox"\]\s*\{[^}]*margin:0/);
assert.match(html, /\.entry-condition-list label\s*\{[^}]*gap:4px/);
assert.match(html, /\.entry-filter-row \.entry-filter-control\s*\{\s*align-items:stretch/);

const appliedDom = new JSDOM('<div id="entryTableScroll"></div>');
const appliedState = {
  draft: { term: '', difference: 'both', languages: [], status: '', translation: '', modes: { term: {}, translation: {} } },
  applied: { term: '', difference: 'all', languages: [], status: '', translation: '', modes: { term: {}, translation: {} } }
};
let selectionCleared = false;
let rendered = false;
const applyEntryFilters = loadFunction('applyEntryFilters', {
  entryFilterState: appliedState,
  entryFilterLanguages: ['en_US'],
  translationLanguageNames: { en_US: '英文' },
  defaultEntryMatchOptions,
  createEntryTextMatcher: () => () => true,
  selectedEntryRows: { clear: () => { selectionCleared = true; } },
  document: appliedDom.window.document,
  renderCurrentEntryFilters: () => { rendered = true; }
});
appliedDom.window.document.getElementById('entryTableScroll').scrollTop = 50;
applyEntryFilters();
assert.equal(appliedState.applied.difference, 'both');
assert.equal(selectionCleared, true);
assert.equal(rendered, true);
assert.equal(appliedDom.window.document.getElementById('entryTableScroll').scrollTop, 0);

const resetDom = new JSDOM('<input id="entryTermFilter"><select id="entryDifferenceFilter"><option value="all"></option><option value="both"></option></select><div id="entryTableScroll"></div><input data-entry-condition checked><button data-entry-mode-field="term" data-mode="regex" aria-pressed="true"></button>');
const resetState = {
  draft: { term: 'menu', difference: 'both', languages: ['en_US'], status: '3', translation: 'Remote', modes: { term: { regex: true }, translation: {} } },
  applied: { term: 'menu', difference: 'both', languages: ['en_US'], status: '3', translation: 'Remote', modes: { term: { regex: true }, translation: {} } },
  visibleConditions: new Set(['translation:en_US', 'status:en_US'])
};
let resetSelectionCleared = false;
let resetRendered = false;
const resetEntryFilters = loadFunction('resetEntryFilters', {
  entryFilterState: resetState,
  createEntryFilterValues: loadFunction('createEntryFilterValues', {
    defaultEntryMatchOptions
  }),
  selectedEntryRows: { clear: () => { resetSelectionCleared = true; } },
  document: resetDom.window.document,
  renderEntryFilterControls: () => {},
  renderCurrentEntryFilters: () => { resetRendered = true; }
});
resetEntryFilters();
assert.equal(resetState.draft.difference, 'all');
assert.equal(resetState.applied.difference, 'all');
assert.equal(resetDom.window.document.getElementById('entryDifferenceFilter').value, 'all');
assert.equal(resetState.visibleConditions.size, 0);
assert.equal(resetSelectionCleared, true);
assert.equal(resetRendered, true);
assert.equal(resetDom.window.document.querySelector('[data-entry-condition]').checked, false);
assert.equal(resetDom.window.document.querySelector('[data-entry-mode-field="term"]').getAttribute('aria-pressed'), 'false');

const selectedRows = new Set();
const visibleRows = filterRows([remoteOnly, localOnly, bothDifferent], { difference: 'remote-only' });
let selectionStateSynced = false;
const setVisibleEntrySelection = loadFunction('setVisibleEntrySelection', {
  entryVirtualState: { groups: [visibleRows] },
  selectedEntryRows: selectedRows,
  syncEntrySelectionControls: () => { selectionStateSynced = true; }
});
setVisibleEntrySelection(true);
assert.deepEqual([...selectedRows], [remoteOnly.rowIndex]);
assert.equal(selectionStateSynced, true);

const autoAccepted = makeRow(6, 'en_US', 'Remote text', 'Base text', 'Base text', false);
selectedRows.clear();
const selectVisibleWithAutoAccepted = loadFunction('setVisibleEntrySelection', {
  entryVirtualState: { groups: [[remoteOnly, autoAccepted]] },
  selectedEntryRows: selectedRows,
  syncEntrySelectionControls: () => {}
});
selectVisibleWithAutoAccepted(true);
assert.deepEqual([...selectedRows], [remoteOnly.rowIndex], 'automatically accepted rows are not batch-selectable');

console.log('sync management entry difference checks passed');
