import { entryImportExcle, entryImportExcle_v2, entryValidate_v2 as entryValidateApi_v2, updateEntryInfosByFile } from "@/http/api/entryManage";
import { message, notification } from "ant-design-vue";
import { isAllTranslationFields } from "@/utils/dataStructureUtils";
import commonParam from "@/constants/commonParam.js";

/**
 * 格式化 Map 对象为字符串
 * @param {Map} mapObj - 需要格式化的 Map 对象
 * @returns {string} 格式化后的字符串
 */
function formatMapToString(mapObj) {
  const result = [];
  mapObj.forEach((valueArray, key) => {
    const valueStr = valueArray.join(", ");
    result.push(`${key}：${valueStr}`);
  });
  return result.join("；");
}

/**
 * 批量导入 Excel 词条（旧版本 - 已备份）
 * @param {Array<string>} translateTypes - 需要导入的翻译语种列表
 * @param {FormData} formData - Excel 文件等表单数据
 * @returns {Promise<Object>} 返回结果对象，包含：
 *   - code: 200 表示完全成功，201 表示有失败信息
 *   - success: 成功语种列表
 *   - failed: 失败语种 Map
 *   - failedEntryInfos: 可重试失败词条数组
 *   - exceptionVos: 异常信息数组
 *   - globalMessage: 总体错误提示信息
 * @deprecated 已废弃，请使用新版本的 entryBatchImportExcel
 */
export async function entryBatchImportExcel(translateTypes, formData) {
  // 在函数作用域定义变量，确保在 try-catch 中都可以访问
  let hasCode201 = false;
  let returnMsg = {};

  try {
    console.log('参数', translateTypes, formData);
    const msg = { success: [], failed: new Map() };
    let allFailedEntryInfos = [];
    let allExceptionVos = [];
    let globalMessage = "";
    hasCode201 = false;

    // 每种翻译语种的导入
    for (const lang of translateTypes) {
      const params = {
        transType: lang,
      };
      try {
        await entryImportExcle(params, formData);
        msg.success.push(lang);
      } catch (error) {
        console.log(`${lang}导入响应：`, error);
        const errorData = error?.response?.data || error?.data || error;
        const { code, data } = errorData || {};

        if (code === 201) {
          // code=201 表示有失败信息，这是正常的业务响应
          hasCode201 = true;
          const failedInfos = data?.failedEntryInfos || [];
          const exceptionVos = data?.exceptionVos || data?.exceptionVOs || [];
          const globalMsg = data?.globalMessage || error?.data?.message || "";

          console.log('globalMsg', globalMsg);
          allFailedEntryInfos = allFailedEntryInfos.concat(failedInfos);
          allExceptionVos = allExceptionVos.concat(exceptionVos);

          if (globalMsg) {
            globalMessage = globalMsg;
          }

          // 记录失败语种
          const errMsg = globalMsg || "导入存在失败或异常信息";
          if (!msg.failed.has(errMsg)) {
            msg.failed.set(errMsg, []);
          }
          msg.failed.get(errMsg).push(lang);
        } else {
          // 真正的错误
          // console.error(`${lang}导入失败：`, error);
          const errMsg = error?.message || error?.data?.message || "未知错误";
          if (!msg.failed.has(errMsg)) {
            msg.failed.set(errMsg, []);
          }
          msg.failed.get(errMsg).push(lang);
        }
      }
      console.log(lang, 'msg', msg);
    }

    // 返回结果
    if (hasCode201) {
      // 有 code=201 的响应，返回失败信息
      returnMsg = {
        code: 201,
        success: msg.success,
        failed: msg.failed,
        failedEntryInfos: allFailedEntryInfos,
        exceptionVos: allExceptionVos,
        globalMessage: globalMessage,
      };
    } else if (msg.success.length > 0 && msg.failed.size === 0) {
      // 完全成功
      returnMsg = {
        code: 200,
        success: msg.success,
        failed: msg.failed,
        failedEntryInfos: [],
        exceptionVos: [],
        globalMessage: "",
      };
    } else if (msg.success.length === 0 && msg.failed.size === 0) {
      // 空语言列表，视为成功
      returnMsg = {
        code: 200,
        success: [],
        failed: msg.failed,
        failedEntryInfos: [],
        exceptionVos: [],
        globalMessage: "",
      };
    } else {
      // 有失败但没有 code=201（可能是其他错误）
      returnMsg = {
        code: 201,
        success: msg.success,
        failed: msg.failed,
        failedEntryInfos: allFailedEntryInfos,
        exceptionVos: allExceptionVos,
        globalMessage: globalMessage || "导入存在失败",
      };
    }
  } catch (error) {
    console.error("entryBatchImportExcel 发生异常：", error);
    returnMsg = {
      code: 201,
      success: [],
      failed: new Map(),
      failedEntryInfos: [],
      exceptionVos: [],
      globalMessage: error.message || "未知错误",
    };
  }
  console.log('返回结果', hasCode201, returnMsg);
  return returnMsg;
}

