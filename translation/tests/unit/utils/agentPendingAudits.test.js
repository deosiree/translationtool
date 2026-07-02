/**
 * 术语学习待审核筛选工具单元测试
 */

import { describe, it, expect } from "vitest";
import {
  buildAuditListParams,
  matchesAuditFilters,
} from "@/utils/agentPendingAudits";

describe("agentPendingAudits confidence open range", () => {
  describe("buildAuditListParams", () => {
    it("仅 max 时只传 confidenceMax（0~max）", () => {
      const params = buildAuditListParams({ confidenceMax: 80 });
      expect(params).toEqual({ confidenceMax: 0.8 });
      expect(params.confidenceMin).toBeUndefined();
    });

    it("仅 min 时只传 confidenceMin（min~100）", () => {
      const params = buildAuditListParams({ confidenceMin: 60 });
      expect(params).toEqual({ confidenceMin: 0.6 });
      expect(params.confidenceMax).toBeUndefined();
    });

    it("min 与 max 同时存在时两者都传", () => {
      const params = buildAuditListParams({
        confidenceMin: 60,
        confidenceMax: 80,
      });
      expect(params).toEqual({
        confidenceMin: 0.6,
        confidenceMax: 0.8,
      });
    });

    it("min 与 max 颠倒时自动交换后传参", () => {
      const params = buildAuditListParams({
        confidenceMin: 80,
        confidenceMax: 60,
      });
      expect(params).toEqual({
        confidenceMin: 0.6,
        confidenceMax: 0.8,
      });
    });

    it("无效置信度数值不传参", () => {
      const params = buildAuditListParams({
        confidenceMin: "abc",
        confidenceMax: 80,
      });
      expect(params).toEqual({ confidenceMax: 0.8 });
    });
  });

  describe("matchesAuditFilters", () => {
    it("仅 max 时匹配 confidence <= max", () => {
      const filters = { confidenceMax: 0.8 };
      expect(
        matchesAuditFilters({ confidence: 0.5 }, filters),
      ).toBe(true);
      expect(
        matchesAuditFilters({ confidence: 0.9 }, filters),
      ).toBe(false);
    });

    it("仅 min 时匹配 confidence >= min", () => {
      const filters = { confidenceMin: 0.6 };
      expect(
        matchesAuditFilters({ confidence: 0.7 }, filters),
      ).toBe(true);
      expect(
        matchesAuditFilters({ confidence: 0.5 }, filters),
      ).toBe(false);
    });
  });
});
