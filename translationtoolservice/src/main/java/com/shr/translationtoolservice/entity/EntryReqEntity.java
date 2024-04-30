package com.shr.translationtoolservice.entity;

import com.sun.org.glassfish.gmbal.Description;
import lombok.Data;

import java.util.Date;

/**
 * @ClassName Term
 * @Description 词条bean
 * @USER: Cola
 * @Date 2023/6/19 0019 14:08
 **/
@Data
public class EntryReqEntity {

    @Description(value = "词条内容")
    private String entry;

    @Description(value = "Abbr")
    private String abbr;

    @Description(value = "词库")
    private String lexicon;

    @Description(value = "创建人")
    private String creator;

    @Description(value = "创建开始日期")
    private Date createStartDate;

    @Description(value = "创建结束日期")
    private Date createEndDate;

    @Description(value = "修改人")
    private String update;

    @Description(value = "修改开始日期")
    private Date updateStartDate;

    @Description(value = "修改结束日期")
    private Date updateEndDate;

    @Description(value = "审核状态")
    private String entryState;

    @Description(value = "版本信息")
    private String version;

    @Description(value = "翻译内容")
    private String translate;

  /*  @Description(value = "英文翻译")
    private String english;

    @Description(value = "俄文翻译")
    private String russian;

    @Description(value = "西班牙文翻译")
    private String spanish;

    @Description(value = "法语翻译")
    private String french;*/
}
