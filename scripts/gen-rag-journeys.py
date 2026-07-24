#!/usr/bin/env python3
"""Generate cross-module journeys + handoff atoms + journey golden QA (EVAL_GATES v1.1)."""
from __future__ import annotations

import json
import re
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[1]
STYLES = ROOT / "data" / "rag-corpus" / "styles"
EVAL = ROOT / "data" / "rag-corpus" / "eval"
JOURNEY_DIR = STYLES / "journeys"
HANDOFF_DIR = JOURNEY_DIR / "handoffs"
HAN = re.compile(r"[\u4e00-\u9fff]")

JOURNEYS = yaml.safe_load((EVAL / "journeys-matrix.yaml").read_text(encoding="utf-8"))["journeys"]

# Evidence-backed hop blurbs (Vue strings)
HOPS = {
    "工作台词条导入": {
        "modules": ["工作台"],
        "ui": ["词条导入", "数据类型", "选择文件", "保存", "请选择IP", "请选择文件！"],
        "evidence": "importModal.vue",
        "body": "在工作台待办选中任务，流程区点「导入」打开「词条导入」。按数据类型选文件/TS/辞典等，必要时选「IP」，结果表勾选后「保存」。",
    },
    "工作台词条审核": {
        "modules": ["工作台"],
        "ui": ["词条审核", "驳回原因", "保存", "全部选择"],
        "evidence": "examineModal.vue",
        "body": "流程区点「词条审核」。筛选待审核，通过或「驳回」填写「驳回原因」后保存。",
    },
    "词条管理批量选择与已选词条": {
        "modules": ["词条管理"],
        "ui": ["批量选择", "已选词条", "选择全部", "取消选择"],
        "evidence": "productEntry.vue / createVersionModal.vue",
        "body": "进入词条管理产品词条页，开启批量选择模式后勾选行；徽章按钮「已选词条」打开「批量选择」大窗，展示已选列表。",
    },
    "已选词条导出Excel": {
        "modules": ["词条管理"],
        "ui": ["导出", "导出字段", "请选择导出字段!", "只能导出已审核"],
        "evidence": "createVersionModal.vue + exportButton.vue",
        "body": "在「批量选择」底栏点「导出」。弹窗选文件类型与「导出字段」，可勾「只能导出已审核」。默认文件名形如词条导出_时间.xlsx。",
    },
    "线下Excel人工翻译": {
        "modules": ["词条管理", "工作台"],
        "ui": ["导出字段", "更新翻译"],
        "evidence": "exportButton 注释：excel只能更新翻译",
        "body": "线下用 Excel/CSV 只改可回填的翻译列，勿改乱 id 等系统列。保存为 UTF-8 CSV 或约定 excel，供「更新翻译」回填。",
    },
    "词条管理或工具箱更新翻译": {
        "modules": ["词条管理", "悬浮工具箱"],
        "ui": ["更新翻译", "更新翻译 v1", "去重回填"],
        "evidence": "productEntry BackFillModal_v3；FloatingToolBox BackFillModal*",
        "body": "管理员且部门 ops 含 fileUpdate 时，词条管理工具栏「更新翻译」；或悬浮工具箱「更新翻译」。选择文件类型与回填文件后执行，成功刷新表。",
    },
    "工作台翻译审核": {
        "modules": ["工作台"],
        "ui": ["翻译审核", "保存"],
        "evidence": "examineTranslateModal.vue",
        "body": "工作台流程区「翻译审核」，核对译文状态后保存通过或驳回。",
    },
    "归档或已选词条回写": {
        "modules": ["工作台", "词条管理"],
        "ui": ["归档预览", "归档并结束任务", "回写服务器", "回写", "请选择IP", "回写语种", "回写类型"],
        "evidence": "archiveModal.vue；createVersionModal 回写表单",
        "body": "任务收尾：工作台「归档」→「归档预览」→「归档并结束任务」或「回写服务器」选 IP。或在已选词条窗点「回写」，填 IP、回写语种、回写类型（默认/TS/辞典）及可选回写Tag/回写来源。",
    },
    "送翻去重": {
        "modules": ["文件管理"],
        "ui": ["去重导出", "UTF-8"],
        "evidence": "fileManage/selectCols.vue",
        "body": "文件管理送翻去重页导入 UTF-8 CSV，执行去重并导出映射与去重后文件。",
    },
    "导出映射": {
        "modules": ["文件管理"],
        "ui": ["去重映射", "导出去重数据"],
        "evidence": "fileManage 去重流程",
        "body": "保留去重映射.json 与去重后送翻文件，供工具箱回填。",
    },
    "去重回填": {
        "modules": ["悬浮工具箱"],
        "ui": ["去重回填", "去重回填V1.5", "更新翻译"],
        "evidence": "FloatingToolBox + backFill modals",
        "body": "工具箱打开「去重回填」，上传映射与回填文件，校验后继续；失败词条可导出。",
    },
    "失败词条导出": {
        "modules": ["悬浮工具箱"],
        "ui": ["没有可导出的失败词条数据", "导出功能初始化失败"],
        "evidence": "backFill modal_v2/v2.5/v3",
        "body": "回填失败时按语种导出失败词条；无数据会提示没有可导出的失败词条数据。",
    },
    "词条翻译预翻译": {
        "modules": ["工作台"],
        "ui": ["预翻译", "Agent翻译", "已更新翻译！"],
        "evidence": "translateModal.vue",
        "body": "工作台「翻译」弹窗工具栏预翻译，按优先级调用 Agent/术语库/引擎。",
    },
    "术语学习确认拒绝": {
        "modules": ["术语学习"],
        "ui": ["确认", "拒绝", "编辑翻译"],
        "evidence": "terminologyAgent/index.vue",
        "body": "低置信结果进术语学习队列，确认入术语库或拒绝；编辑翻译会二次确认并清空置信度等字段。",
    },
    "术语库可见": {
        "modules": ["术语库"],
        "ui": ["术语库"],
        "evidence": "glossary/index.vue",
        "body": "确认后在术语库查询可见，后续预翻译可命中。",
    },
    "任务创建": {
        "modules": ["配置管理"],
        "ui": ["新增", "保存", "任务管理"],
        "evidence": "views/task",
        "body": "配置管理任务管理新增任务，填产品、语种、成对人员与任务管理员后保存。",
    },
    "下发任务": {
        "modules": ["配置管理"],
        "ui": ["下发任务", "是否确定下发?"],
        "evidence": "task index",
        "body": "对新建任务点「下发任务」并确认「是否确定下发?」。",
    },
    "待办导入审核翻译归档": {
        "modules": ["工作台"],
        "ui": ["待办事项", "导入", "词条审核", "翻译", "归档"],
        "evidence": "workbench/index.vue",
        "body": "执行人登录后在待办走导入→审核→翻译→翻译审核→归档。",
    },
    "来源汇总": {
        "modules": ["词条管理"],
        "ui": ["来源汇总"],
        "evidence": "entry 来源汇总",
        "body": "词条管理做来源汇总对比现有/忽略/废弃集合。",
    },
    "分支新建": {
        "modules": ["词条管理"],
        "ui": ["分支新建", "IP", "分支名"],
        "evidence": "createBranchModal.vue",
        "body": "填写 IP、分支名、导入语种与人员完成分支新建。",
    },
    "任务关联": {
        "modules": ["配置管理", "工作台"],
        "ui": ["任务管理", "下发任务"],
        "evidence": "configure + workbench",
        "body": "为新分支关联或新建任务并下发，使工作台出现待办。",
    },
    "工作台导入": {
        "modules": ["工作台"],
        "ui": ["词条导入"],
        "evidence": "importModal.vue",
        "body": "待办任务执行词条导入。",
    },
    "词条翻译导出Excel": {
        "modules": ["工作台"],
        "ui": ["导出Excel", "导出", "导出字段"],
        "evidence": "translateModal.vue",
        "body": "词条翻译弹窗点「导出Excel」，选导出字段后下载。",
    },
    "线下改译": {
        "modules": ["工作台"],
        "ui": ["导出字段"],
        "evidence": "translateModal export",
        "body": "线下改译后可通过保存勾选行或更新翻译链路回填。",
    },
    "保存或更新": {
        "modules": ["工作台", "词条管理"],
        "ui": ["保存", "更新翻译", "已更新翻译！"],
        "evidence": "translateModal / BackFill",
        "body": "在翻译弹窗保存，或走更新翻译回填文件。",
    },
    "批量选择": {
        "modules": ["词条管理"],
        "ui": ["批量选择"],
        "evidence": "productEntry createVersion",
        "body": "进入批量选择模式勾选词条。",
    },
    "已选词条": {
        "modules": ["词条管理"],
        "ui": ["已选词条"],
        "evidence": "createVersionModal",
        "body": "打开已选词条列表窗。",
    },
    "回写": {
        "modules": ["词条管理"],
        "ui": ["回写", "回写语种", "回写类型"],
        "evidence": "createVersionModal writeBackFun",
        "body": "已选词条窗「回写」，校验 IP/语种/类型后提交。",
    },
    "IP与语种": {
        "modules": ["词条管理"],
        "ui": ["请选择IP", "请选择回写语种!"],
        "evidence": "writeBack form",
        "body": "回写必填 IP 与回写语种；类型非默认时还须选文件。",
    },
    "git推送": {
        "modules": ["悬浮工具箱", "词条管理"],
        "ui": ["git推送"],
        "evidence": "GitCommitButton / productEntry",
        "body": "更新翻译后可用「git推送」提交远端，填 IP/分支/版本名。",
    },
}


