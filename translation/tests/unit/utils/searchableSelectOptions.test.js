import { describe, it, expect } from "vitest";
import {
  appendTypedOption,
  clonePresetOptions,
} from "@/utils/searchableSelectOptions";

describe("searchableSelectOptions", () => {
  const presets = [
    { label: "全部中文", value: "^[\\x{4e00}-\\x{9fff}]+$" },
    { label: "包含英文", value: "[A-Za-z]" },
  ];

  it("clonePresetOptions 深拷贝且不共享引用", () => {
    const cloned = clonePresetOptions(presets);
    expect(cloned).toEqual(presets);
    expect(cloned).not.toBe(presets);
    expect(cloned[0]).not.toBe(presets[0]);
  });

  it("appendTypedOption 手写项 label===value", () => {
    const out = appendTypedOption(presets, "[0-9]+");
    expect(out).toHaveLength(3);
    expect(out[2]).toEqual({ label: "[0-9]+", value: "[0-9]+" });
  });

  it("appendTypedOption 空串不追加", () => {
    expect(appendTypedOption(presets, "")).toEqual(presets);
    expect(appendTypedOption(presets, null)).toEqual(presets);
  });

  it("appendTypedOption 已存在 value 不重复追加", () => {
    const out = appendTypedOption(presets, "[A-Za-z]");
    expect(out).toHaveLength(2);
  });
});
