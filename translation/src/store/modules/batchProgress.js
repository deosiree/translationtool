export default {
  namespaced: true,
  state: {
    phase: 'idle',
    config: null,
    progresses: []
  },
  getters: {
    visible: state => state.phase !== 'idle',
    isRunning: state => state.phase === 'running',
    allCompleted: state => state.progresses.every(p =>
      Object.values(p.stages).every(s => s === 'success' || s === 'skipped')
    )
  },
  mutations: {
    START(state, { config, tasks }) {
      state.phase = 'running'
      state.config = config
      state.progresses = tasks.map(t => ({
        taskId: t.id,
        taskName: t.name,
        stages: {
          entryExamine: config.stages.entryExamine ? 'pending' : 'skipped',
          preTranslate: config.stages.preTranslate ? 'pending' : 'skipped',
          translateExamine: config.stages.translateExamine ? 'pending' : 'skipped'
        },
        currentStage: null,
        error: null,
        retryCount: 0
      }))
    },
    UPDATE_PROGRESS(state, progresses) {
      state.progresses = progresses
    },
    COMPLETE(state) {
      state.phase = 'completed'
    },
    RESET(state) {
      state.phase = 'idle'
      state.config = null
      state.progresses = []
    }
  },
  actions: {
    start({ commit }, { config, tasks }) { commit('START', { config, tasks }) },
    updateProgress({ commit }, progresses) { commit('UPDATE_PROGRESS', progresses) },
    complete({ commit }) { commit('COMPLETE') },
    reset({ commit }) { commit('RESET') }
  }
}