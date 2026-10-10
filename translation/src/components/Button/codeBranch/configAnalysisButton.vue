<template>
  <a-button type="primary" @click="openButton" :size="size">
    <template #icon>
      <PlusOutlined />
    </template>
    {{ buttonTitle }}
  </a-button>

  <CustomModal :visible="visible" :okLoading="loading" :modalTitle="buttonTitle" @handleClose="analysisClose" @handleOK="analysisOK">
    <div class="content">
      <a-form ref="otherForm" name="custom-validation" :model="modal">
        <a-form-item label="文件" name="fileList" :rules="[{required: true, message: '请选择文件!' }]">
          <a-upload name="file" :beforeUpload="beforeUpload" :customRequest="customRequest" :accept="modal.accept" :max-count="1"
            :fileList="modal.fileList" @change="handleChange" @remove="removeFile">
            <a-button type="primary" size="small">选择</a-button>
          </a-upload>

          <!-- 显示文件内容预览 -->
          <div v-if="modal.configList" style="margin-top: 10px; padding: 10px; background: #f0f2f5; border-radius: 4px;">
            <div style="font-weight: bold; margin-bottom: 5px;">文件内容预览：</div>
            <pre style="max-height: 200px; overflow-y: auto; margin: 0;">{{ JSON.stringify(modal.configList, null, 2) }}</pre>
          </div>
        </a-form-item>
      </a-form>
    </div>
  </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import { PlusOutlined } from "@ant-design/icons-vue";
import { setModalAriaHidden } from "@/utils/domUtils";

export default {
  components: {
    CustomModal,
    PlusOutlined,
  },
  emits: ["configList"],
  props: {
    size: {
      type: String,
      default: "small",
    },
    buttonTitle: {
      type: String,
      default: "配置新增",
    },
  },
  data() {
    return {
      visible: false,
      loading: false,
      modal: {
        accept: ".json",
        fileList: [], // 确保fileList是数组
        configList: null,
      },
    };
  },
  methods: {
    // 点击按钮打开模态框
    async openButton() {
      this.visible = true;
      this.loading = true;
      this.removeFile();
      setModalAriaHidden(this, document);
      this.loading = false;
    },

    // 阻止默认上传行为
    beforeUpload(file) {
      // 验证文件类型
      if (file.type !== "application/json" && !file.name.endsWith(".json")) {
        message.error("请选择JSON格式的文件");
        return Upload.LIST_IGNORE;
      }
      return false; // 阻止自动上传
    },

    // 自定义请求处理（实际是客户端读取）
    customRequest(options) {
      const file = options.file;
      this.readFileContent(file);
    },

    // 文件变化处理
    handleChange(info) {
      this.modal.fileList = info.fileList;

      // 只有一个文件时才读取
      if (info.fileList.length === 1) {
        this.readFileContent(
          info.fileList[0].originFileObj || info.fileList[0]
        );
      } else {
        this.modal.configList = null;
      }
    },

    // 读取文件内容
    readFileContent(file) {
      const reader = new FileReader();

      reader.onload = (e) => {
        try {
          const configData = JSON.parse(e.target.result);
          // 确保结果是数组
          if (Array.isArray(configData)) {
            // 如果已经是数组，直接使用
            this.modal.configList = configData;
          } else if (typeof configData === "object" && configData !== null) {
            // 如果是对象，转换为数组
            this.modal.configList = Object.values(configData);
          } else {
            // 其他情况，包装为数组
            this.modal.configList = [configData];
          }
          message.success("文件读取成功");
        } catch (error) {
          console.error("JSON解析错误:", error);
          message.error("JSON文件格式错误，请检查文件内容");
          this.modal.configList = null;
        }
      };

      reader.onerror = () => {
        message.error("文件读取失败");
        this.modal.configList = null;
      };

      // 读取文件为文本
      reader.readAsText(file, "utf-8");
    },

    // 移除文件
    removeFile(file) {
      this.modal.fileList = [];
      this.modal.configList = null;
      return true;
    },

    // 确认导入
    analysisOK() {
      this.$refs.otherForm.validate().then(() => {
        if (this.modal.configList) {
          console.log("确认导入", this.modal.configList);
          this.$emit("configList", this.modal.configList);
          this.analysisClose();
        } else {
          message.error("文件未解析成功，请重新选择");
        }
      });
    },

    // 关闭导入模态框
    analysisClose() {
      this.visible = false;
    },
  },
};
</script>