package com.shr.translationtoolservice.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ：210093
 * @date ：Created in 2023/6/19 16:02
 * @description：字符串常量
 */
public class ConstantInterface {


    public static final int ZERO = 0;
    public static final int DB_SUCCESS_RESULT = 1;

    public static  final  String DELIVERY_STATE="0";
    public static  final  String IMPORT_STATE="1";
    public static  final  String ENTRY_AUDIT_STATE="2";
    public static  final  String TRANSLATE_STATE="3";
    public static  final  String TRANSLATE_AUDIT_STATE="4";
    public static  final  String EXPORT_STATE="5";


    public static final String IS_DEFAULT = "1";
    public static final String OK_STR = "OK";
    public static final String NOCHANGE = "词条未有变化";
    public static final String REPETITION_STR = "版本库存在重复的词条";

    public static final String UNTRANSLATED = "未翻译";
    public static final String TRANSLATED = "已翻译";
    public static final String TRANSLATING = "正在翻译";

    public static final String OPERATION_TYPE_INSERT = "新增";
    public static final String OPERATION_TYPE_UPDATE = "编辑";
    public static final String OPERATION_TYPE_DELETE = "删除";
    public static final String OPERATION_TYPE_UPGRATE = "升级";
    public static final String OPERATION_TYPE_AUDIT = "审核";

    public static final String CREATE_STATE = "创建";

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

    public static final String ENTRY_AUDITOR = "词条审核员";
    public static final String TRANSLATE_AUDITOR = "翻译审核员";
    public static final String TRANSLATOR = "翻译员";
    public static final String DEVELOPER = "开发员";
    public static final String ADMIN = "管理员";


    public static final String EN_TRANS = "英文翻译";
    public static final String RU_TRANS = "俄文翻译";
    public static final String SPA_TRANS = "西文翻译";
    public static final String FRA_TRANS = "法文翻译";



    public static HashMap<String, String> constructUserAndRole() {
        HashMap<String, String> user_role = new HashMap<>();
        user_role.put("词条审核员", "ENTRY_AUDITOR");
        user_role.put("翻译审核员", "TRANSLATE_AUDITOR");
        user_role.put("翻译员", "TRANSLATOR");
        user_role.put("开发员", "DEVELOPER");
        user_role.put("管理员", "ADMIN");
        return user_role;
    }




    public static  Map<String,String> LANGUAGE_MAP=new HashMap();


    public static final String UNION  =" union all";
    public static final String SEARCH = " select  <include refid=\"Base_Column_List\"/>,#{entryEntity.tableName}  as tableName  from    ";
    private static ConstantInterface _instance = null;
    /**
     * 获取单例
     * @return
     */
    public static ConstantInterface getInstance() {
        if( null == _instance){
            _instance = new ConstantInterface();
            _instance.init();
        }
        return _instance;
    }

    public static HashMap<String, String> constructEntryName() {
        HashMap<String, String> entryName = new HashMap<>();
        entryName.put("chinese", "词条");
        entryName.put("abbr", "abbr");
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
        entryName.put("version", "版本");
        entryName.put("isLatestVersion", "是否最新版本");
        entryName.put("entryLabel", "词条标签");
        entryName.put("partOfSpeech", "词性备注");
        entryName.put("classifyId", "词条所属分类");
        entryName.put("repeatEntryId", "重复词条id");
        entryName.put("enTransId", EN_TRANS);
        entryName.put("ruTransId", RU_TRANS);
        entryName.put("russianTranslateState", "俄文翻译状态");
        entryName.put("spaTransId", SPA_TRANS);
        entryName.put("spanishTranslateState", "西文翻译状态");
        entryName.put("fraTransId", FRA_TRANS);
        entryName.put("frenchTranslateState", "法文翻译状态");
        entryName.put("environmentRemark", "环境备注");
        entryName.put("remark", "备注");
        return entryName;
    }



