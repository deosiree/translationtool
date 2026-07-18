import { describe, it, expect } from "vitest";
import { calcOpStrip, labelTextW, moreSlotW } from "@/components/OperationColumn/operationWidth.js";

describe("calcOpStrip", () => {
  it("N=4 slots=2：1 行内 + 更多（编辑+更多）", () => {
    expect(calcOpStrip(4, 2)).toEqual({ inlineOpCount: 1, showMore: true });
  });

  it("N=4 slots=3：2 行内 + 更多", () => {
    expect(calcOpStrip(4, 3)).toEqual({ inlineOpCount: 2, showMore: true });
  });

  it("N=2 slots=2：全行内无更多", () => {
    expect(calcOpStrip(2, 2)).toEqual({ inlineOpCount: 2, showMore: false });
  });

  it("N=2 slots=3：全行内无更多", () => {
    expect(calcOpStrip(2, 3)).toEqual({ inlineOpCount: 2, showMore: false });
  });

  it("N=3 slots=3：全行内无更多", () => {
    expect(calcOpStrip(3, 3)).toEqual({ inlineOpCount: 3, showMore: false });
  });

  it("N=3 slots=2：1 行内 + 更多（下拉 2）", () => {
    expect(calcOpStrip(3, 2)).toEqual({ inlineOpCount: 1, showMore: true });
  });

  it("slots=1：仅更多", () => {
    expect(calcOpStrip(5, 1)).toEqual({ inlineOpCount: 0, showMore: true });
  });
});

describe("labelTextW / moreSlotW", () => {
  it("纯 CJK 文案：每字 12px", () => {
    expect(labelTextW("编辑")).toBe(24);
  });

  it("「更多」槽宽", () => {
    expect(moreSlotW("更多")).toBe(58);
  });
});
