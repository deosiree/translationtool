import { describe, expect, it, vi } from "vitest";
import { useSelectionHeaderState } from "@/composables/useSelectionHeaderState";
import { createDeferred } from "../testUtils/deferred";

describe("useSelectionHeaderState", () => {
  it("fast paths the 5381-of-6484 mismatch without fetching IDs", async () => {
    const header = useSelectionHeaderState();
    const fetchAllIds = vi.fn();

    await header.refresh({
      scope: "query-5381-of-6484",
      selectedIds: Array.from({ length: 5381 }, (_, index) => `selected-${index}`)
        .map((id, index) => index === 0 ? "row-1" : id),
      rows: [{ id: "row-1" }],
      total: 6484,
      fetchAllIds,
    });

    expect(header.state.value).toBe("indeterminate");
    expect(fetchAllIds).not.toHaveBeenCalled();
  });

  it("fast paths a partially selected page without fetching IDs", async () => {
    const header = useSelectionHeaderState();
    const fetchAllIds = vi.fn();

    await header.refresh({
      scope: "partial-page",
      selectedIds: ["row-1"],
      rows: [{ id: "row-1" }, { id: "row-2" }],
      total: 100,
      fetchAllIds,
    });

    expect(header.state.value).toBe("indeterminate");
    expect(fetchAllIds).not.toHaveBeenCalled();
  });

  it("compares IDs from one full query and reuses them across pages and selection changes", async () => {
    const header = useSelectionHeaderState();
    const fetchAllIds = vi.fn().mockResolvedValue({
      ids: ["row-1", "row-2", "row-3"],
      total: 3,
    });
    const initial = {
      scope: "query-with-outside-selection",
      selectedIds: ["row-1", "row-2", "outside-query"],
      rows: [{ id: "row-1" }, { id: "row-2" }],
      total: 3,
      fetchAllIds,
    };

    await header.refresh(initial);
    expect(header.state.value).toBe("indeterminate");
    expect(fetchAllIds).toHaveBeenCalledTimes(1);

    await header.refresh({
      ...initial,
      selectedIds: ["row-1", "row-2", "row-3"],
      rows: [{ id: "row-2" }, { id: "row-3" }],
      currentPage: 2,
    });
    expect(header.state.value).toBe("checked");

    await header.refresh({
      ...initial,
      selectedIds: ["outside-query"],
    });
    expect(header.state.value).toBe("unchecked");
    expect(fetchAllIds).toHaveBeenCalledTimes(1);
  });

  it("re-fetches once after an explicitly re-submitted query", async () => {
    const header = useSelectionHeaderState();
    const fetchAllIds = vi.fn().mockResolvedValue({
      ids: ["row-1", "row-2", "row-3"],
      total: 3,
    });
    const query = {
      scope: "same-query",
      selectedIds: ["row-1", "row-2", "row-3"],
      rows: [{ id: "row-1" }, { id: "row-2" }],
      total: 3,
      fetchAllIds,
    };

    await header.refresh(query);
    header.beginQuery(query.scope);
    await header.refresh(query);

    expect(header.state.value).toBe("checked");
    expect(fetchAllIds).toHaveBeenCalledTimes(2);
  });

  it("shares a pending full-ID request and commits only the latest selection", async () => {
    const header = useSelectionHeaderState();
    const deferred = createDeferred();
    const fetchAllIds = vi.fn(() => deferred.promise);
    const first = header.refresh({
      scope: "pending-query",
      selectedIds: ["row-1", "row-2", "row-3"],
      rows: [{ id: "row-1" }, { id: "row-2" }],
      total: 3,
      fetchAllIds,
    });
    const latest = header.refresh({
      scope: "pending-query",
      selectedIds: ["row-1", "row-2", "outside-query"],
      rows: [{ id: "row-1" }, { id: "row-2" }],
      total: 3,
      fetchAllIds,
    });

    expect(header.state.value).toBe("checking");
    expect(fetchAllIds).toHaveBeenCalledTimes(1);
    deferred.resolve({ ids: ["row-1", "row-2", "row-3"], total: 3 });
    await Promise.all([first, latest]);

    expect(header.state.value).toBe("indeterminate");
  });

  it("does not repeat a failed or incomplete full-ID request for the same query", async () => {
    const header = useSelectionHeaderState();
    const fetchAllIds = vi.fn().mockResolvedValue({
      ids: ["row-1", "row-2"],
      total: 3,
    });
    const query = {
      scope: "incomplete-full-query",
      selectedIds: ["row-1", "row-2", "row-3"],
      rows: [{ id: "row-1" }, { id: "row-2" }],
      total: 3,
      fetchAllIds,
    };

    await header.refresh(query);
    await header.refresh({ ...query, selectedIds: ["row-2", "row-3", "outside"] });

    expect(header.state.value).toBe("indeterminate");
    expect(fetchAllIds).toHaveBeenCalledTimes(1);
  });

  it("falls back to indeterminate after a failed full-ID request without retrying the same scope", async () => {
    const header = useSelectionHeaderState();
    const fetchAllIds = vi.fn().mockRejectedValue(new Error("network error"));
    const query = {
      scope: "failed-full-query",
      selectedIds: ["row-1", "row-2", "row-3"],
      rows: [{ id: "row-1" }, { id: "row-2" }],
      total: 3,
      fetchAllIds,
    };

    await header.refresh(query);
    await header.refresh({
      ...query,
      selectedIds: ["row-1", "row-2", "outside-query"],
    });

    expect(header.state.value).toBe("indeterminate");
    expect(fetchAllIds).toHaveBeenCalledTimes(1);
  });

  it("ignores a late full-ID response after the query scope changes", async () => {
    const header = useSelectionHeaderState();
    const deferred = createDeferred();
    const oldCheck = header.refresh({
      scope: "old-scope",
      selectedIds: ["row-1", "row-2", "row-3"],
      rows: [{ id: "row-1" }, { id: "row-2" }],
      total: 3,
      fetchAllIds: () => deferred.promise,
    });

    expect(header.state.value).toBe("checking");
    await header.refresh({
      scope: "new-scope",
      selectedIds: [],
      rows: [{ id: "new-row" }],
      total: 1,
      fetchAllIds: vi.fn(),
    });
    deferred.resolve({ ids: ["row-1", "row-2", "row-3"], total: 3 });
    await oldCheck;

    expect(header.state.value).toBe("unchecked");
    expect(header.isChecking.value).toBe(false);
  });

  it("uses a complete local result without fetching IDs", async () => {
    const header = useSelectionHeaderState();
    const fetchAllIds = vi.fn();
    const rows = [{ id: "row-1" }, { id: "row-2" }];

    await header.refresh({
      scope: "local-result",
      selectedIds: ["row-1", "row-2"],
      rows,
      total: 2,
      isCompleteResult: true,
      fetchAllIds,
    });

    expect(header.state.value).toBe("checked");
    expect(fetchAllIds).not.toHaveBeenCalled();
  });
});
