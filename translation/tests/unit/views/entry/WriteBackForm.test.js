import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'

const writeBackMock = vi.hoisted(() => vi.fn())
const formValidateMock = vi.hoisted(() => vi.fn())

vi.mock('@/utils/domUtils', () => ({
  setModalAriaHidden: vi.fn(),
}))

vi.mock('@/http/api/entryManage', () => ({
  writeBack: writeBackMock,
}))

vi.mock('@/http/api/workbench.js', () => ({
  getI18nAdress: vi.fn(() => Promise.resolve({ data: { list: [] } })),
}))

vi.mock('@/http/api/i18Server', () => ({
  getFileListByLang: vi.fn(() => Promise.resolve({ data: { list: [] } })),
  getDictionary: vi.fn(() => Promise.resolve({ data: { list: [] } })),
}))

vi.mock('ant-design-vue', () => ({
  message: {
    info: vi.fn(),
    error: vi.fn(),
  },
  notification: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn(),
  },
}))

import { message, notification } from 'ant-design-vue'
import WriteBackForm from '@/views/entry/createVersionModal/WriteBackForm.vue'
import { isLoading, resetLoading } from '@/composables/useLoading'

const SlotStub = {
  template: '<div><slot /><slot name="title" /></div>',
}

const CustomModalStub = {
  props: ['okLoading'],
  template: '<div><slot /><slot name="title" /></div>',
}

const SpinStub = {
  props: ['spinning'],
  template: '<div><slot /></div>',
}

const FormStub = {
  name: 'AFormStub',
  methods: {
    /**
     * 转发测试配置的表单校验结果。
     * @param {...any} args - 表单校验参数。
     * @returns {Promise<any>} 测试配置的校验结果。
     */
    validate(...args) {
      return formValidateMock(...args)
    },
  },
  template: '<form><slot /></form>',
}

/**
 * 挂载回写表单，并用轻量组件替身隔离 Ant Design Vue 的渲染行为。
 * @param {boolean} visible - 是否显示回写弹窗。
 * @returns {import('@vue/test-utils').VueWrapper} 已挂载的回写表单。
 */
function mountWriteBackForm(visible = true) {
  return mount(WriteBackForm, {
    props: { visible },
    global: {
      stubs: {
        CustomModal: CustomModalStub,
        QuestionCircleOutlined: true,
        'a-spin': SpinStub,
        'a-form': FormStub,
        'a-form-item': SlotStub,
        'a-select': true,
        'a-radio-group': SlotStub,
        'a-radio': true,
        'a-checkbox': true,
        'a-tooltip': SlotStub,
      },
    },
  })
}

