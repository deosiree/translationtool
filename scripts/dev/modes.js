const { BROWSER_URL } = require("./services");

/**
 * 7 种根目录 dev 命令的单一配置源。
 * local / docker 均含 ui、java、agent、infra（infra 展开为 mysql+redis）。
 */
const MODES = {
  "dev:ui": {
    local: ["ui"],
    docker: ["java", "agent", "infra"],
  },
  "dev:java": {
    local: ["java"],
    docker: ["ui", "agent", "infra"],
  },
  "dev:agent": {
    local: ["agent"],
    docker: ["ui", "java", "infra"],
  },
  "dev:ui-java": {
    local: ["ui", "java"],
    docker: ["agent", "infra"],
  },
  "dev:ui-agent": {
    local: ["ui", "agent"],
    docker: ["java", "infra"],
  },
  "dev:java-agent": {
    local: ["java", "agent"],
    docker: ["ui", "infra"],
  },
  dev: {
    local: ["ui", "java", "agent"],
    docker: ["infra"],
  },
};

function listModes() {
  return Object.keys(MODES);
}

function resolveMode(name) {
  const key = name.startsWith("dev") ? name : `dev:${name}`;
  if (MODES[key]) return key;
  if (MODES[name]) return name;
  return null;
}

function expandDockerLayers(docker) {
  const out = [];
  for (const layer of docker) {
    if (layer === "infra") {
      out.push("mysql", "redis");
    } else {
      out.push(layer);
    }
  }
  const order = { mysql: 0, redis: 1, java: 2, agent: 3, ui: 4 };
  return [...new Set(out)].sort((a, b) => (order[a] ?? 9) - (order[b] ?? 9));
}

function getLocalPanes(modeName) {
  const cfg = MODES[modeName];
  if (!cfg) return [];
  const panes = [];
  // 后端先启动，UI 最后（launch 里 UI 会 wait-for-ports）
  const order = ["java", "agent", "ui"];
  for (const layer of order) {
    if (!cfg.local.includes(layer)) continue;
    if (layer === "ui") {
      panes.push({ layer: "ui", title: "UI", dir: "translation", script: "serve" });
    } else if (layer === "java") {
      panes.push({
        layer: "java",
        title: "Java",
        dir: "translationtoolservice",
        script: "dev",
      });
    } else if (layer === "agent") {
      panes.push({
        layer: "agent",
        title: "Agent",
        dir: "terminology-agent",
        script: "dev",
      });
    }
  }
  return panes;
}

function formatStackSummary(modeName) {
  const cfg = MODES[modeName];
  const parts = [];
  const fmt = (layer, where) => {
    if (layer === "ui") return `UI=${where}:18000`;
    if (layer === "java") return `Java=${where}:18001`;
    if (layer === "agent") return `Agent=${where}:18002`;
    return null;
  };
  for (const layer of ["ui", "java", "agent"]) {
    const where = cfg.local.includes(layer) ? "local" : "docker";
    parts.push(fmt(layer, where));
  }
  parts.push("infra=docker");
  return parts.join(" ");
}

module.exports = {
  MODES,
  BROWSER_URL,
  listModes,
  resolveMode,
  expandDockerLayers,
  getLocalPanes,
  formatStackSummary,
};
