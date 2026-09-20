/**
 * 词条已选 / 预翻译处理：底部保存流水线与行内确认。
 * 校验全部本地（checkLen / checkPlace），0 special API。
 */
import { message, notification } from "ant-design-vue";
import { updateEntryInfoList } from "@/http/api/entryManage";
import { checkTranslateSide } from "@/utils/formRules.js";
import { partitionBatchUpdateResults } from "@/utils/batchUpdateResults.js";
import {
  clearCellError,
  clearCellErrorsForRecords,
  getMaxLength,
  getMethods,
  isBlankTranslation,
  openSetEdit,
  setCellError,
} from "@/utils/validationUtils.js";
import commonParam from "@/constants/commonParam.js";
import { EDIT_NOTES } from "@/constants/editNotes.js";

/**
 * 只提交不校验：editableData → record 并退出编辑。
 * @param {Object} vm
 * @param {Object} row
 */
export function commitRow(vm, row) {
  const editRow = vm.editableData?.[row.id];
  if (!editRow) return;
  for (const col of Object.keys(editRow)) {
    if (col === "id" || col === "children") continue;
    row[col] = editRow[col];
  }
  delete vm.editableData[row.id];
  clearCellErrorsForRecords(vm, [row.id]);
}

/**
 * 对本行各语种按勾选规则本地校验；通过则 commitRow。
 * @param {Object} vm
 * @param {Object} row
 * @returns {boolean}
 */
export function confirmRow(vm, row) {
  const editRow = vm.editableData?.[row.id];
  if (!editRow) return true;
  const methods = getMethods(vm);
  const langList = commonParam.langValList || [];
  const entryText = editRow.entry ?? row.entry;
  let ok = true;

  for (const col of langList) {
    if (!(col in editRow) && row[col] == null) continue;
    const value = col in editRow ? editRow[col] : row[col];
    if (isBlankTranslation(value)) {
      clearCellError(vm, row.id, col);
      continue;
    }
    const err = checkTranslateSide({
      entry: entryText,
      translate: value,
      maxLength: getMaxLength(row, vm, "foreignMaxByte"),
      methods,
    });
    if (err) {
      setCellError(vm, row.id, col, err);
      ok = false;
      continue;
    }
    clearCellError(vm, row.id, col);
  }

  if (!ok) return false;
  commitRow(vm, row);
  return true;
}

/**
 * 对所有仍在 editableData 的行执行与 √ 相同的本地确认。
 * @param {Object} vm
 * @returns {boolean}
 */
export function confirmAll(vm) {
  const ids = Object.keys(vm.editableData || {});
  let allOk = true;
  for (const id of ids) {
    const row = (vm.dataSource || []).find((r) => String(r.id) === String(id));
    if (!row) continue;
    if (!confirmRow(vm, row)) allOk = false;
  }
  return allOk;
}

/** 把 editableData 叠到 record 上生成提交副本（不改原 record） */
function mergeEditableOntoRecord(record, editRow) {
  if (!editRow) return { ...record };
  const merged = { ...record };
  for (const col of Object.keys(editRow)) {
    if (col === "id" || col === "children") continue;
    merged[col] = editRow[col];
  }
  return merged;
}

/**
 * 将失败行的草稿保留在 editableData，并把 record 回退到快照。
 * @param {Object} vm
 * @param {Object} record
 * @param {Object|undefined} before
 * @param {Object} effective
 */
async function rehydrateFailedRow(vm, record, before, effective) {
  const cols = (vm.fieldsNeedSave || []).filter(
    (col) => (effective[col] ?? "") !== (before?.[col] ?? "")
  );
  const editCols =
    cols.length > 0
      ? cols
      : Object.keys(effective).filter((c) => c !== "id" && c !== "children");
  if (!vm.editableData?.[record.id]) {
    await openSetEdit(record, editCols, vm);
  }
  for (const col of editCols) {
    vm.editableData[record.id][col] = effective[col] ?? "";
  }
  if (before) {
    for (const col of vm.fieldsNeedSave || editCols) {
      record[col] = before[col] ?? "";
    }
  }
}

/**
 * 底部保存流水线：confirmAll → update → 成功三层同步回调 / 失败留下+notify。
 * @param {Object} vm
 * @param {Object} options
 * @param {string} [options.notes] - updateEntryInfoList notes
 * @param {Object[]} [options.snapshot] - 对比改动用快照
 * @param {boolean} [options.removeOnSuccess=false] - 成功行是否从 remaining 剔除（预翻译处理表）
 * @param {(ctx: {savedRecords:Object[],successIdSet:Set,remaining:Object[]}) => void} [options.onSync]
 * @returns {Promise<{allPassed:boolean,savedRecords:Object[],failedCount:number,remaining:Object[],remainingCount:number}>}
 */
