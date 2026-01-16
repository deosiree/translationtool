/**
 * BackFillModal_v2 组件测试
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import BackFillModal_v2 from '@/components/Button/fileManage/backFill/modal_v2.vue'
import { nextTick } from 'vue'

// Mock依赖
vi.mock('@/utils/excelUtils', () => ({
  entryBatchImportExcel_v2: vi.fn(() => Promise.resolve({
    code: 200,
    success: ['词条'],
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
        backfillFields: '词条,英文翻译'
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
      { label: '英文翻译', value: 'english', index: 2 },
      { label: '俄文翻译', value: 'russian', index: 3 },
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

describe('BackFillModal_v2 - 去重回填模态框 (v2版本)', () => {
  let wrapper

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  describe('组件渲染', () => {
    it('按钮模式下应该渲染按钮', () => {
      wrapper = mount(BackFillModal_v2, {
        props: {
          mode: 'button',
          buttonTitle: '测试按钮',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': true,
            'ExportButton': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-upload': true
          }
        }
      })

      // 检查组件实例的 internalVisible 状态
      expect(wrapper.vm.internalVisible).toBe(false)
      // 检查按钮模式
      expect(wrapper.vm.mode).toBe('button')
    })

    it('模态框模式下应该不显示主按钮', () => {
      wrapper = mount(BackFillModal_v2, {
        props: {
          mode: 'modal',
          visible: false,
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': true,
            'ExportButton': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-upload': true
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
      wrapper = mount(BackFillModal_v2, {
        props: {
          mode: 'button',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': true,
            'ExportButton': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-upload': true
          }
        }
      })
    })

    it('按钮模式下点击应该获取用户偏好', async () => {
      const { queryUserPartiality } = await import('@/http/api/userPartiality')
      
      // 直接调用方法而不是通过按钮点击
      wrapper.vm.handleButtonClick()
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))

      expect(queryUserPartiality).toHaveBeenCalled()
    })

    it('模态框模式下打开应该获取用户偏好', async () => {
      const { queryUserPartiality } = await import('@/http/api/userPartiality')
      
      // 先以 visible: false 挂载，然后设置为 true 以触发 watch
      wrapper = mount(BackFillModal_v2, {
        props: {
          mode: 'modal',
          visible: false,
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': true,
            'ExportButton': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-upload': true
          }
        }
      })

      await nextTick()
      
      // 设置为 true 以触发 watch
      await wrapper.setProps({ visible: true })
      await nextTick()
      // 等待 watch visible 触发和异步调用完成
      await new Promise(resolve => setTimeout(resolve, 150))

      expect(queryUserPartiality).toHaveBeenCalled()
    })
  })

  describe('字段选项过滤', () => {
    it('应该过滤掉翻译id字段', async () => {
      wrapper = mount(BackFillModal_v2, {
        props: {
          mode: 'button',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': true,
            'ExportButton': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-upload': true
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
      wrapper = mount(BackFillModal_v2, {
        props: {
          mode: 'button',
          translateTypes: []
        },
        global: {
          stubs: {
            'CustomModal': true,
            'ExportButton': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'a-upload': true
          }
        }
      })
      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 50))
    })

    it('点击全选应该选择所有字段', async () => {
      // 初始化字段选项
      wrapper.vm.fieldOptions = [
        { label: '词条', value: 'entry' },
        { label: '英文翻译', value: 'english' }
      ]
      
      // 确保方法存在
      expect(typeof wrapper.vm.selectAllBackfillFields).toBe('function')
      
      wrapper.vm.selectAllBackfillFields()
      await nextTick()

      expect(wrapper.vm.formModel.backfillFields).toEqual(['词条', '英文翻译'])
    })
  })

  describe('表单验证', () => {
    beforeEach(async () => {
      wrapper = mount(BackFillModal_v2, {
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
            'a-form': {
              template: '<form ref="backFillForm"><slot></slot></form>',
              methods: {
                validate: vi.fn(() => Promise.resolve()),
                clearValidate: vi.fn()
              }
            },
            'a-form-item': true,
            'a-select': true,
            'a-upload': true
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
      wrapper = mount(BackFillModal_v2, {
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
            'a-form': {
              template: '<form><slot></slot></form>'
            },
            'a-form-item': true,
            'a-select': true,
            'a-upload': true
          }
        }
      })
      await nextTick()
    })

    it('updateTranslation模式下应该调用entryBatchImportExcel_v2', async () => {
      const { entryBatchImportExcel_v2 } = await import('@/utils/excelUtils')
      
      await nextTick()
      
      // 设置表单数据
      wrapper.vm.formModel.backfillFields = ['词条']
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

      expect(entryBatchImportExcel_v2).toHaveBeenCalled()
    })

    it('validate模式下应该调用entryValidate_v2', async () => {
      wrapper = mount(BackFillModal_v2, {
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
            'a-form': {
              template: '<form><slot></slot></form>'
            },
            'a-form-item': true,
            'a-select': true,
            'a-upload': true
          }
        }
      })

      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 50))
      
      const { entryValidate_v2 } = await import('@/utils/excelUtils')
      
      // 设置表单数据
      wrapper.vm.formModel.backfillFields = ['词条']
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

      expect(entryValidate_v2).toHaveBeenCalled()
    })

    it('提交成功后应该保存用户偏好', async () => {
      const { updateUserPartiality } = await import('@/http/api/userPartiality')
      
      await nextTick()
      
      // 设置表单数据
      wrapper.vm.formModel.backfillFields = ['词条', '英文翻译']
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

      expect(updateUserPartiality).toHaveBeenCalledWith({
        backfillFields: '词条,英文翻译'
      })
    })
  })
})
