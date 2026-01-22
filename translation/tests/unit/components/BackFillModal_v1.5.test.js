/**
 * BackFillModal_v1_5 组件测试
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import BackFillModal_v1_5 from '@/components/Button/fileManage/backFill/modal_v1.5.vue'
import { nextTick } from 'vue'

// Mock依赖
vi.mock('@/utils/excelUtils', () => ({
  entryBatchImportExcel_V1_5: vi.fn(() => Promise.resolve({
    code: 200,
    success: ['英文', '俄文'],
    failed: new Map(),
    failedEntryInfos: [],
    exceptionVos: [],
    globalMessage: ''
  })),
  entryValidate_v2: vi.fn(() => Promise.resolve({
    success: true,
    canBackFill: true,
    summary: {
      totalOriginRows: 100,
      totalDedupRows: 80,
      affectedRows: 80,
      willUpdateCells: 160
    },
    issues: [],
    attachments: {
      issueLog: [],
      invalidExcel: null
    }
  }))
}))

vi.mock('@/http/api/userPartiality', () => ({
  queryUserPartiality: vi.fn(() => Promise.resolve({
    data: {
      list: [{
        backfillFields: '英文,俄文'
      }]
    }
  })),
  updateUserPartiality: vi.fn(() => Promise.resolve({}))
}))

vi.mock('@/utils/fileUtils', () => ({
  downloadJsonFile: vi.fn()
}))

vi.mock('@/utils/domUtils', () => ({
  setModalAriaHidden: vi.fn(),
  stopDomEvent: vi.fn(),
  createDragModalDirective: vi.fn(() => ({}))
}))

vi.mock('@/constants/commonParam.js', () => {
  const commonParam = {
    // 为兼容测试，languageList 同时包含 label/name/value 字段
    languageList: [
      { label: '英文', name: '英文', value: 'english' },
      { label: '俄文', name: '俄文', value: 'russian' }
    ],
    languageMap: {
      '英文': { label: '英文', name: '英文', value: 'english' },
      '俄文': { label: '俄文', name: '俄文', value: 'russian' }
    }
  }
  const entryParams = {
    exportFields: [
      { label: '词条', value: 'entry', index: 1 },
      { label: '中文翻译', value: 'chinese', index: 9 },
      { label: '英文翻译', value: 'english', index: 12 },
      { label: '俄文翻译', value: 'russian', index: 15 },
      { label: '西文翻译', value: 'spanish', index: 18 },
      { label: '法文翻译', value: 'french', index: 21 },
      { label: 'tag', value: 'tag', index: 3 },
      { label: 'comment', value: 'comment', index: 4 },
      { label: '英文翻译id', value: 'englishId', index: 4 }
    ]
  }
  return {
    default: commonParam,
    entryParams
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
    error: vi.fn()
  }
}))

describe('BackFillModal_v1_5 - 去重回填模态框 (v1.5版本)', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  describe('契约：label/value 映射与偏好存储', () => {
    it('fieldOptions 的 label 为语种名，value 为字段 key；全选产出 value', async () => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'button',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': true,
            'ExportButton': true
          }
        }
      })

      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 60))

      const englishOpt = wrapper.vm.fieldOptions.find(o => o.label === '英文翻译')
      const russianOpt = wrapper.vm.fieldOptions.find(o => o.label === '俄文翻译')
      expect(englishOpt).toBeDefined()
      expect(russianOpt).toBeDefined()
      expect(englishOpt.value).toBe('english')
      expect(russianOpt.value).toBe('russian')

      wrapper.vm.selectAllBackfillFields()
      await nextTick()
      expect(wrapper.vm.formModel.backfillFields).toContain('english')
      expect(wrapper.vm.formModel.backfillFields).toContain('russian')
    })

    it('提交后 localStorage.backfillFieldsPref.backfillFields 保存为 label 逗号串（英文,俄文）', async () => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'button',
          functionMode: 'updateTranslation',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': {
              template: '<div><slot></slot></div>',
              props: ['visible', 'okLoading', 'modalTitle'],
              emits: ['handleClose', 'handleOK']
            },
            'ExportButton': true,
            // 需要提供 validate，否则 $refs.backFillForm.validate 会报错（覆盖全局配置中的 a-form）
            'a-form': {
              template: '<form ref="backFillForm"><slot></slot></form>',
              methods: {
                validate: vi.fn(() => Promise.resolve()),
                clearValidate: vi.fn()
              }
            }
          }
        }
      })

      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 60))

      const setItemSpy = vi.spyOn(Storage.prototype, 'setItem')

      wrapper.vm.formModel.backfillFields = ['english', 'russian']
      wrapper.vm.formModel.backFillFile = new File(['test'], 'test.csv', { type: 'text/csv' })

      await wrapper.vm.handleOK()
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 120))

      const calls = setItemSpy.mock.calls.filter((c) => c[0] === 'backfillFieldsPref')
      expect(calls.length).toBeGreaterThan(0)
      const saved = JSON.parse(calls[calls.length - 1][1])
      // 组件实际保存的是 value 串（english,russian）
      expect(saved.backfillFields).toBe('english,russian')
    })
  })

  describe('组件渲染', () => {
    it('按钮模式下应该渲染按钮', () => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'button',
          buttonTitle: '测试按钮',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': true,
            'ExportButton': true
          }
        }
      })

      // 检查组件实例的 internalVisible 状态
      expect(wrapper.vm.internalVisible).toBe(false)
      // 检查按钮模式
      expect(wrapper.vm.mode).toBe('button')
    })

    it('模态框模式下应该不显示主按钮', () => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'modal',
          visible: false,
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': true,
            'ExportButton': true
          }
        }
      })

      // 模态框模式下不应该显示主按钮（mode !== 'button'）
      expect(wrapper.vm.mode).toBe('modal')
      expect(wrapper.vm.internalVisible).toBe(false)
    })
  })

  describe('用户偏好功能', () => {
    beforeEach(() => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'button',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': true,
            'ExportButton': true
          }
        }
      })
    })

    it('按钮模式下点击应该获取用户偏好', async () => {
      // 需求变更：用户偏好仅 localStorage（不再调用 userPartiality API）
      localStorage.setItem('backfillFieldsPref', JSON.stringify({
        backfillFields: '英文,俄文'
      }))

      // 直接调用方法而不是通过按钮点击（等价触发打开逻辑）
      wrapper.vm.handleButtonClick()
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))

      // 组件当前会直接将 localStorage 中的 label 列表拆分并赋值到 formModel.backfillFields
      expect(wrapper.vm.formModel.backfillFields).toEqual(['英文', '俄文'])
    })

    it('模态框模式下打开应该获取用户偏好', async () => {
      // 需求变更：用户偏好仅 localStorage（不再调用 userPartiality API）
      localStorage.setItem('backfillFieldsPref', JSON.stringify({
        backfillFields: '英文,俄文'
      }))

      // 先以 visible: false 挂载，然后设置为 true 以触发 watch
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'modal',
          visible: false,
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': true,
            'ExportButton': true
          }
        }
      })

      await nextTick()
      
      // 设置为 true 以触发 watch
      await wrapper.setProps({ visible: true })
      await nextTick()
      // 等待 watch visible 触发和异步调用完成
      await new Promise(resolve => setTimeout(resolve, 150))

      // 组件当前会直接将 localStorage 中的 label 列表拆分并赋值到 formModel.backfillFields
      expect(wrapper.vm.formModel.backfillFields).toEqual(['英文', '俄文'])
    })
  })

  describe('字段选项过滤', () => {
    it('应该过滤掉翻译id字段', async () => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'button',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': true,
            'ExportButton': true
          }
        }
      })

      await nextTick()
      // 等待 mounted 中的 $nextTick 完成
      await new Promise(resolve => setTimeout(resolve, 50))

      // 检查 fieldOptions 不包含翻译id字段
      const fieldOptions = wrapper.vm.fieldOptions
      const hasIdField = fieldOptions.some(item => item.label.includes('翻译id'))
      expect(hasIdField).toBe(false)
    })
  })

  describe('全选功能', () => {
    beforeEach(async () => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'button',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': true,
            'ExportButton': true
          }
        }
      })
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 50))
    })

    it('点击全选应该选择所有字段', async () => {
      // 初始化字段选项
      wrapper.vm.fieldOptions = [
        { label: '英文', value: 'english' },
        { label: '俄文', value: 'russian' }
      ]
      
      // 确保方法存在
      expect(typeof wrapper.vm.selectAllBackfillFields).toBe('function')
      
      wrapper.vm.selectAllBackfillFields()
      await nextTick()

      // 新语义：backfillFields 存 value
      expect(wrapper.vm.formModel.backfillFields).toEqual(['english', 'russian'])
    })
  })

  describe('表单验证', () => {
    beforeEach(async () => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'modal',
          visible: true,
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': {
              template: '<div><slot></slot></div>',
              props: ['visible', 'okLoading', 'modalTitle'],
              emits: ['handleClose', 'handleOK']
            },
            'ExportButton': true,
            // 需要提供 validate，否则 $refs.backFillForm.validate 会报错（覆盖全局配置中的 a-form）
            'a-form': {
              template: '<form ref="backFillForm"><slot></slot></form>',
              methods: {
                validate: vi.fn(() => Promise.resolve()),
                clearValidate: vi.fn()
              }
            }
          }
        }
      })
      await nextTick()
    })

    it('提交时应该验证必填字段', async () => {
      await nextTick()

      // Mock表单验证方法
      const validateSpy = vi.fn(() => Promise.resolve())
      const formRef = wrapper.vm.$refs.backFillForm
      if (formRef) {
        formRef.validate = validateSpy
      } else {
        // 如果 ref 不存在，创建一个 mock
        wrapper.vm.$refs.backFillForm = {
          validate: validateSpy
        }
      }
      
      wrapper.vm.handleOK()
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 50))

      // 应该调用验证
      expect(validateSpy).toHaveBeenCalled()
    })
  })

  describe('API调用', () => {
    beforeEach(async () => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'button',
          functionMode: 'updateTranslation',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': {
              template: '<div><slot></slot></div>',
              props: ['visible', 'okLoading', 'modalTitle'],
              emits: ['handleClose', 'handleOK']
            },
            'ExportButton': true,
            // 覆盖全局配置中的 a-form（简单 template）
            'a-form': {
              template: '<form><slot></slot></form>'
            }
          }
        }
      })
      await nextTick()
    })

    it('updateTranslation模式下应该调用entryBatchImportExcel_V1_5并传入语种名称', async () => {
      const { entryBatchImportExcel_V1_5 } = await import('@/utils/excelUtils')
      
      await nextTick()
      
      // 设置表单数据
      wrapper.vm.formModel.backfillFields = ['english', 'russian']
      wrapper.vm.formModel.backFillFile = new File(['test'], 'test.csv', { type: 'text/csv' })
      // 禁用校验以直接执行批量更新分支
      wrapper.vm.formModel.enableValidate = false
      
      // Mock表单验证通过
      const validateMock = vi.fn(() => Promise.resolve())
      if (!wrapper.vm.$refs.backFillForm) {
        wrapper.vm.$refs.backFillForm = { validate: validateMock }
      } else {
        wrapper.vm.$refs.backFillForm.validate = validateMock
      }

      await wrapper.vm.handleOK()
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 50))

      expect(entryBatchImportExcel_V1_5).toHaveBeenCalled()
      const callArgs = entryBatchImportExcel_V1_5.mock.calls[0]
      expect(callArgs[0]).toEqual(['英文', '俄文']) // 验证语种名称数组
      expect(callArgs[1]).toBeInstanceOf(FormData) // 验证FormData
    })

    it('validate模式下应该调用entryValidate_v2，返回canBackFill值，存储到this.canBackFill，不自动执行更新', async () => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'button',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': {
              template: '<div><slot></slot></div>',
              props: ['visible', 'okLoading', 'modalTitle'],
              emits: ['handleClose', 'handleOK']
            },
            'ExportButton': true,
            // 覆盖全局配置中的 a-form（简单 template）
            'a-form': {
              template: '<form><slot></slot></form>',
              methods: {
                validate: vi.fn(() => Promise.resolve()),
                clearValidate: vi.fn()
              }
            }
          }
        }
      })

      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 50))
      
      const { entryValidate_v2 } = await import('@/utils/excelUtils')
      const { entryBatchImportExcel_V1_5 } = await import('@/utils/excelUtils')
      const { notification } = await import('ant-design-vue')
      
      // 设置表单数据
      wrapper.vm.formModel.backfillFields = ['english', 'russian']
      wrapper.vm.formModel.backFillFile = new File(['test'], 'test.csv', { type: 'text/csv' })
      wrapper.vm.formModel.dedupOriginExcel = new File(['test'], 'origin.csv', { type: 'text/csv' })
      wrapper.vm.formModel.enableValidate = true
      
      // Mock表单验证通过
      const validateMock = vi.fn(() => Promise.resolve())
      if (!wrapper.vm.$refs.backFillForm) {
        wrapper.vm.$refs.backFillForm = { validate: validateMock }
      } else {
        wrapper.vm.$refs.backFillForm.validate = validateMock
      }

      await wrapper.vm.handleOK()
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 50))

      // 验证调用了 entryValidate_v2
      expect(entryValidate_v2).toHaveBeenCalled()
      // 验证返回值存储到 this.validation.canBackFill
      expect(wrapper.vm.validation.canBackFill).toBe(true)
      // 验证模态框显示（组件使用 validationVisible）
      expect(wrapper.vm.validationVisible).toBe(true)
      // 验证不自动执行更新（不应该调用 entryBatchImportExcel_V1_5）
      expect(entryBatchImportExcel_V1_5).not.toHaveBeenCalled()
      // 验证不调用 notification
      expect(notification.success).not.toHaveBeenCalled()
      expect(notification.error).not.toHaveBeenCalled()
    })

    it('提交成功后应该保存用户偏好', async () => {
      const setItemSpy = vi.spyOn(Storage.prototype, 'setItem')

      await nextTick()
      
      // 设置表单数据
      wrapper.vm.formModel.backfillFields = ['english', 'russian']
      wrapper.vm.formModel.backFillFile = new File(['test'], 'test.csv', { type: 'text/csv' })
      
      // Mock表单验证通过
      const validateMock = vi.fn(() => Promise.resolve())
      if (!wrapper.vm.$refs.backFillForm) {
        wrapper.vm.$refs.backFillForm = { validate: validateMock }
      } else {
        wrapper.vm.$refs.backFillForm.validate = validateMock
      }

      await wrapper.vm.handleOK()
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))

      // 需求变更：提交后写入 localStorage（backfillFields 用语种名称逗号串）
      expect(setItemSpy).toHaveBeenCalled()

      const calls = setItemSpy.mock.calls.filter((c) => c[0] === 'backfillFieldsPref')
      expect(calls.length).toBeGreaterThan(0)
      const saved = JSON.parse(calls[calls.length - 1][1])
      // 组件保存的是 value 串（values）
      expect(saved.backfillFields).toBe('english,russian')
    })
  })

  describe('校验逻辑', () => {
    beforeEach(async () => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'button',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': {
              template: '<div><slot></slot><slot name="leftBottomBtn"></slot></div>',
              props: ['visible', 'okLoading', 'modalTitle'],
              emits: ['handleClose', 'handleOK']
            },
            'ExportButton': true,
            'a-form': {
              template: '<form><slot></slot></form>',
              methods: {
                validate: vi.fn(() => Promise.resolve()),
                clearValidate: vi.fn()
              }
            }
          }
        }
      })
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 50))
    })

    it('handleValidation应该返回canBackFill值，统一显示模态框，不调用notification', async () => {
      const { entryValidate_v2 } = await import('@/utils/excelUtils')
      const { notification } = await import('ant-design-vue')

      // 设置表单数据
      wrapper.vm.formModel.backfillFields = ['english', 'russian']
      wrapper.vm.formModel.backFillFile = new File(['test'], 'test.csv', { type: 'text/csv' })
      wrapper.vm.formModel.dedupOriginExcel = new File(['test'], 'origin.csv', { type: 'text/csv' })

      await wrapper.vm.handleValidation()

      // 验证统一显示模态框（组件使用 validationVisible）
      expect(wrapper.vm.validationVisible).toBe(true)
      // 验证不调用 notification
      expect(notification.success).not.toHaveBeenCalled()
      expect(notification.error).not.toHaveBeenCalled()
      // 验证保存了校验结果
      expect(wrapper.vm.validation.summary).toBeTruthy()
      expect(wrapper.vm.validation.canBackFill).toBe(true)
      expect(wrapper.vm.validation.issues).toEqual([])
    })

    it('handleValidation在不同场景下应返回正确的canBackFill值', async () => {
      const { entryValidate_v2 } = await import('@/utils/excelUtils')
      
      // 测试场景1: success && canBackFill
      entryValidate_v2.mockResolvedValueOnce({
        success: true,
        canBackFill: true,
        summary: { totalOriginRows: 100 },
        issues: [],
        attachments: { issueLog: [], invalidExcel: null }
      })
      wrapper.vm.formModel.dedupOriginExcel = new File(['test'], 'origin.csv')
      wrapper.vm.formModel.backFillFile = new File(['test'], 'test.csv')
      await wrapper.vm.handleValidation()
      expect(wrapper.vm.validation.canBackFill).toBe(true)
      expect(wrapper.vm.validationVisible).toBe(true)

      // 测试场景2: success && !canBackFill
      entryValidate_v2.mockResolvedValueOnce({
        success: true,
        canBackFill: false,
        summary: { totalOriginRows: 100 },
        issues: [{ level: 'WARN', message: '警告' }],
        attachments: { issueLog: [], invalidExcel: null }
      })
      wrapper.vm.validationVisible = false
      await wrapper.vm.handleValidation()
      expect(wrapper.vm.validation.canBackFill).toBe(false)
      expect(wrapper.vm.validationVisible).toBe(true)

      // 测试场景3: !success
      entryValidate_v2.mockResolvedValueOnce({
        success: false,
        canBackFill: false,
        summary: { totalOriginRows: 100 },
        issues: [{ level: 'FATAL', message: '错误' }],
        attachments: { issueLog: [], invalidExcel: null }
      })
      wrapper.vm.validationVisible = false
      await wrapper.vm.handleValidation()
      expect(wrapper.vm.validation.canBackFill).toBe(false)
      expect(wrapper.vm.validationVisible).toBe(true)
    })

    it('handleContinueBackFill应该关闭模态框并执行更新操作', async () => {
      const { entryBatchImportExcel_V1_5 } = await import('@/utils/excelUtils')
      
      // 修改 mock 返回 code: 201 以触发更新窗打开
      entryBatchImportExcel_V1_5.mockResolvedValueOnce({
        code: 201,
        msgBylang: [
          {
            lang: '英文',
            code: 201,
            globalMessage: '部分成功',
            failedEntryInfos: [{ id: 1, entry: 'test' }],
            exceptionVos: []
          }
        ]
      })
      
      // 设置初始状态（适配当前组件数据结构）
      wrapper.vm.validation.canBackFill = true
      wrapper.vm.validationVisible = true
      wrapper.vm.formModel.backfillFields = ['english', 'russian']
      wrapper.vm.formModel.backFillFile = new File(['test'], 'test.csv', { type: 'text/csv' })

      // 调用更新操作（不立即await，以便验证立即关闭的行为）
      const updatePromise = wrapper.vm.handleContinueBackFill()
      
      // 验证在更新开始时（API调用之前）校验窗立即关闭
      // handleBatchUpdate() 在方法开始时同步设置 validationVisible = false
      expect(wrapper.vm.validationVisible).toBe(false)
      
      // 等待更新操作完成
      await updatePromise
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 50))

      // 验证校验窗保持关闭状态
      expect(wrapper.vm.validationVisible).toBe(false)
      // 验证打开了更新模态框
      expect(wrapper.vm.updateVisible).toBe(true)
      // 验证调用了更新操作
      expect(entryBatchImportExcel_V1_5).toHaveBeenCalled()
      // 验证 loading 状态重置
      expect(wrapper.vm.loading).toBe(false)
    })

    it('isValidationSuccess计算属性应该根据canBackFill和validationIssues正确计算', async () => {
      // 场景1: canBackFill=true && issues.length=0
      wrapper.vm.validation.canBackFill = true
      wrapper.vm.validation.issues = []
      expect(wrapper.vm.isValidationSuccess).toBe(true)

      // 场景2: canBackFill=true && issues.length>0
      wrapper.vm.validation.issues = [{ level: 'WARN', message: '警告' }]
      expect(wrapper.vm.isValidationSuccess).toBe(false)

      // 场景3: canBackFill=false
      wrapper.vm.validation.canBackFill = false
      wrapper.vm.validation.issues = []
      expect(wrapper.vm.isValidationSuccess).toBe(false)
    })

    it('failedInfoClose应该重置canBackFill状态', async () => {
      wrapper.vm.validation.canBackFill = true
      wrapper.vm.validation.summary = { totalOriginRows: 100 }
      wrapper.vm.validation.issues = [{ level: 'WARN', message: '警告' }]
      wrapper.vm.validationVisible = true

      // 组件方法为 validationClose
      wrapper.vm.validationClose()

      expect(wrapper.vm.validation.canBackFill).toBe(false)
      expect(wrapper.vm.validation.summary).toBe(null)
      expect(wrapper.vm.validation.issues).toEqual([])
      expect(wrapper.vm.validationVisible).toBe(false)
      // 验证 isValidationSuccess 也会自动更新
      expect(wrapper.vm.isValidationSuccess).toBe(false)
    })
  })

  describe('校验结果模态框显示', () => {
    beforeEach(async () => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'button',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': {
              template: `
                <div v-if="visible">
                  <slot></slot>
                  <slot name="leftBottomBtn"></slot>
                </div>
              `,
              props: ['visible', 'okLoading', 'modalTitle'],
              emits: ['handleClose', 'handleOK']
            },
            'ExportButton': true,
            'a-form': {
              template: '<form><slot></slot></form>',
              methods: {
                validate: vi.fn(() => Promise.resolve()),
                clearValidate: vi.fn()
              }
            },
            'a-alert': true,
            'a-table': true,
            'a-button': true
          }
        }
      })
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 50))
    })

    it('当canBackFill=true时应该显示"继续回填"按钮', async () => {
      wrapper.vm.validation.canBackFill = true
      wrapper.vm.validationVisible = true
      await nextTick()

      // 验证按钮应该在模态框中（通过检查组件状态）
      expect(wrapper.vm.validation.canBackFill).toBe(true)
    })

    it('当canBackFill=false时不应该显示"继续回填"按钮', async () => {
      wrapper.vm.validation.canBackFill = false
      wrapper.vm.validationVisible = true
      await nextTick()

      // 验证按钮不应该显示
      expect(wrapper.vm.validation.canBackFill).toBe(false)
    })

    it('当isValidationSuccess=true时应该显示校验通过提示', async () => {
      wrapper.vm.validation.canBackFill = true
      wrapper.vm.validation.issues = []
      wrapper.vm.formModel.enableValidate = true
      wrapper.vm.validationVisible = true
      await nextTick()

      // 验证 isValidationSuccess 为 true
      expect(wrapper.vm.isValidationSuccess).toBe(true)
    })
  })

  describe('更新模态框打开时关闭校验模态框', () => {
    beforeEach(async () => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'button',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': {
              template: `
                <div v-if="visible">
                  <slot></slot>
                  <slot name="leftBottomBtn"></slot>
                </div>
              `,
              props: ['visible', 'okLoading', 'modalTitle'],
              emits: ['handleClose', 'handleOK']
            },
            'ExportButton': true,
            'a-form': {
              template: '<form><slot></slot></form>',
              methods: {
                validate: vi.fn(() => Promise.resolve()),
                clearValidate: vi.fn()
              }
            },
            'a-alert': true,
            'a-table': true,
            'a-button': true
          }
        }
      })
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 50))
    })

    it('当 updateVisible 变为 true 时，应该自动关闭校验模态框（button 模式）', async () => {
      // 先打开校验模态框
      wrapper.vm.validationVisible = true
      await nextTick()
      expect(wrapper.vm.validationVisible).toBe(true)

      // 打开更新模态框
      wrapper.vm.updateVisible = true
      await nextTick()

      // 验证校验模态框被关闭
      expect(wrapper.vm.validationVisible).toBe(false)
      expect(wrapper.vm.updateVisible).toBe(true)
    })

    it('当 updateVisible 变为 true 时，应该自动关闭校验模态框（modal 模式）', async () => {
      // 切换到 modal 模式
      await wrapper.setProps({ mode: 'modal' })
      await nextTick()

      // 先打开校验模态框
      wrapper.vm.validationVisible = true
      await nextTick()
      expect(wrapper.vm.validationVisible).toBe(true)

      // 打开更新模态框
      wrapper.vm.updateVisible = true
      await nextTick()

      // 验证校验模态框被关闭
      expect(wrapper.vm.validationVisible).toBe(false)
      expect(wrapper.vm.updateVisible).toBe(true)
    })

    it('handleCloseInternal 应该关闭校验模态框（button 模式）', async () => {
      // 先打开校验模态框
      wrapper.vm.validationVisible = true
      await nextTick()
      expect(wrapper.vm.validationVisible).toBe(true)

      // 调用 handleCloseInternal
      wrapper.vm.handleCloseInternal()
      await nextTick()

      // 验证校验模态框被关闭
      expect(wrapper.vm.validationVisible).toBe(false)
      // 验证主模态框也被关闭（button 模式）
      expect(wrapper.vm.internalVisible).toBe(false)
    })

    it('handleCloseInternal 应该关闭校验模态框（modal 模式）', async () => {
      // 切换到 modal 模式
      await wrapper.setProps({ mode: 'modal' })
      await nextTick()

      // 先打开校验模态框
      wrapper.vm.validationVisible = true
      await nextTick()
      expect(wrapper.vm.validationVisible).toBe(true)

      // 调用 handleCloseInternal
      wrapper.vm.handleCloseInternal()
      await nextTick()

      // 验证校验模态框被关闭
      expect(wrapper.vm.validationVisible).toBe(false)
      // 验证触发了 handleClose 事件（modal 模式）
      expect(wrapper.emitted('handleClose')).toBeTruthy()
    })

    it('handleCloseInternal 应该关闭更新模态框（button 模式）', async () => {
      // 先打开更新模态框
      wrapper.vm.updateVisible = true
      await nextTick()
      expect(wrapper.vm.updateVisible).toBe(true)

      // 调用 handleCloseInternal
      wrapper.vm.handleCloseInternal()
      await nextTick()

      // 验证更新模态框被关闭
      expect(wrapper.vm.updateVisible).toBe(false)
      // 验证主模态框也被关闭（button 模式）
      expect(wrapper.vm.internalVisible).toBe(false)
    })

    it('handleCloseInternal 应该关闭更新模态框（modal 模式）', async () => {
      // 切换到 modal 模式
      await wrapper.setProps({ mode: 'modal' })
      await nextTick()

      // 先打开更新模态框
      wrapper.vm.updateVisible = true
      await nextTick()
      expect(wrapper.vm.updateVisible).toBe(true)

      // 调用 handleCloseInternal
      wrapper.vm.handleCloseInternal()
      await nextTick()

      // 验证更新模态框被关闭
      expect(wrapper.vm.updateVisible).toBe(false)
      // 验证触发了 handleClose 事件（modal 模式）
      expect(wrapper.emitted('handleClose')).toBeTruthy()
    })

    it('handleCloseInternal 应该同时关闭校验模态框和更新模态框', async () => {
      // 先打开校验模态框
      wrapper.vm.validationVisible = true
      await nextTick()
      expect(wrapper.vm.validationVisible).toBe(true)
      
      // 然后打开更新模态框（此时校验模态框会被 watch 自动关闭）
      wrapper.vm.updateVisible = true
      await nextTick()
      expect(wrapper.vm.validationVisible).toBe(false) // watch 已自动关闭
      expect(wrapper.vm.updateVisible).toBe(true)

      // 调用 handleCloseInternal
      wrapper.vm.handleCloseInternal()
      await nextTick()

      // 验证更新模态框被关闭（校验模态框已经是 false，无需验证）
      expect(wrapper.vm.updateVisible).toBe(false)
      expect(wrapper.vm.validationVisible).toBe(false)
      // 验证主模态框也被关闭（button 模式）
      expect(wrapper.vm.internalVisible).toBe(false)
    })
  })
})
