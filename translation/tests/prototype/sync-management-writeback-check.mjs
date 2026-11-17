import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const htmlPath = new URL('../../prototype/syncManagement/sync-management-v2.html', import.meta.url);
const html = readFileSync(fileURLToPath(htmlPath), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)?.[1];
assert.ok(script, 'prototype inline script exists');

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
  return new Function('scope', `with (scope) { return (${extractFunction(name)}); }`)(scope);
}

/** 转义测试渲染使用的 HTML 文本。
 * @param {string} value 待转义文本。
 * @returns {string} HTML 安全文本。
 */
function escapeHtml(value) {
  return String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

/** 创建渲染预览所需的最小 DOM 替身。
 * @returns {Object<string, any>} 页面节点及查询接口。
 */
function createPreviewHarness() {
  const nodes = {
    pullPreviewSubtitle: { textContent: '' },
    pullPreviewState: { textContent: '' },
    pullPreviewTarget: { textContent: '' },
    pullPreviewSummary: { textContent: '', hidden: true },
    pullPreviewLock: { textContent: '' },
    pullPreviewList: { innerHTML: '', querySelectorAll: () => [] }
  };
  const confirmButton = { dataset: {} };
  const abortButton = { dataset: { requires: 'terminator' } };
  return {
    nodes,
    confirmButton,
    abortButton,
    document: {
      getElementById: (id) => nodes[id],
      querySelector: (selector) => selector.includes('confirm-pull-writeback') ? confirmButton : abortButton
    }
  };
}

const ordinaryTask = { id: 'sync-1066', bindingId: 'bind-admin', hasConflicts: false, previewSummary: '128 条词条有更新' };
const ordinaryRow = { dataset: { productPath: '思源宏瑞 / 通用平台部 / 云平台' }, querySelector: () => ({ textContent: '云平台' }) };
const ordinaryHarness = createPreviewHarness();
loadFunction('renderPullPreview', {
  ...ordinaryHarness,
  activePreviewTaskId: '',
  files: [{ path: 'src/i18n/en_US.ts', changedEntries: 12, changeLabel: '条译文有更新', blockCount: 2 }],
  escapeHtml,
  getConflictBatchSummary: () => '1 个逻辑文件 · 1 个实际文件 · 12 条译文',
  getConflictBatchStats: () => ({ blockCount: 2 }),
  buildFileResultPreview: () => '真实合并结果',
  handlePullPreviewFileToggle: () => {},
  syncRoleControls: () => {}
})(ordinaryTask, ordinaryRow);
assert.equal(ordinaryHarness.nodes.pullPreviewState.textContent, '合并已完成，尚未写入');
assert.equal(ordinaryHarness.nodes.pullPreviewTarget.textContent, ordinaryRow.dataset.productPath);
assert.equal(ordinaryHarness.nodes.pullPreviewList.innerHTML, '', '普通拉取不生成虚假文件明细');
assert.equal(ordinaryHarness.nodes.pullPreviewSummary.textContent, '128 条词条有更新');
assert.equal(ordinaryHarness.confirmButton.dataset.requires, 'write');
assert.equal(ordinaryHarness.abortButton.dataset.requires, 'terminator');

const conflictTask = { id: 'sync-1067', bindingId: 'bind-mobile', hasConflicts: true };
const conflictHarness = createPreviewHarness();
loadFunction('renderPullPreview', {
  ...conflictHarness,
  activePreviewTaskId: '',
  files: [{ path: 'src/i18n/en_US.ts', changedEntries: 12, changeLabel: '条译文有更新', blockCount: 2 }],
  escapeHtml,
  getConflictBatchSummary: () => '1 个逻辑文件 · 1 个实际文件 · 12 条译文',
  getConflictBatchStats: () => ({ blockCount: 2 }),
  buildFileResultPreview: () => '真实合并结果',
  handlePullPreviewFileToggle: () => {},
  syncRoleControls: () => {}
})(conflictTask, { dataset: { productPath: '思源宏瑞 / 通用平台部 / mon-cn-1.12.0' }, querySelector: () => ({ textContent: 'mon-cn-1.12.0' }) });
assert.match(conflictHarness.nodes.pullPreviewList.innerHTML, /src\/i18n\/en_US\.ts/);
assert.match(conflictHarness.nodes.pullPreviewList.innerHTML, /data-preview-content hidden/);
assert.match(conflictHarness.nodes.pullPreviewSummary.textContent, /2 个冲突块已处理/);
assert.equal(conflictHarness.confirmButton.dataset.requires, 'write,conflict');

const renderQueue = extractFunction('renderQueue');
assert.match(renderQueue, /data-action="open-pull-preview"[^>]*>确认写库/);
assert.match(renderQueue, /待确认写库/);
assert.match(renderQueue, /waitingWriteback/);
assert.doesNotMatch(renderQueue, /data-action="confirm-pull-writeback"/);

const returnFromPreview = extractFunction('returnFromPullPreview');
assert.match(returnFromPreview, /task\.state='waiting'/);
assert.match(returnFromPreview, /task\.previewPending=true/);
assert.doesNotMatch(returnFromPreview, /appendTerminal|finishPipeline|confirmPullWriteback/);

let closeCalls = 0;
const closableTask = { id: 'sync-1066', bindingId: 'bind-admin', state: 'waiting', previewPending: true, hasConflicts: false };
const closableRow = { dataset: { state: 'running', error: '' } };
loadFunction('returnFromPullPreview', {
  tasks: [closableTask],
  document: {
    getElementById: () => ({ close: () => { closeCalls += 1; } }),
    querySelector: () => closableRow
  },
  beginHistoryChange: () => ({}),
  clearPullPreviewContent: () => {},
  renderBindingRow: () => {},
  renderQueue: () => {},
  renderConflictFile: () => { throw new Error('ordinary pull must return to the queue'); },
  commitHistoryChange: () => {},
  activePreviewTaskId: closableTask.id
})(closableTask.id);
assert.equal(closeCalls, 1);
assert.equal(closableTask.state, 'waiting');
assert.equal(closableTask.previewPending, true);
assert.equal(closableTask.stage, '待确认写库');

const writeTask = { id: 'sync-1066', bindingId: 'bind-admin', state: 'waiting', previewPending: true, hasConflicts: false };
const writeRow = { dataset: { productPath: ordinaryRow.dataset.productPath } };
const terminalLines = [];
let finishedTask = null;
loadFunction('confirmPullWriteback', {
  tasks: [writeTask],
  document: {
    getElementById: (id) => id === 'roleSelect' ? { value: 'writer' } : { close: () => {} },
    querySelector: () => writeRow
  },
  hasPermission: () => true,
  getConflictCounts: () => ({ unresolved: 0 }),
  beginHistoryChange: () => ({}),
  appendTerminal: (line) => terminalLines.push(line),
  clearPullPreviewContent: () => {},
  finishPipeline: (task) => { finishedTask = task; },
  showToast: () => {},
  commitHistoryChange: () => {},
  activePreviewTaskId: writeTask.id
})(writeTask.id);
assert.equal(finishedTask, writeTask, 'only final confirmation completes the writeback');
assert.deepEqual(terminalLines, [`Mock 分类写入已确认：${ordinaryRow.dataset.productPath}`]);

assert.match(html, /id="pullPreviewTitle">确认写库<\/div>/);
assert.match(html, /<button class="action-icon stop tip"[^>]*data-requires="terminator"[^>]*aria-label="请求安全终止流水线"/);
assert.match(html, /data-action="return-pull-preview"[^>]*>关闭<\/button>/);
assert.equal((html.match(/data-action="return-pull-preview"/g) || []).length, 2, 'both close controls share the no-write action');
assert.match(html, /\.queue-foot\s*\{[^}]*justify-content:flex-end;/, 'queue footer aligns its action group to the right');
assert.match(html, /\.queue-foot > \.muted\s*\{[^}]*margin-right:auto;/, 'queue progress label stays on the left');
assert.match(extractFunction('confirmPullWriteback'), /appendTerminal\(`Mock 分类写入已确认：/);
assert.match(extractFunction('confirmPullWriteback'), /finishPipeline\(task,row\)/);

console.log('sync management writeback checks passed');

