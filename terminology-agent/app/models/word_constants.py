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
  term_word.status = approved | pending | deprecated
        │  仅 approved 进入运行时
        ├─► WordRepository.find_by_word / list_distinct_words（默认过滤 approved）
        ├─► trie_cache.load_trie_for_lang（Trie 只含 approved 词）
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
| ``WORD_STATUS_APPROVED``      | ETL（translate_state='3'）       | Grep 检索、Trie 建库、调试 lookup API    |
| ``WORD_STATUS_PENDING``       | ETL（未审定译文）                | 暂不参与 Grep；治理后可升为 approved     |
| ``WORD_STATUS_DEPRECATED``    | （预留）人工下线 / 数据治理      | 历史留档，检索与 Trie 均排除             |
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

# ── TermWord.status ──────────────────────────────────────────────────────────
# 消歧键 (word, comment, target_lang) 下每一行元词的「是否可检索」状态。
# 运行时 Grep 默认只认 approved；改 status 后需重建 Trie 或等 trie_cache TTL 过期。

WORD_STATUS_APPROVED: Final[str] = "approved"
"""已审定、可参与 Grep 检索。

- **何时写入**：``build_word_index`` ETL 扫描 ``t_translate`` 时，
  ``translate_state == '3'``（业务上「已审定」）→ ``word_status_from_translate_state`` 映射为此值。
- **谁在读**：
  - ``WordRepository.find_by_word`` / ``list_distinct_words`` 默认 ``status=approved``
  - ``load_trie_for_lang`` 只为 approved 词建 Trie
  - ``POST /agent/debug/word-lookup`` 调试端点
- **业务含义**：Agent 可以把该行的 ``translate`` 当作 Grep 线的确定性候选。
"""

WORD_STATUS_PENDING: Final[str] = "pending"
"""未审定，建库时写入但**不参与**在线 Grep。

- **何时写入**：ETL 时 ``translate_state`` 不是 ``'3'`` 的译文行；也是 ``TermWord`` ORM 列默认值。
- **谁在读**：当前运行时路径**不查** pending；仅供数据治理、将来「审定后升 approved」流程。
- **与 audit 的区别**：这是词表行状态，不是 ``term_agent_audit.review_status='pending'``。
"""

WORD_STATUS_DEPRECATED: Final[str] = "deprecated"
"""已下线，保留行供溯源，**永不**进入 Grep / Trie。

- **何时写入**：尚未有自动 ETL 路径；预留给人工治理、批量下线错误词表行。
- **谁在读**：暂无；仓储与 Trie 与 pending 一样默认排除。
- **使用建议**：下线时不要 ``DELETE``，改 status 并 ``clear_trie_cache()`` 或等待 TTL。
"""

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
