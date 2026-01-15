import { notification } from "ant-design-vue";
import { entryImportExcle } from "@/http/api/entryManage";

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
export async function entryBatchImportExcel(translateTypes, formData) {
  try {
    console.log('参数', translateTypes, formData);
    const msg = { success: [], failed: new Map() };
    let allFailedEntryInfos = [];
    let allExceptionVos = [];
    let globalMessage = "";
    let hasCode201 = false;

    // 每种翻译语种的导入
    for (const lang of translateTypes) {
      const params = {
        transType: lang,
      };
      try {
        await entryImportExcle(params, formData);
        msg.success.push(lang);
      } catch (error) {
        // 响应拦截器会将 code !== 200 && code !== 205 的响应 reject
        // 需要从 error.response 或 error.data 中提取数据
        console.log(`${lang}导入响应：`, error);
        
        // 尝试从 error.response.data 或 error.data 中提取数据
        const errorData = error?.response?.data || error?.data || error;
        const { code, data } = errorData || {};
        
        if (code === 201) {
          // code=201 表示有失败信息，这是正常的业务响应
          hasCode201 = true;
          const failedInfos = data?.failedEntryInfos || [];
          const exceptionVos = data?.exceptionVos || data?.exceptionVOs || [];
          const globalMsg = data?.globalMessage || "";
          
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
          console.error(`${lang}导入失败：`, error);
          const errMsg = error?.message || error?.data?.message || "未知错误";
          if (!msg.failed.has(errMsg)) {
            msg.failed.set(errMsg, []);
          }
          msg.failed.get(errMsg).push(lang);
        }
      }
    }

    // 返回结果
    if (hasCode201) {
      // 有 code=201 的响应，返回失败信息
      return {
        code: 201,
        success: msg.success,
        failed: msg.failed,
        failedEntryInfos: allFailedEntryInfos,
        exceptionVos: allExceptionVos,
        globalMessage: globalMessage,
      };
    } else if (msg.success.length > 0 && msg.failed.size === 0) {
      // 完全成功
      if (msg.success.length > 0) {
        notification.success({
          message: "导入成功！",
          description: msg.success.join(", ") + "导入成功！",
          duration: 0,
        });
      }
      return {
        code: 200,
        success: msg.success,
        failed: msg.failed,
        failedEntryInfos: [],
        exceptionVos: [],
        globalMessage: "",
      };
    } else if (msg.success.length === 0 && msg.failed.size === 0) {
      // 空语言列表，视为成功
      return {
        code: 200,
        success: [],
        failed: msg.failed,
        failedEntryInfos: [],
        exceptionVos: [],
        globalMessage: "",
      };
    } else {
      // 有失败但没有 code=201（可能是其他错误）
      if (msg.failed.size > 0) {
        notification.error({
          message: "导入失败！",
          description: formatMapToString(msg.failed),
          duration: 0,
        });
      }
      return {
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
    notification.error({
      message: "导入过程发生异常！",
      description: error.message || "未知错误",
      duration: 0,
    });
    return {
      code: 201,
      success: [],
      failed: new Map(),
      failedEntryInfos: [],
      exceptionVos: [],
      globalMessage: error.message || "未知错误",
    };
  }
}

