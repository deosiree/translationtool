package com.shr.translationtoolservice.entity;

/**
 * @author ：210093
 * @date ：Created in 2023/6/19 16:02
 * @description：字符串常量
 */
public class ConstantInterface {
    public static final String TASK_TYPE = "task_type";
    public static final String DEVICE_LEVEL = "device_level";
    public static final String INTERVAL_TYPE = "interval_type";
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT1 = "yyyy-MM-dd";
    public static final String DATE_FORMAT2 = "yyyy年MM月dd日";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String YEAR_MONTH_DAY_FORMAT = "yyyy-MM-dd";
    public static final String EXPORT_IMAGE_BUCKET = "exportimage";
    public static final String EXPORT_BUCKET = "export/";
    public static final String UNDERLINE_SEPARATOR = "_";
    public static final String COMMA_SEPARATOR = ",";
    public static final String FULL_SEPARATOR = "。";
    public static final String SPACE_SEPARATOR = " ";
    public static final String COLON = ":";
    public static final char SPACE_SEPARATOR1 = ' ';
    public static final String LINE_SEPARATOR = "-";
    public static final String SLASH_SEPARATOR = "/";
    public static final String POINT_SEPARATOR = ".";
    public static final char SLASH_SEPARATOR1 = '/';
    public static final String HAS_FINISH = "已完成";
    public static final String NOT_FINISH = "未完成";
    public static final int PRIORITY_MIN = 1;
    public static final int PRIORITY_MAX = 5;
    public static final int DB_SUCCESS_RESULT = 1;
    public static final String XLS_SUFFIX = ".xls";
    public static final String TASK_ENABLE = "0";
    public static final String TASK_DISABLE = "1";
    public static final String TASK_AUDIT = "已审核";
    public static final String TASK_NOT_AUDIT = "待审核";
    public static final String OK_STR = "OK";
    public static final String FAIL_STR = "Fail";
    public static final String DEVICE_POS = "device_pos\":\"";
    public static final String ONE_HUNDRED = "100";
    public static final String OVERVIEW_STATE = "state";
    public static final String OVERVIEW_TYPE = "type";
    public static final String BUCKET_POLICY = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\"," +
            "\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetBucketLocation\",\"s3:ListBucket\"],\"Resource\":[\"arn:aws:s3:::bucketname\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::bucketname/*\"]}]}";
    public static final String OPERATION_ID_STR = "操作信息ID";
    public static final String DEVICE_POINT_EXPORT = "_device_point_model_export.xls";
    public static final String MISSION_STATE_EXPORT = "_mission_state_export.xls";
    public static final String ALARM_STATE_EXPORT = "_alarm_state_export.xls";
    public static final String ROBOTMAINTENANCERECORD_EXPORT = "_robotMaintenanceRecord_export.xls";
    public static final String BAY_DEVICE_EXPORT = "_bay_device_export.xls";
    public static final String TASK_MODEL_EXPORT = "_task_model_export.xls";
    public static final String PATROL_DEVICE_MODEL_EXPORT = "_patrol_device_export.xls";
    public static final String MAINTENANCE_HAS_FINISH = "已结束";
    public static final String MAINTENANCE_IS_GOING = "检修中";
    public static final Short VIDEO_NUM = 1;
    public static final String VIDEO_STR = "视频";
    public static final String OR_STR = "或";
    public static final Short ROBOT_NUM = 2;
    public static final String ROBOT_STR = "机器人";
    public static final String ZIP_SUFFIX = ".zip";
    public static final String AUDIT_PATH = "/**";
    public static final Short VALID_FAIL = 0;
    public static final Short VALID_SUCCESS = 1;
    public static final Short VALID_ABNORMAL = 2;
    public static final Short VALID_ALARM = 3;
    public static final Short VALID_REPAIR = 4;
    public static final String VALID_FAIL_STR = "失败";
    public static final String VALID_SUCCESS_STR = "正常";
    public static final String SUCCESS = "成功";
    public static final String FAIL = "失败";



}
