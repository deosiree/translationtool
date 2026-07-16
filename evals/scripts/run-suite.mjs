#!/usr/bin/env node
/**
 * 跑整套题；支持 --trials N 对每题重复实跑并生成稳定性报告。
 */
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { EVALS_ROOT, writeText, dumpSimpleYaml } from "./lib.mjs";

function parseArgs(argv) {
  const out = { suite: "protocol", mode: "dry", trials: 1 };
  for (let i = 2; i < argv.length; i++) {
    if (argv[i] === "--suite") out.suite = argv[++i];
    else if (argv[i] === "--mode") out.mode = argv[++i];
    else if (argv[i] === "--batch") out.batch = argv[++i];
    else if (argv[i] === "--trials") out.trials = Math.max(1, Number(argv[++i]) || 1);
  }
  return out;
}

function runOne(script, question, mode, batchId) {
  const r = spawnSync(
    process.execPath,
    [
      script,
      "--question",
      question,
      "--mode",
      mode,
      ...(mode === "dry" ? ["--fixture", "pass"] : []),
      "--batch",
      batchId,
    ],
    { encoding: "utf8", cwd: path.dirname(EVALS_ROOT) },
  );
  let summary = "";
  try {
    const j = JSON.parse(r.stdout || "{}");
    summary = j.summary || "";
  } catch {
    /* ignore */
  }
  return {
    ok: r.status === 0,
    status: r.status,
    summary,
    stdout: r.stdout,
    stderr: r.stderr,
  };
}

function main() {
  const args = parseArgs(process.argv);
  const suiteDir = path.join(EVALS_ROOT, "suites", args.suite);
  if (!fs.existsSync(suiteDir)) {
    console.error(`套件不存在：${suiteDir}`);
    process.exit(2);
  }
  const questions = fs
    .readdirSync(suiteDir, { withFileTypes: true })
    .filter((d) => d.isDirectory())
    .map((d) => d.name)
    .sort();

  const batchId =
    args.batch ||
    `suite-${args.suite}-${args.mode}-t${args.trials}-${new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19)}`;

  const script = path.join(EVALS_ROOT, "scripts", "run-question.mjs");
  const trialMatrix = [];

  for (const q of questions) {
    const row = { question: q, trials: [], pass: 0, fail: 0 };
    for (let t = 1; t <= args.trials; t++) {
      const subBatch = args.trials > 1 ? `${batchId}__${q}__t${t}` : batchId;
      const r = runOne(script, q, args.mode, subBatch);
      row.trials.push({
        trial: t,
        batch: subBatch,
        result: r.ok ? "pass" : "fail",
        summary: r.summary,
      });
      if (r.ok) row.pass += 1;
      else row.fail += 1;
      process.stdout.write(
        `${r.ok ? "通过" : "未通过"} ${q} 第${t}/${args.trials} 次\n`,
      );
      if (!r.ok && r.stderr) process.stderr.write(r.stderr);
    }
    trialMatrix.push(row);
  }

  // 汇总批次：trials=1 时 ingest 一次；trials>1 时每题取最后一次写入主批次目录
  if (args.trials === 1) {
    spawnSync(
      process.execPath,
      [path.join(EVALS_ROOT, "scripts", "ingest-scores.mjs"), "--batch", batchId],
      { encoding: "utf8", stdio: "inherit" },
    );
  }

  const stability = {
    batch_id: batchId,
    suite: args.suite,
    mode: args.mode,
    trials_per_question: args.trials,
    questions: trialMatrix.map((row) => ({
      question_id: row.question,
      pass: row.pass,
      fail: row.fail,
      pass_rate: row.trials.length ? row.pass / row.trials.length : 0,
      trials: row.trials,
    })),
    total_runs: trialMatrix.reduce((n, r) => n + r.trials.length, 0),
    total_pass: trialMatrix.reduce((n, r) => n + r.pass, 0),
  };

  const reportDir = path.join(EVALS_ROOT, "runs", batchId);
  fs.mkdirSync(reportDir, { recursive: true });

  const md = [
    `# 稳定性报告 — ${batchId}`,
    "",
    `- 套件：${args.suite}`,
    `- 模式：${args.mode}`,
    `- 每题次数：${args.trials}`,
    `- 总通过率：${stability.total_pass}/${stability.total_runs}`,
    "",
    `| 题目 | 通过 | 失败 | 通过率 |`,
    `| --- | --- | --- | --- |`,
    ...stability.questions.map(
      (q) =>
        `| ${q.question_id} | ${q.pass} | ${q.fail} | ${(q.pass_rate * 100).toFixed(0)}% |`,
    ),
    "",
    "## 各次明细",
    "",
    ...stability.questions.flatMap((q) => [
      `### ${q.question_id}`,
      "",
      ...q.trials.map(
        (t) =>
          `- 第 ${t.trial} 次：${t.result === "pass" ? "通过" : "未通过"} — ${t.summary || "（无摘要）"}`,
      ),
      "",
    ]),
  ].join("\n");

  writeText(path.join(reportDir, "stability-report.md"), md);
  writeText(path.join(reportDir, "stability-report.yaml"), dumpSimpleYaml(stability));

  process.stdout.write(JSON.stringify(stability, null, 2) + "\n");
  const anyFail = stability.total_pass < stability.total_runs;
  process.exit(anyFail ? 1 : 0);
}

main();
