/**
 * Notification工具函数
 * 包含关闭所有notification等功能
 */

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
