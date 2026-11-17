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

/** 从原型表格和 Mock 附加记录构造完整提交记录夹具。
 * @returns {Object<string, any>} 提交 ID 到提交记录的映射。
 */
function extractCommitRecords() {
  const document = new JSDOM(html).window.document;
  const start = script.indexOf('    const commitRecords = ');
  const end = script.indexOf('    const bindingProfiles =', start);
  assert.notEqual(start, -1, 'commitRecords initializer exists in the prototype');
  assert.notEqual(end, -1, 'bindingProfiles follows commitRecords');
  const setup = script.slice(start, end);
  return new Function('document', `${setup}\nreturn commitRecords;`)(document);
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
  const commitRecords = structuredClone(extractCommitRecords());
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
  const selectBindingCategory = loadFunction('selectBindingCategory', { document, syncBindingCategoryControls, setBindingCategoryPickerOpen });
  const toggleBindingCategoryPicker = loadFunction('toggleBindingCategoryPicker', { document, setBindingCategoryPickerOpen });
  const toggleBindingCategoryBranch = loadFunction('toggleBindingCategoryBranch');
  const handleBindingCategorySearch = loadFunction('handleBindingCategorySearch', { syncBindingCategoryControls, setBindingCategoryPickerOpen });
  const insertMockProductCategory = loadFunction('insertMockProductCategory', shared);
  const parseBranchAbsoluteUrl = loadFunction('parseBranchAbsoluteUrl');
  const renderBindingRow = () => {};
  const renderQueue = () => {};
  const createMockCommitRecord = loadFunction('createMockCommitRecord', {
    commitRecords,
    formatBindingRefreshTime: () => '2026/11/17 10:00'
  });
  const createLocalCommitRecord = loadFunction('createLocalCommitRecord', {
    document,
    bindingProfiles,
    commitRecords,
    createMockCommitRecord,
    formatBindingRefreshTime: () => '2026/11/17 10:00'
  });
  const startPipeline = loadFunction('startPipeline', {
    ...shared,
    bindingProfiles,
    commitRecords,
    createLocalCommitRecord,
    tasks,
    taskSequence: 3000,
    hasPermission: (userId, permission) => permission === 'write' || permission === 'create',
    isBranchLocked: (row) => row.dataset.activeLock === 'true',
    renderBindingRow,
    refreshOpenBindingHistory: () => {},
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
    setBindingCategoryPickerOpen,
    syncBindingCategoryControls,
    selectBindingCategory,
    toggleBindingCategoryPicker,
    toggleBindingCategoryBranch,
    handleBindingCategorySearch,
    startPipeline,
    createBinding
  };
}

/** 创建提交与推送流程所需的隔离页面状态。
 * @param {Object<string,number>} [options={}] 可配置冲突数量的测试场景。
 * @returns {Object<string, any>} 页面 DOM、提交拓扑、流水线和动作处理器。
 */
function createCommitFlowHarness(options = {}) {
  const dom = new JSDOM(html, { pretendToBeVisual: true });
  const { document } = dom.window;
  const bindingProfiles = structuredClone(extractInitializer('bindingProfiles'));
  const commitRecords = structuredClone(extractCommitRecords());
  const users = structuredClone(mockUsers);
  const tasks = [];
  const toasts = [];
  const scheduledTasks = [];
  const operationHistory = [];
  const hasPermission = () => true;
  const renderActionButton = loadFunction('renderActionButton', { escapeHtml });
  const isBranchLocked = (row) => row.dataset.activeLock === 'true';
  const createMockCommitRecord = loadFunction('createMockCommitRecord', {
    commitRecords,
    formatBindingRefreshTime: () => '2026/11/17 10:00'
  });
  const createLocalCommitRecord = loadFunction('createLocalCommitRecord', {
    document,
    bindingProfiles,
    commitRecords,
    createMockCommitRecord,
    formatBindingRefreshTime: () => '2026/11/17 10:00'
  });
  const renderBindingRow = loadFunction('renderBindingRow', {
    document,
    tasks,
    stateMeta: extractInitializer('stateMeta'),
    isBranchLocked,
    getConflictBatchSummary: () => '1 个冲突文件',
    renderActionButton,
    escapeHtml,
    formatBindingRefreshTime: () => '',
    syncRoleControls: () => {}
  });
  const startPipeline = loadFunction('startPipeline', {
    document,
    bindingProfiles,
    commitRecords,
    createLocalCommitRecord,
    mockUserDirectory: users,
    tasks,
    taskSequence: 3000,
    hasPermission,
    isBranchLocked,
    renderBindingRow,
    refreshOpenBindingHistory: () => {},
    renderQueue: () => {},
    scheduleTaskProgress: (taskId) => scheduledTasks.push(taskId),
    showToast: (message) => toasts.push(message)
  });
  const openPendingCommitDialog = loadFunction('openPendingCommitDialog', {
    document,
    bindingProfiles,
    hasPermission,
    isBranchLocked,
    showToast: (message) => toasts.push(message)
  });
  const submitPendingCommit = loadFunction('submitPendingCommit', {
    document,
    startPipeline,
    showToast: (message) => toasts.push(message)
  });
  const handleAction = loadFunction('handleAction', {
    document,
    tasks,
    bulkAcceptInProgress: false,
    activeDetailRow: null,
    hasPermission,
    showToast: (message) => toasts.push(message),
    openPendingCommitDialog,
    startPipeline
  });
  const getBindingCommitState = loadFunction('getBindingCommitState', { commitRecords });
  const renderBindingDetails = loadFunction('renderBindingDetails', {
    document,
    getRecentOperationHistory: () => [],
    escapeHtml,
    formatOperationTime: () => ''
  });
  const finishPipeline = loadFunction('finishPipeline', {
    document,
    tasks,
    bindingProfiles,
    commitRecords,
    getBindingCommitState: loadFunction('getBindingCommitState', { commitRecords }),
    recordBindingOperation: (...args) => operationHistory.push(args),
    getConflictCounts: () => ({ unresolved: options.unresolvedConflicts || 0 }),
    renderBindingRow,
    renderQueue: () => {},
    refreshOpenBindingHistory: () => {},
    activePreviewTaskId: '',
    currentBindingId: ''
  });
  const finishAbort = loadFunction('finishAbort', {
    document,
    tasks,
    beginHistoryChange: () => ({}),
    commitHistoryChange: () => {},
    recordBindingOperation: (...args) => operationHistory.push(args),
    renderBindingRow,
    renderQueue: () => {},
    refreshOpenBindingHistory: () => {},
    showToast: (message) => toasts.push(message),
    activePreviewTaskId: '',
    currentBindingId: ''
  });
  const dialog = document.getElementById('pendingCommitDialog');
  dialog.showModal = () => dialog.setAttribute('open', '');
  dialog.close = () => dialog.removeAttribute('open');
  document.getElementById('roleSelect').innerHTML = users.map((user) => `<option value="${user.id}">${user.userName}</option>`).join('');
  document.getElementById('roleSelect').value = 'u-admin';

  return { dom, document, bindingProfiles, commitRecords, tasks, toasts, scheduledTasks, dialog, renderBindingRow, renderBindingDetails, handleAction, submitPendingCommit, getBindingCommitState, finishPipeline, finishAbort };
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
  const selectedNode = harness.getVisibleCategoryNodes('u-admin').find((node) => node.path === document.getElementById('newTarget').value);
  document.getElementById('newTargetDisplayText').textContent = selectedNode?.name || '';
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

/** 按完整内部路径查找分类树节点。
 * @param {Document} document 当前测试 DOM。
 * @param {string} path 分类完整路径。
 * @returns {HTMLElement|undefined} 匹配的树节点。
 */
function findCategoryTreeItem(document, path) {
  return [...document.querySelectorAll('#categoryTree [role="treeitem"]')].find((item) => item.dataset.categoryPath === path);
}

const adminTreeItems = [...uiHarness.document.querySelectorAll('#categoryTree [role="treeitem"]')];
const adminTreePaths = adminTreeItems.map((item) => item.dataset.categoryPath);
const adminCategoryTree = uiHarness.document.getElementById('categoryTree');
const existingBindingPaths = [...uiHarness.document.querySelectorAll('#bindingTable tbody tr')].map((row) => row.dataset.productPath);
assert.ok(adminTreePaths.includes('思源宏瑞 / 企业应用部 / 企业应用'));
assert.ok(adminTreePaths.includes('公共分类 / 公共库'));
assert.ok(!adminCategoryTree.textContent.includes(' / '), 'tree items show node names without full paths');
assert.ok([...adminCategoryTree.querySelectorAll('.category-node-title')].every((node) => !node.textContent.includes(' / ')));
assert.match(html, /@media \(max-width:700px\) \{ \.binding-category-row\.has-category-suffix \{ grid-template-columns:1fr;/, 'category and suffix stack on narrow screens');
for (const path of existingBindingPaths) {
  const treeItem = findCategoryTreeItem(uiHarness.document, path);
  const selectButton = treeItem?.querySelector('.category-tree-select');
  assert.ok(treeItem, `bound category ${path} exists in the menu tree`);
  assert.equal(selectButton.dataset.categoryBound, 'true');
  assert.equal(selectButton.disabled, true, `bound category ${path} is disabled in ordinary mode`);
}
roleSelect.value = 'u-cloud';
handleDocumentChange({ target: roleSelect });
const categoryTree = uiHarness.document.getElementById('categoryTree');
const cloudTreeItems = [...categoryTree.querySelectorAll('[role="treeitem"]')];
const cloudPaths = cloudTreeItems.map((item) => item.dataset.categoryPath);
assert.ok(cloudPaths.includes('思源宏瑞 / 云平台部 / 云端升级'));
assert.ok(cloudPaths.includes('公共分类 / 公共库'));
assert.ok(!cloudPaths.some((path) => path.includes('通用平台部')));
assert.ok(cloudTreeItems.every((item) => {
  const button = item.querySelector('.category-tree-select');
  return button.disabled === (button.dataset.categoryLeaf !== 'true' || button.dataset.categoryBound === 'true');
}));
roleSelect.value = 'u-review';
handleDocumentChange({ target: roleSelect });
const reviewPaths = [...categoryTree.querySelectorAll('[role="treeitem"]')].map((item) => item.dataset.categoryPath);
assert.ok(reviewPaths.includes('思源宏瑞 / 词条管理部 / 术语共享'));
assert.ok(reviewPaths.includes('公共分类 / 公共库'));
assert.ok(!reviewPaths.some((path) => path.includes('云平台部')));
roleSelect.value = 'u-cloud';
handleDocumentChange({ target: roleSelect });

roleSelect.value = 'u-admin';
handleDocumentChange({ target: roleSelect });
uiHarness.toggleBindingCategoryPicker();
const categorySearch = uiHarness.document.getElementById('categoryTreeSearch');
categorySearch.value = '通用基础词条';
uiHarness.handleBindingCategorySearch();
const searchedPaths = [...categoryTree.querySelectorAll('[role="treeitem"]')].map((item) => item.dataset.categoryPath);
assert.deepEqual(searchedPaths, ['思源宏瑞', '思源宏瑞 / 通用平台部', '思源宏瑞 / 通用平台部 / 通用基础词条']);
assert.equal(uiHarness.document.getElementById('categoryTreePopup').hidden, false, 'search keeps the tree open');
categorySearch.value = '';
uiHarness.handleBindingCategorySearch();
const ordinaryTargetPath = '思源宏瑞 / 通用平台部 / 通用基础词条';
const ordinaryTargetButton = findCategoryTreeItem(uiHarness.document, ordinaryTargetPath).querySelector('.category-tree-select');
uiHarness.selectBindingCategory(ordinaryTargetButton);
assert.equal(uiHarness.document.getElementById('newTarget').value, ordinaryTargetPath);
assert.equal(uiHarness.document.getElementById('newTargetDisplayText').textContent, '通用基础词条');
assert.equal(uiHarness.document.getElementById('categoryTreePopup').hidden, true);
const newCategoryMode = uiHarness.document.getElementById('newCategoryMode');
const suffixInput = uiHarness.document.getElementById('newCategorySuffix');
const boundCategoryPath = '思源宏瑞 / 通用平台部 / 云平台';
let boundCategoryItem = findCategoryTreeItem(uiHarness.document, boundCategoryPath);
let boundCategoryOption = boundCategoryItem?.querySelector('.category-tree-select');
assert.ok(boundCategoryOption, 'existing binding category is present for the administrator');
assert.equal(boundCategoryOption.dataset.categoryBound, 'true');
assert.equal(boundCategoryOption.disabled, true, 'bound leaf is disabled in ordinary mode');
newCategoryMode.checked = true;
handleDocumentChange({ target: newCategoryMode });
const suffixFields = uiHarness.document.getElementById('newCategoryFields');
assert.equal(suffixFields.hidden, false);
assert.equal(suffixFields.parentElement.classList.contains('binding-category-row'), true);
assert.equal(suffixFields.previousElementSibling.classList.contains('category-path-field'), true);
assert.equal(suffixInput.required, true);
boundCategoryItem = findCategoryTreeItem(uiHarness.document, boundCategoryPath);
boundCategoryOption = boundCategoryItem?.querySelector('.category-tree-select');
assert.equal(boundCategoryOption.disabled, true, 'bound leaf stays disabled in create mode');
for (const path of existingBindingPaths) {
  const option = findCategoryTreeItem(uiHarness.document, path)?.querySelector('.category-tree-select');
  assert.equal(option.disabled, true, `bound category ${path} stays disabled in create mode`);
}
for (const item of [...categoryTree.querySelectorAll('[role="treeitem"]')]) {
  const button = item.querySelector('.category-tree-select');
  const parentType = ['department', 'classify'].includes(item.dataset.categoryType);
  assert.equal(button.disabled, button.dataset.categoryBound === 'true' || !parentType);
}
const parentPath = '思源宏瑞 / 云平台部';
const parentButton = findCategoryTreeItem(uiHarness.document, parentPath).querySelector('.category-tree-select');
assert.equal(parentButton.disabled, false, 'create mode allows an eligible visible parent');
uiHarness.toggleBindingCategoryPicker();
assert.equal(uiHarness.document.getElementById('categoryTreePopup').hidden, false);
assert.equal(uiHarness.document.getElementById('newTargetDisplay').getAttribute('aria-expanded'), 'true');
assert.equal(uiHarness.document.getElementById('categoryTreePopup').style.position, 'fixed', 'tree popup escapes the scrolling dialog body');
uiHarness.toggleBindingCategoryPicker();
const pickerTrigger = uiHarness.document.getElementById('newTargetDisplay');
pickerTrigger.getBoundingClientRect = () => ({ top: 650, bottom: 686, left: 900, right: 1100, width: 200, height: 36 });
Object.defineProperty(uiHarness.document.documentElement, 'clientWidth', { configurable: true, value: 1000 });
Object.defineProperty(uiHarness.document.documentElement, 'clientHeight', { configurable: true, value: 700 });
uiHarness.setBindingCategoryPickerOpen(true);
const categoryPopup = uiHarness.document.getElementById('categoryTreePopup');
assert.equal(categoryPopup.style.top, '352px', 'tree popup moves above the selector when space below is limited');
assert.equal(categoryPopup.style.left, '792px', 'tree popup stays within the viewport width');
assert.equal(categoryPopup.style.maxHeight, '294px', 'tree popup height is limited to available space');
uiHarness.setBindingCategoryPickerOpen(false);
assert.equal(categoryPopup.style.position, '', 'closing the tree clears anchored popup positioning');
const rootTreeItem = findCategoryTreeItem(uiHarness.document, '思源宏瑞');
const rootExpander = rootTreeItem.querySelector('.category-tree-expander:not(.is-empty)');
const rootGroup = rootTreeItem.querySelector('.category-tree-group');
uiHarness.toggleBindingCategoryBranch(rootExpander);
assert.equal(rootGroup.hidden, true);
assert.equal(rootExpander.getAttribute('aria-expanded'), 'false');
uiHarness.toggleBindingCategoryBranch(rootExpander);
assert.equal(rootGroup.hidden, false);
uiHarness.selectBindingCategory(parentButton);
assert.equal(uiHarness.document.getElementById('newTarget').value, parentPath);
assert.equal(uiHarness.document.getElementById('newTargetDisplayText').textContent, '云平台部');
assert.equal(uiHarness.document.getElementById('categoryTreePopup').hidden, true);
suffixInput.value = '临时分类';
newCategoryMode.checked = false;
handleDocumentChange({ target: newCategoryMode });
assert.equal(suffixFields.hidden, true);
assert.equal(suffixInput.required, false);
assert.equal(suffixInput.value, '', 'leaving create mode clears the suffix');
assert.equal(uiHarness.document.getElementById('newTarget').value, '', 'a parent selection is cleared when returning to leaf-only mode');

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
assert.equal(categoryHarness.getVisibleCategoryNodes('u-cloud').find((node) => node.path === categoryRow.dataset.productPath).type, 'classify');
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
assertRejectedBinding({ target: '公共分类 / 公共库', createCategory: true, suffix: '不允许的父级' }, /父路径只能选择/);
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

const commitDialogProbe = new JSDOM(html).window.document.getElementById('pendingCommitDialog');
assert.ok(commitDialogProbe, 'pending worktree changes have a commit-message dialog');
assert.ok(script.includes('function openPendingCommitDialog('), 'pending action opens the commit-message dialog');
assert.ok(script.includes('function submitPendingCommit('), 'dialog submission validates and commits pending changes');

const commitHarness = createCommitFlowHarness();
const pendingRow = commitHarness.document.querySelector('[data-binding-id="bind-checkout"]');
commitHarness.renderBindingRow(pendingRow);
const pendingAction = pendingRow.querySelector('[data-action="open-pending-commit"]');
assert.ok(pendingAction, 'pending changes open the commit-message dialog instead of pushing immediately');
const pendingProfile = commitHarness.bindingProfiles['bind-checkout'];
const pendingInput = commitHarness.document.getElementById('pendingCommitMessage');
const initialCommitCount = Object.keys(commitHarness.commitRecords).length;
commitHarness.handleAction(pendingAction);
assert.equal(commitHarness.dialog.open, true);
assert.equal(pendingInput.value, '');
assert.equal(pendingProfile.workingTreeDirty, true);
assert.equal(commitHarness.tasks.length, 0);
assert.equal(Object.keys(commitHarness.commitRecords).length, initialCommitCount);

commitHarness.handleAction(commitHarness.dialog.querySelector('[data-action="close-pending-commit"]'));
assert.equal(pendingProfile.workingTreeDirty, true, 'cancelling leaves the worktree dirty');
assert.equal(commitHarness.tasks.length, 0, 'cancelling does not start a push');
commitHarness.handleAction(pendingAction);
pendingInput.value = '   \n  ';
commitHarness.submitPendingCommit({ preventDefault() {} });
assert.equal(commitHarness.dialog.open, true, 'blank commit messages keep the dialog open');
assert.equal(pendingProfile.workingTreeDirty, true);
assert.equal(commitHarness.tasks.length, 0);
assert.equal(Object.keys(commitHarness.commitRecords).length, initialCommitCount);

pendingInput.value = 'feat(i18n): 修正待提交工作区';
commitHarness.submitPendingCommit({ preventDefault() {} });
assert.equal(commitHarness.dialog.open, false);
assert.equal(pendingProfile.workingTreeDirty, false);
assert.equal(commitHarness.tasks.length, 1);
const pendingCommitId = pendingProfile.localCommitsAfterBase.at(-1);
assert.match(pendingCommitId, /^[a-f0-9]{7}$/);
assert.equal(commitHarness.commitRecords[pendingCommitId].message, pendingInput.value.trim());
assert.ok(commitHarness.commitRecords[pendingCommitId].date);
commitHarness.renderBindingDetails({
  bindingId: 'bind-checkout',
  breadcrumb: '思源宏瑞 / 云平台部 / mon-cn-1.15.0',
  remoteUrl: 'git@10.17.196.23:platform/lang.git',
  branch: 'mon-cn-1.15.0',
  branchUrl: 'https://git.example.com/group/lang/tree/mon-cn-1.15.0',
  stateLabel: '待上传',
  branchLock: '流水线运行中',
  commitState: commitHarness.getBindingCommitState(pendingProfile)
});
const commitCards = [...commitHarness.document.querySelectorAll('#commitDetails .commit-card')];
assert.equal(commitCards[0].querySelector('.commit-card-id').textContent, `提交 ID · ${pendingCommitId}`);
assert.equal(commitCards[0].querySelector('.commit-card-message').textContent, pendingInput.value.trim());
assert.equal(commitCards[0].querySelector('.commit-card-date').textContent, `提交时间 · ${commitHarness.commitRecords[pendingCommitId].date}`);
assert.match(commitCards[1].textContent, /提交 ID · 7f4c821/);
assert.match(commitCards[2].textContent, /提交 ID · 7f4c821/);
assert.equal(commitHarness.tasks[0].returnState, 'upload', 'aborting the push preserves the newly created local commit');
const stateBeforePush = commitHarness.getBindingCommitState(pendingProfile);
assert.equal(stateBeforePush.localHead.id, pendingCommitId);
assert.equal(stateBeforePush.remoteHead.id, pendingProfile.syncBaseId);
assert.notEqual(pendingProfile.syncBaseId, pendingCommitId, 'the baseline does not advance before push success');

const pushTask = commitHarness.tasks[0];
commitHarness.finishAbort(pushTask.id);
assert.equal(pendingRow.dataset.state, 'upload');
assert.ok(pendingRow.querySelector('[data-action="start-pipeline"]'), 'aborted push returns to the direct upload action');
assert.equal(pendingProfile.localCommitsAfterBase.at(-1), pendingCommitId);
assert.equal(commitHarness.getBindingCommitState(pendingProfile).localHead.id, pendingCommitId);
assert.equal(commitHarness.getBindingCommitState(pendingProfile).remoteHead.id, pendingProfile.syncBaseId);

const successHarness = createCommitFlowHarness();
const successRow = successHarness.document.querySelector('[data-binding-id="bind-checkout"]');
successHarness.renderBindingRow(successRow);
successHarness.handleAction(successRow.querySelector('[data-action="open-pending-commit"]'));
successHarness.document.getElementById('pendingCommitMessage').value = 'fix(i18n): 补齐 HEAD 元数据';
successHarness.submitPendingCommit({ preventDefault() {} });
const successfulCommitId = successHarness.bindingProfiles['bind-checkout'].localCommitsAfterBase.at(-1);
successHarness.finishPipeline(successHarness.tasks[0], successRow);
const syncedProfile = successHarness.bindingProfiles['bind-checkout'];
const syncedState = successHarness.getBindingCommitState(syncedProfile);
assert.equal(successRow.dataset.state, 'synced');
assert.equal(syncedProfile.syncBaseId, successfulCommitId);
assert.deepEqual(syncedProfile.localCommitsAfterBase, []);
assert.deepEqual(syncedProfile.remoteCommitsAfterBase, []);
assert.equal(syncedState.localHead.id, successfulCommitId);
assert.equal(syncedState.remoteHead.id, successfulCommitId);

const mergeHarness = createCommitFlowHarness();
const mergeRow = mergeHarness.document.querySelector('[data-binding-id="bind-checkout"]');
const mergeProfile = mergeHarness.bindingProfiles['bind-checkout'];
const mergeBaseId = mergeProfile.syncBaseId;
const mergeTask = { id: 'sync-merge-test', bindingId: 'bind-checkout', operation: 'push', state: 'conflict', hasConflicts: true };
const mergeFiles = [{ path: 'src/i18n/en_US.ts', mergeCompleted: true }];
const mergeProgress = [];
const createMergeCommitRecord = loadFunction('createMockCommitRecord', {
  commitRecords: mergeHarness.commitRecords,
  formatBindingRefreshTime: () => '2026/11/17 10:00'
});
const continuePipelineAfterConflictFiles = loadFunction('continuePipelineAfterConflictFiles', {
  document: mergeHarness.document,
  files: mergeFiles,
  bindingProfiles: mergeHarness.bindingProfiles,
  createMockCommitRecord: createMergeCommitRecord,
  hasPermission: () => true,
  appendTerminal: () => {},
  renderConflictFile: () => {},
  renderBindingRow: () => {},
  renderQueue: () => {},
  scheduleTaskProgress: (taskId) => mergeProgress.push(taskId),
  showToast: (message) => mergeHarness.toasts.push(message)
});
mergeHarness.document.getElementById('commitMessage').value = 'fix(i18n): 记录冲突合并提交';
continuePipelineAfterConflictFiles(mergeTask, mergeRow);
const mergeCommitId = mergeProfile.localCommitsAfterBase.at(-1);
assert.match(mergeCommitId, /^[a-f0-9]{7}$/);
assert.equal(mergeHarness.commitRecords[mergeCommitId].message, 'fix(i18n): 记录冲突合并提交');
assert.equal(mergeTask.advanceCommitTopology, true);
assert.equal(mergeTask.operation, 'push');
assert.equal(mergeTask.state, 'running');
assert.deepEqual(mergeProgress, [mergeTask.id]);
assert.equal(mergeProfile.syncBaseId, mergeBaseId, 'the merge commit does not advance the baseline before push success');
assert.equal(mergeHarness.getBindingCommitState(mergeProfile).localHead.id, mergeCommitId);
assert.equal(mergeHarness.getBindingCommitState(mergeProfile).remoteHead.id, mergeBaseId);
mergeHarness.finishPipeline(mergeTask, mergeRow);
assert.equal(mergeProfile.syncBaseId, mergeCommitId, 'successful merge push advances the baseline to the merge commit');
assert.equal(mergeHarness.getBindingCommitState(mergeProfile).remoteHead.id, mergeCommitId);

const commitConflictHarness = createCommitFlowHarness({ unresolvedConflicts: 1 });
const commitConflictRow = commitConflictHarness.document.querySelector('[data-binding-id="bind-checkout"]');
commitConflictHarness.renderBindingRow(commitConflictRow);
commitConflictHarness.handleAction(commitConflictRow.querySelector('[data-action="open-pending-commit"]'));
commitConflictHarness.document.getElementById('pendingCommitMessage').value = 'fix(i18n): 保留冲突中的本地提交';
commitConflictHarness.submitPendingCommit({ preventDefault() {} });
const conflictProfile = commitConflictHarness.bindingProfiles['bind-checkout'];
const conflictCommitId = conflictProfile.localCommitsAfterBase.at(-1);
const conflictBaseId = conflictProfile.syncBaseId;
commitConflictHarness.tasks[0].hasConflicts = true;
commitConflictHarness.finishPipeline(commitConflictHarness.tasks[0], commitConflictRow);
assert.equal(commitConflictRow.dataset.state, 'conflict');
assert.equal(commitConflictHarness.tasks[0].state, 'conflict');
assert.equal(conflictProfile.localCommitsAfterBase.at(-1), conflictCommitId);
assert.equal(conflictProfile.syncBaseId, conflictBaseId, 'conflicting push does not advance the baseline');
assert.equal(commitConflictHarness.getBindingCommitState(conflictProfile).remoteHead.id, conflictBaseId);

const uploadHarness = createCommitFlowHarness();
const uploadRow = uploadHarness.document.querySelector('[data-binding-id="bind-web-shared"]');
uploadHarness.renderBindingRow(uploadRow);
const existingCommitId = uploadHarness.bindingProfiles['bind-web-shared'].localCommitsAfterBase.at(-1);
const uploadCommitCount = Object.keys(uploadHarness.commitRecords).length;
const uploadAction = uploadRow.querySelector('[data-action="start-pipeline"]');
assert.ok(uploadAction, 'existing local commits keep the direct push action');
uploadHarness.handleAction(uploadAction);
assert.equal(uploadHarness.dialog.open, false, 'already committed changes do not ask for another message');
assert.equal(uploadHarness.bindingProfiles['bind-web-shared'].localCommitsAfterBase.at(-1), existingCommitId);
assert.equal(Object.keys(uploadHarness.commitRecords).length, uploadCommitCount);

console.log('sync management binding checks passed');
