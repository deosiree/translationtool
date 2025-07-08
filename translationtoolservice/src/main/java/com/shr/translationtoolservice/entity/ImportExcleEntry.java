package com.shr.translationtoolservice.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;

/**
 * @ClassName ImportExcleEntry
 * @Date 2023/9/1 0001 8:47
 **/
@Data
@Component
public class ImportExcleEntry {
    @Excel(name = "版本", orderNum = "1")
    private String version;
    @Excel(name = "Abbr", orderNum = "1")
    private String abbr;
    @Excel(name = "创建日期", orderNum = "1")
    private Date creatTime;
    @Excel(name = "创建人", orderNum = "1")
    private String creator;

    @Excel(name = "词性备注", orderNum = "1")
    private String partOfSpeech;

    @Excel(name = "术语中文释义", orderNum = "1")
    private String chineseInterpretation;

    @Excel(name = "术语英文释义", orderNum = "1")
    private String englishInterpretation;

    @Excel(name = "中文术语", orderNum = "1")
    private String entry;

    @Excel(name = "中文术语字符数", orderNum = "1")
    private Integer entryLength;
    //英文术语
    @Excel(name = "英文术语", orderNum = "1")
    private String english;
    //英文术语字符数
    private Integer enCharLength;
    @Excel(name = "西文术语", orderNum = "1")
    private String spanish;
    private Integer spaCharLength;
    @Excel(name = "俄文术语", orderNum = "1")
    private String russia;
    private Integer ruCharLength;

    //英文术语
    @Excel(name = "法文术语", orderNum = "1")
    private String french;
    //英文术语字符数
    private Integer fraCharLength;
    private String classfy1;
    private String classfy2;

    private String ABTerm;
    private int ABLength;
    private String ZSTerm;
    private int ZSLength;
    private String DDTerm;
    private int DDLength;
    private String DSTerm;
    private int DSLength;


    public final static  HashMap<String,String> aliasMap = new HashMap(){{
        put("version","版本");
        put("abbr","Abbr");
        put("creatTime","创建日期");
        put("creator","创建人");
        put("partOfSpeech","词性备注");
        put("chineseInterpretation","术语中文释义");
        put("englishInterpretation","术语英文释义");
        put("entry","中文术语术语");
        put("entryLength","中文术语字符数");
        put("english","英文术语术语");
        put("enCharLength","英文术语字符数");
        put("spanish","西文术语术语");
        put("spaCharLength","西文术语字符数");
        put("russia","俄文术语术语");
        put("ruCharLength","俄文术语字符数");
        put("french","法文术语术语");
        put("fraCharLength","法文术语字符数");
        put("classfy1","类别1");
        put("classfy2","类别2");
        put("ABTerm","ABB类全驼峰术语");
        put("ABLength","ABB类全驼峰字符数");
        put("ZSTerm","展开（缩写采用单驼峰,首字母大写）术语");
        put("ZSLength","展开（缩写采用单驼峰,首字母大写）字符数");
        put("DDTerm","单多驼峰搭配下划线（继保类似）术语");
        put("DDLength","单多驼峰搭配下划线（继保类似）字符数");
        put("DSTerm","展开（缩写可采用单/多驼峰）术语");
        put("DSLength","展开（缩写可采用单/多驼峰）字符数");
        put("diFileName","回写词典");
    }};




}