    /**
     * 初始化语言类
     */
    private void init(){
        LANGUAGE_MAP.put("zh","Chinese");
        LANGUAGE_MAP.put("spa","Spanish");
        LANGUAGE_MAP.put("auto","Automatic");
        LANGUAGE_MAP.put("af","Afrikaans");
        LANGUAGE_MAP.put("sq","Albanian");
        LANGUAGE_MAP.put("am","Amharic");
        LANGUAGE_MAP.put("ar","Arabic");
        LANGUAGE_MAP.put("hy","Armenian");
        LANGUAGE_MAP.put("az","Azerbaijani");
        LANGUAGE_MAP.put("eu","Basque");
        LANGUAGE_MAP.put("be","Belarusian");
        LANGUAGE_MAP.put("bn","Bengali");
        LANGUAGE_MAP.put("bs","Bosnian");
        LANGUAGE_MAP.put("bg","Bulgarian");
        LANGUAGE_MAP.put("ca","Catalan");
        LANGUAGE_MAP.put("ceb","Cebuano");
        LANGUAGE_MAP.put("ny","Chichewa");
        LANGUAGE_MAP.put("zh_cn","Chinese Simplified");
        LANGUAGE_MAP.put("zh_tw","Chinese Traditional");
        LANGUAGE_MAP.put("co","Corsican");
        LANGUAGE_MAP.put("hr","Croatian");
        LANGUAGE_MAP.put("cs","Czech");
        LANGUAGE_MAP.put("da","Danish");
        LANGUAGE_MAP.put("nl","Dutch");
        LANGUAGE_MAP.put("en","English");
        LANGUAGE_MAP.put("eo","Esperanto");
        LANGUAGE_MAP.put("et","Estonian");
        LANGUAGE_MAP.put("tl","Filipino");
        LANGUAGE_MAP.put("fi","Finnish");
        LANGUAGE_MAP.put("fra","French");
        LANGUAGE_MAP.put("fy","Frisian");
        LANGUAGE_MAP.put("gl","Galician");
        LANGUAGE_MAP.put("ka","Georgian");
        LANGUAGE_MAP.put("de","German");
        LANGUAGE_MAP.put("el","Greek");
        LANGUAGE_MAP.put("gu","Gujarati");
        LANGUAGE_MAP.put("ht","Haitian Creole");
        LANGUAGE_MAP.put("ha","Hausa");
        LANGUAGE_MAP.put("haw","Hawaiian");
        LANGUAGE_MAP.put("iw","Hebrew");
        LANGUAGE_MAP.put("hi","Hindi");
        LANGUAGE_MAP.put("hmn","Hmong");
        LANGUAGE_MAP.put("hu","Hungarian");
        LANGUAGE_MAP.put("is","Icelandic");
        LANGUAGE_MAP.put("ig","Igbo");
        LANGUAGE_MAP.put("id","Indonesian");
        LANGUAGE_MAP.put("ga","Irish");
        LANGUAGE_MAP.put("it","Italian");
        LANGUAGE_MAP.put("ja","Japanese");
        LANGUAGE_MAP.put("jw","Javanese");
        LANGUAGE_MAP.put("kn","Kannada");
        LANGUAGE_MAP.put("kk","Kazakh");
        LANGUAGE_MAP.put("km","Khmer");
        LANGUAGE_MAP.put("ko","Korean");
        LANGUAGE_MAP.put("ku","Kurdish (Kurmanji)");
        LANGUAGE_MAP.put("ky","Kyrgyz");
        LANGUAGE_MAP.put("lo","Lao");
        LANGUAGE_MAP.put("la","Latin");
        LANGUAGE_MAP.put("lv","Latvian");
        LANGUAGE_MAP.put("lt","Lithuanian");
        LANGUAGE_MAP.put("lb","Luxembourgish");
        LANGUAGE_MAP.put("mk","Macedonian");
        LANGUAGE_MAP.put("mg","Malagasy");
        LANGUAGE_MAP.put("ms","Malay");
        LANGUAGE_MAP.put("ml","Malayalam");
        LANGUAGE_MAP.put("mt","Maltese");
        LANGUAGE_MAP.put("mi","Maori");
        LANGUAGE_MAP.put("mr","Marathi");
        LANGUAGE_MAP.put("mn","Mongolian");
        LANGUAGE_MAP.put("my","Myanmar (Burmese)");
        LANGUAGE_MAP.put("ne","Nepali");
        LANGUAGE_MAP.put("no","Norwegian");
        LANGUAGE_MAP.put("ps","Pashto");
        LANGUAGE_MAP.put("fa","Persian");
        LANGUAGE_MAP.put("pl","Polish");
        LANGUAGE_MAP.put("pt","Portuguese");
        LANGUAGE_MAP.put("ma","Punjabi");
        LANGUAGE_MAP.put("ro","Romanian");
        LANGUAGE_MAP.put("ru","Russian");
        LANGUAGE_MAP.put("sm","Samoan");
        LANGUAGE_MAP.put("gd","Scots Gaelic");
        LANGUAGE_MAP.put("sr","Serbian");
        LANGUAGE_MAP.put("st","Sesotho");
        LANGUAGE_MAP.put("sn","Shona");
        LANGUAGE_MAP.put("sd","Sindhi");
        LANGUAGE_MAP.put("si","Sinhala");
        LANGUAGE_MAP.put("sk","Slovak");
        LANGUAGE_MAP.put("sl","Slovenian");
        LANGUAGE_MAP.put("so","Somali");
        LANGUAGE_MAP.put("es","Spanish");
        LANGUAGE_MAP.put("su","Sundanese");
        LANGUAGE_MAP.put("sw","Swahili");
        LANGUAGE_MAP.put("sv","Swedish");
        LANGUAGE_MAP.put("tg","Tajik");
        LANGUAGE_MAP.put("ta","Tamil");
        LANGUAGE_MAP.put("te","Telugu");
        LANGUAGE_MAP.put("th","Thai");
        LANGUAGE_MAP.put("tr","Turkish");
        LANGUAGE_MAP.put("uk","Ukrainian");
        LANGUAGE_MAP.put("ur","Urdu");
        LANGUAGE_MAP.put("uz","Uzbek");
        LANGUAGE_MAP.put("vi","Vietnamese");
        LANGUAGE_MAP.put("cy","Welsh");
        LANGUAGE_MAP.put("xh","Xhosa");
        LANGUAGE_MAP.put("yi","Yiddish");
        LANGUAGE_MAP.put("yo","Yoruba");
        LANGUAGE_MAP.put("zu","Zulu");
    }


}
