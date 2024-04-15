<template>
    <a-result status="404" title="404" sub-title="暂无权限，请联系管理员分配权限">
      <template #extra>
        <a-button type="primary" @click="logout">重新登录</a-button>
      </template>
    </a-result>
</template>
<script>
export default {
  components: {
  },
  data() {
    return {

    };
  },
  mounted() {

  },
  methods: {
    logout(){
      //删除缓存数据
      this.$store.commit("removeData")
      //跳转登录页面
      this.$router.push("/")
    }
  },
}
</script>

<style scoped>
.ant-empty{
    margin-top: 10%;
}
.ant-empty span{
    color: #999fa1;
}
</style>