import { describe, it, expect } from 'vitest'
import { canRunStage, getSkipReason } from '@/utils/batchStagePermission'

const user = { userName: 'currentUser', roleName: '翻译员' }

function buildTask(overrides = {}) {
  return {
    id: 'task-1',
    name: 'Task 1',
    entryAuditor: 'currentUser',
    translator: 'currentUser',
    translationAuditor: 'currentUser',
    ...overrides
  }
}

describe('canRunStage', () => {
  it('用户与指派一致返回 true（各阶段字段映射正确）', () => {
    expect(canRunStage(user, buildTask(), 'entryExamine')).toBe(true)
    expect(canRunStage(user, buildTask(), 'preTranslate')).toBe(true)
    expect(canRunStage(user, buildTask(), 'translateExamine')).toBe(true)
  })

  it('指派非本人返回 false', () => {
    expect(canRunStage(user, buildTask({ translator: 'otherUser' }), 'preTranslate')).toBe(false)
  })

  it('指派为空/缺失返回 false', () => {
    expect(canRunStage(user, buildTask({ translator: '' }), 'preTranslate')).toBe(false)
    expect(canRunStage(user, { id: 'task-1' }, 'preTranslate')).toBe(false)
  })

  it('用户为空返回 false', () => {
    expect(canRunStage(null, buildTask(), 'preTranslate')).toBe(false)
    expect(canRunStage(undefined, buildTask(), 'preTranslate')).toBe(false)
    expect(canRunStage({}, buildTask(), 'preTranslate')).toBe(false)
  })

  it('未知阶段返回 false', () => {
    expect(canRunStage(user, buildTask(), 'unknownStage')).toBe(false)
  })
})

describe('getSkipReason', () => {
  it('无权限时含角色与指派人员', () => {
    const reason = getSkipReason(user, buildTask({ translator: '李四' }), 'preTranslate')
    expect(reason).toContain('翻译员')
    expect(reason).toContain('李四')
    expect(reason).toContain('已跳过')
  })

  it('指派为空时提示未指定', () => {
    const reason = getSkipReason(user, buildTask({ translator: '' }), 'preTranslate')
    expect(reason).toContain('未指定')
    expect(reason).toContain('翻译员')
  })

  it('各阶段角色文案映射正确', () => {
    expect(getSkipReason(user, buildTask({ entryAuditor: 'X' }), 'entryExamine')).toContain('词条审核员')
    expect(getSkipReason(user, buildTask({ translationAuditor: 'X' }), 'translateExamine')).toContain('翻译审核员')
  })

  it('未知阶段返回通用文案', () => {
    const reason = getSkipReason(user, buildTask(), 'unknownStage')
    expect(reason).toContain('已跳过')
  })
})
