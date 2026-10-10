/**
 * 测试工具函数
 * 包含用于测试异常处理等功能的工具函数
 */

/**
 * @description 随机抛出任务创建异常，用于测试异常处理
 */
export async function randomMsg(msgs = ["执行中", "未执行"], probs = [0.5]) {
  // 使用Math.random()生成0-1之间的随机数
  // 当随机数小于0.5时返回"执行中"，否则返回"未执行"
  const num = Math.random();
  const n = probs.length;
  for (let i = 0; i < n; i++) {
    if (num < probs[i]) {
      return msgs[i];
    }
  }
  return msgs[n];
}

/**
 * @description 随机抛出任务创建异常，用于测试异常处理
 */
export async function randomError(msg = '随机任务创建失败测试', prob = 0.5) {
  if (Math.random() < prob) {
    throw new Error(msg);
  }
}

/**
 * @description 随机生成API响应，用于测试API接口
 * @param {Object} options - 配置选项
 * @param {number} options.successProb - 成功概率 (0-1)，默认0.7
 * @param {string} options.field - 字段名称，默认'未知字段'
 * @param {string} options.type - 响应类型 ('import' | 'validate')，默认'import'
 * @returns {Promise<Object>} API响应对象
 */
export async function randomApiResponse(options = {}) {
  const {
    successProb = 0.7,
    field = '未知字段',
    type = 'import'
  } = options;

  const num = Math.random();
  const isSuccess = num < successProb;

  // 生成随机的消息文本
  const successMessages = {
    import: '导入成功',
    validate: '校验成功'
  };
  const failureMessages = {
    import: '导入存在失败',
    validate: '校验存在失败'
  };

  if (isSuccess) {
    // 成功响应 (code: 200)
    return {
      code: 200,
      message: successMessages[type] || successMessages.import,
      data: {
        success: true,
        field: field,
      }
    };
  } else {
    // 部分失败响应 (code: 201)
    // 生成随机的失败词条信息
    const failedCount = Math.floor(Math.random() * 3) + 1; // 1-3个失败词条
    const failedEntryInfos = [];
    for (let i = 1; i <= failedCount; i++) {
      failedEntryInfos.push({
        id: `mock-${i}`,
        entry: `测试词条${i}`,
        [field]: `测试值${i}`,
        ...(type === 'validate' ? { error: ['长度超限', '特殊字符不一致', '格式错误'][Math.floor(Math.random() * 3)] } : {})
      });
    }

    // 生成随机的异常信息
    const exceptionCount = Math.floor(Math.random() * 2) + 1; // 1-2个异常
    const exceptionVos = [];
    for (let i = 1; i <= exceptionCount; i++) {
      exceptionVos.push({
        id: `exception-${i}`,
        message: `异常信息${i}`
      });
    }

    // 随机的全局消息
    const globalMessages = [
      '部分词条导入失败',
      '部分词条校验失败',
      '存在数据格式错误',
      '部分数据验证不通过'
    ];
    const globalMessage = globalMessages[Math.floor(Math.random() * globalMessages.length)];

    return {
      code: 201,
      message: failureMessages[type] || failureMessages.import,
      data: {
        failedEntryInfos,
        exceptionVos,
        globalMessage
      }
    };
  }
}
