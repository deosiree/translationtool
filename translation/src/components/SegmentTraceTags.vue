<template>
  <span class="segment-trace-tags">
    <a-tag
      v-for="(word, idx) in words"
      :key="idx"
      :color="tagColors[idx % tagColors.length]"
      size="small"
    >
      {{ word }}
    </a-tag>
    <span v-if="!words.length" class="segment-trace-empty">-</span>
  </span>
</template>

<script>
import { parseSegmentTrace } from "@/utils/parseSegmentTrace.js";

const DEFAULT_COLORS = [
  "cyan",
];

export default {
  name: "SegmentTraceTags",
  props: {
    /** segment_trace 对象（或 JSON 字符串） */
    value: {
      type: [Object, String],
      default: null,
    },
    /** 自定义颜色数组，为空则用默认色板 */
    colors: {
      type: Array,
      default: () => [],
    },
    /** 分隔符（仅当 jieba 不存在时拆 display 用） */
    separator: {
      type: String,
      default: " | ",
    },
  },
  computed: {
    /** 从 value 中提取 jieba 词片列表，若无则从 display 拆分 */
    words() {
      const parsed = parseSegmentTrace(this.value);
      if (!parsed) return [];

      if (Array.isArray(parsed.jieba) && parsed.jieba.length > 0) {
        return parsed.jieba;
      }
      if (typeof parsed.display === "string" && parsed.display.trim()) {
        return parsed.display.split(this.separator).filter(Boolean);
      }
      return [];
    },
    tagColors() {
      return this.colors.length > 0 ? this.colors : DEFAULT_COLORS;
    },
  },
};
</script>

<style scoped>
.segment-trace-tags {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 2px;
  align-items: center;
}
.segment-trace-empty {
  color: #999;
}
</style>
