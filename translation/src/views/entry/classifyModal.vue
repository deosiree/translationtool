<template>
  <Modal :modalWidth="modalWidth" :visible="visible" :modalTitle="modalTitle" @handleClose="handleClose" @handleOK="handleOK" :okLoading="loading"
    @afterClose="afterClose">
    <div class="content">
      <a-form ref="formRef" name="custom-validation" autocomplete='off' :model="classify" :label-col="labelCol">
        <a-form-item label="名称" name="title" :rules="[{ required: true, message: '请输入名称!' }]">
          <a-input v-model:value="classify.title" placeholder="请输入内容"></a-input>
        </a-form-item>
        <a-form-item v-if="(modalTitle === '添加产品' || modalTitle === '编辑产品')&&$currentDepartment && $currentDepartment.ops.has('needBranch')" label="归档分支" name="codeBranch"
          :rules="[{ message: '请输入归档分支(与代码分支相对应)!' }]">
          <a-input v-model:value="classify.codeBranch" placeholder="请输入归档分支(与代码分支相对应)"></a-input>
        </a-form-item>
        <a-form-item v-if="modalTitle === '编辑分类'&&$currentDepartment && $currentDepartment.ops.has('needBranch')" label="批量修改分支" name="codeBranchs">
          <a-input v-model:value="classify.codeBranch" placeholder="批量修改其中所有产品的归档分支" style="width:100%"></a-input>
        </a-form-item>
        <a-form-item v-if="modalTitle === '添加模块' || modalTitle === '编辑模块'" label="中文限制字符数" name="maxByte">
          <a-input-number v-model:value="classify.maxByte" placeholder="请输入中文限制字符数" style="width:100%"></a-input-number>
        </a-form-item>
        <a-form-item v-if="modalTitle === '添加模块' || modalTitle === '编辑模块'" label="外文限制字符数" name="foreignMaxByte">
          <a-input-number v-model:value="classify.foreignMaxByte" placeholder="请输入外文限制字符数" style="width:100%"></a-input-number>
        </a-form-item>
      </a-form>
    </div>
  </Modal>
</template>
<script>
import Modal from "@/components/modal/index.vue";
import { addEntryClassfy, updateEntryClassfy } from "@/http/api/entryManage";
import { addProduct, updateProduct } from "@/http/api/product";
import { message } from "ant-design-vue";
import { v4 as uuidv4 } from "uuid";
export default {
  components: {
    Modal,
  },
  emits: ["classifyClose"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
    },
    currentClass: {},
    treeNode: {},
  },
  data() {
    return {
      labelCol: { style: { width: "130px" } },
      modalWidth: "400px",
      classify: {
        title: "",
        maxByte: "",
        foreignMaxByte: "",
        codeBranch: "",
      },
      loading: false,
    };
  },

  created() {},
  mounted() {
    this.classify = this.currentClass;
    this.$nextTick(() => {
      this.user = this.$store.state.user;
    });
  },
  watch: {
    currentClass(newval, oldval) {
      this.classify = newval;
    },
  },
  methods: {
    // 遍历treeNodes，批量修改其中所有的产品
    async batchUpdateClassify(treeNodes) {
      for (const child of treeNodes) {
        const params = {
          key: child.key,
          codeBranch: this.classify.codeBranch, // 暂时只修改codeBranch
        };
        try {
          await updateEntryClassfy(params);
        } catch (error) {
          console.error("更新产品失败:", error, child);
        }
        await this.batchUpdateClassify(child.children);
      }
    },
    handleClose() {
      this.$emit("classifyClose", true);
    },
    async handleOK() {
      try {
        this.loading = true;

        await this.$refs.formRef.validate();

        if (this.modalTitle === "添加分类" || this.modalTitle === "添加模块") {
          await addEntryClassfy(this.classify);
          message.success("新增成功！");
        } else if (
          this.modalTitle === "编辑分类" ||
          this.modalTitle === "编辑模块"
        ) {
          // 第一步：先完成批量修改所有产品版本
          if (
            this.modalTitle === "编辑分类" &&
            this.treeNode &&
            this.treeNode.children
          ) {
            // console.log("批量修改产品版本", this.classify, this.treeNode);
            await this.batchUpdateClassify(this.treeNode.children);
          }
          // 第二步：再执行当前分类的修改
          await updateEntryClassfy(this.classify);
          message.success("编辑成功！");
        } else if (this.modalTitle === "添加产品") {
          this.classify.key = uuidv4(); // 后端需要，不传会报错(前端加key，可能会重复)
          let data = {
            id: this.classify.key,
            name: this.classify.title,
            parentId: this.classify.parentId,
            codeBranch: this.classify.codeBranch,
          };
          // 产品表添加产品
          await addProduct(data);
          // 分类表添加产品
          await addEntryClassfy(this.classify);
          message.success("添加成功！");
        } else if (this.modalTitle === "编辑产品") {
          await updateEntryClassfy(this.classify);
          let data = {
            id: this.classify.key,
            name: this.classify.title,
            codeBranch: this.classify.codeBranch,
          };
          await updateProduct(data);
          message.success("编辑成功！");
        }
        this.$emit("classifyClose");
      } catch (err) {
        console.error("验证失败或操作错误:", err);
      } finally {
        this.loading = false;
      }
    },
    afterClose() {
      this.classify.title = "";
      this.classify.maxByte = "";
      this.$refs.formRef.clearValidate();
    },
  },
};
</script>
<style scoped>
:deep(.ant-form-item-label) {
  width: 130px;
}
.content {
  width: 100%;
  height: 100%;
  padding: 10px;
  background-color: #f3f3f3;
}
</style>