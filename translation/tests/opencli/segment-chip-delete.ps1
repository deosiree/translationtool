# 术语学习页：切分 chip 删除（删一不丢二）opencli 集成测
# 前置：UI http://127.0.0.1:18000；opencli doctor 绿灯
# 登录：admin / admin123（若已登录，脚本会复用会话；也可先手动登录后 bind）
# 用法：pwsh -File translation/tests/opencli/segment-chip-delete.ps1

$ErrorActionPreference = "Stop"
$Session = "segchip"
$Base = "http://127.0.0.1:18000"
$TermUrl = "$Base/#/translate/terminologyAgent"

function Invoke-OpenCli([string[]]$CliArgs) {
  $out = & opencli @CliArgs 2>&1 | Out-String
  Write-Host $out
  return $out
}

Write-Host "== opencli doctor =="
& opencli doctor
if ($LASTEXITCODE -ne 0) { throw "opencli doctor failed" }

Write-Host "== open terminologyAgent =="
Invoke-OpenCli @("browser", $Session, "open", $TermUrl) | Out-Null
Start-Sleep -Seconds 2

$state = Invoke-OpenCli @("browser", $Session, "state")
if ($state -match "请输入用户名|欢迎登录") {
  Write-Host "== login admin / admin123 =="
  Invoke-OpenCli @("browser", $Session, "fill", "input[placeholder='请输入用户名']", "admin") | Out-Null
  Invoke-OpenCli @("browser", $Session, "fill", "input[placeholder='请输入密码']", "admin123") | Out-Null
  Invoke-OpenCli @("browser", $Session, "click", "button[type=submit]") | Out-Null
  Start-Sleep -Seconds 3
  Invoke-OpenCli @("browser", $Session, "open", $TermUrl) | Out-Null
  Start-Sleep -Seconds 2
}

Write-Host "== seed dirty segment_trace + show 切分列 =="
$seedJs = @'
(() => {
  const dirty = {
    id: "opencli-chip-001",
    entry_info_id: "opencli-entry-001",
    task_id: "opencli-task-001",
    source_text: "文件、系统、资源",
    entry_comment: "",
    suggested_translation: "File System Resource",
    target_lang: "英文",
    task_name: "【opencli】切分chip",
    product_name: "admin",
    department: "通用平台部",
    confidence: 0.5,
    similar_terms: [],
    retrieval_method: "decomposed",
    llm_reasoning: "opencli seed",
    review_status: "pending",
    source_type: "workbench_agent",
    created_at: "2026-07-20 18:00:00",
    segment_trace: {
      jieba: ["文件", "、", "系统", "、", "资源"],
      display: "文件 | 、 | 系统 | 、 | 资源"
    },
    _local: true
  };
  const key = "agent-pending-audits";
  let list = [];
  try { list = JSON.parse(localStorage.getItem(key) || "[]"); } catch (e) { list = []; }
  if (!Array.isArray(list)) list = [];
  list = list.filter((x) => x && x.id !== dirty.id);
  list.unshift(dirty);
  localStorage.setItem(key, JSON.stringify(list));
  const cols = ["index","source_text","entry_comment","suggested_translation","confidence","similar_terms","retrieval_method","translation_source","llm_reasoning","segment_trace","created_at","action"];
  localStorage.setItem("colPref-termAudit", JSON.stringify({ displayColumn: cols.join(",") }));
  return { seeded: true, n: list.length };
})()
'@
Invoke-OpenCli @("browser", $Session, "eval", $seedJs) | Out-Null
Invoke-OpenCli @("browser", $Session, "open", $TermUrl) | Out-Null
Start-Sleep -Seconds 3

Write-Host "== enter edit via dblclick =="
$editJs = @'
(() => {
  const rows = Array.from(document.querySelectorAll("tr.ant-table-row"));
  const row = rows.find((r) => (r.textContent || "").includes("文件、系统、资源"));
  if (!row) return { ok: false, reason: "row-not-found", rowCount: rows.length };
  row.dispatchEvent(new MouseEvent("dblclick", { bubbles: true, cancelable: true, view: window }));
  return { ok: true };
})()
'@
$edit = Invoke-OpenCli @("browser", $Session, "eval", $editJs)
Start-Sleep -Seconds 1
if ($edit -notmatch '"ok":\s*true') { throw "failed to enter edit: $edit" }

Write-Host "== remove first 、 and assert delete-one-only =="
$removeJs = @'
(() => {
  const read = () => Array.from(document.querySelectorAll(".segment-chip")).map((c) =>
    (c.querySelector(".segment-chip__text") || c).textContent.trim()
  );
  const before = read();
  if (before.length !== 5) return Promise.resolve({ ok: false, reason: "expected-5", before });
  const target = Array.from(document.querySelectorAll(".segment-chip")).find((c) =>
    (c.querySelector(".segment-chip__text") || c).textContent.trim() === "、"
  );
  if (!target) return Promise.resolve({ ok: false, reason: "comma-missing", before });
  target.querySelector(".segment-chip__remove").click();
  return new Promise((resolve) => {
    setTimeout(() => {
      const after = read();
      resolve({
        ok: true,
        before,
        after,
        removedExactlyOne: before.length - after.length === 1,
        keptSystem: after.includes("系统"),
        keptResource: after.includes("资源")
      });
    }, 120);
  });
})()
'@
$after = Invoke-OpenCli @("browser", $Session, "eval", $removeJs)

if ($after -notmatch '"ok":\s*true') { throw "remove failed: $after" }
if ($after -notmatch '"removedExactlyOne":\s*true') { throw "did not remove exactly one: $after" }
if ($after -notmatch '"keptSystem":\s*true' -or $after -notmatch '"keptResource":\s*true') {
  throw "neighbor tokens lost: $after"
}

Write-Host "PASS: segment chip delete removes exactly one tag; neighbors kept"
Invoke-OpenCli @("browser", $Session, "close") | Out-Null
exit 0
