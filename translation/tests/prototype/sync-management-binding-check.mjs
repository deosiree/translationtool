import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { JSDOM } from 'jsdom';

const htmlPath = new URL('../../prototype/syncManagement/sync-management-v2.html', import.meta.url);
const html = readFileSync(fileURLToPath(htmlPath), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)?.[1];
assert.ok(script, 'prototype inline script exists');
new Function(script);

/** 从原型内联脚本提取顶层函数源码。
 * @param {string} name 函数名称。
 * @returns {string} 完整函数源码。
 */
function extractFunction(name) {
  const expression = new RegExp(`[ \\t]{0,4}function ${name}\\([^\\n]*\\) \\{[\\s\\S]*?\\n {4}\\}`);
  const match = script.match(expression);
  assert.ok(match, `${name} exists in the prototype`);
  return match[0];
}

/** 从原型内联脚本提取可执行的常量初始化值。
 * @param {string} name 常量名称。
 * @returns {any} 初始化后的常量值。
 */
function extractInitializer(name) {
  const prefix = `const ${name} = `;
  const start = script.indexOf(prefix);
  assert.notEqual(start, -1, `${name} exists in the prototype`);
  const valueStart = start + prefix.length;
  const end = script.indexOf(';', valueStart);
  assert.notEqual(end, -1, `${name} initializer ends with a semicolon`);
  return new Function(`return (${script.slice(valueStart, end)});`)();
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

/** 转义表单和 Mock 行展示中使用的 HTML 文本。
 * @param {string} value 待转义文本。
 * @returns {string} 转义后的文本。
 */
function escapeHtml(value) {
  return String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

const mockUsers = extractInitializer('mockUserDirectory');
const categoryFixture = extractInitializer('mockProductCategoryTree');

/** 创建绑定表单和 Mock 状态所需的隔离 DOM 环境。
 * @returns {Object<string, any>} DOM、分类状态、绑定创建器与流水线状态。
 */
function createHarness() {
  const dom = new JSDOM(html, { pretendToBeVisual: true });
  const { document } = dom.window;
  const users = structuredClone(mockUsers);
  const categories = structuredClone(categoryFixture);
  const tasks = [];
  const toasts = [];
  const scheduledTasks = [];
  const bindingProfiles = {};
  const bindingPermissions = {};
  const operationHistory = {};
  const shared = { document, mockUserDirectory: users, mockProductCategoryTree: categories };
  const getVisibleCategoryNodes = loadFunction('getVisibleCategoryNodes', shared);
  const canSelectBindingCategory = loadFunction('canSelectBindingCategory');
  const buildBindingCategoryTree = loadFunction('buildBindingCategoryTree');
  const renderBindingCategoryTree = loadFunction('renderBindingCategoryTree', {
    buildBindingCategoryTree,
    canSelectBindingCategory,
    escapeHtml
  });
  const setBindingCategoryPickerOpen = loadFunction('setBindingCategoryPickerOpen', { document });
  const syncBindingCategoryControls = loadFunction('syncBindingCategoryControls', {
    ...shared,
    getVisibleCategoryNodes,
    canSelectBindingCategory,
    renderBindingCategoryTree,
    setBindingCategoryPickerOpen
  });
  const selectBindingCategory = loadFunction('selectBindingCategory', { document, syncBindingCategoryControls });
  const toggleBindingCategoryPicker = loadFunction('toggleBindingCategoryPicker', { document, setBindingCategoryPickerOpen });
  const toggleBindingCategoryBranch = loadFunction('toggleBindingCategoryBranch');
  const insertMockProductCategory = loadFunction('insertMockProductCategory', shared);
  const parseBranchAbsoluteUrl = loadFunction('parseBranchAbsoluteUrl');
  const renderBindingRow = () => {};
  const renderQueue = () => {};
  const startPipeline = loadFunction('startPipeline', {
    ...shared,
    tasks,
    taskSequence: 3000,
    hasPermission: (userId, permission) => permission === 'write' || permission === 'create',
    isBranchLocked: (row) => row.dataset.activeLock === 'true',
    renderBindingRow,
    renderQueue,
    scheduleTaskProgress: (taskId) => scheduledTasks.push(taskId),
    showToast: (message) => toasts.push(message)
  });
  const createBinding = loadFunction('createBinding', {
    ...shared,
    hasPermission: (userId, permission) => permission === 'write' || permission === 'create',
    getVisibleCategoryNodes,
    canSelectBindingCategory,
    insertMockProductCategory,
    parseBranchAbsoluteUrl,
    isBranchLocked: (row) => row.dataset.activeLock === 'true',
    bindingProfiles,
    bindingPermissions,
    operationHistory,
    startPipeline,
    renderBindingRow,
    initializeBindingRowFocus: () => {},
    renderQueue,
    closeDialogs: () => {},
    syncBindingCategoryControls,
    showToast: (message) => toasts.push(message)
  });
  document.getElementById('roleSelect').innerHTML = users.map((user) => `<option value="${user.id}">${user.userName}</option>`).join('');
  document.getElementById('roleSelect').value = 'u-admin';
  document.getElementById('permissionDepartment').innerHTML = '<option value="all">所有部门</option>';
  document.getElementById('bindingForm').querySelector('#newBranchUrl').value = 'https://git.example.com/group/lang/-/tree/feature-demo';
  syncBindingCategoryControls();
  return {
    dom,
    document,
    users,
    categories,
    tasks,
    toasts,
    scheduledTasks,
    bindingProfiles,
    bindingPermissions,
    operationHistory,
    getVisibleCategoryNodes,
    canSelectBindingCategory,
    renderBindingCategoryTree,
    syncBindingCategoryControls,
    selectBindingCategory,
    toggleBindingCategoryPicker,
    toggleBindingCategoryBranch,
    startPipeline,
    createBinding
  };
}

/** 按给定值准备并提交新建绑定表单。
 * @param {Object<string, any>} harness 隔离页面状态。
 * @param {Object<string, any>} values 表单控件值。
 * @returns {void} 执行一次原型提交处理。
 */
function submitBinding(harness, values = {}) {
  const { document } = harness;
  document.getElementById('newRepo').value = values.repo || 'git@10.17.196.23:platform/lang.git';
  document.getElementById('newBranch').value = values.branch || 'feature-binding-demo';
  document.getElementById('newBranchUrl').value = Object.hasOwn(values, 'branchUrl') ? values.branchUrl : 'https://git.example.com/group/lang/-/tree/feature-binding-demo';
  document.getElementById('newCategoryMode').checked = Boolean(values.createCategory);
  harness.syncBindingCategoryControls();
  document.getElementById('newTarget').value = values.target || '思源宏瑞 / 通用平台部 / 通用基础词条';
  harness.syncBindingCategoryControls();
  document.getElementById('newCategorySuffix').value = values.suffix || '';
  harness.createBinding({ preventDefault() {} });
}

const uiHarness = createHarness();
const roleSelect = uiHarness.document.getElementById('roleSelect');
const handleDocumentChange = loadFunction('handleDocumentChange', {
  document: uiHarness.document,
  syncRoleControls: () => {},
  syncBindingCategoryControls: uiHarness.syncBindingCategoryControls,
  renderQueue: () => {},
  renderBindingRow: () => {},
  activeDetailRow: null,
  hasPermission: () => true,
  closeDetailDrawer: () => {},
  renderBindingPermissions: () => {},
  currentBindingId: '',
  getDestructivePermissions: () => [],
  destructiveKind: '',
  dismissRepairConfirmation: () => {},
  tasks: [],
  activePreviewTaskId: '',
  clearPullPreviewContent: () => {}
});
const adminOptions = [...uiHarness.document.getElementById('newTarget').options].map((option) => option.value);
const adminCategorySelect = uiHarness.document.getElementById('newTarget');
const existingBindingPaths = [...uiHarness.document.querySelectorAll('#bindingTable tbody tr')].map((row) => row.dataset.productPath);
assert.ok(adminOptions.includes('思源宏瑞 / 企业应用部 / 企业应用'));
assert.ok(adminOptions.includes('公共分类 / 公共库'));
for (const path of existingBindingPaths) {
  const option = [...adminCategorySelect.options].find((item) => item.value === path);
  assert.ok(option, `bound category ${path} exists in the menu tree`);
  assert.equal(option.dataset.categoryBound, 'true');
  assert.equal(option.disabled, true, `bound category ${path} is disabled in ordinary mode`);
}
roleSelect.value = 'u-cloud';
handleDocumentChange({ target: roleSelect });
const categorySelect = uiHarness.document.getElementById('newTarget');
const cloudOptions = [...categorySelect.options];
assert.ok(cloudOptions.some((option) => option.value === '思源宏瑞 / 云平台部 / 云端升级'));
assert.ok(cloudOptions.some((option) => option.value === '公共分类 / 公共库'));
assert.ok(!cloudOptions.some((option) => option.value.includes('通用平台部')));
assert.ok(cloudOptions.filter((option) => option.value).every((option) => option.disabled === (option.dataset.categoryLeaf !== 'true' || option.dataset.categoryBound === 'true')));
roleSelect.value = 'u-review';
handleDocumentChange({ target: roleSelect });
const reviewOptions = [...categorySelect.options].map((option) => option.value);
assert.ok(reviewOptions.includes('思源宏瑞 / 词条管理部 / 术语共享'));
assert.ok(reviewOptions.includes('公共分类 / 公共库'));
assert.ok(!reviewOptions.some((path) => path.includes('云平台部')));
roleSelect.value = 'u-cloud';
handleDocumentChange({ target: roleSelect });

roleSelect.value = 'u-admin';
handleDocumentChange({ target: roleSelect });
const newCategoryMode = uiHarness.document.getElementById('newCategoryMode');
const suffixInput = uiHarness.document.getElementById('newCategorySuffix');
const boundCategoryPath = '思源宏瑞 / 通用平台部 / 云平台';
let boundCategoryOption = [...categorySelect.options].find((option) => option.value === boundCategoryPath);
assert.ok(boundCategoryOption, 'existing binding category is present for the administrator');
assert.equal(boundCategoryOption.dataset.categoryBound, 'true');
assert.equal(boundCategoryOption.disabled, true, 'bound leaf is disabled in ordinary mode');
newCategoryMode.checked = true;
handleDocumentChange({ target: newCategoryMode });
assert.equal(uiHarness.document.getElementById('newCategoryFields').hidden, false);
assert.equal(suffixInput.required, true);
boundCategoryOption = [...categorySelect.options].find((option) => option.value === boundCategoryPath);
assert.equal(boundCategoryOption.disabled, true, 'bound leaf stays disabled in create mode');
for (const path of existingBindingPaths) {
  const option = [...categorySelect.options].find((item) => item.value === path);
  assert.equal(option.disabled, true, `bound category ${path} stays disabled in create mode`);
}
assert.ok(!categorySelect.querySelector('option[value="思源宏瑞 / 云平台部"]').disabled, 'create mode allows a visible parent node');
categorySelect.value = '思源宏瑞 / 云平台部';
suffixInput.value = '临时分类';
newCategoryMode.checked = false;
handleDocumentChange({ target: newCategoryMode });
assert.equal(uiHarness.document.getElementById('newCategoryFields').hidden, true);
assert.equal(suffixInput.required, false);
assert.equal(suffixInput.value, '', 'leaving create mode clears the suffix');
assert.equal(categorySelect.value, '', 'a parent selection is cleared when returning to leaf-only mode');

const ordinaryHarness = createHarness();
submitBinding(ordinaryHarness, { target: '思源宏瑞 / 通用平台部 / 通用基础词条' });
const ordinaryRow = ordinaryHarness.document.querySelector('#bindingTable tbody tr:last-child');
assert.equal(ordinaryRow.dataset.productPath, '思源宏瑞 / 通用平台部 / 通用基础词条');
assert.equal(ordinaryRow.dataset.branchUrl, 'https://git.example.com/group/lang/-/tree/feature-binding-demo');
assert.equal(ordinaryRow.querySelector('.branch-link').href, 'https://git.example.com/group/lang/-/tree/feature-binding-demo');
assert.equal(ordinaryRow.dataset.initialPullPending, 'true');
assert.equal(ordinaryHarness.tasks.length, 1);
assert.equal(ordinaryHarness.tasks[0].operation, 'pull');
assert.equal(ordinaryHarness.tasks[0].initialPull, true);
assert.deepEqual(ordinaryHarness.scheduledTasks, [ordinaryHarness.tasks[0].id]);

const categoryHarness = createHarness();
submitBinding(categoryHarness, {
  target: '思源宏瑞 / 云平台部',
  createCategory: true,
  suffix: '新分支分类',
  branch: 'feature-cloud-category'
});
const categoryRow = categoryHarness.document.querySelector('#bindingTable tbody tr:last-child');
assert.equal(categoryRow.dataset.productPath, '思源宏瑞 / 云平台部 / 新分支分类');
assert.ok(categoryHarness.getVisibleCategoryNodes('u-cloud').some((node) => node.path === categoryRow.dataset.productPath));
assert.ok(!categoryHarness.getVisibleCategoryNodes('u-review').some((node) => node.path === categoryRow.dataset.productPath));
assert.equal(categoryHarness.tasks.length, 1, 'new-category mode also starts the first pull');

/** 验证无效输入只提示错误，不新增绑定、分类或流水线。
 * @param {Object<string, any>} values 待验证表单值。
 * @param {RegExp} messagePattern 预期错误提示。
 * @returns {void} 断言拒绝保存且无副作用。
 */
function assertRejectedBinding(values, messagePattern) {
  const harness = createHarness();
  const rowCount = harness.document.querySelectorAll('#bindingTable tbody tr').length;
  const categoryCount = harness.getVisibleCategoryNodes('u-admin').length;
  submitBinding(harness, values);
  assert.equal(harness.document.querySelectorAll('#bindingTable tbody tr').length, rowCount);
  assert.equal(harness.getVisibleCategoryNodes('u-admin').length, categoryCount);
  assert.equal(harness.tasks.length, 0);
  assert.ok(harness.toasts.some((message) => messagePattern.test(message)), `expected toast ${messagePattern}`);
}

assertRejectedBinding({ branchUrl: '' }, /HTTP\(S\) URL/);
assertRejectedBinding({ branchUrl: 'ftp://git.example.com/group/lang/tree/main' }, /HTTP\(S\) URL/);
assertRejectedBinding({ target: '思源宏瑞 / 通用平台部', branch: 'feature-parent-rejected' }, /叶子分类/);
assertRejectedBinding({ target: '思源宏瑞 / 通用平台部 / 云平台' }, /已有绑定关系/);
assertRejectedBinding({ target: '思源宏瑞 / 通用平台部 / 云平台', createCategory: true, suffix: '重复分支分类' }, /已有绑定关系/);
assertRejectedBinding({ target: '思源宏瑞 / 云平台部', createCategory: true, suffix: '' }, /不能为空/);
assertRejectedBinding({ target: '思源宏瑞 / 通用平台部', createCategory: true, suffix: 'folder/child' }, /路径片段/);
assertRejectedBinding({ target: '思源宏瑞 / 通用平台部', createCategory: true, suffix: 'develop' }, /完整分类路径已存在/);
assertRejectedBinding({ target: '思源宏瑞 / 通用平台部 / 通用基础词条', branch: 'develop' }, /活动流水线/);

const failureHarness = createHarness();
submitBinding(failureHarness, {
  target: '思源宏瑞 / 通用平台部 / 通用基础词条',
  branch: 'feature-retry-demo',
  branchUrl: 'https://git.invalid/group/lang/-/tree/feature-retry-demo'
});
const failureRow = failureHarness.document.querySelector('#bindingTable tbody tr:last-child');
const failureTask = failureHarness.tasks[0];
const recordedOperations = [];
const previewDialog = failureHarness.document.getElementById('pullPreviewDialog');
previewDialog.showModal = () => previewDialog.setAttribute('open', '');
previewDialog.close = () => previewDialog.removeAttribute('open');
const sharedTaskDependencies = {
  document: failureHarness.document,
  tasks: failureHarness.tasks,
  beginHistoryChange: () => ({}),
  commitHistoryChange: () => {},
  recordBindingOperation: (bindingId, operation, result) => recordedOperations.push({ bindingId, operation, result }),
  renderBindingRow: () => {},
  renderQueue: () => {},
  refreshOpenBindingHistory: () => {},
  showToast: (message) => failureHarness.toasts.push(message),
  hasPermission: () => true,
  activePreviewTaskId: failureTask.id,
  getConflictCounts: () => ({ unresolved: 0 }),
  appendTerminal: (line) => recordedOperations.push({ line }),
  clearPullPreviewContent: () => {},
  currentBindingId: ''
};
const advanceTask = loadFunction('advanceTask', {
  ...sharedTaskDependencies,
  renderPullPreview: () => {}
});
advanceTask(failureTask.id);
assert.equal(failureHarness.tasks.length, 0);
assert.equal(failureRow.dataset.state, 'download');
assert.equal(failureRow.dataset.task, '');
assert.equal(failureRow.dataset.initialPullPending, 'true');
assert.equal(failureRow.dataset.initialPullFailed, 'true');
assert.match(failureRow.dataset.error, /可重试/);
assert.ok(recordedOperations.some((record) => record.result === '失败（远端不可达）'));
assert.ok(failureHarness.getVisibleCategoryNodes('u-admin').some((node) => node.path === failureRow.dataset.productPath));

const renderActionButton = loadFunction('renderActionButton', { escapeHtml });
const stateMeta = { download: { icon: '↓', tone: 'download', tip: '待拉取' } };
const renderFailedBindingRow = loadFunction('renderBindingRow', {
  ...sharedTaskDependencies,
  stateMeta,
  isBranchLocked: () => false,
  renderActionButton,
  escapeHtml,
  formatBindingRefreshTime: () => '',
  syncRoleControls: () => {}
});
renderFailedBindingRow(failureRow);
assert.match(failureRow.querySelector('[data-actions]').innerHTML, /aria-label="重试首次拉取"/);
assert.equal(failureHarness.startPipeline(failureRow.dataset.bindingId, 'pull'), true);
const retryTask = failureHarness.tasks[0];
assert.equal(retryTask.initialPull, true);
assert.equal(retryTask.simulatedFirstPullFailure, false);
advanceTask(retryTask.id);
advanceTask(retryTask.id);
assert.equal(retryTask.state, 'waiting');
assert.equal(retryTask.previewPending, true);
assert.equal(failureRow.dataset.initialPullPending, 'true', 'pull preview alone does not finalize the new binding');
assert.equal(previewDialog.hasAttribute('open'), true);

const finishPipeline = loadFunction('finishPipeline', {
  ...sharedTaskDependencies,
  activePreviewTaskId: retryTask.id,
  finishBindingOperation: () => {},
  renderBindingRow: () => {},
  renderQueue: () => {},
  refreshOpenBindingHistory: () => {},
  currentBindingId: ''
});
const confirmPullWriteback = loadFunction('confirmPullWriteback', {
  ...sharedTaskDependencies,
  finishPipeline,
  showToast: () => {}
});
confirmPullWriteback(retryTask.id);
assert.equal(failureRow.dataset.state, 'synced');
assert.equal(failureRow.dataset.initialPullPending, 'false');
assert.equal(failureHarness.tasks.length, 0, 'the task completes only after explicit writeback confirmation');

const conflictHarness = createHarness();
submitBinding(conflictHarness, { target: '思源宏瑞 / 通用平台部 / 通用基础词条', branch: 'feature-conflict-demo' });
const conflictRow = conflictHarness.document.querySelector('#bindingTable tbody tr:last-child');
const conflictTask = conflictHarness.tasks[0];
conflictTask.conflictRecheckPending = true;
const conflictFiles = [{ path: 'src/i18n/en_US.ts', noConflictFixture: false }];
const advanceConflictTask = loadFunction('advanceTask', {
  ...sharedTaskDependencies,
  document: conflictHarness.document,
  tasks: conflictHarness.tasks,
  getConflictFilesForBinding: () => conflictFiles,
  resetConflictFiles: () => {},
  renderBindingRow: () => {},
  renderQueue: () => {},
  showToast: (message) => conflictHarness.toasts.push(message),
  renderPullPreview: () => {}
});
advanceConflictTask(conflictTask.id);
advanceConflictTask(conflictTask.id);
assert.equal(conflictTask.state, 'conflict');
assert.equal(conflictTask.hasConflicts, true);
assert.equal(conflictRow.dataset.state, 'conflict');
const renderQueue = loadFunction('renderQueue', {
  document: conflictHarness.document,
  tasks: conflictHarness.tasks,
  escapeHtml,
  getConflictBatchSummary: () => '1 个冲突文件',
  renderActionButton,
  syncRoleControls: () => {}
});
renderQueue();
assert.match(conflictHarness.document.getElementById('queueList').innerHTML, /data-action="open-conflict"/);

console.log('sync management binding checks passed');
