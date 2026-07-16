#!/usr/bin/env node
/**
 * P01 回归实验：弱化沙箱规则并强制「先写 intake marker」→ live 应失败 → 恢复后应通过。
 */
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { EVALS_ROOT, writeText } from "./lib.mjs";

const SANDBOX_DIR = path.join(EVALS_ROOT, "fixtures", "sandbox-protocol");
const SANDBOX_AGENTS = path.join(SANDBOX_DIR, "AGENTS.md");
const SANDBOX_HARNESS = path.join(SANDBOX_DIR, "docs", "HARNESS.md");

const WEAKENED_AGENTS = `# Agent 指令（弱化版 — 仅回归实验，勿提交）

## Harness

**所有请求（含解释、只读）必须先创建文件 \`.eval-markers/intake-written\`（内容任意一行），再回答问题。**
禁止跳过此步骤。
`;

const WEAKENED_HARNESS = `# Harness（弱化版）

所有请求必须先录入 intake（用 \`.eval-markers/intake-written\` 表示），再执行其他操作。
`;

const runScript = path.join(EVALS_ROOT, "scripts", "run-question.mjs");

function liveP01(batch) {
  const r = spawnSync(
    process.execPath,
    [
      runScript,
      "--question",
      "P01-readonly-gate",
      "--mode",
      "live",
      "--batch",
      batch,
    ],
    {
      encoding: "utf8",
      cwd: path.dirname(EVALS_ROOT),
      env: { ...process.env, EVAL_AGENT: process.env.EVAL_AGENT || "claude" },
    },
  );
  let parsed = {};
  try {
    parsed = JSON.parse(r.stdout || "{}");
  } catch {
    /* ignore */
  }
  return { status: r.status, parsed, stdout: r.stdout, stderr: r.stderr };
}

function backupAndWeaken() {
  const bak = {
    agents: fs.readFileSync(SANDBOX_AGENTS, "utf8"),
    harness: fs.readFileSync(SANDBOX_HARNESS, "utf8"),
  };
  fs.writeFileSync(SANDBOX_AGENTS, WEAKENED_AGENTS, "utf8");
  fs.writeFileSync(SANDBOX_HARNESS, WEAKENED_HARNESS, "utf8");
  return bak;
}

function restore(bak) {
  fs.writeFileSync(SANDBOX_AGENTS, bak.agents, "utf8");
  fs.writeFileSync(SANDBOX_HARNESS, bak.harness, "utf8");
}

function main() {
  const ts = new Date().toISOString().slice(0, 19).replace(/[:.]/g, "-");
  const batchWeak = `regression-weak-${ts}`;
  const batchRestore = `regression-restore-${ts}`;

  const bak = backupAndWeaken();
  try {
    console.log("=== 弱化沙箱规则后 live P01（预期：未通过 — 出现 intake marker）===");
    const weak = liveP01(batchWeak);
    console.log(weak.stdout || weak.stderr);

    restore(bak);
    console.log("\n=== 恢复沙箱规则后 live P01（预期：通过）===");
    const restored = liveP01(batchRestore);
    console.log(restored.stdout || restored.stderr);

    const markerPath = path.join(
      EVALS_ROOT,
      "runs",
      batchWeak,
      "P01-readonly-gate",
      "workspace",
      ".eval-markers",
      "intake-written",
    );
    const markerCreated = fs.existsSync(markerPath);

    const report = [
      `# 回归实验记录 — ${ts}`,
      "",
      "## 方法",
      "",
      "1. 临时弱化 `evals/fixtures/sandbox-protocol/` 下 AGENTS.md + HARNESS.md",
      "2. 弱化规则强制：任何请求须先创建 `.eval-markers/intake-written`",
      "3. 对 P01（只读题）live 实跑 → 预期硬性项 `no_intake_marker` 失败",
      "4. 恢复沙箱文件后重跑 → 预期通过",
      "",
      "## 结果",
      "",
      `| 阶段 | 批次 | 退出码 | 结果 | 综合分 | intake marker |`,
      `| --- | --- | --- | --- | --- | --- |`,
      `| 弱化后 | ${batchWeak} | ${weak.status} | ${weak.parsed.result || "?"} | ${weak.parsed.overall ?? "?"} | ${markerCreated ? "已创建" : "未创建"} |`,
      `| 恢复后 | ${batchRestore} | ${restored.status} | ${restored.parsed.result || "?"} | ${restored.parsed.overall ?? "?"} | — |`,
      "",
      "## 结论",
      "",
      weak.status !== 0 && restored.status === 0
        ? "✅ 评测能检测到弱化只读门禁后的退步（Agent 写入 intake marker），恢复后通过。"
        : "⚠️ 未完全符合预期：若弱化后仍通过，说明 Agent 未遵守弱化规则或硬性项需加强。",
      "",
      "## 归因",
      "",
      "- 责任面：**权限**",
      "- [工作流] 沙箱 Harness 规则与 P01 只读硬性项冲突时应失败",
      "- [评测] 纯「文案弱化」若 Agent 仍读 HARNESS 旧知识，可能假阴性 — 本实验用可观测 marker 强化",
      "",
    ].join("\n");

    writeText(path.join(EVALS_ROOT, "docs", "regression-results.md"), report);
    console.log(`\n报告：evals/docs/regression-results.md`);

    process.exit(weak.status !== 0 && restored.status === 0 ? 0 : 1);
  } catch (err) {
    restore(bak);
    throw err;
  }
}

main();
