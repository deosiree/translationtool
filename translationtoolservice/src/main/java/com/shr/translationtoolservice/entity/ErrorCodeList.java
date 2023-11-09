package com.shr.translationtoolservice.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ：210093
 * @date ：Created in 2023/6/19 17:32
 * @description：ErrorCodeList
 */

public class ErrorCodeList {
    public static final String NAME_EXIST = "名称已存在。";
    public static final String OBJECT_NOT_EXIST = "对象不存在。";
    public static final String INPUT_IS_NULL = "入参为空。";
    public static final String VERSION_IS_EXIST = "version已存在。";
    public static final String TBALE_IS_NULL = "tableName 不能为空。";
    public static final String PRIORITY_IS_ILL = "优先级非法。";
    public static final String PARAM_IS_ILL = "入参非法。";
    public static final String SUCCESS = "success";
    public static final String INSERT_ERROR = "新增失败。";
    public static final String UPDATE_ERROR = "更新失败。";
    public static final String UPDATE_RES_DEFAULT = "Default大于1条。";
    public static final String UPDATE_NOT_EXIST= "找不到对应数据";
    public static final String BRIEF_IS_NOT_JSON = "brief不是json格式。";
    public static final String UPDATE_LOSE_PRIMARYKEY="修改时缺少主键";
    public static final String DELETE_ERROR = "删除失败";
    public static final String UPLOADFAIL = "上传文件失败";
    public static final String JUSTSUPPORTJSON = "只支持JSON格式文件";
    public static final String READJSONFAIL = "读取JSON文件失败";
    public static final String FILENOTEXIST = "文件不存在";
    public static final String OBJECT_HAS_EXIST = "对象已存在。";
    public static final String EXPORT_ERROR = "导出失败";
    public static final String ABBR_HAS_EXIST = "ABBR已存在。";

    private final static List<String> ErrorCodeList = new ArrayList<>();



    static {

        ErrorCodeList.add(PARAM_IS_ILL);
        ErrorCodeList.add(PRIORITY_IS_ILL);
        ErrorCodeList.add(NAME_EXIST);
        ErrorCodeList.add(INPUT_IS_NULL);
        ErrorCodeList.add(OBJECT_NOT_EXIST);
        ErrorCodeList.add(INSERT_ERROR);
        ErrorCodeList.add(UPDATE_ERROR);
        ErrorCodeList.add(BRIEF_IS_NOT_JSON);
        ErrorCodeList.add(UPDATE_LOSE_PRIMARYKEY);
        ErrorCodeList.add(DELETE_ERROR);
        ErrorCodeList.add(UPLOADFAIL);
        ErrorCodeList.add(JUSTSUPPORTJSON);
        ErrorCodeList.add(READJSONFAIL);
        ErrorCodeList.add(FILENOTEXIST);
        ErrorCodeList.add(OBJECT_HAS_EXIST);
        ErrorCodeList.add(EXPORT_ERROR);
    }

    private ErrorCodeList()
    {

    }


    public static  String insertResult(int insert){
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            return INSERT_ERROR;
        }else {
            return ConstantInterface.OK_STR;
        }
    }

    public static List<String> getErrorCodeList()
    {
        return ErrorCodeList;
    }
}
