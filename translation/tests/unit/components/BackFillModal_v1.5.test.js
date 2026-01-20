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
    code: 200,
    success: ['词条'],
    failed: new Map(),
    failedEntryInfos: [],
    exceptionVos: [],
    globalMessage: ''
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
  stopDomEvent: vi.fn()
}))

vi.mock('@/constants/commonParam.js', () => ({
  entryParams: {
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
}))

vi.mock('ant-design-vue', () => ({
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

      const englishOpt = wrapper.vm.fieldOptions.find(o => o.label === '英文')
      const russianOpt = wrapper.vm.fieldOptions.find(o => o.label === '俄文')
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
      expect(saved.backfillFields).toBe('英文,俄文')
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

      // 组件内部会把 label 偏好映射为 value 存到 formModel.backfillFields
      expect(wrapper.vm.formModel.backfillFields).toEqual(['english', 'russian'])
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

      expect(wrapper.vm.formModel.backfillFields).toEqual(['english', 'russian'])
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

    it('validate模式下应该调用entryValidate_v2', async () => {
      wrapper = mount(BackFillModal_v1_5, {
        props: {
          mode: 'button',
          functionMode: 'validate',
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
      await new Promise(resolve => setTimeout(resolve, 50))
      
      const { entryValidate_v2 } = await import('@/utils/excelUtils')
      
      // 设置表单数据
      wrapper.vm.formModel.backfillFields = ['english', 'russian']
      wrapper.vm.formModel.backFillFile = new File(['test'], 'test.csv', { type: 'text/csv' })
      wrapper.vm.formModel.originExcel = new File(['test'], 'origin.csv', { type: 'text/csv' })
      
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

      expect(entryValidate_v2).toHaveBeenCalled()
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
      expect(saved.backfillFields).toBe('英文,俄文')
    })
  })
})
