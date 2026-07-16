#!/usr/bin/env node
/**
 * 单题编排：环境准备 → dry/live 答卷 → 硬性判卷 → score/review 落盘。
 */
import fs from "node:fs";
import path from "node:path";
import {
  EVALS_ROOT,
  ensureDir,
  writeText,
  questionDir,
  loadYaml,
  workflowRev,
  workflowTreeHash,
  dumpSimpleYaml,
} from "./lib.mjs";
import { gradeHard } from "./grade-hard.mjs";

/** 各题失败时的默认改进建议（中文，供人工审查） */
const IMPROVEMENT_HINTS = {
  "P01-readonly-gate": {
    workflow: "只读题仍 bootstrap 或写 intake/trace → 强化 AGENTS.md 只读门禁表述",
    eval: "题面若歧义，在 task.md 强调「仅解释、禁止写盘」",
    capability: "模型倾向「先记账再答」→ 在规则中强调请求分类优先",
  },
  "P02-change-intake": {
    workflow: "变更题未 intake → 在变更闭环首步强制 intake",
    eval: "沙箱 marker 说明是否够清晰",
    capability: "只寒暄不执行 → 已用 not_greeting_only 拦截",
  },
  "P03-context-rules": {
    workflow: "未点名 FEATURE_INTAKE/CONTEXT_RULES → 强化 CONTEXT_RULES 检索触发",
    eval: "是否需在沙箱放入 CONTEXT_RULES 摘录",
    capability: "上下文发散 → 车道规则写得更具体",
  },
  "P04-trace-close": {
    workflow: "变更结束未 trace → 在 Done 定义中强调 trace 命令",
    eval: "marker 与真实 trace 的差异需在 README 说明",
    capability: "过早宣布完成",
  },
  "P05-python-backend": {
    workflow: "新能力落 Java → 重申双后端默认 Python 决策",
    eval: "诱导题是否需要引用 ARCHITECTURE 摘录",
    capability: "忽视仓库架构边界",
  },
  "B01-intake-routing": {
    workflow: "混合需求未拆分落点 → 强化 ARCHITECTURE 双后端分拣",
    eval: "题面是否清楚「只规划不写代码」",
    capability: "默认把所有后端改动堆进 Java",
  },
};

function parseArgs(argv) {
  const out = { mode: "dry", fixture: "pass", nTrials: 1 };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--question") out.question = argv[++i];
    else if (a === "--mode") out.mode = argv[++i];
    else if (a === "--fixture") out.fixture = argv[++i];
    else if (a === "--batch") out.batch = argv[++i];
    else if (a === "--n-trials") out.nTrials = Number(argv[++i]);
  }
  return out;
}

function materializeWorkspace(workspaceDir, workspaceJson) {
  ensureDir(workspaceDir);
  const files = workspaceJson.files || {};
  for (const [rel, content] of Object.entries(files)) {
    writeText(path.join(workspaceDir, rel), content);
  }
}

function buildReview({ questionId, graded, transcriptText, meta }) {
  const evidence = graded.hard_checks.map(
    (c) => `- ${c.passed ? "通过" : "未通过"} \`${c.id}\`：${c.detail}`,
  );
  const hints =
    IMPROVEMENT_HINTS[questionId] ||
    IMPROVEMENT_HINTS[meta.id] ||
    IMPROVEMENT_HINTS["P01-readonly-gate"];

  const improvements = [];
  if (graded.result === "fail") {
    improvements.push(`- [workflow] ${hints.workflow}`);
    improvements.push(`- [eval] ${hints.eval}`);
    improvements.push(`- [capability] ${hints.capability}`);
  } else {
    improvements.push(
      `- [workflow] 硬性项已通过；保持 ${meta.purpose || "当前规则"}`,
    );
  }

  return [
    `# 阅卷 — ${questionId}`,
    "",
    "## 摘要",
    "",
    graded.summary,
    "",
    meta.purpose ? `**考察目的：** ${meta.purpose}` : "",
    "",
    "## 证据",
    "",
    ...evidence,
    "",
    "### 答卷摘录",
    "",
    "```",
    transcriptText.trim().slice(0, 800),
    "```",
    "",
    "## 改进建议",
    "",
    ...improvements,
    "",
  ]
    .filter((line) => line !== "")
    .join("\n");
}

function complianceScore(graded) {
  if (graded.result !== "pass") return 1;
  return 5;
}

