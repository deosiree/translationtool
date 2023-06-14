<template>
  <a-form
    :model="loginForm"
  >
    <a-form-item>
      <a-input v-model:value="loginForm.account" placeholder="Account">
        <template #prefix><UserOutlined style="color: rgba(0, 0, 0, 0.25)" /></template>
      </a-input>
    </a-form-item>
    <a-form-item>
      <a-input v-model:value="loginForm.password" type="password" placeholder="Password">
        <template #prefix><LockOutlined style="color: rgba(0, 0, 0, 0.25)" /></template>
      </a-input>
    </a-form-item>
    <a-form-item>
      <a-button
        type="primary"
        html-type="submit"
        @click="handleLogin"
        :disabled="loginForm.account === '' || loginForm.passWord === ''"
      >
        Log in
      </a-button>
    </a-form-item>
  </a-form>
</template>
<script>
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { login } from "@/http/login";
export default {
  components: {
    UserOutlined,
    LockOutlined,
  },
  name: "Login",
  data() {
    return {
      title: "",
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
            this.$router.push('/translate');
        }).catch((err) => {
            console.log(err);
        });
        
    }
    
  },
};
</script>
<style>
.ant-form{
  width: 400px;
  position: relative;
  left: 50%;
  top: 30%;
  -webkit-transform: translate(-50%,-50%);
  -ms-transform: translate(-50%,-50%);
  transform: translate(-50%,-50%);
}
</style>