export async function runSaveFlow(vm, options = {}) {
  const {
    notes = EDIT_NOTES,
    snapshot = null,
    removeOnSuccess = false,
    onSync = null,
  } = options;

  // 先收集「确认前」有效值，避免 confirmAll 提交后丢失草稿对照
  const snap = snapshot || [];
  const beforeById = new Map(snap.map((r) => [r.id, r]));
  const pendingPayloads = [];
  for (const record of vm.dataSource || []) {
    const effective = mergeEditableOntoRecord(
      record,
      vm.editableData?.[record.id]
    );
    const before = beforeById.get(record.id);
    const fields = vm.fieldsNeedSave || commonParam.langValList || [];
    if (
      !before ||
      fields.some((col) => (effective[col] ?? "") !== (before[col] ?? ""))
    ) {
      pendingPayloads.push({ record, effective });
    }
  }

  const allPassed = confirmAll(vm);
  if (!allPassed) {
    notification.error({
      message: "存在未通过校验的译文",
      description: "已标红，请修正后再保存。",
      duration: 0,
    });
    return {
      allPassed: false,
      savedRecords: [],
      failedCount: 0,
      remaining: vm.dataSource || [],
      remainingCount: (vm.dataSource || []).length,
    };
  }

  if (pendingPayloads.length === 0) {
    message.info("没有需要保存的改动。");
    return {
      allPassed: true,
      savedRecords: [],
      failedCount: 0,
      remaining: vm.dataSource || [],
      remainingCount: (vm.dataSource || []).length,
    };
  }

  let res;
  try {
    res = await updateEntryInfoList(
      pendingPayloads.map((item) => item.effective),
      { notes }
    );
  } catch (err) {
    notification.error({
      message: "保存失败",
      description: err?.message || String(err),
      duration: 0,
    });
    for (const { record, effective } of pendingPayloads) {
      await rehydrateFailedRow(
        vm,
        record,
        beforeById.get(record.id),
        effective
      );
    }
    return {
      allPassed: true,
      savedRecords: [],
      failedCount: pendingPayloads.length,
      remaining: vm.dataSource || [],
      remainingCount: (vm.dataSource || []).length,
    };
  }

  const { successIds, failedCount } = partitionBatchUpdateResults(res);
  const successIdSet = new Set(successIds);
  const savedRecords = pendingPayloads
    .filter((item) => successIdSet.has(item.record.id))
    .map((item) => item.effective);

  for (const { record, effective } of pendingPayloads) {
    if (successIdSet.has(record.id)) {
      // confirmAll 已 commit；浏览态成功行把 effective 写回兜底
      for (const col of Object.keys(effective)) {
        if (col === "id" || col === "children") continue;
        record[col] = effective[col];
      }
      if (vm.editableData?.[record.id]) {
        commitRow(vm, record);
      }
    } else {
      await rehydrateFailedRow(
        vm,
        record,
        beforeById.get(record.id),
        effective
      );
      const reason =
        (res?.data?.list || []).find((x) => x.id === record.id)?.message ||
        "落库失败";
      notification.error({
        message: `词条保存失败（id=${record.id}）`,
        description: reason,
        duration: 0,
      });
    }
  }

  if (failedCount > 0) {
    message.error(`有 ${failedCount} 条词条保存失败，已保留在列表中。`);
  }
  if (savedRecords.length > 0) {
    message.success(`已保存 ${savedRecords.length} 条词条。`);
  }

  const remaining = removeOnSuccess
    ? (vm.dataSource || []).filter((record) => !successIdSet.has(record.id))
    : [...(vm.dataSource || [])];

  if (typeof onSync === "function") {
    onSync({ savedRecords, successIdSet, remaining });
  }

  return {
    allPassed: true,
    savedRecords,
    failedCount,
    remaining,
    remainingCount: remaining.length,
  };
}

/**
 * 工厂：供 Options API 壳解构。
 * @returns {{confirmAll:Function,confirmRow:Function,commitRow:Function,runSaveFlow:Function}}
 */
export function useSaveFlow() {
  return { confirmAll, confirmRow, commitRow, runSaveFlow };
}
