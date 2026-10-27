import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, nextTick } from 'vue'
import BatchPreTranslateModal from '@/views/workbench/components/BatchPreTranslateModal.vue'
import { ARCHIVE_MODE, ARCHIVE_MODES } from '@/constants/batchPreTranslateSteps'

const { confirmMock, executeMock, getI18nAdressMock, validateMock } = vi.hoisted(() => ({
  confirmMock: vi.fn(),
  executeMock: vi.fn(),
  getI18nAdressMock: vi.fn(),
  validateMock: vi.fn()
}))

vi.mock('ant-design-vue', () => ({
  Modal: { confirm: confirmMock },
  message: {
    error: vi.fn(),
    warning: vi.fn()
  }
}))

vi.mock('@/components/modal/index.vue', async () => {
  const { defineComponent: createComponent } = await import('vue')
  return {
    default: createComponent({
      name: 'ModalStub',
    props: {
      okLoading: { type: Boolean, default: false }
    },
      template: '<div class="modal-stub"><slot /></div>'
    })
  }
})

vi.mock('@/components/Dropdown/rulesDropdown.vue', async () => {
  const { defineComponent: createComponent } = await import('vue')
  return {
    default: createComponent({
      name: 'RulesDropdownStub',
      template: '<div class="rules-dropdown-stub" />'
    })
  }
})

vi.mock('@/utils/domUtils', () => ({
  setModalAriaHidden: vi.fn()
}))

vi.mock('@/http/api/workbench', () => ({
  getI18nAdress: getI18nAdressMock
}))

vi.mock('@/composables/workbench/useBatchPreTranslate', () => ({
  useBatchPreTranslate: () => ({ execute: executeMock })
}))

const FormItemStub = defineComponent({
  name: 'FormItemStub',
  props: ['label', 'name', 'rules'],
  template: `
    <div class="form-item" :data-name="name" :data-required="rules?.some(rule => rule.required)">
      <span class="form-item-label">{{ label }}</span>
      <slot />
    </div>
  `
})

const LayoutStub = defineComponent({
  name: 'LayoutStub',
  template: '<div><slot /></div>'
})

/**
 * 挂载批量预翻译弹窗，并提供确认、API、表单及 store 的可控替身。
 * @returns {{wrapper: import('@vue/test-utils').VueWrapper, store: Object}} 弹窗包装器和模拟 store。
 */
function mountBatchPreTranslateModal() {
  const store = {
    dispatch: vi.fn(),
    getters: { 'batchProgress/isRunning': false }
  }
  const wrapper = mount(BatchPreTranslateModal, {
    props: {
      visible: true,
      tasks: [{ id: 'task-1', name: '任务一' }]
    },
    global: {
      mocks: { $store: store },
      stubs: {
        'a-form': LayoutStub,
        'a-form-item': FormItemStub,
        'a-row': LayoutStub,
        'a-col': LayoutStub,
        'a-select': LayoutStub,
        'a-input-number': LayoutStub,
        'a-tooltip': LayoutStub,
        'a-tag': LayoutStub
      }
    }
  })
  wrapper.vm.$refs.execForm.validate = validateMock
  return { wrapper, store }
}

