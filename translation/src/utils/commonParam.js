/**
 * 兼容层：保留旧路径 `@/utils/commonParam`，实际从新位置转发到 `@/constants/commonParam`
 * 建议新代码直接从 `@/constants/commonParam` 导入。
 */

export * from "@/constants/commonParam.js";
export { default } from "@/constants/commonParam.js";
