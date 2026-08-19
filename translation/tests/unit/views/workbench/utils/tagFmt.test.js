import { describe, it, expect } from "vitest";
import { companyCut, formatTagText } from "@/views/workbench/utils/tagFmt";

describe("tagFmt", () => {
  it("companyCut 按中英文分号分割并去空", () => {
    expect(companyCut("a;b；c")).toEqual(["a", "b", "c"]);
    expect(companyCut(null)).toEqual([]);
    expect(companyCut("")).toEqual([]);
  });

  it("formatTagText 用分号连接", () => {
    expect(formatTagText("a;b")).toBe("a; b");
  });
});
