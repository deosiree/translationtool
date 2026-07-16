#!/usr/bin/env node
/**
 * 将 evals/runs/<batchId> 下各题 score.yaml 写入历史并生成批次汇总。
 */
import fs from "node:fs";
import path from "node:path";
import {
  EVALS_ROOT,
  loadYaml,
  dumpSimpleYaml,
  writeText,
  walkFiles,
} from "./lib.mjs";

function parseArgs(argv) {
  const out = {};
  for (let i = 2; i < argv.length; i++) {
    if (argv[i] === "--batch") out.batch = argv[++i];
  }
  return out;
}

function loadHistory() {
  const histPath = path.join(EVALS_ROOT, "history", "score-history.yaml");
  if (!fs.existsSync(histPath)) return { entries: [] };
  const doc = loadYaml(histPath);
  if (!Array.isArray(doc.entries)) doc.entries = [];
  return doc;
}

function main() {
  const args = parseArgs(process.argv);
  if (!args.batch) {
    console.error("用法：node ingest-scores.mjs --batch <批次ID>");
    process.exit(2);
  }
  const batchDir = path.join(EVALS_ROOT, "runs", args.batch);
  if (!fs.existsSync(batchDir)) {
    console.error(`批次目录不存在：${batchDir}`);
    process.exit(2);
  }

  const scoreFiles = walkFiles(batchDir).filter((f) =>
    f.replace(/\\/g, "/").endsWith("/score.yaml"),
  );
  const scores = scoreFiles.map((f) => loadYaml(f));

  const history = loadHistory();
  for (const s of scores) {
    history.entries.push({
      batch_id: args.batch,
      question_id: s.question_id,
      workflow_rev: s.workflow_rev,
      workflow_tree_hash: s.workflow_tree_hash,
      result: s.result,
      overall: s.overall,
      compliance: s.compliance,
      execution_quality: s.execution_quality,
      agent: s.agent,
      component: s.component || "",
      summary: s.summary || "",
    });
  }
  writeText(
    path.join(EVALS_ROOT, "history", "score-history.yaml"),
    dumpSimpleYaml(history),
  );

  const pass = scores.filter((s) => s.result === "pass").length;
  const lines = [
    `# 批次汇总 — ${args.batch}`,
    "",
    `| 题目 | 结果 | 综合分 | 责任面 |`,
    `| --- | --- | --- | --- |`,
    ...scores.map(
      (s) =>
        `| ${s.question_id} | ${s.result === "pass" ? "通过" : "未通过"} | ${s.overall} | ${s.component || ""} |`,
    ),
    "",
    `通过率：${pass}/${scores.length}`,
    "",
  ];
  writeText(path.join(batchDir, "latest.md"), lines.join("\n"));
  writeText(
    path.join(batchDir, "latest-stats.yaml"),
    dumpSimpleYaml({
      batch_id: args.batch,
      total: scores.length,
      pass,
      fail: scores.length - pass,
      scores: scores.map((s) => ({
        question_id: s.question_id,
        result: s.result,
        overall: s.overall,
      })),
    }),
  );

  const improvements = [];
  for (const q of fs.readdirSync(batchDir, { withFileTypes: true })) {
    if (!q.isDirectory()) continue;
    const reviewPath = path.join(batchDir, q.name, "review.md");
    if (!fs.existsSync(reviewPath)) continue;
    const text = fs.readFileSync(reviewPath, "utf8");
    for (const line of text.split("\n")) {
      if (
        line.includes("[workflow]") ||
        line.includes("[eval]") ||
        line.includes("[capability]")
      ) {
        improvements.push(`- （${q.name}）${line.trim().replace(/^- /, "")}`);
      }
    }
  }
  writeText(
    path.join(batchDir, "batch-insights.md"),
    [
      `# 批次改进建议 — ${args.batch}`,
      "",
      "## 按标签汇总",
      "",
      ...improvements,
      "",
    ].join("\n"),
  );

  console.log(
    JSON.stringify(
      { batch_id: args.batch, ingested: scores.length, pass, fail: scores.length - pass },
      null,
      2,
    ),
  );
}

main();
