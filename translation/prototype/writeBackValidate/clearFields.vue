<template>
  <a-button v-if="mode === 'button' && !hideButton" type="primary" @click="showClearFieldsModal" :size="size">{{
    buttonTitle }}</a-button>

  <CustomModal :modalTitle="'列置空'" width="60%" :visible="clearFieldsVisible" :showCancel="false" :showOk="false"
    @handleClose="handleClose" :afterClose="afterClose">
    <div class="content">
      <a-form ref="clearFieldsForm" :model="clearFieldsModal">
        <a-form-item label="置空字段" name="field" :rules="[{ required: true, message: '请选择!' }]">
          <div style="display: flex; justify-content: space-between;">
            <a-select mode="multiple" v-model:value="clearFieldsModal.field" :options="fieldOptions"
              placeholder="请选择置空字段" allowClear style="flex: 1; margin-right: 8px;" />
            <a-button type="link" size="small" @click="selectAllFields" style="
              font-size: smaller;margin-top:0">全选</a-button>
          </div>
        </a-form-item>
      </a-form>
    </div>
    <template #leftBottomBtn>
      <a-button key="back" @click="handleClose">取消</a-button>
      <a-button type="primary" @click="handleOK" :loading="clearFieldsLoading">确定</a-button>
    </template>
  </CustomModal>
</template>

<script>
import { message, notification } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import { clearFieldsByCondition } from "@prototype/writeBackValidate/http/api";
import { entryParams } from "@/constants/commonParam.js";
import { setModalAriaHidden } from "@/utils/domUtils";

export default {
  components: {
    CustomModal,
  },
  emits: ["update:dataSource", "afterClose"],
  props: {
    dataSource: {
      type: Array,
      required: true,
    },
    fieldOptions_: {
      type: Array,
      required: true,
    },
    mode: {
      type: String,
      default: 'button', // 'button' | 'modal'
    },
    size: {
      type: String,
      default: "middle",
    },
    buttonTitle: {
      type: String,
      default: "列置空",
    },
    hideButton: {
      type: Boolean,
      default: false,
    }, // 是否隐藏按钮（仅显示模态框）
  },
  data() {
    return {
      clearFieldsVisible: false,
      clearFieldsLoading: false,
      clearFieldsModal: {
        field: [],
      },
      fieldOptions: [], // 初始值，会在mounted中从fieldOptions_ prop更新
    };
  },
  mounted() {
    this.$nextTick(() => {
      // 排除不能置空的列名（ID列）
      const filterColNames = [
        "英文翻译id",
        "俄文翻译id",
        "西文翻译id",
        "法文翻译id",
        "中文翻译id",
      ];
      // 使用传入的 fieldOptions_ prop，如果不存在则使用 entryParams.exportFields 作为后备
      const sourceOptions = this.fieldOptions_ && Array.isArray(this.fieldOptions_) && this.fieldOptions_.length > 0
        ? this.fieldOptions_
        : (entryParams && entryParams.exportFields ? entryParams.exportFields : []);
      this.fieldOptions = sourceOptions.filter(
        (item) => !filterColNames.includes(item.label)
      );
    });
  },
  methods: {
    // ==================== 模态框控制 ====================
    showClearFieldsModal() {
      this.clearFieldsVisible = true;
      setModalAriaHidden(this, document);
    },
    handleClose() {
      this.clearFieldsVisible = false;
    },
    afterClose() {
      this.clearFieldsModal = {
        field: [],
      };
      this.$emit("afterClose");
    },

    // ==================== 字段选择相关 ====================
    // 全选置空字段方法
    selectAllFields() {
      this.clearFieldsModal.field = this.fieldOptions.map((item) => item.label);
    },

    // ==================== 置空操作 ====================
    // 置空-确认
    async handleOK() {
      this.clearFieldsLoading = true;
      try {
        // 验证字段选择
        if (!this.clearFieldsModal.field || this.clearFieldsModal.field.length === 0) {
          message.error("请选择置空字段！");
          return;
        }

        await this.$refs.clearFieldsForm.validate();
        const { field } = this.clearFieldsModal;

        // 构建请求体（注意：不需要包含"id"，也不需要exportType和excelName）
        const data = {
          columnNames: field,
          entryInfoEntities: this.dataSource,
        };

        // 调用 mock API
        const res = await clearFieldsByCondition(data);
        console.log("列置空响应体", res)

        // 处理响应（mock api返回有效数据至res.data.list）
        if (res && res.data && res.data.list) {
          const updatedData = res.data.list;
          console.log('update:dataSource', updatedData);

          // 触发更新事件
          this.$emit('update:dataSource', updatedData);

          const fieldNames = field.map((item) => entryParams.exportFields.find((option) => option.value === item)?.label);

          // 显示成功通知
          notification.success({
            message: '置空成功',
            description: `已成功将 ${field.length} 个字段置空：${fieldNames.join('、')}`,
          });

          // 关闭模态框
          this.handleClose();
        } else {
          message.error('置空失败：响应数据格式错误');
        }
      } catch (error) {
        console.error('置空失败：', error);
        message.error('置空失败：' + (error.message || '未知错误'));
      } finally {
        this.clearFieldsLoading = false;
      }
    },
  },
};
</script>
