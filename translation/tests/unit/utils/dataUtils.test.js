/**
 * dataUtils 单元测试
 * 覆盖「完成后自动写库」开关的 localStorage 读写与脏数据降级
 */

import { describe, it, expect, beforeEach } from 'vitest'
import {
  getAutoWrite,
  setAutoWrite,
  getCachedI18nUrl,
  setCachedI18nUrl,
  normalizeEditableRow,
} from '@/utils/dataUtils'
import { entryParams } from '@/constants/commonParam'

const AUTO_WRITE_KEY = entryParams.updateEntry.localStorageKey.autoWrite

describe('dataUtils - 自动写库开关持久化', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  describe('常量约定', () => {
    it('autoWrite 的 localStorage key 应已在 commonParam 中声明', () => {
      expect(AUTO_WRITE_KEY).toBeTruthy()
      // 与 i18nUrl 共存，互不覆盖
      expect(AUTO_WRITE_KEY).not.toBe(entryParams.updateEntry.localStorageKey.i18nUrl)
    })
  })

  describe('getAutoWrite', () => {
    it('无缓存时应返回 false', () => {
      expect(getAutoWrite('c1')).toBe(false)
    })

    it('classifyID 为空时应返回 false，不读取存储', () => {
      window.localStorage.setItem(AUTO_WRITE_KEY, JSON.stringify({ c1: true }))
      expect(getAutoWrite('')).toBe(false)
      expect(getAutoWrite(undefined)).toBe(false)
      expect(getAutoWrite(null)).toBe(false)
    })

    it('存储内容为坏 JSON 时应降级为 false，不抛错', () => {
      window.localStorage.setItem(AUTO_WRITE_KEY, '{not-json')
      expect(() => getAutoWrite('c1')).not.toThrow()
      expect(getAutoWrite('c1')).toBe(false)
    })

    it('存储内容为数组等非普通对象时应降级为 false', () => {
      window.localStorage.setItem(AUTO_WRITE_KEY, JSON.stringify(['c1']))
      expect(getAutoWrite('c1')).toBe(false)

      window.localStorage.setItem(AUTO_WRITE_KEY, JSON.stringify('c1'))
      expect(getAutoWrite('c1')).toBe(false)
    })

    it('仅当值严格为 true 时才视为已勾选', () => {
      window.localStorage.setItem(
        AUTO_WRITE_KEY,
        JSON.stringify({ c1: true, c2: 'true', c3: 1, c4: false })
      )
      expect(getAutoWrite('c1')).toBe(true)
      expect(getAutoWrite('c2')).toBe(false)
      expect(getAutoWrite('c3')).toBe(false)
      expect(getAutoWrite('c4')).toBe(false)
    })
  })

  describe('setAutoWrite', () => {
    it('置真后应可被 getAutoWrite 读回', () => {
      setAutoWrite('c1', true)
      expect(getAutoWrite('c1')).toBe(true)
    })

    it('不同 classifyID 之间应互相隔离', () => {
      setAutoWrite('c1', true)
      expect(getAutoWrite('c1')).toBe(true)
      expect(getAutoWrite('c2')).toBe(false)

      setAutoWrite('c2', true)
      expect(getAutoWrite('c1')).toBe(true)
      expect(getAutoWrite('c2')).toBe(true)
    })

    it('置假应删除该键而非存 false', () => {
      setAutoWrite('c1', true)
      setAutoWrite('c2', true)
      setAutoWrite('c1', false)

      const raw = JSON.parse(window.localStorage.getItem(AUTO_WRITE_KEY))
      expect(raw).toEqual({ c2: true })
      expect('c1' in raw).toBe(false)
    })

    it('map 清空后应移除整个 localStorage 键', () => {
      setAutoWrite('c1', true)
      expect(window.localStorage.getItem(AUTO_WRITE_KEY)).not.toBeNull()

      setAutoWrite('c1', false)
      expect(window.localStorage.getItem(AUTO_WRITE_KEY)).toBeNull()
    })

    it('对不存在的 classifyID 置假应安全无副作用', () => {
      expect(() => setAutoWrite('ghost', false)).not.toThrow()
      expect(window.localStorage.getItem(AUTO_WRITE_KEY)).toBeNull()
    })

    it('classifyID 为空时应直接返回，不写入存储', () => {
      setAutoWrite('', true)
      setAutoWrite(undefined, true)
      expect(window.localStorage.getItem(AUTO_WRITE_KEY)).toBeNull()
    })

    it('写入前存储为坏 JSON 时应以空 map 重建，不抛错', () => {
      window.localStorage.setItem(AUTO_WRITE_KEY, '{not-json')
      expect(() => setAutoWrite('c1', true)).not.toThrow()
      expect(JSON.parse(window.localStorage.getItem(AUTO_WRITE_KEY))).toEqual({ c1: true })
    })

    it('重复置真应幂等', () => {
      setAutoWrite('c1', true)
      setAutoWrite('c1', true)
      expect(JSON.parse(window.localStorage.getItem(AUTO_WRITE_KEY))).toEqual({ c1: true })
    })
  })

  describe('与 i18nUrl 缓存互不干扰', () => {
    it('写入自动写库开关不应影响 i18nUrl 缓存', () => {
      setCachedI18nUrl('192.168.1.100')
      setAutoWrite('c1', true)

      expect(getCachedI18nUrl()).toBe('192.168.1.100')
      expect(getAutoWrite('c1')).toBe(true)
    })

    it('清除自动写库开关不应清掉 i18nUrl 缓存', () => {
      setCachedI18nUrl('192.168.1.100')
      setAutoWrite('c1', true)
      setAutoWrite('c1', false)

      expect(getCachedI18nUrl()).toBe('192.168.1.100')
    })
  })

  describe('normalizeEditableRow（回归）', () => {
    it('应把 number/boolean 转为字符串', () => {
      expect(normalizeEditableRow({ a: 1, b: true, c: 'x' })).toEqual({
        a: '1',
        b: 'true',
        c: 'x',
      })
    })

    it('非对象入参应原样返回', () => {
      expect(normalizeEditableRow(null)).toBeNull()
      expect(normalizeEditableRow('s')).toBe('s')
    })
  })
})
