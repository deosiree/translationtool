<template>
  <a-button type="primary" @click="showModal" :size="size" :class="buttonClass">{{ buttonTitle }}</a-button>
  <CustomModal :modalTitle="buttonTitle" modalWidth="500px" :modalVisible="visible" :showCancel="false" :showOk="false" @handleClose="handleClose">
    <div class="content" style="width:100%;height:100%">
      <!-- 添加加载动画 -->
      <a-spin :spinning="loading">
        <a-form ref="contentForm" :model="writeBack" autocomplete="off" :label-col="{ span: 4 }">
          <a-form-item label="IP" name="ip" :rules="[{ required: true, message: '请选择IP!' }]">
            <a-select v-model:value="writeBack.ip" :options="ipOptions" placeholder="请选择IP" allowClear></a-select>
          </a-form-item>
          <a-form-item label="回写语种" name="language" :rules="[{ required: true, message: '请选择回写语种!' }]">
            <!-- 修改为多选 -->
            <a-select mode="multiple" v-model:value="writeBack.language" :options="langOptions" placeholder="请选择" @change="languageChange" allowClear>
              <!-- <a-select mode="multiple" v-model:value="writeBack.language" placeholder="请选择" allowClear> -->
              <!-- <a-select-option value="英文">英文</a-select-option>
              <a-select-option value="俄文">俄文</a-select-option>
              <a-select-option value="西文">西文</a-select-option>
              <a-select-option value="法文">法文</a-select-option> -->
            </a-select>
          </a-form-item>
          <a-form-item label="回写类型" name="type">
            <a-radio-group v-model:value="writeBack.type" name="radioGroup" @change="writeBackTypeChange">
              <a-radio value="DEFAUT">默认 </a-radio>
              <a-radio value="TS">TS文件</a-radio>
              <a-radio value="DI">辞典</a-radio>
            </a-radio-group>
            <a-tooltip placement="top">
              <template #title>
                <span>默认：按词条来源回写；TS文件：写入到ts文件；辞典：写入到辞典</span>
              </template>
              <QuestionCircleOutlined style="color:#00000066;float:right;margin-top:3px" />
            </a-tooltip>
          </a-form-item>
          <a-form-item :label="writeBack.label" name="file" v-if="writeBack.type != 'DEFAUT'">
            <a-select show-search v-model:value="writeBack.file" :options="writeBack.fileOptions" placeholder="请选择" allowClear></a-select>
          </a-form-item>
          <a-form-item label=" " :colon="false">
            <a-checkbox v-model:checked="writeBack.isTag" :disabled="writeBack.tagDisabled">回写Tag</a-checkbox>
            <a-checkbox v-model:checked="writeBack.isComment" :disabled="writeBack.commentDisabled">回写来源</a-checkbox>
            <a-tooltip placement="top">
              <template #title>
                <span>词条默认复用，增加标识可以确保词条唯一性（不推荐）</span>
              </template>
              <QuestionCircleOutlined style="color:#00000066;float:right;margin-top:3px" />
            </a-tooltip>
          </a-form-item>
          <a-form-item label="Git分支" name="branch" :rules="[{ required: true, message: '请选择分支!' }]">
            <a-select v-model:value="writeBack.branch" placeholder="请选择" :options="branchOptions" allowClear>
            </a-select>
          </a-form-item>
          <a-form-item label="Git版本名" name="versionName">
            <a-input v-model:value="writeBack.versionName" placeholder="请输入版本名"></a-input>
          </a-form-item>
          <!-- <a-form-item label="回写Tag" name="isTag">
            <a-switch v-model:checked="writeBack.isTag" checked-children="是" un-checked-children="否" />
          </a-form-item>
          <a-form-item label="回写来源" name="isComment">
            <a-switch v-model:checked="writeBack.isComment" checked-children="是" un-checked-children="否" />
          </a-form-item> -->
        </a-form>
      </a-spin>
    </div>
    <template #leftBottomBtn>
      <a-button key="back" @click="handleClose">取消</a-button>
      <a-button type="primary" @click="handleOK" :loading="loading">确定</a-button>
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
import { writeBack } from "@/http/api/entryManage";
import { getDictionary, getFileListByLang } from "@/http/api/i18Server";
import commonParam, { workbenchParams } from "@/utils/commonParam.js";
import { setModalAriaHidden } from "@/utils/commonUtils.js";
import { QuestionCircleOutlined } from "@ant-design/icons-vue";
export default {
  components: {
    CustomModal,
    QuestionCircleOutlined,
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
      default: "回写",
    },
  },
  data() {
    // 从本地缓存读取用户偏好
    const cachedLanguages = localStorage.getItem("writeBackLanguages");
    return {
      langOptions: Object.values(commonParam.languageMap).map((lang) => ({
        label: lang.name,
        value: lang.name,
      })),
      writeBack: {
        language: cachedLanguages
          ? JSON.parse(cachedLanguages)
          : commonParam.langNameList, // 默认全选或从缓存读取["英文", "俄文", "西文", "法文"]
        type: "DEFAUT",
        label: "",
        file: null,
        isTag: null,
        isComment: null,
        fileOptions: [],
        commentDisabled: false,
        tagDisabled: false,
        ip: null,
        branch: null,
        versionName: "",
        userName: this.$store.state.user.userName,
      },
      ipOptions: [],
      branchOptions: null,
      visible: false,
      loading: false,
    };
  },
  mounted() {
    this.$nextTick(() => {
      // 获取当前用户信息
      this.user = this.$store.state.user;
      // console.log("当前用户信息", this.writeBack.userName);

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
    "writeBack.ip"(newVal, oldval) {
      if (newVal != oldval) {
        this.writeBack.branch = null;
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
      getBranches(this.writeBack.ip).then((res) => {
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
    // 回写语种change事件
    languageChange() {
      if (this.writeBack.type === "TS") {
        this.writeBack.fileOptions = [];
        // 遍历选中的语种，获取对应的 ts 文件列表
        this.writeBack.language.forEach((language) => {
          this.getTsFile(language);
        });
      }
      // 保存用户偏好到本地缓存
      localStorage.setItem(
        "writeBackLanguages",
        JSON.stringify(this.writeBack.language)
      );
    },
    // 回写类型切换事件
    writeBackTypeChange() {
      this.writeBack.file = null;
      this.writeBack.fileOptions = [];
      this.writeBack.isTag = false;
      this.writeBack.isComment = false;
      this.writeBack.commentDisabled = false;
      this.writeBack.tagDisabled = false;

      if (this.writeBack.type === "TS") {
        this.writeBack.label = "ts文件";
        this.writeBack.isTag = true;
        this.writeBack.isComment = false;
        this.writeBack.commentDisabled = true;
        this.writeBack.tagDisabled = true;
        if (
          this.writeBack.language === null ||
          this.writeBack.language === ""
        ) {
          message.warn("请选择回写语种！");
          return;
        }
        // 遍历选中的语种，获取对应的 ts 文件列表
        this.writeBack.language.forEach((language) => {
          this.getTsFile(language);
        });
      } else if (this.writeBack.type === "DI") {
        this.writeBack.label = "辞典";
        // 获取辞典文件列表
        this.getDictionary();
      }
      // 保存用户偏好到本地缓存
      localStorage.setItem(
        "writeBackLanguages",
        JSON.stringify(this.writeBack.language)
      );
    },
    // 获取ts文件
    getTsFile(language) {
      let params = {
        language: language,
        i18nUrl: this.writeBack.ip,
      };
      getFileListByLang(params).then((res) => {
        res.data.list.forEach((item) => {
          let option = {
            label: item,
            value: item,
          };
          this.writeBack.fileOptions.push(option);
          // console.log("fileOptions:", this.writeBack.fileOptions);
        });
      });
    },
    // 获取辞典
    getDictionary() {
      let params = {
        i18nUrl: this.writeBack.ip,
      };
      getDictionary(params).then((res) => {
        // getDictionary().then((res) => {// 之前这里没写完，待重构优化，选择默认时就是辞典的就进入辞典，ts文件的就进入ts文件，用不上ts文件/辞典的选项
        res.data.list.forEach((item) => {
          let option = {
            label: item,
            value: item,
          };
          this.writeBack.fileOptions.push(option);
        });
      });
    },

    // 确认
    async handleOK() {
      let successLanguages = [];
      let failedLanguages = [];
      let successmsg = "";
      let failedmsg = "";
      let commitSuccess = false;
      if (!this.writeBack.ip) {
        message.error("请选择IP！");
        return;
      }
      if (this.writeBack.language.length === 0) {
        message.error("请选择回写语种！");
        return;
      }
      if (this.writeBack.type != "DEFAUT" && this.writeBack.file === null) {
        // 回写类型
        message.info("请选择" + this.writeBack.label + "!");
        return;
      }
      if (!this.writeBack.branch) {
        message.error("请选择分支！");
        return;
      }
      await this.$refs.contentForm.validate();

      this.loading = true;
      // 遍历选中的语种列表，依次执行回写操作
      for (const language of this.writeBack.language) {
        let params = {
          translateType: language,
          isTag: this.writeBack.isTag ? 1 : 0,
          isComment: this.writeBack.isComment ? 1 : 0,
          writeType: this.writeBack.type,
          fileName: this.writeBack.file,
          i18nUrl: this.writeBack.ip,
        };
        try {
          await writeBack(params, this.dataSource);
          successLanguages.push(language);
        } catch (err) {
          failedLanguages.push(`${language}: ${err.message}`);
        }
      }
      if (successLanguages.length > 0) {
        successmsg += `以下语种回写成功：${successLanguages.join(", ")}。`;
        message.success(successmsg);
      }
      if (failedLanguages.length > 0) {
        failedmsg += `以下语种回写失败：${failedLanguages.join(
          ", "
        )},请手动git。`;
        message.error(failedmsg);
      }
      this.loading = false;

      // 回写完成，开始执行git推送
      let params = {
        ip: this.writeBack.ip,
        branch: this.writeBack.branch,
        versionName:
          this.writeBack.versionName == ""
            ? this.writeBack.userName
            : this.writeBack.userName + "-" + this.writeBack.versionName,
      };
      console.log("导出参数", params);
      this.loading = true;
      await gitCommit(params)
        .then((res) => {
          console.log("commit提交成功", wef);
          commitSuccess = true;
        })
        .catch((error) => {
          message.error(`commit提交失败: ${error.message || error}`);
        })
        .finally(() => {
          this.loading = false;
        });
      if (commitSuccess) {
        // this.loading = true;
        await gitPush(params)
          .then((res) => {
            // console.log("push推送成功",qwed);
            message.success("push推送成功");
          })
          .catch((error) => {
            message.error(`push推送失败: ${error.message || error}`);
          })
          .finally(() => {
            this.loading = false;
          });
      }
    },
    // 关闭导出模态框
    handleClose() {
      this.visible = false;
    },
    // 关闭弹窗后的操作
    afterClose() {
      this.writeBack = {
        language: this.writeBack.language,
        type: "DEFAUT",
        label: "",
        file: null,
        isTag: null,
        isComment: null,
        fileOptions: [],
        commentDisabled: false,
        tagDisabled: false,
      };
    },
  },
};
</script>