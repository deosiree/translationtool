import { describe, it, expect, beforeEach, vi } from 'vitest'
import { closeAllNotifications } from '@/utils/notificationUtils'

describe('notificationUtils - Notification工具函数', () => {
  beforeEach(() => {
    // 清理DOM
    document.body.innerHTML = ''
    vi.clearAllMocks()
  })

  describe('closeAllNotifications', () => {
    it('应该保留notification容器，只移除notice元素', () => {
      // 创建模拟的notification容器（包含notice）
      const notification1 = document.createElement('div')
      notification1.className = 'ant-notification'
      const notice1 = document.createElement('div')
      notice1.className = 'ant-notification-notice'
      notification1.appendChild(notice1)
      
      const notification2 = document.createElement('div')
      notification2.className = 'ant-notification'
      const notice2 = document.createElement('div')
      notice2.className = 'ant-notification-notice'
      notification2.appendChild(notice2)
      
      document.body.appendChild(notification1)
      document.body.appendChild(notification2)

      // 验证初始状态
      expect(document.querySelectorAll('.ant-notification').length).toBe(2)
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(2)

      // 调用函数
      closeAllNotifications()

      // 验证容器被保留，但notice被移除
      expect(document.querySelectorAll('.ant-notification').length).toBe(2)
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(0)
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

    it('应该移除notice元素但保留容器', () => {
      // 创建混合的DOM结构
      const notification = document.createElement('div')
      notification.className = 'ant-notification'
      const notice1 = document.createElement('div')
      notice1.className = 'ant-notification-notice'
      notification.appendChild(notice1)
      
      const notice2 = document.createElement('div')
      notice2.className = 'ant-notification-notice'
      document.body.appendChild(notice2)
      document.body.appendChild(notification)

      // 调用函数
      closeAllNotifications()

      // 验证notice被移除，但容器保留
      expect(document.querySelectorAll('.ant-notification').length).toBe(1)
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
      // 创建有parentNode的notification（包含notice）
      const parent = document.createElement('div')
      const notification = document.createElement('div')
      notification.className = 'ant-notification'
      const notice = document.createElement('div')
      notice.className = 'ant-notification-notice'
      notification.appendChild(notice)
      parent.appendChild(notification)
      document.body.appendChild(parent)

      expect(document.querySelectorAll('.ant-notification').length).toBe(1)
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(1)

      // 调用函数
      closeAllNotifications()

      // 验证容器保留，但notice被移除
      expect(document.querySelectorAll('.ant-notification').length).toBe(1)
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(0)
      expect(parent.childNodes.length).toBe(1) // 容器还在
    })

    it('应该处理没有parentNode的notice情况', () => {
      // 创建没有parentNode的notice（直接添加到body）
      const notice = document.createElement('div')
      notice.className = 'ant-notification-notice'
      document.body.appendChild(notice)

      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(1)

      // 调用函数
      closeAllNotifications()

      // 验证notice被移除
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(0)
    })

    it('应该处理嵌套的notification结构（只移除notice）', () => {
      // 创建嵌套的notification结构（包含notice）
      const outerNotification = document.createElement('div')
      outerNotification.className = 'ant-notification'
      const innerNotification = document.createElement('div')
      innerNotification.className = 'ant-notification'
      const notice = document.createElement('div')
      notice.className = 'ant-notification-notice'
      innerNotification.appendChild(notice)
      outerNotification.appendChild(innerNotification)
      document.body.appendChild(outerNotification)

      expect(document.querySelectorAll('.ant-notification').length).toBe(2)
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(1)

      // 调用函数
      closeAllNotifications()

      // 验证容器保留，但notice被移除
      expect(document.querySelectorAll('.ant-notification').length).toBe(2)
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(0)
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

    it('移除所有通知后，容器保留但notice被移除，确保新通知能正常显示', () => {
      // 创建模拟的notification结构（包含容器和notice）
      const container = document.createElement('div')
      container.className = 'ant-notification'
      const notice1 = document.createElement('div')
      notice1.className = 'ant-notification-notice'
      const notice2 = document.createElement('div')
      notice2.className = 'ant-notification-notice'
      container.appendChild(notice1)
      container.appendChild(notice2)
      document.body.appendChild(container)

      // 验证初始状态
      expect(document.querySelectorAll('.ant-notification').length).toBe(1)
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(2)

      // 调用函数移除所有通知
      closeAllNotifications()

      // 验证容器保留，但所有notice被移除
      // 保留容器是为了确保Ant Design Vue的内部状态与DOM一致，从而能正常创建新通知
      expect(document.querySelectorAll('.ant-notification').length).toBe(1)
      expect(document.querySelectorAll('.ant-notification-notice').length).toBe(0)
      
      // 验证容器是空的（可以用于添加新通知）
      const remainingContainer = document.querySelector('.ant-notification')
      expect(remainingContainer).toBeTruthy()
      expect(remainingContainer.children.length).toBe(0)
    })
  })
})
