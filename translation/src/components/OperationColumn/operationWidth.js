/**
 * @file 操作列宽度子系统：逐字估宽、行内/更多切分。
 * @module OperationColumn/operationWidth
 */

/** OpItem 内 icon 槽宽度（px） */
export const OP_ICON_W = 14;

/** OpItem 内 icon 与文案间距（px） */
export const OP_ICON_M = 4;

/** 固定右列与表体纵向滚动条间距（px） */
export const RIGHT_GUT = 8;

/** 与 OpItem 根节点 min-width 对齐（px） */
export const OP_MIN_W = 32;

/** 离屏探针最多渲染的代表行数 */
export const MAX_PROBE_N = 24;

/** 与 label font-size:12px 对齐；CJK / 全角字符横向占位（px） */
const CJK_CHAR_PX = 12;

/** 拉丁字母、数字、半角符号横向占位（px） */
const LATIN_CHAR_PX = 7;

/** 「更多」槽最小宽，对齐 .operation-column-more-trigger min-width */
const MORE_SLOT_MIN = 28;

const BTN_H_PADDING = 12;
const ICON_LABEL_GAP = 4;
const ICON_UNIT = OP_ICON_W + OP_ICON_M + ICON_LABEL_GAP;
const ROW_BUF = 4;

/**
 * 是否为 CJK / 全角字符。
 * @param {string} ch
 * @returns {boolean}
 */
export function isCjkFull(ch) {
  const cp = ch.codePointAt(0);
  if (cp === undefined) return false;
  if (cp <= 0xff) return false;
  if (cp >= 0x4e00 && cp <= 0x9fff) return true;
  if (cp >= 0x3400 && cp <= 0x4dbf) return true;
  if (cp >= 0xff00 && cp <= 0xffef) return true;
  if (cp >= 0x3000 && cp <= 0x303f) return true;
  return false;
}

/**
 * 按 CJK / 拉丁分档累加文案像素宽。
 * @param {string} text
 * @returns {number}
 */
export function labelTextW(text) {
  let w = 0;
  for (const ch of text) {
    w += isCjkFull(ch) ? CJK_CHAR_PX : LATIN_CHAR_PX;
  }
  return w;
}

/**
 * 估算「更多」槽位宽度（px）。
 * @param {string} moreLabel
 * @returns {number}
 */
export function moreSlotW(moreLabel) {
  return Math.max(MORE_SLOT_MIN, labelTextW(moreLabel) + ICON_UNIT + BTN_H_PADDING);
}

/**
 * 单个 OpItem 内容区估宽。
 * @param {{ label: string, hasIcon?: boolean }} desc
 * @returns {number}
 */
export function opItemW(desc) {
  const textW = labelTextW(desc.label);
  const iconW = desc.hasIcon ? ICON_UNIT : 0;
  return Math.max(OP_MIN_W, textW + iconW + BTN_H_PADDING);
}

/**
 * 按行内条总槽位数（含「更多」）解析行内 OpItem 个数与是否显示「更多」。
 * @param {number} totalButtons - 当前场景可见 OpItem 总数 N
 * @param {number} displaySlotCount - 行内条槽位总数（含「更多」占 1 槽），小于 1 按 1 处理
 * @returns {{ inlineOpCount: number, showMore: boolean }}
 */
export function calcOpStrip(totalButtons, displaySlotCount) {
  const slots = Math.max(displaySlotCount, 1);
  if (totalButtons <= 0) {
    return { inlineOpCount: 0, showMore: false };
  }
  if (slots === 1) {
    return { inlineOpCount: 0, showMore: true };
  }
  if (totalButtons <= slots) {
    return { inlineOpCount: totalButtons, showMore: false };
  }
  const inlineOpCount = slots - 1;
  const overflowCount = totalButtons - inlineOpCount;
  if (overflowCount <= 1) {
    return { inlineOpCount: totalButtons, showMore: false };
  }
  return { inlineOpCount, showMore: true };
}

