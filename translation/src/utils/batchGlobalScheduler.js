/**
 * 全局并发调度器：主任务阶段单元与拆分子块共用 concurrency 个槽位。
 */
export function createGlobalScheduler(concurrency) {
  const limit = Math.min(Math.max(concurrency, 1), 100)
  let running = 0
  const queue = []

  function pump() {
    while (running < limit && queue.length > 0) {
      const { fn, resolve, reject } = queue.shift()
      running++
      Promise.resolve()
        .then(fn)
        .then(resolve, reject)
        .finally(() => {
          running--
          pump()
        })
    }
  }

  /**
   * 将 fn 排入队列，有空槽时执行；返回 fn 的 Promise。
   * @param {() => Promise<any>} fn
   * @returns {Promise<any>}
   */
  function schedule(fn) {
    return new Promise((resolve, reject) => {
      queue.push({ fn, resolve, reject })
      pump()
    })
  }

  /**
   * 当前 in-flight 数量（测试用）。
   * @returns {number}
   */
  function getRunningCount() {
    return running
  }

  /**
   * 等待队列与 in-flight 全部完成。
   * @returns {Promise<void>}
   */
  function drain() {
    return new Promise(resolve => {
      const check = () => {
        if (running === 0 && queue.length === 0) resolve()
        else setTimeout(check, 0)
      }
      check()
    })
  }

  return { schedule, getRunningCount, drain }
}
