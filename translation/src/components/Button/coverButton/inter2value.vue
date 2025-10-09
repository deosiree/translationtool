<template>
  <a-button type="primary" @click="showCoverModal" :size="size" :style="style">{{ buttonTitle }}</a-button>

  <CustomModal :modalTitle="buttonTitle" width="500px" :visible="coverVisible" :showCancel="false" :showOk="false" @handleClose="handleClose">
    <div class="content">
      <a-form ref="coverForm" :model="coverModal">

        <!-- 将name改为与coverModal中的属性名匹配 -->
        <a-form-item label="覆盖语种" name="langs" :rules="[{ required: true, message: '请选择覆盖语种！' }]">
          <div style="display: flex; justify-content: space-between;">
            <a-select mode="multiple" v-model:value="coverModal.langs" :options='langsOptions' placeholder="请选择覆盖语种" allowClear
              style="flex: 1; margin-right: 8px;" />
            <a-button type="link" size="small" @click="selectAllLangs" style="
              font-size: smaller;margin-top:0">全选</a-button>
          </div>
        </a-form-item>
        <a-form-item label="校验规则" name="rules">
          <div style="display: flex; justify-content: space-between;">
            <a-select mode="multiple" v-model:value="coverModal.rules" :options="rulesOptions" placeholder="请选择校验规则" allowClear
              style="flex: 1; margin-right: 8px;" />
            <a-button type="link" size="small" @click="selectAllRules" style="
              font-size: smaller;margin-top:0">全选</a-button>
          </div>
        </a-form-item>
        <div style="flex: 1; margin-right: 8px;color: #999;">只校验翻译语种，其他语种有则填入，不校验</div>
      </a-form>
    </div>
    <template #leftBottomBtn>
      <a-button key="back" @click="handleClose">取消</a-button>
      <a-button type="primary" @click="handleOK(false)" :loading="coverLoading">确定</a-button>
    </template>
  </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import commonParam from "@/utils/commonParam.js";
import { setModalAriaHidden } from "@/utils/commonUtils.js";
import { cloneDeep } from "lodash-es";
export default {
  components: {
    CustomModal,
  },
  emits: ["update:oldEditableData"],
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
    return {
      coverVisible: false,
      coverLoading: false,
      coverModal: {
        langs: [], // 要覆盖的语种
        rules: [], // 要校验的规则
      },
      langsOptions: commonParam.languageList.map((item) => ({
        label: item.name,
        value: item.name,
      })),
      rulesOptions: commonParam.rulesOptions.map((item) => ({
        label: item.label,
        value: item.key,
      })), // 要校验的规则
      user: null, // 当前用户的相关信息
      currentDepartment: {
        label: "部门名称",
        importTypes: [],
        value: "name",
        xml_temp: false,
        ops: new Set(),
      }, // 当前用户所在部门的相关信息
      editableData: {},
    };
  },
  mounted() {
    this.$nextTick(() => {
      // 获取当前用户信息
      this.user = this.$store.state.user;
      // 获取当前用户所在部门的相关信息
      if (
        Object.keys(commonParam.departmentMap).includes(this.user.department)
      ) {
        this.currentDepartment =
          commonParam.departmentMap[this.user.department];
      } else {
        this.currentDepartment = commonParam.departmentMap["default"];
      }
    });
  },
  methods: {
    async showCoverModal() {
      this.coverVisible = true;
      setModalAriaHidden(this, document);
      // console.log("获取用户偏好前：导出字段", this.coverModal);
      // 1.读取本地存储的用户偏好
      const langs_json = localStorage.getItem("cover_inter2value_langs");
      if (langs_json) {
        this.coverModal.langs = JSON.parse(langs_json);
      }
      const rules_json = localStorage.getItem("workbench_rules");
      if (rules_json) {
        this.coverModal.rules = JSON.parse(rules_json);
      }
      // // 获取用户偏好（后端写死了固定几列，所以我自定义的存不进去）
      // queryUserPartiality().then((res) => {
      //   if (res.data.list && res.data.list.length > 0) {
      //     console.log("获取用户偏好！！！", res.data.list);
      //     const coverLangs = res.data.list[0].coverLangs;
      //     if (coverLangs != null && coverLangs != "") {
      //       this.coverModal.langs = coverLangs.split(",");
      //     }
      //     const coverRules = res.data.list[0].coverRules;
      //     if (coverRules != null && coverRules != "") {
      //       this.coverModal.rules = coverRules.split(",");
      //     }
      //   }
      // });
      // console.log("获取用户偏好后：导出字段", this.coverModal);
    },
    // 全选覆盖语言方法
    selectAllLangs() {
      this.coverModal.langs = this.langsOptions.map((item) => item.value);
    },
    // 全选校验规则方法
    selectAllRules() {
      this.coverModal.rules = this.rulesOptions.map((item) => item.value);
    },
    // 导出-确认
    async handleOK() {
      try {
        // 1.表单校验（必须选择覆盖语言）
        await this.$refs.coverForm.validate();

        let newDataSource = cloneDeep(this.dataSource);
        // console.log("写入数据：", newDataSource);

        // 2.保存编辑框中的所有信息
        this.editableData = cloneDeep(this.oldEditableData);
        for (let key in this.editableData) {
          const index = this.dataSource.findIndex((item) => item.id === key);
          // this.editableData.splice(index, 1);
          // this.editableData.splice(index, 0, this.editableData[key]);
          newDataSource[index] = cloneDeep(this.editableData[key]);
        }
        // console.log(
        //   "保存编辑框后数据：",
        //   this.editableData,
        //   this.oldEditableData,
        //   this.dataSource
        // );

        // 3.释义覆盖翻译（存到编辑框中）
        for (let item of newDataSource) {
          for (let lang of this.coverModal.langs) {
            const transMap = commonParam.languageMap[lang];
            if (item[transMap.interpretation] != null) {
              // item[transMap.value] = item[transMap.interpretation];// 存到newDataSource了
              if (!this.editableData[item.id]) {
                this.editableData[item.id] = item;
              }
              this.editableData[item.id][transMap.value] =
                item[transMap.interpretation];
            }
          }
        }
        // console.log("释义覆盖后数据：", this.editableData);
        this.$emit("update:oldEditableData", this.editableData); // 存为编辑框，会自动进行校验

        // 记录偏好
        // this.coverFieldChange();//（后端写死了固定几列，所以我自定义的存不进去）
        localStorage.setItem(
          "cover_inter2value_langs",
          JSON.stringify(this.coverModal.langs)
        ); // localStorage存储用户偏好
        localStorage.setItem(
          "workbench_rules",
          JSON.stringify(this.coverModal.rules)
        ); // localStorage存储用户偏好

        // const json = {
        //   langs: this.coverModal.langs.join(","),
        //   rules: this.coverModal.rules.join(","),
        // };
        // localStorage.setItem("cover_inter2value", JSON.stringify(json)); // localStorage存储用户偏好
      } catch (error) {
        if (!(error.name === "AbortError")) {
          console.log("覆盖失败原因", error);
          message.error(error); //.errorFields[0].errors.join("，")
        }
      } finally {
        this.coverVisible = false;
      }
    },
    // // 记录用户偏好（后端写死了固定几列，所以我自定义的存不进去）
    // coverFieldChange() {
    //   let data = {
    //     coverLangs: this.coverModal.langs.join(","),
    //     coverRules: this.coverModal.rules.join(","),
    //   };
    //   updateUserPartiality(data).then((res) => {
    //     console.log("已记录用户偏好", res);
    //   });
    // },
    // 关闭导出模态框
    handleClose() {
      this.coverVisible = false;
    },
  },
};
</script>