#!/usr/bin/env node
/**
 * UI pane 入口：先等后端端口，再启动 webpack（避免 wt 把 ; 当分 pane 符）。
 * 用法: node scripts/dev/start-ui.js 18001 18002 dev
 */
const { spawnSync } = require("child_process");
const path = require("path");

const root = path.resolve(__dirname, "../..");
const translationDir = path.join(root, "translation");
const waitScript = path.join(__dirname, "wait-for-ports.js");

function main() {
  const args = process.argv.slice(2);
  if (args.length < 2) {
    console.error("[start-ui] usage: node start-ui.js [ports...] <pnpm-script>");
    console.error("[start-ui] example: node start-ui.js 18001 18002 dev");
    process.exit(1);
  }

  const script = args[args.length - 1];
  const ports = args.slice(0, -1).map((p) => Number(p)).filter(Boolean);

  if (ports.length > 0) {
    const wait = spawnSync("node", [waitScript, ...ports.map(String)], {
      cwd: root,
      stdio: "inherit",
    });
    if (wait.status !== 0) {
      process.exit(wait.status ?? 1);
    }
  }

  console.log(`[start-ui] pnpm ${script}`);
  const run = spawnSync("pnpm", [script], {
    cwd: translationDir,
    stdio: "inherit",
    shell: true,
  });
  process.exit(run.status ?? 1);
}

main();
