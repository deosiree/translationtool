import { describe, expect, it } from "vitest";
import {
  ENTRY_STATE_STATUS_MAP,
  IS_EXIST_STATUS_MAP,
  TRANSLATE_STATE_STATUS_MAP,
  getEntryStateLabel,
  getEntryStateOptions,
  getEntryStateStatus,
  getIsExistLabel,
  getIsExistStatus,
  getTranslateStateLabel,
  getTranslateStateOptions,
  getTranslateStateStatus,
} from "@/constants/statusMaps.js";

describe("statusMaps", () => {
  it("将数字和字符串存在状态解析为同一标签", () => {
    expect(getIsExistLabel(0)).toBe("新建");
    expect(getIsExistLabel("1")).toBe("已存在");
    expect(getIsExistLabel(99)).toBe("");
    expect(getIsExistStatus("0")).toEqual(IS_EXIST_STATUS_MAP["0"]);
  });

  it("保留词条状态标签、颜色与未知状态空值语义", () => {
    expect(getEntryStateLabel("1")).toBe("审核中");
    expect(getEntryStateLabel(-1)).toBe("禁用");
    expect(getEntryStateLabel(null)).toBe("");
    expect(getEntryStateStatus("2")).toEqual(ENTRY_STATE_STATUS_MAP["2"]);
    expect(ENTRY_STATE_STATUS_MAP["2"].color).toBe("#ff0000");
  });

  it("将未知翻译状态回退为未翻译，并保留原始状态查询", () => {
    expect(getTranslateStateLabel("3")).toBe("已审核");
    expect(getTranslateStateLabel(undefined)).toBe("未翻译");
    expect(getTranslateStateStatus("0")).toEqual(
      TRANSLATE_STATE_STATUS_MAP["0"],
    );
    expect(getTranslateStateStatus("unknown")).toBeUndefined();
  });

  it("为状态下拉提供原有字符串值、顺序和可选禁用状态", () => {
    expect(getEntryStateOptions()).toEqual([
      { label: "新建", value: "0" },
      { label: "审核中", value: "1" },
      { label: "审核不通过", value: "2" },
      { label: "已审核", value: "3" },
    ]);
    expect(getEntryStateOptions(true).at(-1)).toEqual({
      label: "禁用",
      value: "-1",
    });
    expect(getTranslateStateOptions()).toEqual([
      { label: "未翻译", value: "0" },
      { label: "待审核", value: "1" },
      { label: "审核不通过", value: "2" },
      { label: "已审核", value: "3" },
    ]);
  });
});
