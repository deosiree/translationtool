import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import PipelineToolbar from "@/views/workbench/components/PipelineToolbar.vue";

describe("PipelineToolbar", () => {
  const task = {
    name: "任务A",
    productName: "产品B",
    classifyName: "分类C",
    translateType: "英文",
  };

  it("应渲染四项任务信息", () => {
    const wrapper = mount(PipelineToolbar, {
      props: { task },
    });

    const text = wrapper.text();
    expect(text).toContain("任务名称：任务A");
    expect(text).toContain("产品名称：产品B");
    expect(text).toContain("上级分类名称：分类C");
    expect(text).toContain("翻译语种：英文");
  });

  it("应渲染 taskExtra 槽与默认工具栏槽", () => {
    const wrapper = mount(PipelineToolbar, {
      props: { task },
      slots: {
        taskExtra: '<span class="rules-slot">RulesDropdown</span>',
        default: '<button class="query-btn">查询</button>',
      },
      global: {
        stubs: { ColumnActions: true },
      },
    });

    expect(wrapper.find(".rules-slot").exists()).toBe(true);
    expect(wrapper.find(".query-btn").exists()).toBe(true);
  });

  it("trailing 槽时不渲染内联展示列区", () => {
    const wrapper = mount(PipelineToolbar, {
      props: {
        task,
        columnActions: { colPrefName: "colPref-test", modelValue: [], columns: [] },
      },
      slots: {
        trailing: '<span class="trailing-col">展示列</span>',
      },
      global: {
        stubs: { ColumnActions: { template: '<div class="column-actions-stub" />' } },
      },
    });

    expect(wrapper.find(".trailing-col").exists()).toBe(true);
    expect(wrapper.find(".column-actions-stub").exists()).toBe(false);
  });

  it("columnActions prop 时渲染内联展示列", () => {
    const wrapper = mount(PipelineToolbar, {
      props: {
        task,
        columnActions: { colPrefName: "colPref-test", modelValue: [], columns: [] },
      },
      global: {
        stubs: { ColumnActions: { template: '<div class="column-actions-stub" />' } },
      },
    });

    expect(wrapper.find(".column-actions-stub").exists()).toBe(true);
    expect(wrapper.find(".workbench-action-group--offset").exists()).toBe(true);
  });
});
