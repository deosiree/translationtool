/**
 * DOM/UI工具函数
 * 包含DOM操作、UI交互等功能
 */

/**
 * 创建拖拽模态框指令的工厂函数
 * 用于在 Vue 应用入口处通过 app.directive('drag-modal', createDragModalDirective()) 注册
 */
export function createDragModalDirective() {
  return (el, _binding) => {
    // 如果要给其他UI框架中添加modal拖拽事件 修改此处即可
    const dialogHeaderEl = el.querySelector(".modalHeader");
    const dragDom = el.querySelector(".ant-modal");

    if (!dialogHeaderEl || !dragDom) return;

    dialogHeaderEl.style.cursor = "move";
    // 获取原有属性 ie dom元素.currentStyle 火狐谷歌 window.getComputedStyle(dom元素, null);
    const sty = dragDom.currentStyle || window.getComputedStyle(dragDom, null);
    dialogHeaderEl.onmousedown = (e) => {
      // 鼠标按下，计算当前元素距离可视区的距离
      const disX = e.clientX - dialogHeaderEl.offsetLeft;
      const disY = e.clientY - dialogHeaderEl.offsetTop;
      // 获取到的值带px 正则匹配替换
      let styL, styT;
      // 注意在ie中 第一次获取到的值为组件自带50% 移动之后赋值为px
      if (sty.left.includes("%")) {
        styL =
          +document.body.clientWidth * (+sty.left.replace(/%/g, "") / 100);
        styT =
          +document.body.clientHeight * (+sty.top.replace(/%/g, "") / 100);
      } else {
        styL = +sty.left.replace(/\px/g, "");
        styT = +sty.top.replace(/\px/g, "");
      }

      document.onmousemove = function (e) {
        // 通过事件委托，计算移动的距离
        const l = e.clientX - disX;
        const t = e.clientY - disY;
        // 移动当前元素
        dragDom.style.left = `${l + styL}px`;
        dragDom.style.top = `${t + styT}px`;
        // 将此时的位置传出去
        // binding.value({x:e.pageX,y:e.pageY})
      };
      document.onmouseup = function () {
        document.onmousemove = null;
        document.onmouseup = null;
      };
    };
  };
}

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
 * 通用 DOM 事件阻止工具
 * 通常在「只想执行自定义逻辑，而不希望触发浏览器默认行为或父级事件」时使用。
 *
 * 例如：
 * - 阻止点击触发隐藏的 input[type=file]，避免弹出系统文件选择框
 * - 阻止点击事件继续冒泡到父元素上
 *
 * @param {Event} event - 原生 DOM 事件对象
 * @param {Object} [options]
 * @param {boolean} [options.preventDefault=true] - 是否调用 event.preventDefault() 阻止默认行为
 * @param {boolean} [options.stopPropagation=true] - 是否调用 event.stopPropagation() 阻止事件冒泡
 */
export function stopDomEvent(event, options = {}) {
  const { preventDefault = true, stopPropagation = true } = options;

  if (!event) return;

  if (preventDefault && typeof event.preventDefault === "function") {
    event.preventDefault();
  }

  if (stopPropagation && typeof event.stopPropagation === "function") {
    event.stopPropagation();
  }
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
