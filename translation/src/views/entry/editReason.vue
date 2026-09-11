<template>
  <Modal :visible="visible" :modalTitle="modalTitle" :okLoading="okLoading" @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose">
    <div class="content">
      <a-form ref="formRef" name="custom-validation" :model="edit">
        <a-form-item has-feedback label="编辑原因" name="reason" :rules="[{ required: true, message: '请输入编辑原因!' }]">
          <a-textarea v-model:value="edit.reason" placeholder="请输入编辑原因" />
        </a-form-item>
      </a-form>
    </div>
  </Modal>
</template>
<script>
import Modal from "@/components/modal/index.vue";
import { updateEntryInfoList } from "@/http/api/entryManage";
import { message } from "ant-design-vue";
import { partitionBatchUpdateResults } from "@/utils/batchUpdateResults.js";
export default {
  components: {
    Modal,
  },
  emits: ["editClose", "editOk"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
      default: "编辑原因",
    },
    entry: {
      type: Array,
      default: [],
    },
  },

  data() {
    return {
      edit: {
        reason: "",
      },
      editEntry: [],
      okLoading: false,
    };
  },

  created() {},
  mounted() {
    this.editEntry = this.entry;
    this.$nextTick(() => {});
  },
  watch: {
    entry(newval) {
      this.editEntry = newval;
    },
  },
  methods: {
    async handleOK() {
      this.okLoading = true;
      try {
        await this.$refs.formRef.validate();
        const params = {
          notes: this.edit.reason,
        };
        const res = await updateEntryInfoList(this.editEntry, params);
        const { successIds, failed } = partitionBatchUpdateResults(res);
        const successIdSet = new Set(successIds);

        for (const entry of this.editEntry) {
          if (successIdSet.has(entry.id)) {
            this.$emit("editOk", entry);
          }
        }

        if (failed.length > 0) {
          const detail = failed
            .map((item) => `${item.id}: ${item.message}`)
            .join("；");
          message.warning(`有 ${failed.length} 条保存失败：${detail}`);
        }
        if (successIds.length > 0) {
          message.success(`已保存 ${successIds.length} 条！`);
        }
      } catch (err) {
        // 表单校验失败或接口 reject
      } finally {
        this.okLoading = false;
      }
    },
    handleClose() {
      this.$emit("editClose");
    },
    afterClose() {
      this.edit.reason = "";
      this.$refs.formRef.clearValidate();
    },
  },
};
</script>
<style scoped>
.content {
  width: 100%;
  height: 100%;
  padding: 10px;
  background-color: #f3f3f3;
}
</style>
