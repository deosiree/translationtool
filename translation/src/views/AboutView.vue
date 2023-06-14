<template>
  <div class="about">
    <h1>This is an about page</h1>
    <a-button
        type="primary"
        html-type="submit"
        @click="handleLogin"
      >
        工作台
    </a-button>
    <a-button
        type="primary"
        html-type="submit"
        @click="handleLogin2"
      >
        词条管理
    </a-button>
    <a-button
        type="primary"
        html-type="submit"
        @click="handleLogin3"
      >
        用户管理
    </a-button>
    <a-button
        type="primary"
        html-type="submit"
        @click="handleLogin4"
      >
        版本管理
    </a-button>
    <a-button
        type="primary"
        html-type="submit"
        @click="handleLogin5"
      >
        角色管理
    </a-button>
  </div>
</template>
<script>
import { testToken } from "@/http/login";
export default {
  data() {
    return {
      title: "电网业务资源交互中台模拟工具",
      loginForm: {
        account: "",
        password: "",
      },
      loginRules: {
        account: [
          {
            required: true,
            trigger: "change",
          },
        ],
        password: [
          {
            required: true,
            trigger: ["blur", "change"],
          },
        ],
      },
      loading: false,
      passwordType: "password",
      redirect: undefined,
      testCaseRoutes: [],
      configMangeRoutes: [],
    };
  },
  methods: {
    
    handleLogin() {    
      this.$router.push('/workbench');
    },
    handleLogin2() {
      this.$router.push('/entry');
    },
    handleLogin3() {
      this.$router.push('/user');
    },
    handleLogin4() {
      this.$router.push('/version');
    },
    handleLogin5() {
      this.$router.push('/role');
    }
    
  },
};
</script>