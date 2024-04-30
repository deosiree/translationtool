package com.shr.translationtoolservice.entity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ：210093
 * @date ：Created in 2023/6/19 16:02
 * @description：字符串常量
 */
@Component
public class ConstantInterface {

    public static final String TOKEN = "token";

    //i18server url
    public static final String LANGUAGE = "language";
    public static final String GET_FILE_LIST = "getFileListByLang";
    public static final String GET_WORDS = "getWords";
    public static final String DICTIONARY = "dictionary";
    public static final String SAVE_WORDS = "saveWords";
    public static final String GET_ALL_NODE = "tdb/getAllNode";
    public static final String GET_APP_BYNODE = "tdb/getAppByNode";
    public static final String GET_DB_BYAPP = "tdb/getdbByApp";
    public static final String GET_TB_APP = "tdb/getTableByApp";
    public static final String GET_FIELD_TABLE = "tdb/getFieldByTable";
    public static final String GET_FIELD_DATA = "tdb/getFieldData";
    public static final String GET_ALIAS = "tdb/getAlias";
    public static final String GET_DBALLENTRYBYNODE= "tdb/getDBALLEntryByNode";
    public static final String GET_DBALLENTRYBYAPP= "tdb/getDBALLEntryByAPP";
    public static final String GET_DBALLENTRYBYDB= "tdb/getDBALLEntryByDB";

    public static final String GET_CONGIF_ENTRY= "config/getEntry";
    public static final String GET_ENUM_ENTRY= "enum/getEntry";
    public static final String Create_DI= "dictionary/createDic";


    public static final String SPRIT = "/";
    public static final String UNDERLINE = "_";
    public static final String ENUM = "ENUM";
    public static final String CONFIG = "CONFIG";
    public static final String DB = "DB";
    public static final String TS = "TS";
    public static final String DI = "DI";
    public static final String DEFAUT = "DEFAUT";
    public static final String EXCEL = "excel";
    public static final String FIELD = "field";
    public static final String ALIAS = "alias";

    public static final int MINUS_ONE = -1;
    public static final int ZERO = 0;
    public static final int DB_SUCCESS_RESULT = 1;

    public static final String DELIVERY_STATE = "0";
    public static final String IMPORT_STATE = "1";
    public static final String ENTRY_AUDIT_STATE = "2";
    public static final String TRANSLATE_STATE = "3";
    public static final String TRANSLATE_AUDIT_STATE = "4";
    public static final String EXPORT_STATE = "5";
    public static final String END_STATE = "6";


    public static final String IS_DEFAULT = "1";
    public static final String OK_STR = "OK";
    public static final String NOCHANGE = "词条未有变化";
    public static final String REPETITION_STR = "版本库存在重复的词条";

    public static final String UNTRANSLATED = "未翻译";
    public static final String TRANSLATED = "已翻译";
    public static final String TRANSLATING = "正在翻译";
    public static final String AUDIT = "已审核";

    public static final String OPERATION_TYPE_INSERT = "新增";
    public static final String OPERATION_TYPE_UPDATE = "编辑";
    public static final String OPERATION_TYPE_DELETE = "删除";
    public static final String OPERATION_TYPE_UPGRATE = "升级";
    public static final String OPERATION_TYPE_AUDIT = "审核";

    public static final int CREATE_STATE = 0;

    public static final String PROJECT_TABLE = "project";
    public static final String PRODUCT_TABLE = "product";
    public static final String COMMON_TABLE = "common";

    public static final String USER = "user";
    public static final String DEPARTMENT = "department";

    public static final String TIME_ZERO = " 00:00:00";
    public static final String PROJECT_TABLE_Name = "t_entry_project";
    public static final String PRODUCT_TABLE_Name = "t_entry_product";
    public static final String COMMON_TABLE_Name = "t_entry_common";

    public static final String BAIDU_TRANSLATE_APPID = "20230705001734655";
    public static final String BAIDU_TRANSLATE_KEY = "Msyn6CIJy97uY1MXLW0c";

    public static final String CHINESE = "中文";
    public static final String FRENCH = "法文";
    public static final String ENGLISH = "英文";
    public static final String RUSSIAN = "俄文";
    public static final String SPANISH = "西文";
    public static final String AUTO = "auto";

    public static final String EN_CHINESE = "chinese";
    public static final String EN_FRENCH = "french";
    public static final String EN_ENGLISH = "english";
    public static final String EN_RUSSIAN = "russian";
    public static final String EN_SPANISH = "spanish";

