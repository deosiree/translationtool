<template>
  <!-- 修改按钮文本为全量查询 -->
  <a-button type="primary" @click="showSearchModal" :size="size">{{ buttonTitle }}</a-button>

  <CustomModal :modalTitle="buttonTitle" width="500px" :visible="searchVisible" :showCancel="false" :showOk="false" @handleClose="handleClose">
    <div class="content">
      <a-form ref="searchForm" :model="searchModal">
        <!-- 修改标签为全量字段 -->
        <a-form-item label="全量字段" name="field" :rules="[{ required: true, message: '请选择!' }]">
          <div style="display: flex; justify-content: space-between;">
            <a-select mode="multiple" v-model:value="searchModal.field" :options="processedFieldOptions" placeholder="请选择全量字段" allowClear
              style="flex: 1; margin-right: 8px;" />
            <a-button type="link" size="small" @click="selectAllFields" style="
              font-size: smaller;margin-top:0">全选</a-button>
          </div>
        </a-form-item>
      </a-form>
    </div>
    <template #leftBottomBtn>
      <a-button key="back" @click="handleClose">取消</a-button>
      <a-button type="primary" @click="handleSearch" :loading="searchLoading">确定</a-button>
    </template>
  </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import { setModalAriaHidden } from "@/utils/commonUtils.js";

export default {
  components: {
    CustomModal,
  },
  emits: ["searchFunction","update:accurSearch"],
  props: {
    fieldOptions: {
      type: Array,
      required: true,
    },
    size: {
      type: String,
      default: "small",
    },
    buttonTitle: {
      type: String,
      default: "全量查询",
    },
  },
  computed: {
    // 处理 fieldOptions 数组，转换为包含 label 和 value 的格式
    processedFieldOptions() {
      return this.fieldOptions.map((item) => ({ label: item, value: item }));
    },
  },
  data() {
    return {
      searchVisible: false,
      searchLoading: false,
      searchModal: {
        field: [],
      },
    };
  },
  methods: {
    showSearchModal() {
      this.searchVisible = true;
      setModalAriaHidden(this, document);
      // 从本地缓存读取用户偏好
      const cachedButton = localStorage.getItem("button-accurSearch");
      // console.log("读取用户偏好1", cachedButton);
      this.searchModal.field = cachedButton
        ? JSON.parse(cachedButton)
        : this.fieldOptions;
      // console.log("读取用户偏好", this.searchModal.field);
    },
    // 全选全量字段方法
    selectAllFields() {
      this.searchModal.field = this.fieldOptions;
    },
    // 全量查询 - 确认
    async handleSearch() {
      this.searchLoading = true;
      try {
        if (!this.searchModal.field || this.searchModal.field.length === 0) {
          message.error("请选择全量字段！");
          return;
        }

        await this.$refs.searchForm.validate();
        // 调用外部传递的查询函数
        this.$emit("searchFunction", this.searchModal.field);
        this.$emit("update:accurSearch", this.searchModal.field);
        // console.log("保存用户偏好", this.searchModal.field);
        this.searchVisible = false;
        // 保存用户偏好到本地缓存
        localStorage.setItem(
          "button-accurSearch",
          JSON.stringify(this.searchModal.field)
        );
      } catch (error) {
        // console.log("全量查询失败原因", error);
        message.error(`全量查询失败: ${error.message || error}`);
      } finally {
        this.searchLoading = false;
      }
    },
    // 记录用户偏好
    exportFieldChange(value) {
      let data = {
        exportColumn: value.join(","),
      };
      updateUserPartiality(data).then((res) => {});
    },
    // 关闭全量查询模态框
    handleClose() {
      this.searchVisible = false;
    },
  },
};
</script>