package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName t_version_table
 */
@TableName(value ="t_version_table")
@Data
public class VersionTableEntity implements Serializable {
    /**
     * 
     */
    @TableId(value = "id")
    private String id;

    /**
     * 版本表名
     */
    @TableField(value = "version_table_name")
    private String versionTableName;

    /**
     * 版本名
     */
    @TableField(value = "version_id")
    private String versionId;



    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}