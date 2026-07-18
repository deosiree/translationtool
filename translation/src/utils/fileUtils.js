import { message } from "ant-design-vue";
import { getCurrentStringTime } from "./dateUtils";

/**
 * 下载 JSON 文件
 * @param {Object|Array} data - 要下载的数据（对象或数组）
 * @param {string} fileName - 文件名（不含扩展名），默认为 'data'
 * @param {boolean} choosePath - 是否使用 showSaveFilePicker 让用户选择保存路径，默认为 false
 * @returns {Promise<void>}
 */
export async function downloadJsonFile(data, fileName = "data", choosePath = false) {
  try {
    // 将数据转换为格式化的 JSON 字符串
    const jsonString = JSON.stringify(data, null, 2);
    
    // 创建 Blob 对象
    const blob = new Blob([jsonString], { type: "application/json;charset=utf-8" });
    
    // 生成默认文件名（带时间戳）
    const time = getCurrentStringTime();
    const defaultFileName = `${fileName}_${time}.json`;
    
    if (choosePath && "showSaveFilePicker" in window) {
      // 使用 File System Access API（现代浏览器）
      try {
        const fileHandle = await window.showSaveFilePicker({
          suggestedName: defaultFileName,
          types: [
            {
              description: "JSON 文件",
              accept: {
                "application/json": [".json"],
              },
            },
          ],
        });
        
        const writable = await fileHandle.createWritable();
        await writable.write(blob);
        await writable.close();
      } catch (error) {
        if (error.name === "AbortError") {
          // 用户取消了文件保存操作
          console.log("用户取消了文件保存操作");
          return;
        }
        throw error;
      }
    } else {
      // 使用传统的下载方式（兼容旧浏览器）
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = defaultFileName;
      link.style.display = "none";
      document.body.appendChild(link);
      link.click();
      // 清理
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    }
  } catch (error) {
    console.error("下载 JSON 文件失败：", error);
    message.error(`下载 JSON 文件失败: ${error.message || error}`);
    throw error;
  }
}

/**
 * 通用文件上传处理函数
 * 用于处理 Ant Design Vue Upload 组件的文件上传事件
 * @param {Object} formModel - 表单模型对象（Vue 响应式对象）
 * @param {Object} info - Upload 组件的 change 事件信息对象
 *   - info.fileList: 文件列表数组
 *   - info.file: 当前文件对象
 * @param {string} fileKey - 表单模型中存储单个文件的键名
 * @param {string} fileListKey - 表单模型中存储文件列表的键名
 * @returns {void}
 * @example
 * // 在组件中使用
 * import { handleFileUpload } from "@/utils/fileUtils";
 * 
 * handleIdMappingUpload(info) {
 *   handleFileUpload(this.formModel, info, "relationFile", "relationFileList");
 * }
 */
export function handleFileUpload(formModel, info, fileKey, fileListKey) {
  formModel[fileListKey] = info.fileList;
  if (info.fileList.length === 0) {
    formModel[fileKey] = null;
  } else {
    formModel[fileKey] = info.file;
  }
}

/**
 * 通用文件移除处理函数
 * 用于移除表单模型中的文件和文件列表
 * @param {Object} formModel - 表单模型对象（Vue 响应式对象）
 * @param {string} fileKey - 表单模型中存储单个文件的键名
 * @param {string} fileListKey - 表单模型中存储文件列表的键名
 * @returns {boolean} 始终返回 true，用于 Upload 组件的 remove 事件处理
 * @example
 * // 在组件中使用
 * import { removeFile } from "@/utils/fileUtils";
 * 
 * removeIdMappingFile() {
 *   return removeFile(this.formModel, "relationFile", "relationFileList");
 * }
 */
export function removeFile(formModel, fileKey, fileListKey) {
  formModel[fileKey] = null;
  formModel[fileListKey] = [];
  return true;
}

/**
 * 按扩展名推断 showSaveFilePicker 的 accept types
 * @param {string} fileName
 * @param {string} contentType
 * @returns {{ description: string, accept: Record<string, string[]> }[]}
 */
function savePickerTypesForFile(fileName, contentType) {
  const lower = String(fileName || "").toLowerCase();
  if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
    return [
      {
        description: "Excel 文件",
        accept: {
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": [
            ".xlsx",
          ],
          "application/vnd.ms-excel": [".xls"],
        },
      },
    ];
  }
  if (lower.endsWith(".json") || String(contentType).includes("json")) {
    return [
      {
        description: "JSON 文件",
        accept: { "application/json": [".json"] },
      },
    ];
  }
  return [
    {
      description: "文件",
      accept: { [contentType || "application/octet-stream"]: [".*"] },
    },
  ];
}

/**
 * 传统 a[download] 落盘（工作台模板下载 / 回填异常 JSON 等同款）。
 * 异步请求后仍可用；不依赖用户手势令牌。
 * @param {Blob} blob
 * @param {string} finalFileName
 * @returns {void}
 */
