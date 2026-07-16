# Harness Eval Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (inline) or subagent-driven-development. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a runnable Harness Eval scaffold with protocol Wave 1 question P01 end-to-end (dry-fixture + live agent hook), score/history pipeline, then fill P02–P05 and maturity doc links.

**Architecture:** Markdown/YAML question packs under `evals/suites/`; Node scripts prepare sandboxes, compute `workflow_tree_hash`, grade deterministic hard checks, optionally invoke `EVAL_AGENT` headless; promptfoo optional later. Live agent is optional; dry fixtures prove the exam loop without API cost.

**Tech Stack:** Node.js (scripts, no new workspace package required), YAML/Markdown question format, optional `npx promptfoo` later, Claude Code headless as default live adapter.

---

## File map

| Path | Responsibility |
| --- | --- |
| `evals/README.md` | How to run exams |
| `evals/suites/protocol/P01-readonly-gate/` | First protocol question (4 files) |
| `evals/fixtures/sandbox-protocol/` | Minimal harness docs for isolated runs |
| `evals/fixtures/dry/P01-pass/` / `P01-fail/` | Recorded before/after for dry grading |
| `evals/scripts/lib.mjs` | Shared helpers (hash, yaml load, paths) |
| `evals/scripts/workflow-hash.mjs` | Print `workflow_rev` + `workflow_tree_hash` |
| `evals/scripts/grade-hard.mjs` | Deterministic hard-check grader |
| `evals/scripts/ingest-scores.mjs` | Append to `evals/history/score-history.yaml` + batch summary |
| `evals/scripts/run-question.mjs` | Orchestrate one question: env check → (dry\|live) → grade → score/review |
| `evals/templates/score.schema.yaml` | Documented score shape |
| `evals/templates/review.md` | Review template |
| `evals/history/score-history.yaml` | Append-only history (committed empty/header) |
| `evals/runs/` | gitignored batch outputs |
| `.gitignore` | Ignore `evals/runs/` |

---

### Task 1: Scaffold + gitignore + README

**Files:**
- Create: `evals/README.md`
- Create: `evals/history/.gitkeep` and `evals/history/score-history.yaml`
- Create: `evals/suites/protocol/.gitkeep`, `evals/suites/product/.gitkeep`
- Modify: `.gitignore` — add `evals/runs/`

- [ ] **Step 1: Update `.gitignore`**

Append:

```
# Harness eval run artifacts
evals/runs/
```

- [ ] **Step 2: Write `evals/README.md`** with run commands for dry and live modes.

- [ ] **Step 3: Write empty history file**

```yaml
# Append-only harness eval scores. Do not edit by hand unless repairing.
entries: []
```

- [ ] **Step 4: Verify dirs exist**

Run: `Get-ChildItem -Recurse evals | Select-Object FullName`

---

### Task 2: Shared library + workflow hash

**Files:**
- Create: `evals/scripts/lib.mjs`
- Create: `evals/scripts/workflow-hash.mjs`
- Test: run workflow-hash and assert JSON keys

- [ ] **Step 1: Implement `lib.mjs`** with:
  - `WORKFLOW_PATHS` constant matching the design doc
  - `repoRoot()`, `runGit(args)`, `workflowRev()`, `workflowTreeHash()`, `loadYaml()` (minimal YAML for our flat files — use regex/simple parse OR rely on Node without deps: ship JSON sidecars for fixtures; for yaml use a tiny parser for key: value only)

**Decision:** Avoid new npm deps. Use JSON for machine fixtures (`env.json` optional) OR parse simple YAML subsets. Prefer shipping `meta.yaml` as true YAML and parsing with a 30-line subset parser for `key: value` and nested lists used in our files.

- [ ] **Step 2: Implement `workflow-hash.mjs` CLI** printing JSON:

```json
{ "workflow_rev": "...", "workflow_tree_hash": "..." }
```

- [ ] **Step 3: Run and verify**

```powershell
node evals/scripts/workflow-hash.mjs
```

Expected: valid JSON with both keys, 40-char hex-like rev.

---

### Task 3: P01 question pack + sandbox fixture

**Files:**
- Create: `evals/suites/protocol/P01-readonly-gate/meta.yaml`
- Create: `evals/suites/protocol/P01-readonly-gate/task.md`
- Create: `evals/suites/protocol/P01-readonly-gate/rubric.md`
- Create: `evals/suites/protocol/P01-readonly-gate/env.yaml`
- Create: `evals/fixtures/sandbox-protocol/AGENTS.md` (minimal harness block)
- Create: `evals/fixtures/sandbox-protocol/docs/HARNESS.md` (excerpt: read-only vs change rules)

P01 task (candidate-visible): ask only to **explain** what the harness does for a read-only request — must not bootstrap or record intake/trace.

Hard checks in `env.yaml`:

