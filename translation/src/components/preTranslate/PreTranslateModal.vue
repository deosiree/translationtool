<template>
  <Modal
    :visible="visible"
    :modalTitle="modalTitle"
    modalWidth="860px"
    :okLoading="okLoading"
    @handleClose="handleClose"
    @handleOK="handleOKInternal"
    @afterClose="handleAfterClose"
  >
    <div style="width: 100%; height: 100%">
      <a-form
        name="pre-translate-form"
        autocomplete="off"
        :label-col="{ style: { width: '88px' } }"
        :model="formState"
      >
        <!-- <a-form-item label="任务名称">
          <span>{{ currentTask && currentTask.name }}</span>
        </a-form-item>

        <a-form-item label="词条数量">
          <span>{{ dataPreTranslate.length }} 条</span>
        </a-form-item> -->

        <a-form-item
          label="优先级"
          name="priority"
          :rules="[{ required: true, message: '请选择优先级!' }]"
        >
          <a-select
            v-model:value="formState.priority"
            placeholder="请选择"
            allowClear
          >
            <a-select-option value="agent">Agent翻译</a-select-option>
            <a-select-option value="shuyuku">术语库</a-select-option>
            <a-select-option value="deepl">DeepL翻译</a-select-option>
            <a-select-option value="youdao">有道翻译</a-select-option>
            <a-select-option value="baidu">百度翻译</a-select-option>
            <a-select-option value="google">Google翻译</a-select-option>
            <a-select-option value="module">本地模型</a-select-option>
            <a-select-option value="synthesis">
              综合优先级
              <a-tooltip placement="top">
                <template #title>
                  <span
                    >使用所有的翻译引擎进行翻译，取出现次数最多的翻译为当前词条的翻译！</span
                  >
                </template>
                <InfoCircleOutlined
                  style="float: right; color: #fbb31f; margin-top: 5px"
                />
              </a-tooltip>
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>

      <!-- Agent 翻译专属面板 -->
      <div v-if="formState.priority === 'agent'" class="agent-panel">
        <a-form :label-col="{ style: { width: '120px' } }" :model="agentForm">
          <a-form-item label="置信度阈值">
            <a-row :gutter="12" align="middle">
              <a-col :span="16">
                <a-slider
                  v-model:value="agentForm.confidenceThreshold"
                  :min="0.5"
                  :max="0.95"
                  :step="0.05"
                  :tip-formatter="(v) => `${Math.round(v * 100)}%`"
                />
              </a-col>
              <a-col :span="8">
                <span class="threshold-label"
                  >{{ Math.round(agentForm.confidenceThreshold * 100) }}%</span
                >
              </a-col>
            </a-row>
            <div class="field-hint">
              高于阈值：自动回填到翻译列；低于阈值：不改动翻译列，仅进入「术语学习」待审核队列。
            </div>
          </a-form-item>
          <a-form-item label="目标语种">
            <span>{{ targetLanguageLabel || "—" }}</span>
          </a-form-item>
        </a-form>

        <a-alert type="info" show-icon message="Agent 预翻译说明">
          <template #description>
            <div class="alert-desc">
              基于术语库相似词条进行 RAG 检索，由 LangGraph Agent 生成预翻译结果。
              <a-button type="link" size="small" @click="goTerminologyAgent"
                >前往术语学习审核页</a-button
              >
            </div>
          </template>
        </a-alert>
      </div>
    </div>
  </Modal>
</template>

<script>
import Modal from "@/components/modal/index.vue";
import { InfoCircleOutlined } from "@ant-design/icons-vue";
import { preTranslate } from "@/http/api/workbench";
import { agentPreTranslate } from "@/http/api/terminologyAgent";
import { appendPendingFromPreTranslate } from "@/utils/agentPendingAudits";
import { applyAgentBackfill } from "@/utils/agentPreTranslateBackfill";
import { message } from "ant-design-vue";

/** Agent 后端就绪后使用真实 API */
const USE_AGENT_MOCK = false;

