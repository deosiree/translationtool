/**
 * 原型/开发测试用静态路由（不依赖后端菜单/权限配置）
 *
 * 使用说明：
 * - 本文件由原型开发维护（增删改原型页面只改这里）
 * - 需要在 `src/router/index.js` 中“可选导入”并追加到 `/translate` 的 children 中
 * - 路由写法约定：
 *   - path：相对 `/translate` 的子路径，例如 `workbench/prototype-import-config.html`
 *   - component：指向 `src/views/...` 下的“薄包装页”，包装页再引入 `prototype/*.vue` 原型组件
 */

export default [
  {
    // 访问：http://localhost:8080/#/translate/workbench/prototype-import-config.html
    path: 'workbench/prototype-import-config.html',
    name: 'prototypeImportConfig',
    // 原型页入口：整页导入模态框 + 内嵌 JSON/XML 配置文件原型
    component: () => import('./importConfig/PrototypePage.vue'),
  },
  {
    // 访问：http://localhost:8080/#/translate/workbench/prototype-pre-translate.html
    path: 'workbench/prototype-pre-translate.html',
    name: 'prototypePreTranslate',
    // 原型页入口：直接显示工作台 index 原型页（可选择任务并打开原型版翻译/预翻译模态框）
    component: () => import('./preTranslate/index.vue'),
  },
  {
    // 访问：http://localhost:8080/#/translate/workbench/prototype-import-config-v2.html
    path: 'workbench/prototype-import-config-v2.html',
    name: 'prototypeImportConfigV2',
    // 原型页入口：整页导入模态框 + 内嵌 JSON/XML 配置文件原型
    component: () => import('./importConfigV2/PrototypePage.vue'),
  },
  {
    // 访问：http://localhost:8080/#/translate/workbench/prototype-writeback-validate.html
    path: 'workbench/prototype-writeback-validate.html',
    name: 'prototypeWriteBackValidate',
    // 原型页入口：回写前校验 + 重复组处理（父子表格/勾选删除/重校验/回写/展示列设置）
    component: () => import('./writeBackValidate/PrototypePage.vue'),
  },
]

