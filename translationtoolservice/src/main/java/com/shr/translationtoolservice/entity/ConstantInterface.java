package com.shr.translationtoolservice.entity;

/**
 * @author ：210093
 * @date ：Created in 2023/6/19 16:02
 * @description：字符串常量
 */
public class ConstantInterface {
    public static final String TASK_TYPE = "task_type";
    public static final String DEVICE_LEVEL = "device_level";
    public static final String INTERVAL_TYPE = "interval_type";
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT1 = "yyyy-MM-dd";
    public static final String DATE_FORMAT2 = "yyyy年MM月dd日";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String YEAR_MONTH_DAY_FORMAT = "yyyy-MM-dd";
    public static final String EXPORT_IMAGE_BUCKET = "exportimage";
    public static final String EXPORT_BUCKET = "export/";
    public static final String UNDERLINE_SEPARATOR = "_";
    public static final String COMMA_SEPARATOR = ",";
    public static final String FULL_SEPARATOR = "。";
    public static final String SPACE_SEPARATOR = " ";
    public static final String COLON = ":";
    public static final char SPACE_SEPARATOR1 = ' ';
    public static final String LINE_SEPARATOR = "-";
    public static final String SLASH_SEPARATOR = "/";
    public static final String POINT_SEPARATOR = ".";
    public static final char SLASH_SEPARATOR1 = '/';
    public static final int PRIORITY_MIN = 1;
    public static final int PRIORITY_MAX = 5;
    public static final int DB_SUCCESS_RESULT = 1;
    public static final String XLS_SUFFIX = ".xls";
    public static final String TASK_ENABLE = "0";
    public static final String IS_DEFAULT = "1";
    public static final String OK_STR = "OK";
    public static final String FAIL_STR = "Fail";
    public static final String DEVICE_POS = "device_pos\":\"";
    public static final String ONE_HUNDRED = "100";
    public static final String OVERVIEW_STATE = "state";
    public static final String OVERVIEW_TYPE = "type";
    public static final Short VIDEO_NUM = 1;
    public static final String OR_STR = "或";
    public static final Short ROBOT_NUM = 2;
    public static final String ZIP_SUFFIX = ".zip";
    public static final String AUDIT_PATH = "/**";
    public static final String UNTRANSLATED = "未翻译";
    public static final String TRANSLATED = "已翻译";
    public static final String TRANSLATING = "正在翻译";

    public static final String PROJECT_TABLE = "project";
    public static final String PRODUCT_TABLE = "product";
    public static final String COMMON_TABLE = "common";

    public static final String TIME_ZERO = " 00:00:00";
    public static final String PROJECT_TABLE_Name = "t_entry_project";
    public static final String PRODUCT_TABLE_Name = "t_entry_product";
    public static final String COMMON_TABLE_Name = "t_entry_common";

    public static final String BAIDU_TRANSLATE_APPID = "20230705001734655";
    public static final String BAIDU_TRANSLATE_KEY = "Msyn6CIJy97uY1MXLW0c";

    public static final String FRENCH = "fra";
    public static final String ENGLISH = "en";
    public static final String RUSSIAN = "ru";
    public static final String SPANISH = "spa";
    public static final String AUTO = "auto";

    public static final String UNION  =" union all";
    public static final String SEARCH = " select  <include refid=\"Base_Column_List\"/>,#{entryEntity.tableName}  as tableName  from    ";

