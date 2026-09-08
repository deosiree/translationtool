/**
 * 词条管理-已选词条-预翻译编排器（Options API vm 传参风格，参照 useBatchPreTranslate/useLanguageFilter 模式）
 *
 * 职责：预翻译配置提交后到批量保存前的整段流程
 *   1. 快照 dataSource（取消时恢复）
 *   2. 按语种并行调用 /entryInfo/preTranslate，合并各语种响应（id 匹配，逐语种列叠加）
 *   3. 按勾选的校验规则（toLong/special）驱动编辑态：
 *      - 通过 → 译文写入 record 对应语种列（浏览态）
 *      - 失败 → 行进编辑态 + editableData 暂存译文 + cellErrors 红字
 *      - 空白译文 → 无变化，不动
 *   4. 底部切换为"取消/保存"模式（preTranslateActive）
 *   5. cancel：按快照恢复 dataSource、清编辑态/红字，模态框不关
 *   6. save：仍在编辑态的行先逐列校验（过→提交进 record；不过→保持编辑+红字），
 *      然后取有改动的行逐条 updateEntryInfo({notes:"预翻译"})，成功行从已选列表移除；
 *      全部保存成功且列表清空 → 通知壳关闭模态框，否则保持打开
 *
 * 复用 validationUtils 的现成机制：byteLength/getMaxLength/isBlankTranslation/openSetEdit/
 * cancelEdit/setCellError/clearCellError/clearCellErrorsForRecords/validateEditableCell
 */
import { message, notification } from "ant-design-vue";
import { cloneDeep } from "lodash-es";
import { preTranslateEntry, updateEntryInfo } from "@/http/api/entryManage";
import { checkSykEntryBeforeSave } from "@/http/api/glossary";
import commonParam from "@/constants/commonParam.js";
import {
  byteLength,
  getMaxLength,
  isBlankTranslation,
  openSetEdit,
  cancelEdit,
  setCellError,
  clearCellError,
  clearCellErrorsForRecords,
  validateEditableCell,
} from "@/utils/validationUtils.js";

/** 语种中文名 → 语种列字段（"英文"→"english"）；未知语种返回 null */
function langNameToCol(langName) {
  const lang = commonParam.languageList.find((it) => it.name === langName);
  return lang ? lang.value : null;
}

/** 组装单语种预翻译请求 */
function buildRequest(language, priority, entries) {
  return preTranslateEntry(
    { translateType: language, priority },
    entries
  );
}

/**
 * 合并单语种响应：id 匹配，把该语种列值叠写进 pendingMap（id -> { [col]: translation }）
 * 响应结构按工作台 preTranslate 契约：res.data.list 为词条数组（含该语种字段）
 */
function mergeLanguageResponse(pendingMap, language, res, langCol) {
  const list = res?.data?.list || [];
  for (const row of list) {
    if (row?.id == null) continue;
    if (!pendingMap.has(row.id)) pendingMap.set(row.id, {});
    pendingMap.get(row.id)[langCol] = row[langCol] ?? "";
  }
}