/**
 * 单场景操作条总宽度（px）。
 * @param {Array<{ label: string, hasIcon?: boolean }>} descs
 * @param {number} displaySlotCount
 * @param {number} gap
 * @param {string} moreLabel
 * @returns {number}
 */
export function stripSceneW(descs, displaySlotCount, gap, moreLabel) {
  if (descs.length === 0) return 0;

  const { inlineOpCount, showMore } = calcOpStrip(descs.length, displaySlotCount);

  if (showMore && inlineOpCount === 0) {
    return moreSlotW(moreLabel) + ROW_BUF;
  }

  const n = Math.min(inlineOpCount, descs.length);
  const inline = descs.slice(0, n);
  let w = 0;

  if (n >= 2) {
    w = inline.reduce((sum, d) => sum + opItemW(d), 0);
    w += Math.max(0, n - 1) * gap;
  } else if (n === 1) {
    w = opItemW(inline[0]);
  }

  if (showMore) {
    w += (n > 0 ? gap : 0) + moreSlotW(moreLabel);
  }

  return w + ROW_BUF;
}

/**
 * 由多组探针场景取列宽内容区上限。
 * @param {number} displaySlotCount
 * @param {number} gap
 * @param {string} moreLabel
 * @param {Array<Array<{ label: string, hasIcon?: boolean }>>} scenarios
 * @returns {number}
 */
export function maxFromSlots(displaySlotCount, gap, moreLabel, scenarios) {
  const seenConcats = new Set();
  let maxW = 0;

  for (const descs of scenarios) {
    if (descs.length === 0) continue;

    const { inlineOpCount } = calcOpStrip(descs.length, displaySlotCount);
    const n = Math.min(inlineOpCount, descs.length);

    if (n === 0) {
      const w = stripSceneW(descs, displaySlotCount, gap, moreLabel);
      if (w > maxW) maxW = w;
      continue;
    }

    const concat =
      n >= 2
        ? descs
            .slice(0, n)
            .map((d) => d.label)
            .join("\x1f")
        : descs
            .slice(0, n)
            .map((d) => d.label)
            .join("");
    if (seenConcats.has(concat)) continue;
    seenConcats.add(concat);

    const w = stripSceneW(descs, displaySlotCount, gap, moreLabel);
    if (w > maxW) maxW = w;
  }

  return maxW;
}

/**
 * 从 OpItem 根节点读取 data-op-*。
 * @param {HTMLElement} el
 * @returns {{ label: string, icon?: string, iconClass?: string, type: string }}
 */
export function readOpMeta(el) {
  return {
    label: el.dataset.opLabel ?? "",
    icon: el.dataset.opIcon || undefined,
    iconClass: el.dataset.opIconClass || undefined,
    type: el.dataset.opType || "primary",
  };
}

/**
 * 扫描容器内未隐藏的 OpItem。
 * @param {HTMLElement} rootEl
 * @returns {Array<{ label: string, hasIcon: boolean, iconWidth?: number, iconMg?: number }>}
 */
export function scanOpButtons(rootEl) {
  const items = rootEl.querySelectorAll(
    ".operation-column-op-item:not(.operation-column-op-item--hidden)"
  );
  return Array.from(items).map((item) => {
    const meta = readOpMeta(/** @type {HTMLElement} */ (item));
    const hasIcon = !!(meta.icon || meta.iconClass);
    return {
      label: meta.label,
      hasIcon,
      iconWidth: hasIcon ? OP_ICON_W : undefined,
      iconMg: hasIcon ? OP_ICON_M : undefined,
    };
  });
}

/**
 * 探针场景内按 label 去重。
 * @param {Array<{ label: string }>} descs
 * @returns {Array<{ label: string }>}
 */
export function dedupeByLbl(descs) {
  const seen = new Set();
  const out = [];
  for (const d of descs) {
    if (!d.label || seen.has(d.label)) continue;
    seen.add(d.label);
    out.push(d);
  }
  return out;
}
