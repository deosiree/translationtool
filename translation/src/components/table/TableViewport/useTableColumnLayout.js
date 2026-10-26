const DEFAULT_MIN_WIDTH = 50;
const DEFAULT_RESIZE_THRESHOLD = 3;
const RESIZE_CLICK_GUARD_MS = 250;

/**
 * 将数字列宽转换为 Ant Table 所需的 CSS 尺寸。
 * @param {number} width 列宽。
 * @returns {string} 带 px 单位的列宽。
 */
function toCssSize(width) {
  return `${Number(width) || 0}px`;
}
/**
 * 将解析后的固定宽度应用到嵌套列，并覆盖原尺寸回调。
 * @param {Array} columns 原始列定义。
 * @param {Object<string, number>} widths 按列标识索引的解析宽度。
 * @param {number} minWidth 默认最小宽度。
 * @param {Function} getColumnKey 获取列稳定标识的方法。
 * @returns {Array} 带固定尺寸回调的列定义。
 */
function applyFixedColumnWidths(columns, widths, minWidth, getColumnKey) {
  return (columns || []).map((column) => {
    if (column.children?.length) {
      return {
        ...column,
        children: applyFixedColumnWidths(
          column.children,
          widths,
          minWidth,
          getColumnKey
        ),
      };
    }

    const key = getColumnKey(column);
    const width = key == null ? undefined : widths[String(key)];
    if (width == null) return column;

    const fixedSize = toCssSize(width);
    const createCellSize = (original) => (...args) => {
      const cell = original?.(...args) || {};
      return {
        ...cell,
        style: {
          ...(cell.style || {}),
          width: fixedSize,
          minWidth: fixedSize,
          maxWidth: fixedSize,
        },
      };
    };

    return {
      ...column,
      width,
      minWidth: Math.max(Number(column.minWidth) || minWidth, minWidth),
      ...(column.ellipsis
        ? {
            ellipsis: {
              ...(typeof column.ellipsis === "object" ? column.ellipsis : {}),
              showTitle: false,
            },
          }
        : {}),
      customCell: createCellSize(column.customCell),
      customHeaderCell: createCellSize(column.customHeaderCell),
    };
  });
}
/**
 * 根据基础列宽、手动列宽和视口宽度计算最终列宽。
 * @param {Object} options 列宽解析参数。
 * @param {number} options.viewportWidth 当前表格内容视口宽度。
 * @param {Array<{colValue: string, width: number, minWidth?: number}>} options.columns 可见叶子列。
 * @param {string[]} [options.adaptiveKeys=[]] 可分配剩余空间的列标识。
 * @param {Object<string, number>} [options.manualWidths={}] 用户拖拽后的列宽。
 * @param {number} [options.reservedWidth=0] 选择列等额外预留宽度。
 * @param {number} [options.adaptiveBaseWidth=0] 未设置宽度的自适应列基础宽度。
 * @param {number} [options.minWidth=50] 所有受管列允许的最小宽度。
 * @returns {{widths: Object<string, number>, tableWidth: number, distributing: boolean}} 列宽和表格总宽度。
 */
