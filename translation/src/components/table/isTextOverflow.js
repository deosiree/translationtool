/**
 * 判断元素内容是否在水平方向溢出（单行省略场景）
 * @param {HTMLElement|null|undefined} el
 * @returns {boolean}
 */
export function isTextOverflow(el) {
  if (!el) return false;
  return el.scrollWidth > el.clientWidth;
}
