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
  },
}))

import { message } from 'ant-design-vue'
import WriteBackForm from '@/views/entry/createVersionModal/WriteBackForm.vue'

const SlotStub = {
  template: '<div><slot /><slot name="title" /></div>',
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
        CustomModal: SlotStub,
        QuestionCircleOutlined: true,
        'a-spin': SlotStub,
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
    localStorage.clear()
    writeBackMock.mockReset()
    formValidateMock.mockReset().mockResolvedValue([])
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
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
    expect(wrapper.vm.writeBackLoading).toBe(false)
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
    writeBackMock.mockResolvedValue({ data: 'OK' })

    await wrapper.vm.handleOK()

    expect(writeBackMock).toHaveBeenCalledWith(
      expect.objectContaining({
        translateType: '英文',
        i18nUrl: 'http://i18n.example.test',
      }),
      [],
    )
    expect(wrapper.emitted('handleClose')).toHaveLength(1)
    expect(wrapper.vm.writeBackLoading).toBe(false)
  })
})
