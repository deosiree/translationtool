/**
 * entryManage v2 版本 API 的 Mock 实现
 * 
 * 用于在真实 API 未就绪时提供模拟数据
 * 未来切换到真实 API 时，只需在 entryManage.js 中替换导入即可
 */

import { randomApiResponse } from '@/utils/testUtils';

/**
 * Mock: 更新翻译 (v2版本)
 * @param {Object} params - 请求参数，包含 field (字段label)
 * @param {FormData} data - 表单数据，包含文件等
 * @returns {Promise<Object>} 返回模拟的API响应
 */
export async function entryImportExcle_v2(params, data) {
  // 模拟异步请求延迟
  await new Promise(resolve => setTimeout(resolve, 300));
  
  // 使用随机响应生成函数，70%概率成功，30%概率部分失败
  const field = params?.field || '未知字段';
  return await randomApiResponse({
    successProb: 0.7,
    field: field,
    type: 'import'
  });
}

/**
 * Mock: 校验词条 (v2版本)
 * @param {Object} params - 请求参数，包含 field (字段label)
 * @param {FormData} data - 表单数据，包含文件等
 * @returns {Promise<Object>} 返回模拟的API响应
 */
export async function entryValidate_v2(params, data) {
  // 模拟异步请求延迟
  await new Promise(resolve => setTimeout(resolve, 300));
  
  // 使用随机响应生成函数，70%概率成功，30%概率部分失败
  const field = params?.field || '未知字段';
  return await randomApiResponse({
    successProb: 0.7,
    field: field,
    type: 'validate'
  });
}
