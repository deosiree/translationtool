import { describe, expect, it } from "vitest";
import { detectCsvEncodingFromBytes } from "@/utils/encodingDetectUtils";

describe("detectCsvEncodingFromBytes", () => {
  it("detects pure ASCII as ASCII", () => {
    const bytes = new TextEncoder().encode("id,entry,english\n1,hello,world\n");
    expect(detectCsvEncodingFromBytes(bytes)).toBe("ASCII");
  });

  it("detects UTF-8 Chinese as UTF-8", () => {
    const bytes = new TextEncoder().encode("id,词条\n1,中文\n");
    expect(detectCsvEncodingFromBytes(bytes)).toBe("UTF-8");
  });

  it("detects UTF-8 BOM as UTF-8", () => {
    const body = new TextEncoder().encode("id,词条\n");
    const bytes = new Uint8Array(3 + body.length);
    bytes.set([0xef, 0xbb, 0xbf], 0);
    bytes.set(body, 3);
    expect(detectCsvEncodingFromBytes(bytes)).toBe("UTF-8");
  });

  it("detects typical GBK Chinese bytes as GBK", () => {
    // GBK: 中文 = D6 D0 CE C4
    const bytes = new Uint8Array([
      0x69, 0x64, 0x2c, 0xd6, 0xd0, 0xce, 0xc4, 0x0a,
    ]);
    expect(detectCsvEncodingFromBytes(bytes)).toBe("GBK");
  });
});
