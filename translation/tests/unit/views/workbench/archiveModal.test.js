/**
 * ArchiveModal 组件测试
 * 验证组件不再使用 this.user，而是使用 $store.state.user
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import ArchiveModal from '@/views/workbench/archiveModal.vue'
import CustomModal from '@/components/modal/index.vue'
import { createUserStoreMock, createNullUserStoreMock } from '../../testUtils/userStoreMock'

const { setInfoMock, updateTaskInfoMock, formValidateMock } = vi.hoisted(() => ({
  setInfoMock: vi.fn(),
  updateTaskInfoMock: vi.fn(),
  formValidateMock: vi.fn()
}))

vi.mock('@/http/api/i18Server', () => ({
  getI18nAdress: vi.fn(),
  setInfo: setInfoMock
}))

vi.mock('@/http/api/task', () => ({
  updateTaskInfo: updateTaskInfoMock
}))

vi.mock('@/http/api/workbench', () => ({
  getEntryInfoList: vi.fn(),
  getI18nAdress: vi.fn(),
  deleteEntryInfoByTaskID: vi.fn()
}))

// Mock 依赖
vi.mock('@/utils/domUtils', () => ({
  setModalAriaHidden: vi.fn(),
  createDragModalDirective: vi.fn(() => ({}))
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

const ArchiveCustomModalStub = defineComponent({
  name: 'ArchiveCustomModalStub',
  props: ['contentLoading', 'contentLoadingTip', 'okDisabled'],
  template: '<div class="archive-modal-stub"><slot /></div>'
})

const SpinStub = defineComponent({
  name: 'SpinStub',
  props: ['spinning', 'tip'],
  template: '<div class="spin-stub"><span v-if="spinning">{{ tip }}</span><slot /></div>'
})

const ButtonStub = defineComponent({
  name: 'ButtonStub',
  props: ['disabled', 'loading'],
  template: '<button :disabled="disabled"><slot /></button>'
})

const FormStub = defineComponent({
  name: 'FormStub',
  methods: {
    /**
     * 将表单校验调用转交给测试控制的 Promise。
     * @param {void} none 本方法不接收参数。
     * @returns {Promise<void>} 当前测试配置的校验结果。
     */
    validate() {
      return formValidateMock()
    }
  },
  template: '<form><slot /></form>'
})

const FormItemStub = defineComponent({
  name: 'FormItemStub',
  props: ['rules'],
  template: '<div><slot /></div>'
})

/**
 * 创建归档弹窗测试实例，并替换与归档 loading 无关的页面组件。
 * @param {Object} [props] 覆盖归档弹窗输入属性。
 * @returns {import('@vue/test-utils').VueWrapper} 挂载后的归档弹窗实例。
 */
function mountArchiveModal(props = {}) {
  return mount(ArchiveModal, {
    props: {
      visible: false,
      currentTask: { id: 'task-1', translateType: 'en' },
      ...props
    },
    global: {
      mocks: createUserStoreMock(),
      stubs: {
        CustomModal: ArchiveCustomModalStub,
        PipeShell: true,
        'a-form': FormStub,
        'a-form-item': FormItemStub,
        'a-button': true,
        'a-select': true
      }
    }
  })
}

describe('ArchiveModal - user 属性重构测试', () => {
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
      
      wrapper = mount(ArchiveModal, {
        props: {
          visible: true,
          currentTask: {}
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'PipeShell': true,
            'a-form': true,
            'a-form-item': true,
            'a-button': true
          }
        }
      })

      expect(wrapper.vm.user).toBeUndefined()
    })

    it('应该从 $store.state.user 获取用户信息', async () => {
      const testUser = {
        userName: 'testUser',
        department: '测试部门',
        roleName: '普通用户'
      }
      const storeMock = createUserStoreMock(testUser)
      
      wrapper = mount(ArchiveModal, {
        props: {
          visible: true,
          currentTask: {}
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'PipeShell': true,
            'a-form': true,
            'a-form-item': true,
            'a-button': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.$store.state.user).toEqual(testUser)
    })

    it('应该处理 $store.state.user 为 null 的情况', async () => {
      const storeMock = createNullUserStoreMock()
      
      wrapper = mount(ArchiveModal, {
        props: {
          visible: true,
          currentTask: {}
        },
        global: {
          mocks: storeMock,
          stubs: {
            'CustomModal': true,
            'PipeShell': true,
            'a-form': true,
            'a-form-item': true,
            'a-button': true
          }
        }
      })

      await nextTick()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.$store.state.user).toBeNull()
    })
  })
})

