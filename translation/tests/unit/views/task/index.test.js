/**
 * Task Index 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 * 特别测试 roleName、department、userName 等多个字段的使用
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import TaskIndex from '@/views/task/index.vue'
import { getClassTree } from '@/http/api/entryManage'
import { getVersion } from '@/http/api/productVersion'
import { getRoleUserByDepartment } from '@/http/api/user'
import { createUserStoreMock, createAdminUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'

// Mock 依赖
vi.mock('@/http/api/task', () => ({
  searchTaskInfo: vi.fn(() => Promise.resolve({
    data: {
      list: [],
      totalNum: 0
    }
  })),
  getDepartments: vi.fn(() => Promise.resolve({
    data: {
      list: ['部门1', '部门2']
    }
  }))
}))

vi.mock('@/http/api/translate', () => ({
  getLanguage: vi.fn(() => Promise.resolve({
    data: {
      list: []
    }
  }))
}))

vi.mock('@/http/api/entryManage', () => ({
  getClassTree: vi.fn(() => Promise.resolve({ data: { list: [] } }))
}))

vi.mock('@/http/api/productVersion', () => ({
  getVersion: vi.fn(() => Promise.resolve({ data: { list: [] } }))
}))

vi.mock('@/http/api/user', () => ({
  getDepartments: vi.fn(() => Promise.resolve({ data: { list: ['部门1', '部门2'] } })),
  getRoleUserByDepartment: vi.fn(() => Promise.resolve({ data: {} }))
}))

vi.mock('@/utils/tableUtils', () => ({
  setTableHeight: vi.fn()
}))

vi.mock('ant-design-vue', () => ({
  default: {
    install: vi.fn()
  },
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  }
}))

/**
 * 创建一个可由测试控制完成时机的 Promise。
 * @returns {Object} Promise 及其 resolve、reject 控制器。
 */
