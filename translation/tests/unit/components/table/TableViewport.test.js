import { describe, expect, it, vi } from "vitest";
import { mount } from "@vue/test-utils";
import { nextTick } from "vue";
import TableViewport from "@/components/table/TableViewport.vue";
import { Table as AntTable } from "ant-design-vue";

const tableStub = {
  name: "ATable",
  props: ["scroll", "columns", "dataSource", "pagination", "rowSelection"],
  template: '<div class="table-wrapper-stub"><div class="ant-table-content"><table><thead><tr><th>标题</th></tr></thead><tbody><tr class="ant-table-placeholder"><td><div class="ant-empty">原生空态</div></td></tr></tbody></table></div><slot name="bodyCell" /></div>',
};

/**
 * 在共享视口上模拟一次真实的 resize 手势。
 * @param {Object} wrapper TableViewport 测试包装器。
 * @param {Object} column 待调整列。
 * @param {number} width Ant Table 回调返回的目标宽度。
 * @returns {void} 无返回值。
 */
function resizeColumn(wrapper, column, width) {
  const header = {};
  const handle = {
    closest: (selector) => (selector === ".ant-table-resize-handle" ? handle : header),
  };
  wrapper.vm.onResizeStart({ button: 0, clientX: 100, clientY: 0, target: handle });
  wrapper.vm.onResizeMove({ clientX: 110, clientY: 0 });
  wrapper.vm.handleResizeColumn(width, column);
  wrapper.vm.onResizeEnd();
}

function mountViewport(props = {}, options = {}) {
  return mount(TableViewport, {
    props,
    slots: options.slots,
    global: {
      stubs: { "a-table": tableStub },
    },
  });
}

