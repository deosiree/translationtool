#!/usr/bin/env python3
"""RAG corpus Goal gate checker. Exit 0 only when all L1 gates pass (doc goal).

Usage:
  python scripts/check-rag-corpus-gates.py
  python scripts/check-rag-corpus-gates.py --json
  python scripts/check-rag-corpus-gates.py --skip-shots
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

try:
    import yaml
except ImportError:
    print("FAIL: need PyYAML (pip install pyyaml)", file=sys.stderr)
    sys.exit(2)

ROOT = Path(__file__).resolve().parents[1]
CORPUS = ROOT / "data" / "rag-corpus"
STYLES = CORPUS / "styles"
EVAL = CORPUS / "eval"
MATRIX = EVAL / "coverage-matrix.yaml"
JOURNEYS = EVAL / "journeys-matrix.yaml"
GOLDEN = EVAL / "golden-qa.v1.jsonl"
MANIFEST = CORPUS / "MANIFEST.yaml"
GATES_MD = CORPUS / "EVAL_GATES.md"

HAN_RE = re.compile(r"[\u4e00-\u9fff]")
PAD_NAME_RE = re.compile(r"(附录|加厚)")
SECTION_KEYS = ("前置", "步骤", "失败", "证据")
JOURNEY_SECTION_KEYS = ("前置", "步骤", "模块切换", "失败", "证据")
MIN_VOLUME = 80_000
MIN_MD_FILES = 200
MAX_FILE = 12_000
SOP_MIN = 1_500
SOP_COMPLEX_MIN = 2_500
JOURNEY_MIN = 2_000
GOLDEN_TOTAL = 120
GOLDEN_TEST = 80
GOLDEN_RUNTIME = 40
QA_PER_GOAL = 8
QA_PER_JOURNEY = 6
SOP_SOURCE_RATIO = 0.5
GATES_VERSION = "v1.1.0"


def han_count(text: str) -> int:
    return len(HAN_RE.findall(text))


def load_manifest_pad_paths() -> set[str]:
    if not MANIFEST.exists():
        return set()
    data = yaml.safe_load(MANIFEST.read_text(encoding="utf-8")) or {}
    pad: set[str] = set()
    for e in data.get("entries") or []:
        st = (e.get("status") or "").lower()
        if st in ("deprecated", "pad"):
            p = e.get("path") or ""
            if p:
                pad.add(p.replace("\\", "/"))
    return pad


def is_padded(path: Path, pad_paths: set[str]) -> bool:
    rel = path.relative_to(CORPUS).as_posix()
    if rel in pad_paths:
        return True
    if PAD_NAME_RE.search(path.name):
        return True
    return False


def check_volume(pad_paths: set[str]) -> dict:
    total = 0
    md_n = 0
    oversize: list[str] = []
    per_file: list[tuple[str, int]] = []
    for p in STYLES.rglob("*.md"):
        if is_padded(p, pad_paths):
            continue
        text = p.read_text(encoding="utf-8")
        c = han_count(text)
        rel = p.relative_to(STYLES).as_posix()
        per_file.append((rel, c))
        total += c
        md_n += 1
        if c > MAX_FILE:
            oversize.append(f"{rel}:{c}")
    return {
        "ok": total >= MIN_VOLUME and md_n >= MIN_MD_FILES and not oversize,
        "han_chars": total,
        "min": MIN_VOLUME,
        "md_files": md_n,
        "min_md_files": MIN_MD_FILES,
        "oversize": oversize,
    }


def check_journeys() -> dict:
    if not JOURNEYS.exists():
        return {"ok": False, "missing": ["journeys-matrix.yaml missing"], "checks": []}
    data = yaml.safe_load(JOURNEYS.read_text(encoding="utf-8")) or {}
    missing: list[str] = []
    checks: list[dict] = []
    module_names = {
        "workbench": "工作台",
        "entry": "词条管理",
        "glossary": "术语库",
        "configure": "配置管理",
        "check": "翻译校验",
        "file": "文件管理",
        "terminologyAgent": "术语学习",
        "toolbox": "悬浮工具箱",
        "assistant": "智能助手",
    }
    for jid, cfg in (data.get("journeys") or {}).items():
        rel = cfg.get("path") or ""
        path = STYLES / rel if rel else None
        if not rel or not path or not path.is_file():
            missing.append(f"{jid}->{rel or 'empty'}")
            continue
        text = path.read_text(encoding="utf-8")
        c = han_count(text)
        sec_ok = all(k in text for k in JOURNEY_SECTION_KEYS)
        mods = cfg.get("modules") or []
        hit = 0
        for m in mods:
            cn = module_names.get(m, m)
            if m in text or cn in text:
                hit += 1
        # also count CN module names appearing
        cn_hit = sum(1 for cn in module_names.values() if cn in text)
        modules_ok = hit >= min(3, len(mods)) or cn_hit >= 3
        ok = c >= JOURNEY_MIN and sec_ok and modules_ok
        checks.append(
            {
                "id": jid,
                "path": rel,
                "han": c,
                "min": JOURNEY_MIN,
                "sections_ok": sec_ok,
                "modules_ok": modules_ok,
                "ok": ok,
            }
        )
        if not ok:
            missing.append(
                f"{jid} han={c}/{JOURNEY_MIN} sec={sec_ok} mods={modules_ok}"
            )
    return {"ok": not missing and bool(checks), "missing": missing, "checks": checks}


def check_matrix() -> dict:
    matrix = yaml.safe_load(MATRIX.read_text(encoding="utf-8"))
    missing: list[str] = []
    sop_checks: list[dict] = []

    def need_file(label: str, rel: str | None) -> None:
        if not rel:
            missing.append(label)
            return
        path = STYLES / rel
        if not path.is_file():
            missing.append(f"{label}->{rel}")

    for mod, cfg in (matrix.get("modules") or {}).items():
        need_file(f"{mod}.user-manual", cfg.get("user-manual") or "")
        need_file(f"{mod}.faq", cfg.get("faq") or "")
        need_file(f"{mod}.troubleshooting", cfg.get("troubleshooting") or "")
        scenes = cfg.get("scenarios") or []
        if not isinstance(scenes, list) or len(scenes) < 2:
            # assistant may allow 0 if empty goals-only module — still require 2 for product modules
            if mod != "assistant" or scenes == []:
                missing.append(f"{mod}.scenarios need >=2 got {len(scenes) if isinstance(scenes, list) else 0}")
        else:
            for i, s in enumerate(scenes):
                need_file(f"{mod}.scenarios[{i}]", s)

        for gid, g in (cfg.get("goals") or {}).items():
            sop = g.get("sop") or ""
            complex_ = bool(g.get("complex"))
            need_file(f"{gid}.sop", sop)
            if sop and (STYLES / sop).is_file():
                text = (STYLES / sop).read_text(encoding="utf-8")
                c = han_count(text)
                need_min = SOP_COMPLEX_MIN if complex_ else SOP_MIN
                sec_ok = all(k in text for k in SECTION_KEYS)
                sop_checks.append(
                    {
                        "id": gid,
                        "path": sop,
                        "han": c,
                        "min": need_min,
                        "sections_ok": sec_ok,
                        "ok": c >= need_min and sec_ok,
                    }
                )

    for gid, g in (matrix.get("wave25_goals") or {}).items():
        sop = g.get("sop") or ""
        if not sop:
            missing.append(f"wave25.{gid}.sop empty")
        else:
            need_file(f"wave25.{gid}.sop", sop)

    return {
        "ok": not missing and all(s["ok"] for s in sop_checks),
        "missing": missing,
        "sop_checks": sop_checks,
    }


def load_golden() -> list[dict]:
    if not GOLDEN.exists():
        return []
    rows = []
    for line in GOLDEN.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line:
            continue
        rows.append(json.loads(line))
    return rows


def normalize_q(q: str) -> str:
    return re.sub(r"\s+", "", (q or "").strip().lower())


def check_golden(matrix: dict) -> dict:
    rows = load_golden()
    issues: list[str] = []
    by_split: dict[str, int] = defaultdict(int)
    by_module_tags: dict[str, set[str]] = defaultdict(set)
    by_goal: dict[str, int] = defaultdict(int)
    goal_sop_hits: dict[str, int] = defaultdict(int)
    goal_total: dict[str, int] = defaultdict(int)

    # map sop path -> goal id
    sop_to_goal: dict[str, str] = {}
    for mod, cfg in (matrix.get("modules") or {}).items():
        for gid, g in (cfg.get("goals") or {}).items():
            sop = (g.get("sop") or "").replace("\\", "/")
            if sop:
                sop_to_goal[f"styles/{sop}"] = gid
                sop_to_goal[sop] = gid

    test_qs: set[str] = set()
    runtime_qs: set[str] = set()

    required_fields = ("id", "question", "gold_answer", "module", "source_paths", "split")

    for r in rows:
        for f in required_fields:
            if f not in r:
                issues.append(f"missing field {f} in {r.get('id')}")
                break
        split = r.get("split")
        if split not in ("test", "runtime", "dev"):
            issues.append(f"{r.get('id')}: bad split {split!r}")
        else:
            by_split[split] += 1

        for t in r.get("tags") or []:
            by_module_tags[str(r.get("module"))].add(str(t))

        nq = normalize_q(r.get("question") or "")
        if split == "test":
            test_qs.add(nq)
        elif split == "runtime":
            runtime_qs.add(nq)

        paths = r.get("source_paths") or []
        if not isinstance(paths, list) or not paths:
            issues.append(f"{r.get('id')}: empty source_paths")
            continue
        texts = []
        for sp in paths:
            sp_norm = str(sp).replace("\\", "/")
            # allow with or without data/rag-corpus prefix
            candidates = [
                ROOT / sp_norm,
                CORPUS / sp_norm.replace("data/rag-corpus/", ""),
                STYLES / Path(sp_norm).name,
            ]
            # if path contains styles/
            if "styles/" in sp_norm:
                rel = sp_norm.split("styles/", 1)[1]
                candidates.append(STYLES / rel)
            found = None
            for c in candidates:
                if c.is_file():
                    found = c
                    break
            if not found:
                issues.append(f"{r.get('id')}: missing source {sp}")
            else:
                texts.append(found.read_text(encoding="utf-8"))
                # goal association
                for key, gid in sop_to_goal.items():
                    if key in sp_norm or sp_norm.endswith(key) or key.endswith(sp_norm.split("styles/")[-1] if "styles/" in sp_norm else sp_norm):
                        goal_total[gid] += 1
                        if "sop/" in sp_norm:
                            goal_sop_hits[gid] += 1
                        by_goal[gid] += 1
                        break

        # provenance: at least one han bigram from answer in sources
        ans = r.get("gold_answer") or ""
        chunks = [ans[i : i + 4] for i in range(0, min(len(ans), 40)) if HAN_RE.search(ans[i : i + 2] if i + 2 <= len(ans) else "")]
        # simpler: extract consecutive han runs length>=2
        runs = re.findall(r"[\u4e00-\u9fff]{2,}", ans)
        if texts and runs:
            joined = "\n".join(texts)
            if not any(run in joined for run in runs[:5]):
                issues.append(f"{r.get('id')}: answer not grounded in sources")

    overlap = test_qs & runtime_qs
    if overlap:
        issues.append(f"test/runtime question overlap: {len(overlap)}")

    if len(rows) < GOLDEN_TOTAL:
        issues.append(f"golden total {len(rows)} < {GOLDEN_TOTAL}")
    if by_split["test"] < GOLDEN_TEST:
        issues.append(f"test {by_split['test']} < {GOLDEN_TEST}")
    if by_split["runtime"] < GOLDEN_RUNTIME:
        issues.append(f"runtime {by_split['runtime']} < {GOLDEN_RUNTIME}")

    # per-goal qa count from matrix goals
    for mod, cfg in (matrix.get("modules") or {}).items():
        for gid in (cfg.get("goals") or {}):
            if by_goal[gid] < QA_PER_GOAL:
                issues.append(f"{gid}: qa {by_goal[gid]} < {QA_PER_GOAL}")
            if goal_total[gid] > 0:
                ratio = goal_sop_hits[gid] / goal_total[gid]
                if ratio < SOP_SOURCE_RATIO:
                    issues.append(f"{gid}: sop source ratio {ratio:.2f} < {SOP_SOURCE_RATIO}")

    # tag diversity: global how-to / error / edge must appear
    all_tags: set[str] = set()
    for r in rows:
        all_tags.update(str(t) for t in (r.get("tags") or []))
    for t in ("how-to", "error", "edge"):
        if t not in all_tags:
            issues.append(f"global tags missing {t}")

    # journey coverage
    by_journey: dict[str, int] = defaultdict(int)
    for r in rows:
        jid = r.get("journey_id")
        if jid:
            by_journey[str(jid)] += 1
    if JOURNEYS.exists():
        jdata = yaml.safe_load(JOURNEYS.read_text(encoding="utf-8")) or {}
        for jid in (jdata.get("journeys") or {}):
            if by_journey[jid] < QA_PER_JOURNEY:
                issues.append(f"{jid}: qa {by_journey[jid]} < {QA_PER_JOURNEY}")

    return {
        "ok": not issues,
        "total": len(rows),
        "by_split": dict(by_split),
        "issues": issues[:80],
        "issue_count": len(issues),
        "by_goal": dict(by_goal),
        "by_journey": dict(by_journey),
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--json", action="store_true")
    ap.add_argument("--skip-shots", action="store_true", default=True)
    args = ap.parse_args()

    pad_paths = load_manifest_pad_paths()
    matrix = yaml.safe_load(MATRIX.read_text(encoding="utf-8")) if MATRIX.exists() else {}

    report = {
        "gates_version": GATES_VERSION,
        "volume": check_volume(pad_paths),
        "matrix": check_matrix(),
        "journeys": check_journeys(),
        "golden": check_golden(matrix),
        "shots": {"ok": True, "skipped": True, "note": "skip_shots=true; screenshot gate independent"},
    }
    report["pass"] = (
        report["volume"]["ok"]
        and report["matrix"]["ok"]
        and report["journeys"]["ok"]
        and report["golden"]["ok"]
    )

    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        print("=== RAG corpus gates ===")
        print(f"version={GATES_VERSION} {'PASS' if report['pass'] else 'FAIL'}")
        v = report["volume"]
        print(
            f"[volume] {'OK' if v['ok'] else 'FAIL'} han={v['han_chars']}/{v['min']} "
            f"md={v['md_files']}/{v['min_md_files']} oversize={v['oversize'][:5]}"
        )
        m = report["matrix"]
        print(f"[matrix] {'OK' if m['ok'] else 'FAIL'} missing={len(m['missing'])}")
        for x in m["missing"][:25]:
            print(f"  - {x}")
        bad_sop = [s for s in m["sop_checks"] if not s["ok"]]
        for s in bad_sop[:20]:
            print(f"  - SOP {s['id']} han={s['han']}/{s['min']} sections={s['sections_ok']}")
        j = report["journeys"]
        print(f"[journeys] {'OK' if j['ok'] else 'FAIL'} missing={len(j['missing'])}")
        for x in j["missing"][:25]:
            print(f"  - {x}")
        g = report["golden"]
        print(f"[golden] {'OK' if g['ok'] else 'FAIL'} total={g['total']} splits={g['by_split']} issues={g['issue_count']}")
        for x in g["issues"][:25]:
            print(f"  - {x}")
        print(f"[shots] skipped={report['shots']['skipped']}")
        print("---")
        print("Fix only FAIL lines. Re-run: python scripts/check-rag-corpus-gates.py")

    return 0 if report["pass"] else 1


if __name__ == "__main__":
    sys.exit(main())
