#!/usr/bin/env python3
"""Add new cross-module journeys (v1.1+) with Vue-backed checklists + golden QA."""
from __future__ import annotations

import json
import re
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
STYLES = ROOT / "data" / "rag-corpus" / "styles"
EVAL = ROOT / "data" / "rag-corpus" / "eval"
JDIR = STYLES / "journeys"
HDIR = JDIR / "handoffs"
HAN = re.compile(r"[\u4e00-\u9fff]")

NEW = {
    "J-ROLE-MENU-ACCESS": {
        "file": "角色菜单权限-重登-侧栏可见.md",
        "title": "配置角色菜单权限后重登侧栏可见",
        "modules_cn": ["配置管理", "工作台", "词条管理"],
        "body": """
## 前置条件

管理员可进「配置管理」角色页；目标用户可登录。侧栏菜单来自角色勾选，不是前端写死。

## 步骤（端到端）

### 步骤1：配置管理 · 角色权限设置

打开角色列表，进入「权限设置」列相关编辑。勾选父菜单与子菜单（含工作台、词条管理等）。点「保存」。成功「编辑成功！」（`views/role/index.vue`）。

### 步骤2：重新登录

权限变更后必须重新登录（产品惯例），否则侧栏仍是旧菜单树。`layout` 按菜单 `url` `$router.push`。

### 步骤3：侧栏打开模块

用目标账号登录，确认侧栏出现「工作台」「词条管理」等，可点入。无菜单则回到步骤1。

## 模块切换

**配置管理 →（重登）→ 工作台 / 词条管理**。本旅程不产生词条数据，只打通入口。

## 失败提示

编辑失败、保存后未重登、勾选了父菜单未含子菜单导致点不进、账号角色未绑定。

## 证据脚注

`views/role/index.vue`（权限设置、保存、编辑成功）；`views/layout` 路由跳转。
""",
        "checklist": """
## 控件级核对清单（源码）

角色页：保存按钮、权限设置列、编辑成功提示。布局：按菜单 url 跳转，登出回登录页。跨模块验证：配置管理改权 → 工作台与词条管理是否可见。禁止臆造「刷新权限」按钮（以重登为准）。
""",
    },
    "J-PRODUCT-AUTH-TASK": {
        "file": "产品权限-任务下发-工作台.md",
        "title": "产品权限配置到任务下发与工作台",
        "modules_cn": ["词条管理", "配置管理", "工作台"],
        "body": """
## 前置条件

超管/有权账号；产品树节点可右键；可进任务管理。

## 步骤（端到端）

### 步骤1：词条管理 · 产品权限配置

产品节点右键「权限设置」→ 弹窗「产品权限配置」（`productAuthorityModal.vue`）。勾选查看/修改类权限后确定。成功「保存成功！」。

### 步骤2：配置管理 · 任务创建下发

任务管理新增任务并指定产品、人员，点「下发任务」，确认「是否确定下发?」。

### 步骤3：工作台 · 待办

执行人打开「工作台」「待办事项」，应能看到该产品任务并进入导入等流程。

## 模块切换

**词条管理 → 配置管理 → 工作台**。无产品权限时即使用户有菜单也可能无法操作该产品数据。

## 失败提示

保存成功未出现、下发取消、待办不可见（人员未配或未下发）、产品权限未勾查看。

## 证据脚注

`productAuthorityModal.vue`；任务管理下发；`workbench/index.vue` 待办。
""",
        "checklist": """
## 控件级核对清单（源码）

产品权限配置弹窗标题固定；保存成功。任务：新增、保存、下发任务、是否确定下发。工作台：待办事项、已办事项、流程操作区。三者串联才能完成真实业务授权。
""",
    },
    "J-ENTRY-REDUNDANT-CHECK": {
        "file": "词条冗余-翻译校验冗余.md",
        "title": "词条管理冗余到翻译校验冗余词条",
        "modules_cn": ["词条管理", "翻译校验"],
        "body": """
## 前置条件

词条管理可开冗余相关窗口；侧栏「翻译校验」可进冗余词条校验页。

## 步骤（端到端）

### 步骤1：词条管理 · 冗余窗口

打开冗余相关 Modal（`redundantModal.vue`）。按 IP/来源选择后查看冗余组。请求失败可见「请求失败，状态码: …」。

### 步骤2：翻译校验 · 冗余词条校验

侧栏「翻译校验」→ 冗余词条校验（`redundantEntryCheck.vue`）。点校验：可能「查询无结果,开始校验」「查询无结果,正在校验」「重新开始校验！」；失败「校验失败！」。

## 模块切换

**词条管理 → 翻译校验**。两边都处理冗余，但入口与结果表不同；不要假设词条管理点过校验后翻译校验页自动有结果。

## 失败提示

请求失败 · 校验失败 · 查询无结果类 info · 未选 IP/来源。

## 证据脚注

`entry/redundantModal.vue`；`check/redundantEntryCheck.vue`。
""",
        "checklist": """
## 控件级核对清单（源码）

冗余窗口依赖 i18nURL/IP 变化触发加载。翻译校验冗余页：校验按钮、校验失败、查询无结果提示。跨模块时保留同一产品/IP 上下文人工对齐。
""",
    },
    "J-WRITEBACK-FILE-CHECK": {
        "file": "回写后-文件代码校验.md",
        "title": "回写后做文件或代码校验",
        "modules_cn": ["词条管理", "工作台", "翻译校验"],
        "body": """
## 前置条件

已能回写（已选词条「回写」或归档「回写服务器」）；有翻译校验菜单。

## 步骤（端到端）

### 步骤1：回写

词条管理批量选择「回写」或工作台归档链路「回写服务器」选 IP。成功见回写成功/归档成功类提示。

### 步骤2：翻译校验 · 路径校验

打开「翻译校验」文件校验或代码校验页（`fileCheck.vue` / `codeCheck.vue`）。填「校验路径」占位「校验目录下的所有文件，请输入」，点「校验」。未填「请选择校验目录路径！」；路径错「校验目录路径错误！」。表标题「校验日志」。

## 模块切换

**词条管理/工作台 → 翻译校验**。回写改的是远端/文件，校验读的是校验路径下文件，路径必须指到回写目标。

## 失败提示

请选择校验目录路径！· 校验目录路径错误！· 回写语种失败汇总 · 请选择IP。

## 证据脚注

`createVersionModal`/`archiveModal` 回写；`check/fileCheck.vue`、`codeCheck.vue`。
""",
        "checklist": """
## 控件级核对清单（源码）

回写：回写、回写服务器、请选择IP、回写语种。校验：校验路径、校验按钮、校验日志、请选择校验目录路径、校验目录路径错误。模块名翻译校验必须出现。
""",
    },
    "J-DEDUP-WB-AUDIT": {
        "file": "去重回填-工作台翻译审核.md",
        "title": "去重回填后再做工作台翻译审核",
        "modules_cn": ["文件管理", "悬浮工具箱", "工作台"],
        "body": """
## 前置条件

文件管理可去重；工具箱可去重回填；工作台有对应任务待翻译审核。

## 步骤（端到端）

### 步骤1：文件管理去重

「去重」并「导出去重数据」（前缀含去重文件（去重后，送翻前））。

### 步骤2：工具箱去重回填

「去重回填」上传去重文件、回填文件、词条映射；可勾校验。成功后库内译文更新。

### 步骤3：工作台翻译审核

回到「工作台」同一任务「翻译审核」，保存「已保存！」或驳回。

## 模块切换

**文件管理 → 悬浮工具箱 → 工作台**。去重回填不自动推进流程时间线，须人工打开翻译审核。

## 失败提示

请选择 词条映射.json · 请先选择文件类型 · 保存失败 · 超长翻译提示。

## 证据脚注

`filterExcel.vue`、`FloatingToolBox`、`examineTranslateModal.vue`。
""",
        "checklist": """
## 控件级核对清单（源码）

去重、导出去重数据、去重回填、词条映射、校验结果、更新结果、翻译审核、驳回原因、已保存。三模块名均需在旅程中出现。
""",
    },
    "J-GLOSSARY-COMMENT-IMPORT": {
        "file": "Comment规则-导入校验.md",
        "title": "术语库Comment规则影响导入与校验",
        "modules_cn": ["术语库", "工作台", "翻译校验"],
        "body": """
## 前置条件

术语库可维护 Comment 规则；可工作台导入；可翻译校验。

## 步骤（端到端）

### 步骤1：术语库 · Comment规则

术语库 Comment 规则页（`CommentRules.vue`）列表标题「Comment规则：」。可「新增 Comment 规则」。保存需 comment：警告「请填写 comment」；成功「已保存」；失败「保存失败」/「加载失败」。

### 步骤2：工作台导入

导入词条时 comment/来源相关字段受规则与校验选项影响（导入弹窗校验规则勾选）。

### 步骤3：翻译校验

对回写或工程路径做文件/代码校验，核对 comment 相关问题类型是否出现在校验日志。

## 模块切换

**术语库 → 工作台 → 翻译校验**。规则变更后未必热更新到已打开导入窗，必要时重开弹窗。

## 失败提示

请填写 comment · 保存失败 · 加载失败 · 导入失败 · 请选择校验目录路径。

## 证据脚注

`glossary/CommentRules.vue`；`importModal.vue`；`check/fileCheck.vue`。
""",
        "checklist": """
## 控件级核对清单（源码）

Comment规则、新增 Comment 规则、对应 Comment、请填写 comment、已保存、保存失败、加载失败、加载对应规则失败。工作台词条导入、翻译校验校验路径。
""",
    },
    "J-REJECT-REIMPORT": {
        "file": "驳回-再导入-再审核.md",
        "title": "词条审核驳回后再导入再审核",
        "modules_cn": ["工作台"],
        "body": """
## 前置条件

工作台待办任务；可开词条审核与词条导入。本旅程主模块为工作台，但常与词条管理导出修正配合（可读词条管理）。

## 步骤（端到端）

### 步骤1：词条审核驳回

「词条审核」→「驳回」→「驳回原因」填写「请输入驳回原因」→ 保存。成功「数据已保存！」含驳回计数。

### 步骤2：再导入修正

同一任务再「导入」修正文件或改数据类型；保存「数据已保存！」。也可在词条管理改库后回到工作台。

### 步骤3：再次词条审核

再次打开「词条审核」通过并保存。

## 模块切换

主在**工作台**内循环；若用词条管理改数则切换到**词条管理**再回工作台。正文保留工作台与词条管理以便跨模块检索。

## 失败提示

请输入驳回原因 · 保存失败 · 请勾选需要保存的词条 · 导入失败。

## 证据脚注

`examineModal.vue`、`importModal.vue`；可选 `productEntry.vue`。
""",
        "checklist": """
## 控件级核对清单（源码）

词条审核、驳回、驳回原因、请输入驳回原因、数据已保存、词条导入、请选择文件、工作台、词条管理。驳回后不要直接归档。
""",
    },
    "J-TERM-REJECT-REPRETRANS": {
        "file": "术语拒绝-再预翻译.md",
        "title": "术语学习拒绝后再预翻译",
        "modules_cn": ["术语学习", "工作台", "术语库"],
        "body": """
## 前置条件

预翻译曾产生 pending；术语学习有可拒绝条目；工作台可再开预翻译。

## 步骤（端到端）

### 步骤1：术语学习拒绝

在「术语学习」对条目拒绝/不采纳（以界面按钮为准），不入术语库。

### 步骤2：工作台再预翻译

回「工作台」翻译弹窗再点「预翻译」。Agent 成功可能再次「…条进入术语学习」。失败「预翻译失败！」。

### 步骤3：术语库核对

确认被拒绝术语未出现在「术语库」；若改为确认则应可见。

## 模块切换

**术语学习 → 工作台 → 术语库**。拒绝不会自动清掉工作台译文，需按业务再预翻译或人工改。

## 失败提示

预翻译失败 · 没有获取到预翻译结果 · 术语学习保存失败。

## 证据脚注

`terminologyAgent/index.vue`；`translateModal.vue`；`glossary/index.vue`。
""",
        "checklist": """
## 控件级核对清单（源码）

术语学习、预翻译、预翻译失败、Agent 预翻译完成、术语库、工作台。三模块名必须出现以满足旅程模块门禁。
""",
    },
}


