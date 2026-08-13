import { describe, it, expect } from "vitest";
import {
  DEFAULT_ENCODING,
  ENCODING_OPTIONS,
  CSV_ONLY_ACCEPT,
  LOCKED_IMPORT_TYPE,
  formatAcceptExtensionMessage,
  assertAcceptExtension,
} from "@/utils/csvEncodingConstants";

describe("csvEncodingConstants", () => {
  it("保留 UTF-8 默认与 GBK 选项", () => {
    expect(DEFAULT_ENCODING).toBe("UTF-8");
    expect(LOCKED_IMPORT_TYPE).toBe("csv");
    expect(CSV_ONLY_ACCEPT).toBe(".csv");
    expect(ENCODING_OPTIONS.map((o) => o.value)).toEqual(["UTF-8", "GBK"]);
  });

  it("formatAcceptExtensionMessage 跟随 accept", () => {
    expect(formatAcceptExtensionMessage(".csv")).toBe(
      "请选择 .csv 格式的文件！"
    );
    expect(formatAcceptExtensionMessage(".csv,.xlsx")).toBe(
      "请选择 .csv,.xlsx 格式的文件！"
    );
  });

  it("assertAcceptExtension：.csv 通过，其它失败且文案跟 accept", () => {
    expect(assertAcceptExtension("a.csv", ".csv").ok).toBe(true);
    const fail = assertAcceptExtension("a.xlsx", ".csv");
    expect(fail.ok).toBe(false);
    expect(fail.message).toBe("请选择 .csv 格式的文件！");

    const multiOk = assertAcceptExtension("a.xlsx", ".csv,.xlsx");
    expect(multiOk.ok).toBe(true);
    const multiFail = assertAcceptExtension("a.xml", ".csv,.xlsx");
    expect(multiFail.ok).toBe(false);
    expect(multiFail.message).toBe("请选择 .csv,.xlsx 格式的文件！");
  });
});