    public static final String CONDITION = "            \"        where 1=1\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.abbr != null  and entryEntity.abbr != ''\\\">\\n\" +\n" +
            "            \"            and\\n\" +\n" +
            "            \"            abbr  like CONCAT('%','${entryEntity.abbr}','%' )\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.entry != null  and entryEntity.entry != ''\\\">\\n\" +\n" +
            "            \"            and   entry   like CONCAT('%','${entryEntity.entry}','%' )\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.entryLength != null and entryEntity.entryLength != 0 \\\">\\n\" +\n" +
            "            \"            and entry_length = #{entryEntity.entryLength,jdbcType=INTEGER}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.chineseInterpretation != null and entryEntity.chineseInterpretation != ''\\\">\\n\" +\n" +
            "            \"            and   chinese_interpretation = #{entryEntity.chineseInterpretation,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.englishInterpretation != null and entryEntity.englishInterpretation != ''\\\">\\n\" +\n" +
            "            \"            and englishinterpretation = #{entryEntity.englishInterpretation,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.entrySource != null and entryEntity.entrySource != ''\\\">\\n\" +\n" +
            "            \"            and entry_source = #{entryEntity.entrySource,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.entryState != null  and entryEntity.entryState != 0\\\">\\n\" +\n" +
            "            \"            and  entry_state = #{entryEntity.entryState,jdbcType=INTEGER}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.creator != null and entryEntity.creator != ''\\\">\\n\" +\n" +
            "            \"            and  creator like CONCAT('%','${entryEntity.creator}','%' )\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.createTime != null  and entryEntity.createTime != ''\\\">\\n\" +\n" +
            "            \"            and  create_time &gt;=    #{entryEntity.createTime,jdbcType=DATETIMEOFFSET}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.createEndRTime != null   and entryEntity.createEndRTime != ''\\\">\\n\" +\n" +
            "            \"            and create_time &lt;=    #{entryEntity.createEndRTime,jdbcType=DATETIMEOFFSET}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.version != null and entryEntity.version != ''\\\">\\n\" +\n" +
            "            \"            and version = #{entryEntity.version,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.isLatestVersion != null  and entryEntity.isLatestVersion != 0\\\">\\n\" +\n" +
            "            \"            and is_latest_version = #{entryEntity.isLatestVersion,jdbcType=INTEGER}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.entryLabel != null and entryEntity.entryLabel != ''\\\">\\n\" +\n" +
            "            \"            and entry_label = #{entryEntity.entryLabel,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.partOfSpeech != null and entryEntity.partOfSpeech != ''\\\">\\n\" +\n" +
            "            \"            and part_of_speech like CONCAT('%','${entryEntity.partOfSpeech}','%' )\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.classifyId != null and entryEntity.classifyId != ''\\\">\\n\" +\n" +
            "            \"            and classify_id = #{entryEntity.classifyId,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.repeatEntryId != null and entryEntity.repeatEntryId != ''\\\">\\n\" +\n" +
            "            \"            and repeatEntryId = #{entryEntity.repeatEntryId,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.english != null and entryEntity.english != ''\\\">\\n\" +\n" +
            "            \"            and english =#{entryEntity.english,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.englishLength != null and entryEntity.englishLength != 0\\\">\\n\" +\n" +
            "            \"            and english_length = #{entryEntity.englishLength,jdbcType=INTEGER}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.englishTranslateState != null and entryEntity.englishTranslateState != ''\\\">\\n\" +\n" +
            "            \"            and english_translate_state = #{entryEntity.englishTranslateState,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.englishDisable != null and entryEntity.englishDisable != ''\\\">\\n\" +\n" +
            "            \"            and  english_disable = #{entryEntity.englishDisable,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.englishDisableLength != null   and entryEntity.englishDisableLength != 0\\\">\\n\" +\n" +
            "            \"            and english_disable_length = #{entryEntity.englishDisableLength,jdbcType=INTEGER}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.russian != null and entryEntity.russian != ''\\\">\\n\" +\n" +
            "            \"            and russian = #{entryEntity.russian,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.russianLength != null  and entryEntity.russianLength != 0\\\">\\n\" +\n" +
            "            \"            and russian_length = #{entryEntity.russianLength,jdbcType=INTEGER}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.russianTranslateState != null and entryEntity.russianTranslateState != ''\\\">\\n\" +\n" +
            "            \"            and russian_translate_state = #{entryEntity.russianTranslateState,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.spanish != null and entryEntity.spanish != ''\\\">\\n\" +\n" +
            "            \"            and  spanish = #{entryEntity.spanish,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.spanishLength != null and entryEntity.spanishLength != 0\\\">\\n\" +\n" +
            "            \"            and spanish_length = #{entryEntity.spanishLength,jdbcType=INTEGER}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.spanishTranslateState != null and entryEntity.spanishTranslateState != ''\\\">\\n\" +\n" +
            "            \"            and  spanish_translate_state = #{entryEntity.spanishTranslateState,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.french != null and entryEntity.french != ''\\\">\\n\" +\n" +
            "            \"            and french = #{entryEntity.french,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.frenchLength != null  and entryEntity.frenchLength != 0\\\">\\n\" +\n" +
            "            \"            and french_length = #{entryEntity.frenchLength,jdbcType=INTEGER}\\n\" +\n" +
            "            \"        </if>\\n\" +\n" +
            "            \"        <if test=\\\"entryEntity.frenchTranslateState != null and entryEntity.frenchTranslateState != ''\\\">\\n\" +\n" +
            "            \"            and french_translate_state = #{entryEntity.frenchTranslateState,jdbcType=VARCHAR}\\n\" +\n" +
            "            \"        </if>\\n\"";
}
