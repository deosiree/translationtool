package com.shr.translationtoolservice.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class SykEntryVO {
    private String productName;
    private String versionName;
    private String taskName;
    private String id;
    private String entry;
    private String translate;
    private String classify;
    private String abbr;
    private String entrySource;
    private String diName;
    private String userName;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;
    private String tag;
    private String comment;
    private int entryState;
    private String translateState;
}
