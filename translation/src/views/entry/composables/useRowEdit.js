/**
 * 已选词条普通编辑会话：snapshot / active、取消回滚。
 */
import { cloneDeep } from "lodash-es";
import { cancelEdit } from "@/utils/validationUtils.js";

/**
 * @returns {{
 *   beginSession: Function,
 *   finishSession: Function,
 *   cancelSession: Function,
 *   discardRow: Function,
 *   isActive: Function
 * }}
 */
export function useRowEdit() {
  /**
   * 若尚未进入普通编辑会话，则快照并激活。
   * @param {Object} vm
   */
  function beginSession(vm) {
    if (vm.manualEditActive) return;
    vm.manualEditSnapshot = cloneDeep(vm.dataSource);
    vm.manualEditActive = true;
  }

  /**
   * 结束会话并清空编辑态。
   * @param {Object} vm
   */
  function finishSession(vm) {
    vm.manualEditActive = false;
    vm.manualEditSnapshot = null;
    vm.editableData = {};
    vm.rules = {};
    vm.cellErrors = {};
  }

  /**
   * 取消：返回快照副本供壳 emit。
   * @param {Object} vm
   * @returns {Object[]}
   */
  function cancelSession(vm) {
    const restored = vm.manualEditSnapshot
      ? cloneDeep(vm.manualEditSnapshot)
      : cloneDeep(vm.dataSource || []);
    finishSession(vm);
    return restored;
  }

  /**
   * 行内 ×：按快照回滚该行语种列并退出编辑。
   * @param {Object} vm
   * @param {Object} record
   */
  function discardRow(vm, record) {
    const before = vm.manualEditSnapshot?.find((item) => item.id === record.id);
    const editRow = vm.editableData?.[record.id] || {};
    if (before) {
      const current = (vm.dataSource || []).find((item) => item.id === record.id);
      if (current) {
        for (const col of Object.keys(editRow)) {
          if (col === "id" || col === "children") continue;
          current[col] = before[col] ?? "";
        }
      }
    }
    cancelEdit(vm, record.id);
    if (vm.cellErrors?.[record.id]) delete vm.cellErrors[record.id];
  }

  /**
   * @param {Object} vm
   * @returns {boolean}
   */
  function isActive(vm) {
    return !!vm.manualEditActive;
  }

  return {
    beginSession,
    finishSession,
    cancelSession,
    discardRow,
    isActive,
  };
}
