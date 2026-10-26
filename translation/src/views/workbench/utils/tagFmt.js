/**
 * @fileoverview 工作台 Tag 文本工具的兼容导出。
 *
 * Tag 拆分和 Tooltip 文本已收敛至 components/table/cellText，保留原导出名避免
 * 既有页面和测试出现行为差异。
 */

export {
  splitTagText as companyCut,
  formatTagText,
} from "@/components/table/cellText.js";
