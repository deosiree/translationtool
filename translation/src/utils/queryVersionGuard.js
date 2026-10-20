/**
 * 创建用于忽略过期查询响应的局部版本守卫。
 * @returns {{ next: Function, isCurrent: Function, invalidate: Function }} 查询版本操作。
 */
export function createQueryVersionGuard() {
  let version = 0;

  return {
    /**
     * 开始一个新查询并返回它的版本号。
     * @returns {number} 新查询的版本号。
     */
    next() {
      version += 1;
      return version;
    },
    /**
     * 判断给定查询是否仍是当前最新查询。
     * @param {number} token - 查询开始时取得的版本号。
     * @returns {boolean} 查询是否仍然有效。
     */
    isCurrent(token) {
      return token === version;
    },
    /**
     * 使所有已发起查询失效。
     * @returns {number} 失效后的当前版本号。
     */
    invalidate() {
      version += 1;
      return version;
    },
  };
}
