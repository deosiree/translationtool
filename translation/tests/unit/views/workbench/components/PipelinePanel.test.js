import { describe, it, expect, vi } from "vitest";
import { computed, nextTick } from "vue";
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
  name: "ATable",
  props: ["customRow", "columns", "scroll", "pagination", "rowSelection"],
  template: `
    <div class="a-table-stub">
      <slot name="headerCell" :title="'词条'" :column="{ colValue: 'entry' }" />
      <slot name="headerCell" :title="'tag'" :column="{ dataIndex: 'tag' }" />
      <slot name="headerCell" :title="'comment'" :column="{ dataIndex: 'comment' }" />
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
  setup(props) {
    return {
      bodyColumn: computed(() => props.columns?.[0]?.dataIndex || "tag"),
      bodyText: computed(() =>
        props.columns?.[0]?.dataIndex === "isExist" ? "1" : "foo;bar"
      ),
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
        "a-empty": {
          props: ["image"],
          template: "<div class='pipeline-empty-stub' />",
        },
        SearchOutlined: { template: "<span class='search-icon' />" },
        CaretDownOutlined: { template: "<span class='down-icon' />" },
        CaretRightOutlined: { template: "<span class='right-icon' />" },
        CellOverflowTooltip: tooltipStub,
        TableTagCell: {
          name: "TableTagCell",
          props: ["text"],
          template: '<span class="table-tag-cell-stub">{{ text }}</span>',
        },
        TableHeaderHint: {
          props: ['title'],
          template: '<span class="table-header-hint-stub">{{ title }}</span>',
        },
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

  it("tag/comment 列头使用统一提示组件", () => {
    const wrapper = makeWrapper();

    expect(wrapper.findAll(".table-header-hint-stub").map((node) => node.text())).toEqual([
      "tag",
      "comment",
    ]);
  });

  it("存在状态 Tooltip 兼容字符串状态码", () => {
    const wrapper = makeWrapper({
      columns: [{ dataIndex: "isExist", colValue: "isExist", width: 88 }],
      edit: ["isExist"],
      editableData: { 1: { isExist: "1" } },
    });
    const stateTooltip = wrapper
      .findAllComponents(tooltipStub)
      .find((node) => node.find(".exist-badge").exists());

    expect(stateTooltip?.props("content")).toBe("已存在");
  });

  it("edit 包含 tag 且处于编辑态时渲染输入控件", () => {
    const wrapper = makeWrapper({
      edit: ["tag"],
      editableData: { 1: { tag: "foo;bar" } },
      cellErrors: {},
    });

    expect(wrapper.find(".ime-stub").exists()).toBe(true);
    expect(wrapper.findComponent({ name: "TableTagCell" }).exists()).toBe(false);
  });

  it("tag 浏览态复用共享 TableTagCell", () => {
    const wrapper = makeWrapper({
      columns: [{ dataIndex: "tag", colValue: "tag", width: 128 }],
      edit: [],
      editableData: {},
    });

    const tagCell = wrapper.findComponent({ name: "TableTagCell" });

    expect(tagCell.exists()).toBe(true);
    expect(tagCell.props("text")).toBe("foo;bar");
  });

  it("任意动态文本列浏览态统一使用 Tooltip", () => {
    const wrapper = makeWrapper({
      task: {
        name: "任务A",
        productName: "产品B",
        classifyName: "分类C",
        translateType: "英文",
        transMap: {
          value: "english",
          interpretation: "englishInterpretation",
          auditSuggest: "englishAuditSuggest",
        },
      },
      columns: [{ dataIndex: "englishInterpretation", colValue: "note", width: 180 }],
      edit: [],
      editableData: {},
    });

    const textTooltip = wrapper
      .findAllComponents(tooltipStub)
      .find((node) => node.props("content") === "foo;bar");
    expect(textTooltip?.exists()).toBe(true);
  });

  it("动态文本列编辑态仍显示编辑器而不显示浏览 Tooltip", () => {
    const wrapper = makeWrapper({
      task: {
        name: "任务A",
        productName: "产品B",
        classifyName: "分类C",
        translateType: "英文",
        transMap: { value: "english" },
      },
      columns: [{ dataIndex: "english", colValue: "translate", width: 260 }],
      edit: ["english"],
      editableData: { 1: { english: "edited" } },
    });

    expect(wrapper.find(".textarea-stub").exists()).toBe(true);
    expect(
      wrapper
        .findAllComponents(tooltipStub)
        .some((node) => node.props("content") === "foo;bar")
    ).toBe(false);
  });

  it("列宽拖拽状态与排序 click 由共享 TableViewport 处理", () => {
    const wrapper = makeWrapper();
    const viewport = wrapper.findComponent({ name: "TableViewport" });

    expect(viewport.exists()).toBe(true);
    expect(wrapper.classes()).not.toContain("pipeline-panel--resizing");
  });

  /**
   * 验证共享面板在 flex-start 父容器中仍强制占满模态内容宽度。
   * @returns {Promise<void>} 样式契约断言完成。
   */
  it("在非 stretch 父容器中保持面板宽度自适应", async () => {
    const fs = await import("node:fs");
    const path = await import("node:path");
    const source = fs.readFileSync(
      path.resolve(__dirname, "../../../../../src/views/workbench/components/PipelinePanel.vue"),
      "utf8"
    );
    const rule = source.match(/\.pipeline-panel\s*\{([\s\S]*?)\n\}/)?.[1] || "";

    expect(rule).toMatch(/box-sizing:\s*border-box/);
    expect(rule).toMatch(/align-self:\s*stretch/);
    expect(rule).toMatch(/width:\s*100%/);
    expect(rule).toMatch(/max-width:\s*100%/);
    expect(rule).toMatch(/min-width:\s*0/);
  });

  it("使用填满模式的共享视口，并在空数据时渲染居中空态", () => {
    const wrapper = makeWrapper({
      columns: [{ title: "词条", dataIndex: "entry", colValue: "entry", width: 220 }],
      scroll: { x: "max-content", y: 415 },
    });
    const viewport = wrapper.findComponent({ name: "TableViewport" });
    const table = wrapper.findComponent({ name: "ATable" });

    expect(viewport.exists()).toBe(true);
    expect(wrapper.classes()).toContain("pipeline-panel");
    expect(viewport.props("fillHeight")).toBe(true);
    expect(viewport.props("emptyOverlay")).toBe(true);
    expect(viewport.props("scroll")).toEqual({ x: "max-content", y: 415 });
    expect(wrapper.find(".pipeline-empty-stub").exists()).toBe(true);
    expect(table.props("scroll")).toEqual({ x: 268 });
    expect(table.props("columns")[0].width).toBe(220);
  });

  /**
   * 验证共享视口收到随任务和列布局变化的稳定布局键。
   * @returns {Promise<void>} 断言完成。
   */
  it("列数组重建时更新表格布局键", async () => {
    const wrapper = makeWrapper({
      task: { id: "task-a", name: "任务A" },
      columns: [{ title: "词条", dataIndex: "entry", colValue: "entry", width: 220 }],
    });
    const viewport = wrapper.findComponent({ name: "TableViewport" });
    const firstKey = viewport.props("layoutKey");

    await wrapper.setProps({
      columns: [{ title: "词条", dataIndex: "entry", colValue: "entry", width: 220 }],
    });

    expect(viewport.props("layoutKey")).not.toBe(firstKey);
  });
});
