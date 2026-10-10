import { describe, it, expect, afterEach } from "vitest";
import { mount } from "@vue/test-utils";
import InputIME from "@/components/cellEditor/input_IME.vue";

const InputStub = {
  name: "AInput",
  props: ["value"],
  inheritAttrs: false,
  template: `<input class="ant-input" />`,
};

describe("InputIME", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  it("innerValue 变化时立即 emit update:value", async () => {
    wrapper = mount(InputIME, {
      props: { value: "" },
      global: {
        stubs: { "a-input": InputStub },
      },
    });
    wrapper.vm.innerValue = "tag1";
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("update:value")?.slice(-1)[0]).toEqual(["tag1"]);
  });

  it("组合输入结束后经 innerValue watch emit", async () => {
    wrapper = mount(InputIME, {
      props: { value: "" },
      global: {
        stubs: { "a-input": InputStub },
      },
    });
    wrapper.vm.onCompositionStart();
    wrapper.vm.onCompositionEnd({ target: { value: "中文" } });
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("update:value")?.slice(-1)[0]).toEqual(["中文"]);
  });

  it("组字期间已写入终值时 compositionend 仍强制 emit", async () => {
    wrapper = mount(InputIME, {
      props: { value: "" },
      global: {
        stubs: { "a-input": InputStub },
      },
    });
    const full = "而愤然    愤愤然";
    wrapper.vm.onCompositionStart();
    wrapper.vm.innerValue = full;
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("update:value")).toBeUndefined();

    wrapper.vm.onCompositionEnd({ target: { value: full } });
    await wrapper.vm.$nextTick();
    expect(wrapper.emitted("update:value")?.slice(-1)[0]).toEqual([full]);
  });

  it("不再提供 onBlur 同步", () => {
    wrapper = mount(InputIME, {
      props: { value: "a" },
      global: {
        stubs: { "a-input": InputStub },
      },
    });
    expect(wrapper.vm.onBlur).toBeUndefined();
  });
});
