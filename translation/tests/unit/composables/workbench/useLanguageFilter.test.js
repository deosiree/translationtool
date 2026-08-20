import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { filterLanguageChange } from '@/composables/workbench/useLanguageFilter'
import { isLoading, resetLoading } from '@/composables/useLoading'

vi.mock('@/http/api/workbench', () => ({
  filterSourceLanguage: vi.fn(),
}))

vi.mock('ant-design-vue', () => ({
  message: { error: vi.fn() },
}))

import { filterSourceLanguage } from '@/http/api/workbench'
import { message } from 'ant-design-vue'

describe('useLanguageFilter', () => {
  let vm

  beforeEach(() => {
    vm = {
      filterLanguage: '全部',
      allData: [{ id: 1 }, { id: 2 }],
      dataSource: [],
      filterSource: [],
    }
    vi.clearAllMocks()
    resetLoading()
  })

  afterEach(() => {
    resetLoading()
  })

  it('选择「全部」时应恢复 allData', () => {
    vm.filterLanguage = '全部'
    vm.dataSource = []
    vm.filterSource = []

    filterLanguageChange(vm)

    expect(vm.dataSource).toBe(vm.allData)
    expect(vm.filterSource).toBe(vm.allData)
    expect(filterSourceLanguage).not.toHaveBeenCalled()
  })

  it('选择具体语种时应调用 API 并更新 dataSource', async () => {
    const filtered = [{ id: 1 }]
    filterSourceLanguage.mockResolvedValue({
      data: { list: filtered },
    })
    vm.filterLanguage = '中文'

    filterLanguageChange(vm)

    expect(isLoading()).toBe(true)
    expect(filterSourceLanguage).toHaveBeenCalledWith(
      { languageType: '中文' },
      vm.allData
    )

    await flushPromises()

    expect(vm.dataSource).toEqual(filtered)
    expect(vm.filterSource).toEqual(filtered)
    expect(isLoading()).toBe(false)
  })

  it('API 失败时应关闭 loading 并提示错误', async () => {
    filterSourceLanguage.mockRejectedValue(new Error('network'))
    vm.filterLanguage = '英文'

    filterLanguageChange(vm)
    await vi.waitFor(() => {
      expect(isLoading()).toBe(false)
    })

    expect(message.error).toHaveBeenCalled()
  })
})
