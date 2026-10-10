<template>
  <a-layout>
    <a-layout-sider v-model:collapsed="collapsed" :trigger="null" collapsible>
      <div class="logo" style="text-align:center">
        <!-- <VideoCameraOutlined /> -->
        <span v-if="!collapsed" style="color:white;line-height:32px;font-size:18px">词条翻译工具</span>
      </div>
      <a-menu 
      theme="dark" 
      mode="inline" 
      :selectedKeys="selectedKeys" 
      :defaultSelectedKeys="[$route.path]"
      @click="clickMenu">
        <template v-for='(item) in menu'>
          <a-sub-menu :key="item.url" v-if='item.children.length > 0'>
            <template #title>
              <span>
                <BarsOutlined />
                <span>{{item.menuName}}</span>
              </span>
            </template>
            <a-menu-item v-for="(itChild) in item.children" :key="itChild.url">
              <span>{{itChild.menuName}}</span>
            </a-menu-item>
          </a-sub-menu>
 
          <a-menu-item :key="item.url" v-else>
            <BarsOutlined />
            <span>{{item.menuName}}</span>
          </a-menu-item>
        </template>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header style="background: #fff; padding: 0">
        <menu-unfold-outlined
          v-if="collapsed"
          class="trigger"
          @click="() => (collapsed = !collapsed)"
        />
        <menu-fold-outlined v-else class="trigger" @click="() => (collapsed = !collapsed)" />
        <span class="user">登陆人：{{$store.state.user?.userName || ''}}_{{$store.state.user?.department || ''}}</span>
        <a-dropdown>
          <a-avatar style="background-color: #87d068;">
            <template #icon>
              <UserOutlined />
            </template>
          </a-avatar>
          <template #overlay>
            <a-menu>
              <a-menu-item key="1" @click="logout">退出系统</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-layout-header>
      <a-layout-content
        :style="{ margin: '10px 10px 24px 10px', padding: '10px', background: '#fff', minHeight: '280px' }"
      >
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
<script>
import {
  UserOutlined,
  VideoCameraOutlined,
  UploadOutlined,
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  BarsOutlined,
  DownOutlined
} from '@ant-design/icons-vue';
export default {
  components: {
    UserOutlined,
    VideoCameraOutlined,
    UploadOutlined,
    MenuUnfoldOutlined,
    MenuFoldOutlined,
    BarsOutlined,
    DownOutlined
  },
  data() {
    return {
      collapsed:false,
      menu:[],
      selectedKeys: [],
      defaultSelectedKeys: []
    };
  },
  watch: {
    
  },
  mounted() {
    this.$nextTick(() => {
      // 页面加载完成后执行的代码
      this.menu = this.$store.state.menu
      //默认选中第一个导航
      let currentPath = this.$route.path
      if(currentPath === '/translate'){
        if(this.menu.length > 0){
          let item = this.menu[0]
          if(item.children.length > 0){
            this.selectedKeys = [item.children[0].url]
            this.$router.push(item.children[0].url)
          }else{
            this.selectedKeys = [item.url]
            this.$router.push(item.url)
          }
        }
      }else{
        this.selectedKeys = [currentPath]
      }
    })
  },
  computed: {
    
  },
  methods: {
    clickMenu(item){
      this.$router.push(item.key)
      this.selectedKeys = [item.key]
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
    .ant-layout-sider-children .logo{
      height: 32px;
      background: rgba(255, 255, 255, 0.3);
      margin: 16px;
    }
    .ant-layout{
      height: calc(100% - 30px);
    }
    .ant-layout-header{
      display:flex;
      align-items:center;
    }
    .ant-layout-header .trigger {
      font-size: 18px;
      line-height: 64px;
      padding: 0 10px;
      cursor: pointer;
      transition: color 0.3s;
    }

    .ant-layout-header  .trigger:hover {
      color: #1890ff;
    }
    .ant-layout-header .ant-dropdown-trigger{
      position: absolute;
      right: 10px;
    }
    .ant-layout-header .user{
      position: absolute;
      right: 50px;
    }
    
</style>