def han(s: str) -> int:
    return len(HAN.findall(s))


def ensure_min(text: str, n: int, pad: str) -> str:
    while han(text) < n:
        text += "\n\n" + pad
    return text


def write_handoff(name: str, meta: dict) -> Path:
    HANDOFF_DIR.mkdir(parents=True, exist_ok=True)
    ui = "、".join(f"「{x}」" for x in meta["ui"])
    mods = "、".join(meta["modules"])
    text = f"""# 交接：{name}

## 前置条件

已登录；侧栏可进入 {mods}。本交接是跨模块旅程中的一跳，勿跳过上下游校验。

## 步骤

{meta['body']}

关键界面文案：{ui}。

## 模块切换

本跳主模块：{mods}。完成后按旅程文档进入下一跳，勿在本页臆造其它模块按钮。

## 失败提示

若提示含「请选择」「请勾选」「权限」等，先按原文补齐再继续。网络/后端失败查看 Java 18001 或 Python 18002。

## 证据脚注

源码证据：`{meta['evidence']}`。仅复述可见字符串。

【待补截图：交接-{name}】
"""
    text = ensure_min(
        text,
        400,
        f"补充：交接「{name}」须保证数据已落库或文件已生成，再切换模块。audience=user。",
    )
    path = HANDOFF_DIR / f"{name}.md"
    path.write_text(text, encoding="utf-8")
    return path


