<template>
    <a-tabs v-model:activeKey="activeKey">
          <a-tab-pane 
          v-for='(item,index) in menu' 
          :key="index" 
          :tab="item.menuName">
            <User v-if="item.name === 'user'"/>
            <Role v-if="item.name === 'role'"/>
            <Version v-if="item.name === 'version'"/>
          </a-tab-pane>
    </a-tabs>
</template>
<script>
import User from '../user/index.vue';
import Role from '../role/index.vue';
import Version from '../version/index.vue';
export default({
  name: 'layout',
  components: {
    User,
    Role,
    Version
  },
  data() {
    return {
      activeKey: 0,
      menu:[]
    };
  },
  mounted() {
    this.$nextTick(() => {
      // 页面加载完成后执行的代码
      let list = this.$store.state.menu
      for(var item of list){
        if(item.url === this.$route.path && item.children.length > 0){
            this.menu = item.children
        }
      }
    })
  },
  methods: {
    
    
  },
})
</script>
<style>
.ant-tabs-top > .ant-tabs-nav, .ant-tabs-bottom > .ant-tabs-nav, .ant-tabs-top > div > .ant-tabs-nav, .ant-tabs-bottom > div > .ant-tabs-nav {
    margin: 0 0 10px 0;
}
.ant-tabs{
  height: 100%;
}

.ant-tabs-content{
  height: 100%;
  position: relative;
}
.ant-tabs-tab-btn{
  font-size: 12px;
}
</style>
