<template>
    <div class="layout">
        <a-row type="flex" :gutter="16">
            <a-col flex="75px">
                <div class="menu">
                    <template v-for='(item,index) in menu' :key="index">
                        <div :class="isActive === index ? 'menuItem active' : 'menuItem'" @click="clickMenu(item.url,index)">
                            <img src="../../assets/title/data.png"/>
                            <span>{{item.menuName}}</span>
                        </div>
                    </template>
                </div>
            </a-col>
            <a-col flex="auto">
                <div class="content">
                    <div style="width:100%;height:100%;background-color:white;padding:10px">
                        <router-view />
                    </div>
                </div>
            </a-col>
        </a-row>
    </div>
</template>
<script>
export default ({
  name: 'layout',
  components: {
  },
  data() {
    return {
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
    /* border: 1px solid red; */
    padding: 10px;
}
.ant-row{
    height: 100%;
}
.menu{
    width: 100%;
    height: 100%;
    background-color: rgb(243,243,243);
    padding: 5px 3px;
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
    border-bottom: 1px solid #d7dad8;
    position: relative;
}
.menuItem img{
    width: 20px;
    height: 20px;
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
    top: 35px;
    margin: 0 auto;
    text-align: center;
    font-size: 10px;
    color: #7c7c7c;
}
.menuItem:hover span{
    color: rgb(87,159,249);
}
.active{
    background-color: white;
}
.active span{
    color: rgb(87,159,249);
}
</style>