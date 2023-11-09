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
 * @TableName t_task_info
 */
@TableName(value ="t_task_info")
@Data
public class TaskInfoEntity implements Serializable {



    private String version;
    private String productName;



    /**
     * 主键
     */
    @TableField(value = "id")
    private String id;

    /**
     * 创建人
     */
    @TableField(value = "creator")
    private String creator;

    /**
     * 任务名字
     */
    @TableField(value = "name")
    private String name;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 结束时间
     */
    @TableField(value = "end_time")
    private Date endTime;

    /**
     * 部门
     */
    @TableField(value = "department")
    private String department;

    /**
     * 开发员
     */
    @TableField(value = "developer")
    private String developer;

    /**
     * 词条审核员
     */
    @TableField(value = "entry_auditor")
    private String entryAuditor;

    /**
     * 翻译员
     */
    @TableField(value = "translator")
    private String translator;

    /**
     * 翻译审核员
     */
    @TableField(value = "translation_auditor")
    private String translationAuditor;

    /**
     * 任务描述
     */
    @TableField(value = " description")
    private String description;

    /**
     * 任务状态
     */
    @TableField(value = "state")
    private int state;

    /**
     * 版本ID
     */
    @TableField(value = "version_id")
    private String versionId;

    /**
     * 开发员操作时间
     */
    @TableField(value = "import_time")
    private Date importTime;

    /**
     * 词条审核操作时间
     */
    @TableField(value = "entry_autior_start_time")
    private Date entryAutiorStartTime;

    /**
     * 翻译审核操作时间
     */
    @TableField(value = "translation_auditor_start_time")
    private Date translationAuditorStartTime;

    /**
     * 翻译员操作时间
     */
    @TableField(value = "translate_start_time")
    private Date translateStartTime;

    /**
     * 任务下达时间
     */
    @TableField(value = "delivery_time")
    private Date deliveryTime;

    /**
     * 翻译类型
     */
    @TableField(value = "translate_type")
    private String translateType;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}