import { describe, expect, it, vi } from "vitest";
import {
  resolveTableColumnLayout,
  useTableColumnLayout,
} from "@/components/table/useTableColumnLayout";

const columns = [
  { colValue: "entry", width: 220, minWidth: 50 },
  { colValue: "translate", width: 260, minWidth: 50 },
  { colValue: "state", width: 100, minWidth: 50 },
];

/**
 * 创建用于 resize 手势断言的最小事件对象。
 * @param {Object} target 事件目标。
 * @param {number} [clientX=0] 鼠标横向坐标。
 * @param {number} [clientY=0] 鼠标纵向坐标。
 * @returns {Object} 兼容手势 hook 的事件对象。
 */
function createResizeEvent(target, clientX = 0, clientY = 0) {
  return {
    button: 0,
    clientX,
    clientY,
    target,
    preventDefault() {},
    stopPropagation() {},
  };
}

describe("useTableColumnLayout", () => {
  it("在宽屏按基础宽度比例分配剩余空间", () => {
    const layout = resolveTableColumnLayout({
      viewportWidth: 1000,
      columns,
      adaptiveKeys: ["entry", "translate"],
      reservedWidth: 48,
    });

    expect(layout.widths).toEqual({ entry: 390, translate: 462, state: 100 });
    expect(layout.tableWidth).toBe(1000);
    expect(layout.distributing).toBe(true);
  });

  it("在窄屏保留默认列宽并交给表格内部横向滚动", () => {
    const layout = resolveTableColumnLayout({
      viewportWidth: 600,
      columns,
      adaptiveKeys: ["entry", "translate"],
      reservedWidth: 48,
    });

    expect(layout.widths).toEqual({ entry: 220, translate: 260, state: 100 });
    expect(layout.tableWidth).toBe(628);
    expect(layout.distributing).toBe(false);
  });

  it("未显式配置列下限时也使用共享最小宽度", () => {
    const layout = resolveTableColumnLayout({
      viewportWidth: 100,
      columns: [{ colValue: "entry", width: 20 }],
    });

    expect(layout.widths).toEqual({ entry: 50 });
    expect(layout.tableWidth).toBe(50);
  });

  it("为未配置宽度的自适应列使用统一基础宽度", () => {
    const layout = resolveTableColumnLayout({
      viewportWidth: 400,
      columns: [{ colValue: "entry" }],
      adaptiveKeys: ["entry"],
      adaptiveBaseWidth: 220,
    });

    expect(layout.widths).toEqual({ entry: 400 });
    expect(layout.tableWidth).toBe(400);
    expect(layout.distributing).toBe(true);
  });

  it("点击 resize handle 不写入手动列宽", () => {
    const header = {};
    const handle = {
      closest: (selector) => (selector === ".ant-table-resize-handle" ? handle : header),
    };
    const state = useTableColumnLayout();

    expect(state.beginResize(createResizeEvent(handle, 100))).toBe(true);
    expect(state.commitResize(360, { colValue: "entry" })).toBe(false);
    expect(state.finishResize()).toBe(false);
    expect(state.manualWidths.size).toBe(0);
  });

  it("真实拖拽保存列宽并仅吞掉同一表头的一次 click", () => {
    const header = {};
    const handle = {
      closest: (selector) => (selector === ".ant-table-resize-handle" ? handle : header),
    };
    const state = useTableColumnLayout({ resizeThreshold: 3 });
    const resizeEvent = createResizeEvent(handle, 100);
    const clickEvent = {
      target: { closest: () => header },
      preventDefaultCalled: false,
      stopPropagationCalled: false,
      preventDefault() {
        this.preventDefaultCalled = true;
      },
      stopPropagation() {
        this.stopPropagationCalled = true;
      },
    };

    state.beginResize(resizeEvent);
    expect(state.trackResize(createResizeEvent(handle, 104))).toBe(true);
    expect(state.commitResize(304, { colValue: "entry" })).toBe(true);
    expect(state.finishResize()).toBe(true);
    expect(state.manualWidths.get("entry")).toBe(304);
    expect(state.consumeResizeClick(clickEvent)).toBe(true);
    expect(clickEvent.preventDefaultCalled).toBe(true);
    expect(clickEvent.stopPropagationCalled).toBe(true);
    expect(state.consumeResizeClick(clickEvent)).toBe(false);
  });

  it("排序 click 先发生时仍提交 Ant 下一帧返回的最终列宽", () => {
    const header = {};
    const handle = {
      closest: (selector) => (selector === ".ant-table-resize-handle" ? handle : header),
    };
    const state = useTableColumnLayout();
    const clickEvent = {
      target: { closest: () => header },
      preventDefault() {},
      stopPropagation() {},
    };

    state.beginResize(createResizeEvent(handle, 100));
    state.trackResize(createResizeEvent(handle, 112));
    state.finishResize();
    expect(state.consumeResizeClick(clickEvent)).toBe(true);
    expect(state.commitResize(312, { colValue: "entry" })).toBe(true);
    expect(state.manualWidths.get("entry")).toBe(312);
  });

  it("拖拽后未派发 click 时不会拦截下一次排序", () => {
    vi.useFakeTimers();
    const header = {};
    const handle = {
      closest: (selector) => (selector === ".ant-table-resize-handle" ? handle : header),
    };
    const state = useTableColumnLayout();
    const clickEvent = {
      target: { closest: () => header },
      preventDefault() {},
      stopPropagation() {},
    };

    state.beginResize(createResizeEvent(handle, 100));
    state.trackResize(createResizeEvent(handle, 110));
    state.finishResize();
    vi.runAllTimers();

    expect(state.consumeResizeClick(clickEvent)).toBe(false);
    vi.useRealTimers();
  });

  it("重置后清空手动列宽和待处理手势", () => {
    const state = useTableColumnLayout();
    state.setManualWidth("entry", 300);
    state.reset();

    expect(state.manualWidths.size).toBe(0);
  });
});
