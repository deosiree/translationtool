#!/usr/bin/env node
/**
 * 对比当前 workflow_tree_hash 与基线，并对 dry 套件结果做门禁。
 *
 * 用法：
 *   node compare-baseline.mjs --print
 *   node compare-baseline.mjs --gate --batch-prefix <前缀>
 *   node compare-baseline.mjs --gate --batch <批次ID>   # 单套件批次
 *   node compare-baseline.mjs --update-baseline --batch-prefix <前缀>
 */
import fs from "node:fs";
import path from "node:path";
import {
  EVALS_ROOT,
  loadYaml,
  dumpSimpleYaml,
  writeText,
  workflowRev,
  workflowTreeHash,
} from "./lib.mjs";

const BASELINE_PATH = path.join(EVALS_ROOT, "history", "workflow-baseline.yaml");
const SUITE_SUFFIX = { protocol: "__protocol", product: "__product" };

function parseArgs(argv) {
  const out = {
    gate: false,
    updateBaseline: false,
    print: false,
    allowHashChange: false,
    batch: null,
    batchPrefix: null,
  };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--gate") out.gate = true;
    else if (a === "--update-baseline") out.updateBaseline = true;
    else if (a === "--print") out.print = true;
    else if (a === "--allow-hash-change") out.allowHashChange = true;
    else if (a === "--batch") out.batch = argv[++i];
    else if (a === "--batch-prefix") out.batchPrefix = argv[++i];
  }
  return out;
}

function loadBaseline() {
  if (!fs.existsSync(BASELINE_PATH)) return null;
  return loadYaml(BASELINE_PATH);
}

function suiteFromBatchId(batchId) {
  if (batchId.includes("__product")) return "product";
  if (batchId.includes("__protocol")) return "protocol";
  const reportPath = path.join(EVALS_ROOT, "runs", batchId, "stability-report.yaml");
  if (fs.existsSync(reportPath)) {
    const r = loadYaml(reportPath);
    if (r.suite) return r.suite;
  }
  const batchDir = path.join(EVALS_ROOT, "runs", batchId);
  if (!fs.existsSync(batchDir)) return null;
  for (const ent of fs.readdirSync(batchDir, { withFileTypes: true })) {
    if (!ent.isDirectory()) continue;
    if (ent.name.match(/^B\d/)) return "product";
    if (ent.name.match(/^P\d/)) return "protocol";
  }
  return null;
}

function readBatchStats(batchId) {
  const reportPath = path.join(EVALS_ROOT, "runs", batchId, "stability-report.yaml");
  if (fs.existsSync(reportPath)) {
    const r = loadYaml(reportPath);
    return {
      batch_id: batchId,
      suite: r.suite || suiteFromBatchId(batchId),
      pass: r.total_pass ?? 0,
      total: r.total_runs ?? 0,
    };
  }
  const batchDir = path.join(EVALS_ROOT, "runs", batchId);
  if (!fs.existsSync(batchDir)) return null;
  let pass = 0;
  let total = 0;
  let suite = suiteFromBatchId(batchId);
  for (const ent of fs.readdirSync(batchDir, { withFileTypes: true })) {
    if (!ent.isDirectory()) continue;
    const scorePath = path.join(batchDir, ent.name, "score.yaml");
    if (!fs.existsSync(scorePath)) continue;
    const s = loadYaml(scorePath);
    total += 1;
    if (s.result === "pass") pass += 1;
    if (!suite) {
      if (ent.name.match(/^B\d/)) suite = "product";
      else if (ent.name.match(/^P\d/)) suite = "protocol";
    }
  }
  if (!total) return null;
  return { batch_id: batchId, suite, pass, total };
}

function collectSuiteStats(args) {
  const suites = {};
  if (args.batchPrefix) {
    for (const [suite, suffix] of Object.entries(SUITE_SUFFIX)) {
      const batchId = `${args.batchPrefix}${suffix}`;
      const stats = readBatchStats(batchId);
      if (stats) suites[suite] = stats;
    }
    return suites;
  }
  if (args.batch) {
    const stats = readBatchStats(args.batch);
    if (stats && stats.suite) suites[stats.suite] = stats;
    return suites;
  }
  return suites;
}