    public static final String ENTRY_AUDITOR = "词条审核员";
    public static final String TRANSLATE_AUDITOR = "翻译审核员";
    public static final String TRANSLATOR = "翻译员";
    public static final String DEVELOPER = "开发员";
    public static final String ADMIN = "管理员";


    public static final String EN_TRANS = "英文翻译";
    public static final String RU_TRANS = "俄文翻译";
    public static final String SPA_TRANS = "西文翻译";
    public static final String FRA_TRANS = "法文翻译";

    public static final String SYK = "shuyuku";
    public static final String YD = "youdao";
    public static final String BD = "baidu";
    public static final String GG = "google";
    public static final String MD = "module";
    public static final String DEEPL = "deepl";
    public static final String SYNTHESIS = "synthesis";// 综合优先级

    public static HashMap<String, String> translateMachine() {
        HashMap<String, String> translateMachine = new HashMap<>();
        translateMachine.put(SYK, "术语库");
        translateMachine.put(DEEPL, "DeepL翻译");
        translateMachine.put(BD, "百度翻译");
        translateMachine.put(GG, "Google翻译");
        translateMachine.put(YD, "有道翻译");
        translateMachine.put(MD, "模型翻译");
        return translateMachine;
    }

    public static HashMap<String, String> constructUserAndRole() {
        HashMap<String, String> user_role = new HashMap<>();
        user_role.put("词条审核员", "ENTRY_AUDITOR");
        user_role.put("翻译审核员", "TRANSLATE_AUDITOR");
        user_role.put("翻译员", "TRANSLATOR");
        user_role.put("开发员", "DEVELOPER");
        user_role.put("管理员", "ADMIN");
        return user_role;
    }


    public static Map<String, String> LANGUAGE_MAP = new HashMap();


    public static final String UNION = " union all";
    public static final String SEARCH = " select  <include refid=\"Base_Column_List\"/>,#{entryEntity.tableName}  as tableName  from    ";
    private static ConstantInterface _instance = null;

    /**
     * 获取单例
     *
     * @return
     */
    public static ConstantInterface getInstance() {
        if (null == _instance) {
            _instance = new ConstantInterface();
            _instance.init();
        }
        return _instance;
    }

    public static HashMap<String, String> constructEntryName() {
        HashMap<String, String> entryName = new HashMap<>();
        entryName.put("id", "id");
        entryName.put("abbr", "abbr");
        entryName.put("entry", "词条");
        entryName.put("classfy1", "一级分类");
        entryName.put("classfy2", "二级分类");
        entryName.put("chineseInterpretation", "中文释义");
        entryName.put("englishInterpretation", "英文释义");
        entryName.put("chineseTranslateState", "中文翻译状态");
        entryName.put("englishTranslateState", "英文翻译状态");
        entryName.put("entrySource", "词条来源");
        entryName.put("entryState", "词条状态");
        entryName.put("creator", "创建人");
        entryName.put("createTime", "创建时间");
        entryName.put("update", "修改人");
        entryName.put("updateTime", "修改时间");
        entryName.put("entryVersion", "版本");
        entryName.put("isLatestVersion", "是否最新版本");
        entryName.put("entryLabel", "词条标签");
        entryName.put("partOfSpeech", "词性备注");
        entryName.put("classifyId", "词条所属分类");
        entryName.put("repeatEntryId", "重复词条id");
        entryName.put("english", EN_TRANS);
        entryName.put("russian", RU_TRANS);
        entryName.put("russianTranslateState", "俄文翻译状态");
        entryName.put("spanish", SPA_TRANS);
        entryName.put("spanishTranslateState", "西文翻译状态");
        entryName.put("french", FRA_TRANS);
        entryName.put("frenchTranslateState", "法文翻译状态");
        entryName.put("environmentRemark", "环境备注");
        entryName.put("remark", "备注");
        entryName.put("tag", "tag");
        entryName.put("productName", "产品名");
        entryName.put("versionName", "版本名");
        entryName.put("maxLength", "翻译最大长度");
        entryName.put("enCharLength", "英文术语字符数");
        entryName.put("entryLength", "中文术语字符数");
        entryName.put("ruCharLength", "俄文术语字符数");
        entryName.put("spaCharLength", "西文术语字符数");
        entryName.put("fraCharLength", "法文术语字符数");
        return entryName;
    }

