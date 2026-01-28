// Mock: backendInfo 相关 API（更新词条任务状态与结果）
// 用于前端联调时避免真实接口过慢，整体完成时间控制在约 10~15s
// 保留真实 API，当前默认导出使用 mock 版本

// 任务状态存储：key = `${classifyID}_${i18nUrl}`
const taskStore = new Map();

// 在 10~15 秒之间生成一个完成时间
function createRunningDuration() {
  return 10000 + Math.floor(Math.random() * 5000); // 10s ~ 15s
}

// 获取/初始化任务记录
function getOrCreateTask(key) {
  let task = taskStore.get(key);
  if (!task) {
    // 初始状态为 0，未开始
    task = { state: "0", creationTime: null, duration: null, resultsFetched: false };
    taskStore.set(key, task);
  }
  return task;
}

// Simulate task creation: sets state to 1 (Executing)
export function createTaskMock(classifyID, i18nUrl) {
  const key = `${classifyID || ""}_${i18nUrl || ""}`;
  // 任务创建时，状态设置为 1 (执行中)，并记录创建时间和持续时间
  taskStore.set(key, { state: "1", creationTime: Date.now(), duration: createRunningDuration(), resultsFetched: false });
}

/**
 * Mock: 查询更新词条任务的状态
 * - 即时返回
 * - 初始状态 0
 * - 触发更新任务后（调用 createTaskMock）：状态 1，维持 10~15 秒
 * - 10~15 秒后：转为状态 2
 * - 查询结果后（getEntrysourceListByClassfyResultMock）：转为状态 0
 * 响应格式参考真实接口：
 * { code:200, type:'OK', data:{ state:'0' | '1' | '2' }, message:'任务执行成功', operationObject:'' }
 */
export async function getEntrysourceListByClassfyTaskStateMock(params) {
  const { classifyID, i18nUrl } = params || {};
  const key = `${classifyID || ""}_${i18nUrl || ""}`;

  let task = getOrCreateTask(key);
  const now = Date.now();

  let currentState = task.state;

  if (currentState === "1" && task.creationTime && now - task.creationTime >= task.duration) {
    // 状态 1 (执行中) 持续时间已过，转为状态 2 (执行完成)
    currentState = "2";
    task.state = "2";
    taskStore.set(key, task);
  } else if (currentState === "2" && task.resultsFetched) {
    // 状态 2 (执行完成) 且结果已查询，转为状态 0 (未执行)
    currentState = "0";
    task.state = "0";
    task.creationTime = null;
    task.duration = null;
    task.resultsFetched = false;
    taskStore.set(key, task);
  }

  // 模拟极轻量延迟
  await new Promise((resolve) => setTimeout(resolve, 20));

  const messageMap = {
    "0": "没有查到正在执行的任务信息",
    "1": "任务正在执行中",
    "2": "任务执行成功",
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
 * Mock: 获取更新词条任务的结果
 * - 只有当任务状态为 '2' (执行完成) 时才返回结果
 * - 返回结果后，将任务状态重置为 '0'
 * 响应格式参考真实接口：
 * { code:200, type:'OK', data:{ list:[{ type, sourceFileAndEntryVO:[{ sourceFile, taskEntryVOList:[...] }, ...] }], totalNum }, message:null, operationObject:'' }
 */
export async function getEntrysourceListByClassfyResultMock(params) {
  const { classifyID, i18nUrl } = params || {};
  const key = `${classifyID || ""}_${i18nUrl || ""}`;

  const task = taskStore.get(key);
  let list = [];
  let totalNum = 0;
  let message = "任务未完成或无结果"; // Default message if not ready

  // 只有当任务状态为 '2' 时才返回结果
  if (task && task.state === "2") {
    task.resultsFetched = true; // 标记结果已查询
    taskStore.set(key, task); // Update task in store

    const sourceFileAndEntryVO = [
      {
        sourceFile: "pt/gui_i18n_tool",
        taskEntryVOList: [
          {
            taskID: "mock-task-1",
            taskName: "工具-tr",
            productID: "mock-product-id",
            productName: "工具-tr",
            entities: [
              {
                id: "mock-entry-1",
                entry: "搜索:",
                entrySource: "pt/gui_i18n_tool",
                entryState: 1,
                update: "MockUser",
                updateTime: "2026-01-27 16:21:19",
                importType: "DI",
                writeType: "DI",
                entryVersion: 1,
                tag: "",
                comment: "",
              },
              {
                id: "mock-entry-2",
                entry: "entry1",
                entrySource: "pt/gui_i18n_tool",
                entryState: 1,
                update: "MockUser",
                updateTime: "2026-01-27 16:21:19",
                importType: "DI",
                writeType: "DI",
                entryVersion: 1,
                tag: "root/@idx_0/aaa",
                comment: "",
              },
            ],
            translateType: "英文",
          },
        ],
      },
    ];

    list = [{ type: "DI", sourceFileAndEntryVO }];
    totalNum = list.length;
    message = null; // Clear message on success
  }

  await new Promise((resolve) => setTimeout(resolve, 50));

  return {
    code: 200,
    type: "OK",
    data: {
      list,
      totalNum,
    },
    message,
    operationObject: "",
  };
}
