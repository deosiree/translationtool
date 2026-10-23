import { afterEach, describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import TableCellText from "@/components/table/TableCellText.vue";

const tooltipStub = {
  name: "CellOverflowTooltip",
  props: ["content", "emptyText"],
  template: '<span class="tooltip-stub"><slot /></span>',
};

const editorStub = {
  name: "TableCellTextArea",
  props: ["value", "autoSize", "errorMessage"],
  template: "<textarea class=\"editor-stub\" />",
};

describe("TableCellText", () => {
  let wrapper;

  afterEach(() => wrapper?.unmount());

  it("浏览态显示省略 Tooltip，并将双击交给父页面", async () => {
    wrapper = mount(TableCellText, {
      props: { text: "术语文本" },
      global: { stubs: { CellOverflowTooltip: tooltipStub, TableCellTextArea: editorStub } },
    });

    expect(wrapper.findComponent({ name: "CellOverflowTooltip" }).props("content")).toBe("术语文本");
    expect(wrapper.findComponent({ name: "TableCellTextArea" }).exists()).toBe(false);
    await wrapper.find(".table-cell-text").trigger("dblclick");
    expect(wrapper.emitted("dblclick")).toHaveLength(1);
  });

  it("编辑态使用默认五行文本域并向父页面转发文本更新", () => {
    wrapper = mount(TableCellText, {
      props: { text: "术语文本", value: "编辑文本", editing: true },
      global: { stubs: { CellOverflowTooltip: tooltipStub, TableCellTextArea: editorStub } },
    });

    const editor = wrapper.findComponent({ name: "TableCellTextArea" });
    expect(editor.props()).toEqual(expect.objectContaining({
      value: "编辑文本",
      autoSize: { minRows: 1, maxRows: 5 },
    }));
    expect(wrapper.findComponent({ name: "CellOverflowTooltip" }).exists()).toBe(false);
    editor.vm.$emit("update:value", "更新后的文本");
    expect(wrapper.emitted("update:value")).toEqual([["更新后的文本"]]);
  });
});