function triggerAnchorDownload(blob, finalFileName) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = finalFileName;
  link.style.display = "none";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  // 延迟 revoke，避免 Electron/Chromium 尚未开始读流就失效
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/**
 * 是否为「非用户手势」导致的 File Picker 拒绝（应回退 a[download]）
 * @param {unknown} error
 * @returns {boolean}
 */
function isSavePickerGestureError(error) {
  const name = error?.name || "";
  const msg = String(error?.message || "");
  return (
    name === "SecurityError" ||
    name === "NotAllowedError" ||
    /user gesture/i.test(msg) ||
    /showSaveFilePicker/i.test(msg)
  );
}

/**
 * 处理 blob 响应并触发下载；若服务端实际返回 JSON 错误体（如 code=203），提示并不落盘。
 * 默认与 downloadJsonFile / 工作台模板下载一致：走 a[download]。
 * choosePath=true 时尝试另存为；用户取消返回 false；手势失效则回退默认下载。
 * @param {Object} response - axios 响应对象（responseType: 'blob'）
 * @param {string|null} [fileName=null] - 文件名（可选，从响应头提取）
 * @param {boolean} [choosePath=false] - 是否尝试弹出另存为（须仍在用户手势链内）
 * @returns {Promise<boolean>} true=已保存/已触发下载；false=用户取消
 */
export async function downloadBlobResponse(
  response,
  fileName = null,
  choosePath = false,
) {
  try {
    const headerCt = String(
      response?.headers?.["content-type"] ||
        response?.headers?.["Content-Type"] ||
        "",
    ).toLowerCase();
    let payload = response?.data;

    // 仅在 content-type 含 json，或小体积且以 { 开头时探测错误体（避免误伤 xlsx）
    if (typeof Blob !== "undefined" && payload instanceof Blob) {
      const blobCt = String(payload.type || headerCt).toLowerCase();
      const maybeJson =
        blobCt.includes("json") ||
        (payload.size <= 8192 && blobCt.includes("octet-stream"));
      if (maybeJson || blobCt.includes("json")) {
        const head = await payload.slice(0, 96).text();
        if (head.trim().startsWith("{") || blobCt.includes("json")) {
          const text = payload.size <= 8192 ? await payload.text() : head;
          try {
            const errBody = JSON.parse(text);
            if (errBody && (errBody.code != null || errBody.message)) {
              const msg =
                errBody.message || `下载失败（code=${errBody.code}）`;
              message.error(msg);
              throw new Error(msg);
            }
          } catch (parseErr) {
            if (
              parseErr?.message &&
              !String(parseErr.message).includes("JSON")
            ) {
              throw parseErr;
            }
          }
        }
      }
    } else if (typeof payload === "string" && payload.trim().startsWith("{")) {
      try {
        const errBody = JSON.parse(payload);
        if (errBody && (errBody.code != null || errBody.message)) {
          const msg = errBody.message || `下载失败（code=${errBody.code}）`;
          message.error(msg);
          throw new Error(msg);
        }
      } catch (parseErr) {
        if (parseErr?.message && !String(parseErr.message).includes("JSON")) {
          throw parseErr;
        }
      }
    }

    let finalFileName = fileName;
    if (!finalFileName) {
      const contentDisposition = response.headers["content-disposition"];
      if (contentDisposition) {
        const fileNameMatch = contentDisposition.match(
          /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/,
        );
        if (fileNameMatch && fileNameMatch[1]) {
          finalFileName = decodeURIComponent(
            fileNameMatch[1].replace(/['"]/g, ""),
          );
        }
      }
    }

    if (!finalFileName) {
      const time = getCurrentStringTime();
      finalFileName = `download_${time}`;
    }

    const contentType =
      response.headers["content-type"] || "application/octet-stream";
    const blob =
      typeof Blob !== "undefined" && payload instanceof Blob
        ? payload
        : new Blob([payload], { type: contentType });

    if (
      choosePath &&
      typeof window !== "undefined" &&
      "showSaveFilePicker" in window
    ) {
      try {
        const fileHandle = await window.showSaveFilePicker({
          suggestedName: finalFileName,
          types: savePickerTypesForFile(finalFileName, contentType),
        });
        const writable = await fileHandle.createWritable();
        await writable.write(blob);
        await writable.close();
        return true;
      } catch (error) {
        if (error?.name === "AbortError") {
          return false;
        }
        // 异步请求后手势失效：回退默认下载，不把「注意事项生成模板」等链路打挂
        if (!isSavePickerGestureError(error)) {
          throw error;
        }
      }
    }

    triggerAnchorDownload(blob, finalFileName);
    return true;
  } catch (error) {
    console.error("处理下载响应失败：", error);
    if (!error?.message || !String(error.message).includes("下载")) {
      message.error(`下载文件失败: ${error.message || error}`);
    }
    throw error;
  }
}
