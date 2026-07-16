"""US-3C API matrix smoke — batch + pending list（不写假工作台 id）。

用法（需 Agent :18002 已启动）：
  python -m devtools.verify_us3c01_api_matrix

说明：
- entries 故意不带工作台 ``id``，避免 auto_approved 触发 workbench sync 噪音。
- S02：现网常走 none+LLM；矩阵接受 fuzzy|none（见用例注释）。
"""
from __future__ import annotations

import json
import re
import sys
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:18002"

CASES = [
    {
        "name": "exact",
        "entry": "ADM/R01-RAG精确",
        "comment": "",
        "expect_retrieval": "exact",
        "expect_review": "auto_approved",
        "forbid_agent_prefix": True,
    },
    {
        # 产品可接受「模糊低置信」或「未命中走 LLM」；二者均需 needs_human、无 [Agent] 占位
        "name": "fuzzy_or_none_low",
        "entry": "ADM/S02-RAG模糊-用户管理系统",
        "comment": "ADM-S02",
        "expect_retrieval_re": r"^(fuzzy|none)$",
        "expect_review": "needs_human",
        "forbid_agent_prefix": True,
    },
    {
        "name": "decomposed",
        "entry": "文件与系统",
        "comment": "",
        "expect_retrieval": "decomposed",
        "expect_review": "auto_approved",
        "expect_suggested_has_space_or_and": True,
        "forbid_suggested_eq": "FileSystem",
        "forbid_agent_prefix": True,
        "expect_reasoning": True,
    },
    {
        "name": "none_llm",
        "entry": "T99-全新未收录",
        "comment": "ADM-T99",
        "expect_retrieval": "none",
        "expect_review": "needs_human",
        "forbid_agent_prefix": True,
    },
]


def post_batch(entry: str, comment: str) -> dict:
    # 不传工作台 entry id：验收只关心 Agent 契约，避免 sync「词条不存在」告警
    body = {
        "entries": [
            {
                "entry": entry,
                "russian": "",
                "comment": comment,
            }
        ],
        "target_lang": "英文",
        "department": "通用平台部",
    }
    data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(
        f"{BASE}/agent/pre-translate/batch?confidenceThreshold=0.8&taskID=us3c-matrix",
        data=data,
        headers={"Content-Type": "application/json; charset=utf-8"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        return json.loads(resp.read().decode("utf-8"))


def get_pending() -> dict:
    req = urllib.request.Request(
        f"{BASE}/agent/term-learning/list?page=1&pageSize=5",
        method="GET",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def main() -> int:
    failures: list[str] = []
    print(f"{'case':<20} {'retrieval':<12} {'review':<14} {'suggested':<28} OK")
    print("-" * 100)

    for case in CASES:
        try:
            payload = post_batch(case["entry"], case["comment"])
        except urllib.error.HTTPError as exc:
            failures.append(f"{case['name']}: HTTP {exc.code}")
            print(f"{case['name']:<20} HTTP-{exc.code}")
            continue
        except Exception as exc:  # noqa: BLE001
            failures.append(f"{case['name']}: {exc}")
            print(f"{case['name']:<20} ERR {exc}")
            continue

        item = (payload.get("data") or {}).get("list") or [{}]
        row = item[0]
        meta = row.get("agent_meta") or {}
        retrieval = meta.get("retrieval_method") or ""
        review = meta.get("review_status") or ""
        suggested = meta.get("suggested_translation") or row.get("translate") or ""
        reasoning = meta.get("reasoning") or ""
        sync_err = meta.get("workbench_sync_error")

        ok = True
        if sync_err:
            ok = False
            failures.append(f"{case['name']}: unexpected workbench_sync_error={sync_err!r}")
        if "expect_retrieval" in case and retrieval != case["expect_retrieval"]:
            ok = False
            failures.append(
                f"{case['name']}: retrieval={retrieval} want {case['expect_retrieval']}"
            )
        if "expect_retrieval_re" in case and not re.match(
            case["expect_retrieval_re"], retrieval
        ):
            ok = False
            failures.append(
                f"{case['name']}: retrieval={retrieval} want {case['expect_retrieval_re']}"
            )
        if review != case["expect_review"]:
            ok = False
            failures.append(f"{case['name']}: review={review} want {case['expect_review']}")
        if case.get("forbid_agent_prefix") and str(suggested).startswith("[Agent]"):
            ok = False
            failures.append(f"{case['name']}: suggested has [Agent] prefix")
        if case.get("forbid_suggested_eq") and suggested == case["forbid_suggested_eq"]:
            ok = False
            failures.append(f"{case['name']}: suggested equals forbidden FileSystem")
        if case.get("expect_suggested_has_space_or_and"):
            if " " not in suggested and "and" not in suggested.lower():
                ok = False
                failures.append(
                    f"{case['name']}: suggested not natural phrase: {suggested!r}"
                )
        if case.get("expect_reasoning") and not reasoning.strip():
            ok = False
            failures.append(f"{case['name']}: missing reasoning for auditSuggest")

        mark = "OK" if ok else "FAIL"
        sug_show = (suggested[:26] + "..") if len(str(suggested)) > 28 else suggested
        print(f"{case['name']:<20} {retrieval:<12} {review:<14} {sug_show:<28} {mark}")
        if case["name"] == "decomposed":
            print(f"  reasoning_len={len(reasoning)} suggested={suggested!r}")

    try:
        pending = get_pending()
        print(f"pending_list code={pending.get('code')}")
    except Exception as exc:  # noqa: BLE001
        failures.append(f"pending_list: {exc}")
        print(f"pending_list ERR {exc}")

    if failures:
        print("\nFAILURES:")
        for f in failures:
            print(" -", f)
        return 1
    print("\nAll API matrix cases passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
