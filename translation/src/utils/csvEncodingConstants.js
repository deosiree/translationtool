/**
 * CSV / Excel 导入编码相关常量与工具
 * 供组件与业务 utils 共用，避免 utils 反向依赖 components。
 */

/** @type {string} 默认文件编码 */
export const DEFAULT_ENCODING = "UTF-8";

/**
 * 编码下拉选项
 * @type {Array<{ label: string, value: string }>}
 */
export const ENCODING_OPTIONS = [
  { label: "UTF-8", value: "UTF-8" },
  { label: "GBK", value: "GBK" },
];

/**
 * 通用编码说明 tip（多行，配合 white-space: pre-line）
 * @type {string}
 */
export const ENCODING_TIP = [
  "xlsx、xls 本身是 Unicode，不存在编码方式，多国文字不会乱码；",
  "但 csv 存在编码方式，并且简体 Windows 默认为 GBK，小语种文字会出现问号、方块等乱码，请在另存为 csv 时尽量选择 csv UTF-8。",
].join("\n");

/**
 * 文件管理页：强制 csv UTF-8 时的简短提示
 * @type {string}
 */
export const FILE_MANAGE_CSV_TIP =
  "仅支持csv UTF-8，因为简体 Windows 默认的编码方式 GBK，在小语种文字中会出现问号、方块等乱码";

/**
 * 判断文件名是否为 csv
 * @param {string} [fileName] - 文件名
 * @returns {boolean}
 */
export function isCsvFileName(fileName) {
  if (!fileName || typeof fileName !== "string") return false;
  return fileName.toLowerCase().endsWith(".csv");
}

/**
 * 判断 accept 是否仅为 csv（可含多个等价写法，如 `.csv` / `csv`）
 * @param {string} [accept] - 上传 accept 字符串
 * @returns {boolean}
 */
export function isCsvOnlyAccept(accept) {
  if (!accept || typeof accept !== "string") return false;
  const parts = accept
    .split(",")
    .map((s) => s.trim().toLowerCase())
    .filter(Boolean);
  return parts.length > 0 && parts.every((p) => p === ".csv" || p === "csv");
}

/**
 * 是否应展示/传递编码（csv 场景）
 * @param {{ accept?: string, fileName?: string, fileType?: string }} [opts]
 * @param {string} [opts.accept] - 上传 accept
 * @param {string} [opts.fileName] - 已选文件名
 * @param {string} [opts.fileType] - 外部文件类型（如 BackFill 的 importType）
 * @returns {boolean}
 */
export function shouldShowEncoding({ accept, fileName, fileType } = {}) {
  if (fileType === "csv") return true;
  if (isCsvOnlyAccept(accept)) return true;
  if (isCsvFileName(fileName)) return true;
  return false;
}
