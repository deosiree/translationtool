import { describe, it, expect } from "vitest";
import { formatEntryText, formatCellText } from "@/components/table/cellText.js";

describe("formatEntryText", () => {
  it("null 与空字符串应返回空串", () => {
    expect(formatEntryText(null)).toBe("");
    expect(formatEntryText("")).toBe("");
  });

  it("应将换行转义为可见 \\n", () => {
    expect(formatEntryText("a\nb")).toBe("a\\nb");
  });
});

describe("formatCellText", () => {
  it("null 与空字符串应返回空串", () => {
    expect(formatCellText(null)).toBe("");
    expect(formatCellText("")).toBe("");
  });

  it("数字 0 应转为字符串", () => {
    expect(formatCellText(0)).toBe("0");
  });
});