export function resolveTableColumnLayout({
  viewportWidth,
  columns,
  adaptiveKeys = [],
  manualWidths = {},
  reservedWidth = 0,
  adaptiveBaseWidth = 0,
  minWidth = DEFAULT_MIN_WIDTH,
}) {
  const defaultMinWidth = Number.isFinite(Number(minWidth))
    ? Math.max(Number(minWidth), 0)
    : DEFAULT_MIN_WIDTH;
  const fallbackAdaptiveWidth = Number.isFinite(Number(adaptiveBaseWidth))
    ? Math.max(Number(adaptiveBaseWidth), defaultMinWidth)
    : defaultMinWidth;
  const adaptiveSet = new Set((adaptiveKeys || []).map(String));
  const widths = {};
  (columns || []).forEach((column) => {
    const key = String(column.colValue);
    const minimum = Math.max(Number(column.minWidth) || 0, defaultMinWidth);
    const configuredWidth = Number(manualWidths[key] ?? column.width);
    const baseWidth = Number.isFinite(configuredWidth) && configuredWidth > 0
      ? configuredWidth
      : adaptiveSet.has(key)
        ? fallbackAdaptiveWidth
        : minimum;
    widths[key] = Math.max(baseWidth, minimum);
  });

  const baseTableWidth = Object.values(widths).reduce(
    (total, width) => total + width,
    Number(reservedWidth) || 0
  );
  const remainingWidth = Math.floor(Number(viewportWidth) || 0) - baseTableWidth;
  if (remainingWidth <= 0) {
    return {
      widths,
      tableWidth: baseTableWidth,
      distributing: false,
    };
  }

  const expandableKeys = (columns || [])
    .map((column) => String(column.colValue))
    .filter(
      (key) =>
        adaptiveSet.has(key) &&
        manualWidths[key] == null &&
        widths[key] > 0
    );
  const adaptiveWidthTotal = expandableKeys.reduce(
    (total, key) => total + widths[key],
    0
  );
  if (adaptiveWidthTotal <= 0) {
    return {
      widths,
      tableWidth: baseTableWidth,
      distributing: false,
    };
  }

  let distributedWidth = 0;
  expandableKeys.forEach((key, index) => {
    if (index === expandableKeys.length - 1) {
      widths[key] += remainingWidth - distributedWidth;
      return;
    }
    const extraWidth = Math.floor(
      (remainingWidth * widths[key]) / adaptiveWidthTotal
    );
    widths[key] += extraWidth;
    distributedWidth += extraWidth;
  });

  return {
    widths,
    tableWidth: baseTableWidth + remainingWidth,
    distributing: true,
  };
}
/**
 * 创建统一的表格列宽和拖拽状态管理器。
 * @param {Object} [options] hook 配置。
 * @param {number} [options.minWidth=50] 手动拖拽允许的最小宽度。
 * @param {number} [options.resizeThreshold=3] 认定为真实拖拽的最小像素移动量。
 * @returns {Object} 列宽解析、拖拽确认和状态重置方法。
 */
