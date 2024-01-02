<template>
    <div style="100%;height:100%;padding:8px 24px 24px 24px">
      <a-tabs v-model:activeKey="activeKey" ref="tab" @change="changeTab">
          <a-tab-pane 
          v-for='(item) in menu' 
          :key="item.name" 
          :tab="item.menuName">
            <User v-if="item.name === 'user'"/>
            <Role v-if="item.name === 'role'"/>
            <Version v-if="item.name === 'version'"/>
            <Label v-if="item.name === 'label'"/>
            <Property v-if="item.name === 'property'"/>
            <Task v-if="item.name === 'task'"/>
          </a-tab-pane>
      </a-tabs>
    </div>
</template>
<script>
import User from '../user/index.vue';
import Role from '../role/index.vue';
import Version from '../version/index.vue';
import Label from '../label/index.vue';
import Property from '../property/index.vue';
import Task from '../task/index.vue'
export default({
  name: 'layout',
  components: {
    User,
    Role,
    Version,
    Label,
    Property,
    Task
  },
  data() {
    return {
      activeKey: "",
      menu:[]
    };
  },
  mounted() {
    this.$nextTick(() => {
      console.log(this.$store.state.tabActive)
      // 页面加载完成后执行的代码
      let list = this.$store.state.menu
      for(var item of list){
        if(item.url === this.$route.path && item.children.length > 0){
            this.menu = item.children
            // this.activeKey = this.menu[0].name
        }
      }
      if(this.menu.length > 0){
        this.activeKey = this.$store.state.tabActive === null ? this.menu[0].name : this.$store.state.tabActive
      }
    })
  },
  methods: {
    changeTab(activeKey){
      // console.log(activeKey);
      this.$store.commit("setTabActive", activeKey)
    }
  },
})
</script>
<style lang="less" scoped>
:deep(.ant-tabs-nav) {
    margin: 0 0 10px 0;
}
:deep(.ant-tabs){
  height: 100%;
}

:deep(.ant-tabs-content){
  height: 100%;
  position: relative;
}
:deep(.ant-tabs-tab-btn){
  font-size: 12px;
}
</style>
