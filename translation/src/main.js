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
import { initCurrentDepartment } from '@/services/currentDepartmentService'

const app = createApp(App)

// 初始化 currentDepartment 全局属性及其持久化恢复
initCurrentDepartment(app)

app.use(store).use(router).use(Antd)

// 全局注册拖拽模态框指令
app.directive('drag-modal', createDragModalDirective())

app.mount('#app')