```yaml
requires_full_repo: false
sandbox: fixtures/sandbox-protocol
hard_checks:
  - id: no_harness_db
    type: path_absent
    path: harness.db
  - id: no_intake_marker
    type: path_absent
    path: .eval-markers/intake-written
  - id: answered
    type: transcript_min_chars
    min: 40
```

Live agent wrapper will touch `.eval-markers/*` only if it wrongly runs intake (we detect via wrapper hooks or file presence). For dry fixtures we simulate.

---

### Task 4: Hard grader + dry fixtures

**Files:**
- Create: `evals/scripts/grade-hard.mjs`
- Create: `evals/fixtures/dry/P01-pass/snapshot.json`
- Create: `evals/fixtures/dry/P01-fail/snapshot.json`
- Create: `evals/fixtures/dry/P01-pass/transcript.txt`
- Create: `evals/fixtures/dry/P01-fail/transcript.txt`

- [ ] **Step 1: Write failing test script** `evals/scripts/selftest-grade.mjs` that grades pass fixture → expect result pass; fail fixture → fail.

- [ ] **Step 2: Run selftest — expect FAIL** (grader missing).

- [ ] **Step 3: Implement `grade-hard.mjs`**.

- [ ] **Step 4: Run selftest — expect PASS**.

Pass snapshot: no harness.db, transcript long enough.  
Fail snapshot: `harness.db` present OR intake marker present.

---

### Task 5: `run-question.mjs` + `ingest-scores.mjs`

**Files:**
- Create: `evals/scripts/run-question.mjs`
- Create: `evals/scripts/ingest-scores.mjs`
- Create: `evals/templates/review.md`

CLI:

```powershell
node evals/scripts/run-question.mjs --question P01-readonly-gate --mode dry --fixture pass
node evals/scripts/run-question.mjs --question P01-readonly-gate --mode dry --fixture fail
```

Outputs under `evals/runs/<batchId>/P01-readonly-gate/`:
- `score.yaml`
- `review.md`
- `transcript.txt`

Then:

```powershell
node evals/scripts/ingest-scores.mjs --batch <batchId>
```

Appends history + writes `latest.md` / `batch-insights.md`.

Live mode (`--mode live`): call `evals/scripts/run-candidate.mjs` if `EVAL_AGENT` set; if agent binary missing, exit with clear error (not silent pass).

- [ ] **Step 1: Implement run-question + ingest**
- [ ] **Step 2: Dry pass then dry fail; verify scores and history append**
- [ ] **Step 3: Implement stub `run-candidate.mjs`** that documents Claude invocation and exits 2 if binary missing

---

### Task 6: P02–P05 question packs (dry-gradable)

**Files:** Create four more question dirs under `evals/suites/protocol/` with meta/task/rubric/env + dry pass/fail fixtures where hard checks are filesystem-expressible.

| ID | Hard check idea (dry) |
| --- | --- |
| P02 | marker `.eval-markers/intake-ok` must exist after change-request task |
| P03 | transcript must mention `CONTEXT_RULES` or `FEATURE_INTAKE` (string evidence) |
| P04 | marker `.eval-markers/trace-ok` must exist |
| P05 | marker `.eval-markers/python-backend` present and `java-touched` absent |

- [ ] **Step 1: Author four packs**
- [ ] **Step 2: Extend selftest to run all dry pass fixtures**
- [ ] **Step 3: Document `node evals/scripts/run-suite.mjs --suite protocol --mode dry`**

---

### Task 7: Docs maturity link + design status

**Files:**
- Modify: `docs/HARNESS_MATURITY.md` — add Benchmark entry pointing to `evals/README.md` and design spec; keep H3 Partial until live regression experiment recorded
- Modify: `docs/HARNESS_COMPONENTS.md` — Observability gap note: eval harness exists
- Modify: `docs/superpowers/specs/2026-07-16-harness-eval-design.md` — Status: Accepted

- [ ] **Step 1: Doc updates**
- [ ] **Step 2: Add `evals/docs/regression-experiment.md` template** for intentional weaken-rules experiment (fill when first live run happens)

---

### Task 8: Suite runner smoke

**Files:**
- Create: `evals/scripts/run-suite.mjs`

```powershell
node evals/scripts/run-suite.mjs --suite protocol --mode dry
```

Expected: all dry-pass fixtures grade `pass`; exit 0.

---

## Spec coverage check

| Spec item | Task |
| --- | --- |
| evals/ tree + runs gitignore | 1 |
| Four-file question format | 3, 6 |
| score.yaml + review + history | 5 |
| workflow_rev / tree_hash | 2 |
| P01 vertical slice | 3–5 |
| P02–P05 Wave 1 | 6 |
| maturity / components pointer | 7 |
| dry-fixture mitigation | 4–5 |
| promptfoo | Deferred: optional Task 9 later; dry Node path is MVP engine |
| CI smoke | Deferred (spec: non-blocking) |
| Phase 2 product question | Deferred (template only in Task 7 README pointer) |

---

## Execution note

User requested immediate execution. Prefer **inline executing-plans** in this session. Commit only when user asks.
