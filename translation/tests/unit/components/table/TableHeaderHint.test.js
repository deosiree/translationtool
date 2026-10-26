import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import TableHeaderHint from "@/components/table/TableHeaderHint/TableHeaderHint.vue";

const tooltipStub = {
  props: ["title"],
  template: '<div class="tooltip-stub"><slot /><slot name="title" /></div>',
};

describe("TableHeaderHint", () => {
  it("为 tag 和 comment 显示统一提示及信息图标", () => {
    const wrapper = mount({
      template: `
        <div>
          <TableHeaderHint title="tag" :column="{ dataIndex: 'tag' }" />
          <TableHeaderHint title="comment" :column="{ dataIndex: 'comment' }" />
        </div>
      `,
      components: { TableHeaderHint },
    }, {
      global: {
        stubs: {
          "a-tooltip": tooltipStub,
          InfoCircleOutlined: { template: "<i class='info-icon' />" },
        },
      },
    });

    expect(wrapper.text()).toContain("供开发人员定位词条");
    expect(wrapper.text()).toContain("供翻译人员定位词条的使用场景");
    expect(wrapper.findAll(".info-icon")).toHaveLength(2);
    expect(wrapper.findAll("[aria-label]").map((node) => node.attributes("aria-label"))).toEqual([
      "tag：供开发人员定位词条",
      "comment：供翻译人员定位词条的使用场景",
    ]);
  });

  it("未知字段退化为普通列名，不显示提示图标", () => {
    const wrapper = mount(TableHeaderHint, {
      props: { title: "词条", column: { dataIndex: "entry" } },
      global: {
        stubs: {
          "a-tooltip": tooltipStub,
          InfoCircleOutlined: { template: "<i class='info-icon' />" },
        },
      },
    });

    expect(wrapper.text()).toBe("词条");
    expect(wrapper.find(".info-icon").exists()).toBe(false);
    expect(wrapper.attributes("aria-label")).toBeUndefined();
  });
});
