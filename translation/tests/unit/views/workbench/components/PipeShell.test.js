import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import PipeShell from "@/views/workbench/components/PipeShell.vue";

const customModalStub = {
  name: "CustomModal",
  props: ["bodyHeight"],
  template: "<div class='custom-modal-stub'><slot /></div>",
};

describe("PipeShell", () => {
  it("为五个流水线阶段提供固定的可用内容高度", () => {
    const wrapper = mount(PipeShell, {
      global: {
        stubs: { CustomModal: customModalStub },
      },
    });

    expect(wrapper.findComponent({ name: "CustomModal" }).props("bodyHeight")).toBe("70vh");
  });
});
