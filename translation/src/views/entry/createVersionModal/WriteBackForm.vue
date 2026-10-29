<template>
  <!-- 回写：词条管理-已选词条-回写的二级表单弹窗 -->
  <CustomModal modalTitle="回写" modalWidth="500px" :modalVisible="visible" @handleClose="handleClose"
    @handleOK="handleOK" @afterClose="afterClose">
    <div style="width:100%;height:100%">
      <p style="margin: 0 0 12px; color: #666">将所选词条译文写入指定 IP 服务器，不会改变任务状态。</p>
      <a-spin :spinning="writeBackLoading">
        <a-form :model="writeBack" autocomplete="off" ref="writeBackForm" :label-col="{ span: 4 }">
          <a-form-item label="IP" name="ip" :rules="[{ required: true, message: '请选择IP!' }]">
            <a-select v-model:value="writeBack.ip" :options="ipOptions" placeholder="请选择IP" allowClear></a-select>
          </a-form-item>
          <a-form-item label="回写语种" name="language" :rules="[{ required: true, message: '请选择回写语种!' }]">
            <a-select mode="multiple" v-model:value="writeBack.language" :options="langOptions" placeholder="请选择"
              @change="languageChange" allowClear>
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
            <a-select show-search v-model:value="writeBack.file" :options="writeBack.fileOptions" placeholder="请选择"
              allowClear></a-select>
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
        </a-form>
      </a-spin>
    </div>
  </CustomModal>
</template>
<script>
import CustomModal from "@/components/modal/index.vue";
import { QuestionCircleOutlined } from "@ant-design/icons-vue";
import { message, notification } from "ant-design-vue";
import { writeBack } from "@/http/api/entryManage";
import { getI18nAdress } from "@/http/api/workbench.js";
import { getFileListByLang, getDictionary } from "@/http/api/i18Server";
import commonParam from "@/constants/commonParam.js";
import { setModalAriaHidden } from "@/utils/domUtils";
export default {
  components: { CustomModal, QuestionCircleOutlined },
  emits: ["handleClose"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    // 已选词条
    dataSource: {
      type: Array,
      default: () => [],
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
          : commonParam.langNameList, // 默认全选或从缓存读取
        type: "DEFAUT",
        label: "",
        file: null,
        isTag: null,
        isComment: null,
        fileOptions: [],
        commentDisabled: false,
        tagDisabled: false,
        ip: null,
      },
      writeBackLoading: false,
      ipOptions: [],
    };
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        setModalAriaHidden(this, document);
        this.getIPs();
      }
    },
  },
  methods: {
    handleClose() {
      this.$emit("handleClose");
    },
    /**
     * 校验回写表单并执行已选词条的回写操作。
     * @param {void} none - 本方法不接收参数。
     * @returns {Promise<void>} 校验及回写流程结束后完成。
     */
    async handleOK() {
      if (this.writeBack.type != "DEFAUT" && this.writeBack.file === null) {
        message.info("请选择" + this.writeBack.label + "!");
        return;
      }

      try {
        await this.$refs.writeBackForm.validate();
      } catch {
        return;
      }

      let notifyTask = null;
      try {
        this.writeBackLoading = true;
        let successLanguages = [];
        let partialLanguages = [];
        let failedLanguages = [];
        const promises = [];

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
          promises.push(writeBack(params, this.dataSource));
        }

        const results = await Promise.allSettled(promises);
        results.forEach((item, index) => {
          const language = this.writeBack.language[index];
          if (item.status === "rejected") {
            failedLanguages.push(language);
            return;
          }

          const responseCode = item.value?.code;
          if (responseCode === 200) {
            successLanguages.push(language);
          } else if (responseCode === 203) {
            partialLanguages.push(language);
          } else {
            failedLanguages.push(language);
          }
        });

        const summary = [
          { label: "成功", languages: successLanguages },
          { label: "部分成功", languages: partialLanguages },
          { label: "失败", languages: failedLanguages },
        ]
          .filter(({ languages }) => languages.length > 0)
          .map(
            ({ label, languages }) =>
              `${label} ${languages.length} 个：${languages.join(", ")}`
          )
          .join("；");
        const notificationOptions = {
          message: "回写结果",
          description: summary,
          duration: 0,
        };

        if (failedLanguages.length === this.writeBack.language.length) {
          notifyTask = () => notification.error(notificationOptions);
        } else if (partialLanguages.length > 0 || failedLanguages.length > 0) {
          notifyTask = () => notification.warning(notificationOptions);
        } else {
          notifyTask = () => notification.success(notificationOptions);
        }
        this.writeBackLoading = false;
      } catch (err) {
        console.log("回写失败:", err);
      } finally {
        this.$emit("handleClose");
        if (notifyTask) {
          this.$nextTick(notifyTask);
        }
      }
    },
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
        });
      });
    },
    // 获取辞典
    getDictionary() {
      let params = {
        i18nUrl: this.writeBack.ip,
      };
      getDictionary(params).then((res) => {
        res.data.list.forEach((item) => {
          let option = {
            label: item,
            value: item,
          };
          this.writeBack.fileOptions.push(option);
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
    // 获取i18服务器ip
    getIPs() {
      this.ipOptions = [];
      getI18nAdress().then((res) => {
        res.data.list.forEach((item) => {
          let ip = {
            label: item.ip,
            value: item.ip,
          };
          this.ipOptions.push(ip);
        });
      });
    },
  },
};
</script>
