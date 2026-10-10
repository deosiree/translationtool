/**
 * 用户 Store Mock 工具函数
 * 用于在测试中创建统一的 $store mock 对象
 */

import { vi } from 'vitest'

/**
 * 创建用户 Store Mock
 * @param {Object|null} user - 用户对象，如果为 null 则使用默认值
 * @returns {Object} 包含 $store 的 mock 对象
 */
export function createUserStoreMock(user = null) {
  const defaultUser = {
    userName: 'testUser',
    department: '测试部门',
    roleName: '普通用户'
  }

  return {
    $store: {
      state: {
        user: user || defaultUser,
        admin: false,
        token: 'test-token',
        menu: []
      },
      commit: vi.fn(),
      dispatch: vi.fn()
    }
  }
}

/**
 * 创建超级管理员用户 Store Mock
 * @returns {Object} 包含 $store 的 mock 对象
 */
export function createAdminUserStoreMock() {
  return createUserStoreMock({
    userName: 'adminUser',
    department: '通用平台部',
    roleName: '超级管理员'
  })
}

/**
 * 创建空用户 Store Mock（user 为 null）
 * @returns {Object} 包含 $store 的 mock 对象
 */
export function createNullUserStoreMock() {
  return {
    $store: {
      state: {
        user: null,
        admin: false,
        token: null,
        menu: []
      },
      commit: vi.fn(),
      dispatch: vi.fn()
    }
  }
}
