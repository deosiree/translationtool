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
 */
export async function entryBatchImportExcel(translateTypes, formData) {
  try {
    console.log("参数", translateTypes, formData);
    const msg = { success: [], failed: new Map() };

    // 每种翻译语种的导入
    for (const lang of translateTypes) {
      const params = {
        transType: lang,
      };
      try {
        await entryImportExcle(params, formData);
        msg.success.push(lang);
      } catch (error) {
        console.log(`${lang}导入失败原因`, error);
        const errMsg = error?.message || error?.data?.message || "未知错误";
        if (!msg.failed.has(errMsg)) {
          msg.failed.set(errMsg, []);
        }
        msg.failed.get(errMsg).push(lang);
        // 处理错误数据
        // 后续：
        //      后端增加属性错误词条list，错误原因list
        //      前端生成对应语言的错误词条文件，包含错误词条_语言.csv与错误原因_语言.json
      }
    }

    if (msg.success.length > 0) {
      notification.success({
        message: "导入成功！",
        description: msg.success.join(", ") + "导入成功！",
        duration: 0,
      });
    }
    if (msg.failed.size > 0) {
      notification.error({
        message: "导入失败！",
        description: formatMapToString(msg.failed),
        duration: 0,
      });
      return { error: "有失败的导入，提供失败词条的相关文件下载" };
    }
    return { success: "数据为空" };
  } catch (error) {
    console.error("entryBatchImportExcel 发生异常：", error);
    notification.error({
      message: "导入过程发生异常！",
      description: error.message || "未知错误",
      duration: 0,
    });
    return { error: error.message || "未知错误" };
  }
}

