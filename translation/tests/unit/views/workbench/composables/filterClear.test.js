import { describe, it, expect, vi } from "vitest";
import {
  saveClearFn,
  resetOnClose,
  handleFilterSearch,
  resetBuiltinColumnFilters,
} from "@/views/workbench/composables/filterClear";
import { selectAllEntry } from "@/utils/selectionUtils";

describe("filterClear composable", () => {
  it("saveClearFn 保存 Ant clearFilters 回调", () => {
    const vm = {};
    const fn = vi.fn();
    saveClearFn(vm, fn);
    expect(vm.antClearFilter).toBe(fn);
  });

  it("resetOnClose 调用 Ant 回调 confirm true 并清空搜索态", () => {
    const antClear = vi.fn();
    const vm = {
      antClearFilter: antClear,
      filters: { isExist: [1] },
      columns: [{ dataIndex: "isExist", filteredValue: [1] }],
      state: { searchText: "x", searchedColumn: "entry" },
    };
    resetOnClose(vm);
    expect(antClear).toHaveBeenCalledWith({ confirm: true });
    expect(vm.state.searchText).toBe("");
    expect(vm.state.searchedColumn).toBe("");
    expect(vm.filters).toBeNull();
    expect(vm.columns[0].filteredValue).toBeNull();
    expect(vm.antClearFilter).toBeNull();
  });

  it("handleFilterSearch 确认搜索并保存 clearFilters", () => {
    const confirm = vi.fn();
    const clearFilters = vi.fn();
    const vm = { state: { searchText: "", searchedColumn: "" } };
    handleFilterSearch(["kw"], confirm, "entry", clearFilters, vm);
    expect(confirm).toHaveBeenCalled();
    expect(vm.state.searchText).toBe("kw");
    expect(vm.state.searchedColumn).toBe("entry");
    expect(vm.antClearFilter).toBe(clearFilters);
  });

  it("resetBuiltinColumnFilters 重置受控 filteredValue", () => {
    const vm = {
      filters: { isExist: [0] },
      columns: [
        { dataIndex: "isExist", filteredValue: [0] },
        { dataIndex: "entry", filteredValue: null },
      ],
    };
    resetBuiltinColumnFilters(vm);
    expect(vm.columns[0].filteredValue).toBeNull();
    expect(vm.filters).toBeNull();
  });

  it("有筛选时 selectAllEntry 仍用 AND 条件", () => {
    const vm = {
      filters: { isExist: [1], entrySource: ["file"] },
      dataSource: [
        { id: 1, isExist: 1, entrySource: "file" },
        { id: 2, isExist: 0, entrySource: "file" },
        { id: 3, isExist: 1, entrySource: "other" },
      ],
      selectedRowKeys: [],
      selectedRows: [],
    };
    selectAllEntry(vm);
    expect(vm.selectedRowKeys).toEqual([1]);
    expect(vm.selectedRows).toHaveLength(1);
  });
});
