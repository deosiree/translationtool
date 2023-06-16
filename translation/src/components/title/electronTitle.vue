<template>
    <div id="title">
      <div class="electrontitle">
        <div class="logo"></div>
        <span style="font-weight:bold">{{electrontitle}}</span>
      </div>
      <div class="user">
        <a-dropdown>
          <a class="ant-dropdown-link" @click.prevent>
            <span v-if="user != null">{{user.userName}}</span>
            <!-- <DownOutlined /> -->
            <div class="userLogo"></div>
          </a>
          <template #overlay>
            <a-menu>
              <a-menu-item key="0" @click="logout">
                  退出系统
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
      <div class="operate">
        <div class="btn minbtn" @click="clickBtn('min')"></div>
        <div class="btn maxbtn" @click="clickBtn('max')" v-if="flag"></div>
        <div class="btn minimizebtn" @click="clickBtn('max')" v-else></div>
        <div class="btn closebtn" @click="clickBtn('close')"></div>
      </div>
    </div>
</template>
<script>
const {ipcRenderer: ipc} = require('electron');
export default {
  name: 'ElectronTitle',
  components: {
  },
  data() {
    return {
      electrontitle: '词条翻译工具',
      user:{
        userName:"",
        department:""
      },
      flag: true
    };
  },
  watch: {
    "$store.state.user" (newVal, oldVal) {
      this.user = newVal
    }
  },
  mounted() {
    this.$nextTick(() => {
      // 页面加载完成后执行的代码
    })
  },
  methods: {
    clickBtn: function (type) {
      ipc.send(type);
      if(type === 'max'){
        this.flag = !this.flag
      }
    },
    logout(){
      //删除缓存数据
      this.$store.commit("removeData")
      //跳转登录页面
      this.$router.push("/")
    }
    
  },
}
</script>

<style>
.btn{
  width: 20px;
  height: 20px;
  position: absolute;
  top: 0;
  bottom: 0;
  margin: auto 0;
  -webkit-app-region: no-drag;
}
.minbtn{
  right: 70px;
  background-image: url("../../assets/title/min.png");
  background-size: 100%;
  background-repeat: no-repeat;
}
.maxbtn{
  right: 40px;
  background-image: url("../../assets/title/max.png");
  background-size: 100%;
  background-repeat: no-repeat;
}
.minimizebtn{
  width: 16px;
  height: 16px;
  right: 40px;
  background-image: url("../../assets/title/minimize.png");
  background-size: 100%;
  background-repeat: no-repeat;
}
.closebtn{
  right: 10px;
  background-image: url("../../assets/title/close.png");
  background-size: 100%;
  background-repeat: no-repeat;
}
#title {
  position: relative;
  width: 100%;
  height: 30px;
  background-color: rgb(87,159,249);
  -webkit-app-region: drag;
}
.electrontitle .logo{
  width:32px;
  height: 32px;
  background-image: url("../../assets/title/logo.png");
  background-size: 100%;
  background-repeat: no-repeat;
}
.electrontitle span{
  line-height: 30px;
  color: white;
  position: absolute;
  top: 0;
  bottom: 0;
  margin: auto 0;
  left: 35px;
}
#title .user{
  /* width: 100px; */
  height: 30px;
  position: absolute;
  top: 0;
  bottom: 0;
  margin: auto 0;
  right: 100px;
  -webkit-app-region: no-drag;
}
.user .ant-dropdown-trigger{
  line-height: 30px;
  color: white;
}
.user .userLogo{
  width:25px;
  height:25px; 
  background-image: url("../../assets/title/user.png");
  background-size: 100%;
  background-repeat: no-repeat;
  float: right;
  margin: 3px 0px 0px 5px;
}
</style>