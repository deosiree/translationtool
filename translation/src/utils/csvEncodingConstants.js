/**
 * CSV / Excel 导入编码相关常量与工具
 * 供组件与业务 utils 共用，避免 utils 反向依赖 components。
 */

/** @type {string} 默认文件编码 */
export const DEFAULT_ENCODING = "UTF-8";

/**
 * 编码下拉选项（UI 灰禁时仍保留 GBK，永不启用选择）
 * @type {Array<{ label: string, value: string }>}
 */
export const ENCODING_OPTIONS = [
  { label: "UTF-8", value: "UTF-8" },
  { label: "GBK", value: "GBK" },
];

/** @type {string} 当前仅允许的上传 accept */
export const CSV_ONLY_ACCEPT = ".csv";

/** @type {string} 锁定的导入文件类型 */
export const LOCKED_IMPORT_TYPE = "csv";

/**
 * 通用编码说明 tip（多行，配合 white-space: pre-line）
 * @type {string}
 */
export const ENCODING_TIP = [
  "当前仅支持 csv UTF-8。",
  "简体 Windows 默认编码为 GBK，小语种文字可能出现问号、方块等乱码，请另存为 CSV UTF-8 后再导入。",
].join("\n");

/**
 * 文件管理页：强制 csv UTF-8 时的简短提示
 * @type {string}
 */
export const FILE_MANAGE_CSV_TIP =
  "仅支持csv UTF-8，因为简体 Windows 默认的编码方式 GBK，在小语种文字中会出现问号、方块等乱码";

/**
 * 生成扩展名不匹配时的提示文案（accept 可扩展，模板无需改）
 * @param {string} [accept] - 允许的后缀，如 `.csv` 或 `.csv,.xlsx`
 * @returns {string}
 */
export function formatAcceptExtensionMessage(accept) {
  const label = accept && String(accept).trim() ? String(accept).trim() : CSV_ONLY_ACCEPT;
  return `请选择 ${label} 格式的文件！`;
}

/**
 * 校验文件名是否符合 accept 后缀列表
 * @param {string} [fileName] - 文件名
 * @param {string} [accept] - 如 `.csv` 或 `.csv,.xlsx`
 * @returns {{ ok: boolean, message: string }}
 */
export function assertAcceptExtension(fileName, accept = CSV_ONLY_ACCEPT) {
  const message = formatAcceptExtensionMessage(accept);
  if (!fileName || typeof fileName !== "string") {
    return { ok: false, message };
  }
  const parts = String(accept || CSV_ONLY_ACCEPT)
    .split(",")
    .map((s) => s.trim().toLowerCase())
    .filter(Boolean)
    .map((p) => (p.startsWith(".") ? p : `.${p}`));
  if (parts.length === 0) {
    return { ok: true, message };
  }
  const lower = fileName.toLowerCase();
  const ok = parts.some((ext) => lower.endsWith(ext));
  return { ok, message };
}

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
