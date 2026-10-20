import { describe, expect, it } from 'vitest'
import { createQueryVersionGuard } from '@/utils/queryVersionGuard'

describe('createQueryVersionGuard', () => {
  it('只接受最新 token，并允许显式失效', () => {
    const guard = createQueryVersionGuard()
    const first = guard.next()
    const second = guard.next()

    expect(guard.isCurrent(first)).toBe(false)
    expect(guard.isCurrent(second)).toBe(true)

    guard.invalidate()

    expect(guard.isCurrent(second)).toBe(false)
  })

  it('每个守卫实例独立维护版本', () => {
    const firstGuard = createQueryVersionGuard()
    const secondGuard = createQueryVersionGuard()
    const firstToken = firstGuard.next()

    expect(secondGuard.isCurrent(firstToken)).toBe(false)
    expect(secondGuard.next()).toBe(firstToken)
    expect(firstGuard.isCurrent(firstToken)).toBe(true)
  })
})