    public static HashMap<String, String> EXCEL_LIST_NAME_MAP() {
        HashMap<String, String> entryName = new HashMap<>();
        entryName.put("id", "id");
        entryName.put("abbr", "abbr");
        entryName.put("词条", "entry");
        entryName.put("一级分类", "classfy1");
        entryName.put("二级分类", "classfy2");
        entryName.put("中文释义", "chineseInterpretation");
        entryName.put("英文释义", "englishInterpretation");
        entryName.put("中文翻译状态", "chineseTranslateState");
        entryName.put("英文翻译状态", "englishTranslateState");
        entryName.put("词条来源", "entrySource");
        entryName.put("词条状态", "entryState");
        entryName.put("创建人", "creator");
        entryName.put("创建时间", "createTime");
        entryName.put("修改人", "update");
        entryName.put("修改时间", "updateTime");
        entryName.put("版本", "entryVersion");
        entryName.put("是否最新版本", "isLatestVersion");
        entryName.put("词条标签", "entryLabel");
        entryName.put("词性备注", "partOfSpeech");
        entryName.put("词条所属分类", "classifyId");
        entryName.put("重复词条id", "repeatEntryId");
        entryName.put(EN_TRANS, "english");
        entryName.put(RU_TRANS, "russian");
        entryName.put("俄文翻译状态", "russianTranslateState");
        entryName.put(SPA_TRANS, "spanish");
        entryName.put("西文翻译状态", "spanishTranslateState");
        entryName.put(FRA_TRANS, "french");
        entryName.put("法文翻译状态", "frenchTranslateState");
        entryName.put("环境备注", "environmentRemark");
        entryName.put("备注", "remark");
        entryName.put("产品名", "productName");
        entryName.put("版本名", "versionName");
        entryName.put("翻译最大长度", "maxLength");
        entryName.put("中文术语字符数", "entryLength");
        entryName.put("英文术语字符数", "enCharLength");
        entryName.put("俄文术语字符数", "ruCharLength");
        entryName.put("西文术语字符数", "spaCharLength");
        entryName.put("法文术语字符数", "fraCharLength");
        entryName.put("tag", "tag");
        return entryName;
    }

