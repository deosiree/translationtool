/**
 * filterExcel.vue 组件测试
 * 主要测试权限控制功能：去重回填 v1.5 和 v2.0 按钮的显示/隐藏
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import filterExcel from '@/views/fileManage/filterExcel.vue'

// Mock 依赖
vi.mock('@/http/api/translate', () => ({
  getLanguage: vi.fn(() => Promise.resolve({
    data: {
      list: [
        { name: '英文', value: 'english' },
        { name: '俄文', value: 'russian' }
      ]
    }
  }))
}))

vi.mock('@/http/api/entryManage', () => ({
  entryReadExcel: vi.fn(() => Promise.resolve({
    data: []
  })),
  exportDeduplicatedData: vi.fn(() => Promise.resolve({
    data: {
      notReplicatedEntryInfos: [],
      idRelationMap: {}
    }
  }))
}))

vi.mock('@/utils/tableUtils', () => ({
  onSelectChange: vi.fn(),
  onSelect: vi.fn(),
  onSelectAll: vi.fn(),
  pageChange: vi.fn(),
  clickInput: vi.fn(),
  setTableHeight: vi.fn(),
  handleResizeColumn: vi.fn(),
  getRowClassName: vi.fn(),
  getColPref: vi.fn(),
  changeColumn: vi.fn()
}))

vi.mock('@/utils/requestUtils', () => ({
  getSearch: vi.fn()
}))

vi.mock('@/utils/domUtils', () => ({
  setModalAriaHidden: vi.fn(),
  stopDomEvent: vi.fn(),
  createDragModalDirective: vi.fn(() => ({}))
}))

vi.mock('@/components/ColumnFilter', () => ({
  applyTable: vi.fn(),
  changeColumn: vi.fn(),
}))

vi.mock('@/constants/commonParam.js', () => {
  const entryAllCols = [
    { label: '序号', value: 'index', index: 0, required: true },
    { label: '词条', value: 'entry', index: 2, required: true },
    { label: 'comment', value: 'comment', index: 4, visible: true },
    { label: '操作', value: 'operation', index: 100, required: true },
  ]
  const entryPresets = {
    filterExcel: { ovrd: [], defaults: { visible: false } },
  }
  const entryParams = {
    overlayStyle: {},
    checkboxList: [
      { label: '词条', value: 'entry', index: 1 },
      { label: 'comment', value: 'comment', index: 4 }
    ],
    defaultCheckedColumn: ['entry', 'comment'],
    searchConditionList: [
      { label: '词条', value: 'entry', index: 1 }
    ],
    checkedSearchCondition: ['entry'],
    exportFields: [
      { label: '词条', value: 'entry', index: 1 }
    ]
  }
  return {
    default: {
      languageList: [],
      languageMap: {}
    },
    entryParams,
    entryAllCols,
    entryPresets,
  }
})

vi.mock('ant-design-vue', () => ({
  default: {
    install: vi.fn()
  },
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  },
  notification: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

describe('filterExcel - 文件管理组件权限控制测试', () => {
  let wrapper
  let localStorageMock

  beforeEach(() => {
    // Mock localStorage
    localStorageMock = {
      getItem: vi.fn(() => null),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn()
    }
    global.localStorage = localStorageMock

    // Mock window.onresize
    window.onresize = vi.fn()
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
    delete window.onresize
  })

  // 创建有权限的 currentDepartment mock
  const createCurrentDepartmentWithDev = () => ({
    label: '通用平台部',
    value: 'common',
    ops: new Set(['dev', 'needIP', 'needExamine'])
  })

  // 创建无权限的 currentDepartment mock
  const createCurrentDepartmentWithoutDev = () => ({
    label: '其他部门',
    value: 'other',
    ops: new Set(['needIP'])
  })

  describe('按钮权限控制 - 去重回填 2.0', () => {
    it('有 dev 权限时应该显示"去重回填 2.0"按钮', async () => {
      const currentDepartment = createCurrentDepartmentWithDev()
      
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: { userName: 'test', department: '通用平台部' },
                admin: false
              }
            },
            $currentDepartment: currentDepartment
          }
        },
        props: {
          boxHeight: 600
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证权限方法返回 true
      expect(wrapper.vm.hasDevPermission()).toBe(true)
      
      // 由于 DataBox 被 stub 为 true，slot 内容可能没有被渲染到 DOM
      // 我们通过检查组件实例的 $refs 或者直接验证权限逻辑
      // 如果按钮确实被渲染，检查 HTML 中是否包含按钮文本（排除 BackFillModal 的属性）
      const html = wrapper.html()
      // 查找 button 元素中包含"去重回填 2.0"的情况
      // 排除 back-fill-modal_v2-stub 的 modaltitle 属性中的文本
      const buttonMatch = html.match(/<button[^>]*>[\s\S]*?去重回填 2.0[\s\S]*?<\/button>/i)
      expect(buttonMatch).toBeTruthy()
    })

    it('无 dev 权限时不应该显示"去重回填 2.0"按钮', async () => {
      const currentDepartment = createCurrentDepartmentWithoutDev()
      
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: { userName: 'test', department: '其他部门' },
                admin: false
              }
            },
            $currentDepartment: currentDepartment
          }
        },
        props: {
          boxHeight: 600
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 检查按钮中不应该包含"去重回填 2.0"
      const buttons = wrapper.findAll('button')
      const buttonTexts = buttons.map(btn => btn.text())
      expect(buttonTexts.some(text => text.includes('去重回填 2.0'))).toBe(false)
    })

    it('currentDepartment 为 null 时不应该显示"去重回填 2.0"按钮', async () => {
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: { userName: 'test', department: '其他部门' },
                admin: false
              }
            },
            $currentDepartment: null
          }
        },
        props: {
          boxHeight: 600
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 检查按钮中不应该包含"去重回填 2.0"
      const buttons = wrapper.findAll('button')
      const buttonTexts = buttons.map(btn => btn.text())
      expect(buttonTexts.some(text => text.includes('去重回填 2.0'))).toBe(false)
    })
  })

  describe('按钮权限控制 - 去重回填 v1.5', () => {
    it('有 dev 权限时应该显示"去重回填 v1.5"按钮', async () => {
      const currentDepartment = createCurrentDepartmentWithDev()
      
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: { userName: 'test', department: '通用平台部' },
                admin: false
              }
            },
            $currentDepartment: currentDepartment
          }
        },
        props: {
          boxHeight: 600
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证权限方法返回 true
      expect(wrapper.vm.hasDevPermission()).toBe(true)
      
      // 由于 DataBox 被 stub 为 true，slot 内容可能没有被渲染到 DOM
      // 我们通过检查组件实例的 $refs 或者直接验证权限逻辑
      // 如果按钮确实被渲染，检查 HTML 中是否包含按钮文本（排除 BackFillModal 的属性）
      const html = wrapper.html()
      // 查找 button 元素中包含"去重回填 v1.5"的情况
      // 排除 back-fill-modal_v1_5-stub 的 modaltitle 属性中的文本
      const buttonMatch = html.match(/<button[^>]*>[\s\S]*?去重回填 v1.5[\s\S]*?<\/button>/i)
      expect(buttonMatch).toBeTruthy()
    })

    it('无 dev 权限时不应该显示"去重回填 v1.5"按钮', async () => {
      const currentDepartment = createCurrentDepartmentWithoutDev()
      
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: { userName: 'test', department: '其他部门' },
                admin: false
              }
            },
            $currentDepartment: currentDepartment
          }
        },
        props: {
          boxHeight: 600
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 检查按钮中不应该包含"去重回填 v1.5"
      const buttons = wrapper.findAll('button')
      const buttonTexts = buttons.map(btn => btn.text())
      expect(buttonTexts.some(text => text.includes('去重回填 v1.5'))).toBe(false)
    })

    it('currentDepartment 为 null 时不应该显示"去重回填 v1.5"按钮', async () => {
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: { userName: 'test', department: '其他部门' },
                admin: false
              }
            },
            $currentDepartment: null
          }
        },
        props: {
          boxHeight: 600
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 检查按钮中不应该包含"去重回填 v1.5"
      const buttons = wrapper.findAll('button')
      const buttonTexts = buttons.map(btn => btn.text())
      expect(buttonTexts.some(text => text.includes('去重回填 v1.5'))).toBe(false)
    })
  })

  describe('方法权限控制', () => {
    it('有权限时可以打开去重回填 2.0 模态框', async () => {
      const currentDepartment = createCurrentDepartmentWithDev()
      const { setModalAriaHidden } = await import('@/utils/domUtils')
      
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: { userName: 'test', department: '通用平台部' },
                admin: false
              }
            },
            $currentDepartment: currentDepartment
          }
        },
        props: {
          boxHeight: 600
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 初始状态应该是 false
      expect(wrapper.vm.importBackfillVisible_v2).toBe(false)

      // 调用方法
      wrapper.vm.showImportBackfillModal_v2()
      await nextTick()

      // 应该设置为 true
      expect(wrapper.vm.importBackfillVisible_v2).toBe(true)
      expect(setModalAriaHidden).toHaveBeenCalled()
    })

    it('有权限时可以打开去重回填 v1.5 模态框', async () => {
      const currentDepartment = createCurrentDepartmentWithDev()
      const { setModalAriaHidden } = await import('@/utils/domUtils')
      
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: { userName: 'test', department: '通用平台部' },
                admin: false
              }
            },
            $currentDepartment: currentDepartment
          }
        },
        props: {
          boxHeight: 600
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 初始状态应该是 false
      expect(wrapper.vm.importBackfillVisible_v1_5).toBe(false)

      // 调用方法
      wrapper.vm.showImportBackfillModal_v1_5()
      await nextTick()

      // 应该设置为 true
      expect(wrapper.vm.importBackfillVisible_v1_5).toBe(true)
      expect(setModalAriaHidden).toHaveBeenCalled()
    })

    it('关闭模态框应该正确设置 visible 为 false', async () => {
      const currentDepartment = createCurrentDepartmentWithDev()
      
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: { userName: 'test', department: '通用平台部' },
                admin: false
              }
            },
            $currentDepartment: currentDepartment
          }
        },
        props: {
          boxHeight: 600
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 先打开
      wrapper.vm.showImportBackfillModal_v2()
      await nextTick()
      expect(wrapper.vm.importBackfillVisible_v2).toBe(true)

      // 然后关闭
      wrapper.vm.handleImportBackfillClose_v2()
      await nextTick()
      expect(wrapper.vm.importBackfillVisible_v2).toBe(false)

      // 测试 v1.5
      wrapper.vm.showImportBackfillModal_v1_5()
      await nextTick()
      expect(wrapper.vm.importBackfillVisible_v1_5).toBe(true)

      wrapper.vm.handleImportBackfillClose_v1_5()
      await nextTick()
      expect(wrapper.vm.importBackfillVisible_v1_5).toBe(false)
    })
  })

  describe('权限边界情况', () => {
    it('currentDepartment.ops 不存在时不应该显示按钮', async () => {
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: { userName: 'test', department: '其他部门' },
                admin: false
              }
            },
            $currentDepartment: {
              label: '测试部门',
              value: 'test'
              // 没有 ops 属性
            }
          }
        },
        props: {
          boxHeight: 600
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 检查按钮中不应该包含这些按钮文本
      const buttons = wrapper.findAll('button')
      const buttonTexts = buttons.map(btn => btn.text())
      expect(buttonTexts.some(text => text.includes('去重回填 2.0'))).toBe(false)
      expect(buttonTexts.some(text => text.includes('去重回填 v1.5'))).toBe(false)
    })

    it('ops 为空 Set 时不应该显示按钮', async () => {
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: { userName: 'test', department: '其他部门' },
                admin: false
              }
            },
            $currentDepartment: {
              label: '测试部门',
              value: 'test',
              ops: new Set() // 空的 Set
            }
          }
        },
        props: {
          boxHeight: 600
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 检查按钮中不应该包含这些按钮文本
      const buttons = wrapper.findAll('button')
      const buttonTexts = buttons.map(btn => btn.text())
      expect(buttonTexts.some(text => text.includes('去重回填 2.0'))).toBe(false)
      expect(buttonTexts.some(text => text.includes('去重回填 v1.5'))).toBe(false)
    })
  })

  describe('user 属性重构测试', () => {
    it('不应该在 data 中定义 user 属性', () => {
      const currentDepartment = createCurrentDepartmentWithDev()
      
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: { userName: 'test', department: '通用平台部' },
                admin: false
              }
            },
            $currentDepartment: currentDepartment
          }
        },
        props: {
          boxHeight: 600
        }
      })

      // 验证组件实例不应该有 user 属性
      expect(wrapper.vm.user).toBeUndefined()
    })

    it('应该从 $store.state.user 获取用户信息', async () => {
      const currentDepartment = createCurrentDepartmentWithDev()
      const testUser = {
        userName: 'testUser',
        department: '通用平台部',
        roleName: '普通用户'
      }
      
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: testUser,
                admin: false
              }
            },
            $currentDepartment: currentDepartment
          }
        },
        props: {
          boxHeight: 600
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证可以直接访问 $store.state.user
      expect(wrapper.vm.$store.state.user).toEqual(testUser)
      expect(wrapper.vm.$store.state.user.userName).toBe('testUser')
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const currentDepartment = createCurrentDepartmentWithDev()
      
      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            'SearchBox': true,
            'DataBox': {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'BackFillModal_v1_5': true,
            'ExportButton': true,
            'SelectCols': true,
            'EntryStateSelect': true,
            'TransStateSelect': true,
            'EntryStateBadge': true,
            'TransStateBadge': true,
            'ResetButton': true,
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'size', 'loading', 'danger']
            }
          },
          mocks: {
            $store: {
              state: {
                user: null,
                admin: false
              }
            },
            $currentDepartment: currentDepartment
          }
        },
        props: {
          boxHeight: 600
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      // 验证 user 为 null 时不会报错
      expect(wrapper.vm.$store.state.user).toBeNull()
    })
  })

  describe('beforeUpload CSV 扩展名校验', () => {
    it('非 csv 时 notification 且不调用 entryReadExcel', async () => {
      const { entryReadExcel } = await import('@/http/api/entryManage')
      const { notification } = await import('ant-design-vue')

      wrapper = mount(filterExcel, {
        global: {
          stubs: {
            SearchBox: true,
            DataBox: {
              template: '<div><slot name="operate"></slot><slot name="data"></slot></div>',
              props: ['title', 'height', 'showOperate']
            },
            BackFillModal: true,
            BackFillModal_v2: true,
            BackFillModal_v2_5: true,
            BackFillModal_v3: true,
            BackFillModal_v1_5: true,
            ExportButton: true,
            SelectCols: true,
            EntryStateSelect: true,
            TransStateSelect: true,
            EntryStateBadge: true,
            TransStateBadge: true,
            ResetButton: true,
            ColumnFilter: true,
            FileSelectWithEncoding: true,
            'a-button': true,
            'a-config-provider': true,
            'a-table': true,
            'a-tooltip': true,
          },
          mocks: {
            $store: {
              state: {
                user: { userName: 'test', department: '通用平台部' },
                admin: false
              }
            },
            $currentDepartment: {
              label: '通用平台部',
              value: 'common',
              ops: new Set(['fileUpdate'])
            }
          }
        },
        props: { boxHeight: 600 }
      })

      await nextTick()
      const result = await wrapper.vm.beforeUpload({ name: 'bad.xlsx' })
      expect(result).toBe(false)
      expect(notification.error).toHaveBeenCalled()
      const arg = notification.error.mock.calls.at(-1)[0]
      expect(arg.description).toBe('请选择 .csv 格式的文件！')
      expect(entryReadExcel).not.toHaveBeenCalled()
    })
  })
})
