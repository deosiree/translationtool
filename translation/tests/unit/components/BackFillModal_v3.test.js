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
          "a-button": {
            props: ["type", "size"],
            template: '<button :data-button-type="type"><slot /></button>',
          },
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

  /**
   * 验证按钮模式下触发按钮类型的默认值与可配置行为。
   * @param {void} none - 无参数。
   * @returns {Promise<void>} 属性更新断言完成后结束。
   */
  it("按钮模式默认保留主按钮并支持指定默认按钮", async () => {
    expect(wrapper.find("button").attributes("data-button-type")).toBe(
      "primary"
    );

    await wrapper.setProps({ buttonType: "default" });

    expect(wrapper.find("button").attributes("data-button-type")).toBe(
      "default"
    );
  });

  it("validateFileExtension 非 csv 时 notification 并 reject", async () => {
    await expect(
      wrapper.vm.validateFileExtension({ name: "x.xlsx" })
    ).rejects.toBe("请选择 .csv 格式的文件！");
    expect(notification.error).toHaveBeenCalled();
    const arg = notification.error.mock.calls[0][0];
    expect(arg.description).toBe("请选择 .csv 格式的文件！");
  });

  it("完整成功和部分成功通知刷新，全部失败不触发刷新", /**
   * 验证完整或部分成功通知调用方刷新，而全部失败不通知刷新。
   * @param {void} none - 测试用例不接收参数。
   * @returns {Promise<void>} 三种导入结果断言完成后结束。
   */
  async () => {
    await wrapper.vm.handleImportResponse({
      code: 200,
      success: ["english"],
      failed: {},
    });

    await wrapper.vm.handleImportResponse({
      code: 201,
      success: ["english"],
      failed: {
        russian: {
          code: 201,
          globalMessage: "俄文更新失败",
          failedEntryInfos: [],
          exceptionVos: [],
        },
      },
    });

    expect(wrapper.emitted("importSuccess")).toHaveLength(2);
    await wrapper.vm.handleImportResponse({
      code: 201,
      success: [],
      failed: {
        english: {
          code: 201,
          globalMessage: "英文更新失败",
          failedEntryInfos: [],
          exceptionVos: [],
        },
      },
    });

    expect(wrapper.emitted("importSuccess")).toHaveLength(2);
  });
});
