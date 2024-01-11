package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 
 * @TableName t_entry_temp
 */
@TableName(value ="t_entry_temp")
@Data
public class EntryTempEntity implements Serializable {

    //重复词条
    @TableField(exist = false)
    private List<EntryTempEntity> children;

    //聚合id
    @TableField(value = "parent_id")
    private String parentID;

    /**
     * 唯一id
     */
    @TableId(value = "id")
    private String id;

    /**
     * 翻译ID
     */
    @TableField(value = "translate_id")
    private String translateID;

    /**
     * 词条
     */
    @TableField(value = "entry")
    private String entry;

    /**
     * 翻译
     */
    @TableField(value = "translate")
    private String translate;

    /**
     * 翻译类型
     */
    @TableField(value = "translate_type")
    private String translateType;

    /**
     * tag
     */
    @TableField(value = "tag")
    private String tag;

    /**
     * 状态(创建 0，导入 1，词条审核2，翻译 3 ， 翻译审核 4， 导出 5，结束 6)
     */
    @TableField(value = "audit_state")
    private Integer auditState;

    /**
     * 翻译审核意见
     */
    @TableField(value = "audit_trans_feedback")
    private String auditTransFeedback;

    /**
     * 词条审核意见
     */
    @TableField(value = "audit_entry_feedback")
    private String auditEntryFeedback;

    /**
     * Abbr
     */
    @TableField(value = "abbr")
    private String abbr;

    /**
     * 任务id
     */
    @TableField(value = "task_id")
    private String taskId;

    /**
     * 词条路径
     */
    @TableField(value = "source")
    private String source;

    /**
     * 版本ID
     */
    @TableField(value = "version_id")
    private String versionID;

    /**
     * 版本ID
     */
    @TableField(value = "import_type")
    private String importype;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}