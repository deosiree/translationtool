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
import Code from "@/views/check/codeCheck.vue";
import File from "@/views/check/fileCheck.vue";
import RedundantEntry from "@/views/check/redundantEntryCheck.vue";
import Qt from "@/views/check/qtCheck.vue";

export default {
  name: "layout",
  components: {
    Code,
    File,
    RedundantEntry,
    Qt,
  },
  data() {
    return {
      activeKey: "code",
      menu: [
        { name: "code", menuName: "代码编写检查" },
        {
          name: "translation",
          menuName: "翻译产物检查",
          activeKey: "file",
          children: [
            { name: "file", menuName: "文件校验" },
            { name: "redundantEntry", menuName: "冗余词条校验" },
          ],
        },
        { name: "qt", menuName: "qt机制检查" },
      ],
    };
  },
  mounted() {
    this.$nextTick(() => {
      // console.log(this.$store.state.tabActive)
      // 页面加载完成后执行的代码

      while (0) {
        // let list = this.$store.state.menu;
        // console.log("拿到store中存储的menu", list);
        // console.log("此时的this.menu", this.menu);// 没用，那是写到后端数据库的路由，这个页面根本进这个循环没意义
        // for (var item of list) {
        //   if (item.url === this.$route.path && item.children.length > 0) {
        //     this.menu = item.children;
        //     this.activeKey = this.menu[0].name;
        //     console.log("拿到store中存储的当前页面的menu", this.menu);
        //     console.log("拿到store中存储的当前页面的activeKey", this.activeKey);
        //   }
        // }
      }

      console.log("1.当前组件的数据：", this);
      // 检查当前组件的菜单是否有菜单项
      if (this.menu.length > 0) {
        console.log("2.当前组件的菜单有菜单项", this.menu);
        // 如果 store 中的 tabActive 状态为 null，则将激活的标签页设置为菜单的第一个菜单项
        // 否则，将激活的标签页设置为 store 中的 tabActive 状态
        this.activeKey =
          this.$store.state.tabActive === null
            ? this.menu[0].name
            : this.$store.state.tabActive;
      }
      console.log("3.当前激活的标签页：", this.activeKey);
    });
  },
  methods: {
    changeTab(activeKey) {
      console.log("6.更新前菜单的激活项：", activeKey);
      // 提交 mutation 更新 store 中的 tabActive 状态
      this.$store.commit("setTabActive", activeKey);
      console.log("7.更新后菜单的激活项", this.$store.state.tabActive);
    },
    changeSubTab(parentName, activeKey) {
      console.log("进入子菜单的changeTab方法", parentName, activeKey);
      // 找到父菜单
      const parentMenu = this.menu.find((item) => item.name === parentName);
      if (parentMenu.children) {
        console.log("4.有子菜单", parentMenu);
        // 设置子菜单的激活项
        parentMenu.activeKey = activeKey;
        console.log("5.设置子菜单的激活项", parentMenu.activeKey);
        // 提交 mutation 更新 store 中的 tabActive 状态
        console.log("6.更新前菜单的激活项：", activeKey);
        this.$store.commit("setTabActive", activeKey);
        console.log("7.更新后菜单的激活项", this.$store.state.tabActive);
      } else {
        console.log("4.没有子菜单");
        console.log("6.更新前菜单的激活项：", activeKey);
        // 提交 mutation 更新 store 中的 tabActive 状态
        this.$store.commit("setTabActive", fullActiveKey);
        console.log("7.更新后菜单的激活项", this.$store.state.tabActive);
      }
    },
  },
};
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