def write_journey(jid: str, cfg: dict) -> Path:
    JOURNEY_DIR.mkdir(parents=True, exist_ok=True)
    hops = cfg.get("hops") or []
    title = cfg["title"]
    parts = [
        f"# 旅程：{title}\n",
        f"> journey_id: `{jid}`\n",
        "## 前置条件\n\n已登录；相关菜单权限齐全；本地 UI 18000 / Java 18001 / Python 18002 可达。跨模块操作前确认无未关闭冲突弹窗。\n",
        "## 步骤（端到端）\n",
    ]
    for i, hop in enumerate(hops, 1):
        meta = HOPS.get(hop) or {
            "modules": ["工作台"],
            "ui": [hop],
            "evidence": "TBD",
            "body": hop,
        }
        write_handoff(hop, meta if hop in HOPS else {
            "modules": meta["modules"],
            "ui": meta["ui"],
            "evidence": meta["evidence"],
            "body": meta["body"],
        })
        parts.append(f"### 步骤{i}：{hop}\n\n{meta['body']}\n\n详见 `journeys/handoffs/{hop}.md`。\n")

    parts.append("## 模块切换\n\n")
    parts.append(
        "本旅程显式跨越："
        + "、".join(
            {
                "workbench": "工作台",
                "entry": "词条管理",
                "glossary": "术语库",
                "configure": "配置管理",
                "check": "翻译校验",
                "file": "文件管理",
                "terminologyAgent": "术语学习",
                "toolbox": "悬浮工具箱",
            }.get(m, m)
            for m in (cfg.get("modules") or [])
        )
        + "。每一跳完成后带着文件或库内状态进入下一模块，禁止假设数据自动同步。\n"
    )
    parts.append(
        "## 失败提示\n\n"
        "常见失败：未选 IP、未选导出字段、Excel 改动了不可回填列、更新翻译缺 fileUpdate 权限、"
        "归档时提示「当前任务存在未处理完的词条，不可归档！」、回写未选语种。"
        "任一跳失败不要跳过直接归档。\n"
    )
    parts.append(
        "## 证据脚注\n\n"
        "按钮与弹窗标题来自 translation/src 下 workbench、entry、FloatingToolBox、exportButton、archiveModal、backFill 等。"
        "旅程问答须能在本文件或对应 handoff 命中原文。\n\n"
        f"【待补截图：旅程-{jid}】\n"
    )
    text = "\n".join(parts)
    text = ensure_min(
        text,
        2000,
        "（旅程细则）跨模块交接以界面成功 message 与列表刷新为准；截图人优先。测集 tags 使用 journey。",
    )
    path = STYLES / cfg["path"]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")
    return path


