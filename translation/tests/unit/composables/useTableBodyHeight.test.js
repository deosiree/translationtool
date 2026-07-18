import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { ref, nextTick, defineComponent, h } from "vue";
import { mount, flushPromises } from "@vue/test-utils";
import { useTableBodyHeight } from "@/composables/useTableBodyHeight.js";

describe("useTableBodyHeight", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "ResizeObserver",
      class {
        observe() {}
        disconnect() {}
      }
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("subtractHeader 时 scroll.y = 容器高 - 表头", async () => {
    const Host = defineComponent({
      setup() {
        const tableWrapperRef = ref(null);
        const { tableBodyHeight, tableScrollY, syncHeight } = useTableBodyHeight(
          tableWrapperRef,
          { subtractHeader: true, minHeight: 50, headerFallback: 40 }
        );
        return { tableWrapperRef, tableBodyHeight, tableScrollY, syncHeight };
      },
      render() {
        return h(
          "div",
          {
            ref: "tableWrapperRef",
            style: { height: "300px" },
            class: "table-wrapper",
          },
          [h("div", { class: "ant-table-header", style: { height: "39px" } })]
        );
      },
    });

    const wrapper = mount(Host, { attachTo: document.body });
    await nextTick();
    await flushPromises();

    // jsdom 不计算 layout，手动 mock clientHeight / offsetHeight
    const el = wrapper.vm.tableWrapperRef;
    Object.defineProperty(el, "clientHeight", { value: 300, configurable: true });
    const header = el.querySelector(".ant-table-header");
    Object.defineProperty(header, "offsetHeight", { value: 39, configurable: true });

    wrapper.vm.syncHeight();
    await nextTick();

    expect(wrapper.vm.tableBodyHeight).toBe(300);
    expect(wrapper.vm.tableScrollY).toBe(261);
    wrapper.unmount();
  });
});
