<template>
  <a-config-provider :locale="locale">
    <electronTitle />
    <router-view />
    <FloatingToolBox v-if="currentDepartment.ops.has('toolBox')" />
  </a-config-provider>
</template>
<script>
import electronTitle from './components/title/electronTitle';
import FloatingToolBox from './components/FloatingToolBox/index.vue';
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import commonParam from '@/constants/commonParam';
export default {
  name: '',
  components: {
    electronTitle,
    FloatingToolBox
  },
  data() {
    return {
      locale: zhCN,
    }
  },
  computed: {
    currentDepartment() {
      const user = this.$store.state.user || {};
      const department = user.department;
      if (department && Object.keys(commonParam.departmentMap).includes(department)) {
        return commonParam.departmentMap[department];
      }
      return commonParam.departmentMap["default"];
    },
  },
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  /* text-align: center; */
  /* color: #2c3e50; */
  color: #F3F3F3;
  height: 100%;
  overflow: hidden;
  white-space: pre-wrap;
}
</style>
