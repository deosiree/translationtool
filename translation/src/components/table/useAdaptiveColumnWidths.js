const DEFAULT_MIN_WIDTH = 50;

/**
 * Resolve adaptive columns against the available viewport width.
 * @param {number} viewportWidth Available table viewport width.
 * @param {Array<{colValue: string, width: number, minWidth?: number}>} columns Visible table columns.
 * @param {string[]} adaptiveKeys Columns that may receive remaining width.
 * @param {Object<string, number>} [manualWidths={}] User-resized column widths.
 * @param {number} [reservedWidth=0] Width reserved for columns outside the input list.
 * @returns {Object<string, number>} Resolved widths indexed by column value.
 */
export function resolveAdaptiveColumnWidths(
  viewportWidth,
  columns,
  adaptiveKeys,
  manualWidths = {},
  reservedWidth = 0
) {
  const widths = {};
  columns.forEach((column) => {
    const minWidth = Number(column.minWidth) || 0;
    const width = Number(manualWidths[column.colValue] ?? column.width) || 0;
    widths[column.colValue] = Math.max(width, minWidth);
  });

  const totalWidth = Object.values(widths).reduce((total, width) => total + width, reservedWidth);
  const remainingWidth = Math.floor(Number(viewportWidth) || 0) - totalWidth;
  if (remainingWidth <= 0) return widths;

  const adaptiveSet = new Set(adaptiveKeys);
  const expandableKeys = columns
    .map((column) => column.colValue)
    .filter((key) => adaptiveSet.has(key) && manualWidths[key] == null && widths[key] > 0);
  const adaptiveBaseWidth = expandableKeys.reduce((total, key) => total + widths[key], 0);
  if (adaptiveBaseWidth <= 0) return widths;

  let distributedWidth = 0;
  expandableKeys.forEach((key, index) => {
    if (index === expandableKeys.length - 1) {
      widths[key] += remainingWidth - distributedWidth;
      return;
    }
    const extraWidth = Math.floor((remainingWidth * widths[key]) / adaptiveBaseWidth);
    widths[key] += extraWidth;
    distributedWidth += extraWidth;
  });

  return widths;
}

/**
 * Keep user-resized widths and resolve adaptive columns from their base widths.
 * @param {number} [minWidth=50] Minimum width accepted for manual resizing.
 * @returns {{manualWidths: Map<string, number>, setManualWidth: Function, resolve: Function}} Width state and resolver.
 */
export function useAdaptiveColumnWidths(minWidth = DEFAULT_MIN_WIDTH) {
  const manualWidths = new Map();

  /**
   * Save a user-resized width, clamped to the configured minimum.
   * @param {string} key Column value.
   * @param {number} width User-selected width in pixels.
   * @returns {number} Stored width.
   */
  function setManualWidth(key, width) {
    const normalizedWidth = Number(width);
    const storedWidth = Math.max(
      Number.isFinite(normalizedWidth) ? normalizedWidth : minWidth,
      minWidth
    );
    manualWidths.set(key, storedWidth);
    return storedWidth;
  }

  /**
   * Resolve widths while keeping recorded user widths fixed.
   * @param {number} viewportWidth Available table viewport width.
   * @param {Array<{colValue: string, width: number, minWidth?: number}>} columns Visible table columns.
   * @param {string[]} adaptiveKeys Columns that may receive remaining width.
   * @param {number} [reservedWidth=0] Width reserved for columns outside the input list.
   * @returns {Object<string, number>} Resolved widths indexed by column value.
   */
  function resolve(viewportWidth, columns, adaptiveKeys, reservedWidth = 0) {
    return resolveAdaptiveColumnWidths(
      viewportWidth,
      columns,
      adaptiveKeys,
      Object.fromEntries(manualWidths),
      reservedWidth
    );
  }

  return { manualWidths, setManualWidth, resolve };
}
