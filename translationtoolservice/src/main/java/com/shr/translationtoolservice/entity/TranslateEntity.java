package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 
 * @TableName t_translate
 */
@TableName(value ="t_translate")
@Data
public class TranslateEntity implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id")
    private String id;

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
     * 唯一属性
     */
    @TableField(value = "`unique`")
    private String unique;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 翻译类型
     */
    @TableField(value = "type")
    private String type;

    /**
     * 可见范围
     */
    @TableField(value = "visual_range")
    private String visualRange;

    /**
     * 公共库（0否，1是）
     */
    @TableField(value = "public_state")
    private Integer publicState;

    /**
     * 删除状态（0否1是）
     */
    @TableField(value = "delete_state")
    private Integer deleteState;

    /**
     * 翻译状态（0未翻译，1已翻译未审核，2审核不通过，3审核已通过）
     */
    @TableField(value = "translate_state")
    private String translateState;


    /**
     * 版本id
     */
    @TableField(value = "version_id")
    private String versionID;

    /**
     * 翻译建议
     */
    @TableField(value = "audit_suggest")
    private String auditSuggest;

    /**
     * 翻译字符数
     */
    @TableField(value = "char_length")
    private int charLength;

    /**
     * 最大限制字符数
     */
    @TableField(value = "max_length")
    private int maxLength;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}