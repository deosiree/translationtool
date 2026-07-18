"""term_word 域枚举常量 — ``TermWord.status`` 与 ``TermWordConflict`` 工单字段。

本文件是 **Grep 线元词表**（``term_word`` / ``term_word_conflict``）在 ORM、仓储、ETL
之间共享的**字符串枚举唯一源**，避免魔法字符串散落各处。

与 ``term_agent_audit.review_status`` 无关
----------------------------------------
``term_agent_audit`` 管的是「预翻译结果要不要人工审」；
本文件的 ``WORD_STATUS_*`` 管的是「某条 term_word 行能否参与 Grep 检索」——两套生命周期。

数据流简图
----------

::

  t_translate + t_entry_info
        │  scripts/build_word_index（ETL）
        ▼
  term_word.status = "0"|"1"|"2"|"3"（与 t_translate.translate_state 对齐）
        │  仅 "3"（已审核）进入运行时
        ├─► WordRepository.find_by_word / list_distinct_words（默认过滤 "3"）
        ├─► trie_cache.load_trie_for_lang（Trie 只含已审核词）
        └─► grep_retrieve（在线 Grep 检索）

  同 (word, comment, target_lang) 多译法
        │  etl/conflict.detect_translate_mismatches
        ▼
  term_word_conflict.conflict_type / resolution

常量速查
--------

+-------------------------------+----------------------------------+------------------------------------------+
| 常量                          | 写入方                           | 读取方 / 场景                            |
+===============================+==================================+==========================================+
| ``WORD_STATUS_UNTRANSLATED``  | ETL / 人工 CRUD                  | 列表筛选；不参与 Grep                    |
| ``WORD_STATUS_PENDING``       | ETL / 人工 CRUD（默认新建）      | 列表筛选；不参与 Grep                    |
| ``WORD_STATUS_REJECTED``      | ETL / 人工 CRUD                  | 列表筛选；不参与 Grep                    |
| ``WORD_STATUS_APPROVED``      | ETL（translate_state='3'）/ CRUD | Grep 检索、Trie 建库、调试 lookup API    |
| ``CONFLICT_TYPE_TRANSLATE_MISMATCH`` | ETL 矛盾检测              | 矛盾工单列表展示、将来按类型分流         |
| ``CONFLICT_RESOLUTION_OPEN``  | ETL 新建矛盾工单                 | ``list_open_conflicts`` 待处理队列       |
+-------------------------------+----------------------------------+------------------------------------------+

相关代码
--------
- ORM 字段：``app/models/word.py`` → ``TermWord.status``、``TermWordConflict``
- 仓储过滤：``app/repository/word_repo.py``
- ETL 写入：``app/shared/term_word/etl/join_entry_info.py``、``etl/conflict.py``
- 在线消费：``app/repository/trie_cache.py``、``grep_retrieve``
"""

from typing import Final

# ── TermWord.status（与 translate_state / 前端 TransStateSelect 对齐）────────
# 运行时 Grep 默认只认 "3"；改 status 后需 clear_trie_cache() 或等 TTL。

WORD_STATUS_UNTRANSLATED: Final[str] = "0"
"""未翻译 — 不参与 Grep / Trie。"""

WORD_STATUS_PENDING: Final[str] = "1"
"""待审核 — 不参与 Grep / Trie；新建词片默认值。"""

WORD_STATUS_REJECTED: Final[str] = "2"
"""审核不通过 — 不参与 Grep / Trie。"""

WORD_STATUS_APPROVED: Final[str] = "3"
"""已审核、可参与 Grep 检索。

- **何时写入**：ETL 透传 ``translate_state``；或术语字典人工审定。
- **谁在读**：
  - ``WordRepository.find_by_word`` / ``list_distinct_words`` 默认 ``status="3"``
  - ``load_trie_for_lang`` 只为已审核词建 Trie
  - ``GET /agent/word/{word}`` 调试端点
- **业务含义**：Agent 可以把该行的 ``translate`` 当作 Grep 线的确定性候选。
"""

WORD_STATUS_VALUES: Final[frozenset[str]] = frozenset(
    {
        WORD_STATUS_UNTRANSLATED,
        WORD_STATUS_PENDING,
        WORD_STATUS_REJECTED,
        WORD_STATUS_APPROVED,
    }
)

# ── TermWordConflict 工单字段 ────────────────────────────────────────────────
# 当同一消歧键下存在多种 ``translate`` 时，ETL 写入矛盾工单，供治理页处理。

CONFLICT_TYPE_TRANSLATE_MISMATCH: Final[str] = "translate_mismatch"
"""矛盾类型：同一 ``(word, comment, target_lang)`` 对应多种译法。

- **何时写入**：``detect_translate_mismatches`` 发现组内 ``distinct_translates > 1`` 时，
  ``ConflictGroup.to_conflict_payload()`` 写入 ``term_word_conflict.conflict_type``。
- **谁在读**：目前仅落库；将来可按类型展示图标或走不同化解流程。
- **不含的场景**：部门不同但译法相同、comment 不同视为不同消歧键，不算此类矛盾。
"""

CONFLICT_RESOLUTION_OPEN: Final[str] = "open"
"""矛盾工单处理状态：待人工处理。

- **何时写入**：ETL 新建矛盾记录时默认 ``resolution='open'``（与 ORM 列默认一致）。
- **谁在读**：``WordRepository.list_open_conflicts`` 只拉取 ``resolution == open`` 的工单。
- **闭环（预留）**：治理确认后应改为 ``resolved`` / ``ignored`` 等终态（常量尚未定义）。
"""
