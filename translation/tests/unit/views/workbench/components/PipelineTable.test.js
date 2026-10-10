import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import PipelineTable from "@/views/workbench/components/PipelineTable.vue";

const inputStub = {
  props: ["placeholder", "value"],
  template: `<input class="filter-input" @change="$emit('change', $event)" @keyup.enter="$emit('pressEnter')" />`,
};
const buttonStub = {
  template: `<button class="filter-btn" @click="$emit('click')"><slot /><slot name="icon" /></button>`,
};
const tableStub = {
  template: `
    <div class="a-table-stub">
      <slot name="headerCell" :title="'词条'" :column="{ colValue: 'entry' }" />
      <slot
        name="customFilterDropdown"
        :selectedKeys="['kw']"
        :setSelectedKeys="setSelectedKeys"
        :confirm="confirm"
        :clearFilters="clearFilters"
        :column="{ title: '词条', dataIndex: 'entry' }"
      />
      <slot name="customFilterIcon" :filtered="true" />
      <slot
        name="expandIcon"
        :record="{ id: 1, children: [{ id: 2 }] }"
        :expanded="false"
        :onExpand="onExpand"
      />
      <slot name="bodyCell" :column="{ dataIndex: 'entry' }" text="x" :record="{ id: 1 }" />
    </div>
  `,
  setup() {
    return {
      setSelectedKeys: vi.fn(),
      confirm: vi.fn(),
      clearFilters: vi.fn(),
      onExpand: vi.fn(),
    };
  },
};

describe("PipelineTable", () => {
  it("模块求值时 SearchOutlined 与 Caret 图标已注册", () => {
    expect(PipelineTable.components.SearchOutlined).toBeTruthy();
    expect(PipelineTable.components.CaretDownOutlined).toBeTruthy();
    expect(PipelineTable.components.CaretRightOutlined).toBeTruthy();
  });

  it("filterSearch 转发 selectedKeys", async () => {
    const wrapper = mount(PipelineTable, {
      props: { columns: [], dataSource: [] },
      global: {
        stubs: {
          "a-table": tableStub,
          "a-input": inputStub,
          "a-button": buttonStub,
          SearchOutlined: { template: "<span />" },
          CellOverflowTooltip: { template: "<span><slot /></span>" },
        },
      },
    });
    await wrapper.findAll(".filter-btn")[0].trigger("click");
    expect(wrapper.emitted("filterSearch")[0][0]).toEqual(["kw"]);
  });

  it("filterReset 转发 clearFilters", async () => {
    const wrapper = mount(PipelineTable, {
      props: { columns: [], dataSource: [] },
      global: {
        stubs: {
          "a-table": tableStub,
          "a-input": inputStub,
          "a-button": buttonStub,
          SearchOutlined: { template: "<span />" },
          CellOverflowTooltip: { template: "<span><slot /></span>" },
        },
      },
    });
    await wrapper.findAll(".filter-btn")[1].trigger("click");
    expect(wrapper.emitted("filterReset")?.[0]).toBeTruthy();
  });

  it("showExpandIcon 为 false 时不渲染 Caret 图标", () => {
    const wrapper = mount(PipelineTable, {
      props: { columns: [], dataSource: [], showExpandIcon: false },
      global: {
        stubs: {
          "a-table": tableStub,
          CellOverflowTooltip: { template: "<span><slot /></span>" },
        },
      },
    });
    expect(wrapper.findComponent({ name: "CaretRightOutlined" }).exists()).toBe(
      false
    );
  });

  it("未 stub 时 customFilterIcon 能解析 SearchOutlined", () => {
    const wrapper = mount(PipelineTable, {
      props: { columns: [], dataSource: [] },
      global: {
        stubs: {
          "a-table": tableStub,
          "a-input": inputStub,
          "a-button": buttonStub,
          CellOverflowTooltip: { template: "<span><slot /></span>" },
        },
      },
    });
    expect(wrapper.findComponent({ name: "SearchOutlined" }).exists()).toBe(true);
  });
});
