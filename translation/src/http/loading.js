// 修改导入方式
import { createApp } from 'vue';
import LoadingComponent from '@/components/Loading/index.vue';

let loadingInstance = null;

export default {
  show(config = {}) {
    if (!loadingInstance) {
      const app = createApp(LoadingComponent, config);
      const div = document.createElement('div');
      loadingInstance = app.mount(div);
      document.body.appendChild(loadingInstance.$el);
    }
    // 假设 Loading 组件有 start 方法
    if (loadingInstance.start) {
      loadingInstance.start();
    }
  },

  hide() {
    if (loadingInstance) {
      // 假设 Loading 组件有 stop 方法
      if (loadingInstance.stop) {
        loadingInstance.stop();
      }
      // 销毁组件实例
      const app = loadingInstance.$.appContext.app;
      // 检查 loadingInstance.$el 是否是 document.body 的子节点
      if (document.body.contains(loadingInstance.$el)) {
        document.body.removeChild(loadingInstance.$el);
      }
      app.unmount();
      loadingInstance = null;
    }
  }
};