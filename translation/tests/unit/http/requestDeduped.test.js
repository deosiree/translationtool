import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createDeferred } from '../testUtils/deferred'

const { axiosRequestMock } = vi.hoisted(() => {
  const axiosRequestMock = vi.fn()
  axiosRequestMock.interceptors = {
    request: { use: vi.fn() },
    response: { use: vi.fn() },
  }
  return { axiosRequestMock }
})

vi.mock('axios', () => ({
  default: Object.assign(vi.fn(), {
    create: vi.fn(() => axiosRequestMock),
    isCancel: vi.fn(),
  }),
}))

vi.mock('@/router/index', () => ({ default: { push: vi.fn() } }))
vi.mock('@/store', () => ({ default: { state: { token: null }, commit: vi.fn() } }))
vi.mock('ant-design-vue', () => ({ message: { error: vi.fn() } }))

import { requestDeduped } from '@/http/request'

describe('requestDeduped', () => {
  beforeEach(() => {
    axiosRequestMock.mockReset()
  })

  it('合并相同的在途只读请求，完成后再次调用会重新请求', async () => {
    const firstRequest = createDeferred()
    const response = { data: { list: ['词条'] } }
    axiosRequestMock.mockReturnValueOnce(firstRequest.promise)
      .mockResolvedValueOnce(response)
    const config = {
      url: '/entryInfo/getEntryByClassfy',
      method: 'POST',
      params: { classfyID: 'product-1', pageIndex: 1 },
      data: { entry: '词条' },
    }

    const first = requestDeduped(config)
    const duplicate = requestDeduped({ ...config })

    expect(axiosRequestMock).toHaveBeenCalledTimes(1)
    firstRequest.resolve(response)
    await expect(Promise.all([first, duplicate])).resolves.toEqual([response, response])

    await expect(requestDeduped(config)).resolves.toBe(response)
    expect(axiosRequestMock).toHaveBeenCalledTimes(2)
  })

  it('不同查询条件不会合并', async () => {
    const firstRequest = createDeferred()
    const secondRequest = createDeferred()
    axiosRequestMock.mockReturnValueOnce(firstRequest.promise)
      .mockReturnValueOnce(secondRequest.promise)

    const first = requestDeduped({
      url: '/entryInfo/getPublicEntry',
      method: 'POST',
      params: { pageIndex: -1 },
      data: { entry: 'A' },
    })
    const second = requestDeduped({
      url: '/entryInfo/getPublicEntry',
      method: 'POST',
      params: { pageIndex: -1 },
      data: { entry: 'B' },
    })

    expect(axiosRequestMock).toHaveBeenCalledTimes(2)
    firstRequest.resolve({ data: { list: ['A'] } })
    secondRequest.resolve({ data: { list: ['B'] } })
    await Promise.all([first, second])
  })
})