describe("TableViewport", () => {
  it("真实 Ant Table resize handle 会触发共享回调", async () => {
    window.matchMedia = window.matchMedia || (() => ({
      matches: false,
      addListener() {},
      removeListener() {},
      addEventListener() {},
      removeEventListener() {},
    }));
    const wrapper = mount(TableViewport, {
      props: {
        columns: [{ title: "词条", dataIndex: "entry", width: 220, resizable: true }],
        dataSource: [{ key: "1", entry: "text" }],
        pagination: false,
      },
      global: {
        components: { ATable: AntTable },
        stubs: { "a-table": false, "a-spin": false },
      },
    });
    await nextTick();
    const handle = wrapper.find(".ant-table-resize-handle");
    expect(handle.exists()).toBe(true);
    const rect = { width: 220, height: 20, top: 0, left: 0, right: 220, bottom: 20 };
    handle.element.parentNode.getBoundingClientRect = () => rect;
    const downEvent = new MouseEvent("mousedown", { bubbles: true, button: 0, clientX: 100 });
    Object.defineProperty(downEvent, "which", { value: 1 });
    Object.defineProperty(downEvent, "pageX", { value: 100 });
    handle.element.dispatchEvent(downEvent);
    document.documentElement.dispatchEvent(new MouseEvent("mousemove", { bubbles: true, clientX: 120, pageX: 120 }));
    await nextTick();
    document.documentElement.dispatchEvent(new MouseEvent("mouseup", { bubbles: true, clientX: 120, pageX: 120 }));
    await new Promise((resolve) => setTimeout(resolve, 60));
    await nextTick();
    expect(wrapper.vm.tableLayoutState.manualWidths.get("entry")).toBe(240);
    wrapper.unmount();
  });
  it("只向 Ant Table 传递横向滚动配置", () => {
    const wrapper = mountViewport({
      scroll: { x: "max-content", y: 400 },
      height: 320,
    });

    expect(wrapper.findComponent({ name: "ATable" }).props("scroll")).toEqual({
      x: "max-content",
    });
    expect(wrapper.attributes("style")).toContain("--table-viewport-height: 320px");
  });

  it("把旧 scroll.y 转成包含表头和分页的视口高度", () => {
    const wrapper = mountViewport({ scroll: { x: "max-content", y: 400 } });

    expect(wrapper.attributes("style")).toContain("--table-viewport-height: 488px");
    expect(wrapper.findComponent({ name: "ATable" }).props("scroll")).toEqual({
      x: "max-content",
    });
  });

  it("填满模式作为父级弹性剩余区域，不沿用固定视口高度", () => {
    const wrapper = mountViewport({
      fillHeight: true,
      scroll: { x: "max-content", y: 400 },
    });

    expect(wrapper.classes()).toContain("table-viewport-fill-height");
    expect(wrapper.attributes("style")).toContain("--table-viewport-height: auto");
  });

  it("启用空态覆盖层时保留表头高度并在内容区渲染空态插槽", () => {
    const wrapper = mountViewport(
      {
        dataSource: [],
        emptyOverlay: true,
        scroll: { x: "max-content", y: 400 },
      },
      {
        slots: {
          empty: '<span class="custom-empty">暂无数据</span>',
        },
      }
    );

    expect(wrapper.classes()).toContain("table-viewport-empty");
    expect(wrapper.attributes("style")).toContain("--table-empty-body-height: 400px");
    expect(wrapper.find(".table-empty-overlay").text()).toBe("暂无数据");
    expect(wrapper.findComponent({ name: "ATable" }).classes()).toContain(
      "table-native-empty-hidden"
    );
    expect(wrapper.find(".ant-table-placeholder").exists()).toBe(true);
  });

  it("有数据时不渲染 opt-in 空态覆盖层", () => {
    const wrapper = mountViewport({
      dataSource: [{ id: "entry-1" }],
      emptyOverlay: true,
      scroll: { x: "max-content", y: 400 },
    });

    expect(wrapper.classes()).not.toContain("table-viewport-empty");
    expect(wrapper.find(".table-empty-overlay").exists()).toBe(false);
    expect(wrapper.findComponent({ name: "ATable" }).classes()).not.toContain(
      "table-native-empty-hidden"
    );
  });

  it("按剩余宽度扩展自适应列并保留拖拽宽度", async () => {
    const wrapper = mountViewport({
      columns: [
        { title: "序号", dataIndex: "index", width: 100 },
        { title: "词条", dataIndex: "entry", width: 220 },
      ],
      adaptiveColumnKeys: ["entry"],
      rowSelection: {},
    });
    Object.defineProperty(wrapper.find(".ant-table-content").element, "clientWidth", {
      configurable: true,
      value: 600,
    });
    wrapper.vm.syncMetrics();
    await nextTick();

    expect(wrapper.vm.resolvedColumns[1].width).toBe(452);
    expect(wrapper.vm.tableScroll).toEqual({ x: 600 });
    await wrapper.vm.$forceUpdate();
    await nextTick();
    expect(wrapper.findComponent({ name: "ATable" }).props("scroll")).toEqual({
      x: 600,
    });
    resizeColumn(wrapper, wrapper.vm.resolvedColumns[1], 50);
    await nextTick();

    expect(wrapper.vm.resolvedColumns[1].width).toBe(50);
    expect(wrapper.vm.resolvedColumns[1].customCell({}).style).toMatchObject({
      width: "50px",
      minWidth: "50px",
      maxWidth: "50px",
    });
  });

  /**
   * 验证视口不足时自适应列保持原有固定尺寸回调。
   * @returns {Promise<void>} 断言完成。
   */
  it("视口不足时不把自适应列改成流体列", async () => {
    const customCell = () => ({
      style: { width: "260px", minWidth: "260px", maxWidth: "260px" },
    });
    const wrapper = mountViewport({
      columns: [
        { title: "序号", dataIndex: "index", width: 100 },
        { title: "翻译", dataIndex: "english", width: 260, customCell },
      ],
      adaptiveColumnKeys: ["english"],
      rowSelection: {},
    });
    Object.defineProperty(wrapper.find(".ant-table-content").element, "clientWidth", {
      configurable: true,
      value: 400,
    });
    wrapper.vm.syncMetrics();
    await nextTick();

    expect(wrapper.vm.resolvedColumns[1].width).toBe(260);
    expect(wrapper.vm.resolvedColumns[1].customCell({}).style.width).toBe("260px");
    expect(wrapper.findComponent({ name: "ATable" }).props("scroll")).toEqual({
      x: 408,
    });
  });

  it("只从显式 adaptive 列配置推导自适应列", async () => {
    const wrapper = mountViewport({
      adaptiveColumns: true,
      columns: [
        { title: "状态", dataIndex: "state", width: 100 },
        {
          title: "描述",
          dataIndex: "description",
          width: 220,
          resizable: true,
          adaptive: true,
        },
      ],
    });
    Object.defineProperty(wrapper.find(".ant-table-content").element, "clientWidth", {
      configurable: true,
      value: 600,
    });
    wrapper.vm.syncMetrics();
    await nextTick();

    expect(wrapper.vm.resolvedColumns[1].width).toBe(500);
  });

  it("旧页面开启 adaptiveColumns 时仅按长文本语义回退", async () => {
    const wrapper = mountViewport({
      adaptiveColumns: true,
      columns: [
        { title: "状态", dataIndex: "state", width: 100, resizable: true },
        { title: "词条来源", dataIndex: "entrySource", width: 200, resizable: true },
      ],
    });
    Object.defineProperty(wrapper.find(".ant-table-content").element, "clientWidth", {
      configurable: true,
      value: 600,
    });
    wrapper.vm.syncMetrics();
    await nextTick();

    expect(wrapper.vm.resolvedColumns[0].width).toBe(100);
    expect(wrapper.vm.resolvedColumns[1].width).toBe(500);
  });

  it("根据真实滚动容器显示覆盖滚动条并同步滚动位置", async () => {
    const wrapper = mountViewport({ scroll: { x: "max-content" } });
    const target = wrapper.find(".ant-table-content").element;
    Object.defineProperties(target, {
      scrollTop: { value: 40, writable: true },
      scrollLeft: { value: 20, writable: true },
      scrollHeight: { value: 400 },
      scrollWidth: { value: 300 },
      clientHeight: { value: 100 },
      clientWidth: { value: 100 },
    });

    wrapper.vm.scrollTarget = target;
    wrapper.vm.syncMetrics();
    await nextTick();

    expect(wrapper.find(".table-overlay-scrollbar-vertical").exists()).toBe(true);
    expect(wrapper.find(".table-overlay-scrollbar-horizontal").exists()).toBe(true);
    expect(wrapper.vm.metrics.scrollTop).toBe(40);
    expect(wrapper.vm.metrics.scrollLeft).toBe(20);
  });

  /**
   * 验证布局键变化会清理手动宽度并将表格滚动回最左侧。
   * @returns {Promise<void>} 断言完成。
   */
  it("列布局重建后恢复默认宽度和最左侧滚动位置", async () => {
    const wrapper = mountViewport({
      layoutKey: "task-a:0",
      columns: [{ title: "词条", dataIndex: "entry", width: 220 }],
      adaptiveColumnKeys: ["entry"],
      scroll: { x: "max-content" },
    });
    const target = wrapper.find(".ant-table-content").element;
    Object.defineProperties(target, {
      scrollLeft: { configurable: true, writable: true, value: 32 },
      scrollWidth: { configurable: true, value: 600 },
      clientWidth: { configurable: true, value: 200 },
      scrollHeight: { configurable: true, value: 100 },
      clientHeight: { configurable: true, value: 100 },
    });
    wrapper.vm.scrollTarget = target;
    wrapper.vm.syncMetrics();
    resizeColumn(wrapper, wrapper.vm.resolvedColumns[0], 360);
    expect(wrapper.vm.tableLayoutState.manualWidths.get("entry")).toBe(360);

    await wrapper.setProps({ layoutKey: "task-b:1" });
    await nextTick();

    expect(wrapper.vm.tableLayoutState.manualWidths.size).toBe(0);
    expect(target.scrollLeft).toBe(0);
    expect(wrapper.vm.metrics.scrollLeft).toBe(0);
    expect(wrapper.vm.resolvedColumns[0].width).toBe(220);
  });

  it("滚动时显示覆盖滚动条并按内容区定位轨道", async () => {
    const wrapper = mountViewport({ scroll: { x: "max-content" } });
    const target = wrapper.find(".ant-table-content").element;
    Object.defineProperties(target, {
      scrollTop: { value: 40, writable: true },
      scrollLeft: { value: 20, writable: true },
      scrollHeight: { value: 400 },
      scrollWidth: { value: 300 },
      clientHeight: { value: 100 },
      clientWidth: { value: 100 },
    });
    target.getBoundingClientRect = () => ({ top: 30, left: 20, right: 120, bottom: 130, width: 100, height: 100 });
    wrapper.vm.$el.getBoundingClientRect = () => ({ top: 10, left: 10, right: 210, bottom: 210, width: 200, height: 200 });
    Object.defineProperties(wrapper.vm.$el, {
      clientHeight: { value: 200 },
      clientWidth: { value: 200 },
    });
    wrapper.vm.scrollTarget = target;

    wrapper.vm.handleScroll();
    await nextTick();

    expect(wrapper.classes()).toContain("table-viewport-scrolling");
    expect(wrapper.vm.verticalTrackStyle).toEqual({ top: "24px", right: "92px", bottom: "84px" });
    expect(wrapper.vm.horizontalTrackStyle).toEqual({ left: "14px", right: "94px", bottom: "82px" });
    wrapper.unmount();
  });

  it("卸载时清理尺寸观察器和滚动监听器", () => {
    const wrapper = mountViewport();
    const target = wrapper.find(".ant-table-content").element;
    const removeEventListener = vi.spyOn(target, "removeEventListener");
    const resizeObserver = { disconnect: vi.fn() };

    wrapper.vm.scrollTarget = target;
    wrapper.vm.resizeObserver = resizeObserver;
    wrapper.unmount();

    expect(removeEventListener).toHaveBeenCalledWith("scroll", wrapper.vm.handleScroll);
    expect(resizeObserver.disconnect).toHaveBeenCalled();
  });
});
