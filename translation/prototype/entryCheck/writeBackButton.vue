<template>
  <!-- 按钮模式：显示按钮 -->
  <a-button v-if="mode === 'button'" type="primary" @click="showModal" :size="size" :class="buttonClass">{{ buttonTitle
  }}</a-button>

  <!-- 模态框：按钮模式和模态框模式共用 -->
<CustomModal :modalTitle="buttonTitle" modalWidth="500px"
  :modalVisible="mode === 'button' ? buttonVisible : (visible || internalVisible)" :showCancel="false" :showOk="false"
  @handleClose="handleClose">
    <div class="content" style="width:100%;height:100%">
      <!-- 添加加载动画 -->
      <a-spin :spinning="loading">
        <a-form ref="contentForm" :model="writeBack" autocomplete="off" :label-col="{ span: 4 }">
          <a-form-item label="IP" name="ip" :rules="[{ required: true, message: '请选择IP!' }]">
            <a-select v-model:value="writeBack.ip" :options="ipOptions" placeholder="请选择IP" allowClear></a-select>
          </a-form-item>
          <a-form-item label="回写语种" name="language" :rules="[{ required: true, message: '请选择回写语种!' }]">
            <!-- 修改为多选 -->
            <a-select mode="multiple" v-model:value="writeBack.language" :options="langOptions" placeholder="请选择"
              @change="languageChange" allowClear>
              <!-- <a-select mode="multiple" v-model:value="writeBack.language" placeholder="请选择" allowClear> -->
              <!-- <a-select-option value="英文">英文</a-select-option>
              <a-select-option value="俄文">俄文</a-select-option>
              <a-select-option value="西文">西文</a-select-option>
              <a-select-option value="法文">法文</a-select-option> -->
            </a-select>
          </a-form-item>
          <a-form-item label="回写类型" name="type">
            <a-radio-group v-model:value="writeBack.type" name="radioGroup" @change="writeBackTypeChange">
              <a-radio v-for="opt in writeBackTypeOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </a-radio>
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
            <div style="display: flex; flex-direction: column; gap: 8px;">
              <div style="display: flex; gap: 24px;">
                <a-checkbox v-if="enableValidation" v-model:checked="writeBack.needValidation">回写前校验</a-checkbox>
                <a-checkbox v-model:checked="writeBack.needPush">回写后推送</a-checkbox>
              </div>
              <div style="display: flex; gap: 24px;">
                <a-checkbox v-model:checked="writeBack.isTag" :disabled="writeBack.tagDisabled">回写Tag</a-checkbox>
                <a-checkbox v-model:checked="writeBack.isComment" :disabled="writeBack.commentDisabled">回写来源</a-checkbox>
              </div>
            </div>
            <a-tooltip placement="top">
              <template #title>
                <span>词条默认复用，增加标识可以确保词条唯一性（不推荐）</span>
              </template>
              <QuestionCircleOutlined style="color:#00000066;float:right;margin-top:3px" />
            </a-tooltip>
          </a-form-item>
          <a-form-item v-if="writeBack.needPush" label="Git分支" name="branch"
            :rules="writeBack.needPush ? [{ required: true, message: '请选择分支!' }] : []">
            <a-select v-model:value="writeBack.branch" placeholder="请选择" :options="branchOptions" allowClear />
          </a-form-item>
          <a-form-item v-if="writeBack.needPush" label="Git版本名" name="versionName">
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

<!-- 回写前校验弹窗：由 writeBackButton 内部直接拉起 -->
  <WriteBackValidateModal mode="modal" :visible="validationVisible" :validationData="validationTableData"
    :deleteMode="'soft'" :writeBackParams="writeBack" :isPrototype="isPrototype" :dataSource="dataSource"
    @close="validationVisible = false" @writeBack="handleValidationWriteBack"
    @update:dataSource="$emit('update:dataSource', $event)" />

