/**
 * Notification工具函数
 * 包含关闭所有notification、错误处理等功能
 */
import { notification } from "ant-design-vue";
import { entryParams } from "@/constants/commonParam";

/**
 * 关闭所有当前显示的notification
 * 通过查找DOM中的.ant-notification元素并移除
 * 
 * 关键改进：只移除notice元素，保留容器
 * 原因：Ant Design Vue内部维护了容器引用，直接移除容器会导致内部状态与DOM不一致，
 * 从而无法创建新通知。保留容器后，Ant Design Vue可以正常在现有容器中添加新通知。
 */
export function closeAllNotifications() {
  // 只移除所有notice元素（单个通知项），保留容器
  // 这样Ant Design Vue的内部状态和DOM保持一致，可以正常创建新通知
  const noticeElements = document.querySelectorAll('.ant-notification-notice');
  noticeElements.forEach(notice => {
    if (notice && notice.parentNode) {
      notice.parentNode.removeChild(notice);
    } else if (notice) {
      notice.remove();
    }
  });
  
  // 注意：我们不移除容器本身，因为：
  // 1. Ant Design Vue内部可能维护了容器的引用
  // 2. 移除容器会导致内部状态与DOM不一致
  // 3. 保留空容器不会影响UI，且Ant Design Vue可以在其中正常添加新通知
  // 4. 如果容器为空，Ant Design Vue可能会自动清理，但我们不主动移除以避免破坏内部状态
}

/**
 * 通用错误通知处理函数
 * 用于统一处理异步操作中的错误，提取错误信息并显示通知
 * @param {Error} error - 错误对象，可能包含 response.data 或 data 属性
 * @param {string} message - 错误通知的标题，默认为 "操作失败"
 * @param {Object} options - 可选配置
 *   - duration: 通知显示时长（秒），0 表示不自动关闭，默认为 0
 *   - defaultDescription: 默认错误描述，当无法从错误中提取描述时使用，默认为 "未知错误"
 * @returns {void}
 * @example
 * // 在组件中使用
 * import { handleErrorNotification } from "@/utils/notificationUtils";
 * 
 * try {
 *   await someAsyncOperation();
 * } catch (error) {
 *   handleErrorNotification(error, "更新/回填过程发生异常！");
 * }
 */
export function handleErrorNotification(error, message = "操作失败", options = {}) {
  const { duration = 0, defaultDescription = "未知错误" } = options;
  const errorData = error?.response?.data || error?.data || error;
  const description = errorData?.message || error.message || defaultDescription;

  notification.error({
    message,
    description,
    duration,
  });
}

/**
 * 处理更新任务失败状态的通知
 * 根据任务状态码显示对应的错误通知
 * @param {string} status - 任务状态码（"2"/"3"/"4"/"5"/"6"）
 * @returns {void}
 * @example
 * // 在组件中使用
 * import { handleTaskFailureStatusNotification } from "@/utils/notificationUtils";
 * 
 * if (status === "3") {
 *   handleTaskFailureStatusNotification(status);
 * }
 */
export function handleTaskFailureStatusNotification(status, message = "操作失败") {
  const messageText = entryParams.updateEntry.taskStatusMessages[status] || "未知状态";

  notification.error({
    message: message,
    description: messageText,
    duration: 0,
  });
}