export function useTableColumnLayout({
  minWidth = DEFAULT_MIN_WIDTH,
  resizeThreshold = DEFAULT_RESIZE_THRESHOLD,
} = {}) {
  const minimumWidth = Number.isFinite(Number(minWidth))
    ? Number(minWidth)
    : DEFAULT_MIN_WIDTH;
  const threshold = Number.isFinite(Number(resizeThreshold))
    ? Number(resizeThreshold)
    : DEFAULT_RESIZE_THRESHOLD;
  const manualWidths = new Map();
  let gesture = null;
  let pendingResizeHeader = null;
  let pendingResizeClickTimer = null;
  let pendingResizeCommit = false;

  /**
   * 清理本轮 resize 手势遗留的表头 click 拦截状态。
   * @returns {void} 无返回值。
   */
  function clearPendingResizeClick() {
    if (pendingResizeClickTimer != null) {
      clearTimeout(pendingResizeClickTimer);
      pendingResizeClickTimer = null;
    }
    pendingResizeHeader = null;
  }

  /**
   * 规范化并保存单列的手动宽度。
   * @param {string} key 列标识。
   * @param {number} width Ant Table 返回的宽度。
   * @returns {number} 保存后的宽度。
   */
  function setManualWidth(key, width) {
    const normalizedWidth = Number(width);
    const storedWidth = Math.max(
      Number.isFinite(normalizedWidth) ? normalizedWidth : minimumWidth,
      minimumWidth
    );
    manualWidths.set(String(key), storedWidth);
    return storedWidth;
  }

  /**
   * 开始记录一次列宽手势。
   * @param {MouseEvent} event resize handle 的鼠标按下事件。
   * @returns {boolean} 是否识别为有效的 resize handle 按下。
   */
  function beginResize(event) {
    const handle = event?.target?.closest?.(".ant-table-resize-handle");
    if (!handle || event.button !== 0) return false;
    gesture = {
      header: handle.closest("th"),
      startX: Number(event.clientX) || 0,
      startY: Number(event.clientY) || 0,
      moved: false,
    };
    clearPendingResizeClick();
    pendingResizeCommit = false;
    return true;
  }

  /**
   * 根据鼠标移动量更新当前手势是否为真实拖拽。
   * @param {MouseEvent} event document 级鼠标移动事件。
   * @returns {boolean} 当前手势是否已经超过拖拽阈值。
   */
  function trackResize(event) {
    if (!gesture) return false;
    const distance = Math.max(
      Math.abs((Number(event?.clientX) || 0) - gesture.startX),
      Math.abs((Number(event?.clientY) || 0) - gesture.startY)
    );
    if (distance > threshold) gesture.moved = true;
    return gesture.moved;
  }

  /**
   * 结束当前列宽手势并记录真实拖拽后的表头。
   * @returns {boolean} 当前手势是否发生了真实移动。
   */
  function finishResize() {
    if (!gesture) return false;
    const moved = gesture.moved;
    pendingResizeHeader = moved ? gesture.header : null;
    if (moved) {
      pendingResizeClickTimer = setTimeout(
        clearPendingResizeClick,
        RESIZE_CLICK_GUARD_MS
      );
    }
    pendingResizeCommit = moved;
    gesture = null;
    return moved;
  }

  /**
   * 在 Ant Table 回调中提交真实拖拽产生的列宽。
   * @param {number} width Ant Table 返回的列宽。
   * @param {Object} column 被拖拽的列。
   * @returns {boolean} 是否实际保存了手动列宽。
   */
  function commitResize(width, column) {
    if (!gesture?.moved && !pendingResizeCommit) return false;
    const key = column?.colValue ?? column?.dataIndex ?? column?.key;
    if (key == null) return false;
    setManualWidth(String(key), width);
    if (!gesture) pendingResizeCommit = false;
    return true;
  }

  /**
   * 拦截真实拖拽结束后同一表头产生的一次 click。
   * @param {MouseEvent} event 表头 click 事件。
   * @returns {boolean} 是否拦截了本次 click。
   */
  function consumeResizeClick(event) {
    if (!pendingResizeHeader) return false;
    if (event?.target?.closest?.("th") !== pendingResizeHeader) return false;
    event.preventDefault();
    event.stopPropagation();
    clearPendingResizeClick();
    return true;
  }

  /**
   * 清理当前表格的手动列宽和拖拽状态。
   * @returns {void} 无返回值。
   */
  function reset() {
    manualWidths.clear();
    gesture = null;
    clearPendingResizeClick();
    pendingResizeCommit = false;
  }

  /**
   * 删除已经不在可见列集合中的手动宽度。
   * @param {string[]} visibleKeys 当前可见列标识。
   * @returns {void} 无返回值。
   */
  function pruneManualWidths(visibleKeys) {
    const visibleSet = new Set((visibleKeys || []).map(String));
    [...manualWidths.keys()].forEach((key) => {
      if (!visibleSet.has(key)) manualWidths.delete(key);
    });
  }

  /**
   * 解析当前表格列宽并返回总宽度。
   * @param {Object} options 列宽解析参数。
   * @param {number} options.viewportWidth 当前内容视口宽度。
   * @param {Array} options.columns 可见叶子列。
   * @param {string[]} [options.adaptiveKeys=[]] 自适应列标识。
   * @param {number} [options.reservedWidth=0] 额外预留宽度。
 * @param {number} [options.adaptiveBaseWidth] 未设置宽度的自适应列基础宽度。
   * @returns {{widths: Object<string, number>, tableWidth: number, distributing: boolean}} 解析后的布局。
   */
  function resolve({
    viewportWidth,
    columns,
    adaptiveKeys = [],
    reservedWidth = 0,
    adaptiveBaseWidth,
  }) {
    const visibleKeys = (columns || []).map((column) => String(column.colValue));
    pruneManualWidths(visibleKeys);
    return resolveTableColumnLayout({
      viewportWidth,
      columns,
      adaptiveKeys,
      manualWidths: Object.fromEntries(manualWidths),
      reservedWidth,
      adaptiveBaseWidth,
      minWidth: minimumWidth,
    });
  }

  /**
   * 将布局解析结果应用到列定义并统一锁定单元格尺寸。
   * @param {Array} columns 原始列定义。
   * @param {Object<string, number>} widths 按列标识索引的解析宽度。
   * @param {Function} getColumnKey 获取列稳定标识的方法。
   * @returns {Array} 带固定尺寸的列定义。
   */
  function applyFixedWidths(columns, widths, getColumnKey) {
    return applyFixedColumnWidths(columns, widths, minimumWidth, getColumnKey);
  }

  return {
    manualWidths,
    get isResizing() {
      return Boolean(gesture?.moved);
    },
    beginResize,
    trackResize,
    finishResize,
    commitResize,
    consumeResizeClick,
    setManualWidth,
    pruneManualWidths,
    resolve,
    applyFixedWidths,
    reset,
  };
}
