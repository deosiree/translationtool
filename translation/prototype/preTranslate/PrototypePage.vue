<template>
  <div style="padding: 16px;">
    <a-typography-title :level="4" style="margin-bottom: 12px;">
      原型：词条翻译 + 预翻译（支持词条管理优先级）
    </a-typography-title>
    <a-typography-paragraph type="secondary" style="margin-bottom: 16px; max-width: 860px;">
      本页用于在不改动正式工作台页面的前提下，基于指定任务 ID 加载真实词条数据，
      并体验工作台翻译模态框中的“预翻译（含词条管理优先级）”新交互。
    </a-typography-paragraph>

    <a-space>
      <a-button type="primary" @click="openModal">
        打开翻译模态框（原型，真实数据）
      </a-button>
      <a-button @click="resetMockTask">
        重置任务上下文
      </a-button>
    </a-space>

    <a-card size="small" title="当前任务（真实接口）" style="margin-top: 16px; max-width: 720px;">
      <a-descriptions size="small" :column="2" bordered>
        <a-descriptions-item label="任务ID">
          {{ currentTask.id }}
        </a-descriptions-item>
        <a-descriptions-item label="任务名称">
          {{ currentTask.name }}
        </a-descriptions-item>
        <a-descriptions-item label="产品名称">
          {{ currentTask.productName || "（从接口返回的 productName）" }}
        </a-descriptions-item>
        <a-descriptions-item label="上级分类名称">
          {{ currentTask.classifyName || "（从接口返回的 classifyName）" }}
        </a-descriptions-item>
        <a-descriptions-item label="翻译语种">
          {{ currentTask.translateType || "（从接口返回的 translateType/lang）" }}
        </a-descriptions-item>
      </a-descriptions>
    </a-card>

    <!-- 翻译模态框原型：整组件拷贝自工作台 translateModal.vue，使用真实接口 -->
    <TranslateModal :visible="modalVisible" modalTitle="原型：词条翻译（任务 92dd17ca-99d5-4e39-92f3-44efd73027bb）"
      :currentTask="currentTask" :classifyLimit="classifyLimit" @handleClose="handleModalClose"
      @handleOK="handleModalOk" @afterSave="handleAfterSave" />
  </div>
</template>

<script>
import { message } from "ant-design-vue";
import TranslateModal from "./translateModal.vue";
import { getEntryInfoList } from "@/http/api/workbench";

export default {
  components: {
    TranslateModal,
  },
  data() {
    return {
      // 控制翻译模态框显隐
      modalVisible: false,
      // 目标任务 ID（真实任务）
      taskId: "92dd17ca-99d5-4e39-92f3-44efd73027bb",
      // 由真实接口返回的数据推导的任务上下文（尽量减少 mock）
      currentTask: {
        name: "工具-ts",
        productName: "工具-ts",
        translateType: "英文",
        department: "通用平台部",
        auditor: "惠岩",
        creator: "惠岩",
      },
      currentTaskRes: {
        "versionName": null,
        "productName": "工具-ts",
        "productId": "b177ab7a-d368-4fe2-9cb6-a4b4d1215b98",
        "entryNum": 0,
        "tableName": null,
        "classifyName": null,
        "id": "92dd17ca-99d5-4e39-92f3-44efd73027bb",
        "creator": "惠岩",
        "upgrade": 0,
        "name": "工具-ts",
        "createTime": "2025-12-11 06:18:15",
        "endTime": null,
        "department": "通用平台部",
        "developer": "惠岩",
        "entryAuditor": "惠岩",
        "translator": "惠岩",
        "translationAuditor": "惠岩",
        "description": null,
        "state": "1",
        "versionId": null,
        "importTime": null,
        "entryAutiorStartTime": null,
        "translationAuditorStartTime": null,
        "translateStartTime": null,
        "deliveryTime": "2025-12-11 06:18:15",
        "translateType": "英文",
        "isDelete": 0,
        "codeBranch": "分支测试",
        "index": 3,
        "isHighlighted": true,
        "num__total": 37,
        "num_entryExamine": 0,
        "num_import": 0,
        "num_translate": 37,
        "num_translateExamine": 0,
        "transMap": {
          "name": "英文",
          "value": "english",
          "state": "englishTranslateState",
          "chineseState": "englishChineseState",
          "publicState": "englishPublicState",
          "checked": "englishChecked",
          "auditSuggest": "englishAuditSuggest",
          "transIdName": "engTransId",
          "interpretation": "englishInterpretation"
        }
      },
      // 对于当前原型场景，classifyLimit 可给一个最小可用对象
      classifyLimit: {},
    };
  },
  mounted() {
    // 进入页面时就调用一次真实任务的词条接口，并在控制台打印返回结果
    this.loadTaskEntryInfo();
  },
  methods: {
    async loadTaskEntryInfo() {
      try {
        const data = {
          taskID: this.taskId,
          entryState: 3,
          entry: "",
        };
        const params = ["0", "2"];
        const res = await getEntryInfoList(data, params);
        // 打印完整响应，便于在浏览器控制台查看真实数据结构
        // eslint-disable-next-line no-console
        console.log(
          "[prototype][preTranslate] getEntryInfoList res:",
          res
        );
        const list =
          (res && (res.data || res.result || res.list || res.rows)) || [];
        if (Array.isArray(list) && list.length > 0) {
          const first = list[0];
          this.currentTask = {
            ...this.currentTask,
            id: this.taskId,
            name:
              first.taskName ||
              this.currentTask.name ||
              "【原型】工作台预翻译联调（任务 92dd…）",
            productName: first.productName || this.currentTask.productName,
            classifyName: first.classifyName || this.currentTask.classifyName,
            translateType:
              first.translateType ||
              first.lang ||
              this.currentTask.translateType,
            versionId: first.versionId || this.currentTask.versionId,
          };
        }
      } catch (e) {
        // 拉取失败时仅做日志与轻提示，不阻塞原型使用
        // eslint-disable-next-line no-console
        console.error(
          "[prototype][preTranslate] 加载任务词条失败（mounted 阶段）",
          e
        );
        message.error("加载任务词条失败，将使用默认任务上下文");
      }
    },

    // ==================== UI交互控制 ====================
    async openModal() {
      // 打开模态框前，确保任务上下文已经从真实接口加载（mounted 时已调用一次，这里仅作为兜底）
      if (!this.currentTask.productName && !this.currentTask.translateType) {
        await this.loadTaskEntryInfo();
      }
      this.modalVisible = true;
    },
    handleModalClose() {
      this.modalVisible = false;
    },
    handleModalOk() {
      message.success("已触发保存（翻译模态框 原型）");
    },
    handleAfterSave() {
      // 这里可根据需要触发列表刷新等逻辑，原型先做简单日志与轻提示
      // eslint-disable-next-line no-console
      console.log(
        "[prototype][preTranslate] translateModal afterSave with task",
        this.currentTask
      );
      message.success("原型：翻译模态框 afterSave 回调已触发");
    },
    resetMockTask() {
      this.currentTask = {
        id: this.taskId,
        name: "【原型】工作台预翻译联调（任务 92dd…）",
        productName: "",
        classifyName: "",
        translateType: "",
        versionId: "",
      };
      message.success("已重置任务上下文（仍指向任务 92dd…）");
    },
  },
};
</script>
