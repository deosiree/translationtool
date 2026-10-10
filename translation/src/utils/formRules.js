/**
 * 全站通用表单/单元格规则工厂（纯函数，无 Vue / 无 HTTP）。
 * 词条超长、特殊字符占位、可选禁检规则等跨模块 SSOT。
 */

/** 超长错误文案前缀；完整句由 checkLen 拼出 */
export const MSG_LEN = "允许最大字符数为";

/** 特殊字符/占位符不一致文案 */
export const MSG_PLACE = "特殊字符不一致\r\n(如%1翻译成% 1)";

/** 原文 ：%n 须对应译文 : %n */
export const MSG_COLON_PLACE = "冒号占位格式不一致（原文 ：%n 应对应译文 : %n）";

export const MSG_BAD_DISPLAY = "包含显示异常字符（· 或孤立 &）";
export const MSG_MULTI_SPACE = "包含连续空格";
export const MSG_ESCAPE = "包含转义或控制字符";
export const MSG_OPERATOR = "词条为纯运算符，不应翻译";
export const MSG_CONCAT = "疑似拼接残句，请合并后再译";

const OPERATORS = new Set([
  "!=",
  "<",
  ">",
  ">=",
  "=",
  "<=",
  "IN",
  "NOT",
  "×",
  "+",
  "%",
  "÷",
  "-",
]);

/**
 * 计算字符串字节长度：英=1，中文(CJK)=2，俄(西里尔)=3；其余按 1。
 * @param {string|null|undefined} str
 * @returns {number}
 */
export function byteLen(str) {
  if (str == null) return 0;
  const s = String(str).trim();
  let n = 0;
  for (let i = 0; i < s.length; i++) {
    const c = s.charCodeAt(i);
    if (c >= 0x0400 && c <= 0x04ff) n += 3;
    else if (c >= 0x4e00 && c <= 0x9fa5) n += 2;
    else n += 1;
  }
  return n;
}

/**
 * 从文本提取占位符序列。长匹配优先：[{}]、(*.后缀)、%n/{n}、{}。
 * @param {string|null|undefined} text
 * @returns {string[]}
 */
export function pickPlace(text) {
  const s = String(text ?? "");
  const out = [];
  const re =
    /\[\{\}\]|\(\*\.[A-Za-z0-9]+\)|%\d+|%[a-zA-Z]|%\.\d+[a-zA-Z]|\{\d+\}|\{\}/g;
  let m;
  while ((m = re.exec(s))) out.push(m[0]);
  return out;
}

/**
 * 超长校验。
 * @param {string|null|undefined} text
 * @param {number|null|undefined} max
 * @returns {string|null}
 */
export function checkLen(text, max) {
  if (max == null || max === "" || Number(max) <= 0) return null;
  const lim = Number(max);
  if (!Number.isFinite(lim)) return null;
  if (byteLen(text) > lim) {
    return `${MSG_LEN}${lim}`;
  }
  return null;
}

/**
 * 统计文本中某正则各捕获组出现次数。
 * @param {string} text
 * @param {RegExp} re - 须带 g，且 group1 为编号
 * @returns {Map<string, number>}
 */
function countCaptureGroups(text, re) {
  const map = new Map();
  let m;
  const flags = re.flags.includes("g") ? re.flags : `${re.flags}g`;
  const local = new RegExp(re.source, flags);
  while ((m = local.exec(text))) {
    const key = m[1];
    map.set(key, (map.get(key) || 0) + 1);
  }
  return map;
}

/**
 * 原文 ：%n 须在译文出现同等次数的 : %n（拦 :%n / :% n；避免 : %1 误匹配 : %10）。
 * @param {string} src
 * @param {string} dst
 * @returns {string|null}
 */
function checkColonPercentPlace(src, dst) {
  const srcCounts = countCaptureGroups(src, /：%(\d+)/g);// 原文`：%n`
  if (srcCounts.size === 0) return null;
  const dstCounts = countCaptureGroups(dst, /: %(\d+)(?!\d)/g);// 译文`: %n`
  for (const [n, need] of srcCounts) {
    if ((dstCounts.get(n) || 0) < need) return MSG_COLON_PLACE;
  }
  return null;
}

