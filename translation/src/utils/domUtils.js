/**
 * DOM/UI工具函数
 * 包含DOM操作、UI交互等功能
 */

/**
 * 表单单元格的点击事件处理函数
 * @param {VueInstance} vm - Vue 实例
 * @param {Event} event - 点击事件对象
 */
export function clickInput(vm, event) {
  event.stopPropagation();
  // // 这里可以添加更多的交互逻辑，例如聚焦输入框、记录点击信息等
  // const inputElement = event.target;
  // inputElement.focus();
  // const inputName = inputElement.name;
  // console.log(`点击了输入框: ${inputName}`);
}

/**
 * 设置模态框的 aria-hidden 属性为 false
 * @param {Object} vm - Vue 实例，用于调用 $nextTick 方法
 * @param {Document} _document - 文档对象，用于获取 DOM 元素
 */
// 定义一个函数，接受 _document 作为参数
export function setModalAriaHidden(vm, _document) {
  // 等待 Vue 实例的 DOM 更新完成后执行回调函数
  vm.$nextTick(() => {
    // 通过传入的文档对象获取所有类名为 'ant-modal' 的 DOM 元素
    const domArr = _document.getElementsByClassName("ant-modal");
    // 检查是否存在类名为 'ant-modal' 的元素
    if (domArr && domArr.length > 0) {
      Array.from(domArr).forEach((item) => {
        // 检查当前 'ant-modal' 元素是否存在子节点
        if (item.childNodes && item.childNodes.length > 0) {
          Array.from(item.childNodes).forEach((child) => {
            // 检查子节点是否具有 setAttribute 方法
            if (child.setAttribute) {
              // 设置子节点的 aria-hidden 属性为 'false'，表示该元素可见
              child.setAttribute("aria-hidden", "false");
            }
          });
        }
      });
    }
  });
}
