import { describe, it, expect } from "vitest";
import {
  checkLen,
  checkPlace,
  pickPlace,
  checkBadDisplay,
  checkMultiSpace,
  checkEscape,
  checkOperator,
  checkConcat,
  byteLen,
  MSG_LEN,
  MSG_PLACE,
  MSG_COLON_PLACE,
  MSG_BAD_DISPLAY,
  MSG_MULTI_SPACE,
  MSG_ESCAPE,
  MSG_OPERATOR,
  MSG_CONCAT,
  RULES,
  rulesForTarget,
} from "@/utils/formRules.js";

describe("formRules.byteLen / checkLen", () => {
  it("max 为空/≤0 不限", () => {
    expect(checkLen("很长很长", null)).toBeNull();
    expect(checkLen("很长很长", 0)).toBeNull();
    expect(checkLen("很长很长", "")).toBeNull();
  });

  it("英1/中2/俄3；超长文案无后缀", () => {
    expect(byteLen("ab")).toBe(2);
    expect(byteLen("中文")).toBe(4);
    expect(byteLen("Привет")).toBe(18); // 6 * 3
    expect(checkLen("中文", 3)).toBe(`${MSG_LEN}3`);
    expect(checkLen("中文", 4)).toBeNull();
    expect(checkLen("ab", 2)).toBeNull();
    expect(checkLen("П", 2)).toBe(`${MSG_LEN}2`);
    expect(checkLen("П", 3)).toBeNull();
  });
});

describe("formRules.pickPlace / checkPlace", () => {
  it("提取 %数字 / %字母 / {n}", () => {
    expect(pickPlace("A %1 and %2 {0}")).toEqual(["%1", "%2", "{0}"]);
  });

  it("提取 [{}] / {} / (*.后缀)，长匹配优先", () => {
    expect(pickPlace("[{}]")).toEqual(["[{}]"]);
    expect(pickPlace("[{}-{}-{}]")).toEqual(["{}", "{}", "{}"]);
    expect(pickPlace("(*.scd)")).toEqual(["(*.scd)"]);
    expect(pickPlace("[{ }]")).toEqual([]);
    expect(pickPlace("(* .scd)")).toEqual([]);
  });

  it("占位一致通过（含占位间空格）", () => {
    expect(checkPlace("请输入%1", "enter %1")).toBeNull();
    expect(checkPlace("[{}]-[{}]", "[{}] - [{}]")).toBeNull();
    expect(checkPlace("[{}-{}-{}]", "[{} - {} - {}]")).toBeNull();
  });

  it("占位不一致或插空失败", () => {
    expect(checkPlace("请输入%1", "enter % 1")).toBe(MSG_PLACE);
    expect(checkPlace("无占位", "has %1")).toBe(MSG_PLACE);
    expect(checkPlace("有%1", "none")).toBe(MSG_PLACE);
    expect(checkPlace("[{}]", "[{ }]")).toBe(MSG_PLACE);
    expect(checkPlace("(*.scd)", "(* .scd)")).toBe(MSG_PLACE);
  });

  it("& 不作为占位符", () => {
    expect(pickPlace("Verify&Loader")).toEqual([]);
    expect(checkPlace("Verify&Loader", "Verify&&Loader")).toBeNull();
  });

  it("原文 ：%n 须对应译文 : %n", () => {
    expect(checkPlace("状态：%1", "Status: %1")).toBeNull();
    expect(checkPlace("状态：%1", "Status:%1")).toBe(MSG_COLON_PLACE);
    expect(checkPlace("状态：%1", "Status:% 1")).toBe(MSG_COLON_PLACE);
    expect(checkPlace("A：%1 B：%2", "A: %1")).toBe(MSG_COLON_PLACE);
    expect(checkPlace("状态：%1", "Status: %10")).toBe(MSG_COLON_PLACE);
    // 原文无 ：%n 时不触发本条（其它占位规则仍生效）
    expect(checkPlace("状态%1", "Status: %1")).toBeNull();
    expect(checkPlace("无冒号", "Status: %1")).toBe(MSG_PLACE);
  });
});

describe("formRules.checkBadDisplay", () => {
  it("· 与孤立 & 失败；&& 通过", () => {
    expect(checkBadDisplay("a·b")).toBe(MSG_BAD_DISPLAY);
    expect(checkBadDisplay("Verify&Loader")).toBe(MSG_BAD_DISPLAY);
    expect(checkBadDisplay("Verify&&Loader")).toBeNull();
    expect(checkBadDisplay("ok")).toBeNull();
  });
});

describe("formRules.checkMultiSpace", () => {
  it("连续普通空格失败", () => {
    expect(checkMultiSpace("a  b")).toBe(MSG_MULTI_SPACE);
    expect(checkMultiSpace("a b")).toBeNull();
  });
});

describe("formRules.checkEscape", () => {
  it("字面转义与真实控制符均失败", () => {
    expect(checkEscape("line\\nbreak")).toBe(MSG_ESCAPE);
    expect(checkEscape("line\\tbreak")).toBe(MSG_ESCAPE);
    expect(checkEscape("a\nb")).toBe(MSG_ESCAPE);
    expect(checkEscape("a\tb")).toBe(MSG_ESCAPE);
    expect(checkEscape("ok")).toBeNull();
  });
});

describe("formRules.checkOperator", () => {
  it("纯运算符失败，含词通过", () => {
    expect(checkOperator("!=")).toBe(MSG_OPERATOR);
    expect(checkOperator("IN")).toBe(MSG_OPERATOR);
    expect(checkOperator("in")).toBe(MSG_OPERATOR);
    expect(checkOperator("-")).toBe(MSG_OPERATOR);
    expect(checkOperator("a + b")).toBeNull();
    expect(checkOperator("not empty")).toBeNull();
  });
});

describe("formRules.checkConcat", () => {
  it("拼接残片失败", () => {
    expect(checkConcat("参数verName[")).toBe(MSG_CONCAT);
    expect(checkConcat("日志【")).toBe(MSG_CONCAT);
    expect(checkConcat("】")).toBe(MSG_CONCAT);
    expect(checkConcat("]")).toBe(MSG_CONCAT);
    expect(checkConcat("完整[句子]")).toBeNull();
    expect(checkConcat("正常句")).toBeNull();
  });
});

describe("formRules.RULES / rulesForTarget", () => {
  it("translate 不含 entry-only；entry 含原文规则", () => {
    const tKeys = rulesForTarget("translate").map((r) => r.key);
    const eKeys = rulesForTarget("entry").map((r) => r.key);
    expect(tKeys).toContain("toLong");
    expect(tKeys).toContain("special");
    expect(tKeys).toContain("noBadDisplay");
    expect(tKeys).toContain("noMultiSpace");
    expect(tKeys).not.toContain("noEscape");
    expect(tKeys).not.toContain("noOperator");
    expect(tKeys).not.toContain("noConcat");
    expect(eKeys).toContain("noEscape");
    expect(eKeys).toContain("noBadDisplay");
    expect(RULES.find((r) => r.key === "toLong").checked).toBe(true);
    expect(RULES.find((r) => r.key === "noBadDisplay").checked).toBe(false);
  });

  it("translate options 均带多行 tooltip", () => {
    for (const opt of rulesForTarget("translate")) {
      expect(opt.tooltip).toBeTruthy();
      expect(opt.tooltip).toContain("\n");
    }
  });
});
