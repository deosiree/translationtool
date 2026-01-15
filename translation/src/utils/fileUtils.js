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
