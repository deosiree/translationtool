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
 * @param {string} mockType - Mock 类型：'success' | 'warning' | 'fail'，如果不指定则随机返回
 * @returns {Object} 校验响应
 */
function generateValidateResponse(payload, mockType = null) {
  // 如果指定了 mockType，则根据类型返回对应的响应
  if (mockType === 'success') {
    // 完全成功
    return {
      success: true,
      canBackFill: true,
      summary: {
        totalOriginRows: 1200,
        totalDedupRows: 300,
        affectedRows: 1180,
        willUpdateCells: 3500
      },
      issues: [],
      previews: [],
      attachments: {
        issueLog: [],
        invalidExcel: null
      }
    };
  } else if (mockType === 'warning') {
    // 有警告但可回填
    const issues = [];
    const hasAttachments = true;

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
      canBackFill: true,
      summary: {
        totalOriginRows: 1200,
        totalDedupRows: 300,
        affectedRows: 1180,
        skippedRows: 20
      },
      issues: issues,
      previews: [],
      attachments: {
        issueLog: []
      }
    };

    if (hasAttachments) {
      response.attachments.invalidExcel = {
        fileName: 'backfill_invalid_rows.xlsx',
        downloadUrl: '/api/backfill/validate/files/invalid-excel'
      };
    }

    return response;
  } else if (mockType === 'fail') {
    // 致命错误，禁止回填
    const fatalTypes = [
      { type: 'MAPPING_MISSING_PARENT', message: '去重后 id 在映射文件中不存在' },
      { type: 'CHECK_FIELDS_MISMATCH', message: 'Excel 中字段值与数据库不一致' },
      { type: 'ORIGIN_ID_NOT_FOUND', message: '去重前 Excel 的 id 不存在于数据库' }
    ];

    const selectedFatal = fatalTypes[Math.floor(Math.random() * fatalTypes.length)];

    return {
      success: false,
      canBackFill: false,
      summary: {
        totalOriginRows: 1200,
        totalDedupRows: 300,
        affectedRows: 0,
        willUpdateCells: 0
      },
      issues: [
        {
          level: 'FATAL',
          type: selectedFatal.type,
          message: selectedFatal.message
        }
      ],
      previews: [],
      attachments: {
        issueLog: [],
        invalidExcel: null
      }
    };
  }

  // 如果没有指定类型，则随机返回（保持原有逻辑）
  const random = Math.random();

  // 70% 概率成功，30% 概率有警告或错误
  if (random < 0.5) {
    // 50% 概率：完全成功
    return {
      success: true,
      canBackFill: true,
      summary: {
        totalOriginRows: 1200,
        totalDedupRows: 300,
        affectedRows: 1180,
        willUpdateCells: 3500
      },
      issues: [],
      previews: [],
      attachments: {
        issueLog: [],
        invalidExcel: null
      }
    };
  } else if (random < 0.7) {
    // 20% 概率：有警告但可回填
    const issues = [];
    const hasAttachments = true;

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
      canBackFill: true,
      summary: {
        totalOriginRows: 1200,
        totalDedupRows: 300,
        affectedRows: 1180,
        skippedRows: 20
      },
      issues: issues,
      previews: [],
      attachments: {
        issueLog: []
      }
    };

    if (hasAttachments) {
      response.attachments.invalidExcel = {
        fileName: 'backfill_invalid_rows.xlsx',
        downloadUrl: '/api/backfill/validate/files/invalid-excel'
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
      canBackFill: false,
      summary: {
        totalOriginRows: 1200,
        totalDedupRows: 300,
        affectedRows: 0,
        willUpdateCells: 0
      },
      issues: [
        {
          level: 'FATAL',
          type: selectedFatal.type,
          message: selectedFatal.message
        }
      ],
      previews: [],
      attachments: {
        issueLog: [],
        invalidExcel: null
      }
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

  if (random < 0.5) {
    return {
      code: 200,
      type: 'OK',
      data: {
        globalMessage: null,
        failedEntryInfos: [],
        exceptionVOs: []
      },
      message: null,
      operationObject: ''
    };
  }

  const failedEntryInfos = [
    {
      id: '3ba12ada-5308-4ad1-93e0-6331bcae1fff',
      entry: '送翻测试2',
      english: '送翻测试2',
      russian: '子节点a',
      spanish: '子节点aa',
      comment: 'a'
    },
    {
      id: 'b7f5856c-52b7-4260-9339-f6aee49fd923',
      entry: '送翻测试2',
      english: '送翻测试3',
      russian: '子节点b',
      spanish: '子节点bb',
      comment: 'a'
    }
  ];

  const exceptionVOs = [
    {
      message: '父节点id: "1a9431a6-e24d-41c6-9c8e-107c02b5dd2a",子节点id信息为 "[3ba12ada-5308-4ad1-93e0-6331bcae1fff, b7f5856c-52b7-4260-9339-f6aee49fd923]", 送翻前去重属于同一组的多个词条在该翻译文件中, 并且这多个词条的翻译有所不同, 相关的翻译结果分别为: "[送翻测试3, 送翻测试2]"',
      resolvedMethodMessage: '送翻后的文件中该组词条只保留一个，删除掉该组其他的词条, 然后重新更新翻译'
    }
  ];

  return {
    code: 201,
    type: 'ERROR',
    data: {
      globalMessage: '更新词条翻译时部分词条更新后存在警告和异常信息, 总共有1个信息',
      failedEntryInfos,
      exceptionVOs
    },
    message: '词条翻译更新存在异常, 请查看相关日志信息',
    operationObject: ''
  };
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
 * @param {Object} params - 请求参数，可包含 mockType 字段用于控制返回类型
 *   - mockType: 'success' | 'warning' | 'fail'，用于指定返回的响应类型
 *     - 'success': 完全成功（success=true, canBackFill=true, issues=[]）
 *     - 'warning': 有警告但可回填（success=true, canBackFill=true, issues有WARN级别）
 *     - 'fail': 校验失败不可回填（success=false, canBackFill=false, issues有FATAL级别）
 *     如果不指定 mockType，则随机返回
 * @param {FormData} data - 表单数据，包含文件（dedupOriginExcel, dedupUpdateExcel, mappingJson）和 payload
 * @returns {Promise<Object>} 返回模拟的API响应（符合新 API 文档格式）
 */
export async function entryValidate_v2(params, data) {
  // 模拟异步请求延迟
  await new Promise(resolve => setTimeout(resolve, 300));
  
  // 从 FormData 中提取 payload（如果存在）
  const payload = extractPayloadFromFormData(data);
  
  // 从 params 中获取 mockType（用于手动控制返回类型）
  const mockType = params?.mockType || null;
  
  // 生成符合新 API 文档的响应（直接返回数据段）
  const response = generateValidateResponse(payload, mockType);

  return response;
}

/**
 * Mock: 更新翻译 (v1 版本 - 现网接口响应仿真)
 * @param {Object} params - 请求参数
 * @param {FormData} data - 表单数据
 * @returns {Promise<Object>} v1 更新接口响应
 */
export async function entryImportExcle(params, data) {
  await new Promise(resolve => setTimeout(resolve, 200));
  const payload = extractPayloadFromFormData(data);
  return generateImportResponse(payload);
}
