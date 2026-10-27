import { describe, expect, it } from 'vitest'
import BatchProgressOverlay from '@/views/workbench/components/BatchProgressOverlay.vue'

describe('BatchProgressOverlay progress counts', () => {
  it('does not display task completion count as an entry count', () => {
    const progress = {
      steps: { archive: { endTask: 'success', writeBack: 'success' } },
      stepCounts: { archive: { endTask: 1, writeBack: 3 } }
    }

    expect(BatchProgressOverlay.methods.formatStepCount(progress, 'archive', 'endTask')).toBe('')
    expect(BatchProgressOverlay.methods.formatStepCount(progress, 'archive', 'writeBack')).toBe(' (3条)')
  })
})
