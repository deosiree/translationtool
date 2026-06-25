#!/usr/bin/env node
/**
 * 在 Windows Terminal 分 pane 启动本地 dev 进程；无 wt 时打印手动命令。
 * 用法: node scripts/dev/launch.js dev:java
 */
const { spawnSync, execSync } = require("child_process");
const fs = require("fs");
const path = require("path");
const { resolveMode, getLocalPanes, BROWSER_URL, MODES } = require("./modes");
const { SERVICES } = require("./services");

const root = path.resolve(__dirname, "../..");

function findWt() {
  if (process.platform !== "win32") return null;
  const candidates = [
    process.env.WT_EXE,
    "wt.exe",
    path.join(
      process.env.LOCALAPPDATA || "",
      "Microsoft",
      "WindowsApps",
      "wt.exe"
    ),
  ].filter(Boolean);
  for (const c of candidates) {
    try {
      if (c.includes(path.sep) && fs.existsSync(c)) return c;
      execSync(`where ${c}`, { stdio: "ignore" });
      return c;
    } catch {
      /* try next */
    }
  }
  return null;
}

function buildPaneCommand(pane, modeName) {
  if (pane.layer !== "ui") {
    return `pnpm ${pane.script}`;
  }

  const cfg = MODES[modeName];
  const waitPorts = [];
  if (cfg.local.includes("java")) waitPorts.push(SERVICES.java.port);
  if (cfg.local.includes("agent")) waitPorts.push(SERVICES.agent.port);

  const startUi = path.join(root, "scripts", "dev", "start-ui.js");
  if (waitPorts.length === 0) {
    return `node "${startUi}" ${pane.script}`;
  }
  // 单条 node 命令，不含 ; —— wt 会把 ; 当成新 pane/新程序
  return `node "${startUi}" ${waitPorts.join(" ")} ${pane.script}`;
}

function printManual(panes, modeName) {
  console.log("[launch] Windows Terminal (wt.exe) not found.");
  console.log("[launch] Start local processes manually:");
  for (const pane of panes) {
    const dir = path.join(root, pane.dir);
    console.log(`  cd ${dir}`);
    console.log(`  ${buildPaneCommand(pane, modeName)}`);
  }
  console.log(`[launch] Open ${BROWSER_URL}`);
}

function launchWt(panes, modeName) {
  const ps1 = path.join(__dirname, "launch-wt.ps1");
  const payload = JSON.stringify({
    panes: panes.map((p) => ({
      title: p.title,
      dir: path.join(root, p.dir),
      command: buildPaneCommand(p, modeName),
    })),
  });
  const wt = findWt();
  if (!wt) {
    printManual(panes, modeName);
    return;
  }

  const result = spawnSync(
    "powershell",
    [
      "-NoProfile",
      "-ExecutionPolicy",
      "Bypass",
      "-File",
      ps1,
      "-PayloadJson",
      payload,
    ],
    { cwd: root, stdio: "inherit", shell: false }
  );

  if (result.status !== 0) {
    console.warn("[launch] wt launch failed, falling back to manual commands");
    printManual(panes, modeName);
    return;
  }

  const titles = panes.map((p) => p.title).join(", ");
  console.log(`[launch] opened Windows Terminal (${panes.length} pane(s)): ${titles}`);
  console.log(`[launch] Open ${BROWSER_URL}`);
}

function main() {
  const arg = process.argv[2];
  const modeName = resolveMode(arg);
  if (!modeName) {
    console.error(`[launch] unknown mode: ${arg}`);
    process.exit(1);
  }

  const panes = getLocalPanes(modeName);
  if (panes.length === 0) {
    console.log(`[launch] no local panes for ${modeName}`);
    console.log(`[launch] Open ${BROWSER_URL}`);
    return;
  }

  if (process.platform === "win32") {
    launchWt(panes, modeName);
  } else {
    printManual(panes, modeName);
  }
}

main();
