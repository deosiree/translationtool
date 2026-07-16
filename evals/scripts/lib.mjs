import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export const EVALS_ROOT = path.resolve(__dirname, "..");
export const REPO_ROOT = path.resolve(EVALS_ROOT, "..");

/** Harness-relevant paths for workflow_tree_hash (design §7). */
export const WORKFLOW_PATHS = [
  "AGENTS.md",
  "docs/HARNESS.md",
  "docs/HARNESS_MATURITY.md",
  "docs/HARNESS_COMPONENTS.md",
  "docs/HARNESS_AUDIT.md",
  "docs/HARNESS_BACKLOG.md",
  "docs/FEATURE_INTAKE.md",
  "docs/CONTEXT_RULES.md",
  "docs/TRACE_SPEC.md",
  "docs/IMPROVEMENT_PROTOCOL.md",
  "scripts/agent-harness-block.md",
  "scripts/claude-harness-block.md",
];

export function repoRoot() {
  return REPO_ROOT;
}

export function runGit(args, cwd = REPO_ROOT) {
  const r = spawnSync("git", args, {
    cwd,
    encoding: "utf8",
    shell: false,
  });
  if (r.error) throw r.error;
  if (r.status !== 0) {
    const err = (r.stderr || r.stdout || "").trim();
    throw new Error(`git ${args.join(" ")} failed: ${err}`);
  }
  return (r.stdout || "").trim();
}

export function workflowRev() {
  return runGit(["rev-parse", "HEAD"]);
}

/**
 * Stable hash over existing workflow paths at HEAD.
 * Missing paths are skipped.
 */
export function workflowTreeHash() {
  const parts = [];
  for (const p of WORKFLOW_PATHS) {
    const full = path.join(REPO_ROOT, p);
    if (!fs.existsSync(full)) continue;
    try {
      const h = runGit(["rev-parse", `HEAD:${p.replace(/\\/g, "/")}`]);
      parts.push(`${p}:${h}`);
    } catch {
      const body = fs.readFileSync(full, "utf8");
      parts.push(`${p}:wt:${hashString(body)}`);
    }
  }
  for (const dir of [".cursor/rules", ".cursor/skills"]) {
    const abs = path.join(REPO_ROOT, dir);
    if (!fs.existsSync(abs)) continue;
    const files = walkFiles(abs).sort();
    for (const f of files) {
      const rel = path.relative(REPO_ROOT, f).replace(/\\/g, "/");
      const body = fs.readFileSync(f, "utf8");
      parts.push(`${rel}:wt:${hashString(body)}`);
    }
  }
  return hashString(parts.join("\n"));
}

export function walkFiles(dir) {
  const out = [];
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) out.push(...walkFiles(p));
    else out.push(p);
  }
  return out;
}

export function hashString(s) {
  let h = 0xcbf29ce484222325n;
  const prime = 0x100000001b3n;
  for (let i = 0; i < s.length; i++) {
    h ^= BigInt(s.charCodeAt(i));
    h = (h * prime) & 0xffffffffffffffffn;
  }
  return h.toString(16).padStart(16, "0");
}

function parseScalar(v) {
  if (v === "true") return true;
  if (v === "false") return false;
  if (v === "null" || v === "~") return null;
  if (/^-?\d+$/.test(v)) return Number(v);
  if (
    (v.startsWith('"') && v.endsWith('"')) ||
    (v.startsWith("'") && v.endsWith("'"))
  ) {
    return v.slice(1, -1);
  }
  return v;
}

