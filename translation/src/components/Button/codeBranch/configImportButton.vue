<template>
  <a-button type="primary" @click="importConfig" :size="size">
    <template #icon>
      <PlusOutlined />
    </template>
    {{ buttonTitle }}
  </a-button>

  <CustomModal :visible="importVisible" :okLoading="importLoading" :modalTitle="buttonTitle" @handleClose="importClose" @handleOK="importOK">
    <div class="content">
      <a-form ref="otherForm" name="custom-validation" :model="importModal">
        <a-form-item label="部门" name="department" :rules="[{ required: true, message: '请选择!' }]">
          <a-select allowClear show-search placeholder="用于提供配置文件选项" :options="departmentOptions" v-model:value="importModal.department">
          </a-select>
        </a-form-item>
        <a-form-item label="配置文件" name="config" :rules="[{ required: true, message: '请选择!' }]">
          <a-select allowClear show-search placeholder="选择配置文件" :options="configOptions" v-model:value="importModal.config">
          </a-select>
        </a-form-item>
        <!-- <a-form-item label="子分类名" name="sub-classify">
                <a-input v-model:value="codeBranch" placeholder="所有其他文件将存入子分类中" :rules="[{ required: true, message: '请输入子分类名，所有其他文件将存入子分类中' }]"
                  style="width: 400px;"></a-input>
              </a-form-item> -->
      </a-form>
    </div>
  </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import { PlusOutlined } from "@ant-design/icons-vue";
import { setModalAriaHidden } from "@/utils/domUtils";
import { createBranchParams } from "@/utils/commonParam.js";
import { cloneDeep } from "lodash-es";
export default {
  components: {
    CustomModal, // 注册 CustomModal 组件
    PlusOutlined,
  },
  emits: ["configList"],
  props: {
    department: {
      type: String,
      default: null,
    },
    size: {
      type: String,
      default: "small",
    },
    buttonTitle: {
      type: String,
      default: "配置新增", // 设置默认值为 "配置新增"
    },
  },
  data() {
    return {
      importVisible: false,
      importLoading: false,
      importModal: {
        department: null,
        config: null,
      },
      configJson: createBranchParams.otherConfig, // 前端方案：根据公共参数中定死的部门选项
      departmentOptions: [],
      configOptions: [],
      configMap: new Map(),
    };
  },
  watch: {
    "importModal.department": {
      handler(newValue, oldValue) {
        if (this.importVisible && newValue !== oldValue) {
          console.log("切换部门", newValue);
          this.getConfigOptions();
        }
      },
      immediate: false,
    },
  },
  methods: {
    // 获取可选部门
    async getDepartmentOptions() {
      this.importModal.department = this.department; // 默认当前用户所在部门

      // 前端方案(提供给其他部门时要有后端写入/读取的方案)：根据公共参数中定死的部门选项
      this.departmentOptions = Object.keys(this.configJson).map((key) => ({
        label: key,
        value: key,
      }));
      console.log("部门选项2", this.departmentOptions);
    },
    // 根据部门获取配置文件选项
    async getConfigOptions() {
      if (!this.importModal.department) {
        this.configOptions = [];
        this.configMap = new Map();
        this.importModal.config = null; // 清空配置文件选项
        return;
      }

      // 前端方案(提供给其他部门时要有后端写入/读取的方案)：根据公共参数中定死的部门选项
      const configs = this.configJson[this.importModal.department];
      for (const [key, value] of Object.entries(configs)) {
        this.configOptions.push({
          label: key,
          value: key,
        });

        this.configMap.set(key, value);
      }


      if (this.configOptions.length > 0) {
        this.importModal.config = this.configOptions[0].value; // 默认选择第一个配置（默认值develop）
      } else {
        this.importModal.config = null;
      }

      console.log("配置选项", this.configMap, this.configOptions);
    },
    // 导入配置文件，根据配置文件进行新增
    async importConfig() {
      this.importVisible = true;
      this.importLoading = true;
      setModalAriaHidden(this, document);
      await this.getDepartmentOptions();
      this.importLoading = false;
    },
    // 确认导入
    importOK() {
      this.$refs.otherForm.validate().then(() => {
        console.log("importModal", this.configMap.get(this.importModal.config));
        this.$emit("configList", this.configMap.get(this.importModal.config));
        this.importClose();
      });
    },
    // 关闭导入模态框
    importClose() {
      this.importModal = {
        department: null,
        config: null,
      };
      this.departmentOptions = [];
      this.configOptions = [];
      this.configMap = new Map();

      this.importVisible = false;
    },
  },
};
</script>