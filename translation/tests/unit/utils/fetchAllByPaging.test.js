import { describe, it, expect, vi } from "vitest";
import {
  calcPageCount,
  fetchAllByPaging,
} from "@/utils/fetchAllByPaging.js";

describe("calcPageCount", () => {
  it("正常整除与进位", () => {
    expect(calcPageCount(100, 100)).toBe(1);
    expect(calcPageCount(101, 100)).toBe(2);
    expect(calcPageCount(0, 100)).toBe(0);
  });

  it("非法入参返回 0", () => {
    expect(calcPageCount(-1, 100)).toBe(0);
    expect(calcPageCount(10, 0)).toBe(0);
  });
});

describe("fetchAllByPaging", () => {
  it("total=0 返回空或仅第一页空列表", async () => {
    const fetchPage = vi.fn().mockResolvedValue({ list: [], total: 0 });
    const rows = await fetchAllByPaging(fetchPage, { pageSize: 100 });
    expect(rows).toEqual([]);
    expect(fetchPage).toHaveBeenCalledTimes(1);
    expect(fetchPage).toHaveBeenCalledWith(1, 100);
  });

  it("单页数据不再请求后续页", async () => {
    const list = [{ id: 1 }, { id: 2 }];
    const fetchPage = vi.fn().mockResolvedValue({ list, total: 2 });
    const rows = await fetchAllByPaging(fetchPage, { pageSize: 100 });
    expect(rows).toEqual(list);
    expect(fetchPage).toHaveBeenCalledTimes(1);
  });

  it("多页顺序拉取并按 id 去重", async () => {
    const fetchPage = vi.fn(async (page) => {
      if (page === 1) {
        return {
          list: [{ id: 1, n: "a" }, { id: 2, n: "b" }],
          total: 3,
        };
      }
      return {
        list: [{ id: 2, n: "b2" }, { id: 3, n: "c" }],
        total: 3,
      };
    });
    const rows = await fetchAllByPaging(fetchPage, { pageSize: 2 });
    expect(fetchPage).toHaveBeenCalledTimes(2);
    expect(fetchPage.mock.calls[0]).toEqual([1, 2]);
    expect(fetchPage.mock.calls[1]).toEqual([2, 2]);
    expect(rows.map((r) => r.id)).toEqual([1, 2, 3]);
    expect(rows.find((r) => r.id === 2).n).toBe("b2");
  });

  it("fetchPage 非函数抛错", async () => {
    await expect(fetchAllByPaging(null)).rejects.toThrow(/须为函数/);
  });
});