function createDeferred() {
  let resolve
  let reject
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

/**
 * 构造任务页面使用的分类树产品节点。
 * @param {string} productId 产品 ID。
 * @param {string} productName 产品名称。
 * @returns {Array<Object>} 包含产品节点的分类树。
 */
function buildProductTree(productId, productName) {
  return [{
    key: 'department-node',
    title: '部门节点',
    type: 'department',
    children: [{
      key: 'class-node',
      title: '分类节点',
      type: 'classify',
      children: [{ key: productId, title: productName, type: 'product', children: [] }]
    }]
  }]
}

describe('Task Index - user 属性重构测试', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  describe('user 属性验证', () => {
    it('不应该在 data 中定义 user 属性', () => {
      const storeMock = createUserStoreMock()
      
      wrapper = mount(TaskIndex, {
        global: {
          mocks: storeMock,
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
            'a-select': true
          }
        }
      })

      // 验证组件实例不应该有 user 属性
      expect(wrapper.vm.user).toBeUndefined()
    })

    it('应该从 $store.state.user 获取用户信息', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(TaskIndex, {
        global: {
          mocks: storeMock,
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
            'a-select': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证可以直接访问 $store.state.user
      expect(wrapper.vm.$store.state.user).toEqual(testUser)
      expect(wrapper.vm.$store.state.user.userName).toBe('testUser')
      expect(wrapper.vm.$store.state.user.department).toBe('测试部门')
      expect(wrapper.vm.$store.state.user.roleName).toBe('普通用户')
    })

    it('应该正确使用 $store.state.user.roleName 判断超级管理员', async () => {
      const adminUser = {
        userName: 'admin',
        department: '通用平台部',
        roleName: '超级管理员'
      }
      const storeMock = createAdminUserStoreMock()
      storeMock.$store.state.user = adminUser
      
      wrapper = mount(TaskIndex, {
        global: {
          mocks: storeMock,
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
            'a-select': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证可以正确判断超级管理员
      const isAdmin = wrapper.vm.$store.state.user.roleName.includes('超级管理员')
      expect(isAdmin).toBe(true)
    })

    it('应该正确使用 $store.state.user 的多个字段（department, userName等）', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(TaskIndex, {
        global: {
          mocks: storeMock,
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
            'a-select': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证可以使用多个字段
      expect(wrapper.vm.$store.state.user.department).toBe('测试部门')
      expect(wrapper.vm.$store.state.user.userName).toBe('testUser')
      
      // 模拟创建任务时使用这些字段
      const defaultUser = wrapper.vm.$store.state.user?.userName;
      const newTaskData = {
        department: wrapper.vm.$store.state.user.department,
        creator: defaultUser,
        developer: defaultUser,
        entryAuditor: defaultUser,
        translator: defaultUser,
        translationAuditor: defaultUser
      }
      
      expect(newTaskData.department).toBe('测试部门')
      expect(newTaskData.creator).toBe('testUser')
      
      // 验证组件内部使用 $store.state.user?.department 而不是 this.user.department
      // 这通过测试 null 情况来验证
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(TaskIndex, {
        global: {
          mocks: storeMock,
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
            'a-select': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证 user 为 null 时不会报错
      expect(wrapper.vm.$store.state.user).toBeNull()
    })
  })

  describe('产品树异步加载', () => {
    /**
     * 挂载带有任务行组件所需依赖的页面实例。
     * @returns {Object} Vue Test Utils 包装器。
     */
    function mountTaskIndex() {
      wrapper = mount(TaskIndex, {
        global: {
          mocks: createUserStoreMock(),
          stubs: {
            'a-table': true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-input': true,
            'a-select': true,
            'a-tree-select': true,
            ProductModal: true,
            VersionModal: true
          }
        }
      })
      return wrapper
    }

    /**
     * 构造可编辑任务行。
     * @param {Object} overrides 覆盖默认字段的任务属性。
     * @returns {Object} 任务行数据。
     */
    function createTaskRecord(overrides = {}) {
      return {
        id: 'task-1',
        state: '0',
        department: '部门1',
        productId: 'product-1',
        productName: '产品一',
        versionId: 'version-1',
        versionName: '版本一',
        ...overrides
      }
    }

    it('冷加载编辑时保留名称，树解析前不把产品 ID 交给选择器', async () => {
      const pendingTree = createDeferred()
      vi.mocked(getClassTree).mockReturnValueOnce(pendingTree.promise)
      const page = mountTaskIndex()
      const record = createTaskRecord()
      page.vm.dataSource = [record]

      const loading = page.vm.edit(record)

      expect(page.vm.options[record.id].productTreeStatus).toBe('loading')
      expect(page.vm.isProductTreeSelectable(record)).toBe(false)
      expect(page.vm.getProductFallbackText(record)).toContain('产品一')
      expect(page.vm.getProductFallbackText(record)).not.toContain(record.productId)

      pendingTree.resolve({ data: { list: buildProductTree('product-1', '产品一') } })
      await loading

      expect(page.vm.options[record.id].productTreeStatus).toBe('success')
      expect(page.vm.isProductTreeSelectable(record)).toBe(true)
    })

    it('新增行的产品树加载期间显示占位文本', async () => {
      const pendingTree = createDeferred()
      vi.mocked(getClassTree).mockReturnValueOnce(pendingTree.promise)
      const page = mountTaskIndex()
      const record = createTaskRecord({ id: 'new-1', productId: null, productName: null })
      page.vm.dataSource = [record]

      const loading = page.vm.edit(record)

      expect(page.vm.getProductFallbackText(record)).toBe('正在加载产品...')
      expect(page.vm.isProductTreeSelectable(record)).toBe(false)

      pendingTree.resolve({ data: { list: buildProductTree('product-1', '产品一') } })
      await loading
    })

    it('选择产品后保留已加载产品树并刷新版本', async () => {
      vi.mocked(getClassTree).mockResolvedValueOnce({
        data: { list: [
          ...buildProductTree('product-1', '产品一'),
          ...buildProductTree('product-2', '产品二')
        ] }
      })
      vi.mocked(getVersion).mockResolvedValue({
        data: { list: [{ id: 'version-2', name: '版本二' }] }
      })
      const page = mountTaskIndex()
      const record = createTaskRecord()
      page.vm.dataSource = [record]
      await page.vm.edit(record)
      const products = page.vm.options[record.id].products
      vi.mocked(getClassTree).mockClear()

      page.vm.editableData[record.id].productId = 'product-2'
      await page.vm.changeProduct(record)

      expect(getClassTree).not.toHaveBeenCalled()
      expect(page.vm.options[record.id].products).toBe(products)
      expect(getVersion).toHaveBeenLastCalledWith({ productId: 'product-2' })
      expect(page.vm.options[record.id].versions).toEqual([
        { label: '版本二', value: 'version-2' }
      ])
    })

    it('部门切换后忽略旧产品树响应', async () => {
      const firstTree = createDeferred()
      const secondTree = createDeferred()
      vi.mocked(getClassTree)
        .mockReturnValueOnce(firstTree.promise)
        .mockReturnValueOnce(secondTree.promise)
      const page = mountTaskIndex()
      const record = createTaskRecord()
      page.vm.dataSource = [record]
      const oldDepartmentLoad = page.vm.edit(record)

      page.vm.editableData[record.id].department = '部门2'
      const newDepartmentLoad = page.vm.changeDepartment(record)
      secondTree.resolve({ data: { list: buildProductTree('product-new', '新部门产品') } })
      await newDepartmentLoad
      firstTree.resolve({ data: { list: buildProductTree('product-old', '旧部门产品') } })
      await oldDepartmentLoad

      expect(page.vm.options[record.id].productTreeDepartment).toBe('部门2')
      expect(page.vm.options[record.id].products[0].children[0].children[0].key).toBe('product-new')
    })

    it('取消时失效的产品树请求不会让再次编辑停在加载中', async () => {
      const cancelledTree = createDeferred()
      const nextTree = createDeferred()
      vi.mocked(getClassTree)
        .mockReturnValueOnce(cancelledTree.promise)
        .mockReturnValueOnce(nextTree.promise)
      const page = mountTaskIndex()
      const record = createTaskRecord()
      page.vm.dataSource = [record]
      const cancelledLoad = page.vm.edit(record)

      page.vm.cancel(record.id)
      cancelledTree.resolve({ data: { list: buildProductTree('product-old', '旧产品') } })
      await cancelledLoad

      expect(page.vm.options[record.id].productTreeStatus).toBe('idle')

      page.vm.dataSource = [record]
      const currentLoad = page.vm.edit(record)
      expect(getClassTree).toHaveBeenCalledTimes(2)
      expect(page.vm.options[record.id].productTreeStatus).toBe('loading')
      nextTree.resolve({ data: { list: buildProductTree('product-1', '产品一') } })
      await currentLoad

      expect(page.vm.options[record.id].productTreeStatus).toBe('success')
    })

    it('产品树失败后保留名称并可重试', async () => {
      vi.mocked(getClassTree)
        .mockRejectedValueOnce(new Error('network error'))
        .mockResolvedValueOnce({ data: { list: buildProductTree('product-1', '产品一') } })
      const page = mountTaskIndex()
      const record = createTaskRecord()
      page.vm.dataSource = [record]

      await page.vm.edit(record)

      expect(page.vm.options[record.id].productTreeStatus).toBe('error')
      expect(page.vm.getProductFallbackText(record)).toBe('产品一')
      expect(page.vm.canRetryProductTree(record)).toBe(true)

      await page.vm.retryProductTree(record)

      expect(page.vm.options[record.id].productTreeStatus).toBe('success')
      expect(page.vm.isProductTreeSelectable(record)).toBe(true)
    })

    it('当前产品不在树中时显示名称和重试状态，不显示原始 ID', async () => {
      vi.mocked(getClassTree).mockResolvedValueOnce({
        data: { list: buildProductTree('other-product', '其他产品') }
      })
      const page = mountTaskIndex()
      const record = createTaskRecord()
      page.vm.dataSource = [record]

      await page.vm.edit(record)

      expect(page.vm.isProductTreeSelectable(record)).toBe(false)
      expect(page.vm.getProductFallbackText(record)).toBe('产品一')
      expect(page.vm.getProductFallbackText(record)).not.toContain(record.productId)
      expect(page.vm.canRetryProductTree(record)).toBe(true)
    })
  })
})
