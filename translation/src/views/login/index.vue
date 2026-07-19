<template>
  <div class="loginBg" :style="bgImg">
    <div class="loginBox">
      <div class="title"><span>{{title}}</span></div>
      <div class="loginForm">
        <div class="welcome">欢迎登录</div>
        <a-form
          :model="loginForm"
          layout="vertical"
          ref="loginFrom"
        >
          <a-form-item 
          name="account"
          label="用户名："
          >
            <a-input v-model:value="loginForm.account" placeholder="请输入用户名">
              <template #prefix><UserOutlined style="color: rgba(0, 0, 0, 0.25)" /></template>
            </a-input>
          </a-form-item>
          <a-form-item
          name="password"
          label="密码："
          >
            <a-input-password v-model:value="loginForm.password" placeholder="请输入密码">
              <template #prefix><LockOutlined style="color: rgba(0, 0, 0, 0.25)" /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item style="margin-top:24px">
            <a-button
              type="primary"
              html-type="submit"
              @click="handleLogin"
              style="width:100%"
              :loading="loading"
            >
              登录
            </a-button>
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>
<script>
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { login } from "@/http/api/login";
export default {
  components: {
    UserOutlined,
    LockOutlined,
  },
  name: "Login",
  data() {
    return {
      title: "词条翻译工具",
      loginForm: {
        account: "",
        password: "",
      },
      loading: false,
      passwordType: "password",
      redirect: undefined,
      bgImg: {
        backgroundImage: "url(" + require("../../assets/loginImg.png") + ")",
        backgroundRepeat: "no-repeat",
        backgroundSize: "100% 100%",
      }
    };
  },
  methods: {
    
    handleLogin() {
      if(this.loginForm.account === "" || this.loginForm.account === null){
        message.info("请输入用户名！")
        return
      }
      if(this.loginForm.password === "" || this.loginForm.password === null){
        message.info("请输入密码！")
        return
      }
      this.loading = true
      login(this.loginForm).then((res) => {
          // message.info('登录成功！')
          this.$store.commit("setData", res.data)
          if(res.data.menu.length === 0){
            this.$router.push('/notPermission');
          }else{
            const redirect = this.$route && this.$route.query && this.$route.query.redirect;// 重定向地址，场景：登录某个路由页面但是当前未登录->跳转登录页，登录->根据这个重定向跳转之前访问的页面，登录成功后跳转
            if (redirect) {
              window.location.href = redirect;
            } else {
              this.$router.push('/translate');
            }
          }
          this.loading = false
      }).catch((err) => {
          // console.log(err);
          message.error("登录失败！",err.message)
          this.loading = false
      });
    }
  },
};
</script>
<style scoped>
.loginBg{
  width: 100%;
  height: calc(100% - 40px);
  /* background-image: url('../../assets/loginImg.png');
  background-repeat: no-repeat;
  background-size : 100% 100%; */
  position: relative;
}
.loginBox{
  width: 300px;
  height: 310px;
  position: absolute;
  top: 20%;
  right: 12%;
}
.loginBox .title{
  width: 100%;
  height: 40px;
  text-align: center;
  margin-bottom: 10px;
}
.title span{
  font-size: 24px;
  font-family: Microsoft YaHei;
  font-style: normal;
  font-weight: 700;
  line-height: 40px;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 1), rgba(226, 250, 255, 0.8));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.loginBox .loginForm{
  width: 100%;
  height: calc(100% - 40px);
  background-color: white;
  padding: 15px 20px;
  text-align: center;
  border-radius:5px;
}
.loginForm .welcome{
  color: var(--primary-blue-normal, #369FFF);
  font-size: 16px;
  font-family: Microsoft YaHei;
  font-style: normal;
  font-weight: 700;
  margin-bottom: 15px;
}
/* .ant-form{
  width: 400px;
  position: relative;
  left: 50%;
  top: 30%;
  -webkit-transform: translate(-50%,-50%);
  -ms-transform: translate(-50%,-50%);
  transform: translate(-50%,-50%);
} */
.ant-form-item{
  margin-bottom: 12px;
}
</style>