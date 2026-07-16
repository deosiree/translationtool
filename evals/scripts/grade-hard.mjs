#!/usr/bin/env node
/**
 * 确定性硬性判卷器。
 * 用法：node grade-hard.mjs --workspace <dir> --env <env.yaml> --transcript <file>
 * 输出 JSON：{ result, hard_checks, summary }
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { loadYaml, resolveUnder } from "./lib.mjs";

export function gradeHard({ workspace, env, transcriptText }) {
  const checks = env.hard_checks || [];
  const results = [];

  for (const check of checks) {
    const r = runCheck(check, workspace, transcriptText);
    results.push(r);
  }

  const failed = results.filter((x) => !x.passed);
  const result = failed.length === 0 ? "pass" : "fail";
  const summary =
    result === "pass"
      ? `全部 ${results.length} 项硬性检查通过`
      : `硬性检查未通过：${failed.map((f) => f.id).join("、")}`;

  return { result, hard_checks: results, summary };
}

function runCheck(check, workspace, transcriptText) {
  const id = check.id || check.type;
  const text = transcriptText || "";

  switch (check.type) {
    case "path_absent": {
      const p = resolveUnder(workspace, check.path);
      const exists = fs.existsSync(p);
      return {
        id,
        passed: !exists,
        detail: exists ? `路径不应存在但存在：${check.path}` : `路径不存在（符合预期）：${check.path}`,
      };
    }
    case "path_present": {
      const p = resolveUnder(workspace, check.path);
      const exists = fs.existsSync(p);
      return {
        id,
        passed: exists,
        detail: exists ? `路径已存在：${check.path}` : `缺少路径：${check.path}`,
      };
    }
    case "transcript_min_chars": {
      const n = text.trim().length;
      const min = Number(check.min ?? 1);
      return {
        id,
        passed: n >= min,
        detail: `答卷字数=${n}，要求≥${min}`,
      };
    }
    case "transcript_contains": {
      const needle = String(check.text || "");
      const ok = text.includes(needle);
      return {
        id,
        passed: ok,
        detail: ok
          ? `答卷包含关键词 ${JSON.stringify(needle)}`
          : `答卷缺少关键词 ${JSON.stringify(needle)}`,
      };
    }
    case "transcript_contains_any": {
      const needles = Array.isArray(check.texts) ? check.texts : [check.text].filter(Boolean);
      const hit = needles.find((n) => text.includes(String(n)));
      return {
        id,
        passed: Boolean(hit),
        detail: hit
          ? `答卷包含关键词 ${JSON.stringify(hit)}`
          : `答卷未包含任一关键词：${needles.map((n) => JSON.stringify(n)).join("、")}`,
      };
    }
    case "transcript_not_contains": {
      const needle = String(check.text || "");
      const ok = !text.includes(needle);
      return {
        id,
        passed: ok,
        detail: ok
          ? `答卷未出现禁用词 ${JSON.stringify(needle)}`
          : `答卷不应出现 ${JSON.stringify(needle)}`,
      };
    }
    case "not_greeting_only": {
      const t = text.trim();
      const firstLine = t.split("\n")[0].trim();
      const greetingOnly =
        t.length < 120 &&
        /^(您好|你好|Hello|Hi)[^。！\n]{0,40}[。！]?$/.test(firstLine) &&
        !/只读|变更|intake|trace|CONTEXT|FEATURE|terminology-agent|python/i.test(t);
      return {
        id,
        passed: !greetingOnly,
        detail: greetingOnly
          ? "答卷仅有寒暄，未执行任务"
          : "答卷不是纯寒暄",
      };
    }
    default:
      return {
        id,
        passed: false,
        detail: `未知检查类型：${check.type}`,
      };
  }
}

function parseArgs(argv) {
  const out = {};
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--workspace") out.workspace = argv[++i];
    else if (a === "--env") out.env = argv[++i];
    else if (a === "--transcript") out.transcript = argv[++i];
  }
  return out;
}

const isMain =
  process.argv[1] &&
  path.resolve(process.argv[1]) === path.resolve(fileURLToPath(import.meta.url));
if (isMain) {
  const args = parseArgs(process.argv);
  if (!args.workspace || !args.env || !args.transcript) {
    console.error(
      "用法：node grade-hard.mjs --workspace <目录> --env <env.yaml> --transcript <文件>",
    );
    process.exit(2);
  }
  const env = loadYaml(path.resolve(args.env));
  const transcriptText = fs.readFileSync(path.resolve(args.transcript), "utf8");
  const graded = gradeHard({
    workspace: path.resolve(args.workspace),
    env,
    transcriptText,
  });
  process.stdout.write(JSON.stringify(graded, null, 2) + "\n");
  process.exit(graded.result === "pass" ? 0 : 1);
}
