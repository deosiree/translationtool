import { describe, it, expect, afterEach } from "vitest";
import { mount } from "@vue/test-utils";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip/CellOverflowTooltip.vue";

const tooltipStub = {
  name: "TooltipStub",
  props: ["title"],
  template: '<div class="tooltip-stub"><slot /></div>',
};

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
          "a-tooltip": tooltipStub,
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

  it("纯空白内容应按空文本显示", () => {
    wrapper = mountTooltip("   ");

    expect(wrapper.vm.display).toBe("");
    expect(wrapper.element.textContent).toBe("");
  });

  it("词条值为 - 时应原样显示，不与空白混淆", () => {
    wrapper = mountTooltip("-");
    expect(wrapper.text()).toBe("-");
  });

  it("Tooltip 内容应保留文本首尾空格", () => {
    wrapper = mountTooltip(" 原始词条 ");

    expect(wrapper.vm.tooltipContent).toBe(" 原始词条 ");
    expect(wrapper.element.textContent).toBe(" 原始词条 ");
  });

  it("文本未溢出时不启用 Tooltip 内容", async () => {
    wrapper = mountTooltip("短文本");
    const content = wrapper.find(".cell-overflow-tooltip").element;
    Object.defineProperty(content, "scrollWidth", { configurable: true, value: 100 });
    Object.defineProperty(content, "clientWidth", { configurable: true, value: 100 });

    await wrapper.find(".cell-overflow-tooltip").trigger("mouseenter");

    expect(wrapper.vm.isOverflow).toBe(false);
    expect(wrapper.findComponent({ name: "TooltipStub" }).props("title")).toBeUndefined();
  });

  /**
   * 验证内部文本节点溢出时仍会显示完整文本 Tooltip。
   * @returns {Promise<void>} 断言完成。
   */
  it("内部文本节点溢出时应启用完整文本 Tooltip", async () => {
    wrapper = mount(CellOverflowTooltip, {
      props: { content: "一段超出单元格宽度的长文本" },
      slots: { default: '<span class="text">一段超出单元格宽度的长文本</span>' },
      global: {
        stubs: {
          "a-tooltip": tooltipStub,
        },
      },
    });
    const text = wrapper.find(".text").element;
    Object.defineProperty(text, "scrollWidth", { configurable: true, value: 180 });
    Object.defineProperty(text, "clientWidth", { configurable: true, value: 100 });

    await wrapper.find(".cell-overflow-tooltip").trigger("mouseenter");

    expect(wrapper.vm.isOverflow).toBe(true);
    expect(wrapper.findComponent({ name: "TooltipStub" }).props("title")).toBe("一段超出单元格宽度的长文本");
  });
});
