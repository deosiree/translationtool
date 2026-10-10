import { createTaskMock } from "./backendInfo";

/**
 * Mock: 分类树数据（用于本地调试右键菜单 / 删除等交互）
 * 只在前端传入 params.__mock 时才会被使用
 */
export async function getClassTreeMock(params) {
  // 轻微延迟，模拟网络
  await new Promise((resolve) => setTimeout(resolve, 50));

  const treeList = [
    {
      key: "dept-1",
      title: "公共库",
      type: "department",
      children: [
        {
          key: "common-1",
          title: "公共词条库",
          type: "common",
          children: [],
        },
        {
          key: "classify-1",
          title: "分类A",
          type: "classify",
          parentId: "dept-1",
          children: [
            {
              key: "product-1",
              title: "产品A-1",
              type: "product",
              parentId: "classify-1",
              children: [
                {
                  key: "module-1",
                  title: "模块A-1-1",
                  type: "module",
                  parentId: "product-1",
                  children: [],
                },
              ],
            },
            {
              key: "classify-1-1",
              title: "子分类A-1",
              type: "classify",
              parentId: "classify-1",
              children: [],
            },
          ],
        },
      ],
    },
  ];

  return {
    code: 200,
    type: "OK",
    data: {
      list: treeList,
    },
    message: null,
    operationObject: "",
  };
}

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
function generateImportResponse() {
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
 * Mock: 创建更新词条任务
 */
export async function createEntrysourceListByClassfyTaskMock(params) {
  const { classifyID, i18nUrl } = params || {};
  createTaskMock(classifyID, i18nUrl); // Call the backendInfo mock to set the state

  await new Promise(resolve => setTimeout(resolve, 50)); // Small delay for async behavior

  return {
    code: 200,
    type: 'OK',
    message: '更新任务已创建',
    operationObject: '',
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

// ==================== v3 异步更新翻译相关 Mock ====================

// 以 transType 作为 key 维护任务状态
const asyncTaskStore = new Map();

function getAsyncTaskKey(transType) {
  return transType || "default";
}

function createAsyncTaskIfNeeded(transType) {
  const key = getAsyncTaskKey(transType);
  let task = asyncTaskStore.get(key);
  if (!task) {
    task = {
      state: "0",          // 0：无任务；1：执行中；2：已完成
      createdAt: null,
      duration: 10000,     // 固定约 10 秒执行时间
      result: null,
      resultFetched: false,
    };
    asyncTaskStore.set(key, task);
  }
  return { key, task };
}

/**
 * Mock: 提交异步更新任务（asyncEntryImportExcle）
 * - 立即返回 code=200，message="创建词条更新任务成功"
 * - 将对应 transType 的任务状态置为 1（执行中），约 10 秒后转为 2（已完成）
 */
export async function asyncEntryImportExcle(params, data) {
  const transType = params?.transType || "default";
  const { key } = createAsyncTaskIfNeeded(transType);
  const task = {
    state: "1",
    createdAt: Date.now(),
    duration: 10000,
    // 简单固定一份结果，结构对齐真实接口
    result: {
      globalMessage: "更新词条翻译成功（Mock）",
      failedEntryInfos: [],
      exceptionVOs: [],
    },
    resultFetched: false,
  };
  asyncTaskStore.set(key, task);

  // 模拟网络延迟
  await new Promise((resolve) => setTimeout(resolve, 100));

  return {
    code: 200,
    type: "OK",
    data: null,
    message: "创建词条更新任务成功（Mock）",
    operationObject: "",
  };
}

/**
 * Mock: 查询异步更新任务状态（getEntryImportExcleTaskState）
 * - 约 10 秒内返回 state=1（执行中）
 * - 10 秒后返回 state=2（执行完成）
 * - 在结果被 getEntryImportExcleTaskStateResult 取走后，下次再查将恢复为 0
 */
export async function getEntryImportExcleTaskState(params) {
  const transType = params?.transType || "default";
  const { key, task } = createAsyncTaskIfNeeded(transType);

  let currentState = task.state;
  const now = Date.now();

  if (currentState === "1" && task.createdAt && now - task.createdAt >= task.duration) {
    currentState = "2";
    task.state = "2";
    asyncTaskStore.set(key, task);
  } else if (currentState === "2" && task.resultFetched) {
    currentState = "0";
    task.state = "0";
    task.createdAt = null;
    task.resultFetched = false;
    asyncTaskStore.set(key, task);
  }

  await new Promise((resolve) => setTimeout(resolve, 50));

  const messageMap = {
    "0": "当前没有任务（Mock）",
    "1": "任务执行中（Mock）",
    "2": "任务执行成功（Mock）",
  };

  return {
    code: 200,
    type: "OK",
    data: {
      state: currentState,
    },
    message: messageMap[currentState] || "",
    operationObject: "",
  };
}

/**
 * Mock: 获取异步更新任务结果（getEntryImportExcleTaskStateResult）
 * - 仅当 state=2 时返回结果；否则返回空结果并提示消息
 * - 返回后将任务标记为已取结果，后续状态查询会回到 0
 */
export async function getEntryImportExcleTaskStateResult(params) {
  const transType = params?.transType || "default";
  const { key, task } = createAsyncTaskIfNeeded(transType);

  await new Promise((resolve) => setTimeout(resolve, 50));

  if (task.state !== "2") {
    return {
      code: 200,
      type: "OK",
      data: {
        globalMessage: "任务尚未完成或无结果（Mock）",
        failedEntryInfos: [],
        exceptionVOs: [],
      },
      message: "任务尚未完成或无结果（Mock）",
      operationObject: "",
    };
  }

  task.resultFetched = true;
  asyncTaskStore.set(key, task);

  return {
    code: 200,
    type: "OK",
    data: {
      globalMessage: task.result?.globalMessage || "更新词条翻译成功（Mock）",
      failedEntryInfos: task.result?.failedEntryInfos || [],
      exceptionVOs: task.result?.exceptionVOs || [],
    },
    message: "词条翻译更新成功（Mock）",
    operationObject: "",
  };
}

