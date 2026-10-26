import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import IsExistBadge from "@/components/stateBadge/isExistBadge.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import EntryStateSelect from "@/components/select/entryStateSelect.vue";
import TransStateSelect from "@/components/select/transStateSelect.vue";

const selectStubs = {
  "a-select": {
    template: "<div class=\"select-stub\"><slot /></div>",
  },
  "a-select-option": {
    props: ["value"],
    template:
      "<div class=\"select-option-stub\" :data-value=\"value\"><slot /></div>",
  },
};

describe("状态展示控件", () => {
  it("Badge 对数字和字符串状态使用同一份标签与颜色", () => {
    const exists = mount(IsExistBadge, {
      props: { isExist: "1" },
      global: { stubs: { "a-badge": true } },
    });
    const entry = mount(EntryStateBadge, {
      props: { entryState: "2" },
      global: { stubs: { "a-badge": true } },
    });
    const translate = mount(TransStateBadge, {
      props: { translateState: 3 },
      global: { stubs: { "a-badge": true } },
    });

    expect(exists.text()).toBe("已存在");
    expect(exists.find("span").attributes("style")).toContain(
      "rgb(251, 179, 31)",
    );
    expect(entry.text()).toBe("审核不通过");
    expect(entry.find("span").attributes("style")).toContain(
      "rgb(255, 0, 0)",
    );
    expect(translate.text()).toBe("已审核");
    expect(translate.find("span").attributes("style")).toContain(
      "rgb(54, 191, 125)",
    );
  });

  it("词条状态下拉使用审核中文案并按字符串筛选禁用项", () => {
    const wrapper = mount(EntryStateSelect, {
      props: {
        entryState: null,
        filter: new Set(["0", "-1"]),
        showForbbiden: true,
      },
      global: { stubs: selectStubs },
    });

    expect(wrapper.text()).not.toContain("新建");
    expect(wrapper.text()).toContain("审核中");
    expect(wrapper.text()).not.toContain("禁用");
    expect(wrapper.findAll(".select-option-stub").map((node) => node.attributes("data-value"))).toEqual([
      "1",
      "2",
      "3",
    ]);
  });

  it("翻译状态下拉保持字符串值并隐藏被筛选状态", () => {
    const wrapper = mount(TransStateSelect, {
      props: {
        translateState: null,
        filter: new Set(["3"]),
      },
      global: { stubs: selectStubs },
    });

    expect(wrapper.text()).toContain("未翻译");
    expect(wrapper.text()).toContain("待审核");
    expect(wrapper.text()).not.toContain("已审核");
    expect(wrapper.findAll(".select-option-stub").map((node) => node.attributes("data-value"))).toEqual([
      "0",
      "1",
      "2",
    ]);
  });

  it("两个状态下拉都以原有字符串值通知父组件", async () => {
    const entry = mount(EntryStateSelect, {
      props: { entryState: 1 },
      global: { stubs: selectStubs },
    });
    const translate = mount(TransStateSelect, {
      props: { translateState: 1 },
      global: { stubs: selectStubs },
    });

    entry.vm.entryState_ = "2";
    translate.vm.translateState_ = "3";
    await entry.vm.$nextTick();
    await translate.vm.$nextTick();

    expect(entry.emitted("update:entryState")).toEqual([["2"]]);
    expect(translate.emitted("update:translateState")).toEqual([["3"]]);
  });
});
