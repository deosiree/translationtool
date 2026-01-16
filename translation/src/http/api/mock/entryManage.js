/**
 * entryManage v2 版本 API 的 Mock 实现
 * 
 * 根据新 API 文档设计，符合统一任务模型（rules + options）
 * 用于在真实 API 未就绪时提供模拟数据
 * 未来切换到真实 API 时，只需在 entryManage.js 中替换导入即可
 */

/**
 * 从 FormData 中提取 payload（模拟解析，实际 mock 中不需要真正解析文件）
 * @param {FormData} formData - 表单数据
 * @returns {Object|null} 解析后的 payload 对象
 */
function extractPayloadFromFormData(formData) {
  // 在真实环境中，FormData 的 payload 是 JSON 字符串
  // 在 mock 中，我们尝试从 formData 中获取，如果没有则返回默认值
  try {
    const payloadStr = formData.get('payload');
    if (payloadStr && typeof payloadStr === 'string') {
      return JSON.parse(payloadStr);
    }
  } catch (e) {
    console.warn('Mock: 无法解析 payload，使用默认值', e);
  }
  return null;
}

/**
 * 生成校验接口的响应（符合新 API 文档格式）
 * @param {Object} payload - 请求的 payload
 * @returns {Object} 校验响应
 */
function generateValidateResponse(payload) {
  const random = Math.random();
  
  // 70% 概率成功，30% 概率有警告或错误
  if (random < 0.5) {
    // 50% 概率：完全成功
    return {
      success: true,
      canBackfill: true,
      summary: {
        totalOriginRows: 1200,
        totalDedupRows: 300,
        affectedRows: 1180,
        willUpdateCells: 3500
      },
      issues: [],
      preview: []
    };
  } else if (random < 0.7) {
    // 20% 概率：有警告但可回填
    const issues = [];
    const hasAttachments = Math.random() > 0.5;
    
    // 生成一些警告类型的 issues
    const warnTypes = [
      { type: 'EMPTY_VALUE_SKIP', fieldKey: 'englishTranslate', id: 'c321', message: '字段在去重后文件中为空，已跳过更新' },
      { type: 'SPECIAL_CHAR_MISMATCH', fieldKey: 'englishTranslate', id: 'c322', message: '占位符不一致' },
      { type: 'MAX_LENGTH_EXCEEDED', fieldKey: 'englishStatus', id: 'c323', message: '字段长度超出限制' }
    ];
    
    const selectedWarn = warnTypes[Math.floor(Math.random() * warnTypes.length)];
    issues.push({
      level: 'WARN',
      type: selectedWarn.type,
      id: selectedWarn.id,
      fieldKey: selectedWarn.fieldKey,
      message: selectedWarn.message
    });
    
    const response = {
      success: true,
      canBackfill: true,
      summary: {
        totalOriginRows: 1200,
        totalDedupRows: 300,
        affectedRows: 1180,
        skippedRows: 20
      },
      issues: issues,
      preview: []
    };
    
    if (hasAttachments) {
      response.attachments = {
        invalidExcel: {
          fileName: 'backfill_invalid_rows.xlsx',
          downloadUrl: '/api/backfill/validate/files/invalid-excel'
        },
        issueLog: {
          fileName: 'backfill_issues.log',
          downloadUrl: '/api/backfill/validate/files/issue-log'
        }
      };
    }
    
    return response;
  } else {
    // 30% 概率：致命错误，禁止回填
    const fatalTypes = [
      { type: 'MAPPING_MISSING_PARENT', message: '去重后 id 在映射文件中不存在' },
      { type: 'CHECK_FIELDS_MISMATCH', message: 'Excel 中字段值与数据库不一致' },
      { type: 'ORIGIN_ID_NOT_FOUND', message: '去重前 Excel 的 id 不存在于数据库' }
    ];
    
    const selectedFatal = fatalTypes[Math.floor(Math.random() * fatalTypes.length)];
    
    return {
      success: false,
      canBackfill: false,
      issues: [
        {
          level: 'FATAL',
          type: selectedFatal.type,
          message: selectedFatal.message
        }
      ]
    };
  }
}

/**
 * 生成更新接口的响应（符合标准响应格式）
 * @param {Object} payload - 请求的 payload
 * @returns {Object} 更新响应
 * 
 * 响应格式：
 * - 成功：{ code: 200, message: '导入成功' }
 * - 失败：{ code: 201, message: '导入存在失败', data: { failedEntryInfos: [], exceptionVos: [], globalMessage: '...' } }
 */
function generateImportResponse(payload) {
  const random = Math.random();
  
  if (random < 0.7) {
    // 70% 概率：完全成功
    return {
      code: 200,
      message: '导入成功'
    };
  } else {
    // 30% 概率：部分失败
    const failedCount = Math.floor(Math.random() * 3) + 1; // 1-3个失败词条
    const failedEntryInfos = [];
    const backfillFields = payload?.rules?.find(r => r.taskType === 'backfillFields')?.params?.backfillFields || [];
    const firstField = backfillFields[0] || '未知字段';
    
    for (let i = 1; i <= failedCount; i++) {
      const failedInfo = {
        id: `mock-${i}`,
        entry: `测试词条${i}`
      };
      if (firstField) {
        failedInfo[firstField] = `测试值${i}`;
      }
      failedEntryInfos.push(failedInfo);
    }
    
    const exceptionCount = Math.floor(Math.random() * 2) + 1; // 1-2个异常
    const exceptionVos = [];
    for (let i = 1; i <= exceptionCount; i++) {
      exceptionVos.push({
        id: `exception-${i}`,
        message: `异常信息${i}`
      });
    }
    
    return {
      code: 201,
      message: '导入存在失败',
      data: {
        failedEntryInfos,
        exceptionVos,
        globalMessage: '部分词条导入失败'
      }
    };
  }
}

/**
 * Mock: 更新翻译 (v2版本 - 新API)
 * @param {Object} params - 请求参数（通常为空对象）
 * @param {FormData} data - 表单数据，包含文件（dedupExcel, mappingJson）和 payload
 * @returns {Promise<Object>} 返回模拟的API响应
 */
export async function entryImportExcle_v2(params, data) {
  // 模拟异步请求延迟
  await new Promise(resolve => setTimeout(resolve, 300));
  
  // 从 FormData 中提取 payload（如果存在）
  const payload = extractPayloadFromFormData(data);
  
  // 生成响应
  return generateImportResponse(payload);
}

/**
 * Mock: 校验词条 (v2版本 - 新API)
 * @param {Object} params - 请求参数（通常为空对象）
 * @param {FormData} data - 表单数据，包含文件（originExcel, dedupExcel, mappingJson）和 payload
 * @returns {Promise<Object>} 返回模拟的API响应（符合新 API 文档格式）
 */
export async function entryValidate_v2(params, data) {
  // 模拟异步请求延迟
  await new Promise(resolve => setTimeout(resolve, 300));
  
  // 从 FormData 中提取 payload（如果存在）
  const payload = extractPayloadFromFormData(data);
  
  // 生成符合新 API 文档的响应
  const response = generateValidateResponse(payload);
  
  // 包装成标准响应格式（code, message, data）
  return {
    code: 200,
    message: response.success ? '校验成功' : '校验失败',
    data: response
  };
}
