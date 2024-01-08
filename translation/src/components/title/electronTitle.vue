<template>
    <div id="title">
      <div class="electrontitle">
        <div class="logo"></div>
        <span style="font-weight:bold">{{electrontitle}}</span>
      </div>
      <div :class="electron ? 'user' : 'webUser'">
        <a-dropdown>
          <a class="ant-dropdown-link" @click.prevent>
            <span v-if="user != null">{{userInfo}}</span>
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
      <div class="operate" v-if="electron">
        <div class="btn minbtn" @click="clickBtn('min')"></div>
        <div class="btn maxbtn" @click="clickBtn('max')" v-if="flag"></div>
        <div class="btn minimizebtn" @click="clickBtn('max')" v-else></div>
        <div class="btn closebtn" @click="clickBtn('close')"></div>
      </div>
    </div>
</template>
<script>
//   const {ipcRenderer: ipc} = require('electron');
const config = require('../../../public/config')
export default {
  name: 'ElectronTitle',
  components: {
  },
  data() {
    return {
      electron: config.app.electron,
      electrontitle: '词条翻译工具',
      user:{
        userName:"",
        department:""
      },
      userInfo:"",
      flag: true
    };
  },
  watch: {
    "$store.state.user" (newVal, oldVal) {
      this.user = newVal
      if(this.user != null){
        this.userInfo = this.user.department +" , " + this.user.userName
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      // 页面加载完成后执行的代码
      this.user = this.$store.state.user
      if(this.user != null){
        this.userInfo = this.user.department +" , " + this.user.userName
        // console.log(this.user)
      }
      
    })
  },
  methods: {
    clickBtn: function (type) {
      const {ipcRenderer: ipc} = require('electron');
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

<style lang="less" scoped>
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
  height: 40px;
  background-color: rgb(87,159,249);
  -webkit-app-region: drag;
}
.electrontitle{
  padding: 4px 0px;

  .logo{
    width:32px;
    height: 32px;
    background-image: url("../../assets/title/logo.png");
    background-size: 100%;
    background-repeat: no-repeat;
  }

  span{
    line-height: 40px;
    color: white;
    position: absolute;
    top: 0;
    bottom: 0;
    margin: auto 0;
    left: 35px;
    font-size: 14px;
  }
}
#title .user{
  height: 30px;
  position: absolute;
  top: 0;
  bottom: 0;
  margin: auto 0;
  right: 100px;
  -webkit-app-region: no-drag;
}

.ant-dropdown-trigger{
  line-height: 30px;
  color: white;
}
.userLogo{
  width:20px;
  height:20px; 
  background-image: url("../../assets/title/user.png");
  background-size: 100%;
  background-repeat: no-repeat;
  float: right;
  margin: 5px 0px 0px 5px;
}
#title .webUser{
  height: 30px;
  position: absolute;
  top: 0;
  bottom: 0;
  margin: auto 0;
  right: 10px;
  -webkit-app-region: no-drag;
}
</style>