<template>
  <div>
    <a-button type="primary" size="small" @click="showImportModal">导入</a-button>
    <CustomModal :visible="importVisible" :okLoading="importLoading" modalTitle="导入" @handleClose="importClose" @handleOK="importOK"
      @afterClose="importAfterClose">
      <div class="content">
        <a-form ref="formRef" name="custom-validation" :model="importModal">
          <a-form-item label="文件类型" name="importType" :rules="[{ required: true, message: '请选择!' }]">
            <a-select v-model:value="importModal.importType" placeholder="请选择文件类型" :options='importTypes' allowClear>
            </a-select>
          </a-form-item>
          <a-form-item label="语言" name="language" :rules="[{ required: true, message: '请选择!' }]">
            <a-select v-model:value="importModal.language" placeholder="请选择语言" :options='translateTypes' :fieldNames="{label:'name',value:'name'}"
              allowClear>
            </a-select>
          </a-form-item>
          <a-form-item label="文件" name="file" :rules="[{required: true, validator: checkFile }]">
            <a-upload name="file" :accept="accept" :max-count="1" :fileList="fileList" @change="handleChange"
              @remove="removeFile" :disabled="!importModal.language || !importModal.importType">
              <a-button type="primary" size="small" @click="getAccept">选择</a-button>
            </a-upload>
          </a-form-item>
        </a-form>
      </div>
    </CustomModal>
  </div>
</template>

<script>
import CustomModal from "@/components/modal/index.vue";
import { message } from "ant-design-vue";
import { defineComponent, ref } from "vue";
import { getLanguage } from "@/http/api/translate";

export default defineComponent({
  components: {
    CustomModal
  },
  emits: ['importSuccess'],
  setup(props, { emit }) {
    const importVisible = ref(false);
    const importLoading = ref(false);
    const importModal = ref({
      language: null,
      importType: null,
    });
    const importTypes = ref([
      { label: "csv", value: "csv", accept: ".csv" },
      { label: "excel", value: "excel", accept: ".xls,.xlsx" },
    ]);
    const translateTypes = ref([]);
    const accept = ref(null);
    const fileList = ref([]);
    const formRef = ref(null);

    // 获取翻译语言
    const getLanguageList = () => {
      let data = {};
      getLanguage(data).then((res) => {
        translateTypes.value = res.data.list;
      });
    };

    // 显示导入模态框
    const showImportModal = () => {
      importVisible.value = true;
      getLanguageList();
    };

    // 关闭导入模态框
    const importClose = () => {
      importVisible.value = false;
      importModal.value = {
        language: null,
        importType: null,
      };
      fileList.value = [];
      accept.value = null;
    };

    // 确认导入
    const importOK = () => {
      if (!formRef.value) return;
      formRef.value.validate().then(() => {
        importLoading.value = true;
        // 这里添加实际的导入逻辑
        // 导入成功后触发事件
        emit('importSuccess');
        message.success('导入成功');
        importClose();
        importLoading.value = false;
      }).catch((error) => {
        message.error('表单验证失败，请检查输入');
        importLoading.value = false;
      });
    };

    // 导入完成后回调
    const importAfterClose = () => {
      // 可以添加一些清理逻辑
    };

    // 获得导入文件类型
    const getAccept = () => {
      if (!importModal.value.importType) {
        message.error("请选择文件类型！");
        return;
      }
      if (!importModal.value.language) {
        message.error("请选择语言！");
        return;
      }
      for (let key in importTypes.value) {
        if (importModal.value.importType === importTypes.value[key].value) {
          accept.value = importTypes.value[key].accept;
          break;
        }
      }
    };

    // 文件变化处理
    const handleChange = (info) => {
      fileList.value = info.fileList;
    };

    // 移除文件
    const removeFile = () => {
      fileList.value = [];
    };

    // 检查文件
    const checkFile = (rule, value) => {
      if (fileList.value.length === 0) {
        return Promise.reject('请选择文件');
      }
      return Promise.resolve();
    };

    return {
      importVisible,
      importLoading,
      importModal,
      importTypes,
      translateTypes,
      accept,
      fileList,
      formRef,
      showImportModal,
      importClose,
      importOK,
      importAfterClose,
      getAccept,
      handleChange,
      removeFile,
      checkFile
    };
  }
});
</script>