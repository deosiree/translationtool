import { describe, it, expect } from "vitest";
import { isTextOverflow } from "@/components/table/isTextOverflow.js";

describe("isTextOverflow", () => {
  it("元素为空时应返回 false", () => {
    expect(isTextOverflow(null)).toBe(false);
    expect(isTextOverflow(undefined)).toBe(false);
  });

  it("scrollWidth 大于 clientWidth 时应返回 true", () => {
    const el = { scrollWidth: 120, clientWidth: 80 };
    expect(isTextOverflow(el)).toBe(true);
  });

  it("scrollWidth 不大于 clientWidth 时应返回 false", () => {
    const el = { scrollWidth: 80, clientWidth: 80 };
    expect(isTextOverflow(el)).toBe(false);
  });
});