</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import WriteBackValidateModal from "./WriteBackValidateModal.vue";
import {
  getI18nAdress,
  getBranches,
} from "@/http/api/workbench.js";
import { writeBack } from "@/http/api/entryManage";
import { getDictionary, getFileListByLang } from "@/http/api/i18Server";
import commonParam, { workbenchParams } from "@/constants/commonParam.js";
import { setModalAriaHidden } from "@/utils/domUtils";
import { QuestionCircleOutlined } from "@ant-design/icons-vue";
import { doCommitAndPush } from "@/utils/gitUtils";
import { buildMockValidationResult } from "./mockUtils";

// ==================== Mock 函数（原型模式） ====================
// 这些函数仅在原型模式下使用，用于模拟 API 调用
function mockWriteBackApi(params) {
  console.log('【mockWriteBackApi】调用参数：', params)
  return new Promise((resolve) => {
    setTimeout(() => {
      console.log('【mockWriteBackApi】返回：success', {
        writeType: params.writeType,
        translateType: params.translateType, // 回写语种
        selectedCount: params.selectedKeys?.length || 0,
        rowCount: params.rows?.length || 0,
      })
      resolve({ success: true })
    }, 500)
  })
}

function mockGitCommit() {
  return new Promise((resolve) => {
    setTimeout(() => {
      message.success('commit完成（原型）!')
      resolve({ success: true })
    }, 400)
  })
}

function mockGitPush() {
  return new Promise((resolve) => {
    setTimeout(() => {
      message.success('push完成（原型）!')
      resolve({ success: true })
    }, 400)
  })
}

