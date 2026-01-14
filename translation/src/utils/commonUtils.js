/**
 * 工具函数兼容层
 * 
 * 注意：此文件保留作为向后兼容层，所有函数已按功能分类到不同的工具文件中。
 * 建议新代码直接从分类文件或 index.js 导入，而不是从此文件导入。
 * 
 * 分类文件：
 * - testUtils.js - 测试工具
 * - dataStructureUtils.js - 数据结构处理
 * - requestUtils.js - HTTP/请求处理
 * - translationUtils.js - 翻译相关
 * - tableUtils.js - 表格相关
 * - validationUtils.js - 表单校验
 * - dateUtils.js - 时间处理
 * - selectionUtils.js - 表格选择/分页
 * - domUtils.js - DOM/UI工具
 * 
 * 统一导出入口：index.js
 */

// 从分类文件重新导出所有函数，保持向后兼容
export * from './testUtils';
export * from './dataStructureUtils';
export * from './requestUtils';
export * from './translationUtils';
export * from './tableUtils';
export * from './validationUtils';
export * from './dateUtils';
export * from './selectionUtils';
export * from './domUtils';
