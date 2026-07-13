/**
 * 术语学习待审核筛选工具单元测试
 */

import { describe, it, expect, beforeEach } from "vitest";
import {
  buildAuditListParams,
  matchesAuditFilters,
  mergePendingAudits,
  auditWriteFingerprint,
  appendPendingFromPreTranslate,
  clearLocalMockPendingAudits,
  saveLocalPendingAudits,
  AGENT_PENDING_AUDITS_KEY,
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

describe("mergePendingAudits no display dedup", () => {
  beforeEach(() => {
    localStorage.removeItem(AGENT_PENDING_AUDITS_KEY);
  });

  it("同 entry 的 API 与 local 行全部展示", () => {
    saveLocalPendingAudits([
      {
        id: "local-1",
        entry_info_id: "e1",
        source_text: "ADM",
        suggested_translation: "A",
        _local: true,
      },
    ]);
    const apiItems = [
      {
        id: "api-1",
        entry_info_id: "e1",
        source_text: "ADM",
        suggested_translation: "B",
      },
    ];
    const merged = mergePendingAudits({ apiItems });
    expect(merged).toHaveLength(2);
  });
});

describe("appendPendingFromPreTranslate write-time dedup", () => {
  beforeEach(() => {
    localStorage.removeItem(AGENT_PENDING_AUDITS_KEY);
  });

  it("全字段指纹相同则跳过追加", () => {
    saveLocalPendingAudits([
      {
        id: "local-existing",
        entry_info_id: "e1",
        source_text: "ADM",
        entry_comment: "c1",
        suggested_translation: "trans1",
        target_lang: "英文",
        department: "ADM",
        retrieval_method: "grep",
        confidence: 0.8,
        _local: true,
      },
    ]);
    const added = appendPendingFromPreTranslate({
      entries: [
        {
          id: "e1",
          entry: "ADM",
          comment: "c1",
          agent_meta: {
            review_status: "needs_human",
            suggested_translation: "trans1",
            confidence: 0.8,
            retrieval_method: "grep",
          },
        },
      ],
      targetLang: "英文",
      department: "ADM",
    });
    expect(added).toBe(0);
  });
});

describe("clearLocalMockPendingAudits", () => {
  beforeEach(() => {
    localStorage.removeItem(AGENT_PENDING_AUDITS_KEY);
  });

  it("仅清除 _local/_mock 条目", () => {
    saveLocalPendingAudits([
      { id: "mock-1", _mock: true, source_text: "a" },
      { id: "keep-1", source_text: "b" },
    ]);
    const removed = clearLocalMockPendingAudits();
    expect(removed).toBe(1);
  });
});

describe("auditWriteFingerprint", () => {
  it("不同 entry_comment 指纹不同", () => {
    const base = {
      source_text: "ADM",
      suggested_translation: "t",
      target_lang: "英文",
      department: "ADM",
      retrieval_method: "grep",
      confidence: 0.8,
    };
    expect(
      auditWriteFingerprint({ ...base, entry_comment: "a" }),
    ).not.toBe(auditWriteFingerprint({ ...base, entry_comment: "b" }));
  });
});
