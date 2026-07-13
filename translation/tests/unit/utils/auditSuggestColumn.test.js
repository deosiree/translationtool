import { describe, it, expect } from "vitest";
import { isAuditSuggestColumn } from "@/utils/auditSuggestColumn.js";

describe("isAuditSuggestColumn", () => {
  it("colValue 为 auditSuggest 时命中", () => {
    expect(isAuditSuggestColumn({ colValue: "auditSuggest" })).toBe(true);
  });

  it("动态语种 auditSuggest 字段名命中", () => {
    expect(
      isAuditSuggestColumn({ dataIndex: "englishAuditSuggest" })
    ).toBe(true);
  });

  it("归档 auditSuggess 字段名命中", () => {
    expect(isAuditSuggestColumn({ dataIndex: "auditSuggess" })).toBe(true);
  });

  it("普通列不命中", () => {
    expect(isAuditSuggestColumn({ dataIndex: "entry", colValue: "entry" })).toBe(
      false
    );
  });

  it("空 column 不命中", () => {
    expect(isAuditSuggestColumn(null)).toBe(false);
    expect(isAuditSuggestColumn(undefined)).toBe(false);
  });
});
