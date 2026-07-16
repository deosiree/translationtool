#!/usr/bin/env node
/**
 * CI / 本地 smoke：自测 → protocol dry → product dry → 基线门禁。
 */
import { spawnSync } from "node:child_process";
import path from "node:path";
import { EVALS_ROOT } from "./lib.mjs";

function run(script, scriptArgs = []) {
  const full = path.join(EVALS_ROOT, "scripts", script);
  const r = spawnSync(process.execPath, [full, ...scriptArgs], {
    encoding: "utf8",
    cwd: path.dirname(EVALS_ROOT),
    stdio: "inherit",
  });
  return r.status ?? 1;
}

function main() {
  const batchPrefix =
    process.env.HARNESS_EVAL_BATCH ||
    `ci-smoke-${new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19)}`;

  console.log("=== Harness 评测 CI 冒烟 ===\n");

  console.log("1/4 判卷自测…");
  if (run("selftest-grade.mjs") !== 0) process.exit(1);

  console.log("\n2/4 协议套件 dry…");
  if (
    run("run-suite.mjs", [
      "--suite",
      "protocol",
      "--mode",
      "dry",
      "--batch",
      `${batchPrefix}__protocol`,
    ]) !== 0
  ) {
    process.exit(1);
  }

  console.log("\n3/4 业务套件 dry（B01）…");
  if (
    run("run-suite.mjs", [
      "--suite",
      "product",
      "--mode",
      "dry",
      "--batch",
      `${batchPrefix}__product`,
    ]) !== 0
  ) {
    process.exit(1);
  }

  console.log("\n4/4 基线对比门禁…");
  if (
    run("compare-baseline.mjs", [
      "--gate",
      "--batch-prefix",
      batchPrefix,
      "--allow-hash-change",
    ]) !== 0
  ) {
    process.exit(1);
  }

  console.log("\n=== CI 冒烟通过 ===");
  console.log(`批次前缀：${batchPrefix}`);
}

main();
