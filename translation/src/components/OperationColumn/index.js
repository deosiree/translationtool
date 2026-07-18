/**
 * @module OperationColumn
 * @description 表格操作列溢出：行内槽位 +「更多」下拉
 *
 * 公共 API：
 * - {@link OpItem} 声明式操作槽
 * - {@link OperationCellOverflow} 行内/更多切分容器
 * - {@link calcOpStrip} 槽位切分算法
 */
export { default as OpItem } from "./OpItem.vue";
export { default as OperationCellOverflow } from "./OperationCellOverflow.vue";
export { default as OpItemContent } from "./OpItemContent.vue";
export { calcOpStrip, readOpMeta, labelTextW, moreSlotW } from "./operationWidth.js";
