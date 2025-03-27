package com.shr.translationtoolservice.entity.vo;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class CheckNotUseEntryVO {

    private Map<String,String> params;

    private Map<String,List<String>> sources;


    
}
