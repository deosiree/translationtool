package com.shr.translationtoolservice.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.Data;

/**
 * @ClassName OutputExcel
 * @Description
 * @USER: Cola
 * @Date 2024/1/10 0010 14:19
 **/
@Data
public class OutputExcel {
    @Excel(name = "序号")
    private int num;
    @Excel(name = "词条")
    private String entry;
    @Excel(name = "翻译")
    private String translate;

    @Excel(name = "版本")
    private String version;
    @Excel(name = "分类")
    private String classify;
}
