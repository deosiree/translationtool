<template>
  <div style="width: 100%; height: 100%; padding: 8px 24px 24px 24px">
    <a-tabs v-model:activeKey="activeKey" ref="tab" @change="changeTab($event)">
      <a-tab-pane v-for='(item) in menu' :key="item.name" :tab="item.menuName">
        <Code v-if="item.name === 'code'" />
        <a-tabs v-if="item.name === 'translation' &&item.children" v-model:activeKey="item.activeKey" @change="changeTab(item.name, $event)">
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
      let list = this.$store.state.menu;
      for (let item of list) {
        if (item.url === this.$route.path && item.children.length > 0) {
          this.menu = item.children;
        }
      }
      // 从 store 中获取 tabActive 状态
      const tabActive = this.$store.state.tabActive;
      if (tabActive) {
        const [parentName, childName] = tabActive.split("/"); // 解析完整的 activeKey 来设置子菜单的 activeKey
        this.changeTab(parentName, childName);
      } else {
        this.changeTab(this.menu[0].name); // 如果 store 中没有 tabActive 状态，设置默认激活项
      }
    });
  },
  methods: {
    changeTab(parentName, activeKey = null) {
      this.activeKey = parentName; // 更新父菜单的 activeKey
      const parentMenu = this.menu.find((item) => item.name === parentName);
      let fullActiveKey = "";
      if (parentMenu.children) {
        // 如果有子菜单
        let childActiveKey = "";
        if (!activeKey) {
          // 如果默认使用子菜单的激活项
          childActiveKey = parentMenu.children[0].name;
        } else {
          // 如果使用传入的激活项
          childActiveKey = activeKey;
        }
        parentMenu.activeKey = childActiveKey; // 更新子菜单的 activeKey
        fullActiveKey = `${parentName}/${childActiveKey}`;
      } else {
        // 如果没有子菜单
        fullActiveKey = parentName;
      }
      // 提交 mutation 更新 store 中的 tabActive 状态(要以“父亲/儿子”的格式进行激活项的存储，方便后续的路由跳转)
      this.$store.commit("setTabActive", fullActiveKey);
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