/**
 * 批量导入 Excel 词条
 * @param {Array<Object>} translateTypes - 需要导入的翻译语种列表，每个对象包含 name 和 value 字段
 * @param {FormData} formData - Excel 文件等表单数据
 * @returns {Promise<Object>} 返回结果对象，包含：
 *   - code: 200 表示完全成功，201 表示有失败信息，
 *   - success: 成功语种列表，
 *   - failed: 失败语种对象，key 为语种，value 为 {globalMessage, exceptionVos, failedEntryInfos}
 */
export async function entryBatchImportExcel_v2_5(translateTypeObj, formData) {
  // 在函数作用域定义变量，确保在 try-catch 中都可以访问
  let hasCode201 = false;
  let returnMsg = {};

  try {
    console.log('参数', translateTypeObj, formData);
    const msg = { success: [], failed: {} };
    hasCode201 = false;

    // 每种翻译语种的导入
    for (const lang of translateTypeObj) {
      const params = {
        transType: lang.value,
      };
      try {
        await entryImportExcle(params, formData);
        msg.success.push(lang.name);
      } catch (error) {
        console.log(`${lang.name}导入响应：`, error);
        const errorData = error?.response?.data || error?.data || error;
        const { code, data } = errorData || {};
        const failedInfos = data?.failedEntryInfos || [];
        const exceptionVos = data?.exceptionVos || data?.exceptionVOs || [];
        const globalMsg = data?.globalMessage || error?.message || error?.data?.message || "导入存在失败或异常信息";
        // 按语种组织错误信息
        const failedInfo = {
          globalMessage: globalMsg,
          exceptionVos: exceptionVos,
          failedEntryInfos: failedInfos,
          code: code,
        };
        msg.failed[lang.name] = failedInfo;
        if (code === 201) {
          hasCode201 = true;
        }
      }
      console.log(lang.name, 'msg', msg);
    }

    // 返回结果
    if (hasCode201) {
      // 有 code=201 的响应，返回失败信息
      returnMsg = {
        code: 201,
        success: msg.success,
        failed: msg.failed,
      };
    } else if (msg.success.length > 0 && Object.keys(msg.failed).length === 0) {
      // 完全成功
      returnMsg = {
        code: 200,
        success: msg.success,
        failed: {},
      };
    } else if (msg.success.length === 0 && Object.keys(msg.failed).length === 0) {
      // 空语言列表，视为成功
      returnMsg = {
        code: 200,
        success: [],
        failed: {},
      };
    } else {
      // 有失败但没有 code=201（可能是其他错误）
      returnMsg = {
        code: 201,
        success: msg.success,
        failed: msg.failed,
      };
    }
  } catch (error) {
    console.error("entryBatchImportExcel 发生异常：", error);
    returnMsg = {
      code: 201,
      success: [],
      failed: {},
    };
  }
  console.log('返回结果', hasCode201, returnMsg);
  return returnMsg;
}

