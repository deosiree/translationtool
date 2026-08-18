import { describe, it, expect, afterEach } from "vitest";
import { mount } from "@vue/test-utils";
import TableCellTextArea from "@/components/table/TableCellTextArea.vue";

const TextareaStub = {
  name: "TextAreaIME",
  props: ["value", "autoSize"],
  template: `<textarea class="ant-input" />`,
};

describe("TableCellTextArea", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  it("不应包裹 a-form / a-form-item", () => {
    wrapper = mount(TableCellTextArea, {
      props: { value: "hello" },
      global: {
        stubs: {
          TextArea: TextareaStub,
          "a-form": { template: "<form><slot /></form>" },
          "a-form-item": { template: "<div><slot /></div>" },
        },
      },
    });

    expect(wrapper.find(".table-cell-editor").exists()).toBe(true);
    expect(wrapper.findComponent({ name: "TextAreaIME" }).exists()).toBe(true);
    expect(wrapper.find("form").exists()).toBe(false);
  });

  it("有 errorMessage 时在下方渲染红字", () => {
    wrapper = mount(TableCellTextArea, {
      props: {
        value: "bad",
        errorMessage: "允许最大字符数为 10",
      },
      global: {
        stubs: { TextArea: TextareaStub },
      },
    });

    const err = wrapper.find(".table-cell-editor-error");
    expect(err.exists()).toBe(true);
    expect(err.text()).toBe("允许最大字符数为 10");
    expect(wrapper.classes()).toContain("table-cell-editor--error");
  });

  it("默认 autoSize 为 minRows:1 maxRows:5", () => {
    wrapper = mount(TableCellTextArea, {
      props: { value: "" },
      global: {
        stubs: { TextArea: TextareaStub },
      },
    });

    expect(wrapper.findComponent({ name: "TextAreaIME" }).props("autoSize")).toEqual({
      minRows: 1,
      maxRows: 5,
    });
  });
});
