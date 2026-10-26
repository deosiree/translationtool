import { describe, expect, it } from "vitest";
import { mount } from "@vue/test-utils";
import TableTagCell from "@/components/table/TableTagCell/TableTagCell.vue";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip/CellOverflowTooltip.vue";

describe("TableTagCell", () => {
  it("按中英文分号拆分标签，并把完整内容交给共享溢出 Tooltip", () => {
    const wrapper = mount(TableTagCell, {
      props: { text: "前端;后端；测试" },
      global: {
        stubs: {
          "a-tag": { template: "<span class='tag-stub'><slot /></span>" },
          CellOverflowTooltip: {
            props: ["content"],
            template: "<span class='tooltip-stub'><slot /></span>",
          },
        },
      },
    });

    expect(wrapper.findAll(".tag-stub").map((tag) => tag.text())).toEqual([
      "前端",
      "后端",
      "测试",
    ]);
    expect(wrapper.findComponent(CellOverflowTooltip).props("content")).toBe(
      "前端; 后端; 测试",
    );
  });
});
