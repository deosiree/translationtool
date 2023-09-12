package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 
 * @TableName t_version_table
 */
@TableName(value ="t_version_table")
@Data
public class VersionTable implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private String id;

    /**
     * 版本表名
     */
    private String versionTableName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 版本名
     */
    private String version;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}