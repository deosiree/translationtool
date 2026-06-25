#!/usr/bin/env node
/**
 * 首次本地开发：检查 .env 文件是否就绪，并打印下一步命令。
 */
const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");

const checks = [
  {
    example: "translationtoolservice/.env.local.example",
    target: "translationtoolservice/.env.local",
    hint: "pnpm -C translationtoolservice dev",
  },
  {
    example: "terminology-agent/.env.example",
    target: "terminology-agent/.env",
    hint: "填入 LLM_API_KEY",
  },
];

let ok = true;

for (const { example, target, hint } of checks) {
  const examplePath = path.join(root, example);
  const targetPath = path.join(root, target);
  if (!fs.existsSync(targetPath)) {
    ok = false;
    if (fs.existsSync(examplePath)) {
      fs.copyFileSync(examplePath, targetPath);
      console.log(`[setup] 已创建 ${target}（从 example 复制）`);
    } else {
      console.warn(`[setup] 缺少 ${target}，且无 ${example}`);
    }
  } else {
    console.log(`[setup] OK  ${target}`);
  }
  console.log(`         → ${hint}`);
}

const envLocal = path.join(root, "translationtoolservice/.env.local");
if (fs.existsSync(envLocal)) {
  const content = fs.readFileSync(envLocal, "utf8");
  if (/SERVER_PORT\s*=\s*18101/.test(content)) {
    console.warn(
      "[setup] ⚠ translationtoolservice/.env.local 仍为 SERVER_PORT=18101"
    );
    console.warn("         请改为 SERVER_PORT=18001（与 Docker 同端口，prepare 互斥）");
  }
}

console.log("");
if (ok) {
  console.log("环境文件已就绪。建议：");
} else {
  console.log("已生成缺失的 .env 文件，请按需编辑后执行：");
}
console.log("  pnpm dev          # 全本地 UI+Java+Agent → http://localhost:18000");
console.log("  pnpm dev:ui       # 只改前端（webpack 热更新）");
console.log("  pnpm dev:java     # 只改 Java（UI/Agent 走 Docker）");
console.log("  pnpm dev:agent    # 只改 Agent");
console.log("");
console.log("  浏览器统一入口: http://localhost:18000");
console.log("  需安装 Windows Terminal (wt.exe) 以自动分 pane 启动本地进程");
