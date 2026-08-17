import { describe, it, expect, afterEach } from "vitest";
import { mount } from "@vue/test-utils";
import TextAreaIME from "@/components/cellEditor/textarea_IME.vue";

const TextareaStub = {
  name: "ATextarea",
  props: ["value", "autoSize"],
  inheritAttrs: false,
  template: `<textarea class="ant-input" :style="$attrs.style" :data-auto-size="autoSizeJson" />`,
  computed: {
    autoSizeJson() {
      return JSON.stringify(this.autoSize);
    },
  },
};

describe("TextAreaIME", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  it("默认启用 autoSize minRows: 1", () => {
    wrapper = mount(TextAreaIME, {
      props: { value: "hello" },
      global: {
        stubs: { "a-textarea": TextareaStub },
      },
    });
    const ta = wrapper.find("textarea");
    expect(ta.attributes("data-auto-size")).toBe('{"minRows":1}');
    const style = ta.attributes("style") || "";
    expect(style).toContain("width");
    expect(style).toContain("2px");
    expect(style).not.toContain("-5px");
    expect(style).not.toContain("max-height");
  });

  it("传入 autoSize maxRows 时正确传给 a-textarea", () => {
    wrapper = mount(TextAreaIME, {
      props: {
        value: "long text",
        autoSize: { minRows: 1, maxRows: 5 },
      },
      global: {
        stubs: { "a-textarea": TextareaStub },
      },
    });
    const ta = wrapper.find("textarea");
    expect(ta.attributes("data-auto-size")).toBe('{"minRows":1,"maxRows":5}');
  });

  it("IME 组合输入期间外部 value 变更不打断", async () => {
    wrapper = mount(TextAreaIME, {
      props: { value: "a" },
      global: {
        stubs: { "a-textarea": TextareaStub },
      },
    });
    wrapper.vm.onCompositionStart();
    await wrapper.setProps({ value: "external" });
    expect(wrapper.vm.innerValue).toBe("a");
    wrapper.vm.onCompositionEnd({ target: { value: "abc" } });
    expect(wrapper.emitted("update:value")?.pop()).toEqual(["abc"]);
  });
});
