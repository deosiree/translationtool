import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import SelectionToolbar from "@/components/selectToolBar/SelectionToolbar.vue";
import SelectionToolbarButton from "@/components/selectToolBar/SelectionToolbarButton.vue";
import { useSelectedEntries } from "@/components/selectToolBar/composables/useSelectedEntries";
import { useSelectionOperations } from "@/components/selectToolBar/composables/useSelectionOperations";
import { useSelectionToolbarState } from "@/components/selectToolBar/composables/useSelectionToolbarState";

const toolbarButtonStubs = {
  "a-button": {
    name: "AButton",
    props: ["type", "size", "disabled"],
    template:
      '<button :data-type="type" :data-size="size" :disabled="disabled" v-bind="$attrs"><slot /></button>',
  },
  "a-badge": {
    name: "ABadge",
    props: ["count", "overflowCount"],
    template: '<span class="badge-stub" :data-count="count"><slot /></span>',
  },
  LoadingOutlined: { template: '<i class="spinner-stub" />' },
};

/**
 * Mount the shared toolbar button with stable Ant Design component stubs.
 * @param {Object} props - Button props for the current case.
 * @returns {import('@vue/test-utils').VueWrapper} Mounted toolbar button.
 */
function mountButton(props) {
  return mount(SelectionToolbarButton, {
    props,
    global: { stubs: toolbarButtonStubs },
  });
}

describe("selectToolBar shared components", () => {
  it.each([
    ["primary", "primary", ""],
    ["default", "default", ""],
    ["text", "text", ""],
    ["selected", "primary", "resetBtn"],
    ["warning", "primary", "yellowBtn"],
  ])("renders the %s preset with shared type and color", (preset, buttonType, buttonClass) => {
    const wrapper = mountButton({ text: "操作", preset });
    const button = wrapper.find("button");

    expect(button.attributes("data-type")).toBe(buttonType);
    expect(button.attributes("data-size")).toBe("small");
    if (buttonClass) expect(button.classes()).toContain(buttonClass);
  });

  it("keeps label width content while showing a centered loading overlay and hiding the badge", async () => {
    const wrapper = mountButton({
      text: "已选词条",
      badgeCount: 23,
      loading: true,
    });

    expect(wrapper.find("button").element.disabled).toBe(true);
    expect(wrapper.find(".badge-stub").attributes("data-count")).toBe("0");
    expect(wrapper.find(".select-toolbar-button-content").text()).toBe("已选词条");
    expect(wrapper.find(".select-toolbar-button-content").classes()).toContain(
      "select-toolbar-button-content-hidden"
    );
    expect(wrapper.find(".spinner-stub").exists()).toBe(true);

    await wrapper.setProps({ loading: false });

    expect(wrapper.find("button").element.disabled).toBe(false);
    expect(wrapper.find(".badge-stub").attributes("data-count")).toBe("23");
    expect(wrapper.find(".select-toolbar-button-content").classes()).not.toContain(
      "select-toolbar-button-content-hidden"
    );
    expect(wrapper.find(".spinner-stub").exists()).toBe(false);
  });

  it("forwards click events and renders toolbar slot order", async () => {
    const button = mountButton({ text: "已选词条" });
    await button.find("button").trigger("click");
    expect(button.emitted("click")).toHaveLength(1);

    const toolbar = mount(SelectionToolbar, {
      slots: {
        default: '<button>已选词条</button><button>取消选择</button>',
      },
    });
    expect(toolbar.findAll("button").map((item) => item.text())).toEqual([
      "已选词条",
      "取消选择",
    ]);
  });
});

describe("selectToolBar selection composables", () => {
  it("deduplicates by ID, refreshes objects, removes IDs and clears the list", () => {
    const entries = useSelectedEntries();
    const selected = entries.merge(
      [{ id: "a", value: "old" }, { id: "b" }],
      [{ id: "a", value: "new" }, { id: "c" }]
    );

    expect(selected).toEqual([
      { id: "a", value: "new" },
      { id: "b" },
      { id: "c" },
    ]);
    expect(entries.update(selected, { id: "b", value: "updated" })[1]).toEqual({
      id: "b",
      value: "updated",
    });
    expect(entries.remove(selected, ["c"]).map((row) => row.id)).toEqual([
      "a",
      "b",
    ]);
    expect(entries.clear()).toEqual([]);
  });

  it("hides badge count during loading and leaves clear available to cancel work", () => {
    expect(useSelectionToolbarState([{ id: 1 }], true)).toEqual({
      count: 1,
      badgeCount: 0,
      loading: true,
      clearDisabled: false,
    });
    expect(useSelectionToolbarState([], false).clearDisabled).toBe(true);
  });

  it("projects only the current scope and applies the last row operation on resolution", () => {
    const operations = useSelectionOperations();
    const request = operations.begin("scope-a", true);
    expect(operations.project("a", "scope-a", false)).toBe(true);
    expect(operations.project("a", "scope-b", false)).toBe(false);

    operations.record("a", false, "scope-a");
    const rows = operations.resolve(
      request,
      [{ id: "a" }, { id: "b" }],
      [{ id: "outside" }],
      "scope-a"
    );
    expect(rows.map((row) => row.id)).toEqual(["outside", "b"]);
  });

  it("keeps the latest bulk action when overlapping requests resolve in reverse order", () => {
    const operations = useSelectionOperations();
    const select = operations.begin("scope-a", true);
    const deselect = operations.begin("scope-a", false);

    const afterDeselect = operations.resolve(
      deselect,
      [{ id: "shared" }, { id: "only-b" }],
      [],
      "scope-a"
    );
    const afterSelect = operations.resolve(
      select,
      [{ id: "shared" }, { id: "only-a" }],
      afterDeselect,
      "scope-a"
    );

    expect(afterSelect.map((row) => row.id)).toEqual(["only-a"]);
  });

  it("keeps the latest row action across overlapping query scopes", () => {
    const operations = useSelectionOperations();
    const olderDeselect = operations.begin("parent-scope", false);
    const newerSelect = operations.begin("child-scope", true);

    const afterNewer = operations.resolve(
      newerSelect,
      [{ id: "shared" }],
      [],
      "child-scope"
    );
    const afterOlder = operations.resolve(
      olderDeselect,
      [{ id: "shared" }],
      afterNewer,
      "parent-scope"
    );

    expect(afterOlder.map((row) => row.id)).toEqual(["shared"]);
  });

  it("uses a saved row object when an older full-selection response arrives", () => {
    const operations = useSelectionOperations();
    const request = operations.begin("scope", true);
    operations.refresh({ id: "saved", value: "latest" });

    const rows = operations.resolve(
      request,
      [{ id: "saved", value: "stale" }],
      [],
      "scope"
    );

    expect(rows).toEqual([{ id: "saved", value: "latest" }]);
  });

  it("rolls back failed projections and invalidates stale operations", () => {
    const operations = useSelectionOperations();
    const committed = [{ id: "selected" }];
    const failed = operations.begin("scope", false);
    expect(operations.project("selected", "scope", true)).toBe(false);
    operations.reject(failed);
    expect(operations.project("selected", "scope", true)).toBe(true);

    const stale = operations.begin("scope", true);
    operations.invalidate();
    expect(operations.pendingCount).toBe(0);
    expect(operations.resolve(stale, [{ id: "late" }], committed, "scope")).toBe(
      committed
    );
  });
});
