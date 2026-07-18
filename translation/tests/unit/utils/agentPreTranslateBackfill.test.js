/**
 * Agent 预翻译回填映射单元测试
 */

import { describe, it, expect } from "vitest";
import {
  applyAgentBackfill,
  resolveAgentSuggestedTranslation,
  resolveAuditSuggestField,
} from "@/utils/agentPreTranslateBackfill";

describe("agentPreTranslateBackfill", () => {
  describe("resolveAgentSuggestedTranslation", () => {
    it("优先使用 agent_meta.suggested_translation", () => {
      const item = {
        translate: "from translate",
        english: "from english",
        agent_meta: { suggested_translation: "Tool name" },
      };
      expect(resolveAgentSuggestedTranslation(item, "english")).toBe("Tool name");
    });

    it("其次使用顶层 translate", () => {
      const item = { translate: "Tool name" };
      expect(resolveAgentSuggestedTranslation(item, "english")).toBe("Tool name");
    });

    it("最后回退到语种字段", () => {
      const item = { english: "Tool name" };
      expect(resolveAgentSuggestedTranslation(item, "english")).toBe("Tool name");
    });
  });

  describe("resolveAuditSuggestField", () => {
    it("english 映射到 englishAuditSuggest", () => {
      expect(resolveAuditSuggestField("english")).toBe("englishAuditSuggest");
    });

    it("未知语种返回 undefined", () => {
      expect(resolveAuditSuggestField("unknown")).toBeUndefined();
    });
  });

  describe("applyAgentBackfill", () => {
    it("auto_approved 时将 suggested_translation 写入 english 与 translate", () => {
      const item = {
        id: "e1",
        entry: "工具名称",
        agent_meta: {
          review_status: "auto_approved",
          suggested_translation: "Tool name",
          confidence: 1.0,
        },
      };
      const result = applyAgentBackfill(item, "english");
      expect(result.english).toBe("Tool name");
      expect(result.translate).toBe("Tool name");
    });

    it("auto_approved 时将 reasoning 拷贝到 englishAuditSuggest", () => {
      const item = {
        id: "e1b",
        entry: "ADM/R01-RAG精确",
        agent_meta: {
          review_status: "auto_approved",
          suggested_translation: "ADM Exact RAG",
          reasoning: "基于术语：精确匹配术语库",
          confidence: 1.0,
        },
      };
      const result = applyAgentBackfill(item, "english");
      expect(result.englishAuditSuggest).toBe("基于术语：精确匹配术语库");
    });

    it("needs_human 时不写入审核意见", () => {
      const item = {
        id: "e1c",
        entry: "ADM/T99",
        agent_meta: {
          review_status: "needs_human",
          suggested_translation: "New term",
          reasoning: "基于LLM机翻：术语库未命中，LLM 机翻生成",
          confidence: 0.65,
        },
      };
      const result = applyAgentBackfill({ ...item }, "english");
      expect(result.englishAuditSuggest).toBeUndefined();
    });

    it("auto_approved 时将顶层 translate 写入 russian", () => {
      const item = {
        id: "e2",
        entry: "按钮",
        translate: "Кнопка",
        agent_meta: {
          review_status: "auto_approved",
          confidence: 1.0,
        },
      };
      const result = applyAgentBackfill(item, "russian");
      expect(result.russian).toBe("Кнопка");
      expect(result.translate).toBe("Кнопка");
    });

    it("needs_human 时不回填语种字段", () => {
      const item = {
        id: "e3",
        entry: "admin",
        agent_meta: {
          review_status: "needs_human",
          suggested_translation: "[suggest] admin",
          confidence: 0.55,
        },
      };
      const result = applyAgentBackfill({ ...item }, "english");
      expect(result.english).toBeUndefined();
      expect(result.translate).toBeUndefined();
    });

    it("无 langField 时不回填", () => {
      const item = {
        agent_meta: {
          review_status: "auto_approved",
          suggested_translation: "Tool name",
        },
      };
      const result = applyAgentBackfill(item, "");
      expect(result.translate).toBeUndefined();
    });

    it("auto_approved 但无译文时不回填", () => {
      const item = {
        agent_meta: { review_status: "auto_approved" },
      };
      const result = applyAgentBackfill(item, "english");
      expect(result.english).toBeUndefined();
      expect(result.translate).toBeUndefined();
    });

    it("将 agent_meta.segment_trace 挂到 segmentTrace（含 needs_human）", () => {
      const trace = { jieba: ["文件"], aligned: [], display: "文件" };
      const item = {
        agent_meta: {
          review_status: "needs_human",
          segment_trace: trace,
        },
      };
      const result = applyAgentBackfill(item, "english");
      expect(result.segmentTrace).toEqual(trace);
    });
  });
});
