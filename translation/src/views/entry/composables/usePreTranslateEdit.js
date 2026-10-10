/**
 * 批量编辑 / 预翻译处理表编排（挂在 BatchEditModal 工作表上）。
 * special 改本地 checkPlace；保存走 useSaveFlow。
 */
import { message, notification } from "ant-design-vue";
import { cloneDeep } from "lodash-es";
import { preTranslateEntry } from "@/http/api/entryManage";
import commonParam from "@/constants/commonParam.js";
import { checkTranslateSide } from "@/utils/formRules.js";
import { runSaveFlow } from "@/views/entry/composables/useSaveFlow.js";
import {
  getMaxLength,
  isBlankTranslation,
  openSetEdit,
  cancelEdit,
  setCellError,
} from "@/utils/validationUtils.js";

/** 语种中文名 → 语种列字段；未知返回 null */
function langNameToCol(langName) {
  const lang = commonParam.languageList.find((it) => it.name === langName);
  return lang ? lang.value : null;
}

/** 组装单语种预翻译请求 */
function buildRequest(language, priority, entries) {
  return preTranslateEntry({ translateType: language, priority }, entries);
}

/**
 * 合并单语种响应到 pendingMap
 * @param {Map} pendingMap
 * @param {*} _language
 * @param {Object} res
 * @param {string} langCol
 */
function mergeLanguageResponse(pendingMap, _language, res, langCol) {
  const list = res?.data?.list || [];
  for (const row of list) {
    if (row?.id == null) continue;
    if (!pendingMap.has(row.id)) pendingMap.set(row.id, {});
    pendingMap.get(row.id)[langCol] = row[langCol] ?? "";
  }
}

export function usePreTranslateEdit() {
  /**
   * 执行预翻译主流程（写 vm.dataSource / editableData）
   * @param {Object} vm
   * @param {Object} config
   * @returns {Promise<{successLanguages:string[],failedLanguages:string[]}>}
   */
  async function execute(vm, config) {
    const { language, priority, verifyMethods = [] } = config;
    vm.preTranslateSnapshot = cloneDeep(vm.dataSource);
    vm.reviewActive = true;

    const successLanguages = [];
    const failedLanguages = [];
    const pendingMap = new Map();

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
    const requestedButFailed = language.filter(
      (lang) =>
        !successLanguages.includes(lang) && !failedLanguages.includes(lang)
    );
    failedLanguages.push(...requestedButFailed);

    for (const lang of successLanguages) {
      const langCol = langNameToCol(lang);
      if (!langCol) continue;
      await applyLanguageToRows(vm, langCol, pendingMap, verifyMethods);
    }
    await classifySpecial(vm, verifyMethods);

    if (successLanguages.length > 0) {
      notification.success({
        message: `以下语种预翻译成功：${successLanguages.join(", ")}。`,
        description:
          "校验通过的译文已写入对应列；未通过的保持编辑态，请处理后保存。",
        duration: 0,
      });
    }
    if (failedLanguages.length > 0) {
      message.error(`以下语种预翻译失败：${failedLanguages.join(", ")}。`);
    }
    return { successLanguages, failedLanguages };
  }

  /**
   * 单语种列落地
   * @param {Object} vm
   * @param {string} langCol
   * @param {Map} pendingMap
   * @param {string[]} verifyMethods
   */
  async function applyLanguageToRows(vm, langCol, pendingMap, verifyMethods) {
    for (const record of vm.dataSource) {
      const pending = pendingMap.get(record.id);
      if (pending == null || !(langCol in pending)) continue;
      const translation = pending[langCol];
      if (isBlankTranslation(translation)) continue;

      let failMessage = null;
      if (
        verifyMethods.includes("toLong") ||
        verifyMethods.includes("noBadDisplay") ||
        verifyMethods.includes("noMultiSpace")
      ) {
        failMessage = checkTranslateSide({
          entry: record.entry,
          translate: translation,
          maxLength: getMaxLength(record, vm, "foreignMaxByte"),
          methods: verifyMethods.filter((k) => k !== "special"),
        });
      }

      if (failMessage) {
        await openSetEdit(record, [langCol], vm);
        vm.editableData[record.id][langCol] = translation;
        setCellError(vm, record.id, langCol, failMessage);
      } else {
        if (!vm._pendingOk) vm._pendingOk = new Map();
        if (!vm._pendingOk.has(record.id)) vm._pendingOk.set(record.id, {});
        vm._pendingOk.get(record.id)[langCol] = translation;
      }
    }
  }

  /**
   * special 本地判定 + 通过行落地
   * @param {Object} vm
   * @param {string[]} verifyMethods
   */
  async function classifySpecial(vm, verifyMethods) {
    const needSpecial = verifyMethods.includes("special");
    if (!vm._pendingOk || vm._pendingOk.size === 0) {
      flushPendingOk(vm);
      return;
    }
    for (const record of vm.dataSource) {
      const cols = vm._pendingOk.get(record.id);
      if (!cols) continue;
      for (const [col, translation] of Object.entries(cols)) {
        const err = needSpecial
          ? checkTranslateSide({
              entry: record.entry,
              translate: translation,
              methods: ["special"],
            })
          : null;
        if (err) {
          await openSetEdit(record, [col], vm);
          vm.editableData[record.id][col] = translation;
          setCellError(vm, record.id, col, err);
        } else {
          record[col] = translation;
        }
      }
    }
    vm._pendingOk = null;
  }

  /** @param {Object} vm */
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
   * 取消：恢复快照并清状态
   * @param {Object} vm
   * @returns {Object[]}
   */
  function cancelAll(vm) {
    const restored = vm.preTranslateSnapshot || [];
    vm._pendingOk = null;
    vm.editableData = {};
    vm.cellErrors = {};
    vm.rules = {};
    vm.reviewActive = false;
    vm.preTranslateSnapshot = null;
    return restored;
  }

  /**
   * 保存：confirmAll → update → 成功行从处理表移除
   * @param {Object} vm
   * @returns {Promise<Object>}
   */
  async function save(vm) {
    const snapshot = vm.preTranslateSnapshot || [];
    const result = await runSaveFlow(vm, {
      notes: "批量编辑",
      snapshot,
      removeOnSuccess: true,
    });

    if (!result.allPassed) {
      return result;
    }

    // 成功行移出处理表
    vm.dataSource = result.remaining;

    if (result.failedCount === 0) {
      vm.reviewActive = false;
      vm.preTranslateSnapshot = null;
    } else {
      const remainingIds = new Set(result.remaining.map((r) => r.id));
      vm.preTranslateSnapshot = snapshot.filter((r) => remainingIds.has(r.id));
      vm.reviewActive = true;
    }
    return result;
  }

  /**
   * 行内 ×：丢弃该行预翻译译文
   * @param {Object} vm
   * @param {Object} record
   */
  function discardRow(vm, record) {
    const snapshot = vm.preTranslateSnapshot || [];
    const before = snapshot.find((r) => r.id === record.id);
    if (before) {
      for (const col of Object.keys(vm.editableData?.[record.id] || {})) {
        if (col === "id" || col === "children") continue;
        const record_ = vm.dataSource.find((r) => r.id === record.id);
        if (record_) record_[col] = before[col] ?? "";
      }
    }
    cancelEdit(vm, record.id);
  }

  return {
    execute,
    cancelAll,
    save,
    discardRow,
    langNameToCol,
  };
}
