#!/usr/bin/env node
/**
 * 准备 dev 全栈：互斥停本地/Docker、健康检查、按需 docker compose up。
 * 用法: node scripts/dev/prepare-stack.js dev:java
 */
const { execSync } = require("child_process");
const http = require("http");
const path = require("path");
const {
  resolveMode,
  expandDockerLayers,
  formatStackSummary,
  BROWSER_URL,
} = require("./modes");
const { writeUiNginxConfig } = require("./ui-nginx");
const { SERVICES } = require("./services");

const root = path.resolve(__dirname, "../..");

function log(status, service, reason = "") {
  const suffix = reason ? ` (${reason})` : "";
  console.log(`[prepare] ${service}: ${status}${suffix}`);
}

function run(cmd, opts = {}) {
  return execSync(cmd, { cwd: root, encoding: "utf8", stdio: opts.inherit ? "inherit" : "pipe", ...opts });
}

function containerRunning(container) {
  try {
    return run(`docker inspect -f "{{.State.Running}}" ${container}`).trim() === "true";
  } catch {
    return false;
  }
}

function containerHealth(container) {
  try {
    if (!containerRunning(container)) return "not_running";
    const status = run(
      `docker inspect -f "{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}" ${container}`
    ).trim();
    return status;
  } catch {
    return "missing";
  }
}

function checkHttpHealth(port, healthPath = "/") {
  return new Promise((resolve) => {
    const req = http.get(
      { hostname: "127.0.0.1", port, path: healthPath, timeout: 2000 },
      (res) => {
        resolve(res.statusCode >= 200 && res.statusCode < 500);
        res.resume();
      }
    );
    req.on("error", () => resolve(false));
    req.on("timeout", () => {
      req.destroy();
      resolve(false);
    });
  });
}

async function isDockerServiceReady(layerKey) {
  const svc = SERVICES[layerKey];
  if (!svc) return false;
  const health = containerHealth(svc.container);
  if (health === "healthy") return true;
  if (health === "none" && containerRunning(svc.container)) {
    if (layerKey === "ui") {
      return checkHttpHealth(svc.port);
    }
    if (layerKey === "java") {
      return checkHttpHealth(svc.port);
    }
    if (layerKey === "agent") {
      return checkHttpHealth(svc.port, svc.healthPath);
    }
    return true;
  }
  if (health === "starting" || health === "unhealthy") return false;
  return false;
}

function stopDockerContainer(layerKey) {
  const svc = SERVICES[layerKey];
  if (!svc?.container) return;
  if (!containerRunning(svc.container)) {
    log("skip stop", svc.container, "not running");
    return;
  }
  try {
    run(`docker stop ${svc.container}`, { inherit: true });
    log("stopped Docker", svc.container);
  } catch (err) {
    log("FAIL stop", svc.container, err.message);
    throw err;
  }
}

function killLocalPort(port) {
  if (process.platform === "win32") {
    try {
      const ps = `Get-NetTCPConnection -LocalPort ${port} -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique`;
      const out = run(`powershell -NoProfile -Command "${ps}"`).trim();
      const pids = [...new Set(out.split(/\s+/).filter((p) => p && p !== "0" && p !== "4"))];
      for (const pid of pids) {
        try {
          run(`taskkill /PID ${pid} /F`);
          log("stopped local", `:${port}`, `pid ${pid}`);
        } catch {
          /* ignore */
        }
      }
      if (pids.length === 0) {
        log("skip kill", `:${port}`, "no listener");
      }
    } catch {
      log("skip kill", `:${port}`, "no listener");
    }
    return;
  }
  try {
    run(`lsof -ti :${port} | xargs -r kill -9`);
    log("stopped local", `:${port}`);
  } catch {
    log("skip kill", `:${port}`, "no listener");
  }
}

function stopLocalLayer(layerKey) {
  const svc = SERVICES[layerKey];
  if (!svc) return;
  if (layerKey === "ui") {
    killLocalPort(svc.port);
  } else if (svc.port) {
    killLocalPort(svc.port);
  }
}

async function ensureDockerLayer(
  layerKey,
  composeExtraArgs = "",
  forceRecreate = false,
  noDeps = false
) {
  const svc = SERVICES[layerKey];
  if (!svc) return;

  if (!forceRecreate && (await isDockerServiceReady(layerKey))) {
    log("healthy, skip", svc.compose || svc.container);
    return;
  }

  const composePrefix = composeExtraArgs
    ? `docker compose ${composeExtraArgs}`
    : "docker compose";
  const recreateFlag = forceRecreate ? " --force-recreate" : "";
  const noDepsFlag = noDeps ? " --no-deps" : "";

  try {
    log("starting", svc.compose);
    run(`${composePrefix} up -d${recreateFlag}${noDepsFlag} ${svc.compose}`, {
      inherit: true,
    });
  } catch (err) {
    log("FAIL", svc.compose, err.message);
    throw new Error(`docker compose up -d ${svc.compose} failed`);
  }

  const deadline = Date.now() + 120_000;
  while (Date.now() < deadline) {
    if (await isDockerServiceReady(layerKey)) {
      log("OK", svc.compose);
      return;
    }
    await sleep(2000);
  }
  throw new Error(`${svc.compose} did not become ready within 120s`);
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

async function main() {
  const arg = process.argv[2];
  const modeName = resolveMode(arg);
  if (!modeName) {
    console.error(`[prepare] unknown mode: ${arg}`);
    console.error("Usage: node scripts/dev/prepare-stack.js dev:java");
    process.exit(1);
  }

  const { MODES } = require("./modes");
  const cfg = MODES[modeName];
  const localLayers = [...cfg.local];
  const dockerLayers = expandDockerLayers(cfg.docker);

  console.log(`[prepare] mode=${modeName}`);

  for (const layer of localLayers) {
    stopDockerContainer(layer);
    stopLocalLayer(layer);
  }

  for (const layer of dockerLayers) {
    if (localLayers.includes(layer)) continue;
    stopLocalLayer(layer);
  }

  for (const layer of dockerLayers) {
    if (localLayers.includes(layer)) continue;

    const needsUiOverride =
      layer === "ui" &&
      (localLayers.includes("java") || localLayers.includes("agent"));
    const uiNoDeps =
      layer === "ui" &&
      (localLayers.includes("java") || localLayers.includes("agent"));
    let composeExtra = "";
    if (needsUiOverride) {
      writeUiNginxConfig(root, localLayers);
      composeExtra =
        "-f docker-compose.yml -f scripts/dev/docker-compose.ui-dev.yml";
      log("ui nginx", "host.docker.internal override");
      await ensureDockerLayer(layer, composeExtra, true, uiNoDeps);
      continue;
    }

    await ensureDockerLayer(layer, composeExtra, false, uiNoDeps);
  }

  console.log(`[prepare] stack: ${formatStackSummary(modeName)}`);
  console.log(`[prepare] Open ${BROWSER_URL}`);
}

main().catch((err) => {
  console.error(`[prepare] FAIL: ${err.message}`);
  process.exit(1);
});
