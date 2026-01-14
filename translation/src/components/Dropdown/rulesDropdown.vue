<template>
  <div class="validation-rules-dropdown">
    <!-- 下拉框触发按钮 -->
    <a-dropdown :trigger="['click']" :overlay-style="{ minWidth: '180px' }">
      <!-- 触发按钮内容（可自定义） -->
      <a-button type="text" class="dropdown-trigger">
        {{ title }}
        <DownOutlined />
      </a-button>
      <!-- 下拉菜单内容 -->
      <template #overlay>
        <a-menu class="validation-menu">
          <!-- 动态渲染校验规则选项 -->
          <a-menu-item v-for="option in options" :key="option.key" class="menu-item">
            <a-checkbox v-model:checked="option.checked">
              {{ option.label }}
            </a-checkbox>
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</template>
<script>
import { DownOutlined } from "@ant-design/icons-vue";
import commonParam from "@/constants/commonParam.js";
export default {
  components: {
    DownOutlined,
  },
  emits: ["update:options"],
  props: {
    title: {
      type: String,
      default: "校验规则",
    },
    options: {
      type: Array,
      default: () => commonParam.rulesOptions,
    },
  },
  data() {
    return {};
  },
  watch: {
    options: {
      immediate: true, // 初始化时立即执行
      deep: true, // 深度监听
      handler(newVal) {
        // 处理 options 变化
        this.$emit("update:options", newVal);
        console.log("校验规则发生变化",newVal)
      },
    },
  },
  methods: {},
};
</script>

<style scoped>
.validation-rules-dropdown {
  display: inline-block;
  margin: 8px 0;
}

.dropdown-trigger {
  padding: 4px 8px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  gap: 4px; /* 图标与文字间距 */
}

.validation-menu {
  padding: 8px 0;
}

.menu-item {
  padding: 8px 16px;
  display: flex;
  align-items: center;
}

.menu-item .ant-checkbox {
  margin-right: 8px;
}
</style>