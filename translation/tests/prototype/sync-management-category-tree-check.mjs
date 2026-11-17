import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { JSDOM } from 'jsdom';

const htmlUrl = new URL('../../prototype/syncManagement/sync-management-v2.html', import.meta.url);
const html = readFileSync(fileURLToPath(htmlUrl), 'utf8');
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

/** 在隔离的页面依赖对象中装载原型函数。
 * @param {string} name 函数名称。
 * @param {Object<string, any>} dependencies 函数运行时依赖。
 * @returns {Function} 可独立调用的页面函数。
 */
function loadFunction(name, dependencies = {}) {
  const scope = Object.assign(Object.create(globalThis), dependencies);
  return new Function('scope', `with (scope) { return (${extractFunction(name)}); }`)(scope);
}

/** 提取静态 Mock 初始化数据。
 * @param {string} name 常量名称。
 * @returns {any} 常量初始值。
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

/** 转义分类树中的文本与路径属性。
 * @param {string} value 待转义文本。
 * @returns {string} HTML 安全文本。
 */
function escapeHtml(value) {
  return String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

/** 按完整内部路径查找分类树节点。
 * @param {Document} document 当前测试 DOM。
 * @param {string} path 分类完整路径。
 * @returns {HTMLElement|undefined} 匹配的树节点。
 */
function findCategoryTreeItem(document, path) {
  return [...document.querySelectorAll('#categoryTree [role="treeitem"]')].find((item) => item.dataset.categoryPath === path);
}

/** 以无效表单值提交并确认没有新建绑定或流水线。
 * @param {Object<string, any>} values 分类、分支和 URL 字段值。
 * @param {RegExp} messagePattern 预期错误提示。
 * @returns {void} 断言提交被拒绝且没有副作用。
 */
function assertRejectedBinding(values, messagePattern) {
  const rowCount = document.querySelectorAll('#bindingTable tbody tr').length;
  const pipelineCount = startedPipelines.length;
  document.getElementById('newCategoryMode').checked = Boolean(values.createCategory);
  document.getElementById('newTarget').value = values.target;
  document.getElementById('newCategorySuffix').value = values.suffix || '';
  document.getElementById('newBranch').value = values.branch || 'feature-rejected-tree-check';
  document.getElementById('newBranchUrl').value = Object.hasOwn(values, 'branchUrl')
    ? values.branchUrl
    : 'https://git.example.com/group/lang/-/tree/feature-rejected-tree-check';
  createBinding({ preventDefault() {} });
  assert.equal(document.querySelectorAll('#bindingTable tbody tr').length, rowCount);
  assert.equal(startedPipelines.length, pipelineCount);
  assert.ok(toasts.some((message) => messagePattern.test(message)), `expected toast ${messagePattern}`);
}

const dom = new JSDOM(html, { pretendToBeVisual: true });
const { document } = dom.window;
const users = extractInitializer('mockUserDirectory');
const categories = structuredClone(extractInitializer('mockProductCategoryTree'));
const shared = { document, mockUserDirectory: users, mockProductCategoryTree: categories };
const getVisibleCategoryNodes = loadFunction('getVisibleCategoryNodes', shared);
const canSelectBindingCategory = loadFunction('canSelectBindingCategory');
const buildBindingCategoryTree = loadFunction('buildBindingCategoryTree');
const renderBindingCategoryTree = loadFunction('renderBindingCategoryTree', { buildBindingCategoryTree, canSelectBindingCategory, escapeHtml });
const setBindingCategoryPickerOpen = loadFunction('setBindingCategoryPickerOpen', { document });
const syncBindingCategoryControls = loadFunction('syncBindingCategoryControls', {
  ...shared,
  getVisibleCategoryNodes,
  canSelectBindingCategory,
  renderBindingCategoryTree,
  setBindingCategoryPickerOpen
});
const handleDocumentChange = loadFunction('handleDocumentChange', { document, syncBindingCategoryControls });
const selectBindingCategory = loadFunction('selectBindingCategory', { document, syncBindingCategoryControls, setBindingCategoryPickerOpen });
const toggleBindingCategoryBranch = loadFunction('toggleBindingCategoryBranch');
const handleBindingCategorySearch = loadFunction('handleBindingCategorySearch', { syncBindingCategoryControls, setBindingCategoryPickerOpen });
const insertMockProductCategory = loadFunction('insertMockProductCategory', shared);
const parseBranchAbsoluteUrl = loadFunction('parseBranchAbsoluteUrl');
const startedPipelines = [];
const toasts = [];
const createBinding = loadFunction('createBinding', {
  ...shared,
  hasPermission: () => true,
  getVisibleCategoryNodes,
  canSelectBindingCategory,
  insertMockProductCategory,
  parseBranchAbsoluteUrl,
  isBranchLocked: () => false,
  bindingProfiles: {},
  bindingPermissions: {},
  operationHistory: {},
  startPipeline: (bindingId, operation) => {
    startedPipelines.push({ bindingId, operation });
    return true;
  },
  renderBindingRow: () => {},
  initializeBindingRowFocus: () => {},
  renderQueue: () => {},
  closeDialogs: () => {},
  syncBindingCategoryControls,
  showToast: (message) => toasts.push(message)
});

document.getElementById('roleSelect').innerHTML = users.map((user) => `<option value="${user.id}">${user.userName}</option>`).join('');
document.getElementById('roleSelect').value = 'u-admin';
syncBindingCategoryControls();
const categoryRow = document.querySelector('.binding-category-row');
assert.equal(categoryRow.classList.contains('has-category-suffix'), false, 'leaf mode gives the category picker the full row');

const ordinaryPath = '思源宏瑞 / 通用平台部 / 通用基础词条';
const ordinaryItem = findCategoryTreeItem(document, ordinaryPath);
assert.ok(ordinaryItem, 'administrator can see the full category path in the tree');
assert.equal(ordinaryItem.querySelector('.category-node-title').textContent, '通用基础词条');
assert.ok(!document.getElementById('categoryTree').textContent.includes(' / '), 'visible tree labels omit full paths');
for (const row of document.querySelectorAll('#bindingTable tbody tr')) {
  const boundItem = findCategoryTreeItem(document, row.dataset.productPath);
  assert.ok(boundItem, `bound category ${row.dataset.productPath} is represented in the tree`);
  assert.equal(boundItem.querySelector('.category-tree-select').disabled, true, 'bound categories cannot be selected');
}

document.getElementById('roleSelect').value = 'u-cloud';
syncBindingCategoryControls();
const cloudPaths = [...document.querySelectorAll('#categoryTree [role="treeitem"]')].map((item) => item.dataset.categoryPath);
assert.ok(cloudPaths.includes('思源宏瑞 / 云平台部 / 云端升级'));
assert.ok(cloudPaths.includes('公共分类 / 公共库'));
assert.ok(!cloudPaths.some((path) => path.includes('通用平台部')));
document.getElementById('roleSelect').value = 'u-review';
syncBindingCategoryControls();
const reviewPaths = [...document.querySelectorAll('#categoryTree [role="treeitem"]')].map((item) => item.dataset.categoryPath);
assert.ok(reviewPaths.includes('思源宏瑞 / 词条管理部 / 术语共享'));
assert.ok(!reviewPaths.some((path) => path.includes('云平台部')));
document.getElementById('roleSelect').value = 'u-admin';
syncBindingCategoryControls();

const targetDisplay = document.getElementById('newTargetDisplay');
targetDisplay.getBoundingClientRect = () => ({ top: 650, bottom: 686, left: 900, right: 1100, width: 200, height: 36 });
Object.defineProperty(document.documentElement, 'clientWidth', { configurable: true, value: 1000 });
Object.defineProperty(document.documentElement, 'clientHeight', { configurable: true, value: 700 });
setBindingCategoryPickerOpen(true);
const categoryPopup = document.getElementById('categoryTreePopup');
assert.equal(categoryPopup.style.position, 'fixed');
assert.equal(categoryPopup.style.top, '352px', 'popup opens above the selector when the lower space is too small');
assert.equal(categoryPopup.style.left, '792px', 'popup stays within the viewport width');
assert.equal(categoryPopup.style.maxHeight, '294px');
setBindingCategoryPickerOpen(false);

const searchInput = document.getElementById('categoryTreeSearch');
setBindingCategoryPickerOpen(true);
searchInput.value = '通用基础词条';
handleBindingCategorySearch();
assert.deepEqual(
  [...document.querySelectorAll('#categoryTree [role="treeitem"]')].map((item) => item.dataset.categoryPath),
  ['思源宏瑞', '思源宏瑞 / 通用平台部', ordinaryPath],
  'search keeps matching ancestors in the category tree'
);
assert.equal(document.getElementById('categoryTreePopup').hidden, false);
searchInput.value = '';
handleBindingCategorySearch();
const leafButton = findCategoryTreeItem(document, ordinaryPath).querySelector('.category-tree-select');
selectBindingCategory(leafButton);
assert.equal(document.getElementById('newTarget').value, ordinaryPath);
assert.equal(document.getElementById('newTargetDisplayText').textContent, '通用基础词条');
assert.equal(document.getElementById('categoryTreePopup').hidden, true);

const createMode = document.getElementById('newCategoryMode');
const suffix = document.getElementById('newCategorySuffix');
const suffixField = document.getElementById('newCategoryFields');
createMode.checked = true;
document.dispatchEvent(new dom.window.Event('change', { bubbles: true }));
syncBindingCategoryControls();
assert.equal(suffixField.hidden, false);
assert.equal(suffixField.parentElement.classList.contains('binding-category-row'), true);
assert.equal(categoryRow.classList.contains('has-category-suffix'), true, 'create mode places the suffix beside the category picker');
assert.equal(suffixField.previousElementSibling.classList.contains('category-path-field'), true);
assert.equal(suffix.required, true);
for (const item of document.querySelectorAll('#categoryTree [role="treeitem"]')) {
  const button = item.querySelector('.category-tree-select');
  const allowedParent = ['department', 'classify'].includes(item.dataset.categoryType);
  assert.equal(button.disabled, button.dataset.categoryBound === 'true' || !allowedParent);
}

const parentPath = '思源宏瑞 / 云平台部';
const parentButton = findCategoryTreeItem(document, parentPath).querySelector('.category-tree-select');
selectBindingCategory(parentButton);
assert.equal(document.getElementById('newTarget').value, parentPath);
assert.equal(document.getElementById('newTargetDisplayText').textContent, '云平台部');
suffix.value = '新建树检查分类';
document.getElementById('newRepo').value = 'git@example.com:group/lang.git';
document.getElementById('newBranch').value = 'feature-tree-check';
document.getElementById('newBranchUrl').value = 'https://git.example.com/group/lang/-/tree/feature-tree-check';
createBinding({ preventDefault() {} });
const createdRow = document.querySelector('#bindingTable tbody tr:last-child');
assert.equal(createdRow.dataset.productPath, `${parentPath} / 新建树检查分类`);
assert.equal(getVisibleCategoryNodes('u-cloud').find((node) => node.path === createdRow.dataset.productPath)?.type, 'classify');
assert.deepEqual(startedPipelines, [{ bindingId: createdRow.dataset.bindingId, operation: 'pull' }]);

document.getElementById('newTarget').value = ordinaryPath;
document.getElementById('newBranch').value = 'feature-ordinary-tree-check';
document.getElementById('newBranchUrl').value = 'https://git.example.com/group/lang/-/tree/feature-ordinary-tree-check';
syncBindingCategoryControls();
createBinding({ preventDefault() {} });
const ordinaryRow = document.querySelector('#bindingTable tbody tr:last-child');
assert.equal(ordinaryRow.dataset.productPath, ordinaryPath);
assert.equal(ordinaryRow.dataset.branchUrl, 'https://git.example.com/group/lang/-/tree/feature-ordinary-tree-check');
assert.deepEqual(startedPipelines.at(-1), { bindingId: ordinaryRow.dataset.bindingId, operation: 'pull' });

document.getElementById('newCategoryMode').checked = true;
syncBindingCategoryControls();
assertRejectedBinding({ target: '公共分类', createCategory: true, suffix: '不应创建' }, /父路径只能选择/);
assertRejectedBinding({ target: '思源宏瑞 / 云平台部', createCategory: true, suffix: '', branch: 'feature-empty-suffix' }, /不能为空/);
assertRejectedBinding({ target: '思源宏瑞 / 通用平台部', createCategory: true, suffix: 'develop', branch: 'feature-duplicate-path' }, /完整分类路径已存在/);
assertRejectedBinding({ target: '思源宏瑞 / 通用平台部', createCategory: true, suffix: 'folder/child', branch: 'feature-invalid-segment' }, /路径片段/);
assertRejectedBinding({ target: ordinaryPath, createCategory: false, branch: 'feature-invalid-branch-url', branchUrl: 'ftp://git.example.com/group/lang/tree/main' }, /HTTP\(S\) URL/);
assertRejectedBinding({ target: '思源宏瑞 / 通用平台部', createCategory: false, branch: 'feature-parent-in-leaf-mode' }, /叶子分类/);
const boundCategoryPath = '思源宏瑞 / 通用平台部 / 云平台';
assertRejectedBinding({ target: boundCategoryPath, createCategory: false, branch: 'feature-bound-leaf' }, /已有绑定关系/);
assertRejectedBinding({ target: boundCategoryPath, createCategory: true, suffix: '不应创建', branch: 'feature-bound-parent' }, /已有绑定关系/);

document.getElementById('newCategoryMode').checked = true;
syncBindingCategoryControls();
document.getElementById('newTarget').value = '思源宏瑞 / 通用平台部';
document.getElementById('newCategorySuffix').value = '临时名称';
createMode.checked = false;
handleDocumentChange({ target: createMode });
assert.equal(suffixField.hidden, true);
assert.equal(suffix.required, false);
assert.equal(suffix.value, '', 'leaving create mode clears the category suffix');
assert.equal(document.getElementById('newTarget').value, '', 'parent choice is cleared when returning to leaf mode');
assert.equal(categoryRow.classList.contains('has-category-suffix'), false);

assert.match(html, /@media \(max-width:700px\) \{ \.binding-category-row\.has-category-suffix \{ grid-template-columns:1fr;/, 'category and suffix stack on narrow screens');
console.log('sync management category tree checks passed');
