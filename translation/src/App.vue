<template>
  <electronTitle />
  <router-view/>
</template>
<script>
import electronTitle from './components/title/electronTitle';
export default {
    name: '',
    components: {
      electronTitle
    },
    data(){
      return{
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
}

</style>
