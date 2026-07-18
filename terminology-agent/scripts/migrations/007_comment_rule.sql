-- comment_rule：Comment 场景规则（低频配置）
-- 种子来自 comment对应场景及规则.xlsx

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS comment_rule (
  id VARCHAR(64) NOT NULL PRIMARY KEY,
  comment_key VARCHAR(128) NOT NULL COMMENT 'comment 键，如 tabBarTitle',
  entry_source VARCHAR(255) NULL COMMENT '词条来源',
  scene TEXT NULL COMMENT '场景',
  rule_text TEXT NULL COMMENT '规则',
  prefer_abbr TINYINT(1) NOT NULL DEFAULT 0 COMMENT '优先缩写',
  case_type VARCHAR(32) NULL COMMENT 'SentenceCase|TitleCase',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_comment_key (comment_key),
  KEY idx_prefer_abbr (prefer_abbr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Comment 场景规则';

-- 幂等种子：仅当表为空时插入
INSERT INTO comment_rule (id, comment_key, entry_source, scene, rule_text, prefer_abbr, case_type)
SELECT * FROM (
  SELECT '1ba73d99fca14942' AS id, 'tabBarTitle' AS comment_key, NULL AS entry_source, '1.侧边栏标题
2.tab标题' AS scene, '- 英文翻译必须使用 Title Case（每个单词首字母都要大写）：
  - 正确示例： "Tab Bar Title Settings"
  - 错误示例1： "Tab bar title settings"（只第一个单词大写，错误）
  - 错误示例2： "tab bar title settings"（全部小写，错误）
- 精简翻译' AS rule_text, 1 AS prefer_abbr, 'TitleCase' AS case_type UNION ALL
  SELECT '65588d9ed4a94777' AS id, 'toolName' AS comment_key, NULL AS entry_source, '1.⼯具名称标题
2.dialog的标题' AS scene, '- 英文翻译必须使用 Title Case（每个单词首字母都要大写）：
  - 正确示例： "Tab Bar Title Settings"
  - 错误示例1： "Tab bar title settings"（只第一个单词大写，错误）
  - 错误示例2： "tab bar title settings"（全部小写，错误）
- 精简翻译' AS rule_text, 1 AS prefer_abbr, 'TitleCase' AS case_type UNION ALL
  SELECT 'a6db37bb215d49a7' AS id, 'menuName' AS comment_key, NULL AS entry_source, '1.⼯具菜单栏菜单按钮' AS scene, '- 英文翻译必须使用 Sentence case（只有第一个单词首字母大写，其他单词全部小写）：
  - 正确示例： "Tab bar title settings"
  - 错误示例1： "Tab Bar Title Settings"（每个单词都大写，错误）
  - 错误示例2： "tab bar title settings"（全部小写，错误）
- 字符数不要超过“词条”对应的值的字符数的2倍
  - 可以使用通用的缩写，或者电力行业内的缩写，减少字符数
  - 字符数最多不要超过12个' AS rule_text, 1 AS prefer_abbr, 'SentenceCase' AS case_type UNION ALL
  SELECT '3a11020619d84b94' AS id, 'buttonName' AS comment_key, NULL AS entry_source, '1.button 按钮
2.词条中包含“是”/“否”' AS scene, '- 英文翻译必须使用 Sentence case（只有第一个单词首字母大写，其他单词全部小写）：
  - 正确示例： "Tab bar title settings"
  - 错误示例1： "Tab Bar Title Settings"（每个单词都大写，错误）
  - 错误示例2： "tab bar title settings"（全部小写，错误）
- 若词条为“是”/“否”，应翻译为Yes/No
- 字符数不要超过“词条”对应的值的字符数的2倍
  - 可以使用通用的缩写，或者电力行业内的缩写，减少字符数' AS rule_text, 1 AS prefer_abbr, 'SentenceCase' AS case_type UNION ALL
  SELECT '6d44e322d1c64d23' AS id, 'subTitle' AS comment_key, NULL AS entry_source, '1.界⾯功能分区的⼩标题
2.table表头
3.中⽂词条最后带上冒号的，也按 subTitle 的要求翻译' AS scene, '- 英文翻译必须使用 Sentence case（只有第一个单词首字母大写，其他单词全部小写）：
  - 正确示例： "Tab bar title settings"
  - 错误示例1： "Tab Bar Title Settings"（每个单词都大写，错误）
  - 错误示例2： "tab bar title settings"（全部小写，错误）
- 【“是否” → Yes/No（界面文案）】
  - 适用场景：对话框按钮、开关选项、用户可见的界面文本。
  - 英文翻译要求：
    - 使用 Yes / No（首字母大写）。
    - 禁止使用 True / False 作为界面按钮文案。
  - 示例：

    - 正确： 提示框按钮 "是/否" → "Yes" / "No"
    - 错误： 提示框按钮 "是/否" → "True" / "False"（布尔值风格，不适合界面）
- 字符数不要超过“词条”对应的值的字符数的2倍
  - 可以使用通用的缩写，或者电力行业内的缩写，减少字符数' AS rule_text, 1 AS prefer_abbr, 'SentenceCase' AS case_type UNION ALL
  SELECT 'dec0ff6ad27748c6' AS id, 'Time' AS comment_key, NULL AS entry_source, '词条中包含了会产生歧义的词条：“分”' AS scene, '【分钟的“分”】
含义：中文“分”表示时间单位“分钟”（minute）。

英文翻译要求：
- 单位一律翻译为 minute 或缩写 min，如："5 分" → "5 min"。
- 禁止误译为开关“分合”的 open/close 或“除法”的 divide 等含义。

示例：
- 正确： "采样间隔（分）" → "Sampling interval (min)"
- 错误： "采样间隔（分）" → "Sampling interval (open)"（把“分”理解成分合，错误）' AS rule_text, 0 AS prefer_abbr, NULL AS case_type UNION ALL
  SELECT '01bf524e62994a9f' AS id, 'Abbr' AS comment_key, NULL AS entry_source, '词条中包含了会产生歧义的词条：“分”' AS scene, '【开关分合的“分”】
含义：中文“分”表示断开开关（open），与“合”(close) 相对。

英文翻译要求：
- 与“合”成对使用 open / close。
- 禁止翻译为时间单位 minute/min，也禁止翻成 divide 等其他含义。

示例：
- 正确： "开关分合" → "Breaker open/close"
- 错误： "开关分合" → "Breaker minute/close"（把“分”当分钟，错误）' AS rule_text, 0 AS prefer_abbr, NULL AS case_type UNION ALL
  SELECT '9a2c12236d0e410a' AS id, 'Date' AS comment_key, NULL AS entry_source, '词条中包含了会产生歧义的词条：“⽇”' AS scene, '【时间天数的“日”】
含义：这里的“日”表示持续时间的天数（days）。

英文翻译要求：
- 单位统一翻译为 day / days，如："7 日" → "7 days"。
- 禁止误解为星期中的“星期日/Sunday”。

示例：
- 正确： "保存时间（日）" → "Retention time (days)"
- 错误： "保存时间（日）" → "Retention time (Sunday)"（把“日”当星期日，错误）' AS rule_text, 0 AS prefer_abbr, NULL AS case_type UNION ALL
  SELECT '3eec215c4f7247ea' AS id, 'DayOfWeek' AS comment_key, NULL AS entry_source, '词条中包含了会产生歧义的词条：“⽇”' AS scene, '【星期几的“日”】
含义：这里的“日”表示星期中的“星期日”（Sunday）。

英文翻译要求：
- 与星期场景一起使用时，“日”对应 Sunday 或缩写 Sun。
- 禁止翻译成时间单位 day/days。

示例：
- 正确： "周日" → "Sunday"
- 错误： "周日" → "7 days"（把“日”当持续天数，错误）' AS rule_text, 0 AS prefer_abbr, NULL AS case_type UNION ALL
  SELECT '2b84c8d69f0a4653' AS id, 'Number' AS comment_key, NULL AS entry_source, '词条中包含了会产生歧义的词条：⼀/⼆/三/四/五/六' AS scene, '【数值的“一二三四五六”】
含义：这里的“一二三四五六”是纯数字 1–6，不表示星期几。

英文翻译要求：
- 翻译为阿拉伯数字 1/2/3/4/5/6。
- 禁止翻译为 Monday–Saturday 或 Mon–Sat。

示例：
- 正确： "档位一/二/三" → "Level 1/2/3"
- 错误： "档位一/二/三" → "Level Mon/Tue/Wed"（按星期翻译，错误）' AS rule_text, 0 AS prefer_abbr, NULL AS case_type UNION ALL
  SELECT '594d374a35c24a55' AS id, 'DayOfWeek' AS comment_key, NULL AS entry_source, '词条中包含了会产生歧义的词条：⼀/⼆/三/四/五/六' AS scene, '【星期几的“一二三四五六”】
含义：这里的“一二三四五六”表示星期一到星期六。

英文翻译要求：
- 按星期翻译为 Monday–Saturday（或 Mon–Sat），不要用数字 1–6。
- 一致使用同一种风格（全英文或缩写）。

示例：
- 正确： "星期一/二/三" → "Monday/Tuesday/Wednesday"
- 错误： "星期一/二/三" → "Day 1/2/3"（按数字翻译，错误）' AS rule_text, 0 AS prefer_abbr, NULL AS case_type UNION ALL
  SELECT '1ad1bc69317245a6' AS id, 'Boolean' AS comment_key, NULL AS entry_source, '词条中包含了会产生歧义的词条：“是”/“否”' AS scene, '【“是否” → True/False】
适用场景：程序字段、接口参数、配置项等“逻辑型/布尔型”含义。

英文翻译要求：
- 仅使用 True / False（首字母大写，其余小写）。
- 禁止使用 Yes/No 或 0/1 表示。

示例：
- 正确： "是否启用" 字段值 → true/false → "Enable: True / False"
- 错误： "是否启用" → "Enable: Yes / No"（界面用法才用 Yes/No）' AS rule_text, 0 AS prefer_abbr, NULL AS case_type UNION ALL
  SELECT '72055d21f7dd4703' AS id, 'UI' AS comment_key, NULL AS entry_source, '词条中包含了会产生歧义的词条：“是”/“否”' AS scene, '【“是否” → Yes/No（界面文案）】
适用场景：对话框按钮、开关选项、用户可见的界面文本。

英文翻译要求：
- 使用 Yes / No（首字母大写）。
- 禁止使用 True / False 作为界面按钮文案。

示例：
- 正确： 提示框按钮 "是/否" → "Yes" / "No"
- 错误： 提示框按钮 "是/否" → "True" / "False"（布尔值风格，不适合界面）' AS rule_text, 0 AS prefer_abbr, NULL AS case_type UNION ALL
  SELECT '30455f253f124fd5' AS id, 'internal' AS comment_key, NULL AS entry_source, '词条中包含了会产生歧义的词条："状态"' AS scene, '【“状态” → state（内部描述）】
适用场景：内部变量名、内部状态枚举，描述系统内部的状态，例如图元状态、状态前景。

英文翻译要求：
- 优先使用单词 state，如："设备状态" → "deviceState" 或 "device state"。
- 禁止在此类内部字段中使用 status 代替 state。

示例：
- 正确： "图元状态" → "element state"
- 错误： "图元状态" → "element status"（外部观察才用 status）' AS rule_text, 0 AS prefer_abbr, NULL AS case_type UNION ALL
  SELECT '1ae15d84c0bb48f8' AS id, 'external' AS comment_key, NULL AS entry_source, '词条中包含了会产生歧义的词条："状态"' AS scene, '【“状态” → status（外部观察）】
适用场景：从外部看到的状态/结果，如告警状态、运行状态量。

英文翻译要求：
- 使用单词 status，如："告警状态" → "alarm status"。
- 禁止在此类对外结果中使用 state 代替 status。

示例：
- 正确： "遥信状态量" → "status signal"
- 错误： "遥信状态量" → "state signal"（不符合外部结果的常用表达）' AS rule_text, 0 AS prefer_abbr, NULL AS case_type UNION ALL
  SELECT 'dc8c73150f1e438a' AS id, 'Upper' AS comment_key, NULL AS entry_source, NULL AS scene, '【全词大写】
含义：单元格中的英文翻译必须全部使用大写字母（ALL CAPS）。

英文翻译要求：
- 所有字母均为大写，如："ALARM"，"DC VOLTAGE"。
- 禁止出现任何小写字母。

示例：
- 正确： "告警" → "ALARM"
- 错误： "告警" → "Alarm" / "alarm"' AS rule_text, 0 AS prefer_abbr, NULL AS case_type UNION ALL
  SELECT 'ef3d5b45b88747c4' AS id, 'Lower' AS comment_key, NULL AS entry_source, NULL AS scene, '【全词小写】
含义：单元格中的英文翻译必须全部为小写字母。

英文翻译要求：
- 所有字母均为小写，如："offline"，"internal error"。
- 禁止任何大写字母（包括单词首字母）。

示例：
- 正确： "离线" → "offline"
- 错误： "离线" → "Offline" / "OFFLINE"' AS rule_text, 0 AS prefer_abbr, NULL AS case_type
) AS seed
WHERE (SELECT COUNT(*) FROM comment_rule) = 0;

