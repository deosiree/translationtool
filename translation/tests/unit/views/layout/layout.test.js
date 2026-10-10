/**
 * Layout 组件测试
 * 测试 FloatingToolBox 组件在 layout 中的权限控制
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import Layout from '@/views/layout/layout.vue'

// Mock 依赖
vi.mock('@/router', () => ({
  default: {
    push: vi.fn(),
    beforeEach: vi.fn(),
    afterEach: vi.fn(),
    addRoute: vi.fn()
  },
  push: vi.fn(),
  beforeEach: vi.fn(),
  afterEach: vi.fn(),
  addRoute: vi.fn()
}))

vi.mock('ant-design-vue', () => ({
  default: {
    install: vi.fn()
  },
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

describe('Layout - FloatingToolBox 权限控制测试', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  // 创建有 toolBox 权限的 currentDepartment mock
  const createCurrentDepartmentWithToolBox = () => ({
    label: '通用平台部',
    value: 'common',
    ops: new Set(['toolBox', 'dev', 'needIP'])
  })

  // 创建无 toolBox 权限的 currentDepartment mock
  const createCurrentDepartmentWithoutToolBox = () => ({
    label: '其他部门',
    value: 'other',
    ops: new Set(['needIP'])
  })

  describe('FloatingToolBox 权限控制', () => {
    it('有 toolBox 权限时应该显示 FloatingToolBox', async () => {
      const currentDepartment = createCurrentDepartmentWithToolBox()
      
      wrapper = mount(Layout, {
        global: {
          stubs: {
            'a-row': true,
            'a-col': true,
            'a-tooltip': true,
            'router-view': true,
            'FloatingToolBox': {
              template: '<div class="floating-tool-box">FloatingToolBox</div>',
              name: 'FloatingToolBox'
            }
          },
          mocks: {
            $store: {
              state: {
                menu: [],
                user: { userName: 'test', department: '通用平台部' },
                admin: false
              },
              commit: vi.fn()
            },
            $router: {
              push: vi.fn()
            },
            $route: {
              path: '/translate'
            },
            $currentDepartment: currentDepartment
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 检查 HTML 中应该包含 FloatingToolBox
      const html = wrapper.html()
      expect(html).toContain('FloatingToolBox')
    })

    it('无 toolBox 权限时不应该显示 FloatingToolBox', async () => {
      const currentDepartment = createCurrentDepartmentWithoutToolBox()
      
      wrapper = mount(Layout, {
        global: {
          stubs: {
            'a-row': true,
            'a-col': true,
            'a-tooltip': true,
            'router-view': true,
            'FloatingToolBox': {
              template: '<div class="floating-tool-box">FloatingToolBox</div>',
              name: 'FloatingToolBox'
            }
          },
          mocks: {
            $store: {
              state: {
                menu: [],
                user: { userName: 'test', department: '其他部门' },
                admin: false
              },
              commit: vi.fn()
            },
            $router: {
              push: vi.fn()
            },
            $route: {
              path: '/translate'
            },
            $currentDepartment: currentDepartment
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 检查 HTML 中不应该包含 FloatingToolBox
      const html = wrapper.html()
      expect(html).not.toContain('FloatingToolBox')
    })

    it('currentDepartment 为 null 时不应该显示 FloatingToolBox', async () => {
      wrapper = mount(Layout, {
        global: {
          stubs: {
            'a-row': true,
            'a-col': true,
            'a-tooltip': true,
            'router-view': true,
            'FloatingToolBox': {
              template: '<div class="floating-tool-box">FloatingToolBox</div>',
              name: 'FloatingToolBox'
            }
          },
          mocks: {
            $store: {
              state: {
                menu: [],
                user: { userName: 'test', department: '其他部门' },
                admin: false
              },
              commit: vi.fn()
            },
            $router: {
              push: vi.fn()
            },
            $route: {
              path: '/translate'
            },
            $currentDepartment: null
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 检查 HTML 中不应该包含 FloatingToolBox
      const html = wrapper.html()
      expect(html).not.toContain('FloatingToolBox')
    })

    it('currentDepartment.ops 不存在时不应该显示 FloatingToolBox', async () => {
      const currentDepartment = {
        label: '测试部门',
        value: 'test'
        // 没有 ops 属性
      }
      
      wrapper = mount(Layout, {
        global: {
          stubs: {
            'a-row': true,
            'a-col': true,
            'a-tooltip': true,
            'router-view': true,
            'FloatingToolBox': {
              template: '<div class="floating-tool-box">FloatingToolBox</div>',
              name: 'FloatingToolBox'
            }
          },
          mocks: {
            $store: {
              state: {
                menu: [],
                user: { userName: 'test', department: '测试部门' },
                admin: false
              },
              commit: vi.fn()
            },
            $router: {
              push: vi.fn()
            },
            $route: {
              path: '/translate'
            },
            $currentDepartment: currentDepartment
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 检查 HTML 中不应该包含 FloatingToolBox
      const html = wrapper.html()
      expect(html).not.toContain('FloatingToolBox')
    })

    it('ops 为空 Set 时不应该显示 FloatingToolBox', async () => {
      const currentDepartment = {
        label: '测试部门',
        value: 'test',
        ops: new Set() // 空的 Set
      }
      
      wrapper = mount(Layout, {
        global: {
          stubs: {
            'a-row': true,
            'a-col': true,
            'a-tooltip': true,
            'router-view': true,
            'FloatingToolBox': {
              template: '<div class="floating-tool-box">FloatingToolBox</div>',
              name: 'FloatingToolBox'
            }
          },
          mocks: {
            $store: {
              state: {
                menu: [],
                user: { userName: 'test', department: '测试部门' },
                admin: false
              },
              commit: vi.fn()
            },
            $router: {
              push: vi.fn()
            },
            $route: {
              path: '/translate'
            },
            $currentDepartment: currentDepartment
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 检查 HTML 中不应该包含 FloatingToolBox
      const html = wrapper.html()
      expect(html).not.toContain('FloatingToolBox')
    })
  })
})