    /**
     * 初始化语言类
     */
    private void init() {
        LANGUAGE_MAP.put("zh", "Chinese");
        LANGUAGE_MAP.put("spa", "Spanish");
        LANGUAGE_MAP.put("auto", "Automatic");
        LANGUAGE_MAP.put("af", "Afrikaans");
        LANGUAGE_MAP.put("sq", "Albanian");
        LANGUAGE_MAP.put("am", "Amharic");
        LANGUAGE_MAP.put("ar", "Arabic");
        LANGUAGE_MAP.put("hy", "Armenian");
        LANGUAGE_MAP.put("az", "Azerbaijani");
        LANGUAGE_MAP.put("eu", "Basque");
        LANGUAGE_MAP.put("be", "Belarusian");
        LANGUAGE_MAP.put("bn", "Bengali");
        LANGUAGE_MAP.put("bs", "Bosnian");
        LANGUAGE_MAP.put("bg", "Bulgarian");
        LANGUAGE_MAP.put("ca", "Catalan");
        LANGUAGE_MAP.put("ceb", "Cebuano");
        LANGUAGE_MAP.put("ny", "Chichewa");
        LANGUAGE_MAP.put("zh_cn", "Chinese Simplified");
        LANGUAGE_MAP.put("zh_tw", "Chinese Traditional");
        LANGUAGE_MAP.put("co", "Corsican");
        LANGUAGE_MAP.put("hr", "Croatian");
        LANGUAGE_MAP.put("cs", "Czech");
        LANGUAGE_MAP.put("da", "Danish");
        LANGUAGE_MAP.put("nl", "Dutch");
        LANGUAGE_MAP.put("en", "English");
        LANGUAGE_MAP.put("eo", "Esperanto");
        LANGUAGE_MAP.put("et", "Estonian");
        LANGUAGE_MAP.put("tl", "Filipino");
        LANGUAGE_MAP.put("fi", "Finnish");
        LANGUAGE_MAP.put("fra", "French");
        LANGUAGE_MAP.put("fy", "Frisian");
        LANGUAGE_MAP.put("gl", "Galician");
        LANGUAGE_MAP.put("ka", "Georgian");
        LANGUAGE_MAP.put("de", "German");
        LANGUAGE_MAP.put("el", "Greek");
        LANGUAGE_MAP.put("gu", "Gujarati");
        LANGUAGE_MAP.put("ht", "Haitian Creole");
        LANGUAGE_MAP.put("ha", "Hausa");
        LANGUAGE_MAP.put("haw", "Hawaiian");
        LANGUAGE_MAP.put("iw", "Hebrew");
        LANGUAGE_MAP.put("hi", "Hindi");
        LANGUAGE_MAP.put("hmn", "Hmong");
        LANGUAGE_MAP.put("hu", "Hungarian");
        LANGUAGE_MAP.put("is", "Icelandic");
        LANGUAGE_MAP.put("ig", "Igbo");
        LANGUAGE_MAP.put("id", "Indonesian");
        LANGUAGE_MAP.put("ga", "Irish");
        LANGUAGE_MAP.put("it", "Italian");
        LANGUAGE_MAP.put("ja", "Japanese");
        LANGUAGE_MAP.put("jw", "Javanese");
        LANGUAGE_MAP.put("kn", "Kannada");
        LANGUAGE_MAP.put("kk", "Kazakh");
        LANGUAGE_MAP.put("km", "Khmer");
        LANGUAGE_MAP.put("ko", "Korean");
        LANGUAGE_MAP.put("ku", "Kurdish (Kurmanji)");
        LANGUAGE_MAP.put("ky", "Kyrgyz");
        LANGUAGE_MAP.put("lo", "Lao");
        LANGUAGE_MAP.put("la", "Latin");
        LANGUAGE_MAP.put("lv", "Latvian");
        LANGUAGE_MAP.put("lt", "Lithuanian");
        LANGUAGE_MAP.put("lb", "Luxembourgish");
        LANGUAGE_MAP.put("mk", "Macedonian");
        LANGUAGE_MAP.put("mg", "Malagasy");
        LANGUAGE_MAP.put("ms", "Malay");
        LANGUAGE_MAP.put("ml", "Malayalam");
        LANGUAGE_MAP.put("mt", "Maltese");
        LANGUAGE_MAP.put("mi", "Maori");
        LANGUAGE_MAP.put("mr", "Marathi");
        LANGUAGE_MAP.put("mn", "Mongolian");
        LANGUAGE_MAP.put("my", "Myanmar (Burmese)");
        LANGUAGE_MAP.put("ne", "Nepali");
        LANGUAGE_MAP.put("no", "Norwegian");
        LANGUAGE_MAP.put("ps", "Pashto");
        LANGUAGE_MAP.put("fa", "Persian");
        LANGUAGE_MAP.put("pl", "Polish");
        LANGUAGE_MAP.put("pt", "Portuguese");
        LANGUAGE_MAP.put("ma", "Punjabi");
        LANGUAGE_MAP.put("ro", "Romanian");
        LANGUAGE_MAP.put("ru", "Russian");
        LANGUAGE_MAP.put("sm", "Samoan");
        LANGUAGE_MAP.put("gd", "Scots Gaelic");
        LANGUAGE_MAP.put("sr", "Serbian");
        LANGUAGE_MAP.put("st", "Sesotho");
        LANGUAGE_MAP.put("sn", "Shona");
        LANGUAGE_MAP.put("sd", "Sindhi");
        LANGUAGE_MAP.put("si", "Sinhala");
        LANGUAGE_MAP.put("sk", "Slovak");
        LANGUAGE_MAP.put("sl", "Slovenian");
        LANGUAGE_MAP.put("so", "Somali");
        LANGUAGE_MAP.put("es", "Spanish");
        LANGUAGE_MAP.put("su", "Sundanese");
        LANGUAGE_MAP.put("sw", "Swahili");
        LANGUAGE_MAP.put("sv", "Swedish");
        LANGUAGE_MAP.put("tg", "Tajik");
        LANGUAGE_MAP.put("ta", "Tamil");
        LANGUAGE_MAP.put("te", "Telugu");
        LANGUAGE_MAP.put("th", "Thai");
        LANGUAGE_MAP.put("tr", "Turkish");
        LANGUAGE_MAP.put("uk", "Ukrainian");
        LANGUAGE_MAP.put("ur", "Urdu");
        LANGUAGE_MAP.put("uz", "Uzbek");
        LANGUAGE_MAP.put("vi", "Vietnamese");
        LANGUAGE_MAP.put("cy", "Welsh");
        LANGUAGE_MAP.put("xh", "Xhosa");
        LANGUAGE_MAP.put("yi", "Yiddish");
        LANGUAGE_MAP.put("yo", "Yoruba");
        LANGUAGE_MAP.put("zu", "Zulu");
    }


}
