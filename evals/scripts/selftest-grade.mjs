#!/usr/bin/env node
/**
 * 自测：各套件每题 dry pass 应通过，dry fail 应未通过。
 */
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { EVALS_ROOT } from "./lib.mjs";

function listQuestions() {
  const out = [];
  for (const suite of ["protocol", "product"]) {
    const dir = path.join(EVALS_ROOT, "suites", suite);
    if (!fs.existsSync(dir)) continue;
    for (const d of fs.readdirSync(dir, { withFileTypes: true })) {
      if (d.isDirectory()) out.push(d.name);
    }
  }
  return out.sort();
}

const questions = listQuestions();
const script = path.join(EVALS_ROOT, "scripts", "run-question.mjs");
let failures = 0;

for (const q of questions) {
  for (const fixture of ["pass", "fail"]) {
    const expectPass = fixture === "pass";
    const batch = `selftest-${q}-${fixture}`;
    const r = spawnSync(
      process.execPath,
      [
        script,
        "--question",
        q,
        "--mode",
        "dry",
        "--fixture",
        fixture,
        "--batch",
        batch,
      ],
      { encoding: "utf8" },
    );
    const gotPass = r.status === 0;
    const ok = gotPass === expectPass;
    if (!ok) {
      failures += 1;
      console.error(
        `失败 ${q} 夹具=${fixture}：期望${expectPass ? "通过" : "未通过"}，实际退出码 ${r.status}`,
      );
      if (r.stderr) console.error(r.stderr);
      if (r.stdout) console.error(r.stdout);
    } else {
      console.log(`通过 ${q} 夹具=${fixture}`);
    }
  }
}

const hashScript = path.join(EVALS_ROOT, "scripts", "workflow-hash.mjs");
const h = spawnSync(process.execPath, [hashScript], { encoding: "utf8" });
if (h.status !== 0) {
  failures += 1;
  console.error("工作流指纹计算失败", h.stderr);
} else {
  const j = JSON.parse(h.stdout);
  if (!j.workflow_rev || !j.workflow_tree_hash) {
    failures += 1;
    console.error("工作流指纹缺少字段", j);
  } else {
    console.log(
      `通过 工作流指纹 rev=${j.workflow_rev.slice(0, 8)} tree=${j.workflow_tree_hash}`,
    );
  }
}

process.exit(failures ? 1 : 0);
