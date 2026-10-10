import { buildMockEntryWithAllFields } from "../mockUtils";

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

// ==================== 校验查询 Mock API ====================
function withMeta(list, meta = {}) {
  return {
    data: {
      list,
      total: list.length,
      ...meta,
    },
  };
}

/**
 * 特殊字符校验（mock）：返回可直接用于列表渲染的词条数组
 * @param {Object} params
 * @returns {Promise<{data:{list:Array,total:number}}>}
 */
export function checkSpecialChar(params = {}) {
  // 简化：直接生成一批 mock 数据，并用字段标记“命中原因”
  const list = buildMockEntryWithAllFields({ count: 20 }).map((r, idx) => ({
    ...r,
    // 方便 UI 观察：插入一段“特殊字符”
    english: idx % 2 === 0 ? `${r.english || "demo"}@#` : r.english,
    _checkType: "specialChar",
    _checkMsg: "包含特殊字符（mock）",
  }));
  return Promise.resolve(withMeta(list));
}

/**
 * 长度超限校验（mock）：返回可直接用于列表渲染的词条数组
 * @param {Object} params
 * @returns {Promise<{data:{list:Array,total:number}}>}
 */
export function checkMaxLength(params = {}) {
  const list = buildMockEntryWithAllFields({ count: 20 }).map((r, idx) => {
    const longText = "L".repeat(60);
    return {
      ...r,
      // 方便 UI 观察：让部分记录明显超长
      english: idx % 2 === 1 ? `${r.english || "demo"}${longText}` : r.english,
      _checkType: "maxLength",
      _checkMsg: "长度超限（mock）",
    };
  });
  return Promise.resolve(withMeta(list));
}