/**
 * 批量导入 Excel 词条
 * @param {Array<string>} translateTypes - 需要导入的翻译语种列表
 * @param {FormData} formData - Excel 文件等表单数据
 * @returns {Promise<Object>} 返回结果对象，包含：
 *   - code: 200 表示完全成功，201 表示有失败信息
 *   - success: 成功语种列表
 *   - failed: 失败语种 Map
 *   - failedEntryInfos: 可重试失败词条数组
 *   - exceptionVos: 异常信息数组
 *   - globalMessage: 总体错误提示信息
 */
export async function entryBatchImportExcel_V1_5(translateTypes, formData) {
  try {
    console.log('参数', translateTypes, formData);
    const success = [];
    const msgBylang = [];
    let catchError = false;

    // 每种翻译语种的导入
    for (const lang of translateTypes) {
      const params = {
        transType: lang,
      };
      try {
        // console.log('更新V1.5 参数', params, formData);

        const res = await entryImportExcle(params, formData);
        success.push(lang);
        msgBylang.push({ lang: lang, code: res.code, ...res.data });
      } catch (error) {
        catchError = true;
        const res = error?.data || error?.response?.data || error;
        console.log("错误信息: error", error, "res", res, "res.data", res.data);
        msgBylang.push({ lang: lang, code: res.code, ...res.data });
      }
    }

    // 返回结果
    return {
      code: catchError ? 201 : 200,
      success: success,
      msgBylang: msgBylang,
    };
  } catch (error) {
    console.error("entryBatchImportExcel 发生异常：", error);
    notification.error({
      message: "导入失败",
      description: error.message || "未知错误",
    });
    return {
      code: 201,
      success: [],
      msgBylang: [],
    };
  }
}

/**
 * 批量导入 Excel 词条 (v2版本 - 新API)
 * @param {File} dedupExcel - 去重后Excel文件
 * @param {File|null} mappingJson - 映射JSON文件（可选）
 * @param {Array<string>} backfillFields - 回填字段列表（字段label数组）
 * @param {Object} options - 全局配置（如{emptyStringAsValue: true}）
 * @returns {Promise<Object>} 返回结果对象，结构可能与旧版本不同
 * @note 更新接口不需要去重前Excel，后端只需要去重后Excel和映射文件就能回填成去重前Excel
 */
export async function entryBatchImportExcel_v2(dedupExcel, mappingJson, backfillFields, options = {}) {
  try {
    console.log('entryBatchImportExcel_v2 参数', { dedupExcel, mappingJson, backfillFields, options });

    // 参数校验
    if (options.emptyStringAsValue === undefined) {
      const errorMsg = "请明确指定是否导入空值！";
      notification.error({
        message: "参数错误",
        description: errorMsg,
      });
      throw new Error(errorMsg);
    }

    if (!backfillFields || !Array.isArray(backfillFields) || backfillFields.length === 0) {
      const errorMsg = "请至少选择一个回填字段！";
      notification.error({
        message: "参数错误",
        description: errorMsg,
      });
      throw new Error(errorMsg);
    }

    // 构建FormData
    const formData = new FormData();
    // 注意：更新接口不需要dedupOriginExcel，后端只需要dedupUpdateExcel和mappingJson就能回填
    formData.append("dedupExcel", dedupExcel);
    if (mappingJson) {
      formData.append("mappingJson", mappingJson);
    }

    // 构建payload JSON（只包含backfillFields任务，不包含checkFields等校验任务）
    const payload = {
      options: {
        emptyStringAsValue: options.emptyStringAsValue,
      },
      rules: [
        {
          taskType: "backfillFields",
          params: {
            backfillFields: backfillFields
          }
        }
      ]
    };
    formData.append("payload", JSON.stringify(payload));

    // 调用API
    // params: {} - 无URL query参数
    // data: formData - FormData包含所有multipart数据（文件+payload）
    const response = await entryImportExcle_v2({}, formData);

    // 返回完整的API响应（包含 code, message 等字段）
    return response;
  } catch (error) {
    console.error("entryBatchImportExcel_v2 发生异常：", error);
    // 尝试从error中提取响应数据
    const errorData = error?.response?.data || error?.data || error;
    throw errorData;
  }
}

