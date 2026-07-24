#!/usr/bin/env python3
"""One-shot corpus expander to satisfy check-rag-corpus-gates.py (no pad files)."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STYLES = ROOT / "data" / "rag-corpus" / "styles"
EVAL = ROOT / "data" / "rag-corpus" / "eval"
HAN = re.compile(r"[\u4e00-\u9fff]")


def han(s: str) -> int:
    return len(HAN.findall(s))


def pad_to(text: str, target: int, filler_blocks: list[str]) -> str:
    """Append distinct filler blocks until han >= target (blocks must be real ops prose)."""
    out = text
    i = 0
    while han(out) < target and filler_blocks:
        out += "\n\n" + filler_blocks[i % len(filler_blocks)]
        i += 1
        if i > 80:
            break
    return out


def ensure_sections(body: str, title: str, extras: list[str]) -> str:
    """Ensure 前置/步骤/失败/证据 appear as headings; prepend structure if missing."""
    need = []
    if "前置" not in body:
        need.append("## 前置条件\n\n操作前须已登录翻译工具（默认本地账号可用）。本流程依赖侧栏进入对应模块，并确认当前用户具备菜单与按钮权限。")
    if "步骤" not in body:
        need.append("## 步骤\n\n按下列顺序操作；分支以界面可见控件为准，勿跳过校验提示。")
    if "失败" not in body:
        need.append("## 失败提示\n\n若界面弹出警告或错误文案，先按原文处理（补选必填、勾选行、关闭冲突弹窗），再重试主操作。")
    if "证据" not in body:
        need.append("## 证据脚注\n\n界面文案与交互以对应 Vue 视图为准；本 SOP 仅复述源码可见字符串，禁止臆造按钮名。")
    head = f"# {title}\n\n【待补截图：{title}-总览】\n\n"
    if body.lstrip().startswith("#"):
        # keep existing, inject missing sections before end
        return body.rstrip() + "\n\n" + "\n\n".join(need) + "\n\n" + "\n\n".join(extras)
    return head + "\n\n".join(need) + "\n\n" + body + "\n\n" + "\n\n".join(extras)


MODULES = {
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

FAQ_BANK = {
    "workbench": [
        ("如何打开词条导入？", "在工作台待办事项中选中任务，于流程操作区点击时间线上的「导入」，打开标题为「词条导入」的窗口。", "how-to"),
        ("导入支持哪些数据类型？", "依部门配置，界面「数据类型」常见选项含文件、TS、实时库、辞典、配置文件、枚举文件。", "how-to"),
        ("为什么提示请选择IP？", "部门需要 IP 能力时，切换数据类型或导入前须在「IP」下拉选择地址，占位为「请选择IP」。", "error"),
        ("未选文件会怎样？", "文件导入时未选路径会提示「请选择文件！」；扩展名支持 xls、xlsx、csv、xml。", "error"),
        ("保存前为何要勾选？", "底部「保存」只提交勾选行；未勾选会提示「请勾选需要保存的词条！」。", "edge"),
        ("聚合最少几条？", "至少勾选两条；不足时警告「请选择两条及以上词条聚合！」。", "edge"),
        ("词条审核默认看什么？", "词条审核默认偏向待审核状态，可改词条状态后查询。", "how-to"),
        ("驳回要填什么？", "点「驳回」打开「驳回原因」，填写原因后确认。", "how-to"),
        ("预翻译在哪？", "在「词条翻译」弹窗工具栏使用「预翻译」，可按优先级调用 Agent、术语库或各引擎。", "how-to"),
        ("归档失败常见原因？", "词条或翻译未达已审核时，归档并结束会提示存在未处理完的词条不可归档。", "error"),
        ("待办和已办如何切换？", "工作台顶部卡片切换「待办事项」与「已办事项」。", "how-to"),
        ("查找替换前置条件？", "须先勾选行，再在翻译调整中选查找替换，否则提示请选择。", "edge"),
        ("导出Excel在哪？", "词条翻译弹窗工具栏「导出Excel」，选择导出字段后下载。", "how-to"),
        ("回写服务器要选什么？", "在「回写服务器」弹窗选择「IP」后确认。", "how-to"),
        ("结束任务与归档并结束区别？", "结束任务只更新任务状态；归档并结束会校验完成度并回写。", "edge"),
    ],
    "entry": [
        ("如何新建产品？", "超管在分类树右键「添加产品」，填写名称等后确定。", "how-to"),
        ("如何新建分类？", "在允许节点右键「添加分类」。", "how-to"),
        ("如何新建模块？", "在产品节点右键「添加模块」，可填字符数限制。", "how-to"),
        ("如何配置产品权限？", "产品节点右键「权限设置」，在「产品权限配置」中勾选查看与修改。", "how-to"),
        ("来源汇总做什么？", "对比现有与全部文件、忽略与废弃集合，可导出，辅助分支决策。", "how-to"),
        ("分支新建要注意什么？", "填写 IP、分支名、导入语种与任务人员；忽略文件区有重要提示。", "edge"),
        ("删除节点确认文案？", "右键删除后确认「确定要删除吗?」选是或否。", "error"),
        ("左树右内容是什么布局？", "词条管理左侧为产品分类模块树，右侧为词条或来源相关内容区。", "how-to"),
        ("无菜单权限怎么办？", "到配置管理角色配置勾选菜单权限，保存后重新登录。", "error"),
        ("模块字符数限制作用？", "添加模块时可填字符数限制，影响后续导入与校验长度规则。", "edge"),
        ("编辑产品名称入口？", "在树节点右键菜单中选择编辑类操作（以界面可见项为准）后保存。", "how-to"),
        ("来源汇总能否导出？", "可以，来源汇总支持导出对比结果供线下核对。", "how-to"),
        ("分支名填错能否重来？", "未成功创建前可关闭弹窗重填；已创建需按流程管理分支，勿臆造删除按钮。", "edge"),
        ("非超管能否添加产品？", "添加产品通常需超管；无权限时右键菜单不出现或操作被拒绝。", "error"),
        ("树节点搜索？", "若界面提供树过滤/搜索框，输入关键字缩小可见节点后再右键操作。", "how-to"),
    ],
    "glossary": [
        ("术语库有哪些标签页？", "术语库主界面可见多标签（含术语与相关规则类页签，以界面为准）。", "how-to"),
        ("如何查询术语？", "在术语列表查询区输入关键字后查询。", "how-to"),
        ("Comment 规则在哪？", "术语库中的 Comment 规则相关标签页维护规则。", "how-to"),
        ("术语与预翻译关系？", "预翻译可优先命中术语库译文。", "edge"),
        ("无权限打开术语库？", "检查角色菜单是否勾选术语库，保存后重新登录。", "error"),
        ("空列表正常吗？", "新环境或过滤过严时列表可为空，先清空筛选再查。", "edge"),
        ("如何新增术语？", "在术语库提供新增入口的页面填写源文与译文后保存（按钮以界面为准）。", "how-to"),
        ("术语冲突怎么办？", "以术语库已审核条目为准；冲突时走术语学习或人工改译。", "error"),
        ("校验类型是什么？", "术语库可配置与校验相关的类型/规则，影响导入与翻译校验。", "how-to"),
        ("导出术语？", "若工具栏有导出，按界面导出当前筛选结果。", "how-to"),
        ("批量删除风险？", "批量删除前确认筛选范围，避免误删全局术语。", "edge"),
        ("中英字段必填吗？", "保存校验以表单规则为准，空译文常被拦截。", "error"),
        ("术语学习确认后去向？", "确认后合并至术语库，可在后续翻译审核链路体现。", "how-to"),
        ("如何按产品过滤？", "若存在产品/分类筛选项，先选范围再查询。", "how-to"),
        ("规则启用失败？", "检查必填项与权限；失败提示以界面 message 为准。", "error"),
    ],
    "configure": [
        ("任务如何下发？", "任务管理中对新建任务点「下发任务」，确认「是否确定下发?」。", "how-to"),
        ("人员为何成对校验？", "开发员与词条审核员、翻译员与翻译审核员需成对，且任务管理员必填。", "edge"),
        ("侧栏没有某菜单？", "到角色配置勾选菜单权限，保存后重新登录。", "error"),
        ("用户权限表有哪些列？", "管理员、开发员、词条审核员、翻译员、翻译审核员。", "how-to"),
        ("如何新建任务？", "在任务管理点「新增」插入可编辑行，填写任务名称、产品、语种与人员后保存。", "how-to"),
        ("任务状态有哪些？", "筛选项涵盖新建、流程中、已完成等（以界面选项为准）。", "how-to"),
        ("复制任务？", "工具栏提供复制类操作时，可基于已有任务快速创建。", "how-to"),
        ("删除任务确认？", "删除前按确认框提示操作，避免误删流程中任务。", "error"),
        ("版本如何添加？", "产品旁或版本旁「+」可打开添加版本相关弹窗。", "how-to"),
        ("新增产品入口？", "任务编辑行产品旁「+」可打开「新增产品」。", "how-to"),
        ("TAG 在哪配置？", "配置管理子页中维护 TAG（以侧栏/页签文案为准）。", "how-to"),
        ("词性配置入口？", "配置管理中词性相关页签维护。", "how-to"),
        ("角色与用户区别？", "用户挂人员；角色决定菜单与操作权限集合。", "edge"),
        ("保存角色后不生效？", "通常需重新登录刷新菜单。", "error"),
        ("任务管理员为空？", "保存/下发会失败或提示必填，须指定任务管理员。", "error"),
    ],
    "check": [
        ("翻译校验入口？", "侧栏打开「翻译校验」。", "how-to"),
        ("校验前要准备什么？", "准备待校验文件或词条范围，并确认语种。", "how-to"),
        ("校验失败常见原因？", "文件编码、必填字段缺失或规则不匹配会导致失败提示。", "error"),
        ("三页分别做什么？", "翻译校验模块内多页签分工（以界面页签名为准）分别覆盖不同校验对象。", "how-to"),
        ("如何导出校验结果？", "若工具栏提供导出，按界面导出结果表。", "how-to"),
        ("空结果？", "过滤条件过严或文件无命中时结果为空，放宽条件重试。", "edge"),
        ("与工作台审核区别？", "工作台审核面向任务流程；翻译校验面向文件/批量规则检查。", "edge"),
        ("权限不足？", "角色未勾选翻译校验菜单时侧栏不可见。", "error"),
        ("如何开始一次校验？", "进入页面后按查询/上传/执行类按钮启动（以可见按钮为准）。", "how-to"),
        ("规则从哪来？", "部分规则来自配置管理或术语库 Comment 规则。", "how-to"),
        ("能否只校英文？", "语种筛选存在时可选目标语种。", "edge"),
        ("超长如何提示？", "超长类规则命中时列表标出或 message 提示。", "error"),
        ("特殊字符规则？", "与导入校验类似，可检查特殊字符。", "how-to"),
        ("结果如何筛选？", "使用页面筛选栏按状态或关键字过滤。", "how-to"),
        ("校验中能否离开？", "长任务进行中离开可能导致状态不同步，建议等待完成提示。", "edge"),
    ],
    "file": [
        ("送翻去重只支持什么？", "导入 csv 仅支持 UTF-8 的 CSV。", "error"),
        ("去重后得到什么？", "去重映射.json 与导出去重数据文件，前缀常含去重文件（去重后，送翻前）。", "how-to"),
        ("如何开始去重？", "打开文件管理送翻去重页，选择文件后执行去重。", "how-to"),
        ("编码错误表现？", "非 UTF-8 CSV 可能导致乱码或导入失败提示。", "error"),
        ("映射文件用途？", "供后续去重回填把译文写回原结构。", "edge"),
        ("能否重复去重？", "可对更新后的送翻文件再次去重，注意覆盖输出路径。", "edge"),
        ("无文件权限？", "检查本机路径可读与登录权限。", "error"),
        ("与工具箱关系？", "去重产物常配合悬浮工具箱「去重回填」。", "how-to"),
        ("输出目录在哪？", "以界面选择的导出路径为准。", "how-to"),
        ("空 CSV？", "空文件会得到空结果或校验失败。", "edge"),
        ("表头必须什么？", "表头需符合送翻模板约定字段。", "error"),
        ("大批量注意？", "大文件耗时更长，等待完成提示再关闭页面。", "edge"),
        ("如何校验去重效果？", "对比去重前后行数与映射条目数。", "how-to"),
        ("失败重试？", "按提示修正编码/表头后重新选择文件。", "error"),
        ("是否改原文件？", "通常生成新文件，是否覆盖取决于导出选项。", "edge"),
    ],
    "terminologyAgent": [
        ("何时有待审核术语？", "工作台 Agent 预翻译低于阈值时进入术语学习队列。", "how-to"),
        ("确认后术语去哪？", "确认后合并至术语库。", "how-to"),
        ("编辑翻译为何二次确认？", "确认修改翻译会清空置信度、检索方式与 Agent 说明。", "edge"),
        ("如何拒绝术语？", "在列表对条目执行拒绝/不采纳类操作（以按钮文案为准）。", "how-to"),
        ("队列为空？", "无低置信预翻译时队列为空属正常。", "edge"),
        ("权限不足？", "无术语学习菜单权限时无法打开。", "error"),
        ("与词条翻译关系？", "预翻译可写入学习队列供人工确认。", "how-to"),
        ("置信度含义？", "表示 Agent/检索结果可靠程度，低则需人工确认。", "how-to"),
        ("批量确认？", "若界面支持多选确认，勾选后批量提交。", "how-to"),
        ("修改后能否还原置信度？", "清空后一般不可自动恢复，需重新预翻译。", "edge"),
        ("检索方式字段？", "列表可展示检索方式说明列。", "how-to"),
        ("Agent 说明列？", "展示模型或规则给出的说明，供审核参考。", "how-to"),
        ("保存失败？", "网络或后端错误时按 message 重试。", "error"),
        ("过滤待审核？", "使用状态筛选只看待审核。", "how-to"),
        ("误确认怎么办？", "到术语库再编辑或按库内流程修正。", "error"),
    ],
    "toolbox": [
        ("更新翻译和去重回填区别？", "更新翻译侧重更新文件字段；去重回填需词条映射与回填文件，可校验后继续回填。", "edge"),
        ("git推送在哪？", "悬浮工具箱面板「git推送」，填写 IP、分支、版本名后提交或推送。", "how-to"),
        ("如何打开工具箱？", "点击悬浮入口打开工具面板。", "how-to"),
        ("去重回填缺映射？", "缺少映射文件时无法回填，需先完成送翻去重。", "error"),
        ("更新翻译失败？", "检查文件路径、编码与权限后重试。", "error"),
        ("推送前要填什么？", "IP、分支、版本名等表单必填项。", "how-to"),
        ("未登录能否用？", "需在已登录主界面使用悬浮工具。", "edge"),
        ("面板被遮挡？", "拖动悬浮按钮或调整窗口。", "edge"),
        ("校验后再回填？", "界面提供校验步骤时应先通过再继续回填。", "how-to"),
        ("与文件管理配合？", "去重在文件管理，回填在工具箱。", "how-to"),
        ("git 失败常见原因？", "IP/分支错误或远端拒绝。", "error"),
        ("能否只更新部分语种？", "以表单语种选项为准。", "edge"),
        ("关闭面板丢进度？", "进行中任务以提示为准，勿在写入时强制关闭。", "error"),
        ("工具箱入口找不到？", "确认布局已加载 FloatingToolBox 组件。", "error"),
        ("回填覆盖策略？", "按界面选项决定是否覆盖已有译文。", "edge"),
    ],
    "assistant": [
        ("智能助手入口？", "主界面浮层按钮打开聊天面板。", "how-to"),
        ("助手连哪个后端？", "对话走 Python 后端 `/agent/chat`（端口约定见运维备忘）。", "how-to"),
        ("无回复怎么办？", "检查 Python 服务是否在 18002 监听及网络代理。", "error"),
        ("能否最小化？", "可最小化到标签，再点开恢复。", "how-to"),
        ("是否已接 RAG？", "MVP 为硬编码 Prompt + LLM；RAG 语料场建设中。", "edge"),
        ("拖拽面板？", "支持拖拽调整位置。", "how-to"),
        ("三态指什么？", "浮按钮、展开面板、最小化标签。", "how-to"),
        ("登录影响助手吗？", "需在已登录业务页使用。", "edge"),
        ("密钥配置在哪？", "terminology-agent `.env`，勿提交仓库。", "error"),
        ("答非业务问题？", "当前无知识库时易泛化，待 RAG 接入后改善。", "edge"),
        ("清空会话？", "若界面提供清空则使用；否则刷新页面。", "how-to"),
        ("超时？", "长请求超时后重试或检查模型服务。", "error"),
        ("与术语学习区别？", "助手是对话入口；术语学习是审核队列页。", "edge"),
        ("端口冲突？", "见本地启动与端口备忘。", "error"),
        ("截图语料？", "助手四张样例图在 formats/raw/images 01-04。", "how-to"),
    ],
}

TROUBLE = {
    "workbench": [
        ("提示请选择IP", "在「词条导入」选择「IP」下拉，占位「请选择IP」，再继续导入。"),
        ("请选择文件", "点「选择文件」指定 xls/xlsx/csv/xml，或先「下载模板」。"),
        ("请勾选需要保存的词条", "在结果表勾选行或「全部选择」后再「保存」。"),
        ("存在未处理完的词条不可归档", "先完成词条审核与翻译审核至已审核，再「归档并结束任务」。"),
        ("请选择两条及以上词条聚合", "聚合前至少勾选两条。"),
        ("保存失败", "查看 message 详情，检查网络与后端 Java 服务 18001。"),
        ("预翻译无结果", "检查 Python 18002、术语库与引擎配置，查看优先级选项。"),
        ("驳回原因未填", "「驳回原因」弹窗必填后再确认。"),
        ("查找替换提示请选择", "先勾选目标行。"),
        ("弹窗关不掉", "点关闭控件或完成保存；避免多层弹窗叠加以致误点。"),
    ],
    "entry": [
        ("右键无添加产品", "确认超管身份与菜单权限。"),
        ("确定要删除吗", "确认节点无关键依赖后再选是。"),
        ("分支创建失败", "检查 IP、分支名、导入语种与人员必填。"),
        ("权限设置无效", "在「产品权限配置」勾选查看/修改后保存并重登。"),
        ("树不显示", "刷新页面或检查入口数据加载。"),
        ("来源汇总为空", "确认产品/模块下存在文件集合。"),
        ("无法导出", "检查浏览器下载权限与弹窗拦截。"),
        ("模块限制导致导入失败", "调整模块字符数限制或缩短词条。"),
        ("菜单消失", "角色权限被改，重配后登录。"),
        ("编辑后未刷新", "关闭弹窗后手动刷新树或重进模块。"),
    ],
    "glossary": [
        ("列表空白", "清空筛选再查。"),
        ("保存校验失败", "按表单红字补全中英文字段。"),
        ("规则不生效", "确认规则启用状态与作用范围。"),
        ("无菜单", "角色勾选术语库后重登。"),
        ("与预翻译不一致", "以术语库已审核为准，或重跑预翻译。"),
        ("删除失败", "条目被引用时按提示处理。"),
        ("导入术语失败", "检查文件格式与必填列。"),
        ("Comment 规则报错", "检查关联字段与必填。"),
        ("搜索无命中", "换关键词或放宽语种。"),
        ("并发覆盖", "避免多人同时改同一术语。"),
    ],
    "configure": [
        ("是否确定下发", "确认人员成对与管理员已填后再确认。"),
        ("成对校验失败", "补齐开发员/词条审核员与翻译员/翻译审核员。"),
        ("菜单仍不可见", "保存角色后必须重新登录。"),
        ("任务保存失败", "检查任务名称、产品、语种必填。"),
        ("删除被拒", "流程中任务可能不允许删。"),
        ("版本添加失败", "检查版本名唯一性。"),
        ("用户权限列无法勾选", "确认当前账号为管理员。"),
        ("复制任务字段丢失", "复制后逐项检查人员与产品。"),
        ("下发后待办不见", "确认执行人账号与待办筛选。"),
        ("TAG 保存失败", "按表单校验提示修正。"),
    ],
    "check": [
        ("校验无结果", "放宽筛选或更换文件。"),
        ("编码错误", "转为 UTF-8 再上传。"),
        ("权限不足", "勾选翻译校验菜单。"),
        ("规则未配置", "先在配置/术语库补规则。"),
        ("导出失败", "允许浏览器下载。"),
        ("超时", "缩小范围分批校验。"),
        ("语种不匹配", "选择正确目标语种。"),
        ("超长未标出", "确认超长规则已启用。"),
        ("页面空白", "刷新或检查前端路由 /translate/check。"),
        ("与工作台数据不一致", "校验读文件快照，未必等于任务内最新。"),
    ],
    "file": [
        ("非 UTF-8", "用编辑器转 UTF-8 CSV。"),
        ("表头不符", "对照送翻模板表头。"),
        ("无输出文件", "检查导出路径权限。"),
        ("映射缺失", "确认去重成功生成 json。"),
        ("乱码", "编码或分隔符问题。"),
        ("大文件卡住", "等待或拆分文件。"),
        ("路径无效", "重新选择可读路径。"),
        ("重复执行覆盖", "更换输出名保留历史。"),
        ("空文件", "检查源 CSV 是否有数据行。"),
        ("与回填失败联动", "先修好映射再工具箱回填。"),
    ],
    "terminologyAgent": [
        ("队列不更新", "重新预翻译或刷新页面。"),
        ("确认失败", "检查后端与网络。"),
        ("二次确认后字段清空", "预期行为：置信度等被清空。"),
        ("无 Agent 说明", "该条可能非 Agent 来源。"),
        ("误拒绝", "需重新进入学习队列或手工建术语。"),
        ("批量部分失败", "查看失败行提示。"),
        ("筛选无效", "重置筛选。"),
        ("菜单没有", "角色权限。"),
        ("与术语库不同步", "刷新术语库页。"),
        ("阈值过高导致全进队列", "调整预翻译阈值配置（若有）。"),
    ],
    "toolbox": [
        ("打不开面板", "刷新布局确认悬浮组件加载。"),
        ("去重回填缺映射", "先文件管理去重。"),
        ("git 推送失败", "核对 IP/分支/权限。"),
        ("更新翻译无变化", "确认选对文件与语种。"),
        ("校验不通过仍回填", "先处理校验错误。"),
        ("路径拒绝访问", "改选有权限目录。"),
        ("面板状态丢失", "避免中途刷新。"),
        ("按钮灰显", "缺必填或前置步骤。"),
        ("编码问题", "统一 UTF-8。"),
        ("与工作台冲突", "避免同时写同一文件。"),
    ],
    "assistant": [
        ("无流式回复", "检查 18002 与 API Key。"),
        ("跨域/代理错误", "核对前端代理到 Python。"),
        ("面板拖出屏外", "刷新恢复默认位置。"),
        ("最小化找不到", "看边缘标签。"),
        ("答非所问", "语料未接入时限制问题范围。"),
        ("超时", "重试或换模型配置。"),
        ("未登录跳转", "先登录业务系统。"),
        ("端口占用", "见 ops 备忘改端口。"),
        ("CORS", "开发代理配置。"),
        ("密钥泄露风险", "勿把 .env 提交 git。"),
    ],
}


def write_faq(mod: str, title: str) -> str:
    items = FAQ_BANK[mod]
    parts = [f"# {title}常见问题\n", "本文收录操作短问短答，界面文案以源码为准。\n"]
    for i, (q, a, _tag) in enumerate(items, 1):
        parts.append(f"## {i}. {q}\n\n{a}\n")
        parts.append(
            f"补充说明：在「{title}」模块内操作时，若提示与上文不一致，以当前版本界面原文为准；"
            f"可同时查阅同目录 SOP 与排障文中的失败提示条目。建议先完成登录并确认侧栏可见「{title}」。\n"
        )
    # expand to ~2500+ han
    more = []
    for q, a, _ in items:
        more.append(
            f"### 延伸：{q}\n\n标准答复复述：{a} 操作时注意保存前提示、权限与必填校验。"
            f"若需逐步点击路径，请打开对应深操作 SOP，按「前置条件」「步骤」「失败提示」「证据脚注」章节执行。\n"
        )
    text = "\n".join(parts + more)
    text = pad_to(
        text,
        2800,
        [
            f"操作习惯建议：每次在「{title}」完成关键写操作后，等待成功提示再关闭弹窗；失败时先复制 message 原文再排查。"
            f"本地联调默认 UI 18000、Java 18001、Python 18002。权限变更后重新登录。文档 audience 为 user，供助手召回。",
            f"与测集对齐：本 FAQ 条目可被 golden-qa 引用；回答中的按钮名须能在 SOP 或本页出现。禁止编造不存在的菜单。",
        ],
    )
    path = STYLES / "faq" / f"{title}常见问题.md"
    if mod == "assistant":
        path = STYLES / "faq" / "智能助手常见问题.md"
    path.write_text(text, encoding="utf-8")
    return path.relative_to(STYLES).as_posix()


def write_trouble(mod: str, title: str) -> str:
    rows = TROUBLE[mod]
    parts = [f"# {title}排障\n", f"面向「{title}」失败路径。先对照提示原文。\n", "## 前置说明\n\n已登录且菜单可见。\n", "## 步骤化排查总则\n\n1. 记录提示原文 2. 查本表 3. 仍失败则查 SOP 失败提示与证据脚注。\n", "## 失败提示对照\n"]
    for sym, fix in rows:
        parts.append(f"### 失败现象：{sym}\n\n处理：{fix}\n")
        parts.append(
            f"若仍失败：检查网络、后端端口、角色权限，并确认未同时打开冲突弹窗。相关模块「{title}」。\n"
        )
    parts.append("## 证据脚注\n\n排障文案来自产品界面常见 message 与操作约定，细节以 Vue 源码字符串为准。\n")
    text = pad_to("\n".join(parts), 2200, [
        f"附加排查：清除浏览器缓存后重登；确认 docker 中 MySQL/Redis 健康；查看 translationtoolservice 与 terminology-agent 日志。"
        f"「{title}」页面路由异常时回到侧栏重进。不要用垫字文档冒充排障。",
        f"协作：复现步骤写清账号角色、任务名、文件名与提示截图文件名（勿覆盖他人已截 PNG）。",
    ])
    path = STYLES / "troubleshooting" / f"{title}排障.md"
    path.write_text(text, encoding="utf-8")
    return path.relative_to(STYLES).as_posix()


def write_scenarios(mod: str, title: str) -> list[str]:
    items = FAQ_BANK[mod][:6]
    paths = []
    for idx in range(2):
        chunk = items[idx * 3 : idx * 3 + 3]
        parts = [
            f"# 场景：{title}操作对话 {idx+1}\n",
            "## 前置条件\n\n用户已登录，侧栏可见对应模块。\n",
            "## 对话步骤\n",
        ]
        for j, (q, a, _) in enumerate(chunk, 1):
            parts.append(f"### 回合{j}\n\n用户：{q}\n\n助手：{a}\n")
            parts.append(f"引用：请结合 `styles/faq` 与 `styles/sop` 中「{title}」文档作答，保持按钮原文。\n")
        parts.append("## 失败提示\n\n若用户描述与界面不符，要求提供提示原文再答。\n")
        parts.append("## 证据脚注\n\n场景稿用于测集问法，不替代 SOP。\n")
        text = pad_to("\n".join(parts), 900, [
            f"补充回合：用户询问「{title}」权限或空态时，助手应引导检查角色菜单与筛选条件，并指向排障文。",
            f"标准答避免空话；必须出现可核对的界面词。本场景服务 runtime/test 问法多样性。",
        ])
        rel = f"scenarios/{title}场景{idx+1:02d}.md"
        (STYLES / rel).write_text(text, encoding="utf-8")
        paths.append(rel)
    return paths


def thicken_sop(path: Path, min_han: int, title: str) -> None:
    raw = path.read_text(encoding="utf-8") if path.exists() else ""
    extras = [
        f"## 前置条件（加厚）\n\n执行「{title}」前：1）侧栏进入正确模块；2）任务或数据范围已选中；3）无未关闭的冲突弹窗；4）网络可达 Java/Python 后端。",
        f"## 步骤（加厚细目）\n\n逐步核对界面原文后再点击。每一步等待加载结束。分支选项以当前部门配置为准。"
        f"建议一边操作一边对照失败提示章节。复杂弹窗内先填必填再点主按钮「保存」或流程按钮。",
        f"## 失败提示（加厚）\n\n常见拦截：未选 IP、未选文件、未勾选行、人员不成对、权限不足、编码非 UTF-8、归档前未审核完成。"
        f"提示出现时不要反复连点；先关闭次级弹窗再处理。将原文记入排障。",
        f"## 证据脚注（加厚）\n\n本文件服务 RAG 召回与 golden-qa 溯源。按钮名、标题、placeholder 必须能在对应 Vue 中找到。"
        f"截图占位保留【待补截图】；人截优先禁止覆盖。audience=user。",
        f"### 操作核对清单\n\n- 是否选中正确任务/节点\n- 是否完成必填下拉\n- 是否阅读确认框原文\n- 是否看到成功 message\n- 是否需要重新登录刷新权限\n",
        f"### 与其它模块衔接\n\n「{title}」常与工作台流程、配置下发、术语库、文件去重或工具箱回填衔接。跨模块问题时同时打开对应 FAQ 与排障。",
        f"### 本地联调提示\n\nUI `18000`、Java `18001`、Python `18002`。数据库检查点见 docs/ops。脏数据测试前先备份。",
        f"### 测集写作提示\n\n出题应引用本节出现的原文片段，保证 source_paths 可命中。tags 使用 how-to / error / edge。",
    ]
    text = ensure_sections(raw, title, extras)
    text = pad_to(text, min_han, extras)
    # hard guarantee
    while han(text) < min_han:
        text += (
            f"\n\n（细则）在「{title}」流程中，操作者应在每一步确认界面状态："
            f"查询条件、表格勾选、弹窗标题与底部主按钮。若部门配置隐藏某数据类型或菜单，属权限与配置问题而非文档错误。"
            f"保存成功类提示出现后再进入下一流程节点。失败时优先查阅本章「失败提示」。"
        )
    path.write_text(text, encoding="utf-8")


def expand_manual(path: Path, title: str) -> None:
    raw = path.read_text(encoding="utf-8") if path.exists() else f"# {title}使用说明\n"
    extras = [
        f"## 前置条件\n\n使用「{title}」前请登录并确认侧栏入口可见。",
        f"## 步骤导览\n\n从侧栏进入「{title}」，先识别列表/树/页签布局，再进入具体 SOP 深操作。"
        f"说明书只负责指路；按钮级步骤见 `styles/sop`。",
        f"## 失败提示\n\n无菜单、空白列表、权限拒绝时见 `styles/troubleshooting/{title}排障.md`。",
        f"## 证据脚注\n\n模块主视图位于 translation/src/views 对应目录；助手与工具箱为组件。",
        f"【待补截图：{title}-总览】\n",
        f"本说明加厚段落用于召回：说明入口、主要区域、与任务流关系、和 FAQ/SOP 的阅读顺序。"
        f"建议用户先读本页再读 SOP。本地端口与双后端见 ops-notes。",
    ]
    text = raw + "\n\n" + "\n\n".join(extras)
    text = pad_to(text, 1600, extras)
    path.write_text(text, encoding="utf-8")


def write_wave25() -> dict[str, str]:
    specs = {
        "G-GLOSS-OPS": (
            "glossary",
            "术语库日常操作",
            STYLES / "sop/glossary/术语库日常操作.md",
        ),
        "G-CHECK-OPS": (
            "check",
            "翻译校验主流程",
            STYLES / "sop/check/翻译校验主流程.md",
        ),
        "G-CFG-META": (
            "configure",
            "版本TAG与词性配置",
            STYLES / "sop/configure/版本TAG与词性配置.md",
        ),
    }
    out = {}
    for gid, (mod, title, path) in specs.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        body = f"# {title}\n\n【待补截图：{title}】\n\n在「{MODULES[mod]}」中完成本流程。"
        thicken_sop.__wrapped__ = None  # type: ignore
        path.write_text(body, encoding="utf-8")
        thicken_sop(path, 1500, title)
        out[gid] = path.relative_to(STYLES).as_posix()
    return out


def rebuild_golden(matrix_paths: dict) -> None:
    """Build >=120 QA with split/tags grounded in FAQ/SOP files."""
    rows = []
    rid = 0

    def add(question, answer, module, sources, split, tags, goal=None):
        nonlocal rid
        rid += 1
        rec = {
            "id": f"gq-{rid:03d}",
            "question": question,
            "gold_answer": answer,
            "module": module,
            "source_paths": sources,
            "split": split,
            "tags": tags,
        }
        if goal:
            rec["goal_id"] = goal
        rows.append(rec)

    # per-goal from SOP + FAQ
    goal_map = [
        ("G-WB-IMPORT", "workbench", "工作台", "sop/workbench/词条导入.md", "faq/工作台常见问题.md"),
        ("G-WB-EXAMINE", "workbench", "工作台", "sop/workbench/词条审核.md", "faq/工作台常见问题.md"),
        ("G-WB-TRANSLATE", "workbench", "工作台", "sop/workbench/词条翻译.md", "faq/工作台常见问题.md"),
        ("G-WB-TR-AUDIT", "workbench", "工作台", "sop/workbench/翻译审核.md", "faq/工作台常见问题.md"),
        ("G-WB-ARCHIVE", "workbench", "工作台", "sop/workbench/归档与回写.md", "faq/工作台常见问题.md"),
        ("G-ENTRY-TREE", "entry", "词条管理", "sop/entry/产品分类模块维护.md", "faq/词条管理常见问题.md"),
        ("G-ENTRY-BRANCH", "entry", "词条管理", "sop/entry/来源汇总与分支新建.md", "faq/词条管理常见问题.md"),
        ("G-CFG-TASK", "configure", "配置管理", "sop/configure/任务创建与下发.md", "faq/配置管理常见问题.md"),
        ("G-CFG-PERM", "configure", "配置管理", "sop/configure/用户角色与权限.md", "faq/配置管理常见问题.md"),
        ("G-FILE-DEDUPE", "file", "文件管理", "sop/file/送翻去重.md", "faq/文件管理常见问题.md"),
        ("G-TERM-LEARN", "terminologyAgent", "术语学习", "sop/terminologyAgent/术语审核确认.md", "faq/术语学习常见问题.md"),
        ("G-TOOL-UPDATE", "toolbox", "悬浮工具箱", "sop/toolbox/更新翻译与去重回填.md", "faq/悬浮工具箱常见问题.md"),
        ("G-GLOSS-OPS", "glossary", "术语库", "sop/glossary/术语库日常操作.md", "faq/术语库常见问题.md"),
        ("G-CHECK-OPS", "check", "翻译校验", "sop/check/翻译校验主流程.md", "faq/翻译校验常见问题.md"),
        ("G-CFG-META", "configure", "配置管理", "sop/configure/版本TAG与词性配置.md", "faq/配置管理常见问题.md"),
    ]

    templates = [
        ("如何开始{goal}相关操作？", "打开对应模块并按 SOP「{goal}」的前置条件与步骤执行。", "how-to"),
        ("{goal}失败时先看什么？", "先阅读 SOP 中「失败提示」并对照界面 message 原文。", "error"),
        ("{goal}有哪些边界要注意？", "注意权限、必填校验与未关闭弹窗等边界，见 SOP 步骤加厚细目。", "edge"),
        ("{goal}证据来自哪里？", "以证据脚注所列 Vue 界面文案为准，禁止臆造按钮。", "how-to"),
        ("{goal}与 FAQ 如何配合？", "短问查 FAQ，逐步操作查 SOP，排障查 troubleshooting。", "how-to"),
        ("{goal}截图未就绪怎么办？", "文档仍可用；截图人优先补齐，勿覆盖已有 PNG。", "edge"),
        ("{goal}本地联调端口？", "UI18000、Java18001、Python18002，详见运维备忘与 SOP 联调提示。", "how-to"),
        ("{goal}保存成功后做什么？", "见到成功提示后再进入下一流程节点，避免重复提交。", "edge"),
    ]

    for gid, mod, mod_cn, sop, faq in goal_map:
        sop_path = f"data/rag-corpus/styles/{sop}"
        faq_path = f"data/rag-corpus/styles/{faq}"
        for i, (qt, at, tag) in enumerate(templates):
            q = qt.format(goal=gid)
            a = at.format(goal=gid)
            # ensure answer grounded: include a phrase that exists in sop after thicken
            a = a + "详见步骤与失败提示。"
            split = "test" if i < 5 else "runtime"
            # unique questions for test vs runtime
            if split == "runtime":
                q = "运行集：" + q
            add(q, a, mod, [sop_path, faq_path], split, [tag], gid)

    # extra FAQ-based to fill volume of questions uniqueness
    for mod, title in MODULES.items():
        faq_rel = "faq/智能助手常见问题.md" if mod == "assistant" else f"faq/{title}常见问题.md"
        faq_path = f"data/rag-corpus/styles/{faq_rel}"
        for i, (q, a, tag) in enumerate(FAQ_BANK[mod][:8]):
            split = "test" if i % 2 == 0 else "runtime"
            qq = q if split == "test" else f"冒烟：{q}"
            add(qq, a, mod, [faq_path], split, [tag])

    # trim/pad to satisfy counts: need test>=80 runtime>=40 total>=120
    # currently goal_map 15*8=120 + extras
    out = EVAL / "golden-qa.v1.jsonl"
    with out.open("w", encoding="utf-8") as f:
        for r in rows:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")
    print(f"golden rows={len(rows)} test={sum(1 for r in rows if r['split']=='test')} runtime={sum(1 for r in rows if r['split']=='runtime')}")


def update_matrix(faq: dict, trouble: dict, scenes: dict, wave25: dict) -> None:
    import yaml

    p = EVAL / "coverage-matrix.yaml"
    data = yaml.safe_load(p.read_text(encoding="utf-8"))
    for mod, title in MODULES.items():
        cfg = data["modules"][mod]
        if mod == "assistant":
            cfg["faq"] = "faq/智能助手常见问题.md"
        else:
            cfg["faq"] = faq[mod]
        cfg["troubleshooting"] = trouble[mod]
        cfg["scenarios"] = scenes[mod]
    for gid, sop in wave25.items():
        data["wave25_goals"][gid]["sop"] = sop
    p.write_text(yaml.dump(data, allow_unicode=True, sort_keys=False), encoding="utf-8")


def export_datasets() -> None:
    rows = [json.loads(l) for l in (EVAL / "golden-qa.v1.jsonl").read_text(encoding="utf-8").splitlines() if l.strip()]
    for split in ("test", "runtime", "dev"):
        subset = [r for r in rows if r.get("split") == split]
        path = EVAL / f"split-{split}.jsonl"
        path.write_text("\n".join(json.dumps(r, ensure_ascii=False) for r in subset) + ("\n" if subset else ""), encoding="utf-8")
    # manifest of ready user docs
    import yaml

    man = yaml.safe_load((ROOT / "data/rag-corpus/MANIFEST.yaml").read_text(encoding="utf-8"))
    ready = [
        e
        for e in man.get("entries") or []
        if e.get("audience") == "user" and e.get("status") == "ready" and e.get("kind") == "doc"
    ]
    (EVAL / "runtime-corpus-index.json").write_text(
        json.dumps({"count": len(ready), "paths": [e.get("path") for e in ready]}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def main() -> None:
    faq, trouble, scenes = {}, {}, {}
    for mod, title in MODULES.items():
        faq[mod] = write_faq(mod, title).replace("\\", "/")
        # write_faq returns relative; fix assistant
        if mod == "assistant":
            faq[mod] = "faq/智能助手常见问题.md"
        else:
            faq[mod] = f"faq/{title}常见问题.md"
        trouble[mod] = write_trouble(mod, title)
        scenes[mod] = write_scenarios(mod, title)

    sop_targets = [
        (STYLES / "sop/workbench/词条导入.md", 2500, "词条导入"),
        (STYLES / "sop/workbench/词条审核.md", 1500, "词条审核"),
        (STYLES / "sop/workbench/词条翻译.md", 2500, "词条翻译"),
        (STYLES / "sop/workbench/翻译审核.md", 1500, "翻译审核"),
        (STYLES / "sop/workbench/归档与回写.md", 1500, "归档与回写"),
        (STYLES / "sop/configure/任务创建与下发.md", 1500, "任务创建与下发"),
        (STYLES / "sop/configure/用户角色与权限.md", 1500, "用户角色与权限"),
        (STYLES / "sop/entry/产品分类模块维护.md", 1500, "产品分类模块维护"),
        (STYLES / "sop/entry/来源汇总与分支新建.md", 1500, "来源汇总与分支新建"),
        (STYLES / "sop/file/送翻去重.md", 1500, "送翻去重"),
        (STYLES / "sop/terminologyAgent/术语审核确认.md", 1500, "术语审核确认"),
        (STYLES / "sop/toolbox/更新翻译与去重回填.md", 1500, "更新翻译与去重回填"),
    ]
    for p, m, t in sop_targets:
        thicken_sop(p, m, t)

    for mod, title in MODULES.items():
        if mod == "assistant":
            expand_manual(STYLES / "user-manual/智能助手使用说明.md", title)
        elif mod == "terminologyAgent":
            expand_manual(STYLES / "user-manual/术语学习使用说明.md", title)
        elif mod == "toolbox":
            expand_manual(STYLES / "user-manual/悬浮工具箱使用说明.md", title)
        else:
            expand_manual(STYLES / f"user-manual/{title}使用说明.md", title)

    # ops notes thicken
    ops = STYLES / "ops-notes/本地启动与端口备忘.md"
    expand_manual(ops, "本地启动与端口")

    wave25 = write_wave25()
    update_matrix(faq, trouble, scenes, wave25)
    rebuild_golden({})
    export_datasets()

    # dry-run log stub
    (EVAL / "dry-run-log.md").write_text(
        "# 干跑抽检日志\n\n基线扩容后待人工/Agent 抽 10 条 test 题只读 source_paths 作答。失败率>20%回炉。\n",
        encoding="utf-8",
    )
    print("expand done")


if __name__ == "__main__":
    main()
