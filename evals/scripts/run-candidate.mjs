#!/usr/bin/env node
/**
 * Live 考生适配器。默认：Claude Code 无头模式（claude -p）。
 * 找不到 CLI 时以 code 2 退出，不会静默通过。
 */
import { spawnSync } from "node:child_process";
import path from "node:path";
import { writeText } from "./lib.mjs";

export async function runCandidate({ agent, workspaceDir, task, outDir }) {
  const prompt = [
    "【考试指令】本条消息就是完整题面，请立即执行，不要寒暄，不要反问「需要做什么」。",
    "仅在本工作区内操作；严格遵守 AGENTS.md 中的请求分类（只读 / 变更）。",
    "如需写文件请直接写入；完成后用中文简要总结你做了什么或你的答案。",
    "",
    task,
  ].join("\n");

  writeText(path.join(outDir, "prompt.txt"), prompt);

  if (agent === "claude") {
    const which = spawnSync("claude", ["--version"], {
      encoding: "utf8",
      shell: true,
    });
    if (which.status !== 0) {
      const msg =
        "EVAL_AGENT=claude 但未找到 `claude` CLI。请安装 Claude Code，或使用 --mode dry。";
      writeText(path.join(outDir, "error.txt"), msg);
      const err = new Error(msg);
      err.code = 2;
      throw err;
    }

    // Windows 下通过 stdin 传 prompt，避免 shell 转义破坏题面。
    // 沙箱题需创建 marker 文件，启用 skip-permissions。
    const r = spawnSync(
      "claude",
      ["-p", "--output-format", "text", "--dangerously-skip-permissions"],
      {
        cwd: workspaceDir,
        encoding: "utf8",
        shell: true,
        input: prompt,
        timeout: 600000,
        maxBuffer: 10 * 1024 * 1024,
      },
    );
    const transcript = [r.stdout || "", r.stderr || ""]
      .join("\n")
      .replace(/^Warning: no stdin data received.*$/gm, "")
      .trim();
    writeText(path.join(outDir, "claude-exit.txt"), String(r.status ?? ""));
    if (r.status !== 0 && !transcript) {
      throw new Error(`claude 退出码 ${r.status}：${r.stderr || ""}`);
    }
    return { transcript: transcript || "（空答卷）" };
  }

  if (agent === "cursor") {
    const msg =
      "Cursor live 适配器尚未配置（Phase 1）。请使用 EVAL_AGENT=claude 或 --mode dry。";
    writeText(path.join(outDir, "error.txt"), msg);
    const err = new Error(msg);
    err.code = 2;
    throw err;
  }

  throw new Error(`未知 EVAL_AGENT：${agent}`);
}
