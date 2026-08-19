/**
 * 工作台流水线专用：分页默认值与翻页校验
 * 落点 views/workbench/composables
 */
import { verifyArray_workbench_page } from "@/utils/validationUtils";

/**
 * 工作台四页共用 pagination 默认配置
 * @param {Function} onChange 翻页回调，通常绑定 vm.pageChange
 * @returns {Object} Ant Table pagination 配置对象
 */
export function defaultPagination(onChange) {
  return {
    pageSizeOptions: ["20", "50", "100"],
    showSizeChanger: true,
    defaultPageSize: 20,
    total: 0,
    current: 1,
    pageSize: 20,
    showTotal: (total) => `共 ${total} 条`,
    onChange,
  };
}

/**
 * 翻页并校验当前页词条（SSOT，委托 validationUtils）
 * @param {Object} vm 含 pagination
 * @param {number} page 目标页码
 * @param {number} pageSize 每页条数
 * @param {() => string} getLang 当前语种字段，如 () => vm.task.transMap.value
 * @returns {void}
 */
export function pageChange(vm, page, pageSize, getLang) {
  vm.pagination.current = page;
  vm.pagination.pageSize = pageSize;
  verifyArray_workbench_page(vm.pagination, getLang(), vm);
}