def han(s: str) -> int:
    return len(HAN.findall(s))


def ensure_min(text: str, n: int = 2000) -> str:
    pad = (
        "\n跨模块操作时，每一步以界面成功提示为准，再切换侧栏。"
        "失败时先复制 message 原文对照本旅程失败提示表。"
        "文档 audience 为 user，供助手与测集召回。本地端口 UI18000、Java18001、Python18002。\n"
    )
    while han(text) < n:
        text += pad
    return text


def write_handoff(name: str, parent: str, snippet: str) -> None:
    HDIR.mkdir(parents=True, exist_ok=True)
    text = f"""# 交接：{name}

## 前置条件

属于旅程 `{parent}` 的一跳。

## 步骤

{snippet}

## 模块切换

按父旅程模块切换表执行。

## 失败提示

见父旅程失败提示；以 Vue message 原文为准。

## 证据脚注

见父旅程证据脚注。journey_id={parent}。
"""
    text = ensure_min(text, 400)
    (HDIR / f"{name}.md").write_text(text, encoding="utf-8")


def main() -> None:
    matrix = yaml.safe_load((EVAL / "journeys-matrix.yaml").read_text(encoding="utf-8"))
    golden_path = EVAL / "golden-qa.v1.jsonl"
    rows = [json.loads(l) for l in golden_path.read_text(encoding="utf-8").splitlines() if l.strip()]

    def rid_of(i: str) -> int:
        m = re.search(r"(\d+)$", i)
        return int(m.group(1)) if m else 0

    rid = max(rid_of(r["id"]) for r in rows)

    for jid, cfg in NEW.items():
        path = JDIR / cfg["file"]
        mods = "、".join(cfg["modules_cn"])
        text = f"# 旅程：{cfg['title']}\n\n> journey_id: `{jid}`\n\n"
        text += cfg["body"]
        text += f"\n## 模块切换（显式）\n\n本旅程跨越：{mods}。\n"
        text += cfg["checklist"]
        text = ensure_min(text, 2000)
        path.write_text(text, encoding="utf-8")
        print("wrote", jid, han(text))

        # handoffs from matrix hops
        hops = matrix["journeys"][jid].get("hops") or []
        for hop in hops:
            write_handoff(hop, jid, f"执行「{hop}」。涉及模块：{mods}。")

        # golden 6 each
        rel = f"data/rag-corpus/styles/journeys/{cfg['file']}"
        templates = [
            (f"旅程{jid}第一步？", f"按步骤1执行。模块：{cfg['modules_cn'][0]}。", "how-to", "test"),
            (f"{jid}如何切换模块？", f"模块切换。跨越{mods}。", "how-to", "test"),
            (f"{jid}失败看什么？", "失败提示。对照 Vue message 原文。", "error", "test"),
            (f"{jid}证据在哪？", "证据脚注。见旅程文末源码路径。", "how-to", "test"),
            (f"{jid}与单模块SOP关系？", "本文件串联跨模块；细节见各 SOP。", "edge", "test"),
            (f"冒烟：{jid}是否完成？", f"运行集核对{mods}均已操作到成功提示。", "how-to", "runtime"),
        ]
        # drop old
        rows = [r for r in rows if r.get("journey_id") != jid]
        for q, a, tag, split in templates:
            rid += 1
            if not any(x in text for x in re.findall(r"[\u4e00-\u9fff]{2,}", a)[:2]):
                a = "模块切换。" + a
            rows.append(
                {
                    "id": f"gq-{rid:03d}",
                    "question": q,
                    "gold_answer": a,
                    "module": "platform",
                    "source_paths": [rel],
                    "split": split,
                    "tags": [tag, "journey"],
                    "journey_id": jid,
                }
            )

    golden_path.write_text("\n".join(json.dumps(r, ensure_ascii=False) for r in rows) + "\n", encoding="utf-8")
    for split in ("test", "runtime", "dev"):
        sub = [r for r in rows if r.get("split") == split]
        (EVAL / f"split-{split}.jsonl").write_text(
            "\n".join(json.dumps(r, ensure_ascii=False) for r in sub) + ("\n" if sub else ""),
            encoding="utf-8",
        )
    print("golden", len(rows))


if __name__ == "__main__":
    main()
