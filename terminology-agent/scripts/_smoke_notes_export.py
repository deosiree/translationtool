# -*- coding: utf-8 -*-
"""Smoke: notes-adapt + export-rows + export."""
from __future__ import annotations

import json
import urllib.error
import urllib.request
from pathlib import Path

BASE = "http://127.0.0.1:18002"
NOTES = Path(r"F:/DownLoads/常用注意要点清单.xlsx")
OUT = Path(r"F:/Documents/Repertory/Sieyuan/translationtool/docs/demos/term-word-dictionary/_smoke_export.xlsx")


def post(url: str, *, data=None, headers=None, raw: bytes | None = None):
    body = raw if raw is not None else (json.dumps(data, ensure_ascii=False).encode("utf-8") if data is not None else None)
    req = urllib.request.Request(
        url,
        data=body,
        headers=headers or {},
        method="POST" if body is not None else "GET",
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.status, dict(r.headers), r.read()
    except urllib.error.HTTPError as e:
        return e.code, dict(e.headers), e.read()


def main() -> None:
    st, _h, b = post(
        f"{BASE}/agent/word/export-rows",
        data={
            "rows": [{"word": "a", "translate": "b", "target_lang": "en"}],
            "forcePending": True,
        },
        headers={"Content-Type": "application/json"},
    )
    print("export-rows json", st, b[:4], "PK_OK" if b[:2] == b"PK" else b[:120])
    assert st == 200 and b[:2] == b"PK", "export-rows failed"

    st, _h, b = post(
        f"{BASE}/agent/word/export",
        data={"status": "3"},
        headers={"Content-Type": "application/json"},
    )
    print("export json", st, b[:4], "PK_OK" if b[:2] == b"PK" else b[:120])
    assert st == 200 and b[:2] == b"PK", "export failed"

    assert NOTES.is_file(), f"missing notes file: {NOTES}"
    file_data = NOTES.read_bytes()
    boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    parts = [
        f"--{boundary}".encode(),
        b'Content-Disposition: form-data; name="file"; filename="notes.xlsx"',
        b"Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        b"",
        file_data,
        f"--{boundary}--".encode(),
        b"",
    ]
    raw = b"\r\n".join(parts)
    st, _h, b = post(
        f"{BASE}/agent/word/notes-adapt?targetLang=%E8%8B%B1%E6%96%87",
        raw=raw,
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
    )
    print("notes-adapt", st, b[:180])
    assert st == 200, "notes-adapt failed"
    payload = json.loads(b.decode("utf-8"))
    assert payload.get("code") == 200, payload
    rows = (payload.get("data") or {}).get("list") or []
    print("notes rows", len(rows))
    assert rows, "notes-adapt returned empty list"

    st, _h, b = post(
        f"{BASE}/agent/word/export-rows",
        data={"rows": rows[:50], "forcePending": True},
        headers={"Content-Type": "application/json"},
    )
    print("notes->export-rows", st, b[:4], "PK_OK" if b[:2] == b"PK" else b[:120])
    assert st == 200 and b[:2] == b"PK", "notes export-rows failed"
    OUT.write_bytes(b)
    print("wrote", OUT, "bytes", len(b))
    print("SMOKE_OK")


if __name__ == "__main__":
    main()