describe('WriteBackForm - 表单校验与弹窗关闭', () => {
  let wrapper

  beforeEach(() => {
    resetLoading()
    localStorage.clear()
    writeBackMock.mockReset()
    formValidateMock.mockReset().mockResolvedValue([])
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
    vi.restoreAllMocks()
    resetLoading()
  })

  it.each([
    ['IP', 'ip'],
    ['回写语种', 'language'],
  ])('缺少%s时保留弹窗且不发起回写', async (_label, field) => {
    wrapper = mountWriteBackForm()
    formValidateMock.mockRejectedValue({
      errorFields: [{ name: [field] }],
    })

    await wrapper.vm.handleOK()

    expect(formValidateMock).toHaveBeenCalledOnce()
    expect(wrapper.emitted('handleClose')).toBeUndefined()
    expect(writeBackMock).not.toHaveBeenCalled()
    expect(isLoading()).toBe(false)
  })

  it.each([
    ['TS', 'ts文件'],
    ['DI', '辞典'],
  ])('%s 类型未选择目标文件时保留弹窗且不执行表单提交', async (type, label) => {
    wrapper = mountWriteBackForm()
    wrapper.vm.writeBack.type = type
    wrapper.vm.writeBack.label = label

    await wrapper.vm.handleOK()

    expect(message.info).toHaveBeenCalledWith(`请选择${label}!`)
    expect(formValidateMock).not.toHaveBeenCalled()
    expect(wrapper.emitted('handleClose')).toBeUndefined()
    expect(writeBackMock).not.toHaveBeenCalled()
  })

  it('校验通过并完成回写后仍按现有流程关闭弹窗', async () => {
    wrapper = mountWriteBackForm()
    wrapper.vm.writeBack.ip = 'http://i18n.example.test'
    wrapper.vm.writeBack.language = ['英文']
    writeBackMock.mockResolvedValue({ code: 200, data: '后端返回的成功详情' })

    await wrapper.vm.handleOK()
    await wrapper.vm.$nextTick()

    expect(writeBackMock).toHaveBeenCalledWith(
      expect.objectContaining({
        translateType: '英文',
        i18nUrl: 'http://i18n.example.test',
      }),
      [],
    )
    expect(notification.success).toHaveBeenCalledWith({
      message: '回写结果',
      description: '成功 1 个：英文',
      duration: 0,
    })
    expect(wrapper.emitted('handleClose')).toHaveLength(1)
    expect(isLoading()).toBe(false)
  })

  it('请求进行中共享遮罩和确认 loading 生效，重复提交与关闭被拦截', async () => {
    wrapper = mountWriteBackForm()
    wrapper.vm.writeBack.ip = 'http://i18n.example.test'
    wrapper.vm.writeBack.language = ['英文']
    let resolveRequest
    writeBackMock.mockReturnValue(
      new Promise((resolve) => {
        resolveRequest = resolve
      }),
    )

    const firstSubmit = wrapper.vm.handleOK()
    await wrapper.vm.handleOK()
    await vi.waitFor(() => expect(writeBackMock).toHaveBeenCalledOnce())

    expect(isLoading()).toBe(true)
    expect(wrapper.findComponent(SpinStub).props('spinning')).toBe(true)
    expect(wrapper.findComponent(CustomModalStub).props('okLoading')).toBe(true)
    wrapper.vm.handleClose()
    expect(wrapper.emitted('handleClose')).toBeUndefined()

    resolveRequest({ code: 200, data: '' })
    await firstSubmit
    await wrapper.vm.$nextTick()

    expect(isLoading()).toBe(false)
    expect(wrapper.findComponent(SpinStub).props('spinning')).toBe(false)
    expect(wrapper.emitted('handleClose')).toHaveLength(1)
  })

  it('同步请求异常时仍释放共享 loading', async () => {
    wrapper = mountWriteBackForm()
    wrapper.vm.writeBack.ip = 'http://i18n.example.test'
    wrapper.vm.writeBack.language = ['英文']
    vi.spyOn(console, 'log').mockImplementation(() => {})
    writeBackMock.mockImplementationOnce(() => {
      throw new Error('同步请求异常')
    })

    await wrapper.vm.handleOK()

    expect(writeBackMock).toHaveBeenCalledOnce()
    expect(isLoading()).toBe(false)
    expect(wrapper.emitted('handleClose')).toHaveLength(1)
  })

  it('仅部分成功时将语种计入部分成功，并显示持续通知', async () => {
    wrapper = mountWriteBackForm()
    wrapper.vm.writeBack.ip = 'http://i18n.example.test'
    wrapper.vm.writeBack.language = ['俄文']
    writeBackMock.mockResolvedValue({ code: 203, data: '部分成功详情未使用固定文案' })
    let closeEmittedWhenNotified = 0
    notification.warning.mockImplementationOnce(() => {
      closeEmittedWhenNotified = wrapper.emitted('handleClose')?.length ?? 0
    })

    await wrapper.vm.handleOK()
    await wrapper.vm.$nextTick()

    expect(notification.warning).toHaveBeenCalledWith({
      message: '回写结果',
      description: '部分成功 1 个：俄文',
      duration: 0,
    })
    expect(notification.success).not.toHaveBeenCalled()
    expect(notification.error).not.toHaveBeenCalled()
    expect(closeEmittedWhenNotified).toBe(1)
    expect(isLoading()).toBe(false)
  })

  it('混合成功、部分成功和请求失败时在同一通知中汇总全部语种', async () => {
    wrapper = mountWriteBackForm()
    wrapper.vm.writeBack.ip = 'http://i18n.example.test'
    wrapper.vm.writeBack.language = ['英文', '俄文', '西文']
    writeBackMock
      .mockResolvedValueOnce({ code: 200, data: '部分更新成功：此文案不应改变 code 分类' })
      .mockResolvedValueOnce({ code: 203, data: '其他部分成功详情' })
      .mockRejectedValueOnce(new Error('网络错误'))

    await wrapper.vm.handleOK()
    await wrapper.vm.$nextTick()

    expect(notification.warning).toHaveBeenCalledOnce()
    expect(notification.warning).toHaveBeenCalledWith({
      message: '回写结果',
      description: '成功 1 个：英文；部分成功 1 个：俄文；失败 1 个：西文',
      duration: 0,
    })
    expect(notification.success).not.toHaveBeenCalled()
    expect(notification.error).not.toHaveBeenCalled()
    expect(message.error).not.toHaveBeenCalled()
    expect(wrapper.emitted('handleClose')).toHaveLength(1)
    expect(isLoading()).toBe(false)
  })

  it('未知响应和请求拒绝都计入失败，全部失败时显示错误通知', async () => {
    wrapper = mountWriteBackForm()
    wrapper.vm.writeBack.ip = 'http://i18n.example.test'
    wrapper.vm.writeBack.language = ['英文', '俄文']
    writeBackMock
      .mockResolvedValueOnce({ code: 201, data: 'OK' })
      .mockRejectedValueOnce(new Error('网络错误'))

    await wrapper.vm.handleOK()
    await wrapper.vm.$nextTick()

    expect(notification.error).toHaveBeenCalledWith({
      message: '回写结果',
      description: '失败 2 个：英文, 俄文',
      duration: 0,
    })
    expect(notification.success).not.toHaveBeenCalled()
    expect(notification.warning).not.toHaveBeenCalled()
    expect(isLoading()).toBe(false)
  })
})
