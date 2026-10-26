<!-- @fileoverview 统一表格 Tag 浏览态的标签展示、单行省略和溢出 Tooltip。 -->
<template>
  <CellOverflowTooltip
    class="table-tag-cell"
    :content="tooltipText"
  >
    <span class="table-tag-cell__content">
      <a-tag
        v-for="(tag, index) in tags"
        :key="`${tag}-${index}`"
        :color="color"
        class="table-tag-cell__tag"
      >
        {{ tag }}
      </a-tag>
    </span>
  </CellOverflowTooltip>
</template>

<script>
import CellOverflowTooltip from "../CellOverflowTooltip/CellOverflowTooltip.vue";
import { formatTagText, splitTagText } from "../cellText.js";

export default {
  name: "TableTagCell",
  components: { CellOverflowTooltip },
  props: {
    text: {
      type: [String, Number],
      default: "",
    },
    color: {
      type: String,
      default: "cyan",
    },
  },
  computed: {
    /**
     * 返回当前单元格需要逐个展示的 Tag。
     * @returns {string[]} 过滤空值后的 Tag 列表。
     */
    tags() {
      return splitTagText(this.text);
    },
    /**
     * 返回溢出时展示的完整 Tag 文本。
     * @returns {string} 归一化后的 Tooltip 文本。
     */
    tooltipText() {
      return formatTagText(this.text);
    },
  },
};
</script>

<style scoped lang="less">
.table-tag-cell__content {
  display: block;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-tag-cell__tag {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}
</style>
