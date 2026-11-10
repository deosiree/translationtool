import { afterEach, describe, expect, it } from 'vitest'
import { h } from 'vue'
import { mount } from '@vue/test-utils'
import CustomModal from '@/components/modal/index.vue'

const ModalStub = {
  props: ['wrapClassName', 'keyboard'],
  emits: ['cancel'],
  /**
   * 以实际弹窗 DOM 层级承载 CustomModal 插槽，便于检查整窗遮罩位置。
   * @param {Object} props - 弹窗组件属性。
   * @param {Object} context - Vue 插槽上下文。
   * @returns {import('vue').VNode} 模拟的弹窗根节点。
   */
  setup(props, { slots }) {
    return () => {
      const originVNode = h('section', { class: 'ant-modal' }, [
        h('div', { class: 'ant-modal-content' }, [
          h('header', '标题'),
          ...(slots.default?.() ?? []),
          h('footer', { class: 'ant-modal-footer' }, slots.footer?.()),
        ]),
      ])
      return h(
        'div',
        { class: ['ant-modal-wrap', props.wrapClassName] },
        slots.modalRender?.({ originVNode }),
      )
    }
  },
}

const SpinStub = {
  template: '<div class="spin-stub"><slot /></div>',
}

const ButtonStub = {
  props: ['loading', 'disabled'],
  template: '<button :disabled="disabled" :data-loading="loading"><slot /></button>',
}

describe('CustomModal', () => {
  let wrapper

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  it('busy 遮罩避开标题栏、覆盖 footer 并保留弹窗底部圆角', async () => {
    wrapper = mount(CustomModal, {
      props: { modalVisible: true, modalLoading: true },
      slots: { default: '表单内容' },
      global: {
        stubs: {
          'a-modal': ModalStub,
          'a-spin': SpinStub,
          'a-button': ButtonStub,
          ExpandOutlined: true,
          ExpandAltOutlined: true,
        },
      },
    })

    const overlay = wrapper.find('.modal-loading-overlay')
    expect(overlay.exists()).toBe(true)
    expect(overlay.element.style.top).toBe('35px')
    expect(overlay.element.style.bottom).toBe('0px')
    expect(overlay.element.style.borderRadius.split(/\s+/).map(parseFloat)).toEqual([0, 0, 8, 8])
    expect(overlay.element.contains(wrapper.find('.modalHeader').element)).toBe(false)
    const modalContent = overlay.element.closest('.ant-modal-content')
    expect(modalContent).not.toBeNull()
    expect(modalContent.querySelector('.ant-modal-footer')).not.toBeNull()
    expect(wrapper.findAll('button').every((button) => button.element.disabled)).toBe(true)
    expect(wrapper.findAll('button').at(-1).attributes('data-loading')).toBe('false')
    expect(wrapper.find('.ant-modal-wrap').classes()).toContain('modal-loading')
    expect(wrapper.findComponent(ModalStub).props('keyboard')).toBe(false)

    wrapper.vm.ok()
    wrapper.vm.cancel()
    wrapper.findComponent(ModalStub).vm.$emit('cancel')
    await wrapper.find('.close').trigger('click')

    expect(wrapper.emitted('handleOK')).toBeUndefined()
    expect(wrapper.emitted('handleClose')).toBeUndefined()
  })
})
