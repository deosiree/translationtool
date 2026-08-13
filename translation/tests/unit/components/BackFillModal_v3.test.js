/**
 * BackFillModal_v3：CSV UTF-8 灰禁收敛
 */
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { mount } from "@vue/test-utils";
import { nextTick } from "vue";
import { notification } from "ant-design-vue";
import {
  DEFAULT_ENCODING,
  CSV_ONLY_ACCEPT,
  LOCKED_IMPORT_TYPE,
} from "@/utils/csvEncodingConstants";

vi.mock("ant-design-vue", async () => {
  const actual = await vi.importActual("ant-design-vue");
  return {
    ...actual,
    message: { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() },
    notification: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    },
  };
});

vi.mock("@/utils/excelUtils", () => ({
  entryBatchImportExcel_v3: vi.fn(),
  entryValidate_v2: vi.fn(),
}));

vi.mock("@/utils/encodingDetectUtils", () => ({
  assertCsvEncodingMatchAll: vi.fn(() => Promise.resolve({ ok: true })),
}));

vi.mock("@/utils/notificationUtils", () => ({
  handleErrorNotification: vi.fn(),
}));

vi.mock("@/utils/domUtils", () => ({
  setModalAriaHidden: vi.fn(),
  stopDomEvent: vi.fn(),
}));

vi.mock("@/constants/commonParam.js", () => ({
  default: { languageList: [{ name: "英文", value: "english" }] },
  entryParams: {
    validateFields: [{ label: "词条", value: "entry" }],
  },
}));

import BackFillModal_v3 from "@/components/Button/fileManage/backFill/modal_v3.vue";

describe("BackFillModal_v3 locked csv utf-8", () => {
  let wrapper;

  beforeEach(() => {
    vi.clearAllMocks();
    wrapper = mount(BackFillModal_v3, {
      props: {
        mode: "button",
        showFileTypeSelect: true,
        defaultAccept: CSV_ONLY_ACCEPT,
      },
      global: {
        stubs: {
          CustomModal: {
            template: "<div><slot /></div>",
            props: ["visible", "okLoading", "modalTitle"],
          },
          LockedCsvImportMeta: true,
          ExportButton: true,
          "a-button": true,
          "a-form": {
            template: "<form><slot /></form>",
            methods: {
              validate: () => Promise.resolve(),
              clearValidate: vi.fn(),
            },
          },
          "a-form-item": { template: "<div><slot /></div>" },
          "a-select": true,
          "a-upload": true,
          "a-row": { template: "<div><slot /></div>" },
          "a-col": { template: "<div><slot /></div>" },
          "a-checkbox": true,
          "a-modal": true,
          "a-table": true,
          "a-tabs": true,
          "a-tab-pane": true,
          "a-space": true,
          "a-alert": true,
          "a-tooltip": true,
        },
      },
    });
  });

  afterEach(() => {
    wrapper?.unmount();
  });

  it("importType 固定 csv，encoding/accept 固定 UTF-8/.csv", async () => {
    await nextTick();
    expect(wrapper.vm.formModel.importType).toBe(LOCKED_IMPORT_TYPE);
    expect(wrapper.vm.formModel.encoding).toBe(DEFAULT_ENCODING);
    expect(wrapper.vm.backFillAccept).toBe(".csv");
    expect(wrapper.vm.submitEncoding).toBe("UTF-8");
  });

  it("validateFileExtension 非 csv 时 notification 并 reject", async () => {
    await expect(
      wrapper.vm.validateFileExtension({ name: "x.xlsx" })
    ).rejects.toBe("请选择 .csv 格式的文件！");
    expect(notification.error).toHaveBeenCalled();
    const arg = notification.error.mock.calls[0][0];
    expect(arg.description).toBe("请选择 .csv 格式的文件！");
  });
});
