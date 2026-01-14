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
