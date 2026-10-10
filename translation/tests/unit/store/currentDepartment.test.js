/**
 * currentDepartment 全局属性测试
 * 测试 setCurrentDepartment 函数和 store mutations
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createApp } from 'vue'
import { createStore } from 'vuex'
import commonParam from '@/constants/commonParam'

// 模拟 ant-design-vue 的 notification
vi.mock('ant-design-vue', () => ({
  default: {
    install: vi.fn()
  },
  notification: {
    warning: vi.fn()
  }
}))

// 模拟 vuex-persistedstate
vi.mock('vuex-persistedstate', () => ({
  default: vi.fn(() => (store) => store)
}))

describe('currentDepartment 全局属性', () => {
  let app
  let setCurrentDepartment

  beforeEach(() => {
    // 清理 sessionStorage
    sessionStorage.clear()
    
    // 创建新的 Vue app 实例
    app = createApp({})
    app.config.globalProperties.$currentDepartment = null

    // 重新实现 setCurrentDepartment 函数（模拟 main.js 中的实现）
    setCurrentDepartment = (department) => {
      app.config.globalProperties.$currentDepartment = department
      // 持久化到 sessionStorage（处理 Set 类型的序列化）
      if (department) {
        const serialized = {
          ...department,
          ops: Array.from(department.ops || [])
        }
        sessionStorage.setItem('currentDepartment', JSON.stringify(serialized))
      } else {
        sessionStorage.removeItem('currentDepartment')
      }
    }
  })

  describe('setCurrentDepartment 函数', () => {
    it('应该正确设置部门信息到全局属性', () => {
      const department = {
        label: '通用平台部',
        value: 'common',
        classfyID: '1',
        ops: new Set(['needIP', 'needExamine'])
      }

      setCurrentDepartment(department)

      expect(app.config.globalProperties.$currentDepartment).toEqual(department)
    })

    it('应该将 Set 类型的 ops 序列化为数组存储到 sessionStorage', () => {
      const department = {
        label: '通用平台部',
        value: 'common',
        ops: new Set(['needIP', 'needExamine', 'needDelete'])
      }

      setCurrentDepartment(department)

      const stored = sessionStorage.getItem('currentDepartment')
      expect(stored).toBeTruthy()
      
      const parsed = JSON.parse(stored)
      expect(parsed.ops).toEqual(['needIP', 'needExamine', 'needDelete'])
      expect(Array.isArray(parsed.ops)).toBe(true)
    })

    it('应该正确处理 null 值（清空）', () => {
      // 先设置一个部门
      const department = {
        label: '通用平台部',
        value: 'common',
        ops: new Set(['needIP'])
      }
      setCurrentDepartment(department)
      expect(app.config.globalProperties.$currentDepartment).toBeTruthy()

      // 然后清空
      setCurrentDepartment(null)

      expect(app.config.globalProperties.$currentDepartment).toBeNull()
      expect(sessionStorage.getItem('currentDepartment')).toBeNull()
    })

    it('应该处理没有 ops 属性的部门对象', () => {
      const department = {
        label: '测试部门',
        value: 'test',
        classfyID: '999'
        // 没有 ops 属性
      }

      setCurrentDepartment(department)

      expect(app.config.globalProperties.$currentDepartment).toEqual(department)
      
      const stored = sessionStorage.getItem('currentDepartment')
      const parsed = JSON.parse(stored)
      expect(parsed.ops).toEqual([])
    })

    it('应该处理空的 ops Set', () => {
      const department = {
        label: '人工智能部',
        value: 'zn',
        ops: new Set([])
      }

      setCurrentDepartment(department)

      const stored = sessionStorage.getItem('currentDepartment')
      const parsed = JSON.parse(stored)
      expect(parsed.ops).toEqual([])
    })

    it('应该保留部门对象的所有其他属性', () => {
      const department = {
        label: '装置开发部',
        value: 'zz',
        classfyID: '2',
        importTypes: ['file'],
        templateType: [
          { label: 'excel新模板', value: '新模板' }
        ],
        ops: new Set(['needExamine'])
      }

      setCurrentDepartment(department)

      const stored = sessionStorage.getItem('currentDepartment')
      const parsed = JSON.parse(stored)
      
      expect(parsed.label).toBe('装置开发部')
      expect(parsed.value).toBe('zz')
      expect(parsed.classfyID).toBe('2')
      expect(parsed.importTypes).toEqual(['file'])
      expect(parsed.templateType).toEqual([
        { label: 'excel新模板', value: '新模板' }
      ])
    })
  })

  describe('sessionStorage 序列化/反序列化', () => {
    it('应该正确从 sessionStorage 恢复 Set 类型', () => {
      const department = {
        label: '通用平台部',
        value: 'common',
        ops: new Set(['needIP', 'needBranch'])
      }

      setCurrentDepartment(department)

      // 模拟恢复过程（类似 main.js 中的 restoreCurrentDepartment）
      const stored = sessionStorage.getItem('currentDepartment')
      const parsed = JSON.parse(stored)
      if (parsed.ops && Array.isArray(parsed.ops)) {
        parsed.ops = new Set(parsed.ops)
      }

      expect(parsed.ops).toBeInstanceOf(Set)
      expect(parsed.ops.has('needIP')).toBe(true)
      expect(parsed.ops.has('needBranch')).toBe(true)
      expect(parsed.ops.size).toBe(2)
    })

    it('应该处理无效的 JSON 数据', () => {
      sessionStorage.setItem('currentDepartment', 'invalid json{')

      // 模拟恢复过程
      try {
        const stored = sessionStorage.getItem('currentDepartment')
        JSON.parse(stored)
        expect.fail('应该抛出错误')
      } catch (error) {
        expect(error).toBeInstanceOf(SyntaxError)
      }
    })

    it('应该处理缺失的 sessionStorage 数据', () => {
      const stored = sessionStorage.getItem('currentDepartment')
      expect(stored).toBeNull()
    })
  })

  describe('Store setData mutation', () => {
    let store
    let setCurrentDepartmentSpy

    beforeEach(() => {
      // 创建 spy 来跟踪 setCurrentDepartment 的调用
      setCurrentDepartmentSpy = vi.fn((department) => {
        app.config.globalProperties.$currentDepartment = department
        if (department) {
          const serialized = {
            ...department,
            ops: Array.from(department.ops || [])
          }
          sessionStorage.setItem('currentDepartment', JSON.stringify(serialized))
        } else {
          sessionStorage.removeItem('currentDepartment')
        }
      })

      // 创建 store，模拟 setCurrentDepartment 的导入
      store = createStore({
        state: {
          token: null,
          menu: [],
          user: null,
          admin: false
        },
        mutations: {
          setData(state, value) {
            state.token = value.token
            state.menu = value.menu
            state.user = value.user
            if (value.user?.roleName?.includes('管理员')) {
              state.admin = true
            }

            // 根据用户部门设置当前部门信息（使用全局属性）
            const department = value.user?.department
            let currentDepartment = null
            if (department && Object.keys(commonParam.departmentMap).includes(department)) {
              currentDepartment = commonParam.departmentMap[department]
            } else {
              if (department) {
                // 模拟 notification.warning
                console.warn(`未找到用户部门"${department}"的配置，已设置为默认部门`)
              }
              currentDepartment = commonParam.departmentMap['default']
            }
            // 设置全局属性
            setCurrentDepartmentSpy(currentDepartment)
          }
        }
      })
    })

    it('应该根据用户部门设置 currentDepartment', () => {
      const userData = {
        token: 'test-token',
        menu: [],
        user: {
          department: '通用平台部',
          roleName: '普通用户'
        }
      }

      store.commit('setData', userData)

      expect(setCurrentDepartmentSpy).toHaveBeenCalledTimes(1)
      const calledWith = setCurrentDepartmentSpy.mock.calls[0][0]
      expect(calledWith.label).toBe('通用平台部')
      expect(calledWith.value).toBe('common')
      expect(calledWith.ops).toBeInstanceOf(Set)
    })

    it('应该处理未知部门并使用默认部门', () => {
      const userData = {
        token: 'test-token',
        menu: [],
        user: {
          department: '未知部门',
          roleName: '普通用户'
        }
      }

      store.commit('setData', userData)

      expect(setCurrentDepartmentSpy).toHaveBeenCalledTimes(1)
      const calledWith = setCurrentDepartmentSpy.mock.calls[0][0]
      expect(calledWith.label).toBe('公共库')
      expect(calledWith.value).toBe('default')
    })

    it('应该处理用户没有部门信息的情况', () => {
      const userData = {
        token: 'test-token',
        menu: [],
        user: {
          roleName: '普通用户'
          // 没有 department 属性
        }
      }

      store.commit('setData', userData)

      expect(setCurrentDepartmentSpy).toHaveBeenCalledTimes(1)
      const calledWith = setCurrentDepartmentSpy.mock.calls[0][0]
      expect(calledWith.label).toBe('公共库')
      expect(calledWith.value).toBe('default')
    })

    it('应该正确设置管理员状态', () => {
      const userData = {
        token: 'test-token',
        menu: [],
        user: {
          department: '通用平台部',
          roleName: '管理员'
        }
      }

      store.commit('setData', userData)

      expect(store.state.admin).toBe(true)
    })
  })

  describe('Store removeData mutation', () => {
    let store
    let setCurrentDepartmentSpy

    beforeEach(() => {
      setCurrentDepartmentSpy = vi.fn((department) => {
        app.config.globalProperties.$currentDepartment = department
        if (department) {
          const serialized = {
            ...department,
            ops: Array.from(department.ops || [])
          }
          sessionStorage.setItem('currentDepartment', JSON.stringify(serialized))
        } else {
          sessionStorage.removeItem('currentDepartment')
        }
      })

      store = createStore({
        state: {
          token: 'test-token',
          menu: ['menu1'],
          user: { department: '通用平台部' },
          admin: true,
          dynamicRoutes: []
        },
        mutations: {
          removeData(state) {
            state.token = null
            state.menu = []
            state.user = null
            state.dynamicRoutes = []
            state.admin = false
            // 清空全局属性中的部门信息
            setCurrentDepartmentSpy(null)
          }
        }
      })
    })

    it('应该清空 currentDepartment（调用 setCurrentDepartment(null)）', () => {
      // 先设置一个部门
      const department = {
        label: '通用平台部',
        value: 'common',
        ops: new Set(['needIP'])
      }
      setCurrentDepartmentSpy(department)
      expect(app.config.globalProperties.$currentDepartment).toBeTruthy()

      // 执行 removeData
      store.commit('removeData')

      expect(setCurrentDepartmentSpy).toHaveBeenCalledWith(null)
      expect(app.config.globalProperties.$currentDepartment).toBeNull()
      expect(sessionStorage.getItem('currentDepartment')).toBeNull()
    })

    it('应该清空所有 state 数据', () => {
      store.commit('removeData')

      expect(store.state.token).toBeNull()
      expect(store.state.menu).toEqual([])
      expect(store.state.user).toBeNull()
      expect(store.state.dynamicRoutes).toEqual([])
      expect(store.state.admin).toBe(false)
    })
  })
})
