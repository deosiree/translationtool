/**
 * Vitest 全局测试配置
 * 用于设置所有测试文件的通用配置，如全局 stubs、mocks 等
 */

import { config } from '@vue/test-utils'

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
  'a-checkbox': true,
  'a-descriptions': true,
  'a-descriptions-item': true,
  'a-table': true,
  'a-tag': true,
  'a-tabs': true,
  'a-tab-pane': true,
  'a-alert': true,
  'a-card': true,
  'a-badge': true
}
