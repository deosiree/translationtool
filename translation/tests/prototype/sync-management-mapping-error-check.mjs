import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { JSDOM } from 'jsdom';
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
  const expression = new RegExp(`    function ${name}\\([^\\n]*\\) \\{[\\s\\S]*?\\n    \\}`);
  const match = script.match(expression);
  assert.ok(match, `${name} exists in the prototype`);
  return match[0];
}

/** 在隔离的页面依赖对象中装载一个原型函数。
 * @param {string} name 函数名称。
 * @param {Object<string, any>} dependencies 函数运行时依赖。
 * @returns {Function} 可独立调用的页面函数。
 */
function loadFunction(name, dependencies = {}) {
  const scope = Object.assign(Object.create(globalThis), dependencies);
  const loadedFunction = new Function('scope', `with (scope) { return (${extractFunction(name)}); }`)(scope);
  Object.defineProperty(loadedFunction, 'scope', { value: scope });
  return loadedFunction;
}

/** 转义测试渲染使用的 HTML 文本。
 * @param {string} value 待转义文本。
 * @returns {string} HTML 安全文本。
 */
function escapeHtml(value) {
  return String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

const errorFile = {
  kind: 'ts',
  path: 'ts/curve_base_widget.ts',
  generationPath: 'en_US/ts/curve_base_widget_en_US.ts',
  sourcePaths: [
    'en_US/ts/curve_base_widget_en_US.ts',
    'es_ES/ts/curve_base_widget_es_ES.ts',
    'ru_RU/ts/curve_base_widget_ru_RU.ts'
  ],
  mappingErrorBlockIndex: 0,
  sourceFiles: [
    { path: 'en_US/ts/curve_base_widget_en_US.ts', declaredLanguage: 'es_ES' },
    { path: 'es_ES/ts/curve_base_widget_es_ES.ts', declaredLanguage: 'es_ES' },
    { path: 'ru_RU/ts/curve_base_widget_ru_RU.ts', declaredLanguage: 'ru_RU' }
  ],
  segments: [{ type: 'context', lines: [{ line: 3, text: '<TS version="2.1" language="es_ES">' }] }]
};
const remoteRows = [
  { line: 85, text: '<message>' },
  { line: 86, text: '<location filename="curve_base_widget.cpp" line="105"/>' },
  { line: 87, text: '<source>曲线标题</source>' },
  { line: 88, text: '<source>光字牌</source>' },
  { line: 89, text: '<translation>Curve display title</translation>' },
  { line: 90, text: '<translation>Indicator lamp</translation>' },
  { line: 91, text: '</message>' }
];
const conflictBlock = {
  id: 'lang-0-0',
  blockIndex: 0,
  view: {
    incoming: remoteRows,
    current: remoteRows.map((row) => ({ ...row }))
  }
};
const getConflictBlocks = () => [conflictBlock];
const getEntryFileMappingDiagnostics = loadFunction('getEntryFileMappingDiagnostics')(errorFile);

assert.equal(getEntryFileMappingDiagnostics.length, 2, 'TS mismatch and duplicate children are two mapping diagnostics');
const duplicateDiagnostic = getEntryFileMappingDiagnostics.find((diagnostic) => diagnostic.type === 'duplicate-message-children');
assert.deepEqual(duplicateDiagnostic.elements, ['source', 'translation']);
assert.match(duplicateDiagnostic.message, /单个 <message> 中存在重复子元素/);
assert.equal(getEntryFileMappingDiagnostics.filter((diagnostic) => diagnostic.type === 'duplicate-message-children').length, 1);

const physicalFileEntries = loadFunction('getMappingErrorFileEntries', {
  files: [errorFile],
  getEntryFileMappingDiagnostics: () => getEntryFileMappingDiagnostics
})();
assert.equal(physicalFileEntries.length, 1, 'diagnostics for one physical file share one mapping-error item');
assert.equal(physicalFileEntries[0].path, 'en_US/ts/curve_base_widget_en_US.ts');
assert.equal(physicalFileEntries[0].diagnostics.length, 2);
const anotherPhysicalPath = 'es_ES/ts/curve_base_widget_es_ES.ts';
const multiplePhysicalEntries = loadFunction('getMappingErrorFileEntries', {
  files: [errorFile],
  getEntryFileMappingDiagnostics: () => [...getEntryFileMappingDiagnostics, { ...getEntryFileMappingDiagnostics[0], path: anotherPhysicalPath }]
})();
assert.deepEqual(multiplePhysicalEntries.map((entry) => entry.path), [
  'en_US/ts/curve_base_widget_en_US.ts',
  anotherPhysicalPath
], 'different physical error files remain separate list items');

const physicalFileLabel = loadFunction('renderLogicalFileName', { escapeHtml })(
  errorFile,
  'en_US/ts/curve_base_widget_en_US.ts'
);
assert.match(physicalFileLabel, /curve_base_widget_en_US\.ts/);
assert.match(physicalFileLabel, /data-full-path="• en_US\/ts\/curve_base_widget_en_US\.ts"/);
assert.doesNotMatch(physicalFileLabel, /es_ES|ru_RU|curve_base_widget\.ts/);
const logicalFileLabel = loadFunction('renderLogicalFileName', { escapeHtml })(errorFile);
assert.match(logicalFileLabel, /curve_base_widget\.ts/);
assert.match(logicalFileLabel, /en_US\/ts\/curve_base_widget_en_US\.ts/);
assert.match(logicalFileLabel, /es_ES\/ts\/curve_base_widget_es_ES\.ts/);
assert.match(logicalFileLabel, /ru_RU\/ts\/curve_base_widget_ru_RU\.ts/);
const conflictRenderer = extractFunction('renderConflictFile');
assert.match(conflictRenderer, /const displayPath=mappingErrorMode\?currentMappingErrorPath:file\.path/);
assert.match(conflictRenderer, /label\.dataset\.fullPath=fullPath/);
assert.match(conflictRenderer, /label\.dataset\.pathList='true'/);

const detailHtml = loadFunction('renderMappingErrorDetails', {
  ensureConflictFileLoaded: () => {},
  getConflictBlocks,
  escapeHtml
})(errorFile, getEntryFileMappingDiagnostics);
assert.match(detailHtml, /映射错误 · 2 项/);
assert.equal((detailHtml.match(/单个 &lt;message&gt; 中存在重复子元素/g) || []).length, 1);
assert.match(detailHtml, /message 远端 L85–L91/);
assert.match(detailHtml, /&lt;source&gt;<\/code> L87、L88/);
assert.match(detailHtml, /&lt;translation&gt;<\/code> L89、L90/);
assert.match(detailHtml, /第 3 行/);
assert.match(detailHtml, /远端 L3 整行标红/);
assert.doesNotMatch(detailHtml, /已高亮该属性/);

const virtualIncoming = loadFunction('buildVirtualPaneItems', {
  getEntryFileMappingDiagnostics: () => getEntryFileMappingDiagnostics,
  getConflictBlocks,
  deriveConflictBlock: () => ({ resolved: false, label: '未处理', lines: [] }),
  currentMappingErrorPath: 'en_US/ts/curve_base_widget_en_US.ts'
})(errorFile, 'incoming');
assert.deepEqual(virtualIncoming.contextRows.map((row) => row.line), [3]);
assert.equal(virtualIncoming.contextRows[0].mappingError, true, 'the language mismatch row is marked as a mapping error');
assert.deepEqual(virtualIncoming.contextRows[0].highlights, [], 'language mismatch marks the whole line, not the attribute');
const highlightedRows = virtualIncoming.items[0].rows.filter((row) => row.mappingError).map((row) => row.line);
assert.deepEqual(highlightedRows, [85, 86, 87, 88, 89, 90, 91], 'the full malformed message block is marked as a mapping error');
assert.ok(virtualIncoming.items[0].rows.every((row) => row.highlights.length === 0), 'mapping-error lines do not carry yellow difference highlights');

const renderCodeLine = loadFunction('renderCodeLine', {
  escapeHtml,
  renderMarkedCodeText: (text) => escapeHtml(text)
});
const languageErrorLineHtml = renderCodeLine(virtualIncoming.contextRows[0], '', 'language-error');

const dom = new JSDOM(html, { pretendToBeVisual: true });
const { document } = dom.window;
const sidebar = document.getElementById('mergeSidebar');
sidebar.classList.add('mapping-error-blocked');
assert.notEqual(dom.window.getComputedStyle(document.getElementById('mappingErrorSection')).display, 'none');
for (const selector of ['#conflictFilesSection', '#changedFilesSection', '#repairStrategiesSection', '.sidebar-section-resizer']) {
  for (const element of document.querySelectorAll(`#mergeSidebar ${selector}`)) {
    assert.equal(dom.window.getComputedStyle(element).display, 'none', `${selector} is hidden while mapping errors block the batch`);
  }
}
assert.notEqual(dom.window.getComputedStyle(document.querySelector('[data-destructive="abort-merge"]')).display, 'none', 'abort remains available');
assert.equal(dom.window.getComputedStyle(document.querySelector('[data-action="sync-conflict"]')).display, 'none', 'sync is hidden while mapping errors block the batch');
assert.equal(dom.window.getComputedStyle(document.querySelector('#commitSection .commit-box')).display, 'none', 'commit details are hidden in mapping-error mode');

sidebar.classList.remove('mapping-error-blocked');
assert.notEqual(dom.window.getComputedStyle(document.getElementById('conflictFilesSection')).display, 'none', 'ordinary conflict files remain visible without mapping errors');
assert.notEqual(dom.window.getComputedStyle(document.getElementById('changedFilesSection')).display, 'none', 'changed files remain visible without mapping errors');

const codeBlock = document.createElement('section');
codeBlock.className = 'merge-conflict-block mapping-error-block selected';
codeBlock.innerHTML = virtualIncoming.items[0].rows.map((row) => renderCodeLine(row, '', `message-error-${row.line}`)).join('');
document.body.append(codeBlock);
const languageErrorLine = document.createElement('div');
languageErrorLine.innerHTML = languageErrorLineHtml;
const languageErrorElement = languageErrorLine.firstElementChild;
document.body.append(languageErrorElement);
const languageMarkerStyle = dom.window.getComputedStyle(languageErrorElement);
const blockMarkerStyle = dom.window.getComputedStyle(codeBlock);
assert.match(languageMarkerStyle.boxShadow, /inset 2px 0/);
assert.match(languageMarkerStyle.boxShadow, /#bd3632/i);
assert.equal(blockMarkerStyle.marginLeft, '0px', 'the block marker aligns with the standalone error-line marker');
assert.equal(blockMarkerStyle.borderLeftWidth, '2px', 'the message block uses the same two-pixel left marker');
assert.equal(blockMarkerStyle.borderLeftColor, 'rgb(189, 54, 50)');
assert.equal(blockMarkerStyle.borderTopWidth, '1px');
assert.equal(blockMarkerStyle.borderRightWidth, '1px');
assert.equal(blockMarkerStyle.borderBottomWidth, '1px');
assert.equal(dom.window.getComputedStyle(codeBlock).borderTopColor, 'rgb(189, 54, 50)', 'mapping-error conflict block uses a red border even when selected');
assert.equal(dom.window.getComputedStyle(codeBlock).backgroundColor, 'rgb(255, 230, 227)', 'mapping-error conflict block uses the shared red background');
for (const errorLine of codeBlock.querySelectorAll('.code-line')) {
  assert.equal(dom.window.getComputedStyle(errorLine).backgroundColor, 'rgb(255, 230, 227)', `remote line ${errorLine.dataset.line} uses the shared red background`);
  assert.equal(dom.window.getComputedStyle(errorLine).color, 'rgb(134, 45, 41)', `remote line ${errorLine.dataset.line} uses the shared red foreground`);
  assert.equal(dom.window.getComputedStyle(errorLine).boxShadow, 'none', `remote line ${errorLine.dataset.line} does not duplicate the block's left marker`);
}
assert.equal(languageMarkerStyle.backgroundColor, 'rgb(255, 230, 227)', 'the TS language-error row uses the same red background as the malformed message');
assert.equal(languageMarkerStyle.color, 'rgb(134, 45, 41)', 'the TS language-error row uses the same red foreground as the malformed message');
assert.equal((virtualIncoming.items[0].rows[4].highlights || []).length, 0, 'translation lines have no nested yellow difference ranges');

const ordinaryBlock = document.createElement('section');
ordinaryBlock.className = 'merge-conflict-block';
ordinaryBlock.innerHTML = '<span class="code-line conflict">ordinary conflict</span>';
document.body.append(ordinaryBlock);
assert.equal(dom.window.getComputedStyle(ordinaryBlock).borderTopColor, 'rgb(227, 191, 92)', 'ordinary conflict borders keep their existing color');
assert.equal(dom.window.getComputedStyle(ordinaryBlock.firstElementChild).backgroundColor, 'rgb(255, 241, 195)', 'ordinary conflict rows keep their existing yellow difference style');

const detailPanel = document.createElement('section');
detailPanel.className = 'mapping-error-detail';
detailPanel.innerHTML = detailHtml;
document.body.append(detailPanel);
assert.equal(dom.window.getComputedStyle(detailPanel.querySelector('strong')).color, 'rgb(152, 47, 44)', 'both mapping diagnostic labels share the red error color');

/** 验证映射错误批次只能切换错误文件，普通批次仍能选择普通冲突文件。
 * @param {boolean} batchBlocked 批次是否存在映射错误。
 * @returns {Object<string, any>} 文件选择后的状态及调用计数。
 */
function verifyFileSelection(batchBlocked) {
  const normalFile = { kind: 'dic', path: 'dic/curve.dic', fileIndex: 1 };
  const files = [errorFile, normalFile];
  const calls = { history: 0, render: 0 };
  const scope = {
    bulkAcceptInProgress: false,
    files,
    currentFileIndex: 0,
    currentMappingErrorPath: '',
    currentConflictId: 'lang-0-0',
    selectedEntryRows: { clear: () => {} },
    hasEntryFileMappingErrors: () => batchBlocked,
    getEntryFileMappingDiagnostics: (file) => file === errorFile ? getEntryFileMappingDiagnostics : [],
    beginHistoryChange: () => { calls.history += 1; return {}; },
    releaseConflictFile: () => {},
    getFirstPendingConflictId: () => 'lang-1-0',
    renderConflictFile: () => { calls.render += 1; },
    conflictSearch: { owner: '', matches: [] },
    navigateConflictSearchMatch: () => {},
    scrollConflictBlockIntoView: () => {},
    renderRepairSelection: () => {},
    commitHistoryChange: () => {}
  };
  const selectFile = loadFunction('selectConflictFile', scope);
  selectFile(1);
  return { currentFileIndex: selectFile.scope.currentFileIndex, calls };
}

const blockedSelection = verifyFileSelection(true);
assert.equal(blockedSelection.currentFileIndex, 0, 'ordinary file cannot be opened while the batch has mapping errors');
assert.deepEqual(blockedSelection.calls, { history: 0, render: 0 }, 'blocked file selection has no merge side effects');
const ordinarySelection = verifyFileSelection(false);
assert.equal(ordinarySelection.currentFileIndex, 1, 'ordinary conflict file remains selectable without mapping errors');
assert.deepEqual(ordinarySelection.calls, { history: 1, render: 1 });

const pathSelectionScope = {
  bulkAcceptInProgress: false,
  files: [errorFile],
  currentFileIndex: 0,
  currentMappingErrorPath: 'en_US/ts/curve_base_widget_en_US.ts',
  currentConflictId: 'lang-0-0',
  selectedEntryRows: { clear: () => {} },
  hasEntryFileMappingErrors: () => true,
  getEntryFileMappingDiagnostics: () => [...getEntryFileMappingDiagnostics, { ...getEntryFileMappingDiagnostics[0], path: anotherPhysicalPath }],
  beginHistoryChange: () => ({}),
  releaseConflictFile: () => {},
  getFirstPendingConflictId: () => 'lang-0-0',
  renderConflictFile: () => {},
  conflictSearch: { owner: '', matches: [] },
  navigateConflictSearchMatch: () => {},
  scrollConflictBlockIntoView: () => {},
  renderRepairSelection: () => {},
  commitHistoryChange: () => {}
};
const selectPhysicalFile = loadFunction('selectConflictFile', pathSelectionScope);
selectPhysicalFile(0, anotherPhysicalPath);
assert.equal(selectPhysicalFile.scope.currentMappingErrorPath, anotherPhysicalPath, 'selecting an error row switches to its physical file within the logical group');

assert.match(extractFunction('completeCurrentMergeFile'), /getEntryFileValidationErrors\(file\)\.length/);
assert.match(extractFunction('continueConflictSync'), /hasEntryFileMappingErrors\(\)/);
assert.match(extractFunction('applyWholeMergeSide'), /hasEntryFileMappingErrors\(\)/);

console.log('sync management mapping-error checks passed');