export default {
  components: {
    CustomModal,
    QuestionCircleOutlined,
    WriteBackValidateModal,
  },
  emits: ["submit", "update:dataSource"],
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
    // 业务数据源：回写接口需要
    dataSource: {
      type: Array,
      default: () => [],
    },
    /**
     * submitMode:
     * - writeBack: 组件内部直接执行回写 + git commit/push（默认，保持原行为）
     * - emit: 仅校验表单并向外抛出参数，由外部决定"是否校验/是否回写/如何回写"
     */
    submitMode: {
      type: String,
      default: "writeBack",
    },
    /**
     * mode:
     * - button: 显示按钮，点击打开模态框（默认）
     * - modal: 直接显示模态框，不显示按钮
     */
    mode: {
      type: String,
      default: "button",
      validator: (v) => ['button', 'modal'].includes(v)
    },
    /**
     * visible: 仅在 mode="modal" 时生效，控制模态框显示/隐藏
     */
    visible: {
      type: Boolean,
      default: false
    },
    /**
     * enableValidation: 是否启用"回写前校验"选项
     */
    enableValidation: {
      type: Boolean,
      default: false
    },
    /**
     * isPrototype: 是否为原型模式（使用 mock API）
     */
    isPrototype: {
      type: Boolean,
      default: false
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
        versionName: "writeBack",
        userName: this.$store.state.user?.userName || '',
        needPush: false,
        needValidation: false, // 回写前校验
      },
      // 模态框模式下的内部 visible 状态
      internalVisible: false,
      ipOptions: [],
      branchOptions: null,
      writeBackTypeOptions: commonParam.writeBackTypeList || [
        { label: "默认", value: "DEFAUT" },
        { label: "TS文件", value: "TS" },
        { label: "辞典", value: "DI" },
      ],
      buttonVisible: false, // 按钮模式下的内部 visible 状态
      loading: false,

      // ==================== 回写前校验（由组件内部拉起） ====================
      validationVisible: false,
      validationTableData: [],
    };
  },
  mounted() {
    this.$nextTick(() => {
      // 获取当前用户信息
      this.user = this.$store.state.user;
      // console.log("当前用户信息", this.writeBack.userName);
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

    // dataA：监听父组件传入的已选词条变化（用于排查删除/去重后的数据流）
    dataSource: {
      deep: true,
      immediate: true,
      handler(newVal, oldVal) {
        const prev = Array.isArray(oldVal) ? oldVal : []
        const next = Array.isArray(newVal) ? newVal : []
        // eslint-disable-next-line no-console
        console.log('【WriteBackButton】dataA(dataSource) 变化：', {
          prevLength: prev.length,
          nextLength: next.length,
          prevIds: prev.map((r) => (r && r.id) || null),
          nextIds: next.map((r) => (r && r.id) || null),
          prev: prev,
          next: next,
        })
      },
    },
  },
  methods: {
    // ==================== 生命周期和初始化相关 ====================
    showModal() {
      if (this.mode === 'button') {
        this.buttonVisible = true;
      } else {
        this.internalVisible = true;
      }
      setModalAriaHidden(this, document);
      this.getIPs();
      if (this.writeBack.ip) {
        this.getBranches();
      }
    },

    // ==================== 基础数据加载（IP/分支/文件列表） ====================
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
      // 与 gitCommitButton.vue 保持一致：接口入参为对象 { ip }
      getBranches({ ip: this.writeBack.ip }).then((res) => {
        res.data.list.forEach((item) => {
          let branch = {
            label: item,
            value: item,
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

    // ==================== 表单校验与参数构建 ====================
    async validateWriteBackForm() {
      if (!this.writeBack.ip) {
        message.error("请选择IP！");
        return false;
      }
      if (!this.writeBack.language || this.writeBack.language.length === 0) {
        message.error("请选择回写语种！");
        return false;
      }
      if (this.writeBack.type != "DEFAUT" && this.writeBack.file === null) {
        message.info("请选择" + this.writeBack.label + "!");
        return false;
      }
      if (this.writeBack.needPush && !this.writeBack.branch) {
        message.error("请选择分支！");
        return false;
      }
      await this.$refs.contentForm.validate();
      return true;
    },
    buildWriteBackParamsList() {
      const paramsList = [];
      for (const language of this.writeBack.language || []) {
        paramsList.push({
          translateType: language,
          isTag: this.writeBack.isTag ? 1 : 0,
          isComment: this.writeBack.isComment ? 1 : 0,
          writeType: this.writeBack.type,
          fileName: this.writeBack.file,
          i18nUrl: this.writeBack.ip,
        });
      }
      return paramsList;
    },

    // ==================== 执行回写逻辑 ====================
    async executeWriteBack() {
      let successLanguages = [];
      let failedLanguages = [];
      let successmsg = "";
      let failedmsg = "";

      // dataA：严格以父组件传入的已选词条为准
      const dataA = Array.isArray(this.dataSource) ? this.dataSource : []
      // eslint-disable-next-line no-console
      console.log('【WriteBackButton】准备回写，当前 dataA：', {
        length: dataA.length,
        sampleIds: dataA.slice(0, 8).map((r) => (r && r.id) || null),
      })
      if (dataA.length === 0) {
        message.warning("当前无可回写的数据")
        return
      }
      const selectedKeys = dataA.map((r) => r && r.id).filter(Boolean)
      const rows = dataA

      this.loading = true;
      const promises = [];
      const paramsList = this.buildWriteBackParamsList();

      if (this.isPrototype) {
        // 原型模式：使用 mock API
        paramsList.forEach((params) => {
          promises.push(mockWriteBackApi({
            writeType: params.type || 'DEFAUT',
            translateType: params.translateType,
            selectedKeys,
            rows,
          }));
        });
      } else {
        // 正常模式：使用真实 API
        paramsList.forEach((params) => {
          promises.push(writeBack(params, rows));
        });
      }

      await Promise.allSettled(promises).then((rls) => {
        console.log("rls", rls);
        rls.forEach((item, index) => {
          if (item.status === "rejected") {
            failedLanguages.push(
              `${this.writeBack.language[index]}: ${item.reason?.message || item.reason || "请求失败"}`
            );
          } else {
            if (item.value?.data != "") {
              failedLanguages.push(
                `${this.writeBack.language[index]}: ${item.value?.data}`
              );
            } else {
              successLanguages.push(this.writeBack.language[index]);
            }
          }
        });
      });

      // ==================== 原型模式：强制制造“成功 / 失败”分布 ====================
      // 便于验证：
      // - 80% 概率：所有语种都回写成功（方便频繁走 git mock 流程）
      // - 20% 概率：部分语种失败、部分语种成功
      if (this.isPrototype) {
        const langs = Array.isArray(this.writeBack.language) ? this.writeBack.language : [];
        if (langs.length > 0) {
          const rnd = Math.random();
          if (rnd < 0.8) {
            // 80% 场景：全部成功
            successLanguages = [...langs];
            failedLanguages = [];
          } else {
            // 20% 场景：随机一部分失败，其余成功
            const shuffled = [...langs].sort(() => Math.random() - 0.5);
            // 至少 1 个失败；如果语言很多，则大约一半失败
            const failCount = Math.max(1, Math.floor(shuffled.length / 2));
            const failSet = new Set(shuffled.slice(0, failCount));
            failedLanguages = shuffled
              .slice(0, failCount)
              .map((lang) => `${lang}: 回写失败（原型模拟）`);
            successLanguages = shuffled.slice(failCount);
          }
        }
      }

      if (successLanguages.length > 0) {
        successmsg += `以下语种回写成功：${successLanguages.join(", ")}。`;
        message.success(successmsg);
      }
      if (failedLanguages.length > 0) {
        failedmsg += `以下语种回写失败：${failedLanguages.join(", ")}。`;
        message.error(failedmsg);
      }
      this.loading = false;

      // 勾选"回写后推送"：回写完成 -> git commit -> git push，全部结束后再关闭弹窗
      if (this.writeBack.needPush) {
        const versionName =
          this.writeBack.versionName == ""
            ? this.writeBack.userName
            : this.writeBack.userName + "-" + this.writeBack.versionName;
        const commitParams = {
          ip: this.writeBack.ip,
          branch: this.writeBack.branch,
          versionName,
        };
        const pushParams = {
          ip: this.writeBack.ip,
        };
        this.loading = true;
        try {
          if (this.isPrototype) {
            // 原型模式：使用 mock API
            await mockGitCommit();
            await mockGitPush();
          } else {
            // 正常模式：使用真实 API
            await doCommitAndPush(commitParams, pushParams);
          }
        } finally {
          this.loading = false;
        }
      }

      // 关闭模态框
      if (this.mode === 'button') {
        this.buttonVisible = false;
      } else {
        this.internalVisible = false;
      }
    },

    // ==================== 提交（emit / 内部回写） ====================
    async handleOK() {
      const ok = await this.validateWriteBackForm();
      if (!ok) return;

      // emit 模式：若勾选“回写前校验”，组件内部直接拉起校验弹窗；否则直接回写
      if (this.submitMode === "emit") {
        const needValidation = this.writeBack.needValidation || false;

        if (needValidation && this.enableValidation) {
          // 关闭当前回写弹窗
          if (this.mode === 'button') {
            this.buttonVisible = false;
          } else {
            this.internalVisible = false;
          }
          // 原型模式：生成 mock 校验结果；真实模式可在此接入真实校验接口
          // 原型模式：使用 dataSource 作为 dataA，生成校验结果时优先复用其中的 id，便于在校验弹窗中观察删除是否真实作用于 dataA
          this.validationTableData = this.isPrototype
            ? buildMockValidationResult(this.dataSource || [])
            : [];
          this.validationVisible = true;
          message.success("校验完成（原型），请在弹窗中处理重复组");
          return;
        }

        // 不需要校验，直接在组件内部执行回写逻辑
        await this.executeWriteBack();
        return;
      }

      // writeBack 模式：直接在组件内部执行回写逻辑
      await this.executeWriteBack();
    },
    // 关闭导出模态框
    handleClose() {
      if (this.mode === 'button') {
        this.buttonVisible = false;
      } else {
        this.internalVisible = false;
      }
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

    // ==================== 回写校验弹窗事件 ====================
    async handleValidationWriteBack(/* payload */) {
      // 回写校验模态框点击“回写”后：
      // 1）先关闭校验模态框
      // 2）复用本组件的 executeWriteBack 逻辑，避免重复业务代码
      //    且能正确根据“回写后推送”执行 git commit / push（含原型 mock）
      this.validationVisible = false;
      await this.executeWriteBack();
    },
  },
};
</script>