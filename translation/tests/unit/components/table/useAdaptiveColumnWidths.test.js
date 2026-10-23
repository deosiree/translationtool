import { describe, expect, it } from "vitest";
import {
  resolveAdaptiveColumnWidths,
  useAdaptiveColumnWidths,
} from "@/components/table/useAdaptiveColumnWidths";

const columns = [
  { colValue: "entry", width: 220, minWidth: 50 },
  { colValue: "translate", width: 260, minWidth: 50 },
  { colValue: "state", width: 100 },
];

describe("useAdaptiveColumnWidths", () => {
  /**
   * 验证剩余视口宽度按自适应列基础宽度比例分配。
   * @returns {void} 无返回值。
   */
  it("按基础宽度比例分配视口剩余空间", () => {
    const widths = resolveAdaptiveColumnWidths(
      1000,
      columns,
      ["entry", "translate"],
      {},
      48
    );

    expect(widths).toEqual({ entry: 390, translate: 462, state: 100 });
  });

  /**
   * 验证手动设置的列宽固定，并由另一自适应列接收剩余空间。
   * @returns {void} 无返回值。
   */
  it("固定手动列宽并将剩余空间交给未手动调整的列", () => {
    const widths = resolveAdaptiveColumnWidths(
      1000,
      columns,
      ["entry", "translate"],
      { entry: 300 },
      48
    );

    expect(widths).toEqual({ entry: 300, translate: 552, state: 100 });
  });

  /**
   * 验证视口变窄时保留列宽，不自动压缩到视口范围内。
   * @returns {void} 无返回值。
   */
  it("视口变窄时保留基础列宽并允许横向溢出", () => {
    const widths = resolveAdaptiveColumnWidths(
      600,
      columns,
      ["entry", "translate"],
      { entry: 300 },
      48
    );

    expect(widths).toEqual({ entry: 300, translate: 260, state: 100 });
  });

  /**
   * 验证手动宽度不低于配置下限。
   * @returns {void} 无返回值。
   */
  it("将手动宽度限制在最小宽度", () => {
    const state = useAdaptiveColumnWidths(50);
    expect(state.setManualWidth("entry", 30)).toBe(50);
    expect(state.resolve(0, columns, ["entry", "translate"], 48).entry).toBe(50);
  });
});
