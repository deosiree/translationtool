import { describe, it, expect, vi } from "vitest";
import { mount } from "@vue/test-utils";
import PipelinePanel from "@/views/workbench/components/PipelinePanel.vue";

const inputStub = {
  props: ["placeholder", "value"],
  template:
    '<input class="filter-input" :value="value" @change="$emit(\'change\', $event)" @keyup.enter="$emit(\'pressEnter\')" />',
};

const buttonStub = {
  template:
    '<button class="filter-btn" @click="$emit(\'click\')"><slot /><slot name="icon" /></button>',
};

const tooltipStub = {
  props: ["content"],
  template: '<span class="tooltip-stub"><slot>{{ content }}</slot></span>',
};

const tableStub = {
  props: ["customRow"],
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
      <slot name="bodyCell" :column="{ dataIndex: bodyColumn }" :text="bodyText" :record="bodyRecord" />
    </div>
  `,
  setup() {
    return {
      bodyColumn: "tag",
      bodyText: "foo;bar",
      bodyRecord: { id: 1, auditState: 1, children: [{ id: 2 }] },
      setSelectedKeys: vi.fn(),
      confirm: vi.fn(),
      clearFilters: vi.fn(),
      onExpand: vi.fn(),
    };
  },
};

const makeWrapper = (props = {}, slots = {}) =>
  mount(PipelinePanel, {
    props: {
      task: {
        name: "任务A",
        productName: "产品B",
        classifyName: "分类C",
        translateType: "英文",
      },
      tableHost: {
        state: { searchText: "", searchedColumn: "" },
        selectedRowIndex: null,
        columns: [],
        filters: null,
        antClearFilter: null,
        dataSource: [{ id: 1, isExist: 1, entrySource: "A" }],
      },
      columns: [],
      dataSource: [],
      loading: false,
      pagination: {},
      scroll: {},
      rowSelection: {},
      ...props,
    },
    slots,
    global: {
      stubs: {
        "a-table": tableStub,
        "a-input": inputStub,
        "a-button": buttonStub,
        "a-input-number": {
          template: '<input class="number-input" @change="$emit(\'change\', $event.target.value)" />',
        },
        "a-tooltip": { template: "<span><slot /><slot name='title' /></span>" },
        "a-tag": { template: "<span class='tag-stub'><slot /></span>" },
        SearchOutlined: { template: "<span class='search-icon' />" },
        CaretDownOutlined: { template: "<span class='down-icon' />" },
        CaretRightOutlined: { template: "<span class='right-icon' />" },
        CellOverflowTooltip: tooltipStub,
        ColumnActions: { template: "<div class='column-actions-stub' />" },
        TableCellTextArea: {
          props: ["value", "errorMessage"],
          template:
            '<textarea class="textarea-stub" @update:value="$emit(\'update:value\', \'next\')" />',
        },
        InputIME: {
          props: ["value"],
          template: '<input class="ime-stub" @update:value="$emit(\'update:value\', \'next\')" />',
        },
        IsExistBadge: { template: "<span class='exist-badge' />" },
        EntryStateBadge: { template: "<span class='entry-badge' />" },
        TransStateBadge: { template: "<span class='translate-badge' />" },
        AuditTags: {
          template:
            '<div class="audit-tags"><button class="audit-pass" @click="$emit(\'pass\')"></button><button class="audit-reject" @click="$emit(\'reject\')"></button></div>',
        },
      },
    },
  });

describe("PipelinePanel", () => {
  it("合并渲染任务信息、默认槽与内联展示列", () => {
    const wrapper = makeWrapper(
      {
        columnActions: { colPrefName: "colPref-test", modelValue: [], columns: [] },
      },
      {
        taskExtra: '<span class="rules-slot">RulesDropdown</span>',
        default: '<button class="query-btn">查询</button>',
      }
    );

    expect(wrapper.text()).toContain("任务名称：任务A");
    expect(wrapper.find(".rules-slot").exists()).toBe(true);
    expect(wrapper.find(".query-btn").exists()).toBe(true);
    expect(wrapper.find(".column-actions-stub").exists()).toBe(true);
  });

  it("转发表头搜索与重置事件", async () => {
    const wrapper = makeWrapper();
    const buttons = wrapper.findAll(".filter-btn");

    await buttons[0].trigger("click");
    await buttons[1].trigger("click");

    expect(wrapper.emitted("filterSearch")?.[0]?.[0]).toEqual(["kw"]);
    expect(wrapper.emitted("filterReset")?.[0]).toBeTruthy();
  });

  it("edit 包含 tag 且处于编辑态时渲染输入控件", () => {
    const wrapper = makeWrapper({
      edit: ["tag"],
      editableData: { 1: { tag: "foo;bar" } },
      cellErrors: {},
    });

    expect(wrapper.find(".ime-stub").exists()).toBe(true);
  });
});
