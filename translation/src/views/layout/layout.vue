<template>
    <div class="layout">
        <a-row type="flex">
            <a-col flex="60px">
                <div class="menu">
                    <template v-for='(item,index) in menu' :key="index">
                        <div :class="isActive === index ? 'menuItem active' : 'menuItem'" @click="clickMenu(item.url,index)">
                            <img :src="isActive === index ? menuIcon[item.activeIcon] : menuIcon[item.icon]"/>
                            <span>{{item.menuName}}</span>
                        </div>
                    </template>
                </div>
            </a-col>
            <a-col flex="auto">
                <div class="content">
                    <div style="width:100%;height:100%;background-color:white;padding:10px;">
                        <router-view />
                    </div>
                </div>
            </a-col>
        </a-row>
    </div>
</template>
<script>
import Work from '../../assets/title/work.png'
import WorkActive from '../../assets/title/work_active.png'
import Entry from '../../assets/title/entry.png'
import EntryActive from '../../assets/title/entry_active.png'
import Config from '../../assets/title/config.png'
import ConfigActive from '../../assets/title/config_active.png'
export default ({
  name: 'layout',
  components: {
    
  },
  data() {
    return {
        menuIcon:{'Work':Work,'WorkActive':WorkActive,'Entry':Entry,'EntryActive':EntryActive,'Config':Config,'ConfigActive':ConfigActive},
        menu: [],
        isActive: 0
    };
  },
  mounted() {
    this.$nextTick(() => {
      // 页面加载完成后执行的代码
      this.menu = this.$store.state.menu
      if(this.menu.length > 0){
        this.$router.push(this.menu[0].url)
      }
    })
  },
  methods: {
    clickMenu(url,index) {
      this.$router.push(url)
      this.isActive = index;
    },
    
  },
})
</script>

<style>
.layout{
    width: 100%;
    height: calc(100% - 30px);
    padding: 10px 10px 10px 0;
}
.ant-row{
    height: 100%;
}
.menu{
    width: 100%;
    height: 100%;
    background-color: rgb(243,243,243);
    padding: 10px 0px;
    text-align: center;
}
.content{
    width: 100%;
    height: 100%;
    background-color: rgb(243,243,243);
    padding: 10px;
}
.menu .menuItem{
    width: 100%;
    height: 60px;
    border-bottom: 1px solid #E7E7E7;
    position: relative;
}
.menuItem img{
    width: 28px;
    height: 28px;
    position:absolute;
    left: 0;
    right: 0;
    top: 10px;
    margin: 0 auto;
}
.menuItem span{
    position:absolute;
    left: 0;
    right: 0;
    top: 38px;
    margin: 0 auto;
    text-align: center;
    font-size: 12px;
    color: #000000;
}
.menuItem:hover span{
    color: rgb(87,159,249);
}
.active{
    background-color: white;
}
.active span{
    color: #369FFF;
}
</style>