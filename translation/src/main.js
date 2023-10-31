import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/antd.css'
import './router/permission'

import * as antIcons from '@ant-design/icons-vue'

createApp(App).use(store).use(router).use(Antd).use(antIcons).mount('#app')