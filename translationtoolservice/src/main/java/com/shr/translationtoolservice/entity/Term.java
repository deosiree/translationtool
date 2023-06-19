package com.shr.translationtoolservice.entity;

import com.sun.org.glassfish.gmbal.Description;
import lombok.Data;

/**
 * @ClassName Term
 * @Description 词条bean
 * @USER: Cola
 * @Date 2023/6/19 0019 14:08
 **/
@Data
public class Term {

    @Description(value = "词条内容")
    private String condent;

    @Description(value = "词库")
    private String lexicon;

    @Description(value = "创建人")
    private String creator;

    @Description(value = "创建开始日期")
    private String create_start_date;

    @Description(value = "创建结束日期")
    private String create_end_date;

    @Description(value = "修改人")
    private String modifier;

    @Description(value = "修改开始日期")
    private String modify_end_date;

    @Description(value = "修改结束日期")
    private String modify_start_date;

    @Description(value = "审核状态")
    private String auditStaus;
}
