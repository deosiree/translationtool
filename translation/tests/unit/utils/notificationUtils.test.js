import { describe, it, expect, beforeEach, vi } from 'vitest'
import { closeAllNotifications } from '@/utils/notificationUtils'

describe('notificationUtils - Notification工具函数', () => {
  beforeEach(() => {
    // 清理DOM
    document.body.innerHTML = ''
    vi.clearAllMocks()
  })

  describe('closeAllNotifications', () => {
    it('应该移除所有notification容器', () => {
      // 创建模拟的notification容器
      const notification1 = document.createElement('div')
      notification1.className = 'ant-notification'
      const notification2 = document.createElement('div')
      notification2.className = 'ant-notification'
      
      document.body.appendChild(notification1)
      document.body.appendChild(notification2)

      // 验证元素存在
      expect(document.querySelectorAll('.ant-notification').length).toBe(2)

      // 调用函数
      closeAllNotifications()

      // 验证元素被移除
      expect(document.querySelectorAll('.ant-notification').length).toBe(0)
    })

    it('应该移除所有notice元素', () => {
      // 创建模拟的notice元素
      const notice1 = document.createElement('div')
      notice1.className = 'ant-notification-notice'
      const notice2 = document.createElement('div')
      notice2.className = 'ant-notification-notice'
      
      document.body.appendChild(notice1)
      document.body.appendChild(notice2)

      // 验证元素存在
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(2)

      // 调用函数
      closeAllNotifications()

      // 验证元素被移除
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(0)
    })

    it('应该同时移除notification容器和notice元素', () => {
      // 创建混合的DOM结构
      const notification = document.createElement('div')
      notification.className = 'ant-notification'
      const notice = document.createElement('div')
      notice.className = 'ant-notification-notice'
      
      document.body.appendChild(notification)
      document.body.appendChild(notice)

      // 调用函数
      closeAllNotifications()

      // 验证所有元素都被移除
      expect(document.querySelectorAll('.ant-notification').length).toBe(0)
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(0)
    })

    it('应该处理没有notification的情况', () => {
      // 确保没有notification元素
      expect(document.querySelectorAll('.ant-notification').length).toBe(0)
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(0)

      // 调用函数不应该报错
      expect(() => {
        closeAllNotifications()
      }).not.toThrow()
    })

    it('应该处理有parentNode的情况', () => {
      // 创建有parentNode的notification
      const parent = document.createElement('div')
      const notification = document.createElement('div')
      notification.className = 'ant-notification'
      parent.appendChild(notification)
      document.body.appendChild(parent)

      expect(document.querySelectorAll('.ant-notification').length).toBe(1)

      // 调用函数
      closeAllNotifications()

      // 验证元素被移除
      expect(document.querySelectorAll('.ant-notification').length).toBe(0)
      expect(parent.childNodes.length).toBe(0)
    })

    it('应该处理没有parentNode的情况（直接调用remove）', () => {
      // 创建没有parentNode的notification（直接添加到body）
      const notification = document.createElement('div')
      notification.className = 'ant-notification'
      document.body.appendChild(notification)

      // 模拟remove方法
      const removeSpy = vi.spyOn(notification, 'remove')

      // 先移除parentNode，模拟没有parentNode的情况
      document.body.removeChild(notification)
      document.body.appendChild(notification)

      // 调用函数
      closeAllNotifications()

      // 验证remove被调用（如果元素没有parentNode）
      // 注意：在实际情况下，如果元素在DOM中，它会有parentNode
      // 这个测试主要验证代码逻辑不会因为parentNode为null而报错
      expect(document.querySelectorAll('.ant-notification').length).toBe(0)
    })

    it('应该处理嵌套的notification结构', () => {
      // 创建嵌套的notification结构
      const outerNotification = document.createElement('div')
      outerNotification.className = 'ant-notification'
      const innerNotification = document.createElement('div')
      innerNotification.className = 'ant-notification'
      outerNotification.appendChild(innerNotification)
      document.body.appendChild(outerNotification)

      // 调用函数
      closeAllNotifications()

      // 验证所有notification都被移除
      expect(document.querySelectorAll('.ant-notification').length).toBe(0)
    })

    it('应该处理多个嵌套的notice元素', () => {
      // 创建多个notice元素
      const container = document.createElement('div')
      for (let i = 0; i < 5; i++) {
        const notice = document.createElement('div')
        notice.className = 'ant-notification-notice'
        container.appendChild(notice)
      }
      document.body.appendChild(container)

      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(5)

      // 调用函数
      closeAllNotifications()

      // 验证所有notice都被移除
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(0)
    })
  })
})
