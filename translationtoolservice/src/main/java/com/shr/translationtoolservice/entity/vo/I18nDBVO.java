package com.shr.translationtoolservice.entity.vo;

import lombok.Data;

/**
 * @ClassName i18nDBVO
 * @Description i18n 前端参数传输实体
 * @USER: Cola
 * @Date 2024/3/12 0012 16:47
 **/
@Data
public class I18nDBVO {
    private String tableId;
    private String type;
    private String node;
    private String app;
    private String db;
    private String modeType;


}
