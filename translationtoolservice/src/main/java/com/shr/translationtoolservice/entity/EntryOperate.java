package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @ClassName EntryOperate
 * @Description TODO
 * @USER: Cola
 * @Date 2023/6/27 0027 10:27
 **/

@Data
@TableName(value ="t_entry_operate")
public class EntryOperate {
    private String id;
    private String operator;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date operateTime;
    private String operateContent;
    private String entryId;
    private String notes;
    private String type;

    private Date startOperateTime;
    private Date endOperateTime;
}
