package com.shr.translationtoolservice.util;

/**
 * @ClassName ExcleHead
 * @USER: Cola
 * @Date 2023/8/30 0030 10:02
 **/
public class ExcelHead {
    private String excelName;             //Excel名
    private String entityName;            //实体类属性名
    private boolean required=false;      //值必填

    public String getExcelName() {
        return excelName;
    }

    public void setExcelName(String excelName) {
        this.excelName = excelName;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public ExcelHead(String excelName, String entityName, boolean required) {
        this.excelName = excelName;
        this.entityName = entityName;
        this.required = required;
    }

    public ExcelHead(String excelName, String entityName) {
        this.excelName = excelName;
        this.entityName = entityName;
    }

    public ExcelHead(){};
}


