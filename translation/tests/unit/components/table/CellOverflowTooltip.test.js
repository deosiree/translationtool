import { describe, it, expect, afterEach } from "vitest";
import { mount } from "@vue/test-utils";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip.vue";

describe("CellOverflowTooltip 空值展示", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  function mountTooltip(content) {
    return mount(CellOverflowTooltip, {
      props: { content },
      global: {
        stubs: {
          "a-tooltip": { template: '<div class="tooltip-stub"><slot /></div>' },
        },
      },
    });
  }

  it("空白内容应显示空串，不用 - 占位", () => {
    wrapper = mountTooltip("");
    expect(wrapper.text()).toBe("");
    wrapper.unmount();
    wrapper = mountTooltip(null);
    expect(wrapper.text()).toBe("");
  });

  it("词条值为 - 时应原样显示，不与空白混淆", () => {
    wrapper = mountTooltip("-");
    expect(wrapper.text()).toBe("-");
  });
});