export default {
  name: "PreTranslateModal",
  components: {
    Modal,
    InfoCircleOutlined,
  },
  emits: ["handleClose", "handleOK", "afterClose"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
      default: "预翻译",
    },
    currentTask: {
      type: Object,
      default: () => ({}),
    },
    dataPreTranslate: {
      type: Array,
      default: () => [],
    },
    language: {
      type: Object,
      default: () => ({ value: "" }),
    },
    department: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      okLoading: false,
      formState: {
        priority: null,
      },
      agentForm: {
        confidenceThreshold: 0.8,
      },
    };
  },
  computed: {
    targetLanguageLabel() {
      const type = this.currentTask && this.currentTask.translateType;
      if (!type || typeof type !== "string") return "";
      const parts = type.split("-");
      return parts[1] || type;
    },
  },
  methods: {
    handleClose() {
      this.$emit("handleClose");
    },
    handleAfterClose() {
      this.formState.priority = null;
      this.agentForm.confidenceThreshold = 0.8;
      this.$emit("afterClose");
    },
    goTerminologyAgent() {
      this.$router.push({ name: "terminologyAgent" });
    },
    /**
     * 预翻译确认入口：按 priority 分流 Agent / 传统引擎，向父组件 emit 结果
     * @emits handleOK - { success, data?, priority?, taskId?, meta?, error? }
     */
    async handleOKInternal() {
      if (!this.formState.priority) {
        message.warning("请选择优先级！");
        return;
      }
      if (this.dataPreTranslate.length === 0) {
        message.warning("没有可预翻译的词条！");
        return;
      }

      this.okLoading = true;
      try {
        let translateResult = null;
        let meta = null;

        if (this.formState.priority === "agent") {
          const agentRes = await this.callAgentPreTranslate();
          translateResult = agentRes.list;
          meta = agentRes.meta;
          if (meta?.mock) {
            this.persistPendingAudits(translateResult);
          }
        } else {
          translateResult = await this.callPreTranslateAPI();
        }

        this.$emit("handleOK", {
          success: true,
          data: translateResult,
          priority: this.formState.priority,
          taskId: this.currentTask && this.currentTask.id,
          meta,
        });
      } catch (err) {
        message.error("预翻译失败！" + (err.message || ""));
        this.$emit("handleOK", {
          success: false,
          error: err.message || "预翻译失败",
        });
      } finally {
        this.okLoading = false;
      }
    },

    /**
     * Mock 模式下将 needs_human 条目写入 localStorage 待审核队列
     * @param {Array<Object & { agent_meta?: import('@/http/api/terminologyAgent').AgentMeta }>} entries
     */
    persistPendingAudits(entries) {
      appendPendingFromPreTranslate({
        entries,
        task: this.currentTask,
        targetLang: this.targetLanguageLabel,
        department: this.department,
      });
    },

    /**
     * 调用 Java 后端 /workbench/preTranslate（术语库、DeepL 等传统优先级）
     * @returns {Promise<Array<Object>>} 带 translate 字段的词条列表
     */
    async callPreTranslateAPI() {
      const params = {
        taskID: this.currentTask && this.currentTask.id,
        priority: this.formState.priority,
      };
      const res = await preTranslate(params, this.dataPreTranslate);
      return (res.data.list || []).map((item) => {
        item.translate = item[this.language.value];
        return item;
      });
    },

    /**
     * 调用 Agent 批量预翻译；auto_approved 条目映射到 language.value 与 translate，API 不可用时回退 mock
     * @returns {Promise<{ list: Array<Object>, meta: { autoCount: number, pendingCount: number, threshold: number, mock?: boolean } }>}
     */
    async callAgentPreTranslate() {
      if (USE_AGENT_MOCK) {
        return this.mockAgentPreTranslate();
      }

      const params = {
        taskID: this.currentTask && this.currentTask.id,
        confidenceThreshold: this.agentForm.confidenceThreshold,
        taskName: this.currentTask && this.currentTask.name,
        productName: this.currentTask && this.currentTask.productName,
        targetLang: this.targetLanguageLabel,
        department: this.department,
      };
      try {
        const res = await agentPreTranslate(params, this.dataPreTranslate);
        const data = res.data || {};
        const langField = this.language.value;
        const list = (data.list || []).map((item) =>
          applyAgentBackfill({ ...item }, langField)
        );
        return {
          list,
          meta: {
            autoCount: data.auto_count ?? 0,
            pendingCount: data.pending_count ?? 0,
            threshold: this.agentForm.confidenceThreshold,
          },
        };
      } catch (err) {
        console.warn("[PreTranslateModal] Agent API 不可用，回退 Mock", err);
        message.warning(
          "Agent 服务不可用，当前结果为本地 Mock 演示数据（非真实 Grep/LLM 结果）"
        );
        return this.mockAgentPreTranslate();
      }
    },

    /**
     * Mock Agent 预翻译：模拟 RAG + 置信度分流
     * @returns {Promise<{ list: Array<Object>, meta: { autoCount: number, pendingCount: number, threshold: number, mock: true } }>}
     */
    async mockAgentPreTranslate() {
      await new Promise((resolve) => setTimeout(resolve, 800));

      const threshold = this.agentForm.confidenceThreshold;
      let autoCount = 0;
      let pendingCount = 0;

      const list = this.dataPreTranslate.map((item, index) => {
        // 确定性 mock：根据 index 生成不同置信度，便于 UI 演示
        const confidence = 0.55 + ((index * 17) % 40) / 100;
        const reviewStatus =
          confidence >= threshold ? "auto_approved" : "needs_human";
        if (reviewStatus === "auto_approved") {
          autoCount += 1;
        } else {
          pendingCount += 1;
        }

        const entry = item.entry || "";
        const mockTranslation = this.buildMockTranslation(entry);
        const autoApproved = reviewStatus === "auto_approved";

        return {
          ...item,
          ...(autoApproved
            ? {
                [this.language.value]: mockTranslation,
                translate: mockTranslation,
              }
            : {}),
          agent_meta: {
            confidence,
            review_status: reviewStatus,
            suggested_translation: mockTranslation,
            similar_terms: entry
              ? [
                  {
                    entry: `[相似] ${entry.substring(0, Math.min(20, entry.length))}...`,
                    translate: mockTranslation,
                  },
                ]
              : [],
            retrieval_method: "mock_hybrid",
            reasoning: "Mock：基于术语库相似词条生成的预翻译建议",
          },
        };
      });

      return {
        list,
        meta: {
          autoCount,
          pendingCount,
          threshold,
          mock: true,
        },
      };
    },

    /**
     * 生成 mock 译文，保留 %N 占位符
     * @param {string} entry - 词条原文
     * @returns {string}
     */
    buildMockTranslation(entry) {
      if (!entry) return "[Agent Mock]";
      // 保留 %N 占位符的简易 mock
      const preserved = entry.replace(/%\d+/g, (m) => m);
      return `[Agent] ${preserved}`;
    },
  },
};
</script>

<style scoped>
.agent-panel {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e8e8e8;
}
.threshold-label {
  font-weight: 600;
  color: #1890ff;
}
.field-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}
.alert-desc {
  font-size: 13px;
  line-height: 1.6;
}
</style>
