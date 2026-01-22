import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/antd.css'
import './router/permission'

import * as antIcons from '@ant-design/icons-vue'
import moment from 'moment';
import 'moment/locale/zh-cn'; // 引入中文语言包
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
dayjs.locale('zh-cn');
// 设置全局语言为中文
moment.locale('zh-cn');

import { createDragModalDirective } from '@/utils/domUtils'

const app = createApp(App)

// 初始化全局属性 currentDepartment
app.config.globalProperties.$currentDepartment = null

// 设置 currentDepartment 的辅助函数（供 store 调用）
export function setCurrentDepartment(department) {
    app.config.globalProperties.$currentDepartment = department
    // 持久化到 sessionStorage（处理 Set 类型的序列化）
    if (department) {
        const serialized = {
            ...department,
            ops: Array.from(department.ops || [])
        }
        sessionStorage.setItem('currentDepartment', JSON.stringify(serialized))
    } else {
        sessionStorage.removeItem('currentDepartment')
    }
}

// 从 sessionStorage 恢复 currentDepartment
function restoreCurrentDepartment() {
    try {
        const stored = sessionStorage.getItem('currentDepartment')
        if (stored) {
            const parsed = JSON.parse(stored)
            // 将 ops 数组转回 Set
            if (parsed.ops && Array.isArray(parsed.ops)) {
                parsed.ops = new Set(parsed.ops)
            }
            app.config.globalProperties.$currentDepartment = parsed
        }
    } catch (error) {
        console.error('恢复 currentDepartment 失败:', error)
        app.config.globalProperties.$currentDepartment = null
    }
}

// 恢复持久化的 currentDepartment
restoreCurrentDepartment()

app.use(store).use(router).use(Antd)

// 全局注册拖拽模态框指令
app.directive('drag-modal', createDragModalDirective())

app.mount('#app')