<template>
  <!-- 创建版本：词条管理-已选词条-底部"创建产品版本"的二级表单弹窗 -->
  <CustomModal modalTitle="创建版本" modalWidth="500px" :modalVisible="visible" @handleClose="handleClose"
    @handleOK="handleOK" @afterClose="afterClose">
    <div style="width:100%;height:100%">
      <a-form :model="version" autocomplete="off" ref="versionForm" :label-col="{ span: 6 }">
        <a-form-item label="产品版本名称" name="versionName" :rules="[{ required: true, message: '请输入版本名称!' }]">
          <a-input v-model:value="version.versionName" placeholder="请输入版本名称"></a-input>
        </a-form-item>
        <a-form-item label="备注" name="remarks">
          <a-textarea v-model:value="version.remarks" placeholder="请输入备注" :rows="4" />
        </a-form-item>
      </a-form>
    </div>
  </CustomModal>
</template>
<script>
import CustomModal from "@/components/modal/index.vue";
import { message } from "ant-design-vue";
import { createVersionByEntry } from "@/http/api/entryManage";
import { setModalAriaHidden } from "@/utils/domUtils";
export default {
  components: { CustomModal },
  emits: ["handleClose", "finished"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    // 已选词条
    dataSource: {
      type: Array,
      default: () => [],
    },
    // 当前产品（取 productID）
    product: {
      type: Object,
      default: () => ({}),
    },
  },
  data() {
    return {
      version: {
        versionName: "",
        remarks: "",
      },
    };
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        setModalAriaHidden(this, document);
      }
    },
  },
  methods: {
    handleClose() {
      this.$emit("handleClose");
    },
    async handleOK() {
      try {
        await this.$refs.versionForm.validate();
        let params = {
          productID: this.product.key,
          versionName: this.version.versionName,
          common: this.version.remarks,
        };
        await createVersionByEntry(params, this.dataSource);
        message.success("创建版本完成！");
        this.$emit("handleClose");
        this.$emit("finished");
      } catch (err) {
        console.log("创建版本失败:", err);
      }
    },
    afterClose() {
      this.version = { versionName: "", remarks: "" };
    },
  },
};
</script>
