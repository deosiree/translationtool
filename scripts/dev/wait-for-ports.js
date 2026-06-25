#!/usr/bin/env node
/**
 * 等待本机端口就绪（供 UI pane 在 webpack 启动前等待 Java/Agent）。
 * 用法: node scripts/dev/wait-for-ports.js 18001 18002
 */
const net = require("net");

const ports = process.argv.slice(2).map((p) => Number(p)).filter(Boolean);
const TIMEOUT_MS = 180_000;
const INTERVAL_MS = 2_000;

function checkPort(port) {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    const done = (ok) => {
      socket.destroy();
      resolve(ok);
    };
    socket.setTimeout(1500);
    socket.once("connect", () => done(true));
    socket.once("timeout", () => done(false));
    socket.once("error", () => done(false));
    socket.connect(port, "127.0.0.1");
  });
}

async function main() {
  if (ports.length === 0) {
    console.error("[wait] usage: node wait-for-ports.js 18001 [18002 ...]");
    process.exit(1);
  }

  console.log(`[wait] waiting for :${ports.join(", :")} (max ${TIMEOUT_MS / 1000}s)`);
  const deadline = Date.now() + TIMEOUT_MS;

  while (Date.now() < deadline) {
    const results = await Promise.all(ports.map(checkPort));
    if (results.every(Boolean)) {
      console.log("[wait] all ports ready");
      process.exit(0);
    }
    await new Promise((r) => setTimeout(r, INTERVAL_MS));
  }

  console.error(`[wait] timeout — still not listening: :${ports.join(", :")}`);
  console.error("[wait] check Java/Agent panes for startup errors");
  process.exit(1);
}

main();