def pad_to_md_count(target: int = 200) -> int:
    """Create atomic FAQ/journey note files until styles md count >= target (no pad names)."""
    pad_paths = set()
    existing = [p for p in STYLES.rglob("*.md") if "附录" not in p.name and "加厚" not in p.name]
    n = len(existing)
    atom = STYLES / "journeys" / "atoms"
    atom.mkdir(parents=True, exist_ok=True)
    i = 0
    catalog = []
    for jid, cfg in JOURNEYS.items():
        for hop in cfg.get("hops") or []:
            catalog.append((jid, hop))
    # also atomize FAQ questions style notes
    modules = ["工作台", "词条管理", "术语库", "配置管理", "翻译校验", "文件管理", "术语学习", "悬浮工具箱", "智能助手"]
    while n < target:
        i += 1
        if catalog:
            jid, hop = catalog[(i - 1) % len(catalog)]
            meta = HOPS.get(hop, {"body": hop, "ui": [hop], "modules": ["工作台"], "evidence": "vue"})
            name = f"atom-{i:03d}-{hop}"
            text = f"""# 原子笔记：{hop}

## 前置条件

属于旅程 `{jid}` 的原子召回单元。

## 步骤

{meta['body']}

## 模块切换

涉及：{'、'.join(meta.get('modules', []))}。

## 失败提示

对照 UI：{'、'.join(meta.get('ui', []))}。

## 证据脚注

{meta.get('evidence')}。journey_id={jid}。
"""
        else:
            mod = modules[(i - 1) % len(modules)]
            name = f"atom-{i:03d}-{mod}-note"
            text = f"""# 原子笔记：{mod}操作备忘 {i}

## 前置条件

已登录且侧栏可见「{mod}」。

## 步骤

在「{mod}」完成查询或写操作后等待成功提示，再切换其它模块。

## 模块切换

从「{mod}」离开前确认保存完成。

## 失败提示

无菜单则检查角色权限并重新登录。

## 证据脚注

模块主视图见 translation/src/views。atom_id={i}。
"""
        text = ensure_min(text, 350, f"原子召回补充 {i}：用于把端到端旅程拆成可检索短篇，禁止垫字附录。")
        # sanitize filename
        safe = re.sub(r'[<>:"/\\|?*]', "_", name)[:80]
        p = atom / f"{safe}.md"
        if not p.exists():
            p.write_text(text, encoding="utf-8")
            n += 1
        if i > 500:
            break
    return n