describe('ArchiveModal - 回写 loading 生命周期', () => {
  let wrapper

  beforeEach(() => {
    setInfoMock.mockReset()
    updateTaskInfoMock.mockReset()
    formValidateMock.mockReset().mockResolvedValue()
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = null
    vi.clearAllMocks()
  })

  it('归档请求 pending 时显示遮罩并阻止重复提交', async () => {
    let resolveWriteBack
    setInfoMock.mockReturnValue(new Promise(resolve => {
      resolveWriteBack = resolve
    }))
    wrapper = mountArchiveModal()
    wrapper.vm.selectedRows = [{ id: 'entry-1' }]
    wrapper.vm.ipModal.ip = 'http://archive.example'
    wrapper.vm.optionFlag = 0
    formValidateMock.mockResolvedValue()

    const firstSubmit = wrapper.vm.ipSelectOK()
    await flushPromises()

    expect(wrapper.vm.writeBackLoading).toBe(true)
    expect(wrapper.findComponent(ArchiveCustomModalStub).props('contentLoading')).toBe(true)
    expect(wrapper.findComponent(ArchiveCustomModalStub).props('okDisabled')).toBe(true)

    const duplicateSubmit = wrapper.vm.ipSelectOK()
    await flushPromises()
    expect(setInfoMock).toHaveBeenCalledTimes(1)

    resolveWriteBack({ data: {} })
    await Promise.all([firstSubmit, duplicateSubmit])

    expect(wrapper.vm.writeBackLoading).toBe(false)
    expect(wrapper.vm.ipSelectModal).toBe(false)
  })

  it('接口失败后解除遮罩并允许重试', async () => {
    setInfoMock.mockRejectedValueOnce(new Error('回写失败'))
      .mockResolvedValueOnce({ data: {} })
    wrapper = mountArchiveModal()
    wrapper.vm.selectedRows = [{ id: 'entry-1' }]
    wrapper.vm.ipModal.ip = 'http://archive.example'
    wrapper.vm.optionFlag = 0
    wrapper.vm.ipSelectModal = true
    formValidateMock.mockResolvedValue()

    await wrapper.vm.ipSelectOK()
    await flushPromises()

    expect(wrapper.vm.writeBackLoading).toBe(false)
    expect(wrapper.vm.ipSelectModal).toBe(true)

    await wrapper.vm.ipSelectOK()
    await flushPromises()

    expect(setInfoMock).toHaveBeenCalledTimes(2)
    expect(wrapper.vm.ipSelectModal).toBe(false)
  })

  it('表单 rules 校验失败时不显示全局提示、保留弹窗且不发请求', async () => {
    wrapper = mountArchiveModal()
    wrapper.vm.optionFlag = 0
    wrapper.vm.ipSelectModal = true
    formValidateMock.mockRejectedValue(new Error('请选择IP'))

    await wrapper.vm.ipSelectOK()
    await flushPromises()

    expect(wrapper.vm.writeBackLoading).toBe(false)
    expect(wrapper.vm.ipSelectModal).toBe(true)
    expect(formValidateMock).toHaveBeenCalledTimes(1)
    expect(message.error).not.toHaveBeenCalled()
    expect(setInfoMock).not.toHaveBeenCalled()
    expect(updateTaskInfoMock).not.toHaveBeenCalled()
    expect(wrapper.findComponent(FormItemStub).props('rules')).toEqual([
      { required: true, message: '请选择IP!' }
    ])
  })

  it('归档并结束任务时等待任务更新和回写都完成', async () => {
    let resolveTaskUpdate
    let resolveWriteBack
    updateTaskInfoMock.mockReturnValue(new Promise(resolve => {
      resolveTaskUpdate = resolve
    }))
    setInfoMock.mockReturnValue(new Promise(resolve => {
      resolveWriteBack = resolve
    }))
    wrapper = mountArchiveModal()
    wrapper.vm.task = { id: 'task-1', translateType: 'en' }
    wrapper.vm.ipModal.ip = 'http://archive.example'
    wrapper.vm.optionFlag = 1
    formValidateMock.mockResolvedValue()

    const submit = wrapper.vm.ipSelectOK()
    await flushPromises()
    expect(wrapper.vm.writeBackLoading).toBe(true)

    resolveTaskUpdate({ data: {} })
    await flushPromises()
    expect(wrapper.vm.writeBackLoading).toBe(true)

    resolveWriteBack({ data: {} })
    await submit

    expect(wrapper.vm.writeBackLoading).toBe(false)
  })
})

describe('CustomModal - 内容区 loading', () => {
  it('loading 时显示内容遮罩并禁用确认', () => {
    const wrapper = mount(CustomModal, {
      props: {
        modalVisible: true,
        contentLoading: true,
        contentLoadingTip: '正在归档，请稍候',
        okDisabled: true
      },
      global: {
        stubs: {
          'a-modal': { template: '<div><slot /><slot name="footer" /></div>' },
          'a-spin': SpinStub,
          'a-button': ButtonStub
        }
      }
    })

    expect(wrapper.findComponent(SpinStub).props('spinning')).toBe(true)
    expect(wrapper.findComponent(SpinStub).props('tip')).toBe('正在归档，请稍候')
    expect(wrapper.findAllComponents(ButtonStub).at(-1).props('disabled')).toBe(true)
    wrapper.unmount()
  })
})