function nextMeaningful(lines, fromIdx) {
  for (let i = fromIdx + 1; i < lines.length; i++) {
    const t = lines[i];
    if (t.trim() === "" || /^\s*#/.test(t)) continue;
    return { line: t, index: i };
  }
  return null;
}

/** Minimal YAML: maps, nested maps, list of scalars or list of maps. */
export function parseSimpleYaml(text) {
  const lines = text.replace(/\r\n/g, "\n").split("\n");
  const root = {};
  const stack = [{ indent: -1, container: root, type: "map" }];

  function top() {
    return stack[stack.length - 1];
  }

  for (let i = 0; i < lines.length; i++) {
    const raw = lines[i];
    if (/^\s*#/.test(raw) || raw.trim() === "") continue;
    const indent = raw.match(/^ */)[0].length;
    const line = raw.trim();

    while (stack.length > 1 && indent <= top().indent) stack.pop();
    const ctx = top();

    if (line.startsWith("- ")) {
      const rest = line.slice(2).trim();
      if (ctx.type !== "list") {
        throw new Error(`Unexpected list item: ${line}`);
      }
      if (/^[\w.-]+\s*:/.test(rest)) {
        const obj = {};
        const cidx = rest.indexOf(":");
        const k = rest.slice(0, cidx).trim();
        const v = rest.slice(cidx + 1).trim();
        if (v === "") {
          obj[k] = {};
          ctx.container.push(obj);
          stack.push({ indent, container: obj[k], type: "map" });
        } else {
          obj[k] = parseScalar(v);
          ctx.container.push(obj);
          stack.push({ indent, container: obj, type: "map" });
        }
      } else {
        ctx.container.push(parseScalar(rest));
      }
      continue;
    }

    const cidx = line.indexOf(":");
    if (cidx < 0) continue;
    const key = line.slice(0, cidx).trim();
    const val = line.slice(cidx + 1).trim();

    if (ctx.type === "list") {
      const last = ctx.container[ctx.container.length - 1];
      if (!last || typeof last !== "object" || Array.isArray(last)) {
        throw new Error(`Cannot attach key ${key} to list scalar`);
      }
      if (val === "") {
        const peek = nextMeaningful(lines, i);
        if (peek && peek.line.trim().startsWith("- ")) {
          last[key] = [];
          stack.push({
            indent,
            container: last[key],
            type: "list",
          });
        } else {
          last[key] = {};
          stack.push({ indent, container: last[key], type: "map" });
        }
      } else {
        last[key] = parseScalar(val);
      }
      continue;
    }

    if (val === "") {
      const peek = nextMeaningful(lines, i);
      if (peek && peek.line.trim().startsWith("- ")) {
        ctx.container[key] = [];
        stack.push({ indent, container: ctx.container[key], type: "list" });
      } else {
        ctx.container[key] = {};
        stack.push({ indent, container: ctx.container[key], type: "map" });
      }
    } else {
      ctx.container[key] = parseScalar(val);
    }
  }

  return root;
}

export function loadYaml(filePath) {
  return parseSimpleYaml(fs.readFileSync(filePath, "utf8"));
}

export function dumpSimpleYaml(obj, indent = 0) {
  const pad = "  ".repeat(indent);
  const lines = [];
  for (const [k, v] of Object.entries(obj)) {
    if (Array.isArray(v)) {
      lines.push(`${pad}${k}:`);
      for (const item of v) {
        if (item !== null && typeof item === "object") {
          const keys = Object.keys(item);
          const [fk, ...rest] = keys;
          lines.push(`${pad}  - ${fk}: ${formatScalar(item[fk])}`);
          for (const rk of rest) {
            if (typeof item[rk] === "object" && item[rk] !== null) {
              lines.push(`${pad}    ${rk}:`);
              // shallow only for nested under list item
              for (const [nk, nv] of Object.entries(item[rk])) {
                lines.push(`${pad}      ${nk}: ${formatScalar(nv)}`);
              }
            } else {
              lines.push(`${pad}    ${rk}: ${formatScalar(item[rk])}`);
            }
          }
        } else {
          lines.push(`${pad}  - ${formatScalar(item)}`);
        }
      }
    } else if (v !== null && typeof v === "object") {
      lines.push(`${pad}${k}:`);
      lines.push(dumpSimpleYaml(v, indent + 1));
    } else {
      lines.push(`${pad}${k}: ${formatScalar(v)}`);
    }
  }
  return lines.filter((l) => l !== "").join("\n") + "\n";
}

function formatScalar(v) {
  if (typeof v === "string") {
    if (/[:#\n]/.test(v) || v.includes('"')) return JSON.stringify(v);
    return v;
  }
  return String(v);
}

export function questionDir(questionId) {
  const protocol = path.join(EVALS_ROOT, "suites", "protocol", questionId);
  if (fs.existsSync(protocol)) return protocol;
  const product = path.join(EVALS_ROOT, "suites", "product", questionId);
  if (fs.existsSync(product)) return product;
  throw new Error(`Question not found: ${questionId}`);
}

export function ensureDir(p) {
  fs.mkdirSync(p, { recursive: true });
}

export function writeText(file, text) {
  ensureDir(path.dirname(file));
  fs.writeFileSync(file, text, "utf8");
}

export function resolveUnder(base, rel) {
  const root = path.resolve(base);
  const full = path.resolve(base, rel);
  const relToBase = path.relative(root, full);
  if (relToBase.startsWith("..") || path.isAbsolute(relToBase)) {
    throw new Error(`Path escapes base: ${rel}`);
  }
  return full;
}
