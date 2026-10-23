import { describe, expect, it } from "vitest";
import {
  applyColumnLayout,
  getAdaptiveColumnKeys,
  getColumnLayout,
} from "@/components/table/columnLayout.js";

describe("columnLayout", () => {
  it("按字段语义返回统一宽度、对齐和自适应配置", () => {
    expect(getColumnLayout("english")).toMatchObject({
      width: 260,
      minWidth: 50,
      align: "left",
      adaptive: true,
    });
    expect(getColumnLayout("entryAuditor")).toMatchObject({
      width: 96,
      align: "center",
    });
  });

  it("允许页面覆盖语义档位但保留其他默认属性", () => {
    expect(applyColumnLayout({ title: "备注" }, "comment", { width: 240 })).toMatchObject({
      title: "备注",
      width: 240,
      minWidth: 50,
      align: "center",
      adaptive: true,
    });
  });

  it("为 abbr 与 Comment 维持各自的统一对齐方式", () => {
    expect(getColumnLayout("abbr").align).toBe("left");
    expect(getColumnLayout("comment").align).toBe("center");
  });

  it("从分组列提取自适应叶子列并去重", () => {
    expect(
      getAdaptiveColumnKeys([
        { key: "group", children: [{ dataIndex: "entry", adaptive: true }] },
        { colValue: "entry", adaptive: true },
        { dataIndex: "state", adaptive: false },
      ])
    ).toEqual(["entry"]);
  });
});
