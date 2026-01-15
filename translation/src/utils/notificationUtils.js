/**
 * Notification工具函数
 * 包含关闭所有notification等功能
 */

/**
 * 关闭所有当前显示的notification
 * 通过查找DOM中的.ant-notification元素并移除
 */
export function closeAllNotifications() {
  // 查找所有notification容器
  const notificationContainers = document.querySelectorAll('.ant-notification');
  
  notificationContainers.forEach(container => {
    // 移除整个notification容器
    if (container && container.parentNode) {
      container.parentNode.removeChild(container);
    } else if (container) {
      container.remove();
    }
  });
  
  // 同时查找并移除notification的notice元素
  const noticeElements = document.querySelectorAll('.ant-notification-notice');
  noticeElements.forEach(notice => {
    if (notice && notice.parentNode) {
      notice.parentNode.removeChild(notice);
    } else if (notice) {
      notice.remove();
    }
  });
}
