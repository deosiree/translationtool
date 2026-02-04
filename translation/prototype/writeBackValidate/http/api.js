// ==================== 列置空 Mock API ====================

/**
 * 列置空 Mock API
 * @param {Object} data - 请求数据
 * @param {string[]} data.columnNames - 需要置空的字段名数组
 * @param {Array} data.entryInfoEntities - 词条数据源
 * @returns {Promise<{data: {list: Array}}>} 返回更新后的数据列表
 */
export function clearFieldsByCondition(data) {
  if (!data || !data.columnNames || !data.entryInfoEntities) {
    return Promise.reject(new Error('请求参数不完整'));
  }

  // Mock实现：返回更新后的数据
  // 实际实现中，应该调用后端API
  return Promise.resolve({
    data: {
      list: data.entryInfoEntities.map(entry => {
        const updatedEntry = { ...entry };
        // 将指定字段置空为 null
        data.columnNames.forEach(columnName => {
          updatedEntry[columnName] = null;
        });
        return updatedEntry;
      })
    }
  });
}
