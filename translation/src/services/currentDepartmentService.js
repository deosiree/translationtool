let appInstance = null
let pendingDepartment = undefined

/**
 * 初始化 currentDepartment，全局只调用一次：
 * - 保存 app 实例
 * - 初始化全局属性 $currentDepartment
 * - 从 sessionStorage 恢复上一次的 department
 * - 若 init 前有缓存的 pendingDepartment，则优先以其为准
 * @param {import('vue').App} app
 */
export function initCurrentDepartment(app) {
  appInstance = app

  // 确保全局属性存在
  if (!app.config.globalProperties.$currentDepartment) {
    app.config.globalProperties.$currentDepartment = null
  }

  // 如果在 init 之前已经有待写入的 department，优先使用该值
  if (pendingDepartment !== undefined) {
    // console.log("使用缓存部门信息:",pendingDepartment)
    internalSetCurrentDepartment(pendingDepartment)
    pendingDepartment = undefined
    return
  }

  // 否则尝试从 sessionStorage 恢复
  // console.log("尝试从 sessionStorage 恢复部门信息")
  restoreCurrentDepartment()
}

/**
 * 对外暴露的设置方法，供 store 调用
 * @param {any} department
 */
export function setCurrentDepartment(department) {
  // app 尚未初始化时，先缓存，待 init 时再真正写入
  if (!appInstance) {
    // console.log("缓存部门信息:",department)
    pendingDepartment = department
    return
  }
  // console.log("设置部门信息1:",department)
  internalSetCurrentDepartment(department)
}

/**
 * 从 sessionStorage 恢复 currentDepartment
 */
export function restoreCurrentDepartment() {
  if (!appInstance) return
  try {
    const stored = sessionStorage.getItem('currentDepartment')
    if (!stored) {
      // console.log("没有恢复部门信息")
      appInstance.config.globalProperties.$currentDepartment = null
      return
    }
    const parsed = JSON.parse(stored)
    // 将 ops 数组转回 Set
    if (parsed && Array.isArray(parsed.ops)) {
      parsed.ops = new Set(parsed.ops)
    }
    // console.log("恢复部门信息:",parsed)
    appInstance.config.globalProperties.$currentDepartment = parsed
  } catch (error) {
    // console.error('恢复 currentDepartment 失败:', error)
    appInstance.config.globalProperties.$currentDepartment = null
  }
}

/**
 * 可选的获取方法，便于调试
 */
export function getCurrentDepartment() {
  if (!appInstance) return null
  // console.log("获取部门信息:",appInstance.config.globalProperties.$currentDepartment)
  return appInstance.config.globalProperties.$currentDepartment
}

/**
 * 仅在 app 已初始化的前提下写入并持久化
 * @param {any} department
 */
function internalSetCurrentDepartment(department) {
  if (!appInstance) return

  appInstance.config.globalProperties.$currentDepartment = department

  // 持久化到 sessionStorage，并处理 Set 类型字段
  if (department) {
    const serialized = {
      ...department,
      ops: Array.isArray(department.ops)
        ? department.ops
        : Array.from(department.ops || []),
    }
    // console.log("设置部门信息2:",serialized)
    sessionStorage.setItem('currentDepartment', JSON.stringify(serialized))
  } else {
    // console.log("清空部门信息")
    sessionStorage.removeItem('currentDepartment')
  }
}

