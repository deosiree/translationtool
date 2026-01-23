/**
 * DOM/UI工具函数
 * 包含DOM操作、UI交互等功能
 */

/**
 * 从 localStorage 恢复悬浮组件位置：
 * - 坐标合法且在视口内：返回 { x, y }
 * - 坐标非法或越界：清除 localStorage，返回默认位置
 *
 * 该方法适用于所有基于窗口可视区域的悬浮组件位置恢复场景，
 * 例如全局工具栏按钮、浮动帮助按钮等。
 *
 * @param {string} storageKey localStorage 键名
 * @param {{x: number|null, y: number|null}} defaultPosition 组件的默认位置（例如 { x: null, y: null }）
 * @param {{width: number, height: number}} size 组件本身的宽高
 * @returns {{x: number|null, y: number|null}}
 */
export function normalizeFloatingPosition(storageKey, defaultPosition, size) {
  // SSR/测试环境下直接返回默认值
  if (typeof window === "undefined" || typeof window.localStorage === "undefined") {
    return defaultPosition;
  }

  const saved = window.localStorage.getItem(storageKey);
  if (!saved) {
    return defaultPosition;
  }

  try {
    const pos = JSON.parse(saved) || {};
    let x = Number(pos.x);
    let y = Number(pos.y);

    // 坐标不是有效数字：清除缓存并回默认
    if (!Number.isFinite(x) || !Number.isFinite(y)) {
      window.localStorage.removeItem(storageKey);
      return defaultPosition;
    }

    const buttonWidth = size && typeof size.width === "number" ? size.width : 0;
    const buttonHeight = size && typeof size.height === "number" ? size.height : 0;

    const maxX = Math.max(0, window.innerWidth - buttonWidth);
    const maxY = Math.max(0, window.innerHeight - buttonHeight);

    const outOfViewport = x < 0 || y < 0 || x > maxX || y > maxY;

    if (outOfViewport) {
      // 一旦越界：清掉 localStorage，下次回到默认位置
      window.localStorage.removeItem(storageKey);
      return defaultPosition;
    }

    // 在范围内再夹紧一次，避免边界精度问题
    x = Math.max(0, Math.min(x, maxX));
    y = Math.max(0, Math.min(y, maxY));

    return { x, y };
  } catch (e) {
    console.error("normalizeFloatingPosition error:", e);
    window.localStorage.removeItem(storageKey);
    return defaultPosition;
  }
}

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

/**
 * 创建通用可拖拽元素
 *
 * @param {HTMLElement} targetEl 需要拖拽的元素
 * @param {Object} [options]
 * @param {(pos: { x: number, y: number }) => void} [options.onDrag] 拖拽时的回调
 * @param {() => { minX?: number, maxX?: number, minY?: number, maxY?: number }} [options.getBounds] 返回拖拽边界
 * @returns {() => void} cleanup 函数，用于移除事件监听
 */
export function createDraggable(targetEl, options = {}) {
  if (!targetEl) {
    // 返回空清理函数，避免调用方还要判空
    return () => {};
  }

  const { onDrag, getBounds } = options;

  let isDragging = false;
  let startX = 0;
  let startY = 0;
  let originLeft = 0;
  let originTop = 0;

  const parsePx = (value) => {
    if (typeof value === "number") return value;
    if (typeof value === "string") {
      const num = parseFloat(value);
      return Number.isNaN(num) ? 0 : num;
    }
    return 0;
  };

  const applyBounds = (x, y) => {
    if (typeof getBounds !== "function") {
      return { x, y };
    }
    const bounds = getBounds() || {};
    const minX = typeof bounds.minX === "number" ? bounds.minX : -Infinity;
    const maxX = typeof bounds.maxX === "number" ? bounds.maxX : Infinity;
    const minY = typeof bounds.minY === "number" ? bounds.minY : -Infinity;
    const maxY = typeof bounds.maxY === "number" ? bounds.maxY : Infinity;

    return {
      x: Math.min(Math.max(x, minX), maxX),
      y: Math.min(Math.max(y, minY), maxY),
    };
  };

  const handleMouseMove = (e) => {
    if (!isDragging) return;
    const deltaX = e.clientX - startX;
    const deltaY = e.clientY - startY;

    let nextX = originLeft + deltaX;
    let nextY = originTop + deltaY;

    const bounded = applyBounds(nextX, nextY);
    nextX = bounded.x;
    nextY = bounded.y;

    targetEl.style.left = `${nextX}px`;
    targetEl.style.top = `${nextY}px`;

    if (typeof onDrag === "function") {
      onDrag({ x: nextX, y: nextY });
    }
  };

  const handleMouseUp = () => {
    if (!isDragging) return;
    isDragging = false;
    document.removeEventListener("mousemove", handleMouseMove);
    document.removeEventListener("mouseup", handleMouseUp);
  };

  const handleMouseDown = (e) => {
    // 只响应主键（左键）
    if (typeof e.button === "number" && e.button !== 0) return;

    isDragging = true;
    startX = e.clientX;
    startY = e.clientY;

    const style = window.getComputedStyle
      ? window.getComputedStyle(targetEl)
      : targetEl.style;

    originLeft = parsePx(style.left);
    originTop = parsePx(style.top);

    document.addEventListener("mousemove", handleMouseMove);
    document.addEventListener("mouseup", handleMouseUp);
  };

  targetEl.addEventListener("mousedown", handleMouseDown);

  // 返回清理函数
  return () => {
    targetEl.removeEventListener("mousedown", handleMouseDown);
    document.removeEventListener("mousemove", handleMouseMove);
    document.removeEventListener("mouseup", handleMouseUp);
  };
}
