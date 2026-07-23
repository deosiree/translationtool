import { describe, it, expect } from "vitest";
import { canDeleteAsEntryAuditor } from "@/utils/entryAuditorAuth";

describe("canDeleteAsEntryAuditor", () => {
  const task = { entryAuditor: "zhangsan", id: "t1" };

  it("角色与指派同时满足时返回 true", () => {
    expect(
      canDeleteAsEntryAuditor(
        { userName: "zhangsan", roleName: "词条审核员" },
        task
      )
    ).toBe(true);
  });

  it("roleName 为数组且含词条审核员时返回 true", () => {
    expect(
      canDeleteAsEntryAuditor(
        { userName: "zhangsan", roleName: ["词条审核员", "翻译员"] },
        task
      )
    ).toBe(true);
  });

  it("仅有角色但非本任务指派人返回 false", () => {
    expect(
      canDeleteAsEntryAuditor(
        { userName: "lisi", roleName: "词条审核员" },
        task
      )
    ).toBe(false);
  });

  it("是指派人但无词条审核员角色返回 false", () => {
    expect(
      canDeleteAsEntryAuditor(
        { userName: "zhangsan", roleName: "翻译员" },
        task
      )
    ).toBe(false);
  });

  it("user 或 task 缺失返回 false", () => {
    expect(canDeleteAsEntryAuditor(null, task)).toBe(false);
    expect(canDeleteAsEntryAuditor({ userName: "zhangsan" }, task)).toBe(
      false
    );
    expect(
      canDeleteAsEntryAuditor(
        { userName: "zhangsan", roleName: "词条审核员" },
        null
      )
    ).toBe(false);
  });
});
