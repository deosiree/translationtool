/**
 * 定时器轮询通用类
 * 提供启动、停止、状态查询方法，自动清理定时器
 */

/**
 * 定时器轮询类
 * @class Polling
 */
export class Polling {
  /**
   * 创建定时器轮询实例
   * @param {Function} checkFn - 检查函数，返回Promise，resolve状态值
   * @param {Function} onStatusChange - 状态变化回调函数，接收新状态值
   * @param {Object} options - 配置选项
   *   - interval: 轮询间隔（毫秒），默认60000（1分钟）
   *   - maxAttempts: 最大尝试次数，默认不限制
   *   - onError: 错误处理回调
   */
  constructor(checkFn, onStatusChange, options = {}) {
    const {
      interval = 60000, // 默认1分钟
      maxAttempts = null, // 默认不限制
      onError = null, // 错误处理回调
    } = options;

    this.checkFn = checkFn;
    this.onStatusChange = onStatusChange;
    this.interval = interval;
    this.maxAttempts = maxAttempts;
    this.onError = onError;
    this.timerId = null;
    this._isRunning = false;
    this.attemptCount = 0;
    this.lastStatus = null;
  }

  /**
   * 停止轮询
   */
  stop() {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
      this._isRunning = false;
      this.attemptCount = 0;
    }
  }

  /**
   * 执行检查
   * @private
   */
  async check() {
    if (this.maxAttempts !== null && this.attemptCount >= this.maxAttempts) {
      this.stop();
      return;
    }

    try {
      const status = await this.checkFn();
      this.attemptCount++;

      // 如果状态发生变化，调用回调
      if (status !== this.lastStatus) {
        this.lastStatus = status;
        if (this.onStatusChange) {
          this.onStatusChange(status);
        }
      }
    } catch (error) {
      console.error("轮询检查失败:", error);
      if (this.onError) {
        this.onError(error);
      } else {
        // 默认错误处理：停止轮询
        this.stop();
      }
    }
  }

  /**
   * 启动轮询
   */
  start() {
    if (this._isRunning) {
      return; // 已经在运行中
    }

    this._isRunning = true;
    this.attemptCount = 0;
    this.lastStatus = null;

    // 立即执行一次检查
    this.check();

    // 设置定时器
    this.timerId = setInterval(() => {
      this.check();
    }, this.interval);
  }

  /**
   * 获取运行状态
   * @returns {boolean} 是否正在运行
   */
  get isRunning() {
    return this._isRunning;
  }
}

/**
 * 多请求轮询管理器类
 * 支持管理多个请求的轮询，统一按周期执行
 * @class MultiRequestPolling
 */
export class MultiRequestPolling {
  /**
   * 创建多请求轮询管理器实例
   * @param {Object} options - 配置选项
   *   - interval: 轮询间隔（毫秒），默认60000（1分钟）
   *   - onError: 错误处理回调
   *   - initialRequests: 初始请求列表，数组格式 [{ checkFn, onStatusChange }, ...]
   */
  constructor(options = {}) {
    const {
      interval = 60000, // 默认1分钟
      onError = null, // 错误处理回调
      initialRequests = [], // 初始请求列表
    } = options;

    this.interval = interval;
    this.onError = onError;
    this.timerId = null;
    this._isRunning = false;
    this.requests = new Map(); // 使用Map存储请求，key为请求ID，value为{ checkFn, onStatusChange, lastStatus }
    this.requestIdCounter = 0;
    this.lastCheckTime = null; // 上次检查时间

    // 初始化时添加初始请求列表
    if (Array.isArray(initialRequests) && initialRequests.length > 0) {
      initialRequests.forEach(({ checkFn, onStatusChange }) => {
        if (checkFn && typeof checkFn === 'function') {
          this.addRequest(checkFn, onStatusChange);
        }
      });
    }
  }

  /**
   * 生成唯一的请求ID
   * @private
   */
  generateRequestId() {
    return `req_${++this.requestIdCounter}_${Date.now()}`;
  }

  /**
   * 执行所有请求的检查
   * @private
   */
  async checkAll() {
    if (this.requests.size === 0) {
      this.stop();
      return;
    }

    const checkPromises = [];
    const requestEntries = Array.from(this.requests.entries());

    for (const [requestId, request] of requestEntries) {
      const checkPromise = (async () => {
        try {
          const status = await request.checkFn();
          
          // 如果状态发生变化，调用回调
          if (status !== request.lastStatus) {
            request.lastStatus = status;
            if (request.onStatusChange) {
              request.onStatusChange(status, requestId);
            }
          }
        } catch (error) {
          console.error(`轮询检查失败 [${requestId}]:`, error);
          if (this.onError) {
            this.onError(error, requestId);
          }
        }
      })();
      
      checkPromises.push(checkPromise);
    }

    await Promise.allSettled(checkPromises);
    this.lastCheckTime = Date.now();
  }

  /**
   * 添加请求到轮询列表
   * @param {Function} checkFn - 检查函数，返回Promise，resolve状态值
   * @param {Function} onStatusChange - 状态变化回调函数，接收(status, requestId)
   * @returns {string} - 返回请求ID，用于后续移除请求
   */
  addRequest(checkFn, onStatusChange) {
    const requestId = this.generateRequestId();
    this.requests.set(requestId, {
      checkFn,
      onStatusChange,
      lastStatus: null,
    });

    // 如果轮询未运行，启动它
    if (!this._isRunning) {
      this.start();
    }
    // 如果轮询正在运行，新请求会自动在下一个周期执行（通过setInterval）
    // 不需要立即执行，保持统一的轮询周期

    return requestId;
  }

  /**
   * 从轮询列表移除请求
   * @param {string} requestId - 请求ID
   */
  removeRequest(requestId) {
    this.requests.delete(requestId);
    // 如果没有请求了，停止轮询
    if (this.requests.size === 0) {
      this.stop();
    }
  }

  /**
   * 启动轮询
   */
  start() {
    if (this._isRunning) {
      return; // 已经在运行中
    }

    if (this.requests.size === 0) {
      return; // 没有请求，不启动
    }

    this._isRunning = true;
    this.lastCheckTime = null;

    // 立即执行一次检查
    this.checkAll();

    // 设置定时器
    this.timerId = setInterval(() => {
      this.checkAll();
    }, this.interval);
  }

  /**
   * 停止轮询
   */
  stop() {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
      this._isRunning = false;
      this.lastCheckTime = null;
    }
  }

  /**
   * 获取所有请求ID列表
   * @returns {Array<string>} 请求ID数组
   */
  getRequests() {
    return Array.from(this.requests.keys());
  }

  /**
   * 获取运行状态
   * @returns {boolean} 是否正在运行
   */
  get isRunning() {
    return this._isRunning;
  }
}