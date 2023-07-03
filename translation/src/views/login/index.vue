<template>
  <div class="loginBg">
    <div class="loginBox">
      <div class="title"><span>{{title}}</span></div>
      <div class="loginForm">
        <div class="welcome">欢迎登录</div>
        <a-form
          :model="loginForm"
        >
          <a-form-item 
          :rules="[{ required: true, message: 'Please input your username!' }]"
          >
            <a-input v-model:value="loginForm.account" placeholder="请输入用户名">
              <template #prefix><UserOutlined style="color: rgba(0, 0, 0, 0.25)" /></template>
            </a-input>
          </a-form-item>
          <a-form-item>
            <a-input-password v-model:value="loginForm.password" placeholder="请输入密码">
              <template #prefix><LockOutlined style="color: rgba(0, 0, 0, 0.25)" /></template>
            </a-input-password>
          </a-form-item>
          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              @click="handleLogin"
              style="width:100%"
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
    };
  },
  methods: {
    
    handleLogin() {
        login(this.loginForm).then((res) => {
            message.info('登录成功！')
            this.$store.commit("setData", res.data)
            if(res.data.menu.length === 0){
              this.$router.push('/notPermission');
            }else{
              this.$router.push('/translate');
            }
        }).catch((err) => {
            console.log(err);
        });
        
    }
    
  },
};
</script>
<style scoped>
.loginBg{
  width: 100%;
  height: calc(100% - 30px);
  background-image: url('@/assets/loginImg.png');
  background-repeat: no-repeat;
  background-size : 100% 100%;
  position: relative;
}
.loginBox{
  width: 300px;
  height: 280px;
  position: absolute;
  top: 20%;
  right: 12%;
}
.loginBox .title{
  width: 100%;
  height: 40px;
  text-align: center;
}
.title span{
  font-size: 18px;
  font-family: Microsoft YaHei;
  font-style: normal;
  font-weight: 700;
  line-height: 40px;
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
  margin-bottom: 20px;
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

</style>