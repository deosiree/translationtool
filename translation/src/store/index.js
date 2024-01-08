import { createStore } from 'vuex'
import createPersistedState from 'vuex-persistedstate'

export default createStore({
  state: {
    token: null,
    menu:[],
    authority:[],
    user:null,
    admin:false,
    dynamicRoutes: [],
    tabActive:null   // 配置页面子菜单激活的tab
  },
  getters: {
  },
  mutations: {
    // 设置token
    setData(state, value) {
      // console.log("value:",value)
      state.token = value.token // 设置token
      state.menu = value.menu
      // state.authority = value.authority
      state.user = value.user
      if(value.user.roleName.indexOf('管理员') != -1){
        state.admin = true
      }
    },
    // 删除token
    removeData(state) {
      state.token = null // 删除vuex的token
      state.menu = []
      // state.authority = []
      state.user = null
      state.dynamicRoutes = []
      state.admin = false
    },
    setTabActive(state, value){
      state.tabActive = value
    },
    removeTabActive(state){
      state.tabActive = null
    },
    DYNAMIC_ROUTES (state, routes) {
      state.dynamicRoutes = routes
    }
  },
  actions: {
    dynamicRoutes ({commit}, routes) {
      commit('DYNAMIC_ROUTES', routes)
    }
  },
  modules: {
  },
  plugins: [createPersistedState({
    storage: window.sessionStorage
  })]
})