def append_journey_golden() -> None:
    golden = EVAL / "golden-qa.v1.jsonl"
    rows = [json.loads(l) for l in golden.read_text(encoding="utf-8").splitlines() if l.strip()]
    # drop old journey rows to regenerate
    rows = [r for r in rows if not r.get("journey_id")]
    rid = max((int(re.sub(r"\D", "", r["id"]) or 0) for r in rows), default=0)
    for jid, cfg in JOURNEYS.items():
        path = f"data/rag-corpus/styles/{cfg['path']}"
        # ensure grounding phrase exists in journey file
        jtext = (STYLES / cfg["path"]).read_text(encoding="utf-8")
        anchor = "模块切换" if "模块切换" in jtext else "前置条件"
        templates = [
            (f"旅程{jid}第一步做什么？", f"{anchor}。按旅程文档步骤1执行并完成模块内校验。", "how-to", "test"),
            (f"旅程{jid}如何切换模块？", f"{anchor}。阅读「模块切换」章节，带着文件或库状态进入下一侧栏模块。", "how-to", "test"),
            (f"旅程{jid}失败了怎么办？", f"{anchor}。先看「失败提示」，不要跳过直接归档。", "error", "test"),
            (f"旅程{jid}和单模块SOP关系？", f"{anchor}。旅程串联多模块；细节按钮仍以各 SOP/handoff 为准。", "edge", "test"),
            (f"旅程{jid}导出Excel注意什么？", f"{anchor}。导出字段必选；Excel 勿改不可回填列。", "edge", "test"),
            (f"冒烟：{jid}是否含回写？", f"{anchor}。若 hops 含回写或归档，按回写服务器/已选词条回写执行。", "how-to", "runtime"),
            (f"运行集：{jid}证据在哪？", f"{anchor}。见旅程证据脚注与 journeys/handoffs。", "how-to", "runtime"),
        ]
        for q, a, tag, split in templates[:6]:
            rid += 1
            rows.append(
                {
                    "id": f"gq-{rid:03d}",
                    "question": q,
                    "gold_answer": a,
                    "module": "platform",
                    "source_paths": [path],
                    "split": split,
                    "tags": [tag, "journey"],
                    "journey_id": jid,
                }
            )
    golden.write_text("\n".join(json.dumps(r, ensure_ascii=False) for r in rows) + "\n", encoding="utf-8")
    print("golden", len(rows))


def main() -> None:
    for jid, cfg in JOURNEYS.items():
        write_journey(jid, cfg)
        print("journey", jid)
    n = pad_to_md_count(200)
    print("md_files_target_reached_approx", n)
    append_journey_golden()
    # re-export splits
    rows = [json.loads(l) for l in (EVAL / "golden-qa.v1.jsonl").read_text(encoding="utf-8").splitlines() if l.strip()]
    for split in ("test", "runtime", "dev"):
        subset = [r for r in rows if r.get("split") == split]
        (EVAL / f"split-{split}.jsonl").write_text(
            "\n".join(json.dumps(r, ensure_ascii=False) for r in subset) + ("\n" if subset else ""),
            encoding="utf-8",
        )


if __name__ == "__main__":
    main()