/**
 * 特殊字符/占位符一致性（原文 vs 译文）。
 * 返回 null 表示通过；返回文案表示失败原因（优先冒号占位专用文案）。
 * @param {string|null|undefined} src
 * @param {string|null|undefined} dst
 * @returns {string|null}
 */
export function checkPlace(src, dst) {
  // 统一成字符串，避免 null/undefined 干扰后续正则与 pickPlace
  const srcStr = String(src ?? "");
  const dstStr = String(dst ?? "");

  // ① 中文冒号占位：原文 ：%n → 译文必须 : %n（拦 :%n / :% n）；命中则直接返回专用文案
  const colonErr = checkColonPercentPlace(srcStr, dstStr);
  if (colonErr) return colonErr;

  // ② 抽取双方占位符集合（含 %n / {n} 等），排序后比较多重集是否一致
  const a = pickPlace(srcStr).slice().sort().join("\0");
  const b = pickPlace(dstStr).slice().sort().join("\0");
  if (a !== b) return MSG_PLACE;

  // ③ 兜底：原文有紧贴数字的 %n，译文却写成 "% 1" 这类空格分隔，仍判不一致
  //    （部分场景 pickPlace 抽到的 token 相同，需额外拦掉）
  if (/%\d/.test(srcStr) && /%\s+\d/.test(dstStr)) {
    return MSG_PLACE;
  }

  return null;
}

/**
 * 禁止显示异常字符：· 或孤立 &（&& 合法）。
 * @param {string|null|undefined} text
 * @returns {string|null}
 */
export function checkBadDisplay(text) {
  const s = String(text ?? "");
  if (s.includes("·")) return MSG_BAD_DISPLAY;
  if (s.replace(/&&/g, "").includes("&")) return MSG_BAD_DISPLAY;
  return null;
}

/**
 * 禁止连续两个及以上普通空格。
 * @param {string|null|undefined} text
 * @returns {string|null}
 */
export function checkMultiSpace(text) {
  if (/ {2,}/.test(String(text ?? ""))) return MSG_MULTI_SPACE;
  return null;
}

/**
 * 禁止字面 \n\t\r 或真实换行/制表。
 * @param {string|null|undefined} text
 * @returns {string|null}
 */
export function checkEscape(text) {
  const s = String(text ?? "");
  if (/\\[ntr]/.test(s) || /[\n\t\r]/.test(s)) return MSG_ESCAPE;
  return null;
}

/**
 * 整条仅为运算符。
 * @param {string|null|undefined} text
 * @returns {string|null}
 */
export function checkOperator(text) {
  const s = String(text ?? "").trim();
  if (!s) return null;
  const key = /^[a-zA-Z]+$/.test(s) ? s.toUpperCase() : s;
  if (OPERATORS.has(key)) return MSG_OPERATOR;
  return null;
}

/**
 * 疑似拼接残句：未闭合括号，或以开括号结尾、以闭括号开头。
 * @param {string|null|undefined} text
 * @returns {string|null}
 */
export function checkConcat(text) {
  const s = String(text ?? "");
  if (!s) return null;
  if (/^[\]】)]/.test(s) || /[\[【(]$/.test(s)) return MSG_CONCAT;

  const pairs = [
    ["[", "]"],
    ["【", "】"],
    ["(", ")"],
  ];
  for (const [open, close] of pairs) {
    let depth = 0;
    for (const ch of s) {
      if (ch === open) depth += 1;
      else if (ch === close) {
        depth -= 1;
        if (depth < 0) return MSG_CONCAT;
      }
    }
    if (depth !== 0) return MSG_CONCAT;
  }
  return null;
}

