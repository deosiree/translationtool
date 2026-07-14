import { notification } from "ant-design-vue";
import { isCsvFileName } from "@/utils/csvEncodingConstants";

/** 读取前若干字节做启发式探测（默认 256KB） */
const SAMPLE_BYTES = 256 * 1024;

/**
 * 判断字节序列是否带 UTF-8 BOM（EF BB BF）
 * @param {Uint8Array} bytes - 文件字节样本
 * @returns {boolean}
 */
function hasUtf8Bom(bytes) {
  return (
    bytes.length >= 3 &&
    bytes[0] === 0xef &&
    bytes[1] === 0xbb &&
    bytes[2] === 0xbf
  );
}

/**
 * 判断字节序列是否为纯 ASCII（全部字节 ≤ 0x7F）
 * @param {Uint8Array} bytes - 文件字节样本
 * @returns {boolean}
 */
function isPureAscii(bytes) {
  for (let i = 0; i < bytes.length; i++) {
    if (bytes[i] > 0x7f) return false;
  }
  return true;
}

/**
 * 严格按 UTF-8（fatal）解码，任一非法字节则判定为非 UTF-8
 * @param {Uint8Array} bytes - 文件字节样本
 * @returns {boolean}
 */
function isValidUtf8(bytes) {
  try {
    new TextDecoder("utf-8", { fatal: true }).decode(bytes);
    return true;
  } catch {
    return false;
  }
}

/**
 * 根据字节样本探测编码（仅区分 UTF-8 / GBK / ASCII）
 * - 纯 ASCII：与 UTF-8、GBK 均兼容，不做冲突判定
 * - 合法 UTF-8（含中文等多字节）：判为 UTF-8
 * - 非法 UTF-8 且含高位字节：按本系统场景判为 GBK（简体 Windows 常见）
 * @param {Uint8Array} bytes - 文件字节样本
 * @returns {'UTF-8'|'GBK'|'ASCII'|'UNKNOWN'}
 */
export function detectCsvEncodingFromBytes(bytes) {
  if (!bytes || bytes.length === 0) return "UNKNOWN";
  if (hasUtf8Bom(bytes)) return "UTF-8";
  if (isPureAscii(bytes)) return "ASCII";
  if (isValidUtf8(bytes)) return "UTF-8";
  return "GBK";
}

/**
 * 读取文件前缀样本，用于编码探测（避免整文件加载）
 * @param {File|Blob} file - 待探测文件
 * @param {number} [maxBytes=SAMPLE_BYTES] - 最大读取字节数
 * @returns {Promise<Uint8Array>}
 */
async function readFileSample(file, maxBytes = SAMPLE_BYTES) {
  const size = Math.min(file.size || 0, maxBytes);
  const slice = file.slice(0, size);
  const buffer = await slice.arrayBuffer();
  return new Uint8Array(buffer);
}

/**
 * 探测文件编码（读取样本后启发式判断）
 * @param {File|Blob} file - 待探测文件
 * @returns {Promise<'UTF-8'|'GBK'|'ASCII'|'UNKNOWN'>}
 */
export async function detectCsvFileEncoding(file) {
  if (!file) return "UNKNOWN";
  const bytes = await readFileSample(file);
  return detectCsvEncodingFromBytes(bytes);
}

/**
 * 校验 CSV 文件实际编码是否与所选编码一致
 * @param {File|Blob|null|undefined} file - 待校验文件
 * @param {string} expectedEncoding - 期望编码：'UTF-8' | 'GBK'
 * @param {Object} [options] - 可选配置
 * @param {boolean} [options.notify=true] - 不一致时是否弹出 notification
 * @param {string} [options.fileLabel] - 提示中的文件标识（无 file.name 时使用）
 * @returns {Promise<{ ok: boolean, detected: string, expected: string, message?: string }>}
 */
export async function assertCsvEncodingMatch(
  file,
  expectedEncoding,
  options = {}
) {
  const { notify = true, fileLabel = "" } = options;
  const expected = expectedEncoding === "GBK" ? "GBK" : "UTF-8";

  if (!file) {
    return { ok: true, detected: "UNKNOWN", expected };
  }

  const name = file.name || fileLabel || "";
  if (name && !isCsvFileName(name)) {
    return { ok: true, detected: "UNKNOWN", expected };
  }

  const detected = await detectCsvFileEncoding(file);

  // ASCII / 未知：不拦截（兼容纯英文表头、空内容等）
  if (detected === "ASCII" || detected === "UNKNOWN") {
    return { ok: true, detected, expected };
  }

  if (detected === expected) {
    return { ok: true, detected, expected };
  }

  const label = name ? `「${name}」` : "所选文件";
  const message = `${label}检测编码为 ${detected}，与当前设置的 ${expected} 不一致。请改选编码，或另存为 ${expected} 的 CSV 后再导入。`;

  if (notify) {
    notification.error({
      message: "CSV 编码不匹配",
      description: message,
      duration: 8,
    });
  }

  return { ok: false, detected, expected, message };
}

/**
 * 批量校验多个 CSV（任一不匹配即失败；只弹一次通知汇总）
 * @param {Array<{ file: File|Blob|null|undefined, label?: string }>} items - 待校验文件列表
 * @param {string} expectedEncoding - 期望编码：'UTF-8' | 'GBK'
 * @returns {Promise<{ ok: boolean, failures: Array<{ ok: boolean, detected: string, expected: string, message?: string, label?: string }> }>}
 */
export async function assertCsvEncodingMatchAll(items, expectedEncoding) {
  const failures = [];
  for (const item of items || []) {
    if (!item?.file) continue;
    const res = await assertCsvEncodingMatch(item.file, expectedEncoding, {
      notify: false,
      fileLabel: item.label || item.file.name,
    });
    if (!res.ok) failures.push({ ...res, label: item.label || item.file.name });
  }

  if (failures.length === 0) {
    return { ok: true, failures };
  }

  const description = failures
    .map(
      (f) =>
        `「${f.label || ""}」疑似 ${f.detected}，当前选择 ${f.expected}`
    )
    .join("；");

  notification.error({
    message: "CSV 编码不匹配",
    description: `${description}。请改选编码，或另存为对应编码的 CSV 后再导入。`,
    duration: 8,
  });

  return { ok: false, failures };
}
