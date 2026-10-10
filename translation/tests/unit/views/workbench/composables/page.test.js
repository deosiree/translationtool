import { describe, it, expect, vi, beforeEach } from "vitest";
import { defaultPagination, pageChange } from "@/views/workbench/composables/page";

vi.mock("@/utils/validationUtils", () => ({
  verifyArray_workbench_page: vi.fn(),
}));

import { verifyArray_workbench_page } from "@/utils/validationUtils";

describe("page composable", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("defaultPagination 返回标准分页配置", () => {
    const onChange = vi.fn();
    const p = defaultPagination(onChange);
    expect(p.pageSize).toBe(20);
    expect(p.onChange).toBe(onChange);
    expect(p.showTotal(5)).toBe("共 5 条");
  });

  it("pageChange 更新分页并调用 verifyArray_workbench_page", () => {
    const vm = {
      pagination: { current: 1, pageSize: 20 },
    };
    pageChange(vm, 2, 50, () => "en");
    expect(vm.pagination.current).toBe(2);
    expect(vm.pagination.pageSize).toBe(50);
    expect(verifyArray_workbench_page).toHaveBeenCalledWith(
      vm.pagination,
      "en",
      vm
    );
  });
});
