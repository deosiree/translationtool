<template>
  <a-button type="primary" @click="showCoverModal" :size="size">{{ buttonTitle }}</a-button>

  <CustomModal :modalTitle="buttonTitle" width="500px" :visible="coverVisible" :showCancel="false" :showOk="false" @handleClose="handleClose">
    <div class="content">
      <a-form ref="coverForm" :model="coverModal">

        <!-- 将name改为与coverModal中的属性名匹配 -->
        <a-form-item label="覆盖语种" name="langs" :rules="[{ required: true, message: '请选择!' }]">
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
import { create } from "xmlbuilder2";
// import builder from "xmlbuilder"; // 注意：这是 xmlbuilder 的常见导入方式
import { ref } from "vue";
import CustomModal from "@/components/modal/index.vue";
import {
  queryUserPartiality,
  updateUserPartiality,
} from "@/http/api/userPartiality";
import commonParam, { entryParams } from "@/utils/commonParam.js";
import {
  setModalAriaHidden,
  getCurrentStringTime,
  verifyArray_workbench,
} from "@/utils/commonUtils.js";
import { cloneDeep } from "lodash-es";
export default {
  components: {
    CustomModal,
  },
  emits: ["update:dataSource", "update:editableData"],
  props: {
    translate: {
      type: String,
    },
    dataSource: {
      type: Array,
      required: true,
    },
    editableData: {
      type: Object,
      required: true,
    },
    size: {
      type: String,
      default: "small",
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
        value: item.value,
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
      let arr = {
        acceptIds: new Set(), // 所有校验通过
        errorIds: new Set(), // 所有校验不通过
        toLongIds: new Set(), // 校验长度
        specialIds: new Set(), // 校验特殊字符
      };
      let newDataSource = cloneDeep(this.dataSource);
      let newEditableData = {};
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

      console.log("写入数据：", newDataSource);

      // 1.保存编辑框中的所有信息
      for (let key in this.editableData) {
        let entry = newDataSource.find((item) => item.id === key);
        entry = cloneDeep(this.editableData[key]);

        if (entry[currentLang] != null) {
          // 翻译存在  则状态为待审核状态
          entry[this.task.transMap.state] = "1";
        }
      }
      // 2.校验翻译列
      arr = await verifyArray_workbench(
        this,
        newDataSource,
        this.translate,
        this.coverModal.rules
      );
      console.log("arr", arr);
      let arrCount = {
        updateArr: [],
        insertArr: [],
        toLongNum: arr.toLongIds.size,
        specialNum: arr.specialIds.size,
        errorNum: arr.errorIds.size,
        addNum: 0,
        addChildNum: 0,
        updateNum: 0,
        updateChildNum: 0,
      };
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
        if (!this.coverModal.langs || this.coverModal.langs.length == 0) {
          message.error("请选择指定覆盖语言！");
          return;
        }

        await this.$refs.coverForm.validate(); // 表单校验

        this.$emit("operateClose");
        this.coverVisible = false;
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
          message.error(`覆盖失败: ${error.message || error}`);
        }
      } finally {
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