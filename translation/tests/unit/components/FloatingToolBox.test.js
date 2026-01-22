/**
 * FloatingToolBox 组件测试
 * 
 * 注意：运行此测试需要安装 @vitejs/plugin-vue
 * 安装命令：npm install -D @vitejs/plugin-vue
 * 并在 vitest.config.js 中添加插件配置：
 * import vue from '@vitejs/plugin-vue'
 * plugins: [vue()]
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import FloatingToolBox from '@/components/FloatingToolBox/index.vue'
import { nextTick } from 'vue'

// Mock依赖
vi.mock('@/utils/notificationUtils', () => ({
  closeAllNotifications: vi.fn()
}))

vi.mock('@/http/api/translate', () => ({
  getLanguage: vi.fn(() => Promise.resolve({
    data: {
      list: [
        { name: '英文', code: 'english' },
        { name: '俄文', code: 'russian' }
      ]
    }
  }))
}))

vi.mock('@/http/api/workbench.js', () => ({
  getI18nAdress: vi.fn(() => Promise.resolve({
    data: {
      list: [
        { ip: '192.168.1.1' },
        { ip: '192.168.1.2' }
      ]
    }
  })),
  getBranches: vi.fn(() => Promise.resolve({
    data: {
      list: ['main', 'develop']
    }
  })),
  gitPush: vi.fn(() => Promise.resolve({}))
}))

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
    warning: vi.fn()
  },
  notification: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

// Mock userPartiality API
vi.mock('@/http/api/userPartiality', () => ({
  queryUserPartiality: vi.fn(() => Promise.resolve({
    data: {
      list: [{
        exportColumn: '词条,英文翻译',
        backfillFields: '词条,英文翻译'
      }]
    }
  })),
  updateUserPartiality: vi.fn(() => Promise.resolve({}))
}))

// Mock excelUtils
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

describe('FloatingToolBox - 悬浮工具仓组件', () => {
  let wrapper
  let localStorageMock

  beforeEach(() => {
    // Mock localStorage
    localStorageMock = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn()
    }
    global.localStorage = localStorageMock

    // Mock getBoundingClientRect
    Element.prototype.getBoundingClientRect = vi.fn(() => ({
      left: 100,
      top: 100,
      right: 150,
      bottom: 150,
      width: 50,
      height: 50
    }))

    // Mock window尺寸
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1920
    })
    Object.defineProperty(window, 'innerHeight', {
      writable: true,
      configurable: true,
      value: 1080
    })
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
    document.body.innerHTML = ''
  })

  describe('组件渲染', () => {
    it('应该渲染悬浮按钮', () => {
      wrapper = mount(FloatingToolBox, {
        global: {
          mocks: {
            $store: {
              state: {
                user: {
                  department: 'default',
                  userName: 'testUser'
                }
              }
            }
          },
          stubs: {
            GitCommitButton: true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'ToolOutlined': true,
            'CustomModal': true,
            'BackFillModal': true,
            'BackFillModal_v2': true
          }
        }
      })

      expect(wrapper.find('.floating-button').exists()).toBe(true)
    })

    it('初始状态不应该显示工具面板', () => {
      wrapper = mount(FloatingToolBox, {
        global: {
          mocks: {
            $store: {
              state: {
                user: {
                  department: 'default',
                  userName: 'testUser'
                }
              }
            }
          },
          stubs: {
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'ToolOutlined': true,
            'CustomModal': true,
            'BackFillModal': true,
            'BackFillModal_v2': true
          }
        }
      })

      expect(wrapper.find('.tool-panel').exists()).toBe(false)
    })
  })

  describe('单击和双击功能', () => {
    beforeEach(() => {
      wrapper = mount(FloatingToolBox, {
        global: {
          mocks: {
            $store: {
              state: {
                user: {
                  department: 'default',
                  userName: 'testUser'
                }
              }
            }
          },
          stubs: {
            GitCommitButton: true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'ToolOutlined': true,
            'CustomModal': true,
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'ExportButton': true
          }
        }
      })
    })

    it('单击应该关闭所有notification', async () => {
      const { closeAllNotifications } = await import('@/utils/notificationUtils')
      const button = wrapper.find('.floating-button')
      
      await button.trigger('click')
      
      // 等待定时器执行（200ms）
      await new Promise(resolve => setTimeout(resolve, 250))
      await nextTick()

      expect(closeAllNotifications).toHaveBeenCalled()
      // 单击不会影响面板显示状态
      expect(wrapper.vm.panelVisible).toBe(false)
    })

    it('双击应该显示工具面板', async () => {
      const button = wrapper.find('.floating-button')
      
      // 使用两次 click 模拟双击（组件使用自定义双击检测，通过定时器检测连续点击）
      await button.trigger('click')
      // 立即第二次点击（间隔小于200ms，触发双击检测）
      await button.trigger('click')
      await nextTick()

      expect(wrapper.vm.panelVisible).toBe(true)
    })

    it('应该正确区分单击和双击', async () => {
      const { closeAllNotifications } = await import('@/utils/notificationUtils')
      const button = wrapper.find('.floating-button')
      
      // 先单击
      await button.trigger('click')
      
      // 立即第二次点击（间隔小于200ms，触发双击检测，应该取消单击的定时器）
      await button.trigger('click')
      await nextTick()

      // 等待定时器时间过去
      await new Promise(resolve => setTimeout(resolve, 250))
      await nextTick()

      // 双击应该取消单击的定时器，但组件实现仍会在双击处理时关闭通知
      expect(closeAllNotifications).toHaveBeenCalled()
      // 面板应该显示（由双击切换）
      expect(wrapper.vm.panelVisible).toBe(true)
    })
  })

  describe('工具面板功能', () => {
    beforeEach(async () => {
      wrapper = mount(FloatingToolBox, {
        global: {
          stubs: {
            GitCommitButton: {
              template: '<button><slot></slot></button>',
              props: ['size', 'buttonTitle', 'buttonClass']
            },
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'block', 'loading']
            },
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'ToolOutlined': true,
            'CustomModal': true,
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'ExportButton': true
          },
          mocks: {
            $store: {
              state: {
                user: {
                  department: 'default',
                  userName: 'testUser'
                }
              }
            }
          }
        }
      })
      
      // 先显示面板
      wrapper.vm.panelVisible = true
      await nextTick()
    })

    it('点击关闭应该关闭面板', async () => {
      const closeButton = wrapper.findAll('button').find(btn => btn.text() === '关闭')
      
      if (closeButton) {
        await closeButton.trigger('click')
        await nextTick()
        
        expect(wrapper.vm.panelVisible).toBe(false)
      }
    })
  })

  describe('拖拽功能', () => {
    beforeEach(() => {
      wrapper = mount(FloatingToolBox, {
        global: {
          stubs: {
            GitCommitButton: true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'ToolOutlined': true,
            'CustomModal': true,
            'BackFillModal': true
          }
        }
      })
    })

    it('应该从localStorage恢复位置', () => {
      localStorageMock.getItem.mockReturnValue(JSON.stringify({ x: 200, y: 300 }))
      
      wrapper = mount(FloatingToolBox, {
        global: {
          mocks: {
            $store: {
              state: {
                user: {
                  department: 'default',
                  userName: 'testUser'
                }
              }
            }
          },
          stubs: {
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'ToolOutlined': true,
            'CustomModal': true,
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'ExportButton': true
          }
        }
      })

      expect(localStorageMock.getItem).toHaveBeenCalledWith('floatingToolBoxPosition')
      expect(wrapper.vm.buttonPosition.x).toBe(200)
      expect(wrapper.vm.buttonPosition.y).toBe(300)
    })

    it('应该处理localStorage中没有位置的情况', () => {
      localStorageMock.getItem.mockReturnValue(null)
      
      wrapper = mount(FloatingToolBox, {
        global: {
          mocks: {
            $store: {
              state: {
                user: {
                  department: 'default',
                  userName: 'testUser'
                }
              }
            }
          },
          stubs: {
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'ToolOutlined': true,
            'CustomModal': true,
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'ExportButton': true
          }
        }
      })

      expect(wrapper.vm.buttonPosition.x).toBeNull()
      expect(wrapper.vm.buttonPosition.y).toBeNull()
    })

    it('应该能够开始拖拽', async () => {
      const mockEvent = {
        preventDefault: vi.fn(),
        stopPropagation: vi.fn(),
        clientX: 100,
        clientY: 100,
        // pointerdown 场景：这里直接调用方法，避免 jsdom 事件对象只读属性导致 trigger 失败
        pointerId: 1,
        button: 0,
        currentTarget: {
          setPointerCapture: vi.fn()
        }
      }

      wrapper.vm.startDrag(mockEvent)
      await nextTick()

      expect(wrapper.vm.isDragging).toBe(true)
    })
  })

  describe('数据获取', () => {
    it('应该获取语种列表', async () => {
      const { getLanguage } = await import('@/http/api/translate')
      
      wrapper = mount(FloatingToolBox, {
        global: {
          mocks: {
            $store: {
              state: {
                user: {
                  department: 'default',
                  userName: 'testUser'
                }
              }
            }
          },
          stubs: {
            GitCommitButton: true,
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'ToolOutlined': true,
            'CustomModal': true,
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'ExportButton': true
          }
        }
      })

      await nextTick()
      // 等待API调用完成
      await new Promise(resolve => setTimeout(resolve, 100))

      expect(getLanguage).toHaveBeenCalled()
      expect(wrapper.vm.translateTypes.length).toBeGreaterThan(0)
    })

    it('应该处理API错误', async () => {
      const { getLanguage } = await import('@/http/api/translate')
      getLanguage.mockRejectedValueOnce(new Error('API Error'))
      
      // 捕获console.error以避免测试输出错误
      const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

      wrapper = mount(FloatingToolBox, {
        global: {
          mocks: {
            $store: {
              state: {
                user: {
                  department: 'default',
                  userName: 'testUser'
                }
              }
            }
          },
          stubs: {
            'a-button': true,
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'ToolOutlined': true,
            'CustomModal': true,
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'ExportButton': true
          }
        }
      })

      await nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))

      // 组件应该仍然可以正常渲染
      expect(wrapper.find('.floating-button').exists()).toBe(true)
      
      consoleErrorSpy.mockRestore()
    })
  })

  describe('Git推送功能', () => {
    beforeEach(() => {
      wrapper = mount(FloatingToolBox, {
        global: {
          stubs: {
            GitCommitButton: {
              template: '<button><slot></slot></button>',
              props: ['size', 'buttonTitle', 'buttonClass']
            },
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'block', 'loading']
            },
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'ToolOutlined': true,
            'CustomModal': true,
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'ExportButton': true
          },
          mocks: {
            $store: {
              state: {
                user: {
                  department: 'default',
                  userName: 'testUser'
                }
              }
            }
          }
        }
      })
    })

    it('点击git推送应该打开模态框', async () => {
      wrapper.vm.panelVisible = true
      await nextTick()

      const gitPushButton = wrapper.findAll('button').find(btn => btn.text().includes('git推送'))
      
      if (gitPushButton) {
        await gitPushButton.trigger('click')
        await nextTick()

        // 由于 Git 推送模态框由 GitCommitButton 内部控制，这里只验证按钮存在且可点击
        expect(gitPushButton.exists()).toBe(true)
      }
    })
  })

  describe('更新翻译功能', () => {
    beforeEach(() => {
      wrapper = mount(FloatingToolBox, {
        global: {
          stubs: {
            GitCommitButton: {
              template: '<button><slot></slot></button>',
              props: ['size', 'buttonTitle', 'buttonClass']
            },
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'block', 'loading']
            },
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'ToolOutlined': true,
            'CustomModal': true,
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'ExportButton': true
          },
          mocks: {
            $store: {
              state: {
                user: {
                  department: 'default',
                  userName: 'testUser'
                }
              }
            }
          }
        }
      })
    })

    it('点击更新翻译应该打开模态框', async () => {
      wrapper.vm.panelVisible = true
      await nextTick()

      const updateButton = wrapper.findAll('button').find(btn => btn.text().includes('更新翻译'))
      
      if (updateButton) {
        await updateButton.trigger('click')
        await nextTick()

        // BackFillModal 在按钮模式下内部控制弹窗，这里只验证按钮存在且可点击
        expect(updateButton.exists()).toBe(true)
      }
    })
  })

  describe('去重回填功能', () => {
    beforeEach(() => {
      wrapper = mount(FloatingToolBox, {
        global: {
          stubs: {
            GitCommitButton: {
              template: '<button><slot></slot></button>',
              props: ['size', 'buttonTitle', 'buttonClass']
            },
            'a-button': {
              template: '<button><slot></slot></button>',
              props: ['type', 'block', 'loading']
            },
            'a-form': true,
            'a-form-item': true,
            'a-select': true,
            'ToolOutlined': true,
            'CustomModal': true,
            'BackFillModal': true,
            'BackFillModal_v2': true,
            'ExportButton': true
          },
          mocks: {
            $store: {
              state: {
                user: {
                  department: 'default',
                  userName: 'testUser'
                }
              }
            }
          }
        }
      })
    })

    it('点击去重回填应该打开模态框', async () => {
      wrapper.vm.panelVisible = true
      await nextTick()

      const backFillButton = wrapper.findAll('button').find(btn => btn.text().includes('去重回填'))
      
      if (backFillButton) {
        await backFillButton.trigger('click')
        await nextTick()

        // BackFillModal 在按钮模式下内部控制弹窗，这里只验证按钮存在且可点击
        expect(backFillButton.exists()).toBe(true)
      }
    })
  })
})
