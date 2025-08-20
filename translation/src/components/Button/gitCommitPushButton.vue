<template>
  <a-button type="primary" @click="showModal" :size="size" :class="buttonClass">{{ buttonTitle }}</a-button>
  <CustomModal :modalTitle="buttonTitle" width="500px" :visible="visible" :showCancel="false" :showOk="false" @handleClose="handleClose">
    <div class="content">
      <a-form ref="contentForm" :model="commitMsg">
        <a-form-item label="IP" name="ip" :rules="[{ required: true, message: '请选择IP!' }]">
          <a-select v-model:value="commitMsg.ip" :options="ipOptions" placeholder="请选择IP" allowClear></a-select>
        </a-form-item>
        <a-form-item label="分支" name="branch" :rules="[{ required: true, message: '请选择分支!' }]">
          <a-select v-model:value="commitMsg.branch" placeholder="请选择" :options="branchOptions" allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="版本名" name="versionName">
          <a-input v-model:value="commitMsg.versionName" placeholder="请输入版本名"></a-input>
        </a-form-item>
      </a-form>
    </div>
    <template #leftBottomBtn>
      <a-button key="back" @click="handleClose">取消</a-button>
      <a-button type="primary" @click="handleOK(false)" :loading="loading">确定</a-button>
    </template>
  </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import {
  getI18nAdress,
  getBranches,
  gitCommit,
  gitPush,
} from "@/http/api/workbench.js";
import commonParam, { workbenchParams } from "@/utils/commonParam.js";
import { setModalAriaHidden } from "@/utils/commonUtils.js";
export default {
  components: {
    CustomModal,
  },
  props: {
    size: {
      type: String,
      default: "small",
    },
    buttonClass: {
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
      ipOptions: [],
      branchOptions: null,
      commitMsg: {
        ip: null,
        branch: null,
        versionName: "",
      },
      visible: false,
      loading: false,
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
  watch: {
    // 监听导出类型变化，清空字段选择
    "commitMsg.ip"(newVal, oldval) {
      if (newVal != oldval) {
        this.commitMsg.branch = null;
        this.branchOptions = null;
        if (newVal) this.getBranches();
      }
    },
  },
  methods: {
    showModal() {
      this.visible = true;
      setModalAriaHidden(this, document);
      this.getIPs();
    },
    // 获取i18服务器ip
    getIPs() {
      this.ipOptions = [];
      getI18nAdress().then((res) => {
        console.log("获取i18n服务器ip", res);
        res.data.list.forEach((item) => {
          let ip = {
            label: item.ip,
            value: item.ip,
          };
          this.ipOptions.push(ip);
        });
      });
    },
    // 获取ip对应的分支
    getBranches() {
      this.branchOptions = [];
      getBranches(this.commitMsg.ip).then((res) => {
        res.data.list.forEach((item) => {
          // let branch = {
          //   label: item.branch,
          //   value: item.branch,
          // };
          let branch = {
            label: item.ip,
            value: item.ip,
          };
          this.branchOptions.push(branch);
        });
      });
    },

    // 导出-确认
    async handleOK() {
      this.loading = true;
      try {
        if (!this.commitMsg.ip) {
          message.error("请选择IP！");
          return;
        }
        if (!this.commitMsg.branch) {
          message.error("请选择分支！");
          return;
        }
        await this.$refs.contentForm.validate();
        let params = {
          ...this.commitMsg,
        };
        console.log("导出参数", params);
        gitCommit(params).then((res) => {
          message.success("commit提交成功");
          gitPush(params).then((res) => {
            message.success("push推送成功");
            this.visible = false;
          })
          .catch((error) => {
            message.error(`push推送失败: ${error.message || error}`);
          });
        }).catch((error) => {
          message.error(`commit提交失败: ${error.message || error}`);
        });
      } catch (error) {
        if (!(error.name === "AbortError")) {
          console.log("导出失败原因", error);
          message.error(`导出失败: ${error.message || error}`);
        }
      } finally {
        this.loading = false;
      }
    },
    // 关闭导出模态框
    handleClose() {
      this.visible = false;
    },
  },
};
</script>