import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import SpanByTipsFill from "@/components/SpanByTips/SpanByTipsFill/index.vue";

const mountSpanByTipsFill = (props) =>
  mount(SpanByTipsFill, {
    props,
    global: {
      stubs: {
        "a-tooltip": {
          template: "<div><slot /></div>",
        },
      },
    },
  });

describe("SpanByTipsFill", () => {
  it("有内容时渲染全文", () => {
    const wrapper = mountSpanByTipsFill({ content: "Agent 预填审核意见" });

    expect(wrapper.find(".span-by-tips-fill").text()).toBe("Agent 预填审核意见");
  });

  it("空内容时显示占位符", () => {
    const wrapper = mountSpanByTipsFill({ content: "" });

    expect(wrapper.find(".span-by-tips-fill").text()).toBe("-");
  });

  it("仅空白时显示占位符", () => {
    const wrapper = mountSpanByTipsFill({ content: "   " });

    expect(wrapper.find(".span-by-tips-fill").text()).toBe("-");
  });

  it("支持自定义 emptyText", () => {
    const wrapper = mountSpanByTipsFill({ content: "", emptyText: "无" });

    expect(wrapper.find(".span-by-tips-fill").text()).toBe("无");
  });

  it("默认 maxWidth 为 100px", () => {
    const wrapper = mountSpanByTipsFill({ content: "短文本" });

    const host = wrapper.find(".span-by-tips-fill-host");
    expect(host.attributes("style")).toContain("max-width: 100px");
  });

  it("显式 maxWidth 覆盖默认值", () => {
    const wrapper = mountSpanByTipsFill({
      content: "长审核意见",
      maxWidth: "160px",
    });

    const host = wrapper.find(".span-by-tips-fill-host");
    expect(host.attributes("style")).toContain("max-width: 160px");
  });
});
