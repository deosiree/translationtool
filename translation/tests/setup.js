/**
 * Vitest 全局测试配置
 * 用于设置所有测试文件的通用配置，如全局 stubs、mocks 等
 */

import { config } from '@vue/test-utils'
import { vi } from 'vitest'

// 全局 mock ant-design-vue
// 提供 default 导出以支持 app.use(Antd) 的使用方式
// 注意：测试文件中的 vi.mock 会覆盖此配置，所以需要在每个测试文件中也添加 default 导出
vi.mock('ant-design-vue', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    default: {
      install: vi.fn()
    },
    message: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn()
    },
    notification: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn()
    }
  }
})

// 全局 ant-design-vue 组件 stubs 配置
// 这些是通用的简单 stub（只返回 true，渲染为空标签）
// 如果需要特殊行为（如带方法的组件），应在测试文件中覆盖此配置
config.global.stubs = {
  'a-button': true,
  'a-form': true,
  'a-form-item': true,
  'a-select': true,
  'a-upload': true,
  'a-row': true,
  'a-col': true,
  'a-checkbox': true,
  'a-checkbox-group': true,
  'a-descriptions': true,
  'a-descriptions-item': true,
  'a-table': true,
  'a-tag': true,
  'a-tabs': true,
  'a-tab-pane': true,
  'a-alert': true,
  'a-card': true,
  'a-badge': true,
  'a-input': true,
  'a-modal': true,
  'a-spin': true,
  'a-popover': true,
  'a-dropdown': true,
  'a-menu': true,
  'a-menu-item': true,
  'a-avatar': true,
  'a-layout': true,
  'a-layout-sider': true,
  'a-layout-header': true,
  'a-textarea': true,
  'a-date-picker': true,
  'a-config-provider': true,
  'a-radio': true,
  'a-radio-group': true,
  'a-tree-select': true,
  'a-popconfirm': true,
  'a-pagination': true,
  'a-tooltip': true,
  'a-divider': true,
  'a-input-number': true,
  'a-checkable-tag': true,
  'a-select-option': true
}
