/**
 * 预翻译配置弹窗测试
 * 覆盖：仅保留翻译语种与翻译优先级；校验规则已上移到批量选择弹窗顶部
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'

vi.mock('@/utils/domUtils', () => ({
  setModalAriaHidden: vi.fn(),
}))

import PreTranslateForm from '@/views/entry/createVersionModal/PreTranslateForm.vue'

const ModalStub = {
  name: 'CustomModalStub',
  props: ['modalVisible'],
  template: '<div><slot /></div>',
}

const FormStub = {
  name: 'AFormStub',
  methods: {
    validate: vi.fn().mockResolvedValue(true),
  },
  template: '<form><slot /></form>',
}

const FormItemStub = {
  name: 'AFormItemStub',
  props: ['label', 'name'],
  template: '<div class="form-item"><span class="form-label">{{ label }}</span><slot /></div>',
}

const SelectStub = {
  name: 'ASelectStub',
  props: ['value', 'options', 'mode', 'placeholder', 'maxTagCount', 'allowClear'],
  template: '<div class="a-select-stub"></div>',
}

function mountForm(visible = true) {
  return mount(PreTranslateForm, {
    props: { visible },
    global: {
      stubs: {
        CustomModal: ModalStub,
        'a-form': FormStub,
        'a-form-item': FormItemStub,
        'a-select': SelectStub,
      },
    },
  })
}

describe('PreTranslateForm - 预翻译配置', () => {
  let wrapper

  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
    vi.clearAllMocks()
  })

  it('只渲染翻译语种与翻译优先级，不渲染校验规则', () => {
    wrapper = mountForm(true)

    expect(wrapper.findAll('.form-label').map((item) => item.text())).toEqual([
      '翻译语种',
      '翻译优先级'
    ])
    expect(wrapper.text()).not.toContain('校验规则')
    expect(wrapper.find('.rules-select').exists()).toBe(false)
  })

  it('提交仅携带翻译语种与翻译优先级', async () => {
    wrapper = mountForm(true)
    wrapper.vm.preTran.language = ['英文']
    wrapper.vm.preTran.priority = 'shuyuku'

    await wrapper.vm.handleOK()

    const submitted = wrapper.emitted('submit')?.[0]?.[0]
    expect(submitted).toEqual({
      language: ['英文'],
      priority: 'shuyuku'
    })
  })
})
