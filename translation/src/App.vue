<template>
  <a-config-provider :locale="locale">
    <electronTitle />
    <router-view/>
  </a-config-provider>
</template>
<script>
import electronTitle from './components/title/electronTitle';
import zhCN from 'ant-design-vue/es/locale/zh_CN';
export default {
    name: '',
    components: {
      electronTitle
    },
    data(){
      return{
        locale:zhCN
      }
    }
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