/**
 * 校验词条 (v2版本 - 新API)
 * @param {File} dedupOriginExcel - 去重后送翻前Excel文件
 * @param {File} dedupExcel - 去重后Excel文件
 * @param {File|null} mappingJson - 映射JSON文件（可选）
 * @param {Array<string>} checkFields - 校验字段列表（如["entry", "comment", "tag"]）
 * @param {Array<string>} backfillFields - 回填字段列表（字段label数组）
 * @param {Object} options - 全局配置和可选校验任务
 *   - emptyStringAsValue: boolean - 是否将空字符串视为有效值
 *   - failFast: boolean - 是否在首次FATAL错误时立即终止
 * 
 *   - checkSpecialChar: boolean - 是否启用特殊字符校验（可选）
 *   - checkMaxLength: boolean - 是否启用长度校验（可选）
 * @returns {Promise<Object>} 返回校验结果对象，包含 {success, canBackfill, summary, issues, preview, attachments}
 */
export async function entryValidate_v2(dedupOriginExcel, dedupUpdateExcel, mappingJson, checkFields, backfillFields, options = {}) {
  try {
    console.log('entryValidate_v2 参数', { dedupOriginExcel, dedupUpdateExcel, mappingJson, checkFields, backfillFields, options });

    // 构建FormData
    const formData = new FormData();
    formData.append("dedupOriginExcel", dedupOriginExcel);
    formData.append("dedupUpdateExcel", dedupUpdateExcel);
    if (mappingJson) {
      formData.append("mappingJson", mappingJson);
    }

    // 构建options数组（全局配置）
    const options_ = {
      emptyStringAsValue: options.emptyStringAsValue !== undefined ? options.emptyStringAsValue : true,
      failFast: options.failFast !== undefined ? options.failFast : false,
    };

    // 构建rules数组（校验规则）
    const rules_ = [
      {
        taskType: "checkFields",// 校验任务1（校验 Excel 与数据库中核心字段的一致性，用于防止错批次、错表或人工篡改，例如：id与词条匹配）
        params: {
          checkFields: checkFields || []
        }
      },
      {
        taskType: "filterIdMap",// 校验任务2（去重后的翻译与映射文件的id映射是否错误）
        params: {}
      },
      {
        taskType: "IdMatch",// 校验任务3（送翻后的翻译文件中，存在异常id，不在送翻前的翻译文件中）
        params: {}
      },
      {
        taskType: "backfillFields",// 校验任务4（校验 Excel 中是否存在指定用于回填更新的业务字段列）
        params: {
          backfillFields: backfillFields || []
        }
      }
    ];

    // 添加可选的校验任务
    if (options.translateAttributes) {
      if (options.checkSpecialChar) {
        rules_.push({
          taskType: "checkSpecialChar",
          params: {
            translateAttributes: options.translateAttributes,
          }
        });
      }
      if (options.checkMaxLength) {
        rules_.push({
          taskType: "checkMaxLength",
          params: {
            translateAttributes: options.translateAttributes,
          }
        });
      }
    }

    // 构建payload JSON
    const payload = {
      options: options_,
      rules: rules_
    };
    formData.append("payload", JSON.stringify(payload));

    // console.log('校验 参数', {}, formData, payload);
    const response = await entryValidateApi_v2({}, formData);

    // 直接返回API响应
    return response.data || response;
  } catch (error) {
    console.error("entryValidate_v2 发生异常：", error);
    // 尝试从error中提取响应数据
    const errorData = error?.response?.data || error?.data || error;
    throw errorData;
  }
}