function main() {
  const args = parseArgs(process.argv);
  const current = {
    workflow_rev: workflowRev(),
    workflow_tree_hash: workflowTreeHash(),
  };
  const baseline = loadBaseline();

  const report = {
    current,
    baseline: baseline
      ? {
          workflow_tree_hash: baseline.workflow_tree_hash,
          workflow_rev: baseline.workflow_rev,
          updated_at: baseline.updated_at,
        }
      : null,
    hash_changed: baseline
      ? baseline.workflow_tree_hash !== current.workflow_tree_hash
      : true,
    suites: collectSuiteStats(args),
    gate_passed: true,
    messages: [],
  };

  if (!baseline) {
    report.messages.push("[workflow] 尚无 workflow-baseline.yaml，首次请 --update-baseline");
  } else if (report.hash_changed) {
    report.messages.push(
      `[workflow] 指纹变更：${baseline.workflow_tree_hash} → ${current.workflow_tree_hash}`,
    );
    report.messages.push(
      "[workflow] 合并前请本地 live 回归，再 compare-baseline --update-baseline",
    );
  } else {
    report.messages.push("[workflow] 工作流指纹与基线一致");
  }

  if ((args.gate || args.updateBaseline) && !Object.keys(report.suites).length) {
    console.error("未找到批次统计，请提供 --batch 或 --batch-prefix");
    process.exit(2);
  }

  if (baseline?.suites) {
    for (const [suite, cfg] of Object.entries(baseline.suites)) {
      const actual = report.suites[suite];
      if (!actual) {
        if (args.gate) {
          report.gate_passed = false;
          report.messages.push(`[eval] 缺少套件 ${suite} 的批次结果`);
        }
        continue;
      }
      const minPass = cfg.min_pass ?? cfg.total ?? 0;
      if (actual.pass < minPass) {
        report.gate_passed = false;
        report.messages.push(
          `[eval] 套件 ${suite} 未达基线：${actual.pass}/${actual.total}，要求 ≥ ${minPass}`,
        );
      } else {
        report.messages.push(
          `[eval] 套件 ${suite} 达基线：${actual.pass}/${actual.total}`,
        );
      }
    }
  }

  if (args.gate && report.hash_changed && !args.allowHashChange) {
    report.messages.push(
      "[workflow] CI：指纹变更仅告警；dry 未达标才阻断",
    );
  }

  const outDir = args.batchPrefix
    ? path.join(EVALS_ROOT, "runs", args.batchPrefix)
    : args.batch
      ? path.join(EVALS_ROOT, "runs", args.batch)
      : path.join(EVALS_ROOT, "runs", "_compare");

  const md = [
    "# 基线对比报告",
    "",
    `生成时间：${new Date().toISOString()}`,
    "",
    "## 工作流指纹",
    "",
    "| 项 | 值 |",
    "| --- | --- |",
    `| 当前 tree_hash | \`${current.workflow_tree_hash}\` |`,
    `| 基线 tree_hash | \`${baseline?.workflow_tree_hash ?? "（无）"}\` |`,
    `| 是否变更 | ${report.hash_changed ? "是" : "否"} |`,
    "",
    "## 套件结果",
    "",
    ...Object.entries(report.suites).map(
      ([k, v]) => `- **${k}**（${v.batch_id}）：通过 ${v.pass}/${v.total}`,
    ),
    "",
    "## 消息",
    "",
    ...report.messages.map((m) => `- ${m}`),
    "",
    `**门禁：** ${report.gate_passed ? "通过" : "未通过"}`,
    "",
  ].join("\n");

  writeText(path.join(outDir, "baseline-compare.md"), md);
  writeText(
    path.join(outDir, "baseline-compare.json"),
    JSON.stringify(report, null, 2) + "\n",
  );

  if (args.print || (!args.gate && !args.updateBaseline)) {
    process.stdout.write(JSON.stringify(report, null, 2) + "\n");
  }

  if (args.updateBaseline) {
    const newBaseline = {
      updated_at: new Date().toISOString().slice(0, 10),
      workflow_tree_hash: current.workflow_tree_hash,
      workflow_rev: current.workflow_rev,
      note: "compare-baseline --update-baseline 更新",
      suites: { ...(baseline?.suites || {}) },
    };
    for (const [suite, actual] of Object.entries(report.suites)) {
      newBaseline.suites[suite] = {
        mode: "dry",
        total: actual.total,
        min_pass: actual.pass,
      };
    }
    writeText(BASELINE_PATH, dumpSimpleYaml(newBaseline));
    console.log(`已更新基线：${current.workflow_tree_hash}`);
  }

  if (args.gate) {
    process.stdout.write(report.gate_passed ? "门禁通过\n" : "门禁未通过\n");
    process.exit(report.gate_passed ? 0 : 1);
  }
}

main();
