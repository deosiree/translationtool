<template>
  <a-button type="primary" @click="handleOK" :size="size" :style="style">{{ buttonTitle }}</a-button>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import commonParam from "@/constants/commonParam.js";
import { cloneDeep } from "lodash-es";
export default {
  components: {
    CustomModal,
  },
  emits: ["update:oldEditableData", "showEditOperation"],
  props: {
    translate: {
      type: String,
    },
    dataSource: {
      type: Array,
      required: true,
    },
    oldEditableData: {
      type: Object,
      required: true,
    },
    size: {
      type: String,
      default: "small",
    },
    style: {
      type: Object,
      default: () => ({}),
    },
    buttonTitle: {
      type: String,
      default: "导出",
    },
  },
  data() {
    return {};
  },
  mounted() {},
  methods: {
    // 导出-确认
    async handleOK() {
      try {
        let newDataSource = cloneDeep(this.dataSource);
        let newEditableData = cloneDeep(this.oldEditableData);
        // console.log("写入数据：", newDataSource);

        // 1.保存编辑框中的所有信息
        for (let key in newEditableData) {
          const index = this.dataSource.findIndex((item) => item.id === key);
          // newEditableData.splice(index, 1);
          // newEditableData.splice(index, 0, newEditableData[key]);
          newDataSource[index] = cloneDeep(newEditableData[key]);
        }
        // console.log(
        //   "保存编辑框后数据：",
        //   newEditableData,
        // );

        // 2.释义覆盖翻译（只修改翻译列，存到编辑框中）
        for (let item of newDataSource) {
          const transMap = commonParam.languageMap[this.translate];
          // console.log("lang", this.translate, transMap);
          if (item[transMap.interpretation] != null) {
            // item[transMap.value] = item[transMap.interpretation];// 存到newDataSource了
            if (!newEditableData[item.id]) {
              newEditableData[item.id] = item;
            }
            newEditableData[item.id][transMap.value] =
              item[transMap.interpretation];
          }
        }
        // console.log("释义覆盖后数据：", newEditableData);
        this.$emit("update:oldEditableData", newEditableData); // 存为编辑框，会自动进行校验
        this.$emit("showEditOperation"); // 打开编辑操作列
      } catch (error) {
        if (!(error.name === "AbortError")) {
          console.log("覆盖失败原因", error);
          message.error(error); //.errorFields[0].errors.join("，")
        }
      } finally {
        this.coverVisible = false;
      }
    },
    // 关闭导出模态框
    handleClose() {
      this.coverVisible = false;
    },
  },
};
</script>