export function usePreTranslateEdit() {
  /**
   * 执行预翻译主流程
   * @param {Object} vm - 壳组件实例（须含 dataSource/classifyLimit/editableData/cellErrors/rules/columns/fieldsNeedSave）
   * @param {Object} config - { language: string[], priority: string, verifyMethods: string[] }
   * @returns {Promise<{successLanguages: string[], failedLanguages: string[]}>}
   */
  async function execute(vm, config) {
    const { language, priority, verifyMethods = [] } = config;
    // 1. 快照（取消回滚依据）
    vm.preTranslateSnapshot = cloneDeep(vm.dataSource);
    vm.preTranslateActive = true;

    const successLanguages = [];
    const failedLanguages = [];
    const pendingMap = new Map(); // id -> { [语种列]: 译文 }

    // 2. 按语种并行请求 + 逐语种合并
    const promises = language.map((lang) =>
      buildRequest(lang, priority, vm.dataSource).then((res) => {
        successLanguages.push(lang);
        return { lang, res };
      })
    );
    const results = await Promise.allSettled(promises);
    for (const item of results) {
      if (item.status !== "fulfilled") continue;
      const { lang, res } = item.value;
      const langCol = langNameToCol(lang);
      if (langCol) mergeLanguageResponse(pendingMap, lang, res, langCol);
      else failedLanguages.push(lang);
    }
    // 请求失败的语种（rejected）在 successLanguages 收集不到，直接算失败
    const requestedButFailed = language.filter(
      (lang) =>
        !successLanguages.includes(lang) && !failedLanguages.includes(lang)
    );
    failedLanguages.push(...requestedButFailed);

    // 3. 校验驱动编辑态（逐语种列）
    for (const lang of successLanguages) {
      const langCol = langNameToCol(lang);
      if (!langCol) continue;
      await applyLanguageToRows(vm, langCol, pendingMap, verifyMethods);
    }

    // 3.5 special 批量判定 + 通过行落地（长度通过的先暂存 _pendingOk）
    await classifySpecial(vm, verifyMethods);

    // 4. 汇总提示
    if (successLanguages.length > 0) {
      notification.success({
        message: `以下语种预翻译成功：${successLanguages.join(", ")}。`,
        description: "校验通过的译文已写入对应列；未通过的保持编辑态，请处理后保存。",
        duration: 0,
      });
    }
    if (failedLanguages.length > 0) {
      message.error(`以下语种预翻译失败：${failedLanguages.join(", ")}。`);
    }
    return { successLanguages, failedLanguages };
  }

  /**
   * 单语种列落地：写入/编辑态分发（不产生用户可感知的中间 UI）
   */
  async function applyLanguageToRows(vm, langCol, pendingMap, verifyMethods) {
    for (const record of vm.dataSource) {
      const pending = pendingMap.get(record.id);
      if (pending == null || !(langCol in pending)) continue; // 该词条无此语种译文
      const translation = pending[langCol];
      if (isBlankTranslation(translation)) continue; // 空白译文：无变化

      // 长度校验（toLong）
      let failMessage = null;
      if (verifyMethods.includes("toLong")) {
        const maxLength = getMaxLength(record, vm, "foreignMaxByte");
        if (maxLength && byteLength(translation) > maxLength) {
          failMessage = `允许最大字符数为${maxLength}(1中文=2字符)`;
        }
      }
      // 特殊字符校验（special）稍后统一批量判定（见 classifySpecial）

      if (failMessage) {
        await openSetEdit(record, [langCol], vm);
        vm.editableData[record.id][langCol] = translation;
        setCellError(vm, record.id, langCol, failMessage);
      } else {
        // 长度通过：先暂存到 pendingOk，等 special 批量判定后统一落地
        if (!vm._pendingOk) vm._pendingOk = new Map();
        if (!vm._pendingOk.has(record.id)) vm._pendingOk.set(record.id, {});
        vm._pendingOk.get(record.id)[langCol] = translation;
      }
    }
  }

  /**
   * special 校验批量判定 + 通过行落地（execute 尾部调用）
   */
  async function classifySpecial(vm, verifyMethods) {
    if (!verifyMethods.includes("special") || !vm._pendingOk || vm._pendingOk.size === 0) {
      // 无 special 校验：全部落地
      flushPendingOk(vm);
      return;
    }
    // 组装批量入参（每词条多语种列合并为多个 {id, entry, translate}）
    const datas = [];
    for (const record of vm.dataSource) {
      const cols = vm._pendingOk.get(record.id);
      if (!cols) continue;
      for (const translation of Object.values(cols)) {
        datas.push({ id: record.id, entry: record.entry, translate: translation });
      }
    }
    if (datas.length === 0) {
      flushPendingOk(vm);
      return;
    }
    let specialIds = new Set();
    try {
      const res = await checkSykEntryBeforeSave(datas);
      specialIds = new Set((res.data || []).map((item) => item.id));
    } catch (err) {
      // 接口异常：放行（与工作台 classifyArr 行为一致）
    }
    // 通过 → 写入对应列；不通过 → 进编辑态 + 红字
    for (const record of vm.dataSource) {
      const cols = vm._pendingOk.get(record.id);
      if (!cols) continue;
      for (const [col, translation] of Object.entries(cols)) {
        if (specialIds.has(record.id)) {
          await openSetEdit(record, [col], vm);
          vm.editableData[record.id][col] = translation;
          setCellError(vm, record.id, col, "特殊字符不一致\r\n(如%1翻译成% 1)");
        } else {
          record[col] = translation;
        }
      }
    }
    vm._pendingOk = null;
  }

  /** 把 _pendingOk 全部写入对应列（浏览态） */
  function flushPendingOk(vm) {
    if (!vm._pendingOk) return;
    for (const record of vm.dataSource) {
      const cols = vm._pendingOk.get(record.id);
      if (!cols) continue;
      for (const [col, translation] of Object.entries(cols)) {
        record[col] = translation;
      }
    }
    vm._pendingOk = null;
  }

  /**
   * 取消预翻译：恢复快照、清编辑态/红字/规则，退出编辑模式（模态框不关）
   */
  function cancelAll(vm) {
    // 快照恢复：直接替换引用，配合壳 emit update:dataSource
    const restored = vm.preTranslateSnapshot || [];
    vm._pendingOk = null;
    vm.editableData = {};
    vm.cellErrors = {};
    vm.rules = {};
    vm.preTranslateActive = false;
    vm.preTranslateSnapshot = null;
    // 壳监听此返回值负责 emit
    return restored;
  }

  /**
   * 保存：编辑行逐列校验 → 改动行逐条 updateEntryInfo → 成功行移除出已选列表
   * @returns {Promise<{savedRecords: Object[], allPassed: boolean, remainingCount: number, remaining?: Object[]}>}
   *   - allPassed：所有编辑行校验是否全通过（false 时存在红字行，不进入保存）
   */
  async function save(vm) {
    // 1. 仍在编辑态的行：先全列校验，全过再整体提交（避免第一列通过就删行导致后续列丢失编辑值）
    let allPassed = true;
    for (const record of vm.dataSource) {
      const editRow = vm.editableData?.[record.id];
      if (!editRow) continue;
      const cols = Object.keys(editRow).filter(
        (c) => c !== "id" && c !== "children"
      );
      // 两阶段：先逐列校验收集结果
      const colResults = [];
      let rowOk = true;
      for (const col of cols) {
        try {
          await validateEditableCell(vm, record.id, col);
          colResults.push({ col, ok: true });
        } catch (err) {
          rowOk = false;
          colResults.push({
            col,
            ok: false,
            message: err?.errorMessage || err?.message || String(err),
          });
        }
      }
      if (!rowOk) {
        allPassed = false;
        for (const { col, ok, message } of colResults) {
          if (!ok) setCellError(vm, record.id, col, message);
          else clearCellError(vm, record.id, col);
        }
        continue; // 该行保持编辑态
      }
      // 全列通过：一次性提交 + 退出编辑态
      for (const col of cols) {
        record[col] = editRow[col];
      }
      delete vm.editableData[record.id];
      clearCellErrorsForRecords(vm, [record.id]);
    }
    if (!allPassed) {
      message.warning("存在未通过校验的译文，已标红，请修正后再保存。");
      return { savedRecords: [], allPassed: false, remainingCount: vm.dataSource.length };
    }

    // 2. 对比快照取有改动的行
    const snapshot = vm.preTranslateSnapshot || [];
    const beforeById = new Map(snapshot.map((r) => [r.id, r]));
    const changedRecords = vm.dataSource.filter((record) => {
      const before = beforeById.get(record.id);
      if (!before) return true;
      return vm.fieldsNeedSave.some(
        (col) => (record[col] ?? "") !== (before[col] ?? "")
      );
    });

    if (changedRecords.length === 0) {
      // 没有任何改动（比如全部语种失败）
      vm.preTranslateActive = false;
      vm.preTranslateSnapshot = null;
      message.info("没有需要保存的改动。");
      return { savedRecords: [], allPassed: true, remainingCount: vm.dataSource.length };
    }

    // 3. 逐条保存（词条管理惯例：updateEntryInfo 逐条，notes 免编辑原因）
    const savePromises = changedRecords.map((record) =>
      updateEntryInfo(record, { notes: "预翻译" }).then(() => record)
    );
    const saveResults = await Promise.allSettled(savePromises);
    const savedRecords = saveResults
      .filter((item) => item.status === "fulfilled")
      .map((item) => item.value);
    const failedCount = saveResults.length - savedRecords.length;

    if (failedCount > 0) {
      message.error(`有 ${failedCount} 条词条保存失败，已保留在列表中。`);
    }
    if (savedRecords.length > 0) {
      message.success(`已保存 ${savedRecords.length} 条词条。`);
    }

    // 4. 成功行移除出已选列表（由壳 emit update:*）
    const savedIds = new Set(savedRecords.map((r) => r.id));
    const remaining = vm.dataSource.filter((record) => !savedIds.has(record.id));

    vm.preTranslateActive = false;
    vm.preTranslateSnapshot = null;
    return { savedRecords, allPassed: true, remainingCount: remaining.length, remaining };
  }

  /** 行内 ×：丢弃该行预翻译译文（恢复快照中的原值），退编辑态 */
  function discardRow(vm, record) {
    const snapshot = vm.preTranslateSnapshot || [];
    const before = snapshot.find((r) => r.id === record.id);
    if (before) {
      // 仅恢复预翻译涉及的语种列（用户未动过的其他字段不回滚）
      for (const col of Object.keys(vm.editableData?.[record.id] || {})) {
        if (col === "id" || col === "children") continue;
        const record_ = vm.dataSource.find((r) => r.id === record.id);
        if (record_) record_[col] = before[col] ?? "";
      }
    }
    cancelEdit(vm, record.id);
  }

  /** 行内 ✓：校验该行编辑态各列（两阶段：全过再整体提交），通过则写进 record（不落库，等底部保存） */
  async function confirmRow(vm, record) {
    const editRow = vm.editableData?.[record.id];
    if (!editRow) return true;
    const cols = Object.keys(editRow).filter(
      (c) => c !== "id" && c !== "children"
    );
    // 两阶段：先全列校验
    const colResults = [];
    let rowOk = true;
    for (const col of cols) {
      try {
        await validateEditableCell(vm, record.id, col);
        colResults.push({ col, ok: true });
      } catch (err) {
        rowOk = false;
        colResults.push({
          col,
          ok: false,
          message: err?.errorMessage || err?.message || String(err),
        });
      }
    }
    if (!rowOk) {
      for (const { col, ok, message } of colResults) {
        if (!ok) setCellError(vm, record.id, col, message);
        else clearCellError(vm, record.id, col);
      }
      return false;
    }
    // 全列通过：整体提交 + 退出编辑态
    for (const col of cols) {
      record[col] = editRow[col];
    }
    delete vm.editableData[record.id];
    clearCellErrorsForRecords(vm, [record.id]);
    return true;
  }

  return {
    execute,
    cancelAll,
    save,
    discardRow,
    confirmRow,
    langNameToCol,
  };
}