/**
 * 根据 accept 字符串（如 ".csv"）从类型列表反查 value（如 "csv"），供文件类型选择器等复用。
 * @param {string} accept - accept 字符串，如 ".csv"、".xls,.xlsx"
 * @param {Array<{value: string, accept: string}>} types - 类型列表，每项含 value、accept
 * @returns {string|null} 匹配的 value，未匹配则 null
 */
export function resolveImportTypeFromAccept(accept, types) {
  if (!accept || typeof accept !== 'string') return null;
  if (!Array.isArray(types)) return null;
  const t = types.find((type) => type && type.accept === accept.trim());
  return t ? t.value : null;
}

/**
 * 统一的词条更新入口函数
 * 根据选择的字段自动判断调用翻译更新接口还是通用更新接口
 * @param {File} file - Excel/CSV文件
 * @param {Array<string>} selectedFields - 选中的更新字段列表（可能是name或value）
 * @param {FormData|null} formData - 可选的FormData（如果已构建好），否则函数内部会构建
 * @param {Object} options - 可选配置
 *   - mappingJson: File|null - 映射JSON文件（可选）
 * @returns {Promise<Object>} 返回API响应结果
 * @example
 * // 只选择翻译字段，会调用翻译更新接口
 * await updateEntryInfosUnified(file, ['english', 'russian'], null);
 * 
 * // 选择翻译字段+其他字段，会调用通用更新接口
 * await updateEntryInfosUnified(file, ['english', 'tag', 'maxLength'], null);
 */
export async function updateEntryInfosUnified(file, selectedFields, formData = null, options = {}) {
  try {
    console.log('updateEntryInfosUnified 参数', { file, selectedFields, formData, options });

    // 参数校验
    if (!file) {
      const errorMsg = "请选择文件！";
      notification.error({
        message: "参数错误",
        description: errorMsg,
      });
      throw new Error(errorMsg);
    }

    if (!selectedFields || !Array.isArray(selectedFields) || selectedFields.length === 0) {
      const errorMsg = "请至少选择一个更新字段！";
      notification.error({
        message: "参数错误",
        description: errorMsg,
      });
      throw new Error(errorMsg);
    }

    // 判断是否全是翻译字段
    const isAllTranslations = isAllTranslationFields(selectedFields, commonParam);

    if (isAllTranslations) {
      // 使用翻译更新接口（entryImportExcle）
      // 需要将selectedFields转换为语种object列表（可能是name或value，需要统一处理）
      const translateTypeObj = commonParam?.languageList.filter(lang => selectedFields.includes(lang.value) || selectedFields.includes(lang.name)) || [];

      // 构建FormData
      const finalFormData = formData || new FormData();
      if (!formData) {
        finalFormData.append("file", file);
        if (options.mappingJson) {
          finalFormData.append("relationFile", options.mappingJson);
        }
      }

      // 调用翻译更新接口（按语种循环调用）
      return await entryBatchImportExcel_v2_5(translateTypeObj, finalFormData);
    } else {
      // 使用通用更新接口（updateEntryInfosByFile）
      // 构建FormData
      const finalFormData = formData || new FormData();
      if (!formData) {
        finalFormData.append("file", file);
        if (options.mappingJson) {
          finalFormData.append("relationFile", options.mappingJson);
        }
      }

      // 构建params，columnName[]需要作为数组传递
      // axios会自动将数组参数转换为columnName[]=value1&columnName[]=value2格式
      const params = {
        columnName: selectedFields // 直接传递数组，axios会自动处理
      };

      // 调用通用更新接口
      const response = await updateEntryInfosByFile(params, finalFormData);
      response.data["code"] = response.code;
      const returnMsg = {
        code: response.code || 201,
        success: response.code == 200 ? ["全部"] : [],
        failed: response.code == 200 ? {} : response.data,
      };
      console.log("returnMsg", returnMsg)
      return returnMsg;
    }
  } catch (error) {
    console.error("updateEntryInfosUnified 发生异常：", error);
    // 尝试从error中提取响应数据
    const errorData = error?.response?.data || error?.data || error;
    throw errorData;
  }
}
