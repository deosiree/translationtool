<template>
  <a-button type="primary" @click="showModal" :size="size" :class="buttonClass">{{ buttonTitle }}</a-button>
  <CustomModal :modalTitle="buttonTitle" width="500px" :modalVisible="visible" :showCancel="false" :showOk="false" @handleClose="handleClose">
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
      <a-button type="primary" @click="commitOK" :loading="loading">提交</a-button>
      <a-button type="primary" @click="pushOK" :loading="loading">推送</a-button>
      <a-button type="primary" @click="commitPushOK" :loading="loading">提交并推送</a-button>
    </template>
  </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import {
  getI18nAdress,
  getBranches,
} from "@/http/api/workbench.js";
import commonParam, { workbenchParams } from "@/constants/commonParam.js";
import { setModalAriaHidden } from "@/utils/domUtils";
import { doGitCommit, doGitPush, doCommitAndPush } from "@/utils/gitUtils";
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
      default: null,
    },
    buttonTitle: {
      type: String,
      default: "git推送",
    },
    treeTitle: {
      type: String,
      default: null,
    },
  },
  data() {
    return {
      ipOptions: [],
      branchOptions: null,
      commitMsg: {
        ip: null,
        branch: null,
        versionName: "update",
        userName: this.$store.state.user?.userName || '',
      },
      visible: false,
      loading: false,
    };
  },
  mounted() {
    this.$nextTick(() => {
      // 获取当前用户信息
      this.user = this.$store.state.user;
      // console.log("当前用户信息", this.commitMsg.userName);
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
      if (this.commitMsg.ip) {
        this.getBranches();
      }
    },
    // 获取i18服务器ip
    getIPs() {
      this.ipOptions = [];
      getI18nAdress().then((res) => {
        // console.log("获取i18n服务器ip", res);
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
      getBranches({ ip: this.commitMsg.ip }).then((res) => {
        // console.log("获取ip分支结果", res);
        res.data.list.forEach((item) => {
          let branch = {
            label: item,
            value: item,
          };
          this.branchOptions.push(branch);
        });
      });
    },

    // commit
    async commitOK() {
      if (!this.commitMsg.ip) {
        message.error("请选择IP！");
        return;
      }
      if (!this.commitMsg.branch) {
        message.error("请选择分支！");
        return;
      }
      console.log("节点名称", this.treeTitle);
      await this.$refs.contentForm.validate();
      let vsName = this.commitMsg.versionName + "(";
      if (this.treeTitle) {
        vsName += `Model:${this.treeTitle};`;
      }
      if (this.commitMsg.userName) {
        vsName += `User:${this.commitMsg.userName};`;
      }
      vsName += `IP:${this.commitMsg.ip})`;

      let params = {
        ip: this.commitMsg.ip,
        branch: this.commitMsg.branch,
        versionName: vsName,
      };
      // console.log("导出参数", params);
      this.loading = true;
      try {
        await doGitCommit(params);
      } finally {
        this.loading = false;
      }
    },
    // push
    async pushOK() {
      if (!this.commitMsg.ip) {
        message.error("请选择IP！");
        return;
      }
      if (!this.commitMsg.branch) {
        message.error("请选择分支！");
        return;
      }
      await this.$refs.contentForm.validate();
      this.loading = true;
      let params = {
        ip: this.commitMsg.ip,
      };
      try {
        await doGitPush(params);
      } finally {
        this.loading = false;
      }
    },
    // commit+push
    async commitPushOK() {
      if (!this.commitMsg.ip) {
        message.error("请选择IP！");
        return;
      }
      if (!this.commitMsg.branch) {
        message.error("请选择分支！");
        return;
      }
      await this.$refs.contentForm.validate();

      this.loading = true;

      try {
        let vsName = this.commitMsg.versionName + " (";
        if (this.treeTitle) {
          vsName += `Model:${this.treeTitle};`;
        }
        if (this.commitMsg.userName) {
          vsName += `User: ${this.commitMsg.userName}; `;
        }
        vsName += `IP: ${this.commitMsg.ip})`;

        const commitParams = {
          ip: this.commitMsg.ip,
          branch: this.commitMsg.branch,
          versionName: vsName,
        };
        const pushParams = {
          ip: this.commitMsg.ip,
        };
        await doCommitAndPush(commitParams, pushParams);
      } catch (error) {
        console.log("操作失败", error);
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