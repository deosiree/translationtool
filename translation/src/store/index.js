import { createStore } from 'vuex'
import createPersistedState from 'vuex-persistedstate'
import commonParam from '@/constants/commonParam'
import { notification } from 'ant-design-vue'
import { setCurrentDepartment } from '@/services/currentDepartmentService'
import batchProgress from './modules/batchProgress'

export default createStore({
  state: {
    token: null,
    menu:[],
    authority:[],
    user:null,
    admin:false,
    dynamicRoutes: [],
    tabActive:null,   // 配置页面子菜单激活的tab
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
      if(value.user.roleName != null && value.user.roleName.indexOf('管理员') != -1){
        state.admin = true
      }
      // 根据用户部门设置当前部门信息（使用全局属性）
      const department = value.user?.department;
      let currentDepartment = null;
      if (department && Object.keys(commonParam.departmentMap).includes(department)) {
        currentDepartment = commonParam.departmentMap[department];
      } else {
        // 找不到用户部门时，通知用户并设置为默认部门
        if (department) {
          notification.warning({
            message: '部门信息未找到',
            description: `未找到用户部门"${department}"的配置，已设置为默认部门`
          });
        }
        currentDepartment = commonParam.departmentMap["default"];
      }
      // 设置全局属性
      setCurrentDepartment(currentDepartment);
    },
    // 删除token
    removeData(state) {
      state.token = null // 删除vuex的token
      state.menu = []
      // state.authority = []
      state.user = null
      state.dynamicRoutes = []
      state.admin = false
      // 清空全局属性中的部门信息
      setCurrentDepartment(null);
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
    batchProgress,
  },
  plugins: [createPersistedState({
    storage: window.sessionStorage
  })]
})