/** 规则元数据 SSOT（label / tooltip / message / targets / checked） */
export const RULES = [
  {
    key: "toLong",
    label: "校验字符长度",
    tooltip:
      "按字节检查译文长度，超出分类上限则不通过\n1英文字符 = 1字节\n1中文字符 = 2字节\n1俄文字符 = 3字节",
    message: MSG_LEN,
    checked: true,
    targets: ["translate"],
  },
  {
    key: "special",
    label: "校验特殊字符",
    tooltip:
      "对照原文检查译文占位符种类与数量\n识别：%n、{n}、[{}]、{}、(*.后缀)\n不通过例：%1→% 1；[{}]→[{ }]；(*.scd)→(* .scd)\n原文 ：%n 须对应译文 : %n（拦 :%n / :% n）",
    message: MSG_PLACE,
    checked: true,
    targets: ["translate"],
  },
  {
    key: "noBadDisplay",
    label: "禁止显示异常的特殊字符",
    tooltip:
      "禁止导致显示异常的字符\n· 间隔号（导出 Excel 易异常）\n孤立 &（QT 当作快捷键；应写 && 才显示为 &）",
    message: MSG_BAD_DISPLAY,
    checked: false,
    targets: ["translate", "entry"],
  },
  {
    key: "noMultiSpace",
    label: "禁止连续空格",
    tooltip:
      "禁止连续两个及以上普通空格\n避免排版或对齐异常\n避免翻译困难",
    message: MSG_MULTI_SPACE,
    checked: false,
    targets: ["translate", "entry"],
  },
  {
    key: "noEscape",
    label: "禁止转义字符",
    tooltip:
      "禁止转义或不可见控制符\n含字面 \\n \\t \\r\n及真实换行、制表符",
    message: MSG_ESCAPE,
    checked: false,
    targets: ["entry"],
  },
  {
    key: "noOperator",
    label: "禁止纯运算符",
    tooltip:
      "整条词条仅为运算符时不应送翻\n!= < > >= = <= IN NOT × + % ÷ -\n英文运算符不区分大小写",
    message: MSG_OPERATOR,
    checked: false,
    targets: ["entry"],
  },
  {
    key: "noConcat",
    label: "禁止拼接句",
    tooltip:
      "禁止明显被拆开的拼接残片\n应拼成完整句再入库\n启发式：括号未闭合\n或以 [【( 结尾、以 ]】) 开头",
    message: MSG_CONCAT,
    checked: false,
    targets: ["entry"],
  },
];

/**
 * 按 target 过滤并深拷贝为下拉 options（含 checked）。
 * @param {'translate'|'entry'} target
 * @returns {Array<{key:string,label:string,tooltip:string,checked:boolean}>}
 */
export function rulesForTarget(target) {
  return RULES.filter((r) => r.targets.includes(target)).map((r) => ({
    key: r.key,
    label: r.label,
    tooltip: r.tooltip,
    checked: r.checked,
  }));
}

/**
 * 译文侧规则按固定顺序跑一遍；返回首条错误或 null。
 * @param {{entry?:string,translate:string,maxLength?:number|null,methods:string[]}} opts
 * @returns {string|null}
 */
export function checkTranslateSide({
  entry,
  translate,
  maxLength,
  methods = [],
}) {
  if (methods.includes("toLong")) {
    const err = checkLen(translate, maxLength);
    if (err) return err;
  }
  if (methods.includes("special")) {
    const err = checkPlace(entry, translate);
    if (err) return err;
  }
  if (methods.includes("noBadDisplay")) {
    const err = checkBadDisplay(translate);
    if (err) return err;
  }
  if (methods.includes("noMultiSpace")) {
    const err = checkMultiSpace(translate);
    if (err) return err;
  }
  return null;
}

/**
 * 原文侧规则按固定顺序跑一遍；返回首条错误或 null。
 * @param {{entry:string,methods:string[]}} opts
 * @returns {string|null}
 */
export function checkEntrySide({ entry, methods = [] }) {
  if (methods.includes("noBadDisplay")) {
    const err = checkBadDisplay(entry);
    if (err) return err;
  }
  if (methods.includes("noMultiSpace")) {
    const err = checkMultiSpace(entry);
    if (err) return err;
  }
  if (methods.includes("noEscape")) {
    const err = checkEscape(entry);
    if (err) return err;
  }
  if (methods.includes("noOperator")) {
    const err = checkOperator(entry);
    if (err) return err;
  }
  if (methods.includes("noConcat")) {
    const err = checkConcat(entry);
    if (err) return err;
  }
  return null;
}
