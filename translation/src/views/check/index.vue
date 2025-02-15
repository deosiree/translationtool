<template>
  <div style="width: 100%; height: 100%; padding: 8px 24px 24px 24px">
    <a-tabs v-model:activeKey="activeKey" ref="tab" @change="changeTab">
      <a-tab-pane v-for='(item) in menu' :key="item.name" :tab="item.menuName">
        <Code v-if="item.name === 'code'" />
        <a-tabs v-if="item.name === 'translation' &&item.children" v-model:activeKey="item.activeKey" @change="changeSubTab(item.name, $event)">
          <a-tab-pane v-for='(subItem) in item.children' :key="subItem.name" :tab="subItem.menuName">
            <!-- 根据子菜单的 name 渲染对应的组件 -->
            <File v-if="subItem.name === 'file'" />
            <RedundantEntry v-if="subItem.name === 'redundantEntry'" />
          </a-tab-pane>
        </a-tabs>
        <Qt v-if="item.name === 'qt'" />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>
<script>
import Code from '@/views/check/codeCheck.vue'
import Qt from '@/views/check/qtCheck.vue'
import File from '@/views/check/fileCheck.vue'
import RedundantEntry from '@/views/check/redundantEntryCheck.vue'

export default ({
  name: 'layout',
  components: {
    Code,
    File,
    RedundantEntry,
    Qt,
  },
  data() {
    return {
      activeKey: "",
      menu: [
        { name: 'code', menuName: "代码编写检查" },
        { 
          name: 'translation', 
          menuName: "翻译产物检查",
          activeKey: '',
          children: [
            { name: 'file', menuName: "文件校验"},
            { name: 'redundantEntry', menuName: "冗余词条校验"}
          ]
        },
        { name: 'qt', menuName: "qt机制检查" },
      ]
    };
  },
  mounted() {
    // this.$nextTick(() => {
    //   // console.log(this.$store.state.tabActive)
    //   // 页面加载完成后执行的代码
    //   let list = this.$store.state.menu
    //   for (var item of list) {
    //     if (item.url === this.$route.path && item.children.length > 0) {
    //       this.menu = item.children
    //       // this.activeKey = this.menu[0].name
    //     }
    //   }
    //   if (this.menu.length > 0) {
    //     this.activeKey = this.$store.state.tabActive === null ? this.menu[0].name : this.$store.state.tabActive
    //   }
    // })
  },
  methods: {
    changeTab(activeKey) {
      // console.log(activeKey);
      this.$store.commit("setTabActive", activeKey)
    },
    changeSubTab(parentName, activeKey) {
      // 找到父菜单
      const parentMenu = this.menu.find(item => item.name === parentName);
      if (parentMenu) {
        // 设置子菜单的激活项
        parentMenu.activeKey = activeKey;
      }
    }
  },
})
</script>
<style lang="less" scoped>
:deep(.ant-tabs-nav) {
  margin: 0 0 10px 0;
}

:deep(.ant-tabs) {
  height: 100%;
}

:deep(.ant-tabs-content) {
  height: 100%;
  position: relative;
}

:deep(.ant-tabs-tab-btn) {
  font-size: 12px;
}
</style>