describe('BatchPreTranslateModal archive configuration', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    confirmMock.mockReset()
    validateMock.mockResolvedValue(undefined)
    executeMock.mockResolvedValue(undefined)
    getI18nAdressMock.mockResolvedValue({ data: { list: [{ ip: '10.0.0.1' }] } })
  })

  it('先显示归档方式；两种回写模式要求 IP，纯结束任务隐藏并清空 IP', async () => {
    const { wrapper } = mountBatchPreTranslateModal()
    wrapper.vm.model.stages.archive = true
    await nextTick()
    await flushPromises()

    const archiveFields = wrapper.findAll('.form-item')
      .filter(item => ['archiveMode', 'archiveIp'].includes(item.attributes('data-name')))
    expect(archiveFields.map(item => item.find('.form-item-label').text()))
      .toEqual(['归档方式', 'IP'])
    expect(archiveFields[1].attributes('data-required')).toBe('true')
    expect(wrapper.vm.archiveModes).toEqual(ARCHIVE_MODES)

    wrapper.vm.model.archiveIp = '10.0.0.1'
    wrapper.vm.model.archiveMode = ARCHIVE_MODE.WRITE_END
    await nextTick()
    expect(wrapper.find('[data-name="archiveIp"]').exists()).toBe(true)

    wrapper.vm.model.archiveMode = ARCHIVE_MODE.END_TASK
    await nextTick()
    expect(wrapper.find('[data-name="archiveIp"]').exists()).toBe(false)
    expect(wrapper.vm.model.archiveIp).toBeNull()
    wrapper.unmount()
  })

  it('归档阶段点击确定先确认；取消时不执行也不关闭配置弹窗', async () => {
    const { wrapper } = mountBatchPreTranslateModal()
    wrapper.vm.model.stages.archive = true
    wrapper.vm.model.archiveIp = '10.0.0.1'

    await wrapper.vm.handleExecute()

    expect(confirmMock).toHaveBeenCalledOnce()
    const options = confirmMock.mock.calls[0][0]
    expect(options.title).toBe('是否确认批量归档？')
    expect(executeMock).not.toHaveBeenCalled()
    expect(wrapper.emitted('update:visible')).toBeUndefined()

    options.onCancel()
    expect(wrapper.vm.submitting).toBe(false)
    expect(executeMock).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('确认后立即关闭配置弹窗，执行期间不显示阻塞式 loading', async () => {
    const { wrapper } = mountBatchPreTranslateModal()
    wrapper.vm.model.stages.archive = true
    wrapper.vm.model.archiveIp = '10.0.0.1'
    let finishExecution
    executeMock.mockImplementationOnce(() => new Promise(resolve => {
      finishExecution = resolve
    }))

    await wrapper.vm.handleExecute()
    const onConfirm = confirmMock.mock.calls[0][0].onOk
    const confirmResult = onConfirm()
    await nextTick()

    expect(confirmResult).toBeUndefined()
    expect(wrapper.emitted('update:visible')).toEqual([[false]])
    expect(wrapper.findComponent({ name: 'ModalStub' }).props('okLoading')).toBe(false)

    finishExecution()
    await flushPromises()
    wrapper.unmount()
  })

  it('结束任务无需 IP；确认后启动流程并沿用现有结束任务提示', async () => {
    const { wrapper, store } = mountBatchPreTranslateModal()
    wrapper.vm.model.stages.archive = true
    wrapper.vm.model.archiveMode = ARCHIVE_MODE.END_TASK
    wrapper.vm.model.archiveIp = null
    await nextTick()

    await wrapper.vm.handleExecute()

    const options = confirmMock.mock.calls[0][0]
    expect(options.title).toBe('是否结束所选任务？')
    expect(options.content).toContain('不会回写词条到服务器')
    expect(executeMock).not.toHaveBeenCalled()

    await options.onOk()

    expect(store.dispatch).toHaveBeenCalledWith('batchProgress/start', {
      config: expect.objectContaining({
        archiveIp: null,
        archiveMode: ARCHIVE_MODE.END_TASK
      }),
      tasks: [{ id: 'task-1', name: '任务一' }]
    })
    expect(executeMock).toHaveBeenCalledOnce()
    expect(wrapper.emitted('update:visible')).toEqual([[false]])
    wrapper.unmount()
  })

  it('归档阶段未勾选时不弹归档确认框，表单无效时也不启动', async () => {
    const { wrapper } = mountBatchPreTranslateModal()
    await wrapper.vm.handleExecute()
    expect(confirmMock).not.toHaveBeenCalled()
    expect(executeMock).toHaveBeenCalledOnce()

    wrapper.unmount()
    executeMock.mockClear()
    const invalid = mountBatchPreTranslateModal()
    invalid.wrapper.vm.model.stages.archive = true
    invalid.wrapper.vm.model.archiveIp = '10.0.0.1'
    validateMock.mockRejectedValueOnce(new Error('表单无效'))

    await invalid.wrapper.vm.handleExecute()

    expect(confirmMock).not.toHaveBeenCalled()
    expect(executeMock).not.toHaveBeenCalled()
    expect(invalid.wrapper.vm.submitting).toBe(false)
    invalid.wrapper.unmount()
  })

  it('三种归档方式都提供与操作相符的确认文案', () => {
    const { wrapper } = mountBatchPreTranslateModal()
    const config = { archiveMode: ARCHIVE_MODE.WRITE_END }

    expect(wrapper.vm.getArchiveConfirmOptions(config).title).toBe('是否确认归档并结束任务？')
    expect(wrapper.vm.getArchiveConfirmOptions({ archiveMode: ARCHIVE_MODE.END_TASK }).content)
      .toContain('不会回写词条到服务器')
    expect(wrapper.vm.getArchiveConfirmOptions({ archiveMode: ARCHIVE_MODE.WRITE }).content)
      .toContain('任务不会结束')
    wrapper.unmount()
  })

  it('翻译方式只在翻译阶段启用时显示和校验，隐藏后保留原值', async () => {
    const { wrapper } = mountBatchPreTranslateModal()
    const initialPriority = wrapper.vm.model.translatePriority

    expect(wrapper.find('[data-name="translatePriority"]').exists()).toBe(true)
    expect(wrapper.find('[data-name="translatePriority"]').attributes('data-required')).toBe('true')

    wrapper.vm.model.stages.preTranslate = false
    await nextTick()

    expect(wrapper.find('[data-name="translatePriority"]').exists()).toBe(false)
    expect(wrapper.vm.model.translatePriority).toBe(initialPriority)

    wrapper.vm.model.stages.preTranslate = true
    await nextTick()

    expect(wrapper.find('[data-name="translatePriority"]').exists()).toBe(true)
    expect(wrapper.vm.model.translatePriority).toBe(initialPriority)
    wrapper.unmount()
  })

  it('并发数和重试次数始终显示在独立于翻译方式的配置行', async () => {
    const { wrapper } = mountBatchPreTranslateModal()
    const translateRow = wrapper.find('.translate-config-row')
    const taskRow = wrapper.find('.task-execution-config-row')

    expect(translateRow.exists()).toBe(true)
    expect(taskRow.exists()).toBe(true)
    expect(translateRow.find('[data-name="translatePriority"]').exists()).toBe(true)
    expect(taskRow.find('[data-name="concurrency"]').exists()).toBe(true)
    expect(taskRow.find('[data-name="maxRetries"]').exists()).toBe(true)

    wrapper.vm.model.stages.preTranslate = false
    await nextTick()

    expect(wrapper.find('.translate-config-row').exists()).toBe(false)
    expect(wrapper.find('.task-execution-config-row [data-name="concurrency"]').exists()).toBe(true)
    expect(wrapper.find('.task-execution-config-row [data-name="maxRetries"]').exists()).toBe(true)
    expect(wrapper.find('.task-execution-config-row [data-name="concurrency"]').attributes('data-required'))
      .toBe('true')
    expect(wrapper.find('.task-execution-config-row [data-name="maxRetries"]').attributes('data-required'))
      .toBe('true')
    wrapper.unmount()
  })
})