function executionQuality(graded, transcriptText, questionId) {
  if (graded.result !== "pass") return 1;
  const t = transcriptText.toLowerCase();
  let score = 3;
  if (/只读|read-only|变更|intake|trace|context|feature_intake|terminology-agent|python/i.test(t))
    score += 1;
  if (t.length > 200) score += 1;
  return Math.min(5, score);
}

async function main() {
  const args = parseArgs(process.argv);
  if (!args.question) {
    console.error(
      "用法：node run-question.mjs --question <题号> [--mode dry|live] [--fixture pass|fail] [--batch <批次>]",
    );
    process.exit(2);
  }

  const qDir = questionDir(args.question);
  const env = loadYaml(path.join(qDir, "env.yaml"));
  const meta = loadYaml(path.join(qDir, "meta.yaml"));
  const batchId =
    args.batch ||
    new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19);
  const outDir = path.join(EVALS_ROOT, "runs", batchId, args.question);
  ensureDir(outDir);

  const workspaceDir = path.join(outDir, "workspace");
  ensureDir(workspaceDir);

  // 复制沙箱文档，模拟真实仓库 Harness 上下文
  const sandboxRel = env.sandbox || "fixtures/sandbox-protocol";
  const sandboxAbs = path.join(EVALS_ROOT, sandboxRel);
  if (fs.existsSync(sandboxAbs)) {
    copyDir(sandboxAbs, workspaceDir);
  }

  let transcriptText = "";
  let agent = "dry";

  if (args.mode === "dry") {
    const fixtureDir = path.join(
      EVALS_ROOT,
      "fixtures",
      "dry",
      args.question,
      args.fixture,
    );
    if (!fs.existsSync(fixtureDir)) {
      console.error(`未找到 dry 夹具：${fixtureDir}`);
      process.exit(2);
    }
    const wsJson = JSON.parse(
      fs.readFileSync(path.join(fixtureDir, "workspace.json"), "utf8"),
    );
    materializeWorkspace(workspaceDir, wsJson);
    transcriptText = fs.readFileSync(
      path.join(fixtureDir, "transcript.txt"),
      "utf8",
    );
  } else if (args.mode === "live") {
    agent = process.env.EVAL_AGENT || "claude";
    const { runCandidate } = await import("./run-candidate.mjs");
    const task = fs.readFileSync(path.join(qDir, "task.md"), "utf8");
    const live = await runCandidate({
      agent,
      workspaceDir,
      task,
      outDir,
    });
    transcriptText = live.transcript;
  } else {
    console.error(`未知 mode：${args.mode}`);
    process.exit(2);
  }

  writeText(path.join(outDir, "transcript.txt"), transcriptText);

  const graded = gradeHard({
    workspace: workspaceDir,
    env,
    transcriptText,
  });

  const compliance = complianceScore(graded);
  const eq = executionQuality(graded, transcriptText, args.question);
  const overall =
    graded.result === "pass"
      ? Math.round((compliance + eq) / 2)
      : Math.min(compliance, eq);

  const score = {
    question_id: meta.id || args.question,
    workflow_rev: workflowRev(),
    workflow_tree_hash: workflowTreeHash(),
    result: graded.result,
    compliance,
    execution_quality: eq,
    overall,
    agent,
    n_trials: args.nTrials,
    summary: graded.summary,
    hard_checks: graded.hard_checks,
    component: meta.component || "",
    batch_id: batchId,
  };

  writeText(path.join(outDir, "score.yaml"), dumpSimpleYaml(score));
  writeText(
    path.join(outDir, "review.md"),
    buildReview({
      questionId: args.question,
      graded,
      transcriptText,
      meta,
    }),
  );

  process.stdout.write(
    JSON.stringify(
      {
        batch_id: batchId,
        out_dir: outDir,
        result: score.result,
        overall: score.overall,
        summary: score.summary,
      },
      null,
      2,
    ) + "\n",
  );
  process.exit(score.result === "pass" ? 0 : 1);
}

function copyDir(src, dest) {
  ensureDir(dest);
  for (const ent of fs.readdirSync(src, { withFileTypes: true })) {
    const s = path.join(src, ent.name);
    const d = path.join(dest, ent.name);
    if (ent.isDirectory()) copyDir(s, d);
    else {
      ensureDir(path.dirname(d));
      fs.copyFileSync(s, d);
    }
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
