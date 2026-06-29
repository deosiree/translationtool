/**
 * Agent 预翻译回填映射单元测试
 */

import { describe, it, expect } from "vitest";
import {
  applyAgentBackfill,
  resolveAgentSuggestedTranslation,
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
  });
});
