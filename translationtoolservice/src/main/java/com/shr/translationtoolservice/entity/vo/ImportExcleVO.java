package com.shr.translationtoolservice.entity.vo;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;

/**
 * @ClassName ImportExcleVO
 * @Description excle导入实体
 * @USER: Cola
 * @Date 2024/1/17 0017 11:07
 **/
@Data
@Component
public class ImportExcleVO {
    @Excel(name = "序号", orderNum = "1")
    private int num;
    @Excel(name = "词条", orderNum = "1")
    private String entry;
    @Excel(name = "翻译", orderNum = "1")
    private String translate;
    @Excel(name = "分类", orderNum = "1")
    private String classfy;

    @Excel(name = "翻译类型", orderNum = "1")
    private String translateType;

    @Excel(name = "tag", orderNum = "1")
    private String tag;

    @Excel(name = "abbr", orderNum = "1")
    private String abbr;

    @Excel(name = "词条路径", orderNum = "1")
    private String source;
    @Excel(name = "备注", orderNum = "1")
    private String remark;



    public final static HashMap<String,String> aliasMap = new HashMap(){{
        put("num","序号");
        put("abbr","Abbr");
        put("translate","翻译");
        put("entry","词条");
        put("classfy","分类");
        put("translateType","翻译类型");
        put("tag","tag");
        put("source","词条路径");

        put("remark","备注");
    }};



}
