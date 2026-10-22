/**
 * 按最终 XML `abbr` 值统计重复项与空值，比较时区分大小写。
 * @param {Array<{abbr: string}>} items 最终写入 XML 的属性项。
 * @returns {{duplicates: Array<{abbr: string, count: number}>, emptyCount: number}} 重复项与空值数量。
 */
export function analyzeXmlExportAbbrs(items) {
  const counts = new Map();
  let emptyCount = 0;

  items.forEach(({ abbr }) => {
    if (abbr === null || abbr === undefined || abbr === "") {
      emptyCount += 1;
      return;
    }

    counts.set(abbr, (counts.get(abbr) || 0) + 1);
  });

  const duplicates = Array.from(counts, ([abbr, count]) => ({ abbr, count }))
    .filter(({ count }) => count > 1);

  return { duplicates, emptyCount };
}
