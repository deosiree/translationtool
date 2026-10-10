import { describe, it, expect, vi, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import { nextTick } from "vue";

vi.mock("ant-design-vue", async () => {
  const actual = await vi.importActual("ant-design-vue");
  return {
    ...actual,
    notification: {
      error: vi.fn(),
      success: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    },
  };
});

import { notification } from "ant-design-vue";
import LockedCsvImportMeta from "@/components/LockedCsvImportMeta/index.vue";
import FileSelectWithEncoding from "@/components/FileSelectWithEncoding/index.vue";
import {
  DEFAULT_ENCODING,
  ENCODING_OPTIONS,
  CSV_ONLY_ACCEPT,
} from "@/utils/csvEncodingConstants";

describe("LockedCsvImportMeta", () => {
  it("灰禁展示文件类型 csv 与编码 UTF-8，options 仍含 GBK", async () => {
    const wrapper = mount(LockedCsvImportMeta, {
      props: {
        showFileType: true,
        showEncoding: true,
        asFormItems: false,
        showInlineLabels: true,
      },
      global: {
        stubs: {
          "a-row": { template: "<div><slot /></div>" },
          "a-col": { template: "<div><slot /></div>" },
          "a-form-item": { template: "<div><slot /></div>" },
          "a-select": {
            props: ["value", "options", "disabled"],
            template:
              '<select data-testid="type-or-encoding" :disabled="disabled" :value="value"></select>',
          },
          "a-tooltip": { template: "<span><slot /><slot name='title' /></span>" },
          QuestionCircleOutlined: true,
        },
      },
    });
    await nextTick();
    const selects = wrapper.findAll("select");
    expect(selects.length).toBeGreaterThanOrEqual(1);
    selects.forEach((s) => {
      expect(s.attributes("disabled")).toBeDefined();
    });
    expect(ENCODING_OPTIONS.some((o) => o.value === "GBK")).toBe(true);
    expect(wrapper.text()).toContain("文件类型");
    wrapper.unmount();
  });
});

describe("FileSelectWithEncoding", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("encodingLocked 时编码下拉 disabled 且值为 UTF-8", async () => {
    const wrapper = mount(FileSelectWithEncoding, {
      props: {
        encoding: DEFAULT_ENCODING,
        encodingLocked: true,
        showFileSelect: false,
        accept: CSV_ONLY_ACCEPT,
      },
      global: {
        stubs: {
          "a-select": {
            props: ["value", "options", "disabled"],
            template:
              '<select data-testid="encoding" :disabled="disabled" :value="value"></select>',
          },
          "a-tooltip": { template: "<span><slot /></span>" },
          "a-upload": true,
          "a-button": true,
          "a-input": true,
          QuestionCircleOutlined: true,
        },
      },
    });
    await nextTick();
    const select = wrapper.find('[data-testid="encoding"]');
    expect(select.exists()).toBe(true);
    expect(select.attributes("disabled")).toBeDefined();
    expect(wrapper.vm.effectiveEncoding).toBe("UTF-8");
    wrapper.unmount();
  });

  it("非 csv 扩展名触发 notification", async () => {
    const wrapper = mount(FileSelectWithEncoding, {
      props: {
        encodingLocked: true,
        showFileSelect: true,
        accept: CSV_ONLY_ACCEPT,
        validateAcceptOnSelect: true,
      },
      global: {
        stubs: {
          "a-select": true,
          "a-tooltip": true,
          "a-upload": {
            props: ["beforeUpload"],
            setup(props, { slots }) {
              return () =>
                slots.default
                  ? slots.default()
                  : null;
            },
          },
          "a-button": true,
          "a-input": true,
          QuestionCircleOutlined: true,
        },
      },
    });
    const result = wrapper.vm.onBeforeUpload({ name: "bad.xlsx" }, []);
    expect(result).toBe(false);
    expect(notification.error).toHaveBeenCalled();
    const arg = notification.error.mock.calls[0][0];
    expect(arg.description).toBe("请选择 .csv 格式的文件！");
    wrapper.unmount();
  });
});
