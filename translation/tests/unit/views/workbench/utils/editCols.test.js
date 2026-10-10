import { describe, it, expect } from "vitest";
import { editTextCols } from "@/views/workbench/utils/editCols";

describe("editCols", () => {
  const vm = {
    editList_needValidate: ["en"],
    editList: ["interpretation", "comment", "maxLength", "tag"],
  };

  it("withEditList true 合并 editList 并排除专用列", () => {
    expect(editTextCols(vm, { withEditList: true })).toEqual([
      "en",
      "interpretation",
      "comment",
    ]);
  });

  it("withEditList false 仅 editList_needValidate（translate 阶段）", () => {
    expect(editTextCols(vm, { withEditList: false })).toEqual(["en"]);
  